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
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2>
 * <ul>
 * <li>6.1.1 The Undefined Type
 * <li>6.1.2 The Null Type
 * <li>6.1.3 The Boolean Type
 * <li>6.1.4 The String Type
 * <li>6.1.5 The Symbol Type
 * <li>6.1.6 The Number Type
 * <li>6.1.7 The Object Type
 * </ul>
 */
public enum Type {
    Undefined, Null, Boolean, String, Symbol, Number, Object;

    /**
     * Returns the {@link Type} of the input parameter
     */
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
        if (val instanceof Symbol) {
            return Symbol;
        }
        if (val instanceof Double || val instanceof Integer || val instanceof Long) {
            return Number;
        }
        assert val instanceof ScriptObject : (val != null ? val.getClass() : "<null>");
        return Object;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.Undefined</code>
     */
    public static boolean isUndefined(Object val) {
        return (val == UNDEFINED);
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.Null</code>
     */
    public static boolean isNull(Object val) {
        return (val == NULL);
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.Undefined || Type.of(val) == Type.Null</code>
     */
    public static boolean isUndefinedOrNull(Object val) {
        return (val == UNDEFINED || val == NULL);
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.Boolean</code>
     */
    public static boolean isBoolean(Object val) {
        return (val instanceof Boolean);
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.String</code>
     */
    public static boolean isString(Object val) {
        return (val instanceof String || val instanceof ConsString);
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.Symbol</code>
     */
    public static boolean isSymbol(Object val) {
        return (val instanceof Symbol);
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.Number</code>
     */
    public static boolean isNumber(Object val) {
        return (val instanceof Double || val instanceof Integer || val instanceof Long);
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(val) == Type.Object</code>
     */
    public static boolean isObject(Object val) {
        return (val instanceof ScriptObject);
    }

    /**
     * If {@code val} is a Boolean type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     */
    public static boolean booleanValue(Object val) {
        return ((Boolean) val).booleanValue();
    }

    /**
     * If {@code val} is a String type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     */
    public static CharSequence stringValue(Object val) {
        return (CharSequence) val;
    }

    /**
     * If {@code val} is a Symbol type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     */
    public static Symbol symbolValue(Object val) {
        return (Symbol) val;
    }

    /**
     * If {@code val} is a Number type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     */
    public static double numberValue(Object val) {
        return ((Number) val).doubleValue();
    }

    /**
     * If {@code val} is an Object type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     */
    public static ScriptObject objectValue(Object val) {
        return (ScriptObject) val;
    }
}