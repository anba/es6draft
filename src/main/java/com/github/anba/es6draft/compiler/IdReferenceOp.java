/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;

/**
 * Rename to VarBindingOp?
 */
abstract class IdReferenceOp {
    /**
     * <h1>8.3.1 ResolveBinding</h1>
     * <p>
     * Evaluates {@code node} and pushes the resolved reference object on the stack.
     * <p>
     * stack: [] -> [{@literal <reference>}]
     * 
     * @param node
     *            the reference node
     * @param mv
     *            the code visitor
     * @return the reference value type
     */
    abstract ValType resolveBinding(BindingIdentifier node, CodeVisitor mv);

    /**
     * <h1>6.2.3.2 PutValue (V, W)</h1>
     * <p>
     * Assigns a new value to the reference.
     * <p>
     * stack: [{@literal <reference>}, {@literal <value>}] -> []
     * 
     * @param node
     *            the reference node
     * @param value
     *            the top stack value type
     * @param mv
     *            the code visitor
     */
    abstract void putValue(BindingIdentifier node, ValType value, CodeVisitor mv);

    /**
     * Returns the {@code IdReferenceOp} implementation for the binding identifier.
     * 
     * @param lhs
     *            the binding identifier
     * @return the {@code IdReferenceOp}
     */
    static IdReferenceOp of(BindingIdentifier ident) {
        return IdReferenceOp.LOOKUP;
    }

    private static final class Methods {
        // class: Reference
        static final MethodName Reference_putValue = MethodName.findVirtual(Types.Reference,
                "putValue", Type.methodType(Type.VOID_TYPE, Types.Object, Types.ExecutionContext));
    }

    /**
     * 12.1.6 Runtime Semantics: Evaluation
     */
    static final IdReferenceOp LOOKUP = new IdReferenceOp() {
        @Override
        ValType resolveBinding(BindingIdentifier node, CodeVisitor mv) {
            // stack: [] -> [ref]
            IdentifierResolution.resolve(node, mv);
            return ValType.Reference;
        }

        @Override
        void putValue(BindingIdentifier node, ValType value, CodeVisitor mv) {
            // stack: [ref, value] -> []
            mv.toBoxed(value);
            mv.loadExecutionContext();
            mv.lineInfo(node);
            mv.invoke(Methods.Reference_putValue);
        }
    };
}
