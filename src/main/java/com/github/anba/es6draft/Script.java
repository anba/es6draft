/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>14 Scripts and Modules</h1>
 * <ul>
 * <li>14.1 Script
 * </ul>
 */
public interface Script {
    RuntimeInfo.ScriptBody getScriptBody();

    Object evaluate(ExecutionContext cx);
}
