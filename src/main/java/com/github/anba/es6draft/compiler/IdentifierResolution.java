/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.Identifier;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 *
 */
class IdentifierResolution {
    private static class Methods {
        // identifierResolution()
        static final MethodDesc ExecutionContext_identifierResolution = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "identifierResolution",
                Type.getMethodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

        // identifierValue()
        static final MethodDesc ExecutionContext_identifierValue = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "identifierValue",
                Type.getMethodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));
    }

    ValType resolve(Identifier node, ExpressionVisitor mv) {
        return resolve(node.getName(), mv);
    }

    ValType resolve(BindingIdentifier node, ExpressionVisitor mv) {
        return resolve(node.getName(), mv);
    }

    ValType resolveValue(Identifier node, ExpressionVisitor mv) {
        return resolveValue(node.getName(), mv);
    }

    private ValType resolve(String identifierName, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ExecutionContext_identifierResolution);

        return ValType.Reference;
    }

    private ValType resolveValue(String identifierName, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ExecutionContext_identifierValue);

        return ValType.Any;
    }
}
