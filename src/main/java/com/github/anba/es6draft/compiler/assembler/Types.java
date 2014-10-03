/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import org.objectweb.asm.Type;

/**
 *
 */
final class Types {
    private Types() {
    }

    // java.lang
    static final Type Boolean = Type.getType(Boolean.class);
    static final Type Byte = Type.getType(Byte.class);
    static final Type Character = Type.getType(Character.class);
    static final Type Class = Type.getType(Class.class);
    static final Type Double = Type.getType(Double.class);
    static final Type Float = Type.getType(Float.class);
    static final Type Integer = Type.getType(Integer.class);
    static final Type Long = Type.getType(Long.class);
    static final Type Number = Type.getType(Number.class);
    static final Type Object = Type.getType(Object.class);
    static final Type Object_ = Type.getType(Object[].class);
    static final Type Short = Type.getType(Short.class);
    static final Type String = Type.getType(String.class);
    static final Type StringBuilder = Type.getType(StringBuilder.class);
    static final Type Throwable = Type.getType(Throwable.class);
    static final Type Void = Type.getType(Void.class);

    // java.lang.invoke
    static final Type MethodHandle = Type.getType(MethodHandle.class);
    static final Type MethodType = Type.getType(MethodType.class);
}
