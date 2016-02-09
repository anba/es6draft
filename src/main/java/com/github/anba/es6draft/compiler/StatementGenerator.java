/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.BindingInitialization;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundName;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundNameWithInitializer;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundNameWithUndefined;
import static com.github.anba.es6draft.compiler.DestructuringAssignmentGenerator.DestructuringAssignment;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsAnonymousFunctionDefinition;
import static com.github.anba.es6draft.semantics.StaticSemantics.IsConstantDeclaration;

import java.util.List;

import com.github.anba.es6draft.ast.*;
import com.github.anba.es6draft.ast.scope.BlockScope;
import com.github.anba.es6draft.ast.scope.FunctionScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.ast.scope.TopLevelScope;
import com.github.anba.es6draft.ast.synthetic.StatementListMethod;
import com.github.anba.es6draft.compiler.Labels.BreakLabel;
import com.github.anba.es6draft.compiler.Labels.ContinueLabel;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.assembler.InstructionAssembler;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.TryCatchLabel;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Value;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 *
 */
final class StatementGenerator extends
        DefaultCodeGenerator<StatementGenerator.Completion, StatementVisitor> {
    /**
     * 6.2.2 The Completion Record Specification Type
     */
    enum Completion {
        Empty, Normal, Return, Throw, Break, Continue, Abrupt;

        /**
         * Returns {@code true} if this completion type is not {@link #Normal}.
         * 
         * @return {@code true} if not the normal completion type
         */
        boolean isAbrupt() {
            return !(this == Normal || this == Empty);
        }

        /**
         * <pre>
         * {@code
         * then :: Completion -> Completion -> Completion
         * then a b = case (a, b) of
         *              (Normal, Empty) -> a
         *              (Normal, _) -> b
         *              (Empty, _) -> b
         *              _ -> a
         * }
         * </pre>
         * 
         * @param next
         *            the next completion
         * @return the statically computed completion type
         */
        Completion then(Completion next) {
            if (!(this == Normal || this == Empty) || (this == Normal && next == Empty)) {
                return this;
            }
            return next;
        }

        /**
         * <pre>
         * {@code
         * select :: Completion -> Completion -> Completion
         * select a b = case (a, b) of
         *                (Empty, _) -> Normal
         *                (_, Empty) -> Normal
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
            if (this == Normal || this == Empty || other == Normal || other == Empty) {
                return Normal;
            }
            return this == other ? this : Abrupt;
        }

        /**
         * <pre>
         * {@code
         * normal :: Completion -> Bool -> Completion
         * normal a b = case (a, b) of
         *                (_, True) -> Normal
         *                (Empty, _) -> Normal
         *                _ -> a
         * }
         * </pre>
         * 
         * @param useNormal
         *            the flag to select the normal completion type
         * @return the statically computed completion type
         */
        Completion normal(boolean useNormal) {
            return (useNormal || this == Empty) ? Normal : this;
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
        Completion normalOrEmpty(boolean useNormal) {
            return useNormal ? Normal : this;
        }

        /**
         * <pre>
         * {@code
         * nonEmpty :: Completion -> Completion
         * nonEmpty a = case (a) of
         *                (Empty) -> Normal
         *                _ -> a
         * }
         * </pre>
         * 
         * @return the statically computed completion type
         */
        Completion nonEmpty() {
            return this == Empty ? Normal : this;
        }
    }

    private enum Bool {
        True, False, Any;

        private static Bool from(boolean b) {
            return b ? True : False;
        }

        static Bool evaluate(Expression expr) {
            if (expr instanceof Literal) {
                if (expr instanceof BooleanLiteral) {
                    return from(((BooleanLiteral) expr).getValue());
                }
                if (expr instanceof NumericLiteral) {
                    double num = ((NumericLiteral) expr).getValue();
                    return from(num != 0 && !Double.isNaN(num));
                }
                if (expr instanceof StringLiteral) {
                    String s = ((StringLiteral) expr).getValue();
                    return from(s.length() != 0);
                }
                assert expr instanceof NullLiteral;
                return False;
            }
            return Any;
        }
    }

    private static final class Methods {
        // class: GeneratorObject
        static final MethodName GeneratorObject_isLegacyGenerator = MethodName.findVirtual(
                Types.GeneratorObject, "isLegacyGenerator", Type.methodType(Type.BOOLEAN_TYPE));

        // class: Iterator
        static final MethodName Iterator_hasNext = MethodName.findInterface(Types.Iterator,
                "hasNext", Type.methodType(Type.BOOLEAN_TYPE));

        static final MethodName Iterator_next = MethodName.findInterface(Types.Iterator, "next",
                Type.methodType(Types.Object));

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

        static final MethodName ScriptRuntime_stackOverflowError = MethodName.findStatic(
                Types.ScriptRuntime, "stackOverflowError",
                Type.methodType(Types.StackOverflowError, Types.Error));

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

    @Override
    protected Completion visit(Node node, StatementVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * stack: [value] {@literal ->} []
     * 
     * @param name
     *            the binding name
     * @param clazz
     *            the variable type
     * @param mv
     *            the statement visitor
     */
    private <T> void InitializeBoundNameWithValue(Name name, Class<T> clazz, StatementVisitor mv) {
        mv.enterVariableScope();

        Variable<T> value = mv.newVariable("value", clazz);
        mv.store(value);

        Class<? extends EnvironmentRecord> envRecClass = getEnvironmentRecordClass(mv);
        Variable<? extends EnvironmentRecord> envRec = mv.newVariable("envRec", envRecClass);
        getLexicalEnvironmentRecord(envRec, mv);

        InitializeBoundName(envRec, name, value, mv);

        mv.exitVariableScope();
    }

    /**
     * Extension: Async Function Definitions
     */
    @Override
    public Completion visit(AsyncFunctionDeclaration node, StatementVisitor mv) {
        codegen.compile(node);
        /* step 1 */
        return Completion.Empty;
    }

    /**
     * 13.2 Block
     * <p>
     * 13.2.13 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(BlockStatement node, StatementVisitor mv) {
        if (node.getStatements().isEmpty()) {
            // Block : { }
            // -> Return NormalCompletion(empty)
            return Completion.Empty;
        }

        /* steps 1-4 */
        BlockScope scope = node.getScope();
        if (scope.isPresent()) {
            newDeclarativeEnvironment(scope, mv);
            codegen.blockInit(node, mv);
            pushLexicalEnvironment(mv);
        }

        /* step 5 */
        mv.enterScope(node);
        Completion result = Completion.Empty;
        {
            // 13.2.13 Runtime Semantics: Evaluation
            // StatementList : StatementList StatementListItem
            /* steps 1-4 */
            for (StatementListItem statement : node.getStatements()) {
                if ((result = result.then(statement.accept(this, mv))).isAbrupt()) {
                    break;
                }
            }
        }
        mv.exitScope();

        /* step 6 */
        if (scope.isPresent() && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }

        /* step 7 */
        return result;
    }

    /**
     * 13.9 The break Statement
     * <p>
     * 13.9.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(BreakStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        mv.goTo(mv.breakLabel(node));
        return Completion.Break;
    }

    /**
     * 14.5 Class Definitions
     * <p>
     * 14.5.16 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ClassDeclaration node, StatementVisitor mv) {
        /* steps 1-2 */
        BindingClassDeclarationEvaluation(node, mv);
        /* step 3 */
        return Completion.Empty;
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
            Name className = node.getIdentifier().getName();
            /* steps 2-3 */
            ClassDefinitionEvaluation(node, className, mv);
            /* steps 4-6 */
            SetFunctionName(node, className, mv);

            /* steps 7-9 */
            // stack: [value] -> []
            InitializeBoundNameWithValue(className, FunctionObject.class, mv);

            /* step 10 (return) */
        } else {
            // stack: [] -> [value]
            ClassDefinitionEvaluation(node, null, mv);
        }
    }

    /**
     * 13.8 The continue Statement
     * <p>
     * 13.8.3 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ContinueStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        mv.goTo(mv.continueLabel(node));
        return Completion.Continue;
    }

    /**
     * 13.16 The debugger statement
     * <p>
     * 13.16.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(DebuggerStatement node, StatementVisitor mv) {
        /* steps 1-3 */
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_debugger);
        return Completion.Empty;
    }

    /**
     * 13.7.2 The do-while Statement
     * <p>
     * 13.1.8 Runtime Semantics: Evaluation<br>
     * 13.1.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.7.2.6 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(DoWhileStatement node, StatementVisitor mv) {
        Jump lblNext = new Jump();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();
        Bool btest = Bool.evaluate(node.getTest());

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        /* step 1 */
        if (node.hasCompletionValue()) {
            mv.storeUndefinedAsCompletionValue();
        }
        /* step 2 (repeat loop) */
        mv.mark(lblNext);

        /* steps 2.a-c */
        Completion result;
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 2.b (abrupt completion - continue) */
        if (lblContinue.isTarget()) {
            mv.mark(lblContinue);
            restoreEnvironment(savedEnv, mv);
        }

        /* steps 2.d-g */
        if (!result.isAbrupt() || lblContinue.isTarget()) {
            if (btest == Bool.Any) {
                ValType type = expression(node.getTest(), mv);
                ToBoolean(type, mv);
                mv.ifne(lblNext);
            } else if (btest == Bool.True) {
                mv.goTo(lblNext);
            }
        }

        /* step 2.b (abrupt completion - break) */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 2.b, 2.g */
        if (btest == Bool.True) {
            if (!result.isAbrupt() && !lblBreak.isTarget()) {
                return Completion.Abrupt; // infinite loop
            }
            return result.normal(lblBreak.isTarget());
        }
        return result.normal(lblContinue.isTarget() || lblBreak.isTarget());
    }

    /**
     * 13.4 Empty Statement
     * <p>
     * 13.4.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(EmptyStatement node, StatementVisitor mv) {
        /* step 1 */
        return Completion.Empty;
    }

    /**
     * 15.2.3 Exports
     * <p>
     * 15.2.3.11 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExportDeclaration node, StatementVisitor mv) {
        switch (node.getType()) {
        case All:
        case External:
        case Local:
            return Completion.Empty;
        case Variable:
            return node.getVariableStatement().accept(this, mv);
        case Declaration:
            return node.getDeclaration().accept(this, mv);
        case DefaultHoistableDeclaration:
            return node.getHoistableDeclaration().accept(this, mv);
        case DefaultClassDeclaration: {
            ClassDeclaration decl = node.getClassDeclaration();
            /* steps 1-2 */
            BindingClassDeclarationEvaluation(decl, mv);
            /* steps 3-4 */
            if (decl.getIdentifier() == null) {
                /* steps 4.a-c */
                SetFunctionName(decl, "default", mv);
                /* steps 4.d-f */
                InitializeBoundNameWithValue(decl.getName(), FunctionObject.class, mv);
            }
            /* step 5 */
            return Completion.Empty;
        }
        case DefaultExpression:
            return node.getExpression().accept(this, mv);
        default:
            throw new AssertionError();
        }
    }

    /**
     * 15.2.3 Exports
     * <p>
     * 15.2.3.11 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExportDefaultExpression node, StatementVisitor mv) {
        Expression expr = node.getExpression();
        /* steps 1-3 */
        expressionBoxed(expr, mv);
        /* step 4 */
        if (IsAnonymousFunctionDefinition(expr)) {
            SetFunctionName(expr, "default", mv);
        }
        /* steps 5-6 */
        InitializeBoundNameWithValue(node.getBinding().getName(), Object.class, mv);
        /* step 7 */
        return Completion.Empty;
    }

    /**
     * 13.5 Expression Statement
     * <p>
     * 13.5.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ExpressionStatement node, StatementVisitor mv) {
        boolean hasCompletion = mv.hasCompletion() && node.hasCompletionValue();
        Expression expr = node.getExpression();

        /* steps 1-2 */
        if (hasCompletion) {
            ValType type = expression(expr, mv);
            mv.storeCompletionValue(type);
        } else {
            ValType type = expression(expr.emptyCompletion(), mv);
            mv.pop(type);
        }
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
     * 13.7.5 The for-in and for-of Statements
     * <p>
     * 13.1.8 Runtime Semantics: Evaluation<br>
     * 13.1.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.7.5.11 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForInStatement node, StatementVisitor mv) {
        return visitForInOfLoop(node, IterationKind.Enumerate, mv);
    }

    /**
     * 13.7.5 The for-in and for-of Statements
     * <p>
     * 13.1.8 Runtime Semantics: Evaluation<br>
     * 13.1.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.7.5.11 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForOfStatement node, StatementVisitor mv) {
        return visitForInOfLoop(node, IterationKind.Iterate, mv);
    }

    /**
     * 13.7.5.11 Runtime Semantics: LabelledEvaluation
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
     * 13.7.5.12 Runtime Semantics: ForIn/OfHeadEvaluation (TDZnames, expr, iterationKind, labelSet)
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
        BlockScope scope = node.getScope();
        Node lhs = node.getHead();
        List<Name> tdzNames = null;
        if (lhs instanceof LexicalDeclaration) {
            tdzNames = BoundNames(forDeclarationBinding((LexicalDeclaration) lhs));
            assert scope.isPresent() == !tdzNames.isEmpty();
            if (scope.isPresent()) {
                // stack: [] -> [TDZ]
                newDeclarativeEnvironment(scope, mv);
                mv.enterVariableScope();
                Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                        DeclarativeEnvironmentRecord.class);
                getEnvRec(envRec, mv);

                // stack: [TDZ] -> [TDZ]
                for (Name name : tdzNames) {
                    // FIXME: spec bug (CreateMutableBinding concrete method of `TDZ`)
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
                    op.createMutableBinding(envRec, name, false, mv);
                }
                mv.exitVariableScope();
                // stack: [TDZ] -> []
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(node);
        }

        /* steps 3, 5-6 */
        Expression expr = node.getExpression();
        ValType type = expressionBoxed(expr, mv);

        /* step 4 */
        if (tdzNames != null) {
            mv.exitScope();
            if (scope.isPresent()) {
                popLexicalEnvironment(mv);
            }
        }

        /* steps 7-8 */
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
            /* steps 7.b-c */
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
            /* step 8 */
            assert iterationKind == IterationKind.Iterate;
            mv.loadExecutionContext();
            mv.lineInfo(expr);
            mv.invoke(Methods.ScriptRuntime_iterate);
        }

        return type;
    }

    /**
     * 13.7.5.13 Runtime Semantics: ForIn/OfBodyEvaluation (lhs, stmt, iterator, lhsKind, labelSet)
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
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();
        Jump enter = new Jump(), test = new Jump();

        mv.enterVariableScope();
        Variable<ScriptIterator<?>> iterator = mv.newVariable("iter", ScriptIterator.class)
                .uncheckedCast();
        // stack: [Iterator] -> []
        mv.store(iterator);
        final Variable<Object> nextValue = mv.newVariable("nextValue", Object.class);
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        /* step 1 (not applicable) */
        /* step 2 */
        if (node.hasCompletionValue()) {
            mv.storeUndefinedAsCompletionValue();
        }
        /* steps 3-4 (not applicable) */
        /* step 5 (repeat loop) */
        mv.nonDestructiveGoTo(test);

        /* steps 5.d-e */
        mv.mark(enter);
        mv.load(iterator);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_next);
        mv.store(nextValue);

        /* steps 5.f-l */
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            mv.enterWrapped();
            new IterationGenerator<FORSTATEMENT, StatementVisitor>(codegen) {
                @Override
                protected Completion iterationBody(FORSTATEMENT node,
                        Variable<ScriptIterator<?>> iterator, StatementVisitor mv) {
                    return ForInOfBodyEvaluationInner(node, nextValue, mv);
                }

                @Override
                protected Variable<Object> enterIteration(FORSTATEMENT node, StatementVisitor mv) {
                    return mv.enterIterationBody(node);
                }

                @Override
                protected List<TempLabel> exitIteration(FORSTATEMENT node, StatementVisitor mv) {
                    return mv.exitIterationBody(node);
                }
            }.generate(node, iterator, test, mv);
            mv.exitWrapped();
            mv.exitIteration(node);
        }

        /* steps 5.m-n */
        if (lblContinue.isTarget()) {
            mv.mark(lblContinue);
            restoreEnvironment(savedEnv, mv);
        }

        /* steps 5.a-c */
        mv.mark(test);
        mv.load(iterator);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(enter);

        /* steps 5.m-n */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(savedEnv, mv);
        }
        mv.exitVariableScope();

        return Completion.Normal;
    }

    private <FORSTATEMENT extends IterationStatement & ForIterationNode> Completion ForInOfBodyEvaluationInner(
            FORSTATEMENT node, Variable<Object> nextValue, StatementVisitor mv) {
        BlockScope scope = node.getScope();
        Node lhs = node.getHead();

        /* steps 5.f-j */
        if (lhs instanceof Expression) {
            /* steps 5.f, 5.h-j */
            LeftHandSideExpression lhsExpr = (LeftHandSideExpression) lhs;
            if (!(lhsExpr instanceof AssignmentPattern)) {
                ReferenceOp<LeftHandSideExpression> op = ReferenceOp.of(lhsExpr);

                /* step 5.f.i.1 */
                // stack: [] -> [<ref>]
                ValType ref = op.reference(lhsExpr, mv, codegen);

                /* steps 5.h.i, 5.h.iii */
                // stack: [<ref>] -> []
                mv.load(nextValue);
                op.putValue(lhsExpr, ref, ValType.Any, mv);
            } else {
                /* step 5.i.i */
                mv.load(nextValue);
                DestructuringAssignment(codegen, (AssignmentPattern) lhs, mv);
            }
        } else if (lhs instanceof VariableStatement) {
            /* steps 5.f, 5.h-j */
            Binding binding = forVarDeclarationBinding((VariableStatement) lhs);
            if (binding instanceof BindingIdentifier) {
                BindingIdentifier bindingId = (BindingIdentifier) binding;

                /* step 5.f.i.1 */
                // 13.7.5.14 Runtime Semantics: Evaluation
                IdReferenceOp op = IdReferenceOp.of(bindingId);
                op.resolveBinding(bindingId, mv);

                /* steps 5.h.i, 5.h.iii */
                // stack: [<ref>] -> []
                mv.load(nextValue);
                op.putValue(bindingId, ValType.Any, mv);
            } else {
                /* step 5.i.ii */
                mv.load(nextValue);
                BindingInitialization(codegen, (BindingPattern) binding, mv);
            }
        } else {
            /* step 5.g-j */
            Binding binding = forDeclarationBinding((LexicalDeclaration) lhs);
            Variable<DeclarativeEnvironmentRecord> envRec = null;
            if (scope.isPresent()) {
                mv.enterVariableScope();
                envRec = mv.newVariable("envRec", DeclarativeEnvironmentRecord.class);

                newDeclarativeEnvironment(scope, mv);
                getEnvRec(envRec, mv);
                BindingInstantiation(envRec, (LexicalDeclaration) lhs, mv);
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(node);
            /* step 5.h */
            if (binding instanceof BindingIdentifier) {
                /* step 5.h.ii */
                BindingIdentifier bindingId = (BindingIdentifier) binding;
                Name name = bindingId.getName();
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);

                op.initializeBinding(envRec, name, nextValue, mv);
            } else {
                /* step 5.i.iii */
                // 13.7.5.9 Runtime Semantics: BindingInitialization
                BindingInitialization(codegen, envRec, (BindingPattern) binding, nextValue, mv);
            }
            if (scope.isPresent()) {
                mv.exitVariableScope();
            }
        }
        /* step 5.k */
        Completion result = node.getStatement().accept(this, mv);
        /* step 5.l */
        if (lhs instanceof LexicalDeclaration) {
            mv.exitScope();
            if (scope.isPresent() && !result.isAbrupt()) {
                popLexicalEnvironment(mv);
            }
        }
        return result;
    }

    private static Binding forVarDeclarationBinding(VariableStatement lhs) {
        assert ((VariableStatement) lhs).getElements().size() == 1;
        return ((VariableStatement) lhs).getElements().get(0).getBinding();
    }

    private static Binding forDeclarationBinding(LexicalDeclaration lhs) {
        assert ((LexicalDeclaration) lhs).getElements().size() == 1;
        return ((LexicalDeclaration) lhs).getElements().get(0).getBinding();
    }

    /**
     * 13.7.5.10 Runtime Semantics: BindingInstantiation
     */
    private void BindingInstantiation(Variable<DeclarativeEnvironmentRecord> envRec,
            LexicalDeclaration declaration, StatementVisitor mv) {
        boolean isConst = IsConstantDeclaration(declaration);
        for (Name name : BoundNames(forDeclarationBinding(declaration))) {
            BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
            if (isConst) {
                // FIXME: spec bug (CreateImmutableBinding concrete method of `env`)
                op.createImmutableBinding(envRec, name, true, mv);
            } else {
                // FIXME: spec bug (CreateMutableBinding concrete method of `env`)
                op.createMutableBinding(envRec, name, false, mv);
            }
        }
    }

    /**
     * 13.7.4 The for Statement
     * <p>
     * 13.1.8 Runtime Semantics: Evaluation<br>
     * 13.1.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.7.4.7 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(ForStatement node, StatementVisitor mv) {
        boolean perIterationsLets = false;
        BlockScope scope = node.getScope();
        Node head = node.getHead();
        if (head == null) {
            // empty
        } else if (head instanceof Expression) {
            ValType type = expression(((Expression) head).emptyCompletion(), mv);
            mv.pop(type);
        } else if (head instanceof VariableStatement) {
            head.accept(this, mv);
        } else {
            assert head instanceof LexicalDeclaration;
            LexicalDeclaration lexDecl = (LexicalDeclaration) head;
            List<Name> boundNames = BoundNames(lexDecl);
            boolean isConst = IsConstantDeclaration(lexDecl);
            perIterationsLets = !isConst && !boundNames.isEmpty();

            if (scope.isPresent()) {
                // stack: [] -> [loopEnv]
                newDeclarativeEnvironment(scope, mv);
                // stack: [loopEnv] -> [loopEnv]
                mv.enterVariableScope();
                Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                        DeclarativeEnvironmentRecord.class);
                getEnvRec(envRec, mv);

                // stack: [loopEnv] -> [loopEnv]
                for (Name dn : boundNames) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, dn);
                    if (isConst) {
                        // FIXME: spec bug (CreateImmutableBinding concrete method of `loopEnv`)
                        op.createImmutableBinding(envRec, dn, true, mv);
                    } else {
                        // FIXME: spec bug (CreateMutableBinding concrete method of `loopEnv`)
                        op.createMutableBinding(envRec, dn, false, mv);
                    }
                }
                mv.exitVariableScope();
                // stack: [loopEnv] -> []
                pushLexicalEnvironment(mv);
            }
            mv.enterScope(node);

            lexDecl.accept(this, mv);
        }

        Completion result = ForBodyEvaluation(node, perIterationsLets, mv);

        if (head instanceof LexicalDeclaration) {
            mv.exitScope();
            if (scope.isPresent() && !result.isAbrupt()) {
                popLexicalEnvironment(mv);
            }
        }
        return result;
    }

    /**
     * 13.7.4.8 Runtime Semantics: ForBodyEvaluation(test, increment, stmt, perIterationBindings,
     * labelSet)
     */
    private Completion ForBodyEvaluation(ForStatement node, boolean perIterationsLets,
            StatementVisitor mv) {
        mv.enterVariableScope();
        /* step 1 */
        if (node.hasCompletionValue()) {
            mv.storeUndefinedAsCompletionValue();
        }
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
        Bool btest = node.getTest() != null ? Bool.evaluate(node.getTest()) : Bool.True;

        /* steps 4.b-d */
        Completion result;
        if (btest != Bool.True) {
            mv.nonDestructiveGoTo(lblTest);
        }
        mv.mark(lblStmt);
        {
            mv.enterIteration(node, lblBreak, lblContinue);
            result = node.getStatement().accept(this, mv);
            mv.exitIteration(node);
        }

        /* step 4.c (abrupt completion - continue) */
        if (lblContinue.isTarget()) {
            mv.mark(lblContinue);
            restoreEnvironment(savedEnv, mv);
        }

        /* steps 4.e-f */
        if (perIterationsLets && (!result.isAbrupt() || lblContinue.isTarget())) {
            CreatePerIterationEnvironment(savedEnv, mv);
        }

        /* step 4.g */
        if (node.getStep() != null && (!result.isAbrupt() || lblContinue.isTarget())) {
            ValType type = expression(node.getStep().emptyCompletion(), mv);
            mv.pop(type);
        }

        /* step 4.a */
        if (btest != Bool.True) {
            mv.mark(lblTest);
            ValType type = expression(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblStmt);
        } else {
            mv.goTo(lblStmt);
        }

        /* step 4.c (abrupt completion - break) */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(savedEnv, mv);
        }
        mv.exitVariableScope();

        if (btest == Bool.True) {
            if (!result.isAbrupt() && !lblBreak.isTarget()) {
                return Completion.Abrupt; // infinite loop
            }
            return result.normal(lblBreak.isTarget());
        }
        return Completion.Normal;
    }

    /**
     * 13.7.4.9 Runtime Semantics: CreatePerIterationEnvironment( perIterationBindings )
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
        /* step 2 (not applicable) */
    }

    /**
     * 14.1.20 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(FunctionDeclaration node, final StatementVisitor mv) {
        codegen.compile(node);

        /* B.3.3 Block-Level Function Declarations Web Legacy Compatibility Semantics */
        if (node.isLegacyBlockScoped()) {
            final Name name = node.getIdentifier().getName();
            TopLevelScope top = mv.getScope().getTop();
            assert top instanceof FunctionScope;
            Name varName = ((FunctionScope) top).variableScope().resolveName(name, false);
            assert varName != null && name != varName;
            /* step 1.a.ii.3.1 */
            Value<DeclarativeEnvironmentRecord> fenv = new Value<DeclarativeEnvironmentRecord>() {
                @Override
                protected void load(InstructionAssembler assembler) {
                    getVariableEnvironmentRecord(Types.DeclarativeEnvironmentRecord, mv);
                }
            };
            /* steps 1.a.ii.3.5-6 */
            BindingOp.of(fenv, varName).setMutableBinding(fenv, varName, new Value<Object>() {
                @Override
                protected void load(InstructionAssembler assembler) {
                    /* step 1.a.ii.3.2 */
                    Value<DeclarativeEnvironmentRecord> benv = new Value<DeclarativeEnvironmentRecord>() {
                        @Override
                        protected void load(InstructionAssembler assembler) {
                            getLexicalEnvironmentRecord(Types.DeclarativeEnvironmentRecord, mv);
                        }
                    };
                    /* steps 1.a.ii.3.3-4 */
                    BindingOp.of(benv, name).getBindingValue(benv, name, false, mv);
                }
            }, false, mv);
        }

        /* step 1 */
        return Completion.Empty;
    }

    /**
     * 13.1.8 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(GeneratorDeclaration node, StatementVisitor mv) {
        codegen.compile(node);
        /* step 1 */
        return Completion.Empty;
    }

    /**
     * 13.6 The if Statement
     * <p>
     * 13.6.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(IfStatement node, StatementVisitor mv) {
        Bool btest = Bool.evaluate(node.getTest());
        if (btest != Bool.Any) {
            if (btest == Bool.True) {
                Completion resultThen = node.getThen().accept(this, mv);
                if (node.hasCompletionValue() && resultThen == Completion.Empty) {
                    mv.storeUndefinedAsCompletionValue();
                }
                return resultThen.nonEmpty();
            }
            if (node.getOtherwise() != null) {
                Completion resultOtherwise = node.getOtherwise().accept(this, mv);
                if (node.hasCompletionValue() && resultOtherwise == Completion.Empty) {
                    mv.storeUndefinedAsCompletionValue();
                }
                return resultOtherwise.nonEmpty();
            }
            if (node.hasCompletionValue()) {
                mv.storeUndefinedAsCompletionValue();
            }
            return Completion.Normal;
        }

        /* steps 1-3 */
        ValType type = expression(node.getTest(), mv);
        ToBoolean(type, mv);
        if (node.getOtherwise() != null) {
            // IfStatement : if ( Expression ) Statement else Statement
            Jump l0 = new Jump(), l1 = new Jump();

            /* step 4 */
            mv.ifeq(l0);
            Completion resultThen = node.getThen().accept(this, mv);
            if (node.hasCompletionValue() && resultThen == Completion.Empty) {
                mv.storeUndefinedAsCompletionValue();
            }
            if (!resultThen.isAbrupt()) {
                mv.goTo(l1);
            }

            /* step 5 */
            mv.mark(l0);
            Completion resultOtherwise = node.getOtherwise().accept(this, mv);
            if (node.hasCompletionValue() && resultOtherwise == Completion.Empty) {
                mv.storeUndefinedAsCompletionValue();
            }
            if (!resultThen.isAbrupt()) {
                mv.mark(l1);
            }

            /* steps 6-8 */
            return resultThen.select(resultOtherwise);
        } else {
            // IfStatement : if ( Expression ) Statement
            Jump l0 = new Jump();

            /* step 5 */
            mv.ifeq(l0);
            Completion resultThen = node.getThen().accept(this, mv);
            if (node.hasCompletionValue() && mv.hasCompletion()) {
                if (resultThen == Completion.Normal) {
                    Jump l1 = new Jump();
                    mv.goTo(l1);
                    mv.mark(l0);
                    mv.storeUndefinedAsCompletionValue();
                    mv.mark(l1);
                } else {
                    mv.mark(l0);
                    mv.storeUndefinedAsCompletionValue();
                }
            } else {
                mv.mark(l0);
            }

            /* steps 4-5 */
            return resultThen.select(Completion.Normal);
        }
    }

    /**
     * 15.2.1.20 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ImportDeclaration node, StatementVisitor mv) {
        return Completion.Empty;
    }

    /**
     * 13.13 Labelled Statements
     * <p>
     * 13.13.15 Runtime Semantics: Evaluation<br>
     * 13.13.14 Runtime Semantics: LabelledEvaluation
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
            restoreEnvironment(savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 4-5 */
        return result.normalOrEmpty(label.isTarget());
    }

    /**
     * 13.13 Labelled Statements
     * <p>
     * 13.13.14 Runtime Semantics: LabelledEvaluation<br>
     * 
     * <code>LabelledItem: FunctionDeclaration</code>
     */
    @Override
    public Completion visit(LabelledFunctionStatement node, StatementVisitor mv) {
        return node.getFunction().accept(this, mv);
    }

    /**
     * 13.3.1 Let and Const Declarations
     * <p>
     * 13.3.1.4 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(LexicalDeclaration node, StatementVisitor mv) {
        mv.enterVariableScope();
        Class<? extends EnvironmentRecord> envRecClass = getEnvironmentRecordClass(mv);
        Variable<? extends EnvironmentRecord> envRec = mv.newVariable("envRec", envRecClass);
        getLexicalEnvironmentRecord(envRec, mv);
        /* steps 1-2 */
        for (LexicalBinding lexical : node.getElements()) {
            Binding binding = lexical.getBinding();
            Expression initializer = lexical.getInitializer();
            if (initializer == null) {
                // LexicalBinding : BindingIdentifier
                assert binding instanceof BindingIdentifier;
                Name name = ((BindingIdentifier) binding).getName();
                /* steps 1-2 */
                assert mv.getScope().isDeclared(name);
                InitializeBoundNameWithUndefined(envRec, name, mv);
            } else if (binding instanceof BindingIdentifier) {
                // LexicalBinding : BindingIdentifier Initializer
                Name name = ((BindingIdentifier) binding).getName();
                /* steps 1-7 */
                InitializeBoundNameWithInitializer(codegen, envRec, name, initializer, mv);
            } else {
                // LexicalBinding : BindingPattern Initializer
                assert binding instanceof BindingPattern;
                /* steps 1-3 */
                expressionBoxed(initializer, mv);
                /* steps 4-5 */
                BindingInitialization(codegen, envRec, (BindingPattern) binding, mv);
            }
        }
        mv.exitVariableScope();
        /* step 3 */
        return Completion.Empty;
    }

    /**
     * Extension: 'let' statement
     */
    @Override
    public Completion visit(LetStatement node, StatementVisitor mv) {
        BlockScope scope = node.getScope();
        if (scope.isPresent()) {
            mv.enterVariableScope();
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> env = mv.newVariable("env",
                    LexicalEnvironment.class).uncheckedCast();
            Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                    DeclarativeEnvironmentRecord.class);

            newDeclarativeEnvironment(scope, mv);
            mv.store(env);
            getEnvRec(env, envRec, mv);

            for (LexicalBinding lexical : node.getBindings()) {
                Binding binding = lexical.getBinding();
                Expression initializer = lexical.getInitializer();
                for (Name name : BoundNames(binding)) {
                    BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
                    op.createMutableBinding(envRec, name, false, mv);
                }
                if (initializer == null) {
                    // LexicalBinding : BindingIdentifier
                    assert binding instanceof BindingIdentifier;
                    Name name = ((BindingIdentifier) binding).getName();
                    /* steps 1-2 */
                    InitializeBoundNameWithUndefined(envRec, name, mv);
                } else if (binding instanceof BindingIdentifier) {
                    // LexicalBinding : BindingIdentifier Initializer
                    Name name = ((BindingIdentifier) binding).getName();
                    /* steps 1-7 */
                    InitializeBoundNameWithInitializer(codegen, envRec, name, initializer, mv);
                } else {
                    // LexicalBinding : BindingPattern Initializer
                    assert binding instanceof BindingPattern;
                    /* steps 1-3 */
                    expressionBoxed(initializer, mv);
                    /* steps 4-5 */
                    BindingInitialization(codegen, envRec, (BindingPattern) binding, mv);
                }
            }

            mv.load(env);
            pushLexicalEnvironment(mv);

            mv.exitVariableScope();
        }

        mv.enterScope(node);
        Completion result = node.getStatement().accept(this, mv);
        mv.exitScope();

        if (scope.isPresent() && !result.isAbrupt()) {
            popLexicalEnvironment(mv);
        }
        return result;
    }

    /**
     * 13.10 The return Statement
     * <p>
     * 13.10.1 Runtime Semantics: Evaluation
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
            mv.enterTailCallPosition(expr);
            expressionBoxed(expr, mv);
            mv.exitTailCallPosition();
        }
        /* step 1/4 */
        mv.returnCompletion();
        return Completion.Return;
    }

    @Override
    public Completion visit(StatementListMethod node, StatementVisitor mv) {
        Completion result = codegen.compile(node, mv);
        assert !(result == Completion.Break || result == Completion.Continue);

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

            return Completion.Empty;
        }

        mv.storeCompletionValue(ValType.Any);
        assert result != Completion.Return;
        return result;
    }

    /**
     * 13.12 The switch Statement
     * <p>
     * 13.12.11 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(SwitchStatement node, StatementVisitor mv) {
        if (node.hasCompletionValue()) {
            mv.storeUndefinedAsCompletionValue();
        }
        return node.accept(new SwitchStatementGenerator(codegen), mv);
    }

    /**
     * 13.14 The throw Statement
     * <p>
     * 13.14.1 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(ThrowStatement node, StatementVisitor mv) {
        /* steps 1-3 */
        expressionBoxed(node.getExpression(), mv);
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptException_create);

        /* step 4 */
        mv.athrow();
        return Completion.Throw;
    }

    /**
     * 13.15 The try Statement
     * <p>
     * 13.15.8 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(TryStatement node, StatementVisitor mv) {
        boolean hasCatch = node.getCatchNode() != null || !node.getGuardedCatchNodes().isEmpty();
        if (node.hasCompletionValue()) {
            mv.storeUndefinedAsCompletionValue();
        }
        if (hasCatch && node.getFinallyBlock() != null) {
            return visitTryCatchFinally(node, mv);
        } else if (hasCatch) {
            return visitTryCatch(node, mv);
        } else {
            return visitTryFinally(node, mv);
        }
    }

    /**
     * 13.15.8 Runtime Semantics: Evaluation<br>
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
        Variable<Object> completion = mv.enterFinallyScoped(node);

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

        /* steps 5-8 */
        return finallyResult.then(tryResult.select(catchResult));
    }

    /**
     * 13.15.8 Runtime Semantics: Evaluation<br>
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

        /* step 2 */
        // Emit catch-block
        Completion catchResult = emitCatchBlock(node, savedEnv, handlerCatch,
                handlerCatchStackOverflow, mv);

        /* step 3 */
        if (!tryResult.isAbrupt()) {
            mv.mark(exceptionHandled);
        }

        mv.exitVariableScope();
        mv.tryCatch(startCatch, endCatch, handlerCatch, Types.ScriptException);
        mv.tryCatch(startCatch, endCatch, handlerCatchStackOverflow, Types.Error);

        /* steps 4-6 */
        return tryResult.select(catchResult);
    }

    /**
     * 13.15.8 Runtime Semantics: Evaluation<br>
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
        Variable<Object> completion = mv.enterFinallyScoped(node);

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

        /* steps 3-6 */
        return finallyResult.then(tryResult);
    }

    private Completion emitTryBlock(TryStatement node, Jump noException, StatementVisitor mv) {
        mv.enterWrapped();
        Completion tryResult = node.getTryBlock().accept(this, mv);
        mv.exitWrapped();
        if (!tryResult.isAbrupt()) {
            mv.goTo(noException);
        }
        return tryResult.nonEmpty();
    }

    private Completion emitCatchBlock(TryStatement node, Variable<LexicalEnvironment<?>> savedEnv,
            TryCatchLabel handlerCatch, TryCatchLabel handlerCatchStackOverflow, StatementVisitor mv) {
        boolean hasFinally = node.getFinallyBlock() != null;
        CatchNode catchNode = node.getCatchNode();
        List<GuardedCatchNode> guardedCatchNodes = node.getGuardedCatchNodes();
        assert catchNode != null || !guardedCatchNodes.isEmpty();

        // StackOverflowError -> ScriptException
        mv.catchHandler(handlerCatchStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_stackOverflowError);
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_toInternalError);

        mv.catchHandler(handlerCatch, Types.ScriptException);
        restoreEnvironment(savedEnv, mv);
        if (hasFinally) {
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
        if (hasFinally) {
            mv.exitWrapped();
        }
        return catchResult.nonEmpty();
    }

    private Completion emitFinallyBlock(TryStatement node,
            Variable<LexicalEnvironment<?>> savedEnv, Variable<Object> completion,
            Completion tryResult, Completion catchResult, TryCatchLabel handlerFinally,
            TryCatchLabel handlerFinallyStackOverflow, TryCatchLabel handlerReturn,
            Jump noException, List<TempLabel> tempLabels, StatementVisitor mv) {
        BlockStatement finallyBlock = node.getFinallyBlock();
        assert finallyBlock != null;

        // various finally blocks (1 - 4)
        // (1) finally block for abrupt throw completions within 'try-catch'
        mv.enterVariableScope();
        Variable<Throwable> throwable = mv.newVariable("throwable", Throwable.class);
        mv.catchHandler(handlerFinallyStackOverflow, Types.Error);
        mv.invoke(Methods.ScriptRuntime_stackOverflowError);
        mv.catchHandler(handlerFinally, Types.ScriptException);
        if (handlerReturn != null) {
            mv.catchHandler(handlerReturn, Types.ReturnValue);
        }
        mv.store(throwable);
        restoreEnvironment(savedEnv, mv);
        Completion finallyResult = finallyBlock.accept(this, mv);
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
            finallyBlock.accept(this, mv);
            if (!finallyResult.isAbrupt()) {
                if (node.hasCompletionValue()) {
                    mv.storeCompletionValue(completion);
                }
                if (!tempLabels.isEmpty()) {
                    exceptionHandled = new Jump();
                    mv.goTo(exceptionHandled);
                }
            }
        }

        // (4) finally blocks for other abrupt completion (return, break, continue)
        for (TempLabel temp : tempLabels) {
            if (temp.isTarget()) {
                mv.mark(temp);
                restoreEnvironment(savedEnv, mv);
                finallyBlock.accept(this, mv);
                if (!finallyResult.isAbrupt()) {
                    if (node.hasCompletionValue()) {
                        mv.storeCompletionValue(completion);
                    }
                    mv.goTo(temp, completion);
                }
            }
        }

        if (exceptionHandled != null) {
            mv.mark(exceptionHandled);
        }

        return finallyResult.nonEmpty();
    }

    /**
     * 13.15.7 Runtime Semantics: CatchClauseEvaluation
     */
    @Override
    public Completion visit(CatchNode node, StatementVisitor mv) {
        Binding catchParameter = node.getCatchParameter();
        BlockStatement catchBlock = node.getCatchBlock();

        /* steps 1-6 */
        // stack: [e] -> []
        mv.enterVariableScope();
        {
            Variable<Object> exception = mv.newVariable("exception", Object.class);
            mv.invoke(Methods.ScriptException_getValue);
            mv.store(exception);

            /* step 1 (not applicable) */
            /* step 2 */
            // stack: [] -> [catchEnv]
            BlockScope scope = node.getScope();
            newCatchDeclarativeEnvironment(scope, mv);
            Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                    DeclarativeEnvironmentRecord.class);
            getEnvRec(envRec, mv);

            /* step 3 */
            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            for (Name name : BoundNames(catchParameter)) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
                op.createMutableBinding(envRec, name, false, mv);
            }

            /* step 4 */
            // stack: [catchEnv] -> []
            pushLexicalEnvironment(mv);
            mv.enterScope(node);

            /* steps 5-6 */
            // stack: [ex] -> []
            BindingInitialization(codegen, envRec, catchParameter, exception, mv);
        }
        mv.exitVariableScope();

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

        /* steps 1-6 */
        // stack: [e] -> []
        mv.enterVariableScope();
        {
            Variable<Object> exception = mv.newVariable("exception", Object.class);
            mv.invoke(Methods.ScriptException_getValue);
            mv.store(exception);

            /* step 1 (not applicable) */
            /* step 2 */
            // stack: [] -> [catchEnv]
            BlockScope scope = node.getScope();
            newCatchDeclarativeEnvironment(scope, mv);
            Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                    DeclarativeEnvironmentRecord.class);
            getEnvRec(envRec, mv);

            /* step 3 */
            // FIXME: spec bug (CreateMutableBinding concrete method of `catchEnv`)
            for (Name name : BoundNames(catchParameter)) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
                op.createMutableBinding(envRec, name, false, mv);
            }

            /* step 4 */
            // stack: [catchEnv] -> []
            pushLexicalEnvironment(mv);
            mv.enterScope(node);

            /* steps 5-6 */
            // stack: [] -> []
            BindingInitialization(codegen, envRec, catchParameter, exception, mv);
        }
        mv.exitVariableScope();

        /* step 7 */
        Completion result;
        ToBoolean(expression(node.getGuard(), mv), mv);
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
     * 13.3.2 Variable Statement
     * <p>
     * 13.3.2.4 Runtime Semantics: Evaluation
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
            /* steps 2-3 */
            IdReferenceOp op = IdReferenceOp.of(bindingId);
            op.resolveBinding(bindingId, mv);
            /* steps 4-6 */
            ValType type = expression(initializer, mv);
            /* step 7 */
            if (IsAnonymousFunctionDefinition(initializer)) {
                SetFunctionName(initializer, bindingId.getName(), mv);
            }
            /* step 8 */
            op.putValue(bindingId, type, mv);
        } else {
            // VariableDeclaration : BindingPattern Initializer
            assert binding instanceof BindingPattern;
            /* steps 1-3 */
            expressionBoxed(initializer, mv);
            /* step 4 */
            BindingInitialization(codegen, (BindingPattern) binding, mv);
        }
        return Completion.Empty;
    }

    /**
     * 13.3.2 Variable Statement
     * <p>
     * 13.3.2.4 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(VariableStatement node, StatementVisitor mv) {
        /* steps 1-2 */
        for (VariableDeclaration decl : node.getElements()) {
            decl.accept(this, mv);
        }
        /* step 3 */
        return Completion.Empty;
    }

    /**
     * 13.7.3 The while Statement
     * <p>
     * 13.1.8 Runtime Semantics: Evaluation<br>
     * 13.1.7 Runtime Semantics: LabelledEvaluation<br>
     * 13.7.3.6 Runtime Semantics: LabelledEvaluation
     */
    @Override
    public Completion visit(WhileStatement node, StatementVisitor mv) {
        Jump lblNext = new Jump(), lblTest = new Jump();
        ContinueLabel lblContinue = new ContinueLabel();
        BreakLabel lblBreak = new BreakLabel();
        Bool btest = Bool.evaluate(node.getTest());

        mv.enterVariableScope();
        Variable<LexicalEnvironment<?>> savedEnv = saveEnvironment(node, mv);

        /* step 1 */
        if (node.hasCompletionValue()) {
            mv.storeUndefinedAsCompletionValue();
        }
        /* step 2 (repeat loop) */
        if (btest != Bool.True) {
            mv.nonDestructiveGoTo(lblTest);
        }
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
            restoreEnvironment(savedEnv, mv);
        }

        /* steps 2.a-d */
        if (btest != Bool.True) {
            mv.mark(lblTest);
            ValType type = expression(node.getTest(), mv);
            ToBoolean(type, mv);
            mv.ifne(lblNext);
        } else if (!result.isAbrupt() || lblContinue.isTarget()) {
            mv.goTo(lblNext);
        }

        /* step 2.f (abrupt completion - break) */
        if (lblBreak.isTarget()) {
            mv.mark(lblBreak);
            restoreEnvironment(savedEnv, mv);
        }
        mv.exitVariableScope();

        /* steps 2.d, 2.f */
        if (btest == Bool.True) {
            if (!result.isAbrupt() && !lblBreak.isTarget()) {
                return Completion.Abrupt; // infinite loop
            }
            return result.normal(lblBreak.isTarget());
        }
        return Completion.Normal;
    }

    /**
     * 13.11 The with Statement
     * <p>
     * 13.11.7 Runtime Semantics: Evaluation
     */
    @Override
    public Completion visit(WithStatement node, StatementVisitor mv) {
        /* step 1 */
        ValType type = expression(node.getExpression(), mv);

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
            if (node.hasCompletionValue() && result == Completion.Empty) {
                mv.storeUndefinedAsCompletionValue();
            }
        }

        /* steps 10-11 */
        return result.nonEmpty();
    }
}
