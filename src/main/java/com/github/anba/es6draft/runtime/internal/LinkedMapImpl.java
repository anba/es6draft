/**
 * Copyright (c) 2012-2013 André Bargull
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
    private boolean sameZero;

    /**
     * If {@code sameZero} is <code>true</code>, both, positive and negative zero, will be mapped to
     * positive zero for hashing
     */
    public LinkedMapImpl(boolean sameZero) {
        super(LinkedMap.HashMapBuilder);
        this.sameZero = sameZero;
    }

    @Override
    protected Object hashKey(Object key) {
        switch (Type.of(key)) {
        case Number:
            // int/long/double -> double
            double v = Type.numberValue(key);
            return (v == 0 && sameZero ? +0.0 : v);
        case String:
            // String/ConsString -> String
            return Type.stringValue(key).toString();
        case Undefined:
        case Null:
        case Boolean:
        case Object:
        default:
            return key;
        }
    }
}
