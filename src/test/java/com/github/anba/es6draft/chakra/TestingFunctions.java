/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.chakra;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties;
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

    /** shell-function: {@code $async_enqueueTask(task)} */
    @Properties.Function(name = "$async_enqueueTask", arity = 1)
    public void enqueueTask(ExecutionContext cx, Callable task) {
        cx.getRealm().enqueuePromiseTask(() -> task.call(cx, Undefined.UNDEFINED));
    }
}
