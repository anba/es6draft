/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayList;

import com.github.anba.es6draft.ast.ArrayComprehension;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;

/**
 * <h1>12 ECMAScript Language: Expressions</h1><br>
 * <h2>12.2 Primary Expression</h2>
 * <ul>
 * <li>Array Comprehension
 * </ul>
 */
final class ArrayComprehensionGenerator extends ComprehensionGenerator {
    private static final class Methods {
        // class: AbstractOperations
        static final MethodName AbstractOperations_CreateArrayFromList = MethodName.findStatic(Types.AbstractOperations,
                "CreateArrayFromList", Type.methodType(Types.ArrayObject, Types.ExecutionContext, Types.List));

        // class: ArrayList
        static final MethodName ArrayList_new = MethodName.findConstructor(Types.ArrayList,
                Type.methodType(Type.VOID_TYPE));

        static final MethodName ArrayList_add = MethodName.findVirtual(Types.ArrayList, "add",
                Type.methodType(Type.BOOLEAN_TYPE, Types.Object));
    }

    private Variable<ArrayList<?>> result;

    private ArrayComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    /**
     * Runtime Semantics: Evaluation
     */
    static ValType EvaluateArrayComprehension(CodeGenerator codegen, ArrayComprehension node, CodeVisitor mv) {
        ArrayComprehensionGenerator generator = new ArrayComprehensionGenerator(codegen);

        /* step 1 */
        mv.enterVariableScope();
        generator.result = mv.newVariable("result", ArrayList.class).uncheckedCast();
        mv.anew(Methods.ArrayList_new);
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
     * Runtime Semantics: ComprehensionEvaluation
     * <p>
     * ComprehensionTail : AssignmentExpression
     */
    @Override
    protected Void visit(Expression node, CodeVisitor mv) {
        /* steps 1-3 */
        expressionBoxed(node, mv);
        /* step 4 */
        mv.load(result);
        mv.swap();
        mv.invoke(Methods.ArrayList_add);
        mv.pop();
        /* steps 5-8 (not applicable) */

        return null;
    }
}
