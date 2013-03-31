/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Expression;
import com.github.anba.es6draft.ast.GeneratorComprehension;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * TODO: current draft [rev. 13] does not specify the runtime semantics for
 * generator-comprehensions, therefore the translation from
 * http://wiki.ecmascript.org/doku.php?id=harmony:generator_expressions is used
 */
final class GeneratorComprehensionGenerator extends ComprehensionGenerator {
    private static class Methods {
        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_yield = MethodDesc.create(MethodType.Static,
                Types.ScriptRuntime, "yield",
                Type.getMethodType(Types.Object, Types.Object, Types.ExecutionContext));
    }

    GeneratorComprehensionGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    public Void visit(GeneratorComprehension node, ExpressionVisitor mv) {
        node.getComprehension().accept(this, mv);

        return null;
    }

    @Override
    protected Void visit(Expression node, ExpressionVisitor mv) {
        ValType type = expression(node, mv);
        mv.toBoxed(type);
        invokeGetValue(node, mv);
        mv.loadExecutionContext();
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_yield);
        mv.pop();

        return null;
    }
}
