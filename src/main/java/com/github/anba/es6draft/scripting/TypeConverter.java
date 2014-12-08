/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.runtime.types.Null.NULL;

import com.github.anba.es6draft.runtime.types.Type;

/**
 * Type conversions from/to Java types.
 */
final class TypeConverter {
    private TypeConverter() {
    }

    /**
     * Converts a script language value to a Java value.
     * 
     * @param value
     *            the script value
     * @return the java value
     */
    public static Object toJava(Object value) {
        switch (Type.of(value)) {
        case Undefined:
        case Null:
            return null;
        case Number:
            return Type.numberValue(value);
        case String:
            return value.toString();
        case Boolean:
        case Symbol:
        case Object:
        default:
            return value;
        }
    }

    /**
     * Converts script language values to a Java values.
     * 
     * @param values
     *            the script values
     * @return the java values
     */
    public static Object[] toJava(Object... values) {
        Object[] converted = new Object[values.length];
        for (int i = 0; i < values.length; ++i) {
            converted[i] = toJava(values[i]);
        }
        return converted;
    }

    /**
     * Converts a Java value to a script language value.
     * 
     * @param value
     *            the java value
     * @return the script value
     */
    public static Object fromJava(Object value) {
        if (Type.isType(value)) {
            return value;
        }
        return NULL;
    }

    /**
     * Converts Java values to a script language values.
     * 
     * @param values
     *            the java values
     * @return the script values
     */
    public static Object[] fromJava(Object... values) {
        Object[] converted = new Object[values.length];
        for (int i = 0; i < values.length; ++i) {
            converted[i] = fromJava(values[i]);
        }
        return converted;
    }
}
