/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * 
 */
public final class ErrorOperations {
    private ErrorOperations() {
    }

    /**
     * 13.14 The try Statement
     * 
     * @param e
     *            the error cause
     * @return if either <var>e</var> or its cause is a stack overflow error, that error object
     * @throws Error
     *             if neither the error nor its cause is a stack overflow error
     */
    public static StackOverflowError stackOverflowError(Error e) throws Error {
        if (e instanceof StackOverflowError) {
            return (StackOverflowError) e;
        }
        if (e instanceof BootstrapMethodError) {
            Throwable cause = e.getCause();
            if (cause instanceof StackOverflowError) {
                return (StackOverflowError) cause;
            }
        }
        throw e;
    }

    /**
     * 13.14 The try Statement
     * 
     * @param e
     *            the error cause
     * @param cx
     *            the execution context
     * @return the new script exception
     */
    public static ScriptException toInternalError(StackOverflowError e, ExecutionContext cx) {
        ScriptException exception = newInternalError(cx, Messages.Key.StackOverflow);
        // use stacktrace from original error
        exception.setStackTrace(e.getStackTrace());
        return exception;
    }
}
