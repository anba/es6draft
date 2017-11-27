/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.HashMap;

/**
 * Type-aware {@link Stack} implementation.
 */
public final class TypedStack extends Stack {
    private final HashMap<String, Type> typeCache = new HashMap<>();

    @Override
    protected Type intersectionType(Type left, Type right) {
        assert !left.isPrimitive() && !right.isPrimitive();
        if (Types.Object.equals(left) || Types.Object.equals(right)) {
            return Types.Object;
        }
        return intersectionTypeFromClass(left, right);
    }

    private Type intersectionTypeFromClass(Type left, Type right) {
        String key = left.descriptor() + "|" + right.descriptor();
        return typeCache.computeIfAbsent(key, __ -> computeIntersectionType(left, right));
    }

    private Type computeIntersectionType(Type left, Type right) {
        ClassLoader classLoader = getClass().getClassLoader();
        Class<?> cleft, cright;
        try {
            cleft = Class.forName(left.className(), false, classLoader);
            cright = Class.forName(right.className(), false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (cleft.isAssignableFrom(cright)) {
            return left;
        }
        if (cright.isAssignableFrom(cleft)) {
            return right;
        }
        if (cleft.isInterface() || cright.isInterface()) {
            return Types.Object;
        }
        while ((cleft = cleft.getSuperclass()) != null) {
            if (cleft.isAssignableFrom(cright)) {
                return Type.of(cleft);
            }
        }
        return Types.Object;
    }
}
