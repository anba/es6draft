/**
 * Copyright (c) 2012-2013 André Bargull
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
     * {@inheritDoc}
     */
    public InternalException() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public InternalException(String message) {
        super(message);
    }

    /**
     * {@inheritDoc}
     */
    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     */
    public InternalException(Throwable cause) {
        super(cause);
    }

    /**
     * {@inheritDoc}
     */
    protected InternalException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Returns a {@link ScriptException} for this exception object
     */
    public abstract ScriptException toScriptException(ExecutionContext cx);
}
