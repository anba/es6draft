/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.types.Type;

/**
 * {@link LinkedMap} implementation with additional changes to {@link #hashKey(Object)} to properly
 * support runtime types.
 */
public final class LinkedMapImpl<VALUE> extends LinkedMap<Object, VALUE> {
    @Override
    protected Object hashKey(Object key) {
        if (Type.isString(key)) {
            // String/ConsString -> String
            return Type.stringValue(key).toString();
        }
        if (Type.isNumber(key)) {
            // int/long/double -> double
            double v = Type.numberValue(key);
            // Map +/-0 to +0 to enforce SameValueZero comparison semantics
            return v == 0 ? +0d : v;
        }
        return key;
    }
}
