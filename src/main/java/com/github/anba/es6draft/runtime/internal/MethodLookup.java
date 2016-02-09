/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Wrapper class for {@link MethodHandles.Lookup}.
 */
public final class MethodLookup {
    private final MethodHandles.Lookup lookup;

    /**
     * Constructs a new instance.
     * 
     * @param lookup
     *            the lookup object
     */
    public MethodLookup(MethodHandles.Lookup lookup) {
        this.lookup = lookup;
    }

    /**
     * Returns wrapped the lookup object.
     * 
     * @return the lookup object
     */
    public MethodHandles.Lookup getLookup() {
        return lookup;
    }

    /**
     * Returns a method handle for a static method.
     * 
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a static method
     */
    public MethodHandle findStatic(String name, MethodType type) {
        return findStatic(lookup, lookup.lookupClass(), name, type);
    }

    /**
     * Returns a method handle for a static method.
     * 
     * @param clazz
     *            the reference class
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a static method
     */
    public MethodHandle findStatic(Class<?> clazz, String name, MethodType type) {
        return findStatic(lookup, clazz, name, type);
    }

    /**
     * Returns a method handle for a static method.
     * 
     * @param lookup
     *            the lookup object
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a static method
     */
    public static MethodHandle findStatic(MethodHandles.Lookup lookup, String name, MethodType type) {
        return findStatic(lookup, lookup.lookupClass(), name, type);
    }

    /**
     * Returns a method handle for a static method.
     * 
     * @param lookup
     *            the lookup object
     * @param clazz
     *            the reference class
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a static method
     */
    public static MethodHandle findStatic(MethodHandles.Lookup lookup, Class<?> clazz, String name,
            MethodType type) {
        try {
            return lookup.findStatic(clazz, name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a method handle for a virtual method.
     * 
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a virtual method
     */
    public MethodHandle findVirtual(String name, MethodType type) {
        return findVirtual(lookup, lookup.lookupClass(), name, type);
    }

    /**
     * Returns a method handle for a virtual method.
     * 
     * @param clazz
     *            the reference class
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a virtual method
     */
    public MethodHandle findVirtual(Class<?> clazz, String name, MethodType type) {
        return findVirtual(lookup, clazz, name, type);
    }

    /**
     * Returns a method handle for a virtual method.
     * 
     * @param lookup
     *            the lookup object
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a virtual method
     */
    public static MethodHandle findVirtual(MethodHandles.Lookup lookup, String name, MethodType type) {
        return findVirtual(lookup, lookup.lookupClass(), name, type);
    }

    /**
     * Returns a method handle for a virtual method.
     * 
     * @param lookup
     *            the lookup object
     * @param clazz
     *            the reference class
     * @param name
     *            the method name
     * @param type
     *            the method type
     * @return method handle for a virtual method
     */
    public static MethodHandle findVirtual(MethodHandles.Lookup lookup, Class<?> clazz,
            String name, MethodType type) {
        try {
            return lookup.findVirtual(clazz, name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
