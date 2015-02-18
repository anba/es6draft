/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Base class for internal exceptions.
 */
@SuppressWarnings("serial")
public abstract class InternalException extends RuntimeException implements InternalThrowable {
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
}
