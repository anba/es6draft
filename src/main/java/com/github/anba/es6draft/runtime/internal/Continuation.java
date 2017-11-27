/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * 
 */
public interface Continuation<VALUE> {
    /**
     * 
     */
    interface Handler<VALUE> {
        /**
         * Starts or continues evaluation of the underlying code.
         * 
         * @param resumptionPoint
         *            the optional resumption point, {@code null} if unused
         * @return the result value
         * @throws Throwable
         *             anything thrown by continuation code
         */
        Object evaluate(ResumptionPoint resumptionPoint) throws Throwable;

        /**
         * Called after computation has finished to release internal resources.
         */
        void close();

        /**
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the value
         * @return the result value
         */
        VALUE suspendWith(ExecutionContext cx, Object value);

        /**
         * 
         * @param cx
         *            the execution context
         * @param result
         *            the result value
         * @return the result value
         */
        VALUE returnWith(ExecutionContext cx, Object result);

        /**
         * 
         * @param cx
         *            the execution context
         * @param exception
         *            the script exception
         * @return the result value
         */
        // FIXME: Rename to 'throwWith' ??
        VALUE returnWith(ExecutionContext cx, ScriptException exception);
    }

    /**
     * Starts generator execution.
     * 
     * @param cx
     *            the execution context
     * @return the result value
     */
    VALUE start(ExecutionContext cx);

    /**
     * Resumes generator execution.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the resumption value
     * @return the result value
     */
    VALUE resume(ExecutionContext cx, Object value);

    /**
     * Resumes generator execution with a return instruction.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the return value
     * @return the result value
     */
    VALUE _return(ExecutionContext cx, Object value);

    /**
     * Resumes generator execution with an exception.
     * 
     * @param cx
     *            the execution context
     * @param exception
     *            the exception object
     * @return the result value
     */
    VALUE _throw(ExecutionContext cx, ScriptException exception);
}
