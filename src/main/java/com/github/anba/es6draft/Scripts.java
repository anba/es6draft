/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;

/**
 * <h1>15 ECMAScript Language: Scripts and Modules</h1>
 * <ul>
 * <li>15.1 Scripts
 * </ul>
 */
public final class Scripts {
    private Scripts() {
    }

    /**
     * 15.1.7 Runtime Semantics: ScriptEvaluation
     * 
     * @param script
     *            the script object
     * @param realm
     *            the realm instance
     * @return the script evaluation result
     */
    public static Object ScriptEvaluation(Script script, Realm realm) {
        /* steps 1-2 (not applicable) */
        /* steps 3-7 */
        ExecutionContext scriptCxt = newScriptExecutionContext(realm, script);
        /* steps 8-9 */
        ExecutionContext oldScriptContext = realm.getScriptContext();
        try {
            realm.setScriptContext(scriptCxt);
            /* step 10 */
            script.getScriptBody().globalDeclarationInstantiation(scriptCxt);
            /* steps 11-12 */
            Object result = script.evaluate(scriptCxt);
            /* step 16 */
            return result;
        } finally {
            /* steps 13-15  */
            realm.setScriptContext(oldScriptContext);
        }
    }
}
