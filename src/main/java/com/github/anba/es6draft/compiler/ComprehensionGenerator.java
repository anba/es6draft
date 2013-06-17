/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.BoundNames;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Comprehension;
import com.github.anba.es6draft.ast.ComprehensionFor;
import com.github.anba.es6draft.ast.ComprehensionIf;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * 11.1.4.2 Array Comprehension
 */
abstract class ComprehensionGenerator extends DefaultCodeGenerator<Void, ExpressionVisitor> {
    private static class Methods {
        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

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

        int var = mv.newVariable(Types.Iterator);
        mv.store(var, Types.Iterator);

        mv.mark(lblContinue);
        mv.load(var, Types.Iterator);
        mv.invoke(Methods.Iterator_hasNext);
        mv.ifeq(lblBreak);
        mv.load(var, Types.Iterator);
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

            // FIXME: spec bug (missing ToObject() call?)

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
        mv.freeVariable(var);

        return null;
    }
}
