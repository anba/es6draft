/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.IdentifierReference;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;

/**
 * 8.3.1 ResolveBinding(name)
 */
final class IdentifierResolution {
    private static final class Methods {
        // identifierResolution()
        static final MethodName ExecutionContext_resolveBinding = MethodName.findVirtual(
                Types.ExecutionContext, "resolveBinding",
                Type.methodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

        // identifierValue()
        static final MethodName ExecutionContext_resolveBindingValue = MethodName.findVirtual(
                Types.ExecutionContext, "resolveBindingValue",
                Type.methodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));
    }

    ValType resolve(IdentifierReference node, ExpressionVisitor mv) {
        mv.lineInfo(node);
        return resolve(node.getName(), mv);
    }

    ValType resolve(BindingIdentifier node, ExpressionVisitor mv) {
        return resolve(node.getName().getIdentifier(), mv);
    }

    ValType resolve(Name name, ExpressionVisitor mv) {
        return resolve(name.getIdentifier(), mv);
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
