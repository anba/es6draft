/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.2 Modules
 * </ul>
 */
public interface Module extends Program {
    /**
     * Returns this modules's
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.ModuleBody ModuleBody} object.
     * 
     * @return the module body object
     */
    RuntimeInfo.ModuleBody getModuleBody();

    /**
     * Evaluates this module in the given {@link ExecutionContext}.
     * 
     * @param cx
     *            the execution context
     * @return the return value after evaluating this module
     */
    default Object evaluate(ExecutionContext cx) {
        return getModuleBody().evaluate(cx);
    }
}
