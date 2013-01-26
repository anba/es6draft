/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import org.mozilla.javascript.ConsString;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.1 ECMAScript Language Types</h2>
 * <ul>
 * <li>8.1.1 The Undefined Type
 * <li>8.1.2 The Null Type
 * <li>8.1.3 The Boolean Type
 * <li>8.1.4 The String Type
 * <li>8.1.5 The Number Type
 * <li>8.1.6 The Object Type
 * </ul>
 */
public enum Type {
    Undefined, Null, Boolean, Number, String, Object;

    public static Type of(Object val) {
        if (val == UNDEFINED) {
            return Undefined;
        }
        if (val == NULL) {
            return Null;
        }
        if (val instanceof Boolean) {
            return Boolean;
        }
        if (val instanceof String || val instanceof ConsString) {
            return String;
        }
        if (val instanceof Double || val instanceof Integer || val instanceof Long) {
            return Number;
        }
        assert val instanceof Scriptable : (val != null ? val.getClass() : "<null>");
        return Object;
    }

    public static boolean isUndefined(Object val) {
        return (val == UNDEFINED);
    }

    public static boolean isNull(Object val) {
        return (val == NULL);
    }

    public static boolean isUndefinedOrNull(Object val) {
        return (val == UNDEFINED || val == NULL);
    }

    public static boolean isBoolean(Object val) {
        return (val instanceof Boolean);
    }

    public static boolean isString(Object val) {
        return (val instanceof String || val instanceof ConsString);
    }

    public static boolean isNumber(Object val) {
        return (val instanceof Double || val instanceof Integer || val instanceof Long);
    }

    public static boolean isObject(Object val) {
        return (val instanceof Scriptable);
    }

    public static boolean booleanValue(Object val) {
        return ((Boolean) val).booleanValue();
    }

    public static double numberValue(Object val) {
        return ((Number) val).doubleValue();
    }

    public static CharSequence stringValue(Object val) {
        return (CharSequence) val;
    }

    public static Scriptable objectValue(Object val) {
        return (Scriptable) val;
    }
}