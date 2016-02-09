/**
 * Copyright (c) 2012-2016 André Bargull
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
 * Runtime exception to represent exceptions thrown from the ThrowStatement.
 */
@SuppressWarnings("serial")
public final class ScriptException extends RuntimeException implements InternalThrowable {
    private final Object value;

    /**
     * Create a new {@link ScriptException} instance.
     * 
     * @param value
     *            the wrapped exception value
     */
    public ScriptException(Object value) {
        this.value = value;
    }

    /**
     * Create a new {@link ScriptException} instance.
     * 
     * @param value
     *            the wrapped exception value
     * @param cause
     *            the exception's cause
     */
    public ScriptException(Object value, Throwable cause) {
        super(cause);
        this.value = value;
    }

    /**
     * Creates a new {@link ScriptException} instance, unless {@code value} is an instance of
     * {@link ErrorObject}, in that case {@link ErrorObject#getException()} is returned.
     * 
     * @param value
     *            the wrapped exception value
     * @return the script exception instance
     */
    public static ScriptException create(Object value) {
        if (value instanceof ErrorObject) {
            return ((ErrorObject) value).getException();
        }
        return new ScriptException(value);
    }

    /**
     * Returns the wrapped value of this exception.
     * 
     * @return the wrapped exception value
     */
    public Object getValue() {
        return value;
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
     * @return the script exception error message
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

    /**
     * Returns the script stack trace elements.
     * 
     * @return the script stack trace elements
     */
    public StackTraceElement[] getScriptStackTrace() {
        return StackTraces.scriptStackTrace(this);
    }

    /**
     * Returns the native stack trace elements.
     * 
     * @return the native stack trace elements
     */
    public StackTraceElement[] getNativeStackTrace() {
        return super.getStackTrace();
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        return this;
    }
}
