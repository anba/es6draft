/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Wrapper for {@link MethodHandles.Lookup}.
 */
final class MethodLookup {
    private final MethodHandles.Lookup lookup;

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
        return findStatic(lookup, name, type);
    }

    /**
     * Returns a method handle for a static method.
     * 
     * @param lookup
     *            the lookup object
     * @param name
     *            the method name
     * @param rtype
     *            the method return type
     * @param ptypes
     *            the method parameter types
     * @return method handle for a static method
     */
    public static MethodHandle findStatic(MethodHandles.Lookup lookup, String name, MethodType type) {
        try {
            return lookup.findStatic(lookup.lookupClass(), name, type);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
