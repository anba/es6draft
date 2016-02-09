/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.IdentifierReference;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;

/**
 * 8.3.1 ResolveBinding ( name, [env] )
 */
final class IdentifierResolution {
    private static final class Methods {
        // class: ExecutionContext
        static final MethodName ExecutionContext_resolveBinding = MethodName.findVirtual(
                Types.ExecutionContext, "resolveBinding",
                Type.methodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName ExecutionContext_resolveBindingValue = MethodName.findVirtual(
                Types.ExecutionContext, "resolveBindingValue",
                Type.methodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));
    }

    private IdentifierResolution() {
    }

    static ValType resolve(IdentifierReference node, CodeVisitor mv) {
        return resolve(node, node.getName(), mv);
    }

    static ValType resolve(BindingIdentifier node, CodeVisitor mv) {
        return resolve(node, node.getName().getIdentifier(), mv);
    }

    static ValType resolveValue(IdentifierReference node, CodeVisitor mv) {
        return resolveValue(node, node.getName(), mv);
    }

    private static ValType resolve(Node node, String identifierName, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(Methods.ExecutionContext_resolveBinding);
        return ValType.Reference;
    }

    private static ValType resolveValue(Node node, String identifierName, CodeVisitor mv) {
        mv.loadExecutionContext();
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.lineInfo(node);
        mv.invoke(Methods.ExecutionContext_resolveBindingValue);
        return ValType.Any;
    }
}
