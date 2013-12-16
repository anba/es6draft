/**
 * Copyright (c) 2012-2013 André Bargull
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
     */
    ScriptObject construct(ExecutionContext callerContext, Object... args);

    /**
     * [[Construct]] in tail-call position
     */
    Object tailConstruct(ExecutionContext callerContext, Object... args) throws Throwable;

    /**
     * [[Construct]] internal method is added dynamically to objects, but interfaces cannot be added
     * dynamically, therefore add an extra predicate to test whether the [[Construct]] method is
     * already attached to the object
     */
    boolean isConstructor();
}
