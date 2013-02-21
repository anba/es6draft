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
import com.github.anba.es6draft.compiler.MethodGenerator.Register;

/**
 *
 */
class IdentifierResolution {
    private static class InternMethods {
        // identifierResolution()
        static final MethodDesc ExecutionContext_identifierResolution = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "identifierResolution",
                Type.getMethodType(Types.Reference, Types.String, Type.BOOLEAN_TYPE));

        // identifierValue()
        static final MethodDesc ExecutionContext_identifierValue = MethodDesc.create(
                MethodType.Virtual, Types.ExecutionContext, "identifierValue",
                Type.getMethodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));
    }

    ValType resolve(Identifier node, MethodGenerator mv) {
        return resolve(node.getName(), mv);
    }

    ValType resolve(BindingIdentifier node, MethodGenerator mv) {
        return resolve(node.getName(), mv);
    }

    private ValType resolve(String identifierName, MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.aconst(identifierName);
        mv.iconst(mv.isStrict());
        mv.invoke(InternMethods.ExecutionContext_identifierResolution);

        return ValType.Reference;
    }

    ValType resolveValue(Identifier node, MethodGenerator mv) {
        mv.load(Register.ExecutionContext);
        mv.aconst(node.getName());
        mv.iconst(mv.isStrict());
        mv.invoke(InternMethods.ExecutionContext_identifierValue);

        return ValType.Any;
    }
}
