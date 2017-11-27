/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.promise;

import static org.junit.Assert.assertFalse;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 * 
 */
public final class PromiseAsync {
    private boolean doneCalled = false;

    public boolean isDone() {
        return doneCalled;
    }

    @Properties.Function(name = "$async_done", arity = 0)
    public void done() {
        assertFalse(doneCalled);
        doneCalled = true;
    }

    @Properties.Function(name = "$async_enqueueJob", arity = 1)
    public void enqueueJob(ExecutionContext cx, Callable job) {
        cx.getRealm().enqueuePromiseJob(() -> job.call(cx, Undefined.UNDEFINED));
    }
}
