/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;

/**
 * <h1>6 ECMAScript Data Types and Values</h1><br>
 * <h2>6.1 ECMAScript Language Types</h2><br>
 * <h3>6.1.7 The Object Type</h3>
 * <ul>
 * <li>6.1.7.2 Object Internal Methods and Internal Slots
 * </ul>
 * <p>
 * Internal Method: [[Call]]
 */
public interface Callable extends ScriptObject {
    /**
     * [[Call]]
     * 
     * @param callerContext
     *            the caller's execution context
     * @param thisValue
     *            the this-value
     * @param args
     *            the function arguments
     * @return the function return value
     */
    Object call(ExecutionContext callerContext, Object thisValue, Object... args);

    /**
     * [[Call]] in tail-call position
     * 
     * @param callerContext
     *            the caller's execution context
     * @param thisValue
     *            the this-value
     * @param args
     *            the function arguments
     * @return the function return value
     * @throws Throwable
     *             any error thrown by the underlying method implementation
     */
    Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args) throws Throwable;

    /**
     * Returns the function's realm component.
     * 
     * @param cx
     *            the execution context
     * @return the function's realm
     */
    Realm getRealm(ExecutionContext cx);

    @Override
    default String className() {
        return "Function";
    }

    /**
     * Source representation of this callable.
     * 
     * @param cx
     *            the execution context
     * @return the function source
     */
    String toSource(ExecutionContext cx);
}
