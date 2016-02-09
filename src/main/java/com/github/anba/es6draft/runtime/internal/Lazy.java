/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.function.Supplier;

/**
 * Class to perform lazy value computation.
 */
public abstract class Lazy<VALUE> {
    private VALUE value;

    /**
     * Returns the current value.
     * 
     * @return the current value
     */
    protected final VALUE getInternal() {
        return value;
    }

    /**
     * Computes the actual value lazily.
     * 
     * @return the computed value
     */
    protected abstract VALUE computeValue();

    /**
     * Returns the value for this object, calls {@link #computeValue()} to retrieve the value initially.
     * 
     * @return the computed value
     */
    public final VALUE get() {
        if (value == null) {
            value = computeValue();
        }
        return value;
    }

    /**
     * Creates a new lazy value object using a supplier function to create the initial value.
     * 
     * @param <V>
     *            the lazy value type
     * @param supplier
     *            the value supplier function
     * @return the lazy value object
     */
    public static <V> Lazy<V> of(Supplier<V> supplier) {
        return new LazyImpl<>(supplier);
    }

    /**
     * Creates a new lazy value object using a supplier function to create the initial value.
     * 
     * @param <V>
     *            the lazy value type
     * @param supplier
     *            the value supplier function
     * @return the lazy value object
     */
    public static <V> Lazy<V> syncOf(Supplier<V> supplier) {
        return new SyncLazyImpl<>(supplier);
    }

    private static final class LazyImpl<V> extends Lazy<V> {
        private Supplier<V> supplier;

        LazyImpl(Supplier<V> supplier) {
            this.supplier = supplier;
        }

        @Override
        protected V computeValue() {
            V v = supplier.get();
            supplier = null;
            return v;
        }
    }

    private static final class SyncLazyImpl<V> extends Lazy<V> {
        private Supplier<V> supplier;

        SyncLazyImpl(Supplier<V> supplier) {
            this.supplier = supplier;
        }

        @Override
        protected synchronized V computeValue() {
            Supplier<V> supplier = this.supplier;
            // Lazy#get() is not synchronized, so supplier can be null here.
            if (supplier == null) {
                return getInternal();
            }
            V v = supplier.get();
            this.supplier = null;
            return v;
        }
    }
}
