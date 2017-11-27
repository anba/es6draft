/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Holds the value component of an abrupt return completion in generators.
 */
public final class ReturnValue {
    private final Object value;

    /**
     * Constructs a new ReturnValue instance with the given completion value.
     * 
     * @param value
     *            the completion value
     */
    public ReturnValue(Object value) {
        this.value = value;
    }

    /**
     * Returns the completion value.
     * 
     * @return the completion value
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("Return<%s>", value);
    }
}
