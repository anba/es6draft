/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Holds the value component of an abrupt return completion in generators.
 */
@SuppressWarnings("serial")
public final class ReturnValue extends Throwable {
    private final Object value;

    /**
     * Constructs a new ReturnValue instance with the given value.
     * 
     * @param value
     *            the completion value
     */
    public ReturnValue(Object value) {
        super("ReturnValue", null, false, false);
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
}
