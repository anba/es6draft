/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Mutable reference type.
 */
public final class MutRef<T> {
    private T value;

    /**
     * Constructs a new {@code MutRef} object.
     * 
     * @param value
     *            the referenced value
     */
    public MutRef(T value) {
        this.value = value;
    }

    /**
     * Returns the referenced value.
     * 
     * @return the referenced value
     */
    public T get() {
        return value;
    }

    /**
     * Sets the referenced value to <var>value</var>.
     * 
     * @param value
     *            the new referenced value
     */
    public void set(T value) {
        this.value = value;
    }
}
