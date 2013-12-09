/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.ArrayComprehension;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.1 Primary Expressions</h2>
 * <ul>
 * <li>12.1.4.2 Array Comprehension
 * </ul>
 */
class ArrayComprehensionGenerator extends ComprehensionGenerator {
    private static class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_CreateArrayFromList = MethodDesc.create(
                MethodType.Static, Types.AbstractOperations, "CreateArrayFromList",
                Type.getMethodType(Types.ScriptObject, Types.ExecutionContext, Types.List));

        // class: ArrayList
        static final MethodDesc ArrayList_init = MethodDesc.create(MethodType.Special,
                Types.ArrayList, "<init>", Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc ArrayList_add = MethodDesc.create(MethodType.Virtual,
                Types.ArrayList, "add", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    @SuppressWarnings("rawtypes")
    private Variable<ArrayList> result = null;

    ArrayComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * 12.1.4.2.5 Runtime Semantics: Evaluation
     */
    @Override
    public Void visit(ArrayComprehension node, ExpressionVisitor mv) {
        if (result != null) {
            // nested array comprehension
            return visit((Expression) node, mv);
        }

        mv.enterVariableScope();
        result = mv.newVariable("result", ArrayList.class);
        mv.anew(Types.ArrayList);
        mv.dup();
        mv.invoke(Methods.ArrayList_init);
        mv.store(result);

        node.getComprehension().accept(this, mv);

        mv.loadExecutionContext();
        mv.load(result);
        mv.invoke(Methods.AbstractOperations_CreateArrayFromList);
        mv.exitVariableScope();

        return null;
    }

    /**
     * 12.1.4.2.3 Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionTail : AssignmentExpression
     */
    @Override
    protected Void visit(Expression node, ExpressionVisitor mv) {
        assert result != null : "array-comprehension generator not initialised";

        expressionBoxedValue(node, mv);
        mv.load(result);
        mv.swap();
        mv.invoke(Methods.ArrayList_add);
        mv.pop();

        return null;
    }
}
