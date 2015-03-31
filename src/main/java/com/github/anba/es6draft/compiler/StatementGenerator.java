/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundNameWithEnvironment;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundNameWithValue;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.ResolveBinding;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsConstantDeclaration;

import java.util.List;

import com.github.anba.es6draft.ast.AbruptNode.Abrupt;
import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.compiler.Labels.BreakLabel;
import com.github.anba.es6draft.compiler.Labels.ContinueLabel;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.TryCatchLabel;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.Reference;

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

        /**
         * Returns {@code true} if this completion type is not {@link #Normal}.
         * 
         * @return {@code true} if not the normal completion type
         */
        boolean isAbrupt() {
            return this != Normal;
        }

        /**
         * <pre>
         * {@code
         * then :: Completion -> Completion -> Completion
         * then a b = case (a, b) of
         *              (Normal, _) -> b
         *              _ -> a
         * }
         * </pre>
         * 
         * @param next
         *            the next completion
         * @return the statically computed completion type
         */
        Completion then(Completion next) {
            return this != Normal ? this : next;
        }

        /**
         * <pre>
         * {@code
         * select :: Completion -> Completion -> Completion
         * select a b = case (a, b) of
         *                (Normal, _) -> Normal
         *                (_, Normal) -> Normal
         *                _ | a == b -> a
         *                _ -> Abrupt
         * }
         * </pre>
         * 
         * @param other
         *            the other completion
         * @return the statically computed completion type
         */
        Completion select(Completion other) {
            return this == Normal || other == Normal ? Normal : this == other ? this : Abrupt;
        }

        /**
         * <pre>
         * {@code
         * normal :: Completion -> Bool -> Completion
         * normal a b = case (a, b) of
         *                (_, True) -> Normal
         *                _ -> a
         * }
         * </pre>
         * 
         * @param useNormal
         *            the flag to select the normal completion type
         * @return the statically computed completion type
         */
        Completion normal(boolean useNormal) {
            return useNormal ? Normal : this;
        }
    }

    private static final class Methods {
        // class: EnvironmentRecord
        static final MethodName EnvironmentRecord_createMutableBinding = MethodName.findInterface(
                Types.EnvironmentRecord, "createMutableBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName EnvironmentRecord_createImmutableBinding = MethodName
                .findInterface(Types.EnvironmentRecord, "createImmutableBinding",
                        Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        // class: GeneratorObject
        static final MethodName GeneratorObject_isLegacyGenerator = MethodName.findVirtual(
                Types.GeneratorObject, "isLegacyGenerator", Type.methodType(Type.BOOLEAN_TYPE));

        // class: Iterator
        static final MethodName Iterator_hasNext = MethodName.findInterface(Types.Iterator,
                "hasNext", Type.methodType(Type.BOOLEAN_TYPE));

        static final MethodName Iterator_next = MethodName.findInterface(Types.Iterator, "next",
                Type.methodType(Types.Object));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getEnvRec = MethodName.findVirtual(
                Types.LexicalEnvironment, "getEnvRec", Type.methodType(Types.EnvironmentRecord));

        // class: Reference
        static final MethodName Reference_initializeReferencedBinding = MethodName.findVirtual(
                Types.Reference, "initializeReferencedBinding",
                Type.methodType(Type.VOID_TYPE, Types.Object));

        static final MethodName Reference_putValue = MethodName.findVirtual(Types.Reference,
                "putValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));

        // class: ScriptException
        static final MethodName ScriptException_create = MethodName.findStatic(
                Types.ScriptException, "create",
                Type.methodType(Types.ScriptException, Types.Object));

        static final MethodName ScriptException_getValue = MethodName.findVirtual(
                Types.ScriptException, "getValue", Type.methodType(Types.Object));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_debugger = MethodName.findStatic(Types.ScriptRuntime,
                "debugger", Type.methodType(Type.VOID_TYPE));

        static final MethodName ScriptRuntime_enumerate = MethodName.findStatic(
                Types.ScriptRuntime, "enumerate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_enumerateValues = MethodName.findStatic(
                Types.ScriptRuntime, "enumerateValues",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_getStackOverflowError = MethodName.findStatic(
                Types.ScriptRuntime, "getStackOverflowError",
                Type.methodType(Types.StackOverflowError, Types.Error));

        static final MethodName ScriptRuntime_setFunctionBlockBinding = MethodName.findStatic(
                Types.ScriptRuntime, "setFunctionBlockBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Types.ExecutionContext));

        static final MethodName ScriptRuntime_iterate = MethodName.findStatic(Types.ScriptRuntime,
                "iterate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_toInternalError = MethodName.findStatic(
                Types.ScriptRuntime, "toInternalError", Type.methodType(Types.ScriptException,
                        Types.StackOverflowError, Types.ExecutionContext));
    }

    public StatementGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /* ----------------------------------------------------------------------------------------- */

    /**
     * stack: [Reference, Object] {@literal ->} []
     * 
     * @param type
     *            the value type, must be {@link ValType#Reference}
     * @param mv
     *            the statement visitor
     */
    private void InitializeReferencedBinding(ValType type, StatementVisitor mv) {
        assert type == ValType.Reference : "lhs is not reference: " + type;
        mv.invoke(Methods.Reference_initializeReferencedBinding);
    }

    /**
     * stack: [Reference, Object] {@literal ->} []
     * 
     * @param type
     *            the value type, must be {@link ValType#Reference}
     * @param mv
     *            the statement visitor
     */
    private void PutValue(Node node, ValType type, StatementVisitor mv) {
        assert type == ValType.Reference : "lhs is not reference: " + type;
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.Reference_putValue);
    }

    /**
     * stack: [env] {@literal ->} [env, envRec]
     * 
     * @param mv
     *            the statement visitor
     */
    private void getEnvRec(StatementVisitor mv) {
        mv.dup();
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
    }

    /**
     * stack: [envRec] {@literal ->} [envRec]
     * 
     * @param name
     *            the binding name
     * @param mv
     *            the statement visitor
     */
    private void createImmutableBinding(Name name, boolean strict, StatementVisitor mv) {
        mv.dup();
        mv.aconst(name.getIdentifier());
        mv.iconst(strict);
        mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);
    }

    /**
     * stack: [envRec] {@literal ->} [envRec]
     * 
     * @param name
     *            the binding name
     * @param deletable
     *            the deletable flag
     * @param mv
     *            the statement visitor
     */
    private void createMutableBinding(Name name, boolean deletable, StatementVisitor mv) {
        mv.dup();
        mv.aconst(name.getIdentifier());
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    @Override
    protected Completion visit(Node node, StatementVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * Extension: Async Function Definitions
     */
    @Override
    public Completion visit(AsyncFunctionDeclaration node, StatementVisitor mv) {
        codegen.compile(node);
        /* step 1 */
        return Completion.Normal;
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
        if (node.getScope().isPresent()) {
            newDeclarativeEnvironment(mv);
            codegen.blockInit(node, mv);
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
        if (node.getScope().isPresent() && !result.isAbrupt()) {
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
        /* steps 1-2 */
        BindingClassDeclarationEvaluation(node, mv);
        /* step 3 */
        return Completion.Normal;
    }

    /**
     * 14.5.15 Runtime Semantics: BindingClassDeclarationEvaluation
     * 
     * @param node
     *            the class declaration node
     * @param mv
     *            the statement visitor
     */
    private void BindingClassDeclarationEvaluation(ClassDeclaration node, StatementVisitor mv) {
        if (node.getIdentifier() != null) {
            /* step 1 */
            String className = node.getIdentifier().getName().getIdentifier();
            /* steps 2-3 */
            ClassDefinitionEvaluation(node, className, mv);
            /* steps 4-6 */
            SetFunctionName(node, className, mv);

            /* steps 7-9 */
            getEnvironmentRecord(mv);
            mv.swap();
            // stack: [envRec, value] -> []
            BindingInitializationWithEnvironment(node.getIdentifier(), mv);

            /* step 10 (return) */
        } else {
            // stack: [] -> [value]
            ClassDefinitionEvaluation(node, null, mv);
        }
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
        Jump lblNext = new Jump();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        /* step 1 (not applicable) */
        /* step 2 (repeat loop) */
        mv.mark(lblNext);

        /* steps 2.a-c */
        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 2.c (abrupt completion - continue) */
        if (lblContinue.isTarget()) {
            mv.mark(lblContinue);
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        /* steps 2.d-g */
        if (!result.isAbrupt() || lblContinue.isTarget()) {
            ValType type = expressionValue(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblNext);
        }

        /* step 2.c (abrupt completion - break) */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 2.c, 2.f, 2.g */
        return result.normal(lblContinue.isTarget() || lblBreak.isTarget());
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
     * 15.2.1.23 Runtime Semantics: Evaluation<br>
     * 15.2.3.10 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExportDeclaration node, StatementVisitor mv) {
        switch (node.getType()) {
        case All:
        case External:
        case Local:
            return Completion.Normal;
        case Variable:
            return node.getVariableStatement().accept(this, mv);
        case Declaration:
            return node.getDeclaration().accept(this, mv);
        case DefaultHoistableDeclaration:
            return node.getHoistableDeclaration().accept(this, mv);
        case DefaultClassDeclaration: {
            // return node.getClassDeclaration().accept(this, mv);
            ClassDeclaration decl = node.getClassDeclaration();
            /* steps 1-2 */
            BindingClassDeclarationEvaluation(decl, mv);
            /* steps 3-4 */
            if (decl.getIdentifier() == null) {
                /* steps 4.a-c */
                SetFunctionName(decl, "default", mv);
                /* steps 4.d-f */
                getEnvironmentRecord(mv);
                mv.swap();
                // stack: [envRec, value] -> []
                BindingInitializationWithEnvironment(decl.getName(), mv);
            }
            /* step 5 */
            return Completion.Normal;
        }
        case DefaultExpression:
            return node.getExpression().accept(this, mv);
        default:
            throw new AssertionError();
        }
    }

    /**
     * 15.2.3.10 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExportDefaultExpression node, StatementVisitor mv) {
        Expression expr = node.getExpression();
        /* steps 1-3 */
        expressionBoxedValue(expr, mv);
        /* step 4 */
        if (IsAnonymousFunctionDefinition(expr)) {
            SetFunctionName(expr, "default", mv);
        }
        /* step 5 */
        getEnvironmentRecord(mv);
        mv.swap();
        /* step 6 */
        InitializeBoundNameWithEnvironment(node.getBinding(), mv);
        /* step 7 */
        return Completion.Normal;
    }

    /**
     * 13.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExpressionStatement node, StatementVisitor mv) {
        Expression expr = mv.hasCompletion() ? node.getExpression() : node.getExpression()
                .emptyCompletion();

        /* steps 1-3 */
        ValType type = expressionValue(expr, mv);

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
        return visitForInOfLoop(node, IterationKind.EnumerateValues, mv);
    }

    /**
     * 13.0.8 Runtime Semantics: Evaluation<br>
     * 13.0.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.4.11 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForInStatement node, StatementVisitor mv) {
        return visitForInOfLoop(node, IterationKind.Enumerate, mv);
    }

    /**
     * 13.0.8 Runtime Semantics: Evaluation<br>
     * 13.0.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.4.11 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForOfStatement node, StatementVisitor mv) {
        return visitForInOfLoop(node, IterationKind.Iterate, mv);
    }

    /**
     * 13.6.4.11 Runtime Semantics: LabelledEvaluation
     * 
     * @param <FORSTATEMENT>
     *            the for-statement node type
     * @param node
     *            the for-statement node
     * @param expr
     *            the expression node
     * @param lhs
     *            the left-hand side node
     * @param stmt
     *            the statement node
     * @param iterationKind
     *            the for-statement's iteration kind
     * @param mv
     *            the statement visitor
     * @return the completion value
     */
    private <FORSTATEMENT extends IterationStatement & ForIterationNode> Completion visitForInOfLoop(
            FORSTATEMENT node, IterationKind iterationKind, StatementVisitor mv) {
        Jump lblFail = new Jump();

        /* steps 1-2 */
        ValType type = ForInOfHeadEvaluation(node, iterationKind, lblFail, mv);

        /* step 3 */
        Completion result = ForInOfBodyEvaluation(node, mv);

        if (type != ValType.Object) {
            mv.mark(lblFail);
        }
        return result;
    }

    /**
     * 13.6.4.12 Runtime Semantics: ForIn/OfHeadEvaluation (TDZnames, expr, iterationKind, labelSet)
     * <p>
     * stack: [] {@literal ->} [Iterator]
     * 
     * @param <FORSTATEMENT>
     *            the for-statement node type
     * @param node
     *            the for-statement node
     * @param iterationKind
     *            the for-statement's iteration kind
     * @param lblFail
     *            the target instruction if the expression node does not produce an object type
     * @param mv
     *            the statement visitor
     * @return the value type of the expression
     */
    private <FORSTATEMENT extends IterationStatement & ForIterationNode> ValType ForInOfHeadEvaluation(
            FORSTATEMENT node, IterationKind iterationKind, Jump lblFail, StatementVisitor mv) {
        /* steps 1-2 */
        Node lhs = node.getHead();
        List<Name> tdzNames = null;
        if (lhs instanceof LexicalDeclaration) {
            LexicalDeclaration lexDecl = (LexicalDeclaration) lhs;
            assert lexDecl.getElements().size() == 1;
            LexicalBinding lexicalBinding = lexDecl.getElements().get(0);
            tdzNames = BoundNames(lexicalBinding.getBinding());
            assert node.getScope().isPresent() == !tdzNames.isEmpty();
            if (node.getScope().isPresent()) {
                // stack: [] -> [TDZ]
                newDeclarativeEnvironment(mv);
                // stack: [TDZ] -> [TDZ, TDZRec]
                getEnvRec(mv);

                // stack: [TDZ, TDZRec] -> [TDZ]
                for (Name name : tdzNames) {
                    // FIXME: spec bug (CreateMutableBinding concrete method of `TDZ`)
                    createMutableBinding(name, false, mv);
                }
                mv.pop();
                // stack: [TDZ] -> []
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(node);
        }

        /* steps 3, 5-6 */
        Expression expr = node.getExpression();
        ValType type = expressionBoxedValue(expr, mv);

        /* step 4 */
        if (tdzNames != null) {
            mv.exitScope();
            if (node.getScope().isPresent()) {
                popLexicalEnvironment(mv);
            }
        }

        /* steps 7-10 */
        if (iterationKind == IterationKind.Enumerate
                || iterationKind == IterationKind.EnumerateValues) {
            /* step 7.a */
            if (type != ValType.Object) {
                Jump loopstart = new Jump();
                mv.dup();
                isUndefinedOrNull(mv);
                mv.ifeq(loopstart);
                mv.pop();
                mv.goTo(lblFail);
                mv.mark(loopstart);
            }
            /* steps 7.b-c, 9-10 */
            if (codegen.isEnabled(CompatibilityOption.LegacyGenerator)) {
                // legacy generator mode, both, for-in and for-each, perform Iterate on generators
                Jump l0 = new Jump(), l1 = new Jump();
                mv.dup();
                mv.instanceOf(Types.GeneratorObject);
                mv.ifeq(l0);
                mv.dup();
                mv.checkcast(Types.GeneratorObject);
                mv.invoke(Methods.GeneratorObject_isLegacyGenerator);
                mv.ifeq(l0);
                mv.loadExecutionContext();
                mv.lineInfo(expr);
                mv.invoke(Methods.ScriptRuntime_iterate);
                mv.goTo(l1);
                mv.mark(l0);
                mv.loadExecutionContext();
                if (iterationKind == IterationKind.Enumerate) {
                    mv.lineInfo(expr);
                    mv.invoke(Methods.ScriptRuntime_enumerate);
                } else {
                    mv.lineInfo(expr);
                    mv.invoke(Methods.ScriptRuntime_enumerateValues);
                }
                mv.mark(l1);
            } else if (iterationKind == IterationKind.Enumerate) {
                mv.loadExecutionContext();
                mv.lineInfo(expr);
                mv.invoke(Methods.ScriptRuntime_enumerate);
            } else {
                mv.loadExecutionContext();
                mv.lineInfo(expr);
                mv.invoke(Methods.ScriptRuntime_enumerateValues);
            }
        } else {
            /* steps 8-10 */
            assert iterationKind == IterationKind.Iterate;
            mv.loadExecutionContext();
            mv.lineInfo(expr);
            mv.invoke(Methods.ScriptRuntime_iterate);
        }

        return type;
    }

    /**
     * 13.6.4.13 Runtime Semantics: ForIn/OfBodyEvaluation (lhs, stmt, iterator, lhsKind, labelSet)
     * <p>
     * stack: [Iterator] {@literal ->} []
     * 
     * @param <FORSTATEMENT>
     *            the for-statement node type
     * @param node
     *            the for-statement node
     * @param mv
     *            the statement visitor
     * @return the completion value
     */
    private <FORSTATEMENT extends IterationStatement & ForIterationNode> Completion ForInOfBodyEvaluation(
            FORSTATEMENT node, StatementVisitor mv) {
        Node lhs = node.getHead();
        final boolean destructuring = IsDestructuring(lhs);
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();
        Jump loopbody = new Jump(), loopstart = new Jump();

        mv.enterVariableScope();
        Variable<ScriptIterator<?>> iterator = mv.newVariable("iter", ScriptIterator.class)
                .uncheckedCast();
        // stack: [Iterator] -> []
        mv.store(iterator);

        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);
        final Variable<Reference<?, ?>> lhsRef;
        if (!destructuring) {
            lhsRef = mv.newVariable("lhsRef", Reference.class).uncheckedCast();
            mv.anull();
            mv.store(lhsRef);
        } else {
            lhsRef = null;
        }

        /* steps 1-4 (not applicable) */
        /* step 5 (repeat loop) */
        mv.nonDestructiveGoTo(loopstart);

        /* steps 5.f-g */
        mv.mark(loopbody);
        mv.load(iterator);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_next);

        if (lhs instanceof LexicalDeclaration) {
            mv.enterScope(node);
        }

        /* steps 5.h-m */
        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            mv.enterWrapped();
            result = new IterationGenerator<FORSTATEMENT, StatementVisitor>(codegen) {
                @Override
                protected Completion iterationBody(FORSTATEMENT node,
                        Variable<ScriptIterator<?>> iterator, StatementVisitor mv) {
                    return ForInOfBodyEvaluationInner(node, lhsRef, mv);
                }

                @Override
                protected Variable<Object> enterIteration(FORSTATEMENT node, StatementVisitor mv) {
                    return mv.enterIterationBody(node);
                }

                @Override
                protected List<TempLabel> exitIteration(FORSTATEMENT node, StatementVisitor mv) {
                    return mv.exitIterationBody(node);
                }
            }.generate(node, iterator, loopstart, mv);
            mv.exitWrapped();
            mv.exitIteration(node);
        }

        if (lhs instanceof LexicalDeclaration) {
            mv.exitScope();
        }

        /* steps 5.m-n */
        if (lblContinue.isTarget()) {
            mv.mark(lblContinue);
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        /* steps 5.a-b */
        mv.mark(loopstart);
        if (lhs instanceof Expression) {
            /* step 5.a (1) */
            assert lhs instanceof LeftHandSideExpression;
            if (!destructuring) {
                ValType lhsType = expression((LeftHandSideExpression) lhs, mv);
                assert lhsType == ValType.Reference;
                mv.store(lhsRef);
            }
        } else if (lhs instanceof VariableStatement) {
            /* step 5.a (2) */
            assert ((VariableStatement) lhs).getElements().size() == 1;
            VariableDeclaration varDecl = ((VariableStatement) lhs).getElements().get(0);
            if (!destructuring) {
                BindingIdentifier bindingId = (BindingIdentifier) varDecl.getBinding();
                ResolveBinding(bindingId, mv);
                mv.store(lhsRef);
            }
        } else {
            /* step 5.b */
            assert lhs instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) lhs;
            assert lexDecl.getElements().size() == 1;
            LexicalBinding lexicalBinding = lexDecl.getElements().get(0);
            boolean isConst = IsConstantDeclaration(lexDecl);

            // stack: [] -> []
            if (node.getScope().isPresent()) {
                newDeclarativeEnvironment(mv);
                BindingInstantiation(lexicalBinding, isConst, mv);
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(node);

            if (!destructuring) {
                BindingIdentifier bindingId = (BindingIdentifier) lexicalBinding.getBinding();
                ResolveBinding(bindingId, mv);
                mv.store(lhsRef);
            }
        }

        /* steps 5.c-d */
        mv.load(iterator);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(loopbody);

        /* step 5.e */
        if (lhs instanceof LexicalDeclaration) {
            mv.exitScope();
            if (node.getScope().isPresent()) {
                popLexicalEnvironment(mv);
            }
        }

        /* steps 5.m-n */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        return result.normal(lblContinue.isTarget() || lblBreak.isTarget()).select(
                Completion.Normal);
    }

    private <FORSTATEMENT extends IterationStatement & ForIterationNode> Completion ForInOfBodyEvaluationInner(
            FORSTATEMENT node, Variable<Reference<?, ?>> lhsRef, StatementVisitor mv) {
        Node lhs = node.getHead();
        assert (lhsRef != null) == !IsDestructuring(lhs);
        /* steps 5.h-j */
        if (lhsRef != null) {
            /* steps 5.h, 5.j */
            if (lhs instanceof LexicalDeclaration) {
                /* step 5.h.i */
                // stack: [nextValue] -> [lhsRef, nextValue]
                mv.load(lhsRef);
                mv.swap();
                // stack: [lhsRef, nextValue] -> []
                InitializeReferencedBinding(ValType.Reference, mv);
            } else {
                /* step 5.h.ii */
                // stack: [nextValue] -> [lhsRef, nextValue]
                mv.load(lhsRef);
                mv.swap();
                // stack: [lhsRef, nextValue] -> []
                PutValue(lhs, ValType.Reference, mv);
            }
        } else {
            /* step 5.i, 5.j */
            if (lhs instanceof Expression) {
                /* step 5.i.i */
                assert lhs instanceof AssignmentPattern;
                // stack: [nextValue] -> []
                DestructuringAssignment((AssignmentPattern) lhs, mv);
            } else if (lhs instanceof VariableStatement) {
                /* step 5.i.ii */
                assert ((VariableStatement) lhs).getElements().size() == 1;
                VariableDeclaration varDecl = ((VariableStatement) lhs).getElements().get(0);
                Binding binding = varDecl.getBinding();
                assert binding instanceof BindingPattern;
                // stack: [nextValue] -> []
                BindingInitialization(binding, mv);
            } else {
                /* step 5.i.iii */
                assert lhs instanceof LexicalDeclaration;
                LexicalDeclaration lexDecl = (LexicalDeclaration) lhs;
                assert lexDecl.getElements().size() == 1;
                LexicalBinding lexicalBinding = lexDecl.getElements().get(0);

                // 13.6.4.10 Runtime Semantics: BindingInitialization
                // stack: [nextValue] -> [envRec, nextValue]
                getEnvironmentRecord(mv);
                mv.swap();
                // stack: [envRec, nextValue] -> []
                BindingInitializationWithEnvironment(lexicalBinding.getBinding(), mv);
            }
        }

        /* steps 5.k-l */
        Completion result = node.getStatement().accept(this, mv);

        /* step 5.m */
        if (lhs instanceof LexicalDeclaration) {
            if (node.getScope().isPresent() && !result.isAbrupt()) {
                popLexicalEnvironment(mv);
            }
        }
        return result;
    }

    private static boolean IsDestructuring(Node lhs) {
        if (lhs instanceof Expression) {
            assert lhs instanceof LeftHandSideExpression;
            return lhs instanceof AssignmentPattern;
        } else if (lhs instanceof VariableStatement) {
            assert ((VariableStatement) lhs).getElements().size() == 1;
            VariableDeclaration varDecl = ((VariableStatement) lhs).getElements().get(0);
            return varDecl.getBinding() instanceof BindingPattern;
        } else {
            assert lhs instanceof LexicalDeclaration;
            assert ((LexicalDeclaration) lhs).getElements().size() == 1;
            LexicalBinding lexicalBinding = ((LexicalDeclaration) lhs).getElements().get(0);
            return lexicalBinding.getBinding() instanceof BindingPattern;
        }
    }

    /**
     * 13.6.4.10 Runtime Semantics: BindingInstantiation
     */
    private void BindingInstantiation(LexicalBinding lexicalBinding, boolean isConst,
            StatementVisitor mv) {
        // stack: [iterEnv] -> [iterEnv, envRec]
        getEnvRec(mv);

        // stack: [iterEnv, envRec] -> [iterEnv, envRec]
        for (Name name : BoundNames(lexicalBinding.getBinding())) {
            if (isConst) {
                // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                createImmutableBinding(name, true, mv);
            } else {
                // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                createMutableBinding(name, false, mv);
            }
        }

        // stack: [iterEnv, envRec] -> [iterEnv]
        mv.pop();
    }

    /**
     * 13.0.8 Runtime Semantics: Evaluation<br>
     * 13.0.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.3.7 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForStatement node, StatementVisitor mv) {
        boolean perIterationsLets = false;
        Node head = node.getHead();
        if (head == null) {
            // empty
        } else if (head instanceof Expression) {
            ValType type = expressionValue(((Expression) head).emptyCompletion(), mv);
            mv.pop(type);
        } else if (head instanceof VariableStatement) {
            head.accept(this, mv);
        } else {
            assert head instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) head;
            List<Name> boundNames = BoundNames(lexDecl);
            boolean isConst = IsConstantDeclaration(lexDecl);
            perIterationsLets = !isConst && !boundNames.isEmpty();

            if (node.getScope().isPresent()) {
                // stack: [] -> [loopEnv]
                newDeclarativeEnvironment(mv);
                // stack: [loopEnv] -> [loopEnv, envRec]
                getEnvRec(mv);

                // stack: [loopEnv, envRec] -> [loopEnv]
                for (Name dn : boundNames) {
                    if (isConst) {
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `loopEnv`)
                        createImmutableBinding(dn, true, mv);
                    } else {
                        // FIXME: spec bug (CreateMutableBinding concrete method of `loopEnv`)
                        createMutableBinding(dn, false, mv);
                    }
                }
                mv.pop();
                // stack: [loopEnv] -> []
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(node);

            lexDecl.accept(this, mv);
        }

        Completion result = ForBodyEvaluation(node, perIterationsLets, mv);

        if (head instanceof LexicalDeclaration) {
            mv.exitScope();
            if (node.getScope().isPresent() && !result.isAbrupt()) {
                popLexicalEnvironment(mv);
            }
        }
        return result;
    }

    /**
     * 13.6.3.8 Runtime Semantics: ForBodyEvaluation(test, increment, stmt, perIterationBindings,
     * labelSet)
     */
    private Completion ForBodyEvaluation(ForStatement node, boolean perIterationsLets,
            StatementVisitor mv) {
        mv.enterVariableScope();
        /* step 1 (not applicable) */
        /* steps 2-3 */
        Variable<LexicalEnvironment<?>> savedEnv;
        if (perIterationsLets) {
            savedEnv = mv.newVariable("savedEnv", LexicalEnvironment.class).uncheckedCast();
            CreatePerIterationEnvironment(savedEnv, mv);
        } else {
            savedEnv = saveEnvironment(node, mv);
        }

        Jump lblTest = new Jump(), lblStmt = new Jump();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();

        /* steps 4.b-d */
        Completion result;
        mv.nonDestructiveGoTo(lblTest);
        mv.mark(lblStmt);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 4.c (abrupt completion - continue) */
        if (lblContinue.isTarget()) {
            mv.mark(lblContinue);
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        /* steps 4.e-f */
        if (perIterationsLets && (!result.isAbrupt() || lblContinue.isTarget())) {
            CreatePerIterationEnvironment(savedEnv, mv);
        }

        /* step 4.g */
        if (node.getStep() != null && (!result.isAbrupt() || lblContinue.isTarget())) {
            ValType type = expressionValue(node.getStep().emptyCompletion(), mv);
            mv.pop(type);
        }

        /* step 4.a */
        mv.mark(lblTest);
        if (node.getTest() != null) {
            ValType type = expressionValue(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblStmt);
        } else {
            mv.goTo(lblStmt);
        }

        /* step 4.c (abrupt completion - break) */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        if (node.getTest() == null) {
            if (!result.isAbrupt() && !lblBreak.isTarget()) {
                return Completion.Abrupt; // infinite loop
            }
            return result.normal(lblBreak.isTarget());
        }
        return result.normal(lblContinue.isTarget() || lblBreak.isTarget()).select(
                Completion.Normal);
    }

    /**
     * 13.6.3.9 Runtime Semantics: CreatePerIterationEnvironment( perIterationBindings )
     * 
     * @param savedEnv
     *            the variable which holds the saved environment
     * @param mv
     *            the statement visitor
     */
    private void CreatePerIterationEnvironment(Variable<LexicalEnvironment<?>> savedEnv,
            StatementVisitor mv) {
        /* steps 1.a-e */
        cloneDeclarativeEnvironment(mv);
        mv.store(savedEnv);
        /* step 1.f */
        replaceLexicalEnvironment(savedEnv, mv);
    }

    /**
     * 14.1.23 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(FunctionDeclaration node, StatementVisitor mv) {
        codegen.compile(node);

        /* B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics */
        if (node.isLegacyBlockScoped()) {
            mv.aconst(node.getIdentifier().getName().getIdentifier());
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.ScriptRuntime_setFunctionBlockBinding);
        }

        /* step 1 */
        return Completion.Normal;
    }

    /**
     * 13.0.8 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(GeneratorDeclaration node, StatementVisitor mv) {
        codegen.compile(node);
        /* step 1 */
        return Completion.Normal;
    }

    /**
     * 13.5.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(IfStatement node, StatementVisitor mv) {
        /* steps 1-3 */
        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        if (node.getOtherwise() != null) {
            // IfStatement : if ( Expression ) Statement else Statement
            Jump l0 = new Jump(), l1 = new Jump();

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
            Jump l0 = new Jump();

            /* step 5 */
            mv.ifeq(l0);
            Completion resultThen = node.getThen().accept(this, mv);
            mv.mark(l0);

            /* steps 4, 6-7 */
            return resultThen.select(Completion.Normal);
        }
    }

    /**
     * 15.2.1.23 Runtime Semantics: Evaluation<br>
     * 15.2.2.6 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ImportDeclaration node, StatementVisitor mv) {
        return Completion.Normal;
    }

    /**
     * 13.12.15 Runtime Semantics: Evaluation<br>
     * 13.12.14 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(LabelledStatement node, StatementVisitor mv) {
        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        /* steps 1-3 */
        BreakLabel label = new BreakLabel();
        mv.enterLabelled(node, label);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitLabelled(node);

        /* step 4 */
        if (label.isTarget()) {
            mv.mark(label);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 4-5 */
        return result.normal(label.isTarget());
    }

    /**
     * 13.12.14 Runtime Semantics: LabelledEvaluation<br>
     * 
     * <code>LabelledItem: FunctionDeclaration</code>
     */
    @Override
    public Completion visit(LabelledFunctionStatement node, StatementVisitor mv) {
        return node.getFunction().accept(this, mv);
    }

    /**
     * 13.2.1.4 Runtime Semantics: Evaluation
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
        if (node.getScope().isPresent()) {
            // stack: [] -> [env]
            newDeclarativeEnvironment(mv);
            // stack: [env] -> [env, envRec]
            getEnvRec(mv);

            // stack: [env, envRec] -> [env]
            for (LexicalBinding binding : node.getBindings()) {
                // stack: [env, envRec] -> [env, envRec, envRec]
                mv.dup();

                // stack: [env, envRec, envRec] -> [env, envRec, envRec]
                for (Name name : BoundNames(binding.getBinding())) {
                    createMutableBinding(name, false, mv);
                }

                Expression initializer = binding.getInitializer();
                if (initializer != null) {
                    expressionBoxedValue(initializer, mv);
                } else {
                    assert binding.getBinding() instanceof BindingIdentifier;
                    mv.loadUndefined();
                }

                // stack: [env, envRec, envRec, value] -> [env, envRec]
                BindingInitializationWithEnvironment(binding.getBinding(), mv);
            }
            mv.pop();
            // stack: [env] -> []
            pushLexicalEnvironment(mv);
        }

        mv.enterScope(node);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitScope();

        if (node.getScope().isPresent() && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }

        return result;
    }

    /**
     * 13.2.1.4 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(LexicalBinding node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initializer = node.getInitializer();
        if (initializer == null) {
            // LexicalBinding : BindingIdentifier
            assert binding instanceof BindingIdentifier;
            Name name = ((BindingIdentifier) binding).getName();
            assert mv.getScope().isDeclared(name);
            /* steps 1-2 */
            // stack: [] -> [envRec, name, value]
            getEnvironmentRecord(mv);
            mv.aconst(name.getIdentifier());
            mv.loadUndefined();
            // stack: [envRec, name, value] -> []
            InitializeBoundNameWithValue(mv);
        } else if (binding instanceof BindingIdentifier) {
            // LexicalBinding : BindingIdentifier Initializer
            Name name = ((BindingIdentifier) binding).getName();
            assert mv.getScope().isDeclared(name);
            // stack: [] -> [envRec, name]
            getEnvironmentRecord(mv);
            mv.aconst(name.getIdentifier());
            /* steps 3-5 */
            // stack: [envRec, name] -> [envRec, name, value]
            expressionBoxedValue(initializer, mv);
            /* step 6 */
            if (IsAnonymousFunctionDefinition(initializer)) {
                SetFunctionName(initializer, name, mv);
            }
            /* steps 1-2, 7 */
            // stack: [envRec, name, value] -> []
            InitializeBoundNameWithValue(mv);
        } else {
            // LexicalBinding : BindingPattern Initializer
            assert binding instanceof BindingPattern;
            /* steps 1-3 */
            expressionBoxedValue(initializer, mv);
            /* step 4 */
            getEnvironmentRecord(mv);
            mv.swap();
            /* step 5 */
            BindingInitializationWithEnvironment(binding, mv);
        }
        return Completion.Normal;
    }

    /**
     * 13.9.1 Runtime Semantics: Evaluation
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

        if (mv.isFunction()) {
            // TODO: only emit when `return` used in StatementListMethod
            Jump noReturn = new Jump();
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
     * 13.11.11 Runtime Semantics: Evaluation
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
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptException_create);

        /* step 4 */
        mv.athrow();
        return Completion.Throw;
    }

    /**
     * 13.14.8 Runtime Semantics: Evaluation
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
     * 13.14.8 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-catch-finally</code>
     * 
     * @param node
     *            the try-statement
     * @param mv
     *            the statement visitor
     * @return the completion value
     */
    private Completion visitTryCatchFinally(TryStatement node, StatementVisitor mv) {
        TryCatchLabel startCatchFinally = new TryCatchLabel();
        TryCatchLabel endCatch = new TryCatchLabel(), handlerCatch = new TryCatchLabel();
        TryCatchLabel endFinally = new TryCatchLabel(), handlerFinally = new TryCatchLabel();
        TryCatchLabel handlerCatchStackOverflow = new TryCatchLabel();
        TryCatchLabel handlerFinallyStackOverflow = new TryCatchLabel();
        TryCatchLabel handlerReturn = null;
        if (mv.isGeneratorOrAsync()
                && !(mv.isResumable() && !codegen.isEnabled(Compiler.Option.NoResume))) {
            handlerReturn = new TryCatchLabel();
        }
        Jump noException = new Jump();

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(mv);
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
                catchResult, handlerFinally, handlerFinallyStackOverflow, handlerReturn,
                noException, tempLabels, mv);

        mv.exitVariableScope();
        mv.tryCatch(startCatchFinally, endCatch, handlerCatch, Types.ScriptException);
        mv.tryCatch(startCatchFinally, endCatch, handlerCatchStackOverflow, Types.Error);
        if (handlerReturn != null) {
            mv.tryCatch(startCatchFinally, endFinally, handlerReturn, Types.ReturnValue);
        }
        mv.tryCatch(startCatchFinally, endFinally, handlerFinally, Types.ScriptException);
        mv.tryCatch(startCatchFinally, endFinally, handlerFinallyStackOverflow, Types.Error);

        /* steps 5-6 */
        return finallyResult.then(tryResult.select(catchResult));
    }

    /**
     * 13.14.8 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-catch</code>
     * 
     * @param node
     *            the try-statement
     * @param mv
     *            the statement visitor
     * @return the completion value
     */
    private Completion visitTryCatch(TryStatement node, StatementVisitor mv) {
        TryCatchLabel startCatch = new TryCatchLabel(), endCatch = new TryCatchLabel();
        TryCatchLabel handlerCatch = new TryCatchLabel();
        TryCatchLabel handlerCatchStackOverflow = new TryCatchLabel();
        Jump exceptionHandled = new Jump();

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(mv);

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
     * 13.14.8 Runtime Semantics: Evaluation<br>
     * 
     * <code>try-finally</code>
     * 
     * @param node
     *            the try-statement
     * @param mv
     *            the statement visitor
     * @return the completion value
     */
    private Completion visitTryFinally(TryStatement node, StatementVisitor mv) {
        TryCatchLabel startFinally = new TryCatchLabel(), endFinally = new TryCatchLabel();
        TryCatchLabel handlerFinally = new TryCatchLabel();
        TryCatchLabel handlerFinallyStackOverflow = new TryCatchLabel();
        TryCatchLabel handlerReturn = null;
        if (mv.isGeneratorOrAsync()
                && !(mv.isResumable() && !codegen.isEnabled(Compiler.Option.NoResume))) {
            handlerReturn = new TryCatchLabel();
        }
        Jump noException = new Jump();

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(mv);
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
                Completion.Abrupt, handlerFinally, handlerFinallyStackOverflow, handlerReturn,
                noException, tempLabels, mv);

        mv.exitVariableScope();
        if (handlerReturn != null) {
            mv.tryCatch(startFinally, endFinally, handlerReturn, Types.ReturnValue);
        }
        mv.tryCatch(startFinally, endFinally, handlerFinally, Types.ScriptException);
        mv.tryCatch(startFinally, endFinally, handlerFinallyStackOverflow, Types.Error);

        /* steps 3-4 */
        return finallyResult.then(tryResult);
    }

    private Completion emitTryBlock(TryStatement node, Jump noException, StatementVisitor mv) {
        mv.enterWrapped();
        Completion tryResult = node.getTryBlock().accept(this, mv);
        mv.exitWrapped();
        if (!tryResult.isAbrupt()) {
            mv.goTo(noException);
        }
        return tryResult;
    }

    private Completion emitCatchBlock(TryStatement node, Variable<LexicalEnvironment<?>> savedEnv,
            TryCatchLabel handlerCatch, TryCatchLabel handlerCatchStackOverflow, StatementVisitor mv) {
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
            mv.enterCatchWithGuarded(node, new Jump());

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

    private Completion emitFinallyBlock(TryStatement node,
            Variable<LexicalEnvironment<?>> savedEnv, Variable<Object> completion,
            Completion tryResult, Completion catchResult, TryCatchLabel handlerFinally,
            TryCatchLabel handlerFinallyStackOverflow, TryCatchLabel handlerReturn,
            Jump noException, List<TempLabel> tempLabels, StatementVisitor mv) {
        BlockStatement finallyBlock = node.getFinallyBlock();
        assert finallyBlock != null;

        // various finally blocks (1 - 4)
        // (1) finally block for abrupt completions within 'try-catch'
        mv.enterVariableScope();
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);
        mv.catchHandler(handlerFinallyStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_getStackOverflowError);
        mv.catchHandler(handlerFinally, Types.ScriptException);
        if (handlerReturn != null) {
            mv.catchHandler(handlerReturn, Types.ReturnValue);
        }
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
        Jump exceptionHandled = null;
        if (!tryResult.isAbrupt() || !catchResult.isAbrupt()) {
            mv.mark(noException);
            emitFinallyBlock(finallyBlock, mv);
            if (!finallyResult.isAbrupt() && !tempLabels.isEmpty()) {
                exceptionHandled = new Jump();
                mv.goTo(exceptionHandled);
            }
        }

        // (4) finally blocks for other abrupt completion (return, break, continue)
        for (TempLabel temp : tempLabels) {
            if (temp.isTarget()) {
                mv.mark(temp);
                restoreEnvironment(savedEnv, mv);
                emitFinallyBlock(finallyBlock, mv);
                if (!finallyResult.isAbrupt()) {
                    mv.goTo(temp, completion);
                }
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
     * 13.14.7 Runtime Semantics: CatchClauseEvaluation
     */
    @Override
    public Completion visit(CatchNode node, StatementVisitor mv) {
        Binding catchParameter = node.getCatchParameter();
        BlockStatement catchBlock = node.getCatchBlock();

        // stack: [e] -> [ex]
        mv.invoke(Methods.ScriptException_getValue);

        /* step 1 (not applicable) */
        /* step 2 */
        // stack: [ex] -> [ex, catchEnv]
        newCatchDeclarativeEnvironment(mv);
        {
            // stack: [ex, catchEnv] -> [ex, catchEnv, envRec]
            getEnvRec(mv);

            /* step 3 */
            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            // [ex, catchEnv, envRec] -> [ex, envRec, catchEnv]
            for (Name name : BoundNames(catchParameter)) {
                createMutableBinding(name, false, mv);
            }
            mv.swap();
        }
        /* step 4 */
        // stack: [ex, envRec, catchEnv] -> [ex, envRec]
        pushLexicalEnvironment(mv);
        mv.enterScope(node);

        // stack: [ex, envRec] -> [envRec, ex]
        mv.swap();

        /* steps 5-6 */
        // stack: [envRec, ex] -> []
        BindingInitializationWithEnvironment(catchParameter, mv);

        /* step 7 */
        Completion result = catchBlock.accept(this, mv);

        /* step 8 */
        mv.exitScope();
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
        Jump l0 = new Jump();

        // stack: [e] -> [ex]
        mv.invoke(Methods.ScriptException_getValue);

        /* step 1 (not applicable) */
        /* step 2 */
        // stack: [ex] -> [ex, catchEnv]
        newCatchDeclarativeEnvironment(mv);
        {
            // stack: [ex, catchEnv] -> [ex, catchEnv, envRec]
            getEnvRec(mv);

            /* step 3 */
            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            // [ex, catchEnv, envRec] -> [ex, envRec, catchEnv]
            for (Name name : BoundNames(catchParameter)) {
                createMutableBinding(name, false, mv);
            }
            mv.swap();
        }
        /* step 4 */
        // stack: [ex, envRec, catchEnv] -> [ex, envRec]
        pushLexicalEnvironment(mv);
        mv.enterScope(node);

        // stack: [ex, envRec] -> [envRec, ex]
        mv.swap();

        /* steps 5-6 */
        // stack: [envRec, ex] -> []
        BindingInitializationWithEnvironment(catchParameter, mv);

        /* step 7 */
        Completion result;
        ToBoolean(expressionValue(node.getGuard(), mv), mv);
        mv.ifeq(l0);
        {
            result = catchBlock.accept(this, mv);
            if (!result.isAbrupt()) {
                popLexicalEnvironment(mv);
                mv.goTo(mv.catchWithGuardedLabel());
            }
        }
        mv.mark(l0);

        /* step 8 */
        mv.exitScope();
        popLexicalEnvironment(mv);

        /* step 9 */
        return result;
    }

    /**
     * 13.2.2.4 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(VariableDeclaration node, StatementVisitor mv) {
        Binding binding = node.getBinding();
        Expression initializer = node.getInitializer();
        if (initializer == null) {
            // VariableDeclaration : BindingIdentifier
            assert binding instanceof BindingIdentifier;
            /* step 1 (return) */
        } else if (binding instanceof BindingIdentifier) {
            // VariableDeclaration : BindingIdentifier Initializer
            /* step 1 */
            BindingIdentifier bindingId = (BindingIdentifier) binding;
            /* step 2 */
            ResolveBinding(bindingId, mv);
            /* steps 3-5 */
            expressionBoxedValue(initializer, mv);
            /* step 6 */
            if (IsAnonymousFunctionDefinition(initializer)) {
                SetFunctionName(initializer, bindingId.getName(), mv);
            }
            /* step 7 */
            PutValue(binding, ValType.Reference, mv);
        } else {
            // VariableDeclaration : BindingPattern Initializer
            assert binding instanceof BindingPattern;
            /* steps 1-3 */
            expressionBoxedValue(initializer, mv);
            /* step 4 */
            BindingInitialization(binding, mv);
        }
        return Completion.Normal;
    }

    /**
     * 13.2.2.4 Runtime Semantics: Evaluation
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
     * 13.0.8 Runtime Semantics: Evaluation<br>
     * 13.0.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.6.2.6 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(WhileStatement node, StatementVisitor mv) {
        Jump lblNext = new Jump(), lblTest = new Jump();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        /* step 1 (not applicable) */
        /* step 2 (repeat loop) */
        mv.nonDestructiveGoTo(lblTest);
        mv.mark(lblNext);

        /* steps 2.e-g */
        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 2.f (abrupt completion - continue) */
        if (lblContinue.isTarget()) {
            mv.mark(lblContinue);
            restoreEnvironment(node, Abrupt.Continue, savedEnv, mv);
        }

        /* steps 2.a-d */
        mv.mark(lblTest);
        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifne(lblNext);

        /* step 2.f (abrupt completion - break) */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(node, Abrupt.Break, savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 2.c, 2.d, 2.f */
        return result.normal(lblContinue.isTarget() || lblBreak.isTarget()).select(
                Completion.Normal);
    }

    /**
     * 13.10.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(WithStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        ValType type = expressionValue(node.getExpression(), mv);

        /* steps 2-3 */
        ToObject(node, type, mv);

        /* steps 4-7 */
        newObjectEnvironment(mv, true);
        pushLexicalEnvironment(mv);

        /* step 8 */
        mv.enterScope(node);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitScope();

        /* step 9 */
        if (!result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }

        /* step 10 */
        return result;
    }
}
