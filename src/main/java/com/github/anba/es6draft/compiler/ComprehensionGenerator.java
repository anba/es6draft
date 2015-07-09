/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.BindingInitialization;
import static com.github.anba.es6draft.compiler.BindingInitializationGenerator.InitializeBoundNameWithUndefined;
import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyDeclaredNames;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.ast.Comprehension;
import com.github.anba.es6draft.ast.ComprehensionFor;
import com.github.anba.es6draft.ast.ComprehensionIf;
import com.github.anba.es6draft.ast.ComprehensionQualifier;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.LegacyComprehension;
import com.github.anba.es6draft.ast.LegacyComprehensionFor;
import com.github.anba.es6draft.ast.LegacyComprehensionFor.IterationKind;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.scope.BlockScope;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.Labels.TempLabel;
import com.github.anba.es6draft.compiler.StatementGenerator.Completion;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>Array Comprehension
 * </ul>
 */
abstract class ComprehensionGenerator extends DefaultCodeGenerator<Void, ExpressionVisitor> {
    private static final class Methods {
        // class: GeneratorObject
        static final MethodName GeneratorObject_isLegacyGenerator = MethodName.findVirtual(
                Types.GeneratorObject, "isLegacyGenerator", Type.methodType(Type.BOOLEAN_TYPE));

        // class: Iterator
        static final MethodName Iterator_hasNext = MethodName.findInterface(Types.Iterator,
                "hasNext", Type.methodType(Type.BOOLEAN_TYPE));

        static final MethodName Iterator_next = MethodName.findInterface(Types.Iterator, "next",
                Type.methodType(Types.Object));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_enumerate = MethodName.findStatic(
                Types.ScriptRuntime, "enumerate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_enumerateValues = MethodName.findStatic(
                Types.ScriptRuntime, "enumerateValues",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodName ScriptRuntime_iterate = MethodName.findStatic(Types.ScriptRuntime,
                "iterate",
                Type.methodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));
    }

    private Iterator<Node> elements;
    private Iterator<Variable<ScriptIterator<?>>> iterators;

    protected ComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Void visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionTail : AssignmentExpression
     */
    @Override
    protected abstract Void visit(Expression node, ExpressionVisitor mv);

    /**
     * Runtime Semantics: ComprehensionEvaluation
     */
    @Override
    public Void visit(Comprehension node, ExpressionVisitor mv) {
        ArrayList<Node> list = new ArrayList<>(node.getList().size() + 1);
        list.addAll(node.getList());
        list.add(node.getExpression());
        elements = list.iterator();

        // Create variables early so they'll appear next to each other in the local variable map.
        ArrayList<Variable<ScriptIterator<?>>> iters = new ArrayList<>();
        for (ComprehensionQualifier e : node.getList()) {
            if (e instanceof ComprehensionFor || e instanceof LegacyComprehensionFor) {
                Variable<ScriptIterator<?>> iter = mv.newVariable("iter", ScriptIterator.class)
                        .uncheckedCast();
                iters.add(iter);
            }
        }
        iterators = iters.iterator();

        // Start generating code.
        elements.next().accept(this, mv);

        return null;
    }

    /**
     * Runtime Semantics: ComprehensionEvaluation
     */
    @Override
    public Void visit(LegacyComprehension node, ExpressionVisitor mv) {
        BlockScope scope = node.getScope();
        if (scope.isPresent()) {
            // stack: [] -> [env]
            newDeclarativeEnvironment(scope, mv);
            mv.enterVariableScope();
            Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                    DeclarativeEnvironmentRecord.class);
            getEnvRec(envRec, mv);

            for (Name name : LexicallyDeclaredNames(node.getScope())) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
                op.createMutableBinding(envRec, name, false, mv);

                InitializeBoundNameWithUndefined(envRec, name, mv);
            }
            mv.exitVariableScope();
            // stack: [env] -> []
            pushLexicalEnvironment(mv);
        }

        mv.enterScope(node);
        visit((Comprehension) node, mv);
        mv.exitScope();

        if (scope.isPresent()) {
            popLexicalEnvironment(mv);
        }

        return null;
    }

    /**
     * Runtime Semantics: ComprehensionComponentEvaluation
     * <p>
     * ComprehensionIf : if ( AssignmentExpression )
     */
    @Override
    public Void visit(ComprehensionIf node, ExpressionVisitor mv) {
        /* steps 1-2 */
        ValType type = expression(node.getTest(), mv);
        /* steps 3-4 */
        ToBoolean(type, mv);
        /* steps 5-6 */
        Jump lblTest = new Jump();
        mv.ifeq(lblTest);
        {
            /* step 5a */
            elements.next().accept(this, mv);
        }
        mv.mark(lblTest);

        return null;
    }

    /**
     * Runtime Semantics: ComprehensionComponentEvaluation
     * <p>
     * ComprehensionFor : for ( ForBinding of AssignmentExpression )
     */
    @Override
    public Void visit(ComprehensionFor node, ExpressionVisitor mv) {
        Jump lblTest = new Jump(), lblLoop = new Jump();
        Variable<ScriptIterator<?>> iter = iterators.next();

        /* steps 1-2 */
        expressionBoxed(node.getExpression(), mv);

        /* steps 3-4 */
        mv.loadExecutionContext();
        mv.lineInfo(node.getExpression());
        mv.invoke(Methods.ScriptRuntime_iterate);
        mv.store(iter);

        /* step 5 (not applicable) */

        /* step 6 */
        mv.nonDestructiveGoTo(lblTest);

        /* steps 6.d-e */
        mv.mark(lblLoop);
        mv.load(iter);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_next);

        /* steps 6.f-j */
        BlockScope scope = node.getScope();
        if (scope.isPresent()) {
            mv.enterVariableScope();

            // stack: [nextValue] -> []
            Variable<Object> nextValue = mv.newVariable("nextValue", Object.class);
            mv.store(nextValue);

            // stack: [] -> [forEnv]
            newDeclarativeEnvironment(scope, mv);

            Variable<DeclarativeEnvironmentRecord> envRec = mv.newVariable("envRec",
                    DeclarativeEnvironmentRecord.class);
            getEnvRec(envRec, mv);

            for (Name name : BoundNames(node.getBinding())) {
                BindingOp<DeclarativeEnvironmentRecord> op = BindingOp.of(envRec, name);
                op.createMutableBinding(envRec, name, false, mv);
            }

            BindingInitialization(codegen, envRec, node.getBinding(), nextValue, mv);

            // stack: [forEnv] -> []
            pushLexicalEnvironment(mv);
            mv.exitVariableScope();
        } else {
            // stack: [nextValue] -> []
            mv.pop();
        }

        /* step 6.k */
        mv.enterScope(node);
        new IterationGenerator<ComprehensionFor, ExpressionVisitor>(codegen) {
            @Override
            protected Completion iterationBody(ComprehensionFor node,
                    Variable<ScriptIterator<?>> iterator, ExpressionVisitor mv) {
                elements.next().accept(ComprehensionGenerator.this, mv);
                return Completion.Normal;
            }

            @Override
            protected Variable<Object> enterIteration(ComprehensionFor node, ExpressionVisitor mv) {
                return mv.enterIteration();
            }

            @Override
            protected List<TempLabel> exitIteration(ComprehensionFor node, ExpressionVisitor mv) {
                return mv.exitIteration();
            }
        }.generate(node, iter, mv);
        mv.exitScope();

        /* steps 6.l-m */
        if (scope.isPresent()) {
            popLexicalEnvironment(mv);
        }

        /* steps 6.a-c */
        mv.mark(lblTest);
        mv.load(iter);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(lblLoop);

        return null;
    }

    /**
     * Runtime Semantics: ComprehensionComponentEvaluation
     * <p>
     * ComprehensionFor : for ( ForBinding of AssignmentExpression )
     */
    @Override
    public Void visit(LegacyComprehensionFor node, ExpressionVisitor mv) {
        Jump lblTest = new Jump(), lblLoop = new Jump(), lblFail = new Jump();
        Variable<ScriptIterator<?>> iter = iterators.next();

        ValType type = expressionBoxed(node.getExpression(), mv);
        if (type != ValType.Object) {
            // fail-safe behaviour for null/undefined values in legacy comprehensions
            Jump loopstart = new Jump();
            mv.dup();
            isUndefinedOrNull(mv);
            mv.ifeq(loopstart);
            mv.pop();
            mv.goTo(lblFail);
            mv.mark(loopstart);
        }

        IterationKind iterationKind = node.getIterationKind();
        if (iterationKind == IterationKind.Enumerate
                || iterationKind == IterationKind.EnumerateValues) {
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
            mv.lineInfo(node.getExpression());
            mv.invoke(Methods.ScriptRuntime_iterate);
            mv.goTo(l1);
            mv.mark(l0);
            mv.loadExecutionContext();
            mv.lineInfo(node.getExpression());
            if (iterationKind == IterationKind.Enumerate) {
                mv.invoke(Methods.ScriptRuntime_enumerate);
            } else {
                mv.invoke(Methods.ScriptRuntime_enumerateValues);
            }
            mv.mark(l1);
        } else {
            assert iterationKind == IterationKind.Iterate;
            mv.loadExecutionContext();
            mv.lineInfo(node.getExpression());
            mv.invoke(Methods.ScriptRuntime_iterate);
        }
        mv.store(iter);

        mv.nonDestructiveGoTo(lblTest);
        mv.mark(lblLoop);
        mv.load(iter);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_next);

        new IterationGenerator<LegacyComprehensionFor, ExpressionVisitor>(codegen) {
            @Override
            protected Completion iterationBody(LegacyComprehensionFor node,
                    Variable<ScriptIterator<?>> iterator, ExpressionVisitor mv) {
                // stack: [nextValue] -> []
                BindingInitialization(codegen, node.getBinding(), mv);
                elements.next().accept(ComprehensionGenerator.this, mv);
                return Completion.Normal;
            }

            @Override
            protected Variable<Object> enterIteration(LegacyComprehensionFor node,
                    ExpressionVisitor mv) {
                return mv.enterIteration();
            }

            @Override
            protected List<TempLabel> exitIteration(LegacyComprehensionFor node,
                    ExpressionVisitor mv) {
                return mv.exitIteration();
            }
        }.generate(node, iter, mv);

        mv.mark(lblTest);
        mv.load(iter);
        mv.lineInfo(node);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(lblLoop);
        mv.mark(lblFail);

        return null;
    }
}
