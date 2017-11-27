/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.lang.invoke.MethodHandle;

import org.objectweb.asm.Opcodes;

/**
 * Method handles.
 */
public final class Handle implements Value<MethodHandle> {
    private final org.objectweb.asm.Handle handle;

    Handle(MethodName method) {
        this.handle = new org.objectweb.asm.Handle(toTag(method), method.owner.internalName(), method.name,
                method.descriptor.descriptor(), isInterface(method));
    }

    org.objectweb.asm.Handle handle() {
        return handle;
    }

    @Override
    public void load(InstructionAssembler assembler) {
        assembler.hconst(this);
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

    private static boolean isInterface(MethodName method) {
        switch (method.invoke) {
        case Special:
        case Static:
        case Virtual:
            return false;
        case Interface:
        case SpecialInterface:
        case StaticInterface:
        case VirtualInterface:
            return true;
        default:
            throw new AssertionError();
        }
    }
}
