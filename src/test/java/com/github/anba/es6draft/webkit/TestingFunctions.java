/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    @Value(name = "window")
    public ScriptObject window(ExecutionContext cx) {
        return cx.getGlobalObject();
    }

    @Function(name = "neverInlineFunction", arity = 0)
    public void neverInlineFunction() {
    }

    @Function(name = "numberOfDFGCompiles", arity = 0)
    public double numberOfDFGCompiles() {
        return Double.NaN;
    }

    @Function(name = "failNextNewCodeBlock", arity = 0)
    public void failNextNewCodeBlock() {
    }

    @Function(name = "gc", arity = 0)
    public void gc() {
        System.gc();
    }
}
