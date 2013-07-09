/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsConstantDeclaration;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicalDeclarations;

import java.util.Collection;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.parser.Parser;

/**
 *
 */
class StatementGenerator extends DefaultCodeGenerator<Void, StatementVisitor> {
    private static class Fields {
        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private static class Methods {
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
        static final MethodDesc Reference_PutValue = MethodDesc.create(MethodType.Virtual,
                Types.Reference, "PutValue",
                Type.getMethodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptException
        static final MethodDesc ScriptException_getValue = MethodDesc.create(MethodType.Virtual,
                Types.ScriptException, "getValue", Type.getMethodType(Types.Object));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_enumerate = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "enumerate",
                Type.getMethodType(Types.Iterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_enumerateValues = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "enumerateValues",
                Type.getMethodType(Types.Iterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_iterate = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "iterate",
                Type.getMethodType(Types.Iterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_throw = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "_throw",
                Type.getMethodType(Types.ScriptException, Types.Object));

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
        mv.invoke(Methods.Reference_PutValue);
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

    @Override
    protected Void visit(Node node, StatementVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    @Override
    public Void visit(BlockStatement node, StatementVisitor mv) {
        if (node.getStatements().isEmpty()) {
            // Block : { }
            // -> Return NormalCompletion(empty)
            return null;
        }

        mv.enterScope(node);
        Collection<Declaration> declarations = LexicalDeclarations(node);
        if (!declarations.isEmpty()) {
            newDeclarativeEnvironment(mv);
            new BlockDeclarationInstantiationGenerator(codegen).generate(declarations, mv);
            pushLexicalEnvironment(mv);
        }

        List<StatementListItem> statements = node.getStatements();
        for (StatementListItem statement : statements) {
            statement.accept(this, mv);
        }

        if (!declarations.isEmpty()) {
            popLexicalEnvironment(mv);
        }
        mv.exitScope();

        return null;
    }

    @Override
    public Void visit(BreakStatement node, StatementVisitor mv) {
        mv.goTo(mv.breakLabel(node));
        return null;
    }

    @Override
    public Void visit(ClassDeclaration node, StatementVisitor mv) {
        ClassDefinitionEvaluation(node, null, mv);

        // stack: [lexEnv, value] -> []
        getEnvironmentRecord(mv);
        mv.swap();
        BindingInitialisationWithEnvironment(node.getName(), mv);

        return null;
    }

    @Override
    public Void visit(ContinueStatement node, StatementVisitor mv) {
        mv.goTo(mv.continueLabel(node));
        return null;
    }

    @Override
    public Void visit(DebuggerStatement node, StatementVisitor mv) {
        // no debugging facility supported
        return null;
    }

    @Override
    public Void visit(DoWhileStatement node, StatementVisitor mv) {
        Label lblNext = new Label();
        Label lblContinue = new Label(), lblBreak = new Label();

        // L1: <statement>
        // IFNE ToBoolean(<expr>) L1

        int savedEnv = saveEnvironment(node, mv);

        mv.mark(lblNext);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);

        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifne(lblNext);

        mv.mark(lblBreak);
        restoreEnvironment(node, Abrupt.Break, savedEnv, mv);

        freeVariable(savedEnv, mv);

        return null;
    }

    @Override
    public Void visit(EmptyStatement node, StatementVisitor mv) {
        // nothing to do!
        return null;
    }

    @Override
    public Void visit(ExpressionStatement node, StatementVisitor mv) {
        ValType type = expressionValue(node.getExpression(), mv);
        if (mv.isCompletionValue()) {
            mv.toBoxed(type);
            mv.storeCompletionValue();
        } else {
            mv.pop(type);
        }
        return null;
    }

    private enum IterationKind {
        Enumerate, Iterate, EnumerateValues
    }

    @Override
    public Void visit(ForEachStatement node, StatementVisitor mv) {
        visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.EnumerateValues, mv);
        return null;
    }

    @Override
    public Void visit(ForInStatement node, StatementVisitor mv) {
        visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.Enumerate, mv);
        return null;
    }

    @Override
    public Void visit(ForOfStatement node, StatementVisitor mv) {
        visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.Iterate, mv);
        return null;
    }

    private <FORSTATEMENT extends IterationStatement & ScopedNode> void visitForInOfLoop(
            FORSTATEMENT node, Expression expr, Node lhs, Statement stmt,
            IterationKind iterationKind, StatementVisitor mv) {
        Label lblContinue = new Label(), lblBreak = new Label();
        Label loopstart = new Label();

        int savedEnv = saveEnvironment(node, mv);

        // Runtime Semantics: For In/Of Expression Evaluation Abstract Operation
        ValType type = expressionValue(expr, mv);
        mv.toBoxed(type);

        mv.dup();
        isUndefinedOrNull(mv);
        mv.ifeq(loopstart);
        mv.pop();
        mv.goTo(lblBreak);
        mv.mark(loopstart);

        if ((iterationKind == IterationKind.Enumerate || iterationKind == IterationKind.EnumerateValues)
                && codegen.isEnabled(Parser.Option.LegacyGenerator)) {
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

        int var = mv.newVariable(Types.Iterator);
        mv.store(var, Types.Iterator);

        mv.mark(lblContinue);
        restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);

        mv.load(var, Types.Iterator);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifeq(lblBreak);
        mv.load(var, Types.Iterator);
        mv.invoke(Methods.Iterator_next);

        if (lhs instanceof Expression) {
            assert lhs instanceof LeftHandSideExpression;
            if (lhs instanceof AssignmentPattern) {
                ToObject(ValType.Any, mv);
                DestructuringAssignment((AssignmentPattern) lhs, mv);
            } else {
                ValType lhsType = expression((Expression) lhs, mv);
                mv.swap();
                PutValue((Expression) lhs, lhsType, mv);
            }
        } else if (lhs instanceof VariableStatement) {
            VariableDeclaration varDecl = ((VariableStatement) lhs).getElements().get(0);
            Binding binding = varDecl.getBinding();
            // FIXME: spec bug (missing ToObject() call?)
            if (binding instanceof BindingPattern) {
                ToObject(ValType.Any, mv);
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

                // FIXME: spec bug (missing ToObject() call?)
                if (lexicalBinding.getBinding() instanceof BindingPattern) {
                    ToObject(ValType.Any, mv);
                }

                // stack: [iterEnv, envRec, nextValue] -> [iterEnv]
                BindingInitialisationWithEnvironment(lexicalBinding.getBinding(), mv);
            }
            // stack: [iterEnv] -> []
            pushLexicalEnvironment(mv);
        }

        {
            mv.enterIteration(node, lblBreak, lblContinue);
            stmt.accept(this, mv);
            mv.exitIteration(node);
        }

        if (lhs instanceof LexicalDeclaration) {
            // restore previous lexical environment
            popLexicalEnvironment(mv);
            mv.exitScope();
        }

        mv.goTo(lblContinue);
        mv.mark(lblBreak);
        restoreEnvironment(node, Abrupt.Break, savedEnv, mv);

        mv.freeVariable(var);
        freeVariable(savedEnv, mv);
    }

    @Override
    public Void visit(ForStatement node, StatementVisitor mv) {
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

        int savedEnv = saveEnvironment(node, mv);

        Label lblTest = new Label(), lblStmt = new Label();
        Label lblContinue = new Label(), lblBreak = new Label();

        mv.goTo(lblTest);
        mv.mark(lblStmt);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);

        if (node.getStep() != null) {
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
        restoreEnvironment(node, Abrupt.Break, savedEnv, mv);

        freeVariable(savedEnv, mv);

        if (head instanceof LexicalDeclaration) {
            popLexicalEnvironment(mv);
            mv.exitScope();
        }

        return null;
    }

    @Override
    public Void visit(FunctionDeclaration node, StatementVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionDeclaration
        /* return NormalCompletion(empty) */

        return null;
    }

    @Override
    public Void visit(GeneratorDeclaration node, StatementVisitor mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> GeneratorDeclaration
        /* return NormalCompletion(empty) */

        return null;
    }

    @Override
    public Void visit(IfStatement node, StatementVisitor mv) {
        Label l0 = new Label(), l1 = new Label();

        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifeq(l0);
        node.getThen().accept(this, mv);
        if (node.getOtherwise() != null) {
            mv.goTo(l1);
            mv.mark(l0);
            node.getOtherwise().accept(this, mv);
            mv.mark(l1);
        } else {
            mv.mark(l0);
        }
        return null;
    }

    @Override
    public Void visit(LabelledStatement node, StatementVisitor mv) {
        int savedEnv = saveEnvironment(node, mv);

        Label label = new Label();
        mv.enterLabelled(node, label);
        node.getStatement().accept(this, mv);
        mv.exitLabelled(node);
        mv.mark(label);

        restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        freeVariable(savedEnv, mv);

        return null;
    }

    @Override
    public Void visit(LexicalDeclaration node, StatementVisitor mv) {
        for (LexicalBinding binding : node.getElements()) {
            binding.accept(this, mv);
        }

        return null;
    }

    @Override
    public Void visit(LetStatement node, StatementVisitor mv) {
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
                    ValType type = expressionValue(initialiser, mv);
                    if (binding.getBinding() instanceof BindingPattern) {
                        ToObject(type, mv);
                    } else {
                        mv.toBoxed(type);
                    }
                } else {
                    assert binding.getBinding() instanceof BindingIdentifier;
                    mv.get(Fields.Undefined_UNDEFINED);
                }

                // stack: [env, envRec, envRec, value] -> [env, envRec]
                BindingInitialisationWithEnvironment(binding.getBinding(), mv);
            }
            mv.pop();
        }
        // stack: [env] -> []
        pushLexicalEnvironment(mv);

        node.getStatement().accept(this, mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        return null;
    }

    @Override
    public Void visit(LexicalBinding node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            ValType type = expressionValue(initialiser, mv);
            if (binding instanceof BindingPattern) {
                // ToObject(...)
                ToObject(type, mv);
            } else {
                mv.toBoxed(type);
            }
        } else {
            assert binding instanceof BindingIdentifier;
            mv.get(Fields.Undefined_UNDEFINED);
        }

        getEnvironmentRecord(mv);
        mv.swap();
        BindingInitialisationWithEnvironment(binding, mv);

        return null;
    }

    @Override
    public Void visit(ReturnStatement node, StatementVisitor mv) {
        Expression expr = node.getExpression();
        if (expr != null) {
            if (!mv.isWrapped()) {
                tailCall(expr, mv);
            }
            ValType type = expressionValue(expr, mv);
            mv.toBoxed(type);
        } else {
            mv.get(Fields.Undefined_UNDEFINED);
        }
        mv.storeCompletionValue();
        mv.goTo(mv.returnLabel());
        return null;
    }

    @Override
    public Void visit(StatementListMethod node, StatementVisitor mv) {
        codegen.compile(node, mv);

        mv.loadExecutionContext();
        mv.loadCompletionValue();

        int r = -1;
        if (mv.getCodeType() == StatementVisitor.CodeType.Function) {
            r = mv.newVariable(Types.boolean_);
            mv.newarray(1, Type.BOOLEAN_TYPE);
            mv.dup();
            mv.store(r, Types.boolean_);
        } else {
            mv.aconst(null);
        }

        String desc = Type.getMethodDescriptor(Types.Object, Types.ExecutionContext, Types.Object,
                Types.boolean_);
        mv.invokestatic(codegen.getClassName(), codegen.methodName(node), desc);

        if (mv.getCodeType() == StatementVisitor.CodeType.Function) {
            mv.load(r, Types.boolean_);
            mv.freeVariable(r);
            mv.aload(0, Type.BOOLEAN_TYPE);

            Label noReturn = new Label();
            mv.ifeq(noReturn);
            mv.storeCompletionValue();
            mv.goTo(mv.returnLabel());
            mv.mark(noReturn);
        }

        if (mv.isCompletionValue()) {
            mv.storeCompletionValue();
        } else {
            mv.pop();
        }

        return null;
    }

    @Override
    public Void visit(SwitchStatement node, StatementVisitor mv) {
        node.accept(new SwitchStatementGenerator(codegen), mv);

        return null;
    }

    @Override
    public Void visit(ThrowStatement node, StatementVisitor mv) {
        ValType type = expressionValue(node.getExpression(), mv);
        mv.toBoxed(type);
        mv.invoke(Methods.ScriptRuntime_throw);
        mv.pop(); // explicit pop required
        return null;
    }

    @Override
    public Void visit(TryStatement node, StatementVisitor mv) {
        // NB: nop() instruction are inserted to ensure no empty blocks will be generated

        BlockStatement tryBlock = node.getTryBlock();
        CatchNode catchNode = node.getCatchNode();
        List<GuardedCatchNode> guardedCatchNodes = node.getGuardedCatchNodes();
        BlockStatement finallyBlock = node.getFinallyBlock();

        if ((catchNode != null || !guardedCatchNodes.isEmpty()) && finallyBlock != null) {
            Label startCatchFinally = new Label();
            Label endCatch = new Label(), handlerCatch = new Label();
            Label endFinally = new Label(), handlerFinally = new Label();
            Label handlerCatchStackOverflow = new Label();
            Label handlerFinallyStackOverflow = new Label();
            Label noException = new Label();
            Label exceptionHandled = new Label();

            mv.enterFinallyScoped();

            int savedEnv = saveEnvironment(mv);
            mv.mark(startCatchFinally);
            mv.enterWrapped();
            tryBlock.accept(this, mv);
            mv.exitWrapped();
            mv.nop();
            mv.mark(endCatch);
            mv.goTo(noException);

            // StackOverflowError -> ScriptException
            mv.mark(handlerCatchStackOverflow);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_toInternalError);

            mv.mark(handlerCatch);
            restoreEnvironment(mv, savedEnv);
            mv.enterWrapped();
            if (!guardedCatchNodes.isEmpty()) {
                mv.enterCatchWithGuarded(node, new Label());

                int var = mv.newVariable(Types.ScriptException);
                mv.store(var, Types.ScriptException);
                for (GuardedCatchNode guardedCatchNode : guardedCatchNodes) {
                    mv.load(var, Types.ScriptException);
                    guardedCatchNode.accept(this, mv);
                }
                if (catchNode != null) {
                    mv.load(var, Types.ScriptException);
                    catchNode.accept(this, mv);
                } else {
                    mv.load(var, Types.ScriptException);
                    mv.athrow();
                }

                mv.freeVariable(var);
                mv.mark(mv.catchWithGuardedLabel());
                mv.exitCatchWithGuarded(node);
            } else {
                catchNode.accept(this, mv);
            }
            mv.exitWrapped();
            mv.mark(endFinally);

            // restore temp abrupt targets
            List<Label> tempLabels = mv.exitFinallyScoped();

            // various finally blocks
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            mv.goTo(exceptionHandled);

            mv.mark(handlerFinallyStackOverflow);
            mv.mark(handlerFinally);
            int var = mv.newVariable(Types.Throwable);
            mv.store(var, Types.Throwable);
            restoreEnvironment(mv, savedEnv);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            mv.load(var, Types.Throwable);
            mv.athrow();
            mv.freeVariable(var);

            mv.mark(noException);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            mv.goTo(exceptionHandled);

            // abrupt completion (return, break, continue) finally blocks
            if (tempLabels != null) {
                assert tempLabels.size() % 2 == 0;
                for (int i = 0, size = tempLabels.size(); i < size; i += 2) {
                    Label actual = tempLabels.get(i);
                    Label temp = tempLabels.get(i + 1);

                    mv.mark(temp);
                    restoreEnvironment(mv, savedEnv);
                    mv.enterFinally();
                    finallyBlock.accept(this, mv);
                    mv.exitFinally();
                    mv.goTo(actual);
                }
            }

            mv.mark(exceptionHandled);

            mv.freeVariable(savedEnv);
            mv.visitTryCatchBlock(startCatchFinally, endCatch, handlerCatch,
                    Types.ScriptException.getInternalName());
            mv.visitTryCatchBlock(startCatchFinally, endCatch, handlerCatchStackOverflow,
                    Types.StackOverflowError.getInternalName());
            mv.visitTryCatchBlock(startCatchFinally, endFinally, handlerFinally,
                    Types.ScriptException.getInternalName());
            mv.visitTryCatchBlock(startCatchFinally, endFinally, handlerFinallyStackOverflow,
                    Types.StackOverflowError.getInternalName());
        } else if (catchNode != null || !guardedCatchNodes.isEmpty()) {
            Label startCatch = new Label(), endCatch = new Label(), handlerCatch = new Label();
            Label handlerCatchStackOverflow = new Label();
            Label exceptionHandled = new Label();

            int savedEnv = saveEnvironment(mv);
            mv.mark(startCatch);
            mv.enterWrapped();
            tryBlock.accept(this, mv);
            mv.exitWrapped();
            mv.nop();
            mv.mark(endCatch);
            mv.goTo(exceptionHandled);

            // StackOverflowError -> ScriptException
            mv.mark(handlerCatchStackOverflow);
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_toInternalError);

            mv.mark(handlerCatch);
            restoreEnvironment(mv, savedEnv);
            if (!guardedCatchNodes.isEmpty()) {
                mv.enterCatchWithGuarded(node, new Label());

                int var = mv.newVariable(Types.ScriptException);
                mv.store(var, Types.ScriptException);
                for (GuardedCatchNode guardedCatchNode : guardedCatchNodes) {
                    mv.load(var, Types.ScriptException);
                    guardedCatchNode.accept(this, mv);
                }
                if (catchNode != null) {
                    mv.load(var, Types.ScriptException);
                    catchNode.accept(this, mv);
                } else {
                    mv.load(var, Types.ScriptException);
                    mv.athrow();
                }

                mv.freeVariable(var);
                mv.mark(mv.catchWithGuardedLabel());
                mv.exitCatchWithGuarded(node);
            } else {
                catchNode.accept(this, mv);
            }
            mv.mark(exceptionHandled);

            mv.freeVariable(savedEnv);
            mv.visitTryCatchBlock(startCatch, endCatch, handlerCatch,
                    Types.ScriptException.getInternalName());
            mv.visitTryCatchBlock(startCatch, endCatch, handlerCatchStackOverflow,
                    Types.StackOverflowError.getInternalName());
        } else {
            assert finallyBlock != null;
            Label startFinally = new Label(), endFinally = new Label(), handlerFinally = new Label();
            Label handlerFinallyStackOverflow = new Label();
            Label noException = new Label();
            Label exceptionHandled = new Label();

            mv.enterFinallyScoped();

            int savedEnv = saveEnvironment(mv);
            mv.mark(startFinally);
            mv.enterWrapped();
            tryBlock.accept(this, mv);
            mv.exitWrapped();
            mv.nop();
            mv.mark(endFinally);
            mv.goTo(noException);

            // restore temp abrupt targets
            List<Label> tempLabels = mv.exitFinallyScoped();

            mv.mark(handlerFinallyStackOverflow);
            mv.mark(handlerFinally);
            int var = mv.newVariable(Types.Throwable);
            mv.store(var, Types.Throwable);
            restoreEnvironment(mv, savedEnv);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            mv.load(var, Types.Throwable);
            mv.athrow();
            mv.freeVariable(var);

            mv.mark(noException);
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            mv.goTo(exceptionHandled);

            // abrupt completion (return, break, continue) finally blocks
            if (tempLabels != null) {
                assert tempLabels.size() % 2 == 0;
                for (int i = 0, size = tempLabels.size(); i < size; i += 2) {
                    Label actual = tempLabels.get(i);
                    Label temp = tempLabels.get(i + 1);

                    mv.mark(temp);
                    restoreEnvironment(mv, savedEnv);
                    mv.enterFinally();
                    finallyBlock.accept(this, mv);
                    mv.exitFinally();
                    mv.goTo(actual);
                }
            }

            mv.mark(exceptionHandled);

            mv.freeVariable(savedEnv);
            mv.visitTryCatchBlock(startFinally, endFinally, handlerFinally,
                    Types.ScriptException.getInternalName());
            mv.visitTryCatchBlock(startFinally, endFinally, handlerFinallyStackOverflow,
                    Types.StackOverflowError.getInternalName());
        }

        return null;
    }

    @Override
    public Void visit(CatchNode node, StatementVisitor mv) {
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
                // ToObject(...)
                ToObject(ValType.Any, mv);
            }

            // stack: [catchEnv, envRec, ex] -> [catchEnv]
            BindingInitialisationWithEnvironment(catchParameter, mv);
        }
        // stack: [catchEnv] -> []
        pushLexicalEnvironment(mv);

        catchBlock.accept(this, mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        return null;
    }

    @Override
    public Void visit(GuardedCatchNode node, StatementVisitor mv) {
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
                // ToObject(...)
                ToObject(ValType.Any, mv);
            }

            // stack: [catchEnv, envRec, ex] -> [catchEnv]
            BindingInitialisationWithEnvironment(catchParameter, mv);
        }
        // stack: [catchEnv] -> []
        pushLexicalEnvironment(mv);

        Label l0 = new Label();
        ToBoolean(expressionValue(node.getGuard(), mv), mv);
        mv.ifeq(l0);
        {
            catchBlock.accept(this, mv);

            // restore previous lexical environment and go to end of catch block
            popLexicalEnvironment(mv);
            mv.goTo(mv.catchWithGuardedLabel());
        }
        mv.mark(l0);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        return null;
    }

    @Override
    public Void visit(VariableDeclaration node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            ValType type = expressionValue(initialiser, mv);
            if (binding instanceof BindingPattern) {
                // ToObject(...)
                ToObject(type, mv);
            } else {
                mv.toBoxed(type);
            }
            BindingInitialisation(binding, mv);
        } else {
            assert binding instanceof BindingIdentifier;
        }
        return null;
    }

    @Override
    public Void visit(VariableStatement node, StatementVisitor mv) {
        for (VariableDeclaration decl : node.getElements()) {
            decl.accept(this, mv);
        }
        return null;
    }

    @Override
    public Void visit(WhileStatement node, StatementVisitor mv) {
        Label lblNext = new Label();
        Label lblContinue = new Label(), lblBreak = new Label();

        int savedEnv = saveEnvironment(node, mv);

        mv.goTo(lblContinue);
        mv.mark(lblNext);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);

        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifne(lblNext);

        mv.mark(lblBreak);
        restoreEnvironment(node, Abrupt.Break, savedEnv, mv);

        freeVariable(savedEnv, mv);

        return null;
    }

    @Override
    public Void visit(WithStatement node, StatementVisitor mv) {
        // with(<Expression>)
        ValType type = expressionValue(node.getExpression(), mv);

        // ToObject(<Expression>)
        ToObject(type, mv);

        // create new object lexical environment (withEnvironment-flag = true)
        mv.enterScope(node);
        newObjectEnvironment(mv, true); // withEnvironment-flag = true
        pushLexicalEnvironment(mv);

        node.getStatement().accept(this, mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        return null;
    }
}
