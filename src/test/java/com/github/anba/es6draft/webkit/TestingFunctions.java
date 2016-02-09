/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.DetachArrayBuffer;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    @Value(name = "arguments")
    public ScriptObject arguments(ExecutionContext cx) {
        return ArrayCreate(cx, 0);
    }

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

    @Function(name = "checkModuleSyntax", arity = 1)
    public void checkModuleSyntax(ExecutionContext cx, String source) {
        cx.getRealm().getScriptLoader().parseModule(new Source("<module>", 1), source);
    }

    @Function(name = "transferArrayBuffer", arity = 1)
    public void transferArrayBuffer(ExecutionContext cx, ArrayBufferObject arrayBuffer) {
        DetachArrayBuffer(cx, arrayBuffer);
    }
}
