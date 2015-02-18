/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.lang.invoke.MethodType;

/**
 * Method name descriptor.
 */
public final class MethodName {
    public enum Invoke {
        Interface, Virtual, Special, Static, VirtualInterface, SpecialInterface, StaticInterface
    }

    /**
     * The method invocation type.
     */
    public final MethodName.Invoke invoke;

    /**
     * The type descriptor of the declaring class.
     */
    public final Type owner;

    /**
     * The method name.
     */
    public final String name;

    /**
     * The method type descriptor for the parameters and return value.
     */
    public final MethodTypeDescriptor descriptor;

    private MethodName(MethodName.Invoke invoke, Type owner, String name,
            MethodTypeDescriptor descriptor) {
        this.invoke = invoke;
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
    }

    /**
     * Returns a {@link Handle} object for this method.
     * 
     * @return a handle for this method
     */
    public Handle toHandle() {
        return new Handle(this);
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param desc
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findConstructor(Type owner, MethodTypeDescriptor desc) {
        return new MethodName(Invoke.Special, owner, "<init>", desc);
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param desc
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findInterface(Type owner, String name, MethodTypeDescriptor desc) {
        return new MethodName(Invoke.Interface, owner, name, desc);
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param desc
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findSpecial(Type owner, String name, MethodTypeDescriptor desc) {
        return new MethodName(Invoke.Special, owner, name, desc);
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param desc
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findStatic(Type owner, String name, MethodTypeDescriptor desc) {
        return new MethodName(Invoke.Static, owner, name, desc);
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param desc
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findVirtual(Type owner, String name, MethodTypeDescriptor desc) {
        return new MethodName(Invoke.Virtual, owner, name, desc);
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param methodType
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findConstructor(Class<?> owner, MethodType methodType) {
        return new MethodName(Invoke.Special, Type.of(owner), "<init>",
                MethodTypeDescriptor.methodType(methodType));
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param methodType
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findInterface(Class<?> owner, String name, MethodType methodType) {
        return new MethodName(Invoke.Interface, Type.of(owner), name,
                MethodTypeDescriptor.methodType(methodType));
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param methodType
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findSpecial(Class<?> owner, String name, MethodType methodType) {
        return new MethodName(Invoke.Special, Type.of(owner), name,
                MethodTypeDescriptor.methodType(methodType));
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param methodType
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findStatic(Class<?> owner, String name, MethodType methodType) {
        return new MethodName(Invoke.Static, Type.of(owner), name,
                MethodTypeDescriptor.methodType(methodType));
    }

    /**
     * Creates a new method name descriptor.
     * 
     * @param owner
     *            the owner class
     * @param name
     *            the method name
     * @param methodType
     *            the method argument and return types
     * @return the method name descriptor
     */
    public static MethodName findVirtual(Class<?> owner, String name, MethodType methodType) {
        return new MethodName(Invoke.Virtual, Type.of(owner), name,
                MethodTypeDescriptor.methodType(methodType));
    }
}
