/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 *
 */
final class Types {
    private Types() {
    }

    // java.lang
    static final Type Boolean = Type.of(Boolean.class);
    static final Type Byte = Type.of(Byte.class);
    static final Type Character = Type.of(Character.class);
    static final Type Class = Type.of(Class.class);
    static final Type Double = Type.of(Double.class);
    static final Type Float = Type.of(Float.class);
    static final Type Integer = Type.of(Integer.class);
    static final Type Long = Type.of(Long.class);
    static final Type Number = Type.of(Number.class);
    static final Type Object = Type.of(Object.class);
    static final Type Object_ = Type.of(Object[].class);
    static final Type Short = Type.of(Short.class);
    static final Type String = Type.of(String.class);
    static final Type StringBuilder = Type.of(StringBuilder.class);
    static final Type Throwable = Type.of(Throwable.class);
    static final Type Void = Type.of(Void.class);

    // java.lang.invoke
    static final Type MethodHandle = Type.of(MethodHandle.class);
    static final Type MethodType = Type.of(MethodType.class);
}
