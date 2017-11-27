/**
 * Copyright (c) André Bargull
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
    /**
     * Returns the value for this object.
     * 
     * @return the computed value
     */
    public abstract VALUE get();

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
        private V value;

        LazyImpl(Supplier<V> supplier) {
            this.supplier = supplier;
        }

        @Override
        public V get() {
            V value = this.value;
            if (value == null) {
                this.value = value = supplier.get();
                supplier = null;
            }
            return value;
        }
    }

    private static final class SyncLazyImpl<V> extends Lazy<V> {
        private Supplier<V> supplier;
        private volatile V value;

        SyncLazyImpl(Supplier<V> supplier) {
            this.supplier = supplier;
        }

        @Override
        public V get() {
            V value = this.value;
            if (value == null) {
                synchronized (this) {
                    value = this.value;
                    if (value == null) {
                        this.value = value = supplier.get();
                        supplier = null;
                    }
                }
            }
            return value;
        }
    }
}
