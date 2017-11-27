/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;

/** 
 *
 */
abstract class IterationGenerator<NODE extends Node> extends AbstractIterationGenerator<NODE, ScriptIterator<?>> {
    private static final class Methods {
        // class: ScriptIterator
        static final MethodName ScriptIterator_close = MethodName.findInterface(Types.ScriptIterator, "close",
                Type.methodType(Type.VOID_TYPE));

        static final MethodName ScriptIterator_close_exception = MethodName.findInterface(Types.ScriptIterator, "close",
                Type.methodType(Type.VOID_TYPE, Types.Throwable));
    }

    IterationGenerator(CodeGenerator codegen) {
        super(codegen);
    }

    @Override
    protected final void IteratorClose(NODE node, Variable<ScriptIterator<?>> iterator,
            Variable<? extends Throwable> throwable, CodeVisitor mv) {
        mv.load(iterator);
        mv.load(throwable);
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptIterator_close_exception);
    }

    @Override
    protected final void IteratorClose(NODE node, Variable<ScriptIterator<?>> iterator, CodeVisitor mv) {
        mv.load(iterator);
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptIterator_close);
    }
}
