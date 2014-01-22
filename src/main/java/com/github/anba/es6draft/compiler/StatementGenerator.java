/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsConstantDeclaration;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicalDeclarations;

import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.compiler.JumpLabel.BreakLabel;
import com.github.anba.es6draft.compiler.JumpLabel.ContinueLabel;
import com.github.anba.es6draft.compiler.JumpLabel.TempLabel;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 *
 */
final class StatementGenerator extends
        DefaultCodeGenerator<StatementGenerator.Completion, StatementVisitor> {
    /**
     * 6.2.2 The Completion Record Specification Type
     */
    enum Completion {
        Normal, Return, Throw, Break, Continue, Abrupt;

        boolean isAbrupt() {
            return this != Normal;
        }

        Completion then(Completion next) {
            return this != Normal ? this : next;
        }

        Completion select(Completion other) {
            return this == Normal || other == Normal ? Normal : this == other ? this : Abrupt;
        }

        Completion normal(boolean b) {
            return b ? Normal : this;
        }
    }

    private static final class Methods {
        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc EnvironmentRecord_createImmutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createImmutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String));

        // class: Iterator
        static final MethodDesc Iterator_hasNext = MethodDesc.create(MethodType.Interface,
                Types.Iterator, "hasNext", Type.getMethodType(Type.BOOLEAN_TYPE));

        static final MethodDesc Iterator_next = MethodDesc.create(MethodType.Interface,
                Types.Iterator, "next", Type.getMethodType(Types.Object));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        // class: Reference
        static final MethodDesc Reference_putValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "putValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptException
        static final MethodDesc ScriptException_create = MethodDesc.create(MethodType.Static,
                Types.ScriptException, "create",
                Type.getMethodType(Types.ScriptException, Types.Object));

        static final MethodDesc ScriptException_getValue = MethodDesc.create(MethodType.Virtual,
                Types.ScriptException, "getValue", Type.getMethodType(Types.Object));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_debugger = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "debugger", Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc ScriptRuntime_ensureObject = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "ensureObject",
                Type.getMethodType(Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_enumerate = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "enumerate",
                Type.getMethodType(Types.Iterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_enumerateValues = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "enumerateValues",
                Type.getMethodType(Types.Iterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_getStackOverflowError = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "getStackOverflowError",
                Type.getMethodType(Types.StackOverflowError, Types.Error));

        static final MethodDesc ScriptRuntime_iterate = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "iterate",
                Type.getMethodType(Types.Iterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_toInternalError = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "toInternalError", Type.getMethodType(
                        Types.ScriptException, Types.StackOverflowError, Types.ExecutionContext));
    }

    public StatementGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * stack: [Reference, Object] -> []
     */
    private void PutValue(Expression node, ValType type, StatementVisitor mv) {
        assert type == ValType.Reference : "lhs is not reference: " + type;

        mv.loadExecutionContext();
        mv.invoke(Methods.Reference_putValue);
    }

    /**
     * stack: [envRec] -> [envRec]
     */
    private void createImmutableBinding(String name, StatementVisitor mv) {
        mv.dup();
        mv.aconst(name);
        mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);
    }

    /**
     * stack: [envRec] -> [envRec]
     */
    private void createMutableBinding(String name, boolean deletable, StatementVisitor mv) {
        mv.dup();
        mv.aconst(name);
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    /**
     * stack: [value] -> [value]
     */
    private void ensureObjectOrThrow(ValType type, StatementVisitor mv) {
        if (type != ValType.Object) {
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_ensureObject);
        }
    }

    @Override
    protected Completion visit(Node node, StatementVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 13.1.10 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(BlockStatement node, StatementVisitor mv) {
        if (node.getStatements().isEmpty()) {
            // Block : { }
            // -> Return NormalCompletion(empty)
            return Completion.Normal;
        }

        mv.enterScope(node);
        /* steps 1-4 */
        boolean hasDeclarations = !LexicalDeclarations(node).isEmpty();
        if (hasDeclarations) {
            newDeclarativeEnvironment(mv);
            new BlockDeclarationInstantiationGenerator(codegen).generate(node, mv);
            pushLexicalEnvironment(mv);
        }

        /* step 5 */
        Completion result = Completion.Normal;
        {
            // 13.1.10 Runtime Semantics: Evaluation
            // StatementList : StatementList StatementListItem
            /* steps 1-6 */
            for (StatementListItem statement : node.getStatements()) {
                if ((result = result.then(statement.accept(this, mv))).isAbrupt()) {
                    break;
                }
            }
        }

        /* step 6 */
        if (hasDeclarations && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        mv.exitScope();

        /* steps 7-8 */
        return result;
    }

    /**
     * 13.8.2 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(BreakStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        mv.goTo(mv.breakLabel(node));
        return Completion.Break;
    }

    /**
     * 14.5.16 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ClassDeclaration node, StatementVisitor mv) {
        /* step 1 */
        String className = node.getName().getName();
        /* steps 2-3 */
        ClassDefinitionEvaluation(node, className, mv);
        /* steps 4-6 */
        SetFunctionName(node, className, mv);

        /* steps 7-9 */
        getEnvironmentRecord(mv);
        mv.swap();
        // stack: [envRec, value] -> []
        BindingInitialisationWithEnvironment(node.getName(), mv);

        /* step 10 */
        return Completion.Normal;
    }

    /**
     * 13.7.2 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ContinueStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        mv.goTo(mv.continueLabel(node));
        return Completion.Continue;
    }

    /**
     * 13.15.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(DebuggerStatement node, StatementVisitor mv) {
        /* steps 1-3 */
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_debugger);
        return Completion.Normal;
    }

    /**
     * 13.0.3 Runtime Semantics: Evaluation<br>
     * 13.0.2 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.1.2 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(DoWhileStatement node, StatementVisitor mv) {
        Label lblNext = new Label();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();

        // L1: <statement>
        // IFNE ToBoolean(<expr>) L1

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);

        Completion result;
        mv.mark(lblNext);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        if (lblContinue.isUsed()) {
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        if (!result.isAbrupt() || lblContinue.isUsed()) {
            ValType type = expressionValue(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblNext);
        }

        mv.mark(lblBreak);
        if (lblBreak.isUsed()) {
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }

        mv.exitVariableScope();

        return result.normal(lblContinue.isUsed() || lblBreak.isUsed());
    }

    /**
     * 13.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(EmptyStatement node, StatementVisitor mv) {
        // nothing to do!
        return Completion.Normal;
    }

    /**
     * 13.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExpressionStatement node, StatementVisitor mv) {
        ValType type = expressionValue(node.getExpression(), mv);
        mv.storeCompletionValue(type);
        return Completion.Normal;
    }

    private enum IterationKind {
        Enumerate, Iterate, EnumerateValues
    }

    /**
     * Extension: 'for-each' statement
     */
    @Override
    public Completion visit(ForEachStatement node, StatementVisitor mv) {
        return visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.EnumerateValues, mv);
    }

    /**
     * 13.0.3 Runtime Semantics: Evaluation<br>
     * 13.0.2 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.4.5 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForInStatement node, StatementVisitor mv) {
        return visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.Enumerate, mv);
    }

    /**
     * 13.0.3 Runtime Semantics: Evaluation<br>
     * 13.0.2 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.4.5 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForOfStatement node, StatementVisitor mv) {
        return visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.Iterate, mv);
    }

    /**
     * 13.6.4.6 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation<br>
     * 13.6.4.7 Runtime Semantics: ForIn/OfBodyEvaluation
     */
    private <FORSTATEMENT extends IterationStatement & ScopedNode> Completion visitForInOfLoop(
            FORSTATEMENT node, Expression expr, Node lhs, Statement stmt,
            IterationKind iterationKind, StatementVisitor mv) {
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();
        Label loopbody = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);
        @SuppressWarnings("rawtypes")
        Variable<Iterator> iter = mv.newVariable("iter", Iterator.class);

        // Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
        ValType type = expressionValue(expr, mv);
        if (type != ValType.Object) {
            mv.toBoxed(type);
            Label loopstart = new Label();
            mv.dup();
            isUndefinedOrNull(mv);
            mv.ifeq(loopstart);
            mv.pop();
            mv.goTo(lblBreak);
            mv.mark(loopstart);
        }

        if ((iterationKind == IterationKind.Enumerate || iterationKind == IterationKind.EnumerateValues)
                && codegen.isEnabled(CompatibilityOption.LegacyGenerator)) {
            // legacy generator mode, both, for-in and for-each, perform Iterate on generators
            Label l0 = new Label(), l1 = new Label();
            mv.dup();
            mv.instanceOf(Types.GeneratorObject);
            mv.ifeq(l0);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_iterate);
            mv.goTo(l1);
            mv.mark(l0);
            mv.loadExecutionContext();
            if (iterationKind == IterationKind.Enumerate) {
                mv.invoke(Methods.ScriptRuntime_enumerate);
            } else {
                mv.invoke(Methods.ScriptRuntime_enumerateValues);
            }
            mv.mark(l1);
        } else {
            mv.loadExecutionContext();
            if (iterationKind == IterationKind.Enumerate) {
                mv.invoke(Methods.ScriptRuntime_enumerate);
            } else if (iterationKind == IterationKind.EnumerateValues) {
                mv.invoke(Methods.ScriptRuntime_enumerateValues);
            } else {
                assert iterationKind == IterationKind.Iterate;
                mv.invoke(Methods.ScriptRuntime_iterate);
            }
        }

        // Runtime Semantics: ForIn/OfBodyEvaluation
        mv.store(iter);

        mv.goTo(lblContinue);

        mv.mark(loopbody);
        mv.load(iter);
        mv.invoke(Methods.Iterator_next);

        if (lhs instanceof Expression) {
            assert lhs instanceof LeftHandSideExpression;
            if (lhs instanceof AssignmentPattern) {
                ensureObjectOrThrow(ValType.Any, mv);
                DestructuringAssignment((AssignmentPattern) lhs, mv);
            } else {
                ValType lhsType = expression((Expression) lhs, mv);
                mv.swap();
                PutValue((Expression) lhs, lhsType, mv);
            }
        } else if (lhs instanceof VariableStatement) {
            VariableDeclaration varDecl = ((VariableStatement) lhs).getElements().get(0);
            Binding binding = varDecl.getBinding();
            // 12.1.4.2.2 Runtime Semantics: BindingInitialisation :: ForBinding
            if (binding instanceof BindingPattern) {
                ensureObjectOrThrow(ValType.Any, mv);
            }
            BindingInitialisation(binding, mv);
        } else {
            assert lhs instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) lhs;
            assert lexDecl.getElements().size() == 1;
            LexicalBinding lexicalBinding = lexDecl.getElements().get(0);

            // create new declarative lexical environment
            // stack: [nextValue] -> [nextValue, iterEnv]
            mv.enterScope(node);
            newDeclarativeEnvironment(mv);
            {
                // Runtime Semantics: Binding Instantiation
                // ForDeclaration : LetOrConst ForBinding

                // stack: [nextValue, iterEnv] -> [iterEnv, nextValue, envRec]
                mv.dupX1();
                mv.invoke(Methods.LexicalEnvironment_getEnvRec);

                // stack: [iterEnv, nextValue, envRec] -> [iterEnv, envRec, nextValue]
                for (String name : BoundNames(lexicalBinding.getBinding())) {
                    if (IsConstantDeclaration(lexDecl)) {
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                        createImmutableBinding(name, mv);
                    } else {
                        // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                        createMutableBinding(name, false, mv);
                    }
                }
                mv.swap();

                // 12.1.4.2.2 Runtime Semantics: BindingInitialisation :: ForBinding
                if (lexicalBinding.getBinding() instanceof BindingPattern) {
                    ensureObjectOrThrow(ValType.Any, mv);
                }

                // stack: [iterEnv, envRec, nextValue] -> [iterEnv]
                BindingInitialisationWithEnvironment(lexicalBinding.getBinding(), mv);
            }
            // stack: [iterEnv] -> []
            pushLexicalEnvironment(mv);
        }

        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = stmt.accept(this, mv);
            mv.exitIteration(node);
        }

        if (lhs instanceof LexicalDeclaration) {
            // restore previous lexical environment
            if (!result.isAbrupt()) {
                popLexicalEnvironment(mv);
            }
            mv.exitScope();
        }

        mv.mark(lblContinue);
        if (lblContinue.isUsed()) {
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        mv.load(iter);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(loopbody);

        mv.mark(lblBreak);
        if (lblBreak.isUsed()) {
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }

        mv.exitVariableScope();

        return result.normal(lblContinue.isUsed() || lblBreak.isUsed()).select(Completion.Normal);
    }

    /**
     * 13.0.3 Runtime Semantics: Evaluation<br>
     * 13.0.2 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.3.2 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.3.3 Runtime Semantics: ForBodyEvaluation
     */
    @Override
    public Completion visit(ForStatement node, StatementVisitor mv) {
        Node head = node.getHead();
        if (head == null) {
            // empty
        } else if (head instanceof Expression) {
            ValType type = expressionValue((Expression) head, mv);
            mv.pop(type);
        } else if (head instanceof VariableStatement) {
            head.accept(this, mv);
        } else {
            assert head instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) head;

            mv.enterScope(node);
            newDeclarativeEnvironment(mv);
            {
                // stack: [loopEnv] -> [loopEnv, envRec]
                mv.dup();
                mv.invoke(Methods.LexicalEnvironment_getEnvRec);

                boolean isConst = IsConstantDeclaration(lexDecl);
                for (String dn : BoundNames(lexDecl)) {
                    if (isConst) {
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `loopEnv`)
                        createImmutableBinding(dn, mv);
                    } else {
                        // FIXME: spec bug (CreateMutableBinding concrete method of `loopEnv`)
                        createMutableBinding(dn, false, mv);
                    }
                }
                mv.pop();
            }
            pushLexicalEnvironment(mv);

            lexDecl.accept(this, mv);
        }

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);

        Label lblTest = new Label(), lblStmt = new Label();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();

        Completion result;
        mv.goTo(lblTest);
        mv.mark(lblStmt);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        if (lblContinue.isUsed()) {
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        if (node.getStep() != null && (!result.isAbrupt() || lblContinue.isUsed())) {
            ValType type = expressionValue(node.getStep(), mv);
            mv.pop(type);
        }

        mv.mark(lblTest);
        if (node.getTest() != null) {
            ValType type = expressionValue(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblStmt);
        } else {
            mv.goTo(lblStmt);
        }

        mv.mark(lblBreak);
        if (lblBreak.isUsed()) {
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }

        mv.exitVariableScope();

        if (head instanceof LexicalDeclaration) {
            if (node.getTest() != null || lblBreak.isUsed()) {
                popLexicalEnvironment(mv);
            }
            mv.exitScope();
        }

        if (node.getTest() == null) {
            if (!result.isAbrupt() && !lblBreak.isUsed()) {
                return Completion.Abrupt; // infinite loop
            }
            return result.normal(lblBreak.isUsed());
        }
        return result.normal(lblContinue.isUsed() || lblBreak.isUsed()).select(Completion.Normal);
    }

    /**
     * 14.1.15 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(FunctionDeclaration node, StatementVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionDeclaration
        /* return NormalCompletion(empty) */

        return Completion.Normal;
    }

    /**
     * 14.4.12 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(GeneratorDeclaration node, StatementVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> GeneratorDeclaration
        /* return NormalCompletion(empty) */

        return Completion.Normal;
    }

    /**
     * 13.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(IfStatement node, StatementVisitor mv) {
        Label l0 = new Label(), l1 = new Label();

        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifeq(l0);
        Completion resultThen = node.getThen().accept(this, mv);
        if (node.getOtherwise() != null) {
            if (!resultThen.isAbrupt()) {
                mv.goTo(l1);
            }
            mv.mark(l0);
            Completion resultOtherwise = node.getOtherwise().accept(this, mv);
            mv.mark(l1);
            return resultThen.select(resultOtherwise);
        } else {
            mv.mark(l0);
            return resultThen.select(Completion.Normal);
        }
    }

    /**
     * 13.12.4 Runtime Semantics: Evaluation<br>
     * 13.12.3 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(LabelledStatement node, StatementVisitor mv) {
        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);

        BreakLabel label = new BreakLabel();
        mv.enterLabelled(node, label);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitLabelled(node);
        mv.mark(label);

        if (label.isUsed()) {
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        return result.normal(label.isUsed());
    }

    /**
     * 13.2.1.5 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(LexicalDeclaration node, StatementVisitor mv) {
        for (LexicalBinding binding : node.getElements()) {
            binding.accept(this, mv);
        }

        return Completion.Normal;
    }

    /**
     * Extension: 'let' statement
     */
    @Override
    public Completion visit(LetStatement node, StatementVisitor mv) {
        // create new declarative lexical environment
        // stack: [] -> [env]
        mv.enterScope(node);
        newDeclarativeEnvironment(mv);
        {
            // stack: [env] -> [env, envRec]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [env, envRec] -> [env]
            for (LexicalBinding binding : node.getBindings()) {
                // stack: [env, envRec] -> [env, envRec, envRec]
                mv.dup();

                // stack: [env, envRec, envRec] -> [env, envRec, envRec]
                for (String name : BoundNames(binding.getBinding())) {
                    createMutableBinding(name, false, mv);
                }

                Expression initialiser = binding.getInitialiser();
                if (initialiser != null) {
                    ValType type = expressionBoxedValue(initialiser, mv);
                    if (binding.getBinding() instanceof BindingPattern) {
                        ToObject(type, mv);
                    }
                } else {
                    assert binding.getBinding() instanceof BindingIdentifier;
                    mv.loadUndefined();
                }

                // stack: [env, envRec, envRec, value] -> [env, envRec]
                BindingInitialisationWithEnvironment(binding.getBinding(), mv);
            }
            mv.pop();
        }
        // stack: [env] -> []
        pushLexicalEnvironment(mv);

        Completion result = node.getStatement().accept(this, mv);

        // restore previous lexical environment
        if (!result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        mv.exitScope();

        return result;
    }

    /**
     * 13.2.1.6 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(LexicalBinding node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            ValType type = expressionBoxedValue(initialiser, mv);
            if (binding instanceof BindingPattern) {
                ensureObjectOrThrow(type, mv);
            }
            if (binding instanceof BindingIdentifier && IsAnonymousFunctionDefinition(initialiser)) {
                SetFunctionName(initialiser, ((BindingIdentifier) binding).getName(), mv);
            }
        } else {
            assert binding instanceof BindingIdentifier;
            mv.loadUndefined();
        }

        getEnvironmentRecord(mv);
        mv.swap();
        BindingInitialisationWithEnvironment(binding, mv);

        return Completion.Normal;
    }

    /**
     * 13.9.2 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ReturnStatement node, StatementVisitor mv) {
        Expression expr = node.getExpression();
        if (expr != null) {
            // expression as value to ensure tail-call nodes set contains the value node
            Expression expression = expr.asValue();
            mv.enterTailCallPosition(expression);
            expressionBoxedValue(expression, mv);
            mv.exitTailCallPosition();
        } else {
            mv.loadUndefined();
        }
        mv.returnCompletion();

        return Completion.Return;
    }

    @Override
    public Completion visit(StatementListMethod node, StatementVisitor mv) {
        codegen.compile(node, mv);

        mv.lineInfo(0); // 0 = hint for stacktraces to omit this frame
        mv.loadExecutionContext();
        mv.loadCompletionValue();

        mv.invoke(codegen.methodDesc(node));

        if (mv.getCodeType() == StatementVisitor.CodeType.Function) {
            // TODO: only emit when `return` used in StatementListMethod
            Label noReturn = new Label();
            mv.dup();
            mv.ifnull(noReturn);
            mv.returnCompletion();
            mv.mark(noReturn);
            mv.pop();
        } else {
            mv.storeCompletionValue(ValType.Any);
        }

        return Completion.Normal; // TODO: return correct result
    }

    /**
     * 13.11.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(SwitchStatement node, StatementVisitor mv) {
        return node.accept(new SwitchStatementGenerator(codegen), mv);
    }

    /**
     * 13.13.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ThrowStatement node, StatementVisitor mv) {
        expressionBoxedValue(node.getExpression(), mv);
        mv.invoke(Methods.ScriptException_create);
        mv.athrow();

        return Completion.Throw;
    }

    /**
     * 13.14.5 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(TryStatement node, StatementVisitor mv) {
        boolean hasCatch = node.getCatchNode() != null || !node.getGuardedCatchNodes().isEmpty();
        if (hasCatch && node.getFinallyBlock() != null) {
            return visitTryCatchFinally(node, mv);
        } else if (hasCatch) {
            return visitTryCatch(node, mv);
        } else {
            return visitTryFinally(node, mv);
        }
    }

    /**
     * 13.14.5 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-catch-finally</code>
     */
    private Completion visitTryCatchFinally(TryStatement node, StatementVisitor mv) {
        BlockStatement tryBlock = node.getTryBlock();
        CatchNode catchNode = node.getCatchNode();
        List<GuardedCatchNode> guardedCatchNodes = node.getGuardedCatchNodes();
        BlockStatement finallyBlock = node.getFinallyBlock();
        assert catchNode != null || !guardedCatchNodes.isEmpty();
        assert finallyBlock != null;

        Label startCatchFinally = new Label();
        Label endCatch = new Label(), handlerCatch = new Label();
        Label endFinally = new Label(), handlerFinally = new Label();
        Label handlerCatchStackOverflow = new Label();
        Label handlerFinallyStackOverflow = new Label();
        Label noException = new Label();
        Label exceptionHandled = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(mv);

        Variable<Object> completion = mv.enterFinallyScoped();
        mv.mark(startCatchFinally);
        mv.enterWrapped();
        Completion tryResult = tryBlock.accept(this, mv);
        if (!tryResult.isAbrupt()) {
            mv.goTo(noException);
        }
        mv.exitWrapped();
        mv.mark(endCatch);

        // StackOverflowError -> ScriptException
        mv.catchHandler(handlerCatchStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_toInternalError);

        Completion catchResult;
        mv.catchHandler(handlerCatch, Types.ScriptException);
        restoreEnvironment(savedEnv, mv);
        mv.enterWrapped();
        if (!guardedCatchNodes.isEmpty()) {
            mv.enterVariableScope();
            Variable<ScriptException> exception = mv
                    .newVariable("exception", ScriptException.class);
            mv.enterCatchWithGuarded(node, new Label());

            mv.store(exception);
            Completion result = null;
            for (GuardedCatchNode guardedCatchNode : guardedCatchNodes) {
                mv.load(exception);
                Completion guardedResult = guardedCatchNode.accept(this, mv);
                result = result != null ? result.select(guardedResult) : guardedResult;
            }
            assert result != null;
            if (catchNode != null) {
                mv.load(exception);
                catchResult = catchNode.accept(this, mv);
            } else {
                mv.load(exception);
                mv.athrow();
                catchResult = Completion.Throw;
            }

            mv.mark(mv.catchWithGuardedLabel());
            mv.exitCatchWithGuarded(node);
            mv.exitVariableScope();

            catchResult = catchResult.select(result);
        } else {
            catchResult = catchNode.accept(this, mv);
        }
        if (!catchResult.isAbrupt()) {
            mv.goTo(noException);
        }
        mv.exitWrapped();
        mv.mark(endFinally);

        // restore temp abrupt targets
        List<TempLabel> tempLabels = mv.exitFinallyScoped();

        // various finally blocks (1 - 4)
        // (1) finally block for abrupt completions within 'try-catch'
        mv.enterVariableScope();
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);
        mv.catchHandler(handlerFinallyStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        mv.catchHandler(handlerFinally, Types.ScriptException);
        mv.store(throwable);
        restoreEnvironment(savedEnv, mv);
        mv.enterFinally();
        Completion finallyResult = finallyBlock.accept(this, mv);
        mv.exitFinally();
        if (!finallyResult.isAbrupt()) {
            mv.load(throwable);
            mv.athrow();
        }
        mv.exitVariableScope();

        // (2) finally block if 'try' did not complete abruptly
        // (3) finally block if 'catch' did not complete abruptly
        if (!tryResult.isAbrupt() || !catchResult.isAbrupt()) {
            mv.mark(noException);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            if (!finallyResult.isAbrupt()) {
                mv.goTo(exceptionHandled);
            }
        }

        // (4) finally blocks for other abrupt completion (return, break, continue)
        for (TempLabel temp : tempLabels) {
            mv.mark(temp);
            restoreEnvironment(savedEnv, mv);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            if (!finallyResult.isAbrupt()) {
                mv.goTo(temp, completion);
            }
        }

        mv.mark(exceptionHandled);

        mv.exitVariableScope();
        mv.tryCatch(startCatchFinally, endCatch, handlerCatch, Types.ScriptException);
        mv.tryCatch(startCatchFinally, endCatch, handlerCatchStackOverflow, Types.Error);
        mv.tryCatch(startCatchFinally, endFinally, handlerFinally, Types.ScriptException);
        mv.tryCatch(startCatchFinally, endFinally, handlerFinallyStackOverflow, Types.Error);

        return finallyResult.then(tryResult.select(catchResult));
    }

    /**
     * 13.14.5 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-catch</code>
     */
    private Completion visitTryCatch(TryStatement node, StatementVisitor mv) {
        BlockStatement tryBlock = node.getTryBlock();
        CatchNode catchNode = node.getCatchNode();
        List<GuardedCatchNode> guardedCatchNodes = node.getGuardedCatchNodes();
        assert catchNode != null || !guardedCatchNodes.isEmpty();
        assert node.getFinallyBlock() == null;

        Label startCatch = new Label(), endCatch = new Label(), handlerCatch = new Label();
        Label handlerCatchStackOverflow = new Label();
        Label exceptionHandled = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(mv);

        mv.mark(startCatch);
        mv.enterWrapped();
        Completion tryResult = tryBlock.accept(this, mv);
        if (!tryResult.isAbrupt()) {
            mv.goTo(exceptionHandled);
        }
        mv.exitWrapped();
        mv.mark(endCatch);

        // StackOverflowError -> ScriptException
        mv.catchHandler(handlerCatchStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_toInternalError);

        Completion catchResult;
        mv.catchHandler(handlerCatch, Types.ScriptException);
        restoreEnvironment(savedEnv, mv);
        if (!guardedCatchNodes.isEmpty()) {
            mv.enterVariableScope();
            Variable<ScriptException> exception = mv
                    .newVariable("exception", ScriptException.class);
            mv.enterCatchWithGuarded(node, new Label());

            mv.store(exception);
            Completion result = null;
            for (GuardedCatchNode guardedCatchNode : guardedCatchNodes) {
                mv.load(exception);
                Completion guardedResult = guardedCatchNode.accept(this, mv);
                result = result != null ? result.select(guardedResult) : guardedResult;
            }
            assert result != null;
            if (catchNode != null) {
                mv.load(exception);
                catchResult = catchNode.accept(this, mv);
            } else {
                mv.load(exception);
                mv.athrow();
                catchResult = Completion.Throw;
            }

            mv.mark(mv.catchWithGuardedLabel());
            mv.exitCatchWithGuarded(node);
            mv.exitVariableScope();

            catchResult = catchResult.select(result);
        } else {
            catchResult = catchNode.accept(this, mv);
        }
        mv.mark(exceptionHandled);

        mv.exitVariableScope();
        mv.tryCatch(startCatch, endCatch, handlerCatch, Types.ScriptException);
        mv.tryCatch(startCatch, endCatch, handlerCatchStackOverflow, Types.Error);

        return tryResult.select(catchResult);
    }

    /**
     * 13.14.5 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-finally</code>
     */
    private Completion visitTryFinally(TryStatement node, StatementVisitor mv) {
        BlockStatement tryBlock = node.getTryBlock();
        BlockStatement finallyBlock = node.getFinallyBlock();
        assert node.getCatchNode() == null && node.getGuardedCatchNodes().isEmpty();
        assert finallyBlock != null;

        Label startFinally = new Label(), endFinally = new Label(), handlerFinally = new Label();
        Label handlerFinallyStackOverflow = new Label();
        Label noException = new Label();
        Label exceptionHandled = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(mv);

        Variable<Object> completion = mv.enterFinallyScoped();
        mv.mark(startFinally);
        mv.enterWrapped();
        Completion tryResult = tryBlock.accept(this, mv);
        if (!tryResult.isAbrupt()) {
            mv.goTo(noException);
        }
        mv.exitWrapped();
        mv.mark(endFinally);

        // restore temp abrupt targets
        List<TempLabel> tempLabels = mv.exitFinallyScoped();

        // various finally blocks (1 - 3)
        // (1) finally block for abrupt completions within 'try-catch'
        mv.enterVariableScope();
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);
        mv.catchHandler(handlerFinallyStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        mv.catchHandler(handlerFinally, Types.ScriptException);
        mv.store(throwable);
        restoreEnvironment(savedEnv, mv);
        mv.enterFinally();
        Completion finallyResult = finallyBlock.accept(this, mv);
        mv.exitFinally();
        if (!finallyResult.isAbrupt()) {
            mv.load(throwable);
            mv.athrow();
        }
        mv.exitVariableScope();

        // (2) finally block if 'try' did not complete abruptly
        if (!tryResult.isAbrupt()) {
            mv.mark(noException);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            if (!finallyResult.isAbrupt()) {
                mv.goTo(exceptionHandled);
            }
        }

        // (3) finally blocks for other abrupt completion (return, break, continue)
        for (TempLabel temp : tempLabels) {
            mv.mark(temp);
            restoreEnvironment(savedEnv, mv);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            if (!finallyResult.isAbrupt()) {
                mv.goTo(temp, completion);
            }
        }

        mv.mark(exceptionHandled);

        mv.exitVariableScope();
        mv.tryCatch(startFinally, endFinally, handlerFinally, Types.ScriptException);
        mv.tryCatch(startFinally, endFinally, handlerFinallyStackOverflow, Types.Error);

        return finallyResult.then(tryResult);
    }

    /**
     * 13.14.4 Runtime Semantics: CatchClauseEvaluation
     */
    @Override
    public Completion visit(CatchNode node, StatementVisitor mv) {
        Binding catchParameter = node.getCatchParameter();
        BlockStatement catchBlock = node.getCatchBlock();

        // stack: [e] -> [ex]
        mv.invoke(Methods.ScriptException_getValue);

        // create new declarative lexical environment
        // stack: [ex] -> [ex, catchEnv]
        mv.enterScope(node);
        newDeclarativeEnvironment(mv);
        {
            // stack: [ex, catchEnv] -> [catchEnv, ex, envRec]
            mv.dupX1();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            // [catchEnv, ex, envRec] -> [catchEnv, envRec, ex]
            for (String name : BoundNames(catchParameter)) {
                createMutableBinding(name, false, mv);
            }
            mv.swap();

            if (catchParameter instanceof BindingPattern) {
                ensureObjectOrThrow(ValType.Any, mv);
            }

            // stack: [catchEnv, envRec, ex] -> [catchEnv]
            BindingInitialisationWithEnvironment(catchParameter, mv);
        }
        // stack: [catchEnv] -> []
        pushLexicalEnvironment(mv);

        Completion result = catchBlock.accept(this, mv);

        // restore previous lexical environment
        if (!result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        mv.exitScope();

        return result;
    }

    /**
     * Extension: 'catch-if' statement
     */
    @Override
    public Completion visit(GuardedCatchNode node, StatementVisitor mv) {
        Binding catchParameter = node.getCatchParameter();
        BlockStatement catchBlock = node.getCatchBlock();

        // stack: [e] -> [ex]
        mv.invoke(Methods.ScriptException_getValue);

        // create new declarative lexical environment
        // stack: [ex] -> [ex, catchEnv]
        mv.enterScope(node);
        newDeclarativeEnvironment(mv);
        {
            // stack: [ex, catchEnv] -> [catchEnv, ex, envRec]
            mv.dupX1();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            // [catchEnv, ex, envRec] -> [catchEnv, envRec, ex]
            for (String name : BoundNames(catchParameter)) {
                createMutableBinding(name, false, mv);
            }
            mv.swap();

            if (catchParameter instanceof BindingPattern) {
                ensureObjectOrThrow(ValType.Any, mv);
            }

            // stack: [catchEnv, envRec, ex] -> [catchEnv]
            BindingInitialisationWithEnvironment(catchParameter, mv);
        }
        // stack: [catchEnv] -> []
        pushLexicalEnvironment(mv);

        Completion result;
        Label l0 = new Label();
        ToBoolean(expressionValue(node.getGuard(), mv), mv);
        mv.ifeq(l0);
        {
            result = catchBlock.accept(this, mv);

            // restore previous lexical environment and go to end of catch block
            if (!result.isAbrupt()) {
                popLexicalEnvironment(mv);
                mv.goTo(mv.catchWithGuardedLabel());
            }
        }
        mv.mark(l0);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        return result;
    }

    /**
     * 13.2.2.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(VariableDeclaration node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            ValType type = expressionBoxedValue(initialiser, mv);
            if (binding instanceof BindingPattern) {
                ensureObjectOrThrow(type, mv);
            }
            if (binding instanceof BindingIdentifier && IsAnonymousFunctionDefinition(initialiser)) {
                SetFunctionName(initialiser, ((BindingIdentifier) binding).getName(), mv);
            }
            BindingInitialisation(binding, mv);
        } else {
            assert binding instanceof BindingIdentifier;
        }
        return Completion.Normal;
    }

    /**
     * 13.2.2.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(VariableStatement node, StatementVisitor mv) {
        for (VariableDeclaration decl : node.getElements()) {
            decl.accept(this, mv);
        }
        return Completion.Normal;
    }

    /**
     * 13.0.3 Runtime Semantics: Evaluation<br>
     * 13.0.2 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.2.2 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(WhileStatement node, StatementVisitor mv) {
        Label lblNext = new Label();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);

        Completion result;
        mv.goTo(lblContinue);
        mv.mark(lblNext);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        if (lblContinue.isUsed()) {
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifne(lblNext);

        mv.mark(lblBreak);
        if (lblBreak.isUsed()) {
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }

        mv.exitVariableScope();

        return result.normal(lblContinue.isUsed() || lblBreak.isUsed()).select(Completion.Normal);
    }

    /**
     * 13.10.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(WithStatement node, StatementVisitor mv) {
        // with(<Expression>)
        ValType type = expressionValue(node.getExpression(), mv);

        // ToObject(<Expression>)
        ToObject(type, mv);

        // create new object lexical environment (withEnvironment-flag = true)
        mv.enterScope(node);
        newObjectEnvironment(mv, true);
        pushLexicalEnvironment(mv);

        Completion result = node.getStatement().accept(this, mv);

        // restore previous lexical environment
        if (!result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        mv.exitScope();

        return result;
    }
}
