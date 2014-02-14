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

        /**
         * <pre>
         * then :: Completion -> Completion -> Completion
         * then a b = case (a, b) of
         *              (Normal, _) -> b
         *              _ -> a
         * </pre>
         */
        Completion then(Completion next) {
            return this != Normal ? this : next;
        }

        /**
         * <pre>
         * select :: Completion -> Completion -> Completion
         * select a b = case (a, b) of
         *                (Normal, _) -> Normal
         *                (_, Normal) -> Normal
         *                _ | a == b -> a
         *                _ -> Abrupt
         * </pre>
         */
        Completion select(Completion other) {
            return this == Normal || other == Normal ? Normal : this == other ? this : Abrupt;
        }

        /**
         * <pre>
         * normal :: Completion -> Bool -> Completion
         * normal a b = case (a, b) of
         *                (_, True) -> Normal
         *                _ -> a
         * </pre>
         */
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
    private void PutValue(LeftHandSideExpression node, ValType type, StatementVisitor mv) {
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

        /* steps 1-4 */
        boolean hasDeclarations = !LexicalDeclarations(node).isEmpty();
        if (hasDeclarations) {
            newDeclarativeEnvironment(mv);
            new BlockDeclarationInstantiationGenerator(codegen).generate(node, mv);
            pushLexicalEnvironment(mv);
        }

        /* step 5 */
        mv.enterScope(node);
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
        mv.exitScope();

        /* step 6 */
        if (hasDeclarations && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }

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

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);

        /* step 1 (not applicable) */
        /* step 2 (repeat loop) */
        mv.mark(lblNext);

        /* steps 2a-2c */
        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 2c (abrupt completion - continue) */
        if (lblContinue.isUsed()) {
            mv.mark(lblContinue);
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        /* steps 2d-2g */
        if (!result.isAbrupt() || lblContinue.isUsed()) {
            ValType type = expressionValue(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblNext);
        }

        /* step 2c (abrupt completion - break) */
        if (lblBreak.isUsed()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 2c, 2f, 2g */
        return result.normal(lblContinue.isUsed() || lblBreak.isUsed());
    }

    /**
     * 13.3.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(EmptyStatement node, StatementVisitor mv) {
        /* step 1 */
        return Completion.Normal;
    }

    /**
     * 13.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExpressionStatement node, StatementVisitor mv) {
        /* steps 1-3 */
        ValType type = expressionValue(node.getExpression(), mv);

        /* step 4 */
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
     * 13.6.4.5 Runtime Semantics: LabelledEvaluation
     */
    private <FORSTATEMENT extends IterationStatement & ScopedNode> Completion visitForInOfLoop(
            FORSTATEMENT node, Expression expr, Node lhs, Statement stmt,
            IterationKind iterationKind, StatementVisitor mv) {
        Label lblFail = new Label();

        /* steps 1-2 */
        ValType type = ForInOfExpressionEvaluation(expr, iterationKind, lblFail, mv);

        /* step 3 */
        Completion result = ForInOfBodyEvaluation(node, lhs, stmt, mv);

        if (type != ValType.Object) {
            mv.mark(lblFail);
        }
        return result;
    }

    /**
     * 13.6.4.6 Runtime Semantics: ForIn/OfExpressionEvaluation Abstract Operation
     * <p>
     * stack: [] -> [Iterator]
     */
    private ValType ForInOfExpressionEvaluation(Expression expr, IterationKind iterationKind,
            Label lblFail, StatementVisitor mv) {
        /* steps 1-3 */
        ValType type = expressionValue(expr, mv);

        /* step 4 */
        if (type != ValType.Object) {
            mv.toBoxed(type);
            Label loopstart = new Label();
            mv.dup();
            isUndefinedOrNull(mv);
            mv.ifeq(loopstart);
            mv.pop();
            mv.goTo(lblFail);
            mv.mark(loopstart);
        }

        /* steps 5-9 */
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

        return type;
    }

    /**
     * 13.6.4.7 Runtime Semantics: ForIn/OfBodyEvaluation
     * <p>
     * stack: [Iterator] -> []
     */
    private <FORSTATEMENT extends IterationStatement & ScopedNode> Completion ForInOfBodyEvaluation(
            FORSTATEMENT node, Node lhs, Statement stmt, StatementVisitor mv) {
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();
        Label loopbody = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);
        @SuppressWarnings("rawtypes")
        Variable<Iterator> iter = mv.newVariable("iter", Iterator.class);

        // stack: [Iterator] -> []
        mv.store(iter);

        /* steps 1-2 (not applicable) */
        /* step 3 (repeat loop) */
        mv.goToAndSetStack(lblContinue);
        mv.mark(loopbody);

        /* steps 3d-3e */
        mv.load(iter);
        mv.invoke(Methods.Iterator_next);

        /* steps 3f-3h */
        if (lhs instanceof Expression) {
            /* step 3f.i */
            assert lhs instanceof LeftHandSideExpression;
            if (!(lhs instanceof AssignmentPattern)) {
                /* step 3f.ii */
                ValType lhsType = expression((LeftHandSideExpression) lhs, mv);
                mv.swap();
                PutValue((LeftHandSideExpression) lhs, lhsType, mv);
            } else {
                /* step 3f.iii */
                ensureObjectOrThrow(ValType.Any, mv);
                DestructuringAssignment((AssignmentPattern) lhs, mv);
            }
        } else if (lhs instanceof VariableStatement) {
            /* step 3g */
            VariableDeclaration varDecl = ((VariableStatement) lhs).getElements().get(0);
            Binding binding = varDecl.getBinding();
            // 12.1.4.2.2 Runtime Semantics: BindingInitialisation :: ForBinding
            if (binding instanceof BindingPattern) {
                ensureObjectOrThrow(ValType.Any, mv);
            }
            BindingInitialisation(binding, mv);
        } else {
            /* step 3h */
            assert lhs instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) lhs;
            assert lexDecl.getElements().size() == 1;
            LexicalBinding lexicalBinding = lexDecl.getElements().get(0);

            // create new declarative lexical environment
            // stack: [nextValue] -> [nextValue, iterEnv]
            newDeclarativeEnvironment(mv);
            {
                // 13.6.4.4 Runtime Semantics: BindingInstantiation :: ForDeclaration
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
            mv.enterScope(node);
        }

        /* step 3i */
        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = stmt.accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 3j */
        if (lhs instanceof LexicalDeclaration) {
            mv.exitScope();
            // restore previous lexical environment
            if (!result.isAbrupt()) {
                popLexicalEnvironment(mv);
            }
        }

        /* steps 3j-3k */
        mv.mark(lblContinue);
        if (lblContinue.isUsed()) {
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        /* steps 3a-3c */
        mv.load(iter);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(loopbody);

        /* steps 3j-3k */
        if (lblBreak.isUsed()) {
            mv.mark(lblBreak);
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
        boolean perIterationsLets = false;
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
            boolean isConst = IsConstantDeclaration(lexDecl);
            perIterationsLets = !isConst;

            newDeclarativeEnvironment(mv);
            {
                // stack: [loopEnv] -> [loopEnv, envRec]
                mv.dup();
                mv.invoke(Methods.LexicalEnvironment_getEnvRec);

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
            mv.enterScope(node);

            lexDecl.accept(this, mv);
        }

        // Runtime Semantics: ForBodyEvaluation
        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv;
        if (perIterationsLets) {
            savedEnv = mv.newVariable("savedEnv", LexicalEnvironment.class);
            CreatePerIterationEnvironment(savedEnv, mv);
        } else {
            savedEnv = saveEnvironment(node, mv);
        }

        Label lblTest = new Label(), lblStmt = new Label();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();

        Completion result;
        mv.goToAndSetStack(lblTest);
        mv.mark(lblStmt);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        if (lblContinue.isUsed()) {
            mv.mark(lblContinue);
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        if (perIterationsLets) {
            CreatePerIterationEnvironment(savedEnv, mv);
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

        if (lblBreak.isUsed()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        if (head instanceof LexicalDeclaration) {
            mv.exitScope();
            if (node.getTest() != null || lblBreak.isUsed()) {
                popLexicalEnvironment(mv);
            }
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
     * 13.6.3.4 Runtime Semantics: CreatePerIterationEnvironment
     */
    private void CreatePerIterationEnvironment(Variable<LexicalEnvironment> savedEnv,
            StatementVisitor mv) {
        // Create let-iteration environment
        cloneDeclarativeEnvironment(mv);
        mv.store(savedEnv);
        replaceLexicalEnvironment(savedEnv, mv);
    }

    /**
     * 14.1.17 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(FunctionDeclaration node, StatementVisitor mv) {
        codegen.compile(node);
        /* step 1 */
        return Completion.Normal;
    }

    /**
     * 14.4.14 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(GeneratorDeclaration node, StatementVisitor mv) {
        codegen.compile(node);
        /* step 1 */
        return Completion.Normal;
    }

    /**
     * 13.5.2 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(IfStatement node, StatementVisitor mv) {
        /* steps 1-3 */
        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        if (node.getOtherwise() != null) {
            // IfStatement : if ( Expression ) Statement else Statement
            Label l0 = new Label(), l1 = new Label();

            /* step 4 */
            mv.ifeq(l0);
            Completion resultThen = node.getThen().accept(this, mv);
            if (!resultThen.isAbrupt()) {
                mv.goTo(l1);
            }

            /* step 5 */
            mv.mark(l0);
            Completion resultOtherwise = node.getOtherwise().accept(this, mv);
            if (!resultThen.isAbrupt()) {
                mv.mark(l1);
            }

            /* steps 6-7 */
            return resultThen.select(resultOtherwise);
        } else {
            // IfStatement : if ( Expression ) Statement
            Label l0 = new Label();

            /* step 5 */
            mv.ifeq(l0);
            Completion resultThen = node.getThen().accept(this, mv);
            mv.mark(l0);

            /* steps 4, 6-7 */
            return resultThen.select(Completion.Normal);
        }
    }

    /**
     * 13.12.4 Runtime Semantics: Evaluation<br>
     * 13.12.3 Runtime Semantics: LabelledEvaluation<br>
     * 13.12.3.1 Runtime Semantics: LabelledStatementEvaluation(label, stmt, labelSet)
     */
    @Override
    public Completion visit(LabelledStatement node, StatementVisitor mv) {
        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(node, mv);

        /* steps 1-3 */
        BreakLabel label = new BreakLabel();
        mv.enterLabelled(node, label);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitLabelled(node);

        /* steps 4-5 */
        if (label.isUsed()) {
            mv.mark(label);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        /* step 6 */
        return result.normal(label.isUsed());
    }

    /**
     * 13.2.1.5 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(LexicalDeclaration node, StatementVisitor mv) {
        /* steps 1-2 */
        for (LexicalBinding binding : node.getElements()) {
            binding.accept(this, mv);
        }
        /* step 3 */
        return Completion.Normal;
    }

    /**
     * Extension: 'let' statement
     */
    @Override
    public Completion visit(LetStatement node, StatementVisitor mv) {
        // create new declarative lexical environment
        // stack: [] -> [env]
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

        mv.enterScope(node);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitScope();

        // restore previous lexical environment
        if (!result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }

        return result;
    }

    /**
     * 13.2.1.6 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(LexicalBinding node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser == null) {
            // LexicalBinding : BindingIdentifier
            assert binding instanceof BindingIdentifier;
            /* step 1 */
            getEnvironmentRecord(mv);
            /* step 2 */
            mv.loadUndefined();
            BindingInitialisationWithEnvironment(binding, mv);
            return Completion.Normal;
        }
        if (binding instanceof BindingIdentifier) {
            // LexicalBinding : BindingIdentifier Initialiser
            /* steps 1-3 */
            expressionBoxedValue(initialiser, mv);
            /* step 4 */
            if (IsAnonymousFunctionDefinition(initialiser)) {
                SetFunctionName(initialiser, ((BindingIdentifier) binding).getName(), mv);
            }
        } else {
            // LexicalBinding : BindingPattern Initialiser
            assert binding instanceof BindingPattern;
            /* steps 1-3 */
            ValType type = expressionBoxedValue(initialiser, mv);
            /* step 4 */
            ensureObjectOrThrow(type, mv);
        }
        /* step 5 */
        getEnvironmentRecord(mv);
        mv.swap();
        /* step 6 */
        BindingInitialisationWithEnvironment(binding, mv);
        return Completion.Normal;
    }

    /**
     * 13.9.2 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ReturnStatement node, StatementVisitor mv) {
        Expression expr = node.getExpression();
        if (expr == null) {
            // ReturnStatement : return ;
            /* step 1 */
            mv.loadUndefined();
        } else {
            // ReturnStatement : return Expression;
            /* steps 1-3 */
            // expression-as-value to ensure tail-call nodes set contains the value node
            Expression expression = expr.asValue();
            mv.enterTailCallPosition(expression);
            expressionBoxedValue(expression, mv);
            mv.exitTailCallPosition();
        }
        /* step 1/4 */
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
        /* steps 1-3 */
        expressionBoxedValue(node.getExpression(), mv);
        mv.invoke(Methods.ScriptException_create);

        /* step 4 */
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
        LocationLabel startCatchFinally = new LocationLabel();
        LocationLabel endCatch = new LocationLabel(), handlerCatch = new LocationLabel();
        LocationLabel endFinally = new LocationLabel(), handlerFinally = new LocationLabel();
        LocationLabel handlerCatchStackOverflow = new LocationLabel();
        LocationLabel handlerFinallyStackOverflow = new LocationLabel();
        Label noException = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(mv);
        Variable<Object> completion = mv.enterFinallyScoped();

        /* step 1 */
        // Emit try-block
        mv.mark(startCatchFinally);
        Completion tryResult = emitTryBlock(node, noException, mv);
        mv.mark(endCatch);

        /* steps 2-3 */
        // Emit catch-block
        Completion catchResult = emitCatchBlock(node, savedEnv, handlerCatch,
                handlerCatchStackOverflow, mv);
        if (!catchResult.isAbrupt()) {
            mv.goTo(noException);
        }
        mv.mark(endFinally);

        // Restore temporary abrupt targets
        List<TempLabel> tempLabels = mv.exitFinallyScoped();

        /* step 4 */
        // Emit finally-block
        Completion finallyResult = emitFinallyBlock(node, savedEnv, completion, tryResult,
                catchResult, handlerFinally, handlerFinallyStackOverflow, noException, tempLabels,
                mv);

        mv.exitVariableScope();
        mv.tryCatch(startCatchFinally, endCatch, handlerCatch, Types.ScriptException);
        mv.tryCatch(startCatchFinally, endCatch, handlerCatchStackOverflow, Types.Error);
        mv.tryCatch(startCatchFinally, endFinally, handlerFinally, Types.ScriptException);
        mv.tryCatch(startCatchFinally, endFinally, handlerFinallyStackOverflow, Types.Error);

        /* steps 5-6 */
        return finallyResult.then(tryResult.select(catchResult));
    }

    /**
     * 13.14.5 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-catch</code>
     */
    private Completion visitTryCatch(TryStatement node, StatementVisitor mv) {
        LocationLabel startCatch = new LocationLabel(), endCatch = new LocationLabel();
        LocationLabel handlerCatch = new LocationLabel();
        LocationLabel handlerCatchStackOverflow = new LocationLabel();
        Label exceptionHandled = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(mv);

        /* step 1 */
        // Emit try-block
        mv.mark(startCatch);
        Completion tryResult = emitTryBlock(node, exceptionHandled, mv);
        mv.mark(endCatch);

        /* step 3 */
        // Emit catch-block
        Completion catchResult = emitCatchBlock(node, savedEnv, handlerCatch,
                handlerCatchStackOverflow, mv);

        /* step 2 */
        if (!tryResult.isAbrupt()) {
            mv.mark(exceptionHandled);
        }

        mv.exitVariableScope();
        mv.tryCatch(startCatch, endCatch, handlerCatch, Types.ScriptException);
        mv.tryCatch(startCatch, endCatch, handlerCatchStackOverflow, Types.Error);

        /* steps 2-3 */
        return tryResult.select(catchResult);
    }

    /**
     * 13.14.5 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-finally</code>
     */
    private Completion visitTryFinally(TryStatement node, StatementVisitor mv) {
        LocationLabel startFinally = new LocationLabel(), endFinally = new LocationLabel();
        LocationLabel handlerFinally = new LocationLabel();
        LocationLabel handlerFinallyStackOverflow = new LocationLabel();
        Label noException = new Label();

        mv.enterVariableScope();
        Variable<LexicalEnvironment> savedEnv = saveEnvironment(mv);
        Variable<Object> completion = mv.enterFinallyScoped();

        /* step 1 */
        // Emit try-block
        mv.mark(startFinally);
        Completion tryResult = emitTryBlock(node, noException, mv);
        mv.mark(endFinally);

        // Restore temporary abrupt targets
        List<TempLabel> tempLabels = mv.exitFinallyScoped();

        /* step 2 */
        // Emit finally-block
        Completion finallyResult = emitFinallyBlock(node, savedEnv, completion, tryResult,
                Completion.Abrupt, handlerFinally, handlerFinallyStackOverflow, noException,
                tempLabels, mv);

        mv.exitVariableScope();
        mv.tryCatch(startFinally, endFinally, handlerFinally, Types.ScriptException);
        mv.tryCatch(startFinally, endFinally, handlerFinallyStackOverflow, Types.Error);

        /* steps 3-4 */
        return finallyResult.then(tryResult);
    }

    private Completion emitTryBlock(TryStatement node, Label noException, StatementVisitor mv) {
        mv.enterWrapped();
        Completion tryResult = node.getTryBlock().accept(this, mv);
        mv.exitWrapped();
        if (!tryResult.isAbrupt()) {
            mv.goTo(noException);
        }
        return tryResult;
    }

    private Completion emitCatchBlock(TryStatement node, Variable<LexicalEnvironment> savedEnv,
            LocationLabel handlerCatch, LocationLabel handlerCatchStackOverflow, StatementVisitor mv) {
        boolean isWrapped = node.getFinallyBlock() != null;
        CatchNode catchNode = node.getCatchNode();
        List<GuardedCatchNode> guardedCatchNodes = node.getGuardedCatchNodes();
        assert catchNode != null || !guardedCatchNodes.isEmpty();

        // StackOverflowError -> ScriptException
        mv.catchHandler(handlerCatchStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_toInternalError);

        mv.catchHandler(handlerCatch, Types.ScriptException);
        restoreEnvironment(savedEnv, mv);
        if (isWrapped) {
            mv.enterWrapped();
        }
        Completion catchResult;
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

            if (!result.isAbrupt()) {
                mv.mark(mv.catchWithGuardedLabel());
            }
            mv.exitCatchWithGuarded(node);
            mv.exitVariableScope();

            catchResult = catchResult.select(result);
        } else {
            catchResult = catchNode.accept(this, mv);
        }
        if (isWrapped) {
            mv.exitWrapped();
        }
        return catchResult;
    }

    private Completion emitFinallyBlock(TryStatement node, Variable<LexicalEnvironment> savedEnv,
            Variable<Object> completion, Completion tryResult, Completion catchResult,
            LocationLabel handlerFinally, LocationLabel handlerFinallyStackOverflow,
            Label noException, List<TempLabel> tempLabels, StatementVisitor mv) {
        BlockStatement finallyBlock = node.getFinallyBlock();
        assert finallyBlock != null;

        // various finally blocks (1 - 4)
        // (1) finally block for abrupt completions within 'try-catch'
        mv.enterVariableScope();
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);
        mv.catchHandler(handlerFinallyStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        mv.catchHandler(handlerFinally, Types.ScriptException);
        mv.store(throwable);
        restoreEnvironment(savedEnv, mv);
        Completion finallyResult = emitFinallyBlock(finallyBlock, mv);
        if (!finallyResult.isAbrupt()) {
            mv.load(throwable);
            mv.athrow();
        }
        mv.exitVariableScope();

        // (2) finally block if 'try' did not complete abruptly
        // (3) finally block if 'catch' did not complete abruptly
        Label exceptionHandled = null;
        if (!tryResult.isAbrupt() || !catchResult.isAbrupt()) {
            mv.mark(noException);
            emitFinallyBlock(finallyBlock, mv);
            if (!finallyResult.isAbrupt()) {
                exceptionHandled = new Label();
                mv.goTo(exceptionHandled);
            }
        }

        // (4) finally blocks for other abrupt completion (return, break, continue)
        for (TempLabel temp : tempLabels) {
            mv.mark(temp);
            restoreEnvironment(savedEnv, mv);
            emitFinallyBlock(finallyBlock, mv);
            if (!finallyResult.isAbrupt()) {
                mv.goTo(temp, completion);
            }
        }

        if (exceptionHandled != null) {
            mv.mark(exceptionHandled);
        }

        return finallyResult;
    }

    private Completion emitFinallyBlock(BlockStatement finallyBlock, StatementVisitor mv) {
        mv.enterFinally();
        Completion finallyResult = finallyBlock.accept(this, mv);
        mv.exitFinally();
        return finallyResult;
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

        /* step 1 (not applicable) */
        /* step 2 */
        // create new declarative lexical environment
        // stack: [ex] -> [ex, catchEnv]
        newDeclarativeEnvironment(mv);
        {
            // stack: [ex, catchEnv] -> [catchEnv, ex, envRec]
            mv.dupX1();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            /* step 3 */
            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            // [catchEnv, ex, envRec] -> [catchEnv, envRec, ex]
            for (String name : BoundNames(catchParameter)) {
                createMutableBinding(name, false, mv);
            }
            mv.swap();

            /* steps 4-5 */
            // 13.14.3 Runtime Semantics: BindingInitialisation :: CatchParameter
            if (catchParameter instanceof BindingPattern) {
                ensureObjectOrThrow(ValType.Any, mv);
            }
            // stack: [catchEnv, envRec, ex] -> [catchEnv]
            BindingInitialisationWithEnvironment(catchParameter, mv);
        }
        /* step 6 */
        // stack: [catchEnv] -> []
        pushLexicalEnvironment(mv);

        /* step 7 */
        mv.enterScope(node);
        Completion result = catchBlock.accept(this, mv);
        mv.exitScope();

        /* step 8 */
        // restore previous lexical environment
        if (!result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }

        /* step 9 */
        return result;
    }

    /**
     * Extension: 'catch-if' statement
     */
    @Override
    public Completion visit(GuardedCatchNode node, StatementVisitor mv) {
        Binding catchParameter = node.getCatchParameter();
        BlockStatement catchBlock = node.getCatchBlock();
        Label l0 = new Label();

        // stack: [e] -> [ex]
        mv.invoke(Methods.ScriptException_getValue);

        /* step 1 (not applicable) */
        /* step 2 */
        // create new declarative lexical environment
        // stack: [ex] -> [ex, catchEnv]
        newDeclarativeEnvironment(mv);
        {
            // stack: [ex, catchEnv] -> [catchEnv, ex, envRec]
            mv.dupX1();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            /* step 3 */
            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            // [catchEnv, ex, envRec] -> [catchEnv, envRec, ex]
            for (String name : BoundNames(catchParameter)) {
                createMutableBinding(name, false, mv);
            }
            mv.swap();

            /* steps 4-5 */
            // 13.14.3 Runtime Semantics: BindingInitialisation :: CatchParameter
            if (catchParameter instanceof BindingPattern) {
                ensureObjectOrThrow(ValType.Any, mv);
            }
            // stack: [catchEnv, envRec, ex] -> [catchEnv]
            BindingInitialisationWithEnvironment(catchParameter, mv);
        }
        /* step 6 */
        // stack: [catchEnv] -> []
        pushLexicalEnvironment(mv);

        /* step 7 */
        Completion result;
        mv.enterScope(node);
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
        mv.exitScope();

        /* step 8 */
        // restore previous lexical environment
        popLexicalEnvironment(mv);

        /* step 9 */
        return result;
    }

    /**
     * 13.2.2.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(VariableDeclaration node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser == null) {
            // VariableDeclaration : BindingIdentifier
            assert binding instanceof BindingIdentifier;
            /* step 1 */
            return Completion.Normal;
        }
        if (binding instanceof BindingIdentifier) {
            // VariableDeclaration : BindingIdentifier Initialiser
            /* steps 1-3 */
            expressionBoxedValue(initialiser, mv);
            /* step 4 */
            if (IsAnonymousFunctionDefinition(initialiser)) {
                SetFunctionName(initialiser, ((BindingIdentifier) binding).getName(), mv);
            }
        } else {
            // VariableDeclaration : BindingPattern Initialiser
            assert binding instanceof BindingPattern;
            /* steps 1-3 */
            ValType type = expressionBoxedValue(initialiser, mv);
            /* step 4 */
            ensureObjectOrThrow(type, mv);
        }
        /* step 5 */
        BindingInitialisation(binding, mv);
        return Completion.Normal;
    }

    /**
     * 13.2.2.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(VariableStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        for (VariableDeclaration decl : node.getElements()) {
            decl.accept(this, mv);
        }
        /* step 3 */
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

        /* step 1 (not applicable) */
        /* step 2 (repeat loop) */
        mv.goToAndSetStack(lblContinue);
        mv.mark(lblNext);

        /* steps 2e-2g */
        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 2g (abrupt completion - continue) */
        mv.mark(lblContinue);
        if (lblContinue.isUsed()) {
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        /* steps 2a-2d */
        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifne(lblNext);

        /* step 2g (abrupt completion - break) */
        if (lblBreak.isUsed()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 2c, 2d, 2g */
        return result.normal(lblContinue.isUsed() || lblBreak.isUsed()).select(Completion.Normal);
    }

    /**
     * 13.10.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(WithStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        ValType type = expressionValue(node.getExpression(), mv);

        /* steps 2-3 */
        ToObject(type, mv);

        /* steps 4-7 */
        // create new object lexical environment (withEnvironment-flag = true)
        newObjectEnvironment(mv, true);
        pushLexicalEnvironment(mv);

        /* step 8 */
        mv.enterScope(node);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitScope();

        /* step 9 */
        // restore previous lexical environment
        if (!result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }

        /* step 10 */
        return result;
    }
}
