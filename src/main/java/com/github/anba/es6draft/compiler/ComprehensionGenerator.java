/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyDeclaredNames;

import java.util.ArrayList;
import java.util.Iterator;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.BindingPattern;
import com.github.anba.es6draft.ast.Comprehension;
import com.github.anba.es6draft.ast.ComprehensionFor;
import com.github.anba.es6draft.ast.ComprehensionIf;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.LegacyComprehension;
import com.github.anba.es6draft.ast.LegacyComprehensionFor;
import com.github.anba.es6draft.ast.LegacyComprehensionFor.IterationKind;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;
import com.github.anba.es6draft.compiler.assembler.Variable;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.4.2 Array Comprehension
 * </ul>
 */
abstract class ComprehensionGenerator extends DefaultCodeGenerator<Void, ExpressionVisitor> {
    private static final class Methods {
        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.EnvironmentRecord, "createMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc EnvironmentRecord_initializeBinding = MethodDesc.create(
                MethodDesc.Invoke.Interface, Types.EnvironmentRecord, "initializeBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: GeneratorObject
        static final MethodDesc GeneratorObject_isLegacyGenerator = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.GeneratorObject, "isLegacyGenerator",
                Type.getMethodType(Type.BOOLEAN_TYPE));

        // class: Iterator
        static final MethodDesc Iterator_hasNext = MethodDesc.create(MethodDesc.Invoke.Interface,
                Types.Iterator, "hasNext", Type.getMethodType(Type.BOOLEAN_TYPE));

        static final MethodDesc Iterator_next = MethodDesc.create(MethodDesc.Invoke.Interface,
                Types.Iterator, "next", Type.getMethodType(Types.Object));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_ensureObject = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "ensureObject",
                Type.getMethodType(Types.ScriptObject, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_enumerate = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "enumerate",
                Type.getMethodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_enumerateValues = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.ScriptRuntime, "enumerateValues",
                Type.getMethodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));

        static final MethodDesc ScriptRuntime_iterate = MethodDesc.create(MethodDesc.Invoke.Static,
                Types.ScriptRuntime, "iterate",
                Type.getMethodType(Types.ScriptIterator, Types.Object, Types.ExecutionContext));
    }

    private Iterator<Node> elements;

    private Iterator<Variable<Iterator<?>>> iterators;

    protected ComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Void visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 12.2.4.2.3 Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionTail : AssignmentExpression
     */
    @Override
    protected abstract Void visit(Expression node, ExpressionVisitor mv);

    /**
     * 12.2.4.2.3 Runtime Semantics: ComprehensionEvaluation
     */
    @Override
    public Void visit(Comprehension node, ExpressionVisitor mv) {
        ArrayList<Node> list = new ArrayList<>(node.getList().size() + 1);
        list.addAll(node.getList());
        list.add(node.getExpression());
        elements = list.iterator();

        // create variables early for the sake of generating useful local variable maps
        ArrayList<Variable<Iterator<?>>> iters = new ArrayList<>();
        for (Node e : list) {
            if (e instanceof ComprehensionFor || e instanceof LegacyComprehensionFor) {
                Variable<Iterator<?>> iter = mv.newVariable("iter", Iterator.class).uncheckedCast();
                iters.add(iter);
            }
        }
        iterators = iters.iterator();

        // start generating code
        elements.next().accept(this, mv);

        return null;
    }

    /**
     * 12.2.4.2.3 Runtime Semantics: ComprehensionEvaluation
     */
    @Override
    public Void visit(LegacyComprehension node, ExpressionVisitor mv) {
        // create new declarative lexical environment
        // stack: [] -> [env]
        newDeclarativeEnvironment(mv);
        {
            // stack: [env] -> [env, envRec]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [env, envRec] -> [env]
            for (Name name : LexicallyDeclaredNames(node.getScope())) {
                mv.dup();
                mv.aconst(name.getIdentifier());
                mv.iconst(false);
                mv.invoke(Methods.EnvironmentRecord_createMutableBinding);

                mv.dup();
                mv.aconst(name.getIdentifier());
                mv.loadUndefined();
                mv.invoke(Methods.EnvironmentRecord_initializeBinding);
            }
            mv.pop();
        }
        // stack: [env] -> []
        pushLexicalEnvironment(mv);

        mv.enterScope(node);
        visit((Comprehension) node, mv);
        mv.exitScope();

        // restore previous lexical environment
        popLexicalEnvironment(mv);

        return null;
    }

    /**
     * 12.2.4.2.4 Runtime Semantics: ComprehensionComponentEvaluation
     * <p>
     * ComprehensionIf : if ( AssignmentExpression )
     */
    @Override
    public Void visit(ComprehensionIf node, ExpressionVisitor mv) {
        /* steps 1-2 */
        ValType type = expressionValue(node.getTest(), mv);
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
     * 12.2.4.2.4 Runtime Semantics: ComprehensionComponentEvaluation
     * <p>
     * ComprehensionFor : for ( ForBinding of AssignmentExpression )
     */
    @Override
    public Void visit(ComprehensionFor node, ExpressionVisitor mv) {
        Jump lblTest = new Jump(), lblLoop = new Jump();

        /* steps 1-2 */
        expressionBoxedValue(node.getExpression(), mv);

        /* steps 3-4 */
        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_iterate);

        Variable<Iterator<?>> iter = iterators.next();
        mv.store(iter);

        /* step 5 (not applicable) */

        /* step 6 */
        mv.nonDestructiveGoTo(lblTest);

        /* steps 9d-9e */
        mv.mark(lblLoop);
        mv.load(iter);
        mv.invoke(Methods.Iterator_next);

        /* steps 6f-6j */
        // create new declarative lexical environment
        // stack: [nextValue] -> [nextValue, forEnv]
        newDeclarativeEnvironment(mv);
        {
            // stack: [nextValue, forEnv] -> [forEnv, nextValue, envRec]
            mv.dupX1();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [forEnv, nextValue, envRec] -> [forEnv, envRec, nextValue]
            for (Name name : BoundNames(node.getBinding())) {
                mv.dup();
                mv.aconst(name.getIdentifier());
                mv.iconst(false);
                mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
            }
            mv.swap();

            // 12.2.4.2.2 Runtime Semantics: BindingInitialization :: ForBinding
            if (node.getBinding() instanceof BindingPattern) {
                mv.lineInfo(node.getBinding());
                mv.loadExecutionContext();
                mv.invoke(Methods.ScriptRuntime_ensureObject);
            }

            // stack: [forEnv, envRec, nextValue] -> [forEnv]
            BindingInitializationWithEnvironment(node.getBinding(), mv);
        }
        // stack: [forEnv] -> []
        pushLexicalEnvironment(mv);

        /* steps 6k, 6m */
        mv.enterScope(node);
        elements.next().accept(this, mv);
        mv.exitScope();

        /* step 6l */
        // restore previous lexical environment
        popLexicalEnvironment(mv);

        /* steps 6a-6c */
        mv.mark(lblTest);
        mv.load(iter);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(lblLoop);

        return null;
    }

    /**
     * 12.2.4.2.4 Runtime Semantics: ComprehensionComponentEvaluation
     * <p>
     * ComprehensionFor : for ( ForBinding of AssignmentExpression )
     */
    @Override
    public Void visit(LegacyComprehensionFor node, ExpressionVisitor mv) {
        Jump lblTest = new Jump(), lblLoop = new Jump(), lblFail = new Jump();

        ValType type = expressionValue(node.getExpression(), mv);
        if (type != ValType.Object) {
            // fail-safe behaviour for null/undefined values in legacy comprehensions
            Jump loopstart = new Jump();
            mv.toBoxed(type);
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
            assert iterationKind == IterationKind.Iterate;
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_iterate);
        }

        Variable<Iterator<?>> iter = iterators.next();
        mv.store(iter);

        mv.nonDestructiveGoTo(lblTest);
        mv.mark(lblLoop);
        mv.load(iter);
        mv.invoke(Methods.Iterator_next);

        if (node.getBinding() instanceof BindingPattern) {
            // ToObject(...)
            ToObject(node.getBinding(), ValType.Any, mv);
        }

        // stack: [nextValue] -> []
        BindingInitialization(node.getBinding(), mv);

        elements.next().accept(this, mv);

        mv.mark(lblTest);
        mv.load(iter);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifne(lblLoop);
        mv.mark(lblFail);

        return null;
    }
}
