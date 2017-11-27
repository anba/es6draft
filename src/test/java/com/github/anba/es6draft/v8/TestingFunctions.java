/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations;
import com.github.anba.es6draft.runtime.objects.reflect.RealmObject;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    /** shell-function: {@code gc()} */
    @Function(name = "gc", arity = 0)
    public void gc() {
        // empty
    }

    /**
     * shell-function: {@code getDefaultLocale()}
     * 
     * @param cx
     *            the execution context
     * @return the default locale
     */
    @Function(name = "getDefaultLocale", arity = 0)
    public String getDefaultLocale(ExecutionContext cx) {
        return IntlAbstractOperations.DefaultLocale(cx.getRealm());
    }

    /**
     * shell-function: {@code getDefaultTimeZone()}
     * 
     * @param cx
     *            the execution context
     * @return the default timezone
     */
    @Function(name = "getDefaultTimeZone", arity = 0)
    public String getDefaultTimeZone(ExecutionContext cx) {
        return IntlAbstractOperations.DefaultTimeZone(cx.getRealm());
    }

    /**
     * shell-function: {@code getCurrentRealm()}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @return the current realm object
     */
    @Function(name = "getCurrentRealm", arity = 0)
    public RealmObject getCurrentRealm(ExecutionContext cx, ExecutionContext caller) {
        return caller.getRealm().getRealmObject();
    }

    /**
     * shell-function: {@code evalInRealm(realm, sourceString)}
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param realmObject
     *            the target realm
     * @param sourceString
     *            the source string
     * @return the evaluation result
     */
    @Function(name = "evalInRealm", arity = 2)
    public Object evalInRealm(ExecutionContext cx, ExecutionContext caller, RealmObject realmObject,
            String sourceString) {
        Source source = new Source(caller.sourceInfo(), "<evalInRealm>", 1);
        Script script = realmObject.getRealm().getScriptLoader().script(source, sourceString);
        return script.evaluate(realmObject.getRealm());
    }
}
