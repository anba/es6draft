/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.chakra;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    /** shell-function: {@code DebugPrint(message)} */
    @Function(name = "DebugPrint", arity = 1)
    public void DebugPrint(String message) {
        System.out.println(message);
    }

    /** shell-function: {@code $async_enqueueJob(job)} */
    @Function(name = "$async_enqueueJob", arity = 1)
    public void enqueueJob(ExecutionContext cx, Callable job) {
        cx.getRealm().enqueuePromiseJob(() -> job.call(cx, Undefined.UNDEFINED));
    }
}
