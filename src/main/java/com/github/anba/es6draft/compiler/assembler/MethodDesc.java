/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Method descriptor object.
 */
public final class MethodDesc {
    public enum Invoke {
        Interface, Virtual, Special, Static, VirtualInterface, SpecialInterface, StaticInterface;

        final int toTag() {
            switch (this) {
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

    /**
     * The method invocation type.
     */
    public final MethodDesc.Invoke invoke;

    /**
     * Type descriptor of the declaring class.
     */
    public final String owner;

    /**
     * The method name.
     */
    public final String name;

    /**
     * Type descriptor for arguments and return value.
     */
    public final String desc;

    private MethodDesc(MethodDesc.Invoke invoke, String owner, String name, String desc) {
        this.invoke = invoke;
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    /**
     * Creates a new method descriptor.
     * 
     * @param type
     *            the method type
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param desc
     *            the method argument and return types
     * @return the method descriptor
     */
    public static MethodDesc create(MethodDesc.Invoke type, Type owner, String name, Type desc) {
        return new MethodDesc(type, owner.getInternalName(), name, desc.getDescriptor());
    }

    /**
     * Creates a new method descriptor.
     * 
     * @param type
     *            the method type
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param desc
     *            the method argument and return types
     * @return the method descriptor
     */
    public static MethodDesc create(MethodDesc.Invoke type, String owner, String name, String desc) {
        return new MethodDesc(type, owner, name, desc);
    }
}
