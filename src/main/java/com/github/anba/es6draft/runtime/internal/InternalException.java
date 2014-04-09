/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * Base class for internal exceptions
 */
@SuppressWarnings("serial")
public abstract class InternalException extends RuntimeException {
    /**
     * InternalException constructor
     * 
     * @see RuntimeException#RuntimeException()
     */
    public InternalException() {
        super();
    }

    /**
     * InternalException constructor
     * 
     * @param message
     *            the error message
     * @see RuntimeException#RuntimeException(String)
     */
    public InternalException(String message) {
        super(message);
    }

    /**
     * InternalException constructor
     * 
     * @param message
     *            the error message
     * @param cause
     *            the exception cause
     * @see RuntimeException#RuntimeException(String, Throwable)
     */
    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * InternalException constructor
     * 
     * @param cause
     *            the exception cause
     * @see RuntimeException#RuntimeException(Throwable)
     */
    public InternalException(Throwable cause) {
        super(cause);
    }

    /**
     * InternalException constructor
     * 
     * @param message
     *            the error message
     * @param cause
     *            the exception cause
     * @param enableSuppression
     *            the enableSuppression flag
     * @param writableStackTrace
     *            the writableStackTrace flag
     * @see RuntimeException#RuntimeException(String, Throwable, boolean, boolean)
     */
    protected InternalException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Returns a {@link ScriptException} for this exception object.
     * 
     * @param cx
     *            the execution context to construct the script exception
     * @return the script exception for this exception.
     */
    public abstract ScriptException toScriptException(ExecutionContext cx);
}
