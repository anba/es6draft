/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static org.junit.Assert.assertFalse;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.util.TestAssertions;

/**
 * {@code $DONE} test helper function, replaces the definition from "doneprintHandle.js".
 */
public final class Test262Async {
    private boolean doneCalled = false;

    public boolean isDone() {
        return doneCalled;
    }

    @Properties.Function(name = "$DONE", arity = 0)
    public void done(ExecutionContext cx, Object argument) {
        assertFalse(doneCalled);
        doneCalled = true;
        if (ToBoolean(argument)) {
            // Directly throw an AssertionError to bypass the Promise machinery.
            throw TestAssertions.toAssertionError(cx, ScriptException.create(argument));
        }
    }
}
