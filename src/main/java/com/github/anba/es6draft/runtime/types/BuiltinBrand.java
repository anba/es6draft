/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

/**
 * 
 */
public enum BuiltinBrand {/* @formatter:off */
    BuiltinFunction,
    BuiltinArray,
    BuiltinStringWrapper,
    BuiltinBooleanWrapper,
    BuiltinNumberWrapper,
    BuiltinMath,
    BuiltinDate,
    BuiltinRegExp,
    BuiltinError,
    BuiltinJSON,
    BuiltinArguments,
    ;/* @formatter:on */

    /**
     * Returns <code>true</code> iff object has a [[BuiltinBrand]] property with the value
     * {@code value}
     * <p>
     * This function is currently only used when the [[BuiltinBrand]] value is queried without prior
     * checks like `Type.of(x) == Type.Object`<br>
     * FIXME: possible spec bug?
     */
    public static boolean hasBuiltinBrand(Object object, BuiltinBrand value) {
        if (!(object instanceof Scriptable)) {
            return false;
        }
        return ((Scriptable) object).getBuiltinBrand() == value;
    }
}
