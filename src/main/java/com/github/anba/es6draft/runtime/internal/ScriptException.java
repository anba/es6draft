/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Objects;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * Runtime exception to represent exceptions thrown from the ThrowStatement
 */
@SuppressWarnings("serial")
public final class ScriptException extends RuntimeException {
    private final Object value;

    /**
     * Create a new {@link ScriptException} instance
     */
    public ScriptException(Object value) {
        this.value = value;
    }

    /**
     * Creates a new {@link ScriptException} instance, unless {@code value} is an instance of
     * {@link ErrorObject}, in that case {@link ErrorObject#getException()} is returned
     */
    public static ScriptException create(Object value) {
        if (value instanceof ErrorObject) {
            return ((ErrorObject) value).getException();
        }
        return new ScriptException(value);
    }

    /**
     * Returns the wrapped value of this exception
     */
    public Object getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        if (!Type.isObject(value)) {
            return AbstractOperations.ToFlatString(null, value);
        }
        return Objects.toString(value);
    }

    /**
     * Returns the message string of this exception
     */
    public String getMessage(ExecutionContext cx) {
        try {
            return AbstractOperations.ToFlatString(cx, value);
        } catch (ScriptException t) {
            return Objects.toString(value);
        }
    }
}
