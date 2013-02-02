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
import java.util.EnumSet;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.BreakableStatement.Abrupt;
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 *
 */
class StatementGenerator extends DefaultCodeGenerator<Void> {

    private int saveEnvironment(MethodGenerator mv) {
        int savedEnv = mv.newVariable(Types.LexicalEnvironment);
        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_getLexicalEnvironment);
        mv.store(savedEnv, Types.LexicalEnvironment);
        return savedEnv;
    }

    private void restoreEnvironment(MethodGenerator mv, int savedEnv) {
        mv.load(Register.ExecutionContext);
        mv.load(savedEnv, Types.LexicalEnvironment);
        mv.invokevirtual(Methods.ExecutionContext_restoreLexicalEnvironment);
    }

    /* ----------------------------------------------------------------------------------------- */

    private final CodeGenerator codegen;

    public StatementGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    @Override
    protected Void visit(Node node, MethodGenerator mv) {
        node.accept(codegen, mv);
        return null;
    }

    @Override
    public Void visit(StatementListItem node, MethodGenerator mv) {
        throw new IllegalStateException(String.format("statement/declaration-class: %s",
                node.getClass()));
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * 10.5.4 Block Declaration Instantiation
     */
    private void BlockDeclarationInstantiation(Collection<Declaration> declarations,
            MethodGenerator mv) {
        // stack: [env] -> [env, envRec]
        mv.dup();
        mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);

        for (Declaration d : declarations) {
            for (String dn : BoundNames(d)) {
                if (IsConstantDeclaration(d)) {
                    mv.dup();
                    mv.aconst(dn);
                    // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                    mv.invokeinterface(Methods.EnvironmentRecord_createImmutableBinding);
                } else {
                    mv.dup();
                    mv.aconst(dn);
                    mv.iconst(false);
                    // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                    mv.invokeinterface(Methods.EnvironmentRecord_createMutableBinding);
                }
            }
        }

        // stack: [env, envRec] -> [envRec, env]
        mv.swap();

        for (Declaration d : declarations) {
            if (d instanceof FunctionDeclaration) {
                FunctionDeclaration f = (FunctionDeclaration) d;
                codegen.compile(f);
                String fn = BoundName(f);

                // stack: [envRec, env] -> [envRec, env, envRec, realm, env, fd]
                mv.dup2();
                mv.load(Register.Realm);
                mv.swap();
                mv.invokestatic(codegen.getClassName(), codegen.methodName(f) + "_rti",
                        Type.getMethodDescriptor(Types.RuntimeInfo$Function));

                // stack: [envRec, env, envRec, realm, env, fd] -> [envRec, env, envRec, fo]
                mv.invokestatic(Methods.OrdinaryFunction_InstantiateFunctionObject);

                // stack: [envRec, env, envRec, fn, fo] -> [envRec, env]
                mv.aconst(fn);
                mv.swap();
                mv.invokeinterface(Methods.EnvironmentRecord_initializeBinding);
            } else if (d instanceof GeneratorDeclaration) {
                // TODO: for now same rules as FunctionDeclaration
                GeneratorDeclaration f = (GeneratorDeclaration) d;
                codegen.compile(f);
                String fn = BoundName(f);

                // stack: [envRec, env] -> [envRec, env, envRec, realm, env, fd]
                mv.dup2();
                mv.load(Register.Realm);
                mv.swap();
                mv.invokestatic(codegen.getClassName(), codegen.methodName(f) + "_rti",
                        Type.getMethodDescriptor(Types.RuntimeInfo$Function));

                // stack: [envRec, env, envRec, realm, env, fd] -> [envRec, env, envRec, fo]
                mv.invokestatic(Methods.OrdinaryGenerator_InstantiateGeneratorObject);

                // stack: [envRec, env, envRec, fn, fo] -> [envRec, env]
                mv.aconst(fn);
                mv.swap();
                mv.invokeinterface(Methods.EnvironmentRecord_initializeBinding);
            }
        }

        // stack: [envRec, env] -> [env]
        mv.swap();
        mv.pop();
    }

    /* ----------------------------------------------------------------------------------------- */

    private static String BoundName(FunctionDeclaration f) {
        return f.getIdentifier().getName();
    }

    private static String BoundName(GeneratorDeclaration f) {
        return f.getIdentifier().getName();
    }

    @Override
    public Void visit(BlockStatement node, MethodGenerator mv) {
        if (node.getStatements().isEmpty()) {
            // Block : { }
            // -> Return NormalCompletion(empty)
            return null;
        }

        Collection<Declaration> declarations = LexicalDeclarations(node);
        if (!declarations.isEmpty()) {
            newDeclarativeEnvironment(mv);
            BlockDeclarationInstantiation(declarations, mv);
            pushLexicalEnvironment(mv);
        }

        List<StatementListItem> statements = node.getStatements();
        for (StatementListItem statement : statements) {
            statement.accept(this, mv);
        }

        if (!declarations.isEmpty()) {
            popLexicalEnvironment(mv);
        }

        return null;
    }

    @Override
    public Void visit(BreakStatement node, MethodGenerator mv) {
        mv.goTo(mv.breakLabel(node));
        return null;
    }

    @Override
    public Void visit(ClassDeclaration node, MethodGenerator mv) {
        codegen.ClassDefinitionEvaluation(node, null, mv);

        // stack: [lex, value] -> []
        getLexicalEnvironment(mv);
        mv.swap();
        codegen.BindingInitialisationWithEnvironment(node.getName(), mv);

        return null;
    }

    @Override
    public Void visit(ContinueStatement node, MethodGenerator mv) {
        mv.goTo(mv.continueLabel(node));
        return null;
    }

    @Override
    public Void visit(DebuggerStatement node, MethodGenerator mv) {
        // no debugging facility supported
        return null;
    }

    @Override
    public Void visit(DoWhileStatement node, MethodGenerator mv) {
        Label l0 = new Label();
        Label lblContinue = new Label(), lblBreak = new Label();

        // L1: <statement>
        // IFNE ToBoolean(<expr>) L1

        int savedEnv = -1;
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break) || abrupt.contains(Abrupt.Continue)) {
            savedEnv = saveEnvironment(mv);
        }

        mv.mark(l0);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        if (abrupt.contains(Abrupt.Continue)) {
            restoreEnvironment(mv, savedEnv);
        }
        ValType type = codegen.expression(node.getTest(), mv);
        invokeGetValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifne(l0);
        mv.mark(lblBreak);
        if (abrupt.contains(Abrupt.Break)) {
            restoreEnvironment(mv, savedEnv);
        }
        if (savedEnv != -1) {
            mv.freeVariable(savedEnv);
        }

        return null;
    }

    @Override
    public Void visit(EmptyStatement node, MethodGenerator mv) {
        // nothing to do!
        return null;
    }

    @Override
    public Void visit(ExpressionStatement node, MethodGenerator mv) {
        Expression expr = node.getExpression();
        expr.accept(this, mv);
        invokeGetValue(expr, mv);
        if (mv.isCompletionValue()) {
            mv.checkcast(Types.Object);
            mv.store(Register.CompletionValue);
        } else {
            mv.pop();
        }
        return null;
    }

    private enum IterationKind {
        Enumerate, Iterate
    }

    @Override
    public Void visit(ForInStatement node, MethodGenerator mv) {
        visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.Enumerate, mv);
        return null;
    }

    @Override
    public Void visit(ForOfStatement node, MethodGenerator mv) {
        visitForInOfLoop(node, node.getExpression(), node.getHead(), node.getStatement(),
                IterationKind.Iterate, mv);
        return null;
    }

    private void visitForInOfLoop(IterationStatement node, Expression expr, Node lhs,
            Statement stmt, IterationKind iterationKind, MethodGenerator mv) {
        Label lblContinue = new Label(), lblBreak = new Label();
        Label loopstart = new Label();

        int savedEnv = -1;
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break) || abrupt.contains(Abrupt.Continue)) {
            savedEnv = saveEnvironment(mv);
        }

        // FIXME: spec bug (For In/Of Expression Evaluation called with 'statement')
        // Runtime Semantics: For In/Of Expression Evaluation Abstract Operation
        expr.accept(this, mv);
        invokeGetValue(expr, mv);

        mv.dup();
        isUndefinedOrNull(mv);
        mv.ifeq(loopstart);
        mv.pop();
        mv.goTo(lblBreak);
        mv.mark(loopstart);
        mv.load(Register.Realm);
        if (iterationKind == IterationKind.Enumerate) {
            mv.invokestatic(Methods.ScriptRuntime_enumerate);
        } else {
            assert iterationKind == IterationKind.Iterate;
            mv.invokestatic(Methods.ScriptRuntime_iterate);
        }

        int var = mv.newVariable(Types.Iterator);
        mv.store(var, Types.Iterator);

        mv.mark(lblContinue);
        if (abrupt.contains(Abrupt.Continue)) {
            restoreEnvironment(mv, savedEnv);
        }
        mv.load(var, Types.Iterator);
        mv.invokeinterface(Methods.Iterator_hasNext);
        mv.ifeq(lblBreak);
        mv.load(var, Types.Iterator);
        mv.invokeinterface(Methods.Iterator_next);

        if (lhs instanceof Expression) {
            assert lhs instanceof LeftHandSideExpression;
            if (lhs instanceof AssignmentPattern) {
                ToObject(ValType.Any, mv);
                codegen.DestructuringAssignment((AssignmentPattern) lhs, mv);
            } else {
                lhs.accept(this, mv);
                mv.swap();
                PutValue(mv);
            }
        } else if (lhs instanceof VariableStatement) {
            VariableDeclaration varDecl = ((VariableStatement) lhs).getElements().get(0);
            Binding binding = varDecl.getBinding();
            codegen.BindingInitialisation(binding, mv);
        } else {
            assert lhs instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) lhs;
            assert lexDecl.getElements().size() == 1;
            LexicalBinding lexicalBinding = lexDecl.getElements().get(0);

            // create new declarative lexical environment
            // stack: [nextValue] -> [nextValue, iterEnv]
            newDeclarativeEnvironment(mv);
            {
                // stack: [nextValue, iterEnv] -> [iterEnv, iterEnv, nextValue, envRec]
                mv.dupX1();
                mv.dupX1();
                mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);

                // stack: [iterEnv, iterEnv, nextValue, envRec] -> [iterEnv, iterEnv, nextValue]
                for (String name : BoundNames(lexicalBinding.getBinding())) {
                    if (IsConstantDeclaration(lexDecl)) {
                        mv.dup();
                        mv.aconst(name);
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                        mv.invokeinterface(Methods.EnvironmentRecord_createImmutableBinding);
                    } else {
                        mv.dup();
                        mv.aconst(name);
                        mv.iconst(false);
                        // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                        mv.invokeinterface(Methods.EnvironmentRecord_createMutableBinding);
                    }

                }
                mv.pop();

                // FIXME: spec bug (missing ToObject() call?)

                // stack: [iterEnv, iterEnv, nextValue] -> [iterEnv]
                codegen.BindingInitialisationWithEnvironment(lexicalBinding.getBinding(), mv);
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
        }

        mv.goTo(lblContinue);
        mv.mark(lblBreak);
        mv.freeVariable(var);
        if (abrupt.contains(Abrupt.Break)) {
            restoreEnvironment(mv, savedEnv);
        }
        if (savedEnv != -1) {
            mv.freeVariable(savedEnv);
        }
    }

    @Override
    public Void visit(ForStatement node, MethodGenerator mv) {
        Node head = node.getHead();
        if (head == null) {
            // empty
        } else if (head instanceof Expression) {
            head.accept(this, mv);
            invokeGetValue((Expression) head, mv);
            mv.pop();
        } else if (head instanceof VariableStatement) {
            head.accept(this, mv);
        } else {
            assert head instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) head;

            newDeclarativeEnvironment(mv);
            {
                // stack: [loopEnv] -> [loopEnv, envRec]
                mv.dup();
                mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);

                boolean isConst = IsConstantDeclaration(lexDecl);
                for (String dn : BoundNames(lexDecl)) {
                    if (isConst) {
                        mv.dup();
                        mv.aconst(dn);
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `loopEnv`)
                        mv.invokeinterface(Methods.EnvironmentRecord_createImmutableBinding);
                    } else {
                        mv.dup();
                        mv.aconst(dn);
                        mv.iconst(false);
                        // FIXME: spec bug (CreateMutableBinding concrete method of `loopEnv`)
                        mv.invokeinterface(Methods.EnvironmentRecord_createMutableBinding);
                    }
                }
                mv.pop();
            }
            pushLexicalEnvironment(mv);

            lexDecl.accept(this, mv);
        }

        int savedEnv = -1;
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break) || abrupt.contains(Abrupt.Continue)) {
            savedEnv = saveEnvironment(mv);
        }

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
        if (abrupt.contains(Abrupt.Continue)) {
            restoreEnvironment(mv, savedEnv);
        }
        if (node.getStep() != null) {
            node.getStep().accept(this, mv);
            invokeGetValue(node.getStep(), mv);
            mv.pop();
        }
        mv.mark(lblTest);
        if (node.getTest() != null) {
            ValType type = codegen.expression(node.getTest(), mv);
            invokeGetValue(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblStmt);
        } else {
            mv.goTo(lblStmt);
        }
        mv.mark(lblBreak);
        if (abrupt.contains(Abrupt.Break)) {
            restoreEnvironment(mv, savedEnv);
        }
        if (savedEnv != -1) {
            mv.freeVariable(savedEnv);
        }

        if (head instanceof LexicalDeclaration) {
            popLexicalEnvironment(mv);
        }

        return null;
    }

    @Override
    public Void visit(FunctionDeclaration node, MethodGenerator mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> FunctionDeclaration
        /* return NormalCompletion(empty) */

        return null;
    }

    @Override
    public Void visit(GeneratorDeclaration node, MethodGenerator mv) {
        codegen.compile(node);

        // Runtime Semantics: Evaluation -> GeneratorDeclaration
        /* return NormalCompletion(empty) */

        return null;
    }

    @Override
    public Void visit(IfStatement node, MethodGenerator mv) {
        Label l0 = new Label(), l1 = new Label();

        ValType type = codegen.expression(node.getTest(), mv);
        invokeGetValue(node.getTest(), mv);
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
    public Void visit(LabelledStatement node, MethodGenerator mv) {
        int savedEnv = -1;
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break)) {
            savedEnv = saveEnvironment(mv);
        }

        Label label = new Label();
        mv.enterLabelled(node, label);
        node.getStatement().accept(this, mv);
        mv.exitLabelled(node);
        mv.mark(label);

        if (abrupt.contains(Abrupt.Break)) {
            restoreEnvironment(mv, savedEnv);
        }
        if (savedEnv != -1) {
            mv.freeVariable(savedEnv);
        }

        return null;
    }

    @Override
    public Void visit(LexicalDeclaration node, MethodGenerator mv) {
        for (LexicalBinding binding : node.getElements()) {
            binding.accept(this, mv);
        }

        return null;
    }

    @Override
    public Void visit(LexicalBinding node, MethodGenerator mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            initialiser.accept(this, mv);
            invokeGetValue(initialiser, mv);
            if (binding instanceof BindingPattern) {
                // ToObject(...)
                ToObject(ValType.Any, mv);
            }
        } else {
            assert binding instanceof BindingIdentifier;
            mv.getstatic(Fields.Undefined_UNDEFINED);
        }

        mv.load(Register.ExecutionContext);
        mv.invokevirtual(Methods.ExecutionContext_getLexicalEnvironment);
        mv.swap();

        codegen.BindingInitialisationWithEnvironment(binding, mv);

        return null;
    }

    @Override
    public Void visit(ReturnStatement node, MethodGenerator mv) {
        Expression expr = node.getExpression();
        if (expr != null) {
            if (!mv.isWrapped()) {
                tailCall(expr, mv);
            }
            expr.accept(this, mv);
            invokeGetValue(expr, mv);
        } else {
            mv.getstatic(Fields.Undefined_UNDEFINED);
        }
        mv.store(Register.CompletionValue);
        mv.goTo(mv.returnLabel());
        return null;
    }

    @Override
    public Void visit(SwitchClause node, MethodGenerator mv) {
        // see SwitchStatement
        throw new IllegalStateException();
    }

    @Override
    public Void visit(SwitchStatement node, MethodGenerator mv) {
        Label defaultClause = null;
        Label lblBreak = new Label();
        List<SwitchClause> clauses = node.getClauses();

        int savedEnv = -1;
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break)) {
            savedEnv = saveEnvironment(mv);
        }

        // stack -> switchValue
        node.getExpression().accept(this, mv);
        invokeGetValue(node.getExpression(), mv);

        Collection<Declaration> declarations = LexicalDeclarations(node);
        if (!declarations.isEmpty()) {
            newDeclarativeEnvironment(mv);
            BlockDeclarationInstantiation(declarations, mv);
            pushLexicalEnvironment(mv);
        }

        int index = 0;
        Label[] labels = new Label[clauses.size()];
        for (SwitchClause switchClause : clauses) {
            Label stmtLabel = labels[index++] = new Label();
            Expression expr = switchClause.getExpression();
            if (expr == null) {
                assert defaultClause == null;
                defaultClause = stmtLabel;
            } else {
                Label next = new Label();
                mv.dup();
                expr.accept(this, mv);
                invokeGetValue(expr, mv);
                mv.invokestatic(Methods.ScriptRuntime_strictEqualityComparison);
                mv.ifeq(next);
                mv.pop(); // remove dup'ed entry
                mv.goTo(stmtLabel);
                mv.mark(next);
            }
        }

        // pop switchValue
        mv.pop();

        if (defaultClause != null) {
            mv.goTo(defaultClause);
        } else {
            mv.goTo(lblBreak);
        }

        mv.enterBreakable(node, lblBreak);
        index = 0;
        for (SwitchClause switchClause : clauses) {
            Label stmtLabel = labels[index++];
            mv.mark(stmtLabel);
            for (StatementListItem stmt : switchClause.getStatements()) {
                stmt.accept(this, mv);
            }
        }
        mv.exitBreakable(node);
        mv.mark(lblBreak);

        if (!declarations.isEmpty()) {
            popLexicalEnvironment(mv);
        }

        if (abrupt.contains(Abrupt.Break)) {
            restoreEnvironment(mv, savedEnv);
        }
        if (savedEnv != -1) {
            mv.freeVariable(savedEnv);
        }

        return null;
    }

    @Override
    public Void visit(ThrowStatement node, MethodGenerator mv) {
        node.getExpression().accept(this, mv);
        invokeGetValue(node.getExpression(), mv);
        mv.invokestatic(Methods.ScriptRuntime_throw);
        mv.pop(); // explicit pop required
        return null;
    }

    @Override
    public Void visit(TryStatement node, MethodGenerator mv) {
        // NB: nop() instruction are inserted to ensure no empty blocks will be generated

        BlockStatement tryBlock = node.getTryBlock();
        BlockStatement catchBlock = node.getCatchBlock();
        Binding catchParameter = node.getCatchParameter();
        BlockStatement finallyBlock = node.getFinallyBlock();

        if (catchBlock != null && finallyBlock != null) {
            Label startCatch = new Label();
            Label endCatch = new Label(), handlerCatch = new Label();
            Label endFinally = new Label(), handlerFinally = new Label();
            Label noException = new Label();
            Label exceptionHandled = new Label();

            mv.enterFinallyScoped();

            int savedEnv = saveEnvironment(mv);
            mv.mark(startCatch);
            mv.enterWrapped();
            tryBlock.accept(this, mv);
            mv.exitWrapped();
            mv.nop();
            mv.mark(endCatch);
            mv.goTo(noException);

            mv.mark(handlerCatch);
            restoreEnvironment(mv, savedEnv);
            mv.enterWrapped();
            catchClauseEvaluation(catchBlock, catchParameter, mv);
            mv.exitWrapped();
            mv.mark(endFinally);

            // restore temp abrupt targets
            List<Label> tempLabels = mv.exitFinallyScoped();

            // various finally blocks
            mv.enterFinally();
            finallyBlock.accept(this, mv);
            mv.exitFinally();
            mv.goTo(exceptionHandled);

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
            mv.visitTryCatchBlock(startCatch, endCatch, handlerCatch,
                    Types.ScriptException.getInternalName());
            mv.visitTryCatchBlock(startCatch, endFinally, handlerFinally, null);
        } else if (catchBlock != null) {
            Label startCatch = new Label(), endCatch = new Label(), handlerCatch = new Label();
            Label exceptionHandled = new Label();

            int savedEnv = saveEnvironment(mv);
            mv.mark(startCatch);
            mv.enterWrapped();
            tryBlock.accept(this, mv);
            mv.exitWrapped();
            mv.nop();
            mv.mark(endCatch);
            mv.goTo(exceptionHandled);
            mv.mark(handlerCatch);
            restoreEnvironment(mv, savedEnv);
            catchClauseEvaluation(catchBlock, catchParameter, mv);
            mv.mark(exceptionHandled);

            mv.freeVariable(savedEnv);
            mv.visitTryCatchBlock(startCatch, endCatch, handlerCatch,
                    Types.ScriptException.getInternalName());
        } else {
            assert finallyBlock != null;
            Label startFinally = new Label(), endFinally = new Label(), handlerFinally = new Label();
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
            mv.visitTryCatchBlock(startFinally, endFinally, handlerFinally, null);
        }

        return null;
    }

    private void catchClauseEvaluation(BlockStatement catchBlock, Binding catchParameter,
            MethodGenerator mv) {
        mv.invokevirtual(Methods.ScriptException_getValue);

        // create new declarative lexical environment
        // stack: [ex] -> [ex, catchEnv]
        newDeclarativeEnvironment(mv);
        {
            // stack: [ex, catchEnv] -> [catchEnv, catchEnv, ex, envRec]
            mv.dupX1();
            mv.dupX1();
            mv.invokevirtual(Methods.LexicalEnvironment_getEnvRec);

            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            // [catchEnv, catchEnv, ex, envRec] -> [catchEnv, catchEnv, ex]
            for (String name : BoundNames(catchParameter)) {
                mv.dup();
                mv.aconst(name);
                mv.iconst(false);
                mv.invokeinterface(Methods.EnvironmentRecord_createMutableBinding);
            }
            mv.pop();

            if (catchParameter instanceof BindingPattern) {
                // ToObject(...)
                ToObject(ValType.Any, mv);
            }

            // stack: [catchEnv, catchEnv, ex] -> [catchEnv]
            codegen.BindingInitialisationWithEnvironment(catchParameter, mv);
        }
        // stack: [catchEnv] -> []
        pushLexicalEnvironment(mv);

        catchBlock.accept(this, mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
    }

    @Override
    public Void visit(VariableDeclaration node, MethodGenerator mv) {
        Binding binding = node.getBinding();
        Expression initialiser = node.getInitialiser();
        if (initialiser != null) {
            initialiser.accept(this, mv);
            invokeGetValue(initialiser, mv);
            if (binding instanceof BindingPattern) {
                // ToObject(...)
                ToObject(ValType.Any, mv);
            }
            codegen.BindingInitialisation(binding, mv);
        } else {
            assert binding instanceof BindingIdentifier;
        }
        return null;
    }

    @Override
    public Void visit(VariableStatement node, MethodGenerator mv) {
        for (VariableDeclaration decl : node.getElements()) {
            decl.accept(this, mv);
        }
        return null;
    }

    @Override
    public Void visit(WhileStatement node, MethodGenerator mv) {
        Label lblNext = new Label();
        Label lblContinue = new Label(), lblBreak = new Label();

        int savedEnv = -1;
        EnumSet<Abrupt> abrupt = node.getAbrupt();
        if (abrupt.contains(Abrupt.Break) || abrupt.contains(Abrupt.Continue)) {
            savedEnv = saveEnvironment(mv);
        }

        mv.goTo(lblContinue);
        mv.mark(lblNext);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }
        mv.mark(lblContinue);
        if (abrupt.contains(Abrupt.Continue)) {
            restoreEnvironment(mv, savedEnv);
        }
        ValType type = codegen.expression(node.getTest(), mv);
        invokeGetValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifne(lblNext);
        mv.mark(lblBreak);
        if (abrupt.contains(Abrupt.Break)) {
            restoreEnvironment(mv, savedEnv);
        }
        if (savedEnv != -1) {
            mv.freeVariable(savedEnv);
        }

        return null;
    }

    @Override
    public Void visit(WithStatement node, MethodGenerator mv) {
        // with(<Expression>)
        node.getExpression().accept(this, mv);
        invokeGetValue(node.getExpression(), mv);

        // ToObject(<Expression>)
        ToObject(ValType.Any, mv);

        // create new object lexical environment (withEnvironment-flag = true)
        newObjectEnvironment(mv, true); // withEnvironment-flag = true
        pushLexicalEnvironment(mv);

        node.getStatement().accept(this, mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);

        return null;
    }
}
