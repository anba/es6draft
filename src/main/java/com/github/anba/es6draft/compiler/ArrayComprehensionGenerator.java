/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.ArrayComprehension;
import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * TODO: current draft [rev. 13] does not specify the runtime semantics for array-comprehensions,
 * therefore the translation from
 * http://wiki.ecmascript.org/doku.php?id=harmony:array_comprehensions is used
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

    private int result;

    ArrayComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    public Void visit(ArrayComprehension node, ExpressionVisitor mv) {
        this.result = mv.newVariable(Types.ArrayList);
        mv.anew(Types.ArrayList);
        mv.dup();
        mv.invoke(Methods.ArrayList_init);
        mv.store(result, Types.ArrayList);

        node.getComprehension().accept(this, mv);

        mv.loadExecutionContext();
        mv.load(result, Types.ArrayList);
        mv.invoke(Methods.AbstractOperations_CreateArrayFromList);
        mv.freeVariable(result);

        return null;
    }

    @Override
    protected Void visit(Expression node, ExpressionVisitor mv) {
        ValType type = expression(node, mv);
        mv.toBoxed(type);
        invokeGetValue(node, mv);
        mv.load(result, Types.ArrayList);
        mv.swap();
        mv.invoke(Methods.ArrayList_add);
        mv.pop();

        return null;
    }
}
