/**
 * Copyright (c) 2012-2014 André Bargull
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
     * Returns the {@link Type} of the input parameter.
     * 
     * @param value
     *            the value object
     * @return the script type
     */
    public static Type of(Object value) {
        if (value == UNDEFINED) {
            return Undefined;
        }
        if (value == NULL) {
            return Null;
        }
        if (value instanceof Boolean) {
            return Boolean;
        }
        if (value instanceof String || value instanceof ConsString) {
            return String;
        }
        if (value instanceof Symbol) {
            return Symbol;
        }
        if (value instanceof Double || value instanceof Integer || value instanceof Long) {
            return Number;
        }
        assert value instanceof ScriptObject : (value != null ? value.getClass() : "<null>");
        return Object;
    }

    /**
     * Returns <code>true</code> if {@code value} is a valid language type.
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is a valid language type
     */
    public static boolean isType(Object value) {
        if (value == UNDEFINED) {
            return true;
        }
        if (value == NULL) {
            return true;
        }
        if (value instanceof Boolean) {
            return true;
        }
        if (value instanceof String || value instanceof ConsString) {
            return true;
        }
        if (value instanceof Symbol) {
            return true;
        }
        if (value instanceof Double || value instanceof Integer || value instanceof Long) {
            return true;
        }
        if (value instanceof ScriptObject) {
            return true;
        }
        return false;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Undefined</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is undefined
     */
    public static boolean isUndefined(Object value) {
        return value == UNDEFINED;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Null</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is null
     */
    public static boolean isNull(Object value) {
        return value == NULL;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Undefined || Type.of(value) == Type.Null</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is either undefined or null
     */
    public static boolean isUndefinedOrNull(Object value) {
        return value == UNDEFINED || value == NULL;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Boolean</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is a boolean
     */
    public static boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.String</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is a string
     */
    public static boolean isString(Object value) {
        return value instanceof String || value instanceof ConsString;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Symbol</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is a symbol
     */
    public static boolean isSymbol(Object value) {
        return value instanceof Symbol;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Number</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is a number
     */
    public static boolean isNumber(Object value) {
        return value instanceof Double || value instanceof Integer || value instanceof Long;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Object</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is an object
     */
    public static boolean isObject(Object value) {
        return value instanceof ScriptObject;
    }

    /**
     * Short cut for:<br>
     * <code>Type.of(value) == Type.Object || Type.of(value) == Type.Null</code>
     * 
     * @param value
     *            the value object
     * @return {@code true} if the value is either an object or null
     */
    public static boolean isObjectOrNull(Object value) {
        return value == NULL || value instanceof ScriptObject;
    }

    /**
     * If {@code value} is a Boolean type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     * 
     * @param value
     *            the value object
     * @return the boolean value
     */
    public static boolean booleanValue(Object value) {
        return ((Boolean) value).booleanValue();
    }

    /**
     * If {@code value} is a String type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     * 
     * @param value
     *            the value object
     * @return the string value
     */
    public static CharSequence stringValue(Object value) {
        return (CharSequence) value;
    }

    /**
     * If {@code value} is a Symbol type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     * 
     * @param value
     *            the value object
     * @return the symbol value
     */
    public static Symbol symbolValue(Object value) {
        return (Symbol) value;
    }

    /**
     * If {@code value} is a Number type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     * 
     * @param value
     *            the value object
     * @return the number value
     */
    public static double numberValue(Object value) {
        return ((Number) value).doubleValue();
    }

    /**
     * If {@code value} is an Object type, its value is returned. Otherwise a
     * {@link ClassCastException} is thrown.
     * 
     * @param value
     *            the value object
     * @return the script object
     */
    public static ScriptObject objectValue(Object value) {
        return (ScriptObject) value;
    }

    /**
     * If {@code value} is an Object type, its value is returned. If {@code value} is a Null type,
     * <code>null</code> is returned. Otherwise a {@link ClassCastException} is thrown.
     * 
     * @param value
     *            the value object
     * @return the script object or {@code null}
     */
    public static ScriptObject objectValueOrNull(Object value) {
        return value == NULL ? null : (ScriptObject) value;
    }
}
