/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Nullable reference type.
 */
public final class Ref<T> {
    private T value;

    /**
     * Constructs a new {@code Ref} object.
     * 
     * @param value
     *            the referenced value
     */
    public Ref(T value) {
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
     * Clears this reference, i.e. sets the reference to {@code null}.
     */
    public void clear() {
        value = null;
    }
}
