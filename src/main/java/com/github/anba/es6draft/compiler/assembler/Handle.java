/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import org.objectweb.asm.Opcodes;

/**
 * Method handles.
 */
public final class Handle {
    private final org.objectweb.asm.Handle handle;

    /*package*/Handle(MethodName method) {
        this.handle = new org.objectweb.asm.Handle(toTag(method), method.owner.internalName(),
                method.name, method.descriptor.descriptor());
    }

    org.objectweb.asm.Handle handle() {
        return handle;
    }

    private static int toTag(MethodName method) {
        switch (method.invoke) {
        case Interface:
            return Opcodes.H_INVOKEINTERFACE;
        case Special:
        case SpecialInterface:
            return Opcodes.H_INVOKESPECIAL;
        case Static:
        case StaticInterface:
            return Opcodes.H_INVOKESTATIC;
        case Virtual:
        case VirtualInterface:
            return Opcodes.H_INVOKEVIRTUAL;
        default:
            throw new AssertionError();
        }
    }
}
