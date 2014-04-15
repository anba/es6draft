/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.objects.intl.IntlAbstractOperations;

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
}
