/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;
import static com.github.anba.es6draft.semantics.StaticSemantics.LexicallyDeclaredNames;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Label;
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
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.FieldType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;

/**
 * 11.1.4.2 Array Comprehension
 */
abstract class ComprehensionGenerator extends DefaultCodeGenerator<Void, ExpressionVisitor> {
    private static class Fields {
        static final FieldDesc Undefined_UNDEFINED = FieldDesc.create(FieldType.Static,
                Types.Undefined, "UNDEFINED", Types.Undefined);
    }

    private static class Methods {
        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc EnvironmentRecord_initialiseBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initialiseBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        // class: Iterator
        static final MethodDesc Iterator_hasNext = MethodDesc.create(MethodType.Interface,
                Types.Iterator, "hasNext", Type.getMethodType(Type.BOOLEAN_TYPE));

        static final MethodDesc Iterator_next = MethodDesc.create(MethodType.Interface,
                Types.Iterator, "next", Type.getMethodType(Types.Object));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

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
    }

    private Iterator<Node> elements;

    ComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected Void visit(Node node, ExpressionVisitor mv) {
        throw new IllegalStateException(String.format("node-class: %s", node.getClass()));
    }

    /**
     * 11.1.4.2 Array Comprehension
     * <p>
     * Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionQualifierTail: AssignmentExpression
     */
    @Override
    protected abstract Void visit(Expression node, ExpressionVisitor mv);

    /**
     * 11.1.4.2 Array Comprehension
     * <p>
     * Runtime Semantics: ComprehensionEvaluation
     * <p>
     * Comprehension : ComprehensionFor ComprehensionQualifierTail
     */
    @Override
    public Void visit(Comprehension node, ExpressionVisitor mv) {
        List<Node> list = new ArrayList<>(node.getList().size() + 1);
        list.addAll(node.getList());
        list.add(node.getExpression());
        elements = list.iterator();

        elements.next().accept(this, mv);

        return null;
    }

    @Override
    public Void visit(LegacyComprehension node, ExpressionVisitor mv) {
        // create new declarative lexical environment
        // stack: [] -> [env]
        mv.enterScope(node);
        newDeclarativeEnvironment(mv);
        {
            // stack: [env] -> [env, envRec]
            mv.dup();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [env, envRec] -> [env]
            for (String name : LexicallyDeclaredNames(node.getScope())) {
                mv.dup();
                mv.aconst(name);
                mv.iconst(false);
                mv.invoke(Methods.EnvironmentRecord_createMutableBinding);

                mv.dup();
                mv.aconst(name);
                mv.get(Fields.Undefined_UNDEFINED);
                mv.invoke(Methods.EnvironmentRecord_initialiseBinding);
            }
            mv.pop();
        }
        // stack: [env] -> []
        pushLexicalEnvironment(mv);

        visit((Comprehension) node, mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        return null;
    }

    /**
     * 11.1.4.2 Array Comprehension
     * <p>
     * Runtime Semantics: QualifierEvaluation
     * <p>
     * ComprehensionFor : if ( AssignmentExpression )
     */
    @Override
    public Void visit(ComprehensionIf node, ExpressionVisitor mv) {
        Label lblTest = new Label();
        ValType type = expressionValue(node.getTest(), mv);
        ToBoolean(type, mv);
        mv.ifeq(lblTest);

        elements.next().accept(this, mv);

        mv.mark(lblTest);

        return null;
    }

    /**
     * 11.1.4.2 Array Comprehension
     * <p>
     * Runtime Semantics: QualifierEvaluation
     * <p>
     * ComprehensionFor : for (ForBinding of AssignmentExpression )
     */
    @Override
    public Void visit(ComprehensionFor node, ExpressionVisitor mv) {
        Label lblContinue = new Label(), lblBreak = new Label();

        ValType type = expressionValue(node.getExpression(), mv);
        mv.toBoxed(type);

        mv.loadExecutionContext();
        mv.invoke(Methods.ScriptRuntime_iterate);

        @SuppressWarnings("rawtypes")
        Variable<Iterator> iter = mv.newVariable("iter", Iterator.class);
        mv.store(iter);

        mv.mark(lblContinue);
        mv.load(iter);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifeq(lblBreak);
        mv.load(iter);
        mv.invoke(Methods.Iterator_next);

        // create new declarative lexical environment
        // stack: [nextValue] -> [nextValue, forEnv]
        mv.enterScope(node);
        newDeclarativeEnvironment(mv);
        {
            // stack: [nextValue, forEnv] -> [forEnv, nextValue, envRec]
            mv.dupX1();
            mv.invoke(Methods.LexicalEnvironment_getEnvRec);

            // stack: [forEnv, nextValue, envRec] -> [forEnv, envRec, nextValue]
            for (String name : BoundNames(node.getBinding())) {
                mv.dup();
                mv.aconst(name);
                mv.iconst(false);
                mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
            }
            mv.swap();

            // Binding Instantiation: ForBinding
            if (node.getBinding() instanceof BindingPattern) {
                ToObject(ValType.Any, mv);
            }

            // stack: [forEnv, envRec, nextValue] -> [forEnv]
            BindingInitialisationWithEnvironment(node.getBinding(), mv);
        }
        // stack: [forEnv] -> []
        pushLexicalEnvironment(mv);

        elements.next().accept(this, mv);

        // restore previous lexical environment
        popLexicalEnvironment(mv);
        mv.exitScope();

        mv.goTo(lblContinue);
        mv.mark(lblBreak);
        mv.freeVariable(iter);

        return null;
    }

    @Override
    public Void visit(LegacyComprehensionFor node, ExpressionVisitor mv) {
        Label lblContinue = new Label(), lblBreak = new Label();
        Label loopstart = new Label();

        ValType type = expressionValue(node.getExpression(), mv);
        mv.toBoxed(type);

        mv.dup();
        isUndefinedOrNull(mv);
        mv.ifeq(loopstart);
        mv.pop();
        mv.goTo(lblBreak);
        mv.mark(loopstart);

        IterationKind iterationKind = node.getIterationKind();
        if (iterationKind == IterationKind.Enumerate
                || iterationKind == IterationKind.EnumerateValues) {
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
            assert iterationKind == IterationKind.Iterate;
            mv.loadExecutionContext();
            mv.invoke(Methods.ScriptRuntime_iterate);
        }

        @SuppressWarnings("rawtypes")
        Variable<Iterator> iter = mv.newVariable("iter", Iterator.class);
        mv.store(iter);

        mv.mark(lblContinue);
        mv.load(iter);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifeq(lblBreak);
        mv.load(iter);
        mv.invoke(Methods.Iterator_next);

        if (node.getBinding() instanceof BindingPattern) {
            // ToObject(...)
            ToObject(ValType.Any, mv);
        }

        // stack: [nextValue] -> []
        BindingInitialisation(node.getBinding(), mv);

        elements.next().accept(this, mv);

        mv.goTo(lblContinue);
        mv.mark(lblBreak);
        mv.freeVariable(iter);

        return null;
    }
}
