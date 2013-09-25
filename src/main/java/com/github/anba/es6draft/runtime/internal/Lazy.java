/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Class to perform lazy value computation.
 */
public abstract class Lazy<VALUE> {
    private VALUE value = null;

    /**
     * Computes the actual value lazily
     */
    protected abstract VALUE computeValue();

    /**
     * Returns the value for this object, calls {@link #computeValue()} to retrieve the value
     * initially
     */
    public VALUE get() {
        if (value == null) {
            value = computeValue();
        }
        return value;
    }
}
