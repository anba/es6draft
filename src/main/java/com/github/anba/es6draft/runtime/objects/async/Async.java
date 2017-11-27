/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * 
 */
public interface Async {
    /**
     * Resumes async function execution.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the resumption value
     */
    void resume(ExecutionContext cx, Object value);

    /**
     * Stops async function execution with a {@code throw} event.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the exception value
     */
    void _throw(ExecutionContext cx, Object value);
}
