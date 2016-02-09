/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Objects;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.objects.ErrorObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/** 
 *
 */
@SuppressWarnings("serial")
public final class UnhandledRejectionException extends RuntimeException {
    private final Object value;

    /**
     * Creates a new {@link UnhandledRejectionException} instance.
     * 
     * @param value
     *            the wrapped exception value
     */
    public UnhandledRejectionException(Object value) {
        this.value = value;
    }

    public Throwable getCauseIfPresent() {
        if (value instanceof ErrorObject) {
            return ((ErrorObject) value).getException();
        }
        return this;
    }

    @Override
    public String getMessage() {
        return Objects.toString(value);
    }

    /**
     * Returns the message string of this exception.
     * 
     * @param cx
     *            the execution context
     * @return the exception error message
     */
    public String getMessage(ExecutionContext cx) {
        try {
            return AbstractOperations.ToFlatString(cx, value);
        } catch (ScriptException t) {
            if (value instanceof ScriptObject) {
                return cx.getRealm().message(Messages.Key.ToStringFailed);
            }
            return Objects.toString(value);
        }
    }
}
