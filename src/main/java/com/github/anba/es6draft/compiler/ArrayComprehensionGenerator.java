/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.ArrayComprehension;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;
import com.github.anba.es6draft.compiler.assembler.Variable;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>12.2.4.2 Array Comprehension
 * </ul>
 */
final class ArrayComprehensionGenerator extends ComprehensionGenerator {
    private static final class Methods {
        // class: AbstractOperations
        static final MethodDesc AbstractOperations_CreateArrayFromList = MethodDesc.create(
                MethodDesc.Invoke.Static, Types.AbstractOperations, "CreateArrayFromList",
                Type.getMethodType(Types.ArrayObject, Types.ExecutionContext, Types.List));

        // class: ArrayList
        static final MethodDesc ArrayList_init = MethodDesc.create(MethodDesc.Invoke.Special,
                Types.ArrayList, "<init>", Type.getMethodType(Type.VOID_TYPE));

        static final MethodDesc ArrayList_add = MethodDesc.create(MethodDesc.Invoke.Virtual,
                Types.ArrayList, "add", Type.getMethodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    @SuppressWarnings("rawtypes")
    private Variable<ArrayList> result;

    private ArrayComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * 12.2.4.2.5 Runtime Semantics: Evaluation
     */
    static ValType EvaluateArrayComprehension(CodeGenerator codegen, ArrayComprehension node,
            ExpressionVisitor mv) {
        ArrayComprehensionGenerator generator = new ArrayComprehensionGenerator(codegen);

        /* step 1 */
        mv.enterVariableScope();
        generator.result = mv.newVariable("result", ArrayList.class);
        mv.anew(Types.ArrayList, Methods.ArrayList_init);
        mv.store(generator.result);

        /* steps 2-3 */
        node.getComprehension().accept(generator, mv);

        /* step 4 */
        mv.loadExecutionContext();
        mv.load(generator.result);
        mv.invoke(Methods.AbstractOperations_CreateArrayFromList);
        mv.exitVariableScope();

        return ValType.Object;
    }

    /**
     * 12.2.4.2.3 Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionTail : AssignmentExpression
     */
    @Override
    protected Void visit(Expression node, ExpressionVisitor mv) {
        /* steps 1-3 */
        expressionBoxedValue(node, mv);
        /* step 4 */
        mv.load(result);
        mv.swap();
        mv.invoke(Methods.ArrayList_add);
        mv.pop();
        /* steps 5-8 (not applicable) */

        return null;
    }
}
