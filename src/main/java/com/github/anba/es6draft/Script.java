/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1>
 * <ul>
 * <li>15.2 Script
 * </ul>
 */
public interface Script {
    /**
     * Returns this script's
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.ScriptBody} object
     */
    RuntimeInfo.ScriptBody getScriptBody();

    /**
     * Evaluates this script in the given {@link ExecutionContext}
     */
    Object evaluate(ExecutionContext cx);
}
