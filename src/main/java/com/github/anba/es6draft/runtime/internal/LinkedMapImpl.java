/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.objects.simd.SIMD.SIMDCreate;

import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.runtime.objects.simd.SIMDValue;

/**
 * {@link LinkedMap} implementation with additional changes to {@link #newEntry(Object, Object)} and
 * {@link #hashKey(Object)} to properly support runtime types.
 */
public final class LinkedMapImpl<VALUE> extends LinkedMap<Object, VALUE> {
    @Override
    protected Entry<Object, VALUE> newEntry(Object key, VALUE value) {
        if (key instanceof Double) {
            // Map +/-0 to +0 per Map.prototype.set and Set.prototype.add.
            double v = (Double) key;
            return super.newEntry(v == 0 ? +0d : v, value);
        }
        return super.newEntry(key, value);
    }

    @Override
    protected Object hashKey(Object key) {
        if (key instanceof ConsString) {
            // ConsString -> String
            return ((ConsString) key).toString();
        }
        if (key instanceof Integer) {
            // int -> double
            return (double) (Integer) key;
        }
        if (key instanceof Long) {
            // long -> double
            return (double) (Long) key;
        }
        if (key instanceof Double) {
            // Map +/-0 to +0 to enforce SameValueZero comparison semantics.
            double v = (Double) key;
            return v == 0 ? +0d : v;
        }
        if (key instanceof SIMDValue) {
            // Map +/-0 to +0 to enforce SameValueZero comparison semantics.
            return hashKeySIMD((SIMDValue) key);
        }
        return key;
    }

    private SIMDValue hashKeySIMD(SIMDValue value) {
        if (!value.getType().isFloatingPoint()) {
            return value;
        }
        double[] copy = null, elements = value.asDouble();
        for (int i = 0; i < elements.length; ++i) {
            double v = elements[i];
            if (v == 0 && Double.doubleToRawLongBits(v) != 0L) {
                if (copy == null) {
                    copy = elements.clone();
                }
                copy[i] = +0d;
            }
        }
        if (copy != null) {
            return SIMDCreate(value.getType(), copy);
        }
        return value;
    }
}
