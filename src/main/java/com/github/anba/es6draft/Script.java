/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.1 Scripts
 * </ul>
 */
public interface Script extends Program {
    /**
     * Returns this script's
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.ScriptBody ScriptBody} object.
     * 
     * @return the script body object
     */
    RuntimeInfo.ScriptBody getScriptBody();

    /**
     * Evaluates this script in the given {@link ExecutionContext}.
     * 
     * @param realm
     *            the realm instance
     * @return the return value after evaluating this script
     */
    default Object evaluate(Realm realm) {
        return getScriptBody().evaluate(realm.defaultContext(), this);
    }

    /**
     * Evaluates this script in the given {@link ExecutionContext}.
     * 
     * @param cx
     *            the execution context
     * @return the return value after evaluating this script
     */
    default Object evaluate(ExecutionContext cx) {
        return getScriptBody().evaluate(cx, this);
    }
}
