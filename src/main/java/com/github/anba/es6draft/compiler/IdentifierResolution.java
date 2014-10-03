/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.IdentifierReference;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.MethodDesc;

/**
 * 8.3.1 ResolveBinding(name)
 */
final class IdentifierResolution {
    private static final class Methods {
        // identifierResolution()
        static final MethodDesc ExecutionContext_resolveBinding = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "resolveBinding",
                Type.getMethodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

        // identifierValue()
        static final MethodDesc ExecutionContext_resolveBindingValue = MethodDesc.create(
                MethodDesc.Invoke.Virtual, Types.ExecutionContext, "resolveBindingValue",
                Type.getMethodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));
    }

    ValType resolve(IdentifierReference node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        return resolve(node.getName(), mv);
    }

    ValType resolve(BindingIdentifier node, ExpressionVisitor mv) {
        return resolve(node.getName().getIdentifier(), mv);
    }

    ValType resolveValue(IdentifierReference node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        return resolveValue(node.getName(), mv);
    }

    private ValType resolve(String identifierName, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ExecutionContext_resolveBinding);

        return ValType.Reference;
    }

    private ValType resolveValue(String identifierName, ExpressionVisitor mv) {
        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.invoke(Methods.ExecutionContext_resolveBindingValue);

        return ValType.Any;
    }
}
