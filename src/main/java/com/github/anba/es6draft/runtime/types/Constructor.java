/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2><br>
 * <h3>6.1.7 The Object Type</h3>
 * <ul>
 * <li>6.1.7.2 Object Internal Methods and Internal Slots
 * </ul>
 * <p>
 * Internal Method: [[Construct]]
 */
public interface Constructor extends ScriptObject, Callable {
    /**
     * [[Construct]]
     * 
     * @param callerContext
     *            the caller's execution context
     * @param newTarget
     *            the constructor to which the {@code new} operator was initially applied
     * @param args
     *            the constructor function arguments
     * @return the new script object
     */
    ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args);

    /**
     * [[Construct]] in tail-call position
     * 
     * @param callerContext
     *            the caller's execution context
     * @param newTarget
     *            the constructor to which the {@code new} operator was initially applied
     * @param args
     *            the constructor function arguments
     * @return the new script object
     * @throws Throwable
     *             any error thrown by the underlying method implementation
     */
    Object tailConstruct(ExecutionContext callerContext, Constructor newTarget, Object... args)
            throws Throwable;
}
