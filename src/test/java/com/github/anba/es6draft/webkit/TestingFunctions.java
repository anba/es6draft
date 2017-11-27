/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.DetachArrayBuffer;

import java.io.IOException;
import java.util.Objects;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Stub functions for tests.
 */
public final class TestingFunctions {
    @Value(name = "window")
    public ScriptObject window(ExecutionContext cx) {
        return cx.getRealm().getGlobalThis();
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
    public void checkModuleSyntax(ExecutionContext cx, String sourceCode) {
        Source source = new Source("<module>", 1);
        cx.getRealm().getScriptLoader().parseModule(source, sourceCode);
    }

    @Function(name = "transferArrayBuffer", arity = 1)
    public void transferArrayBuffer(ExecutionContext cx, ArrayBufferObject arrayBuffer) {
        DetachArrayBuffer(cx, arrayBuffer);
    }

    @Function(name = "createGlobalObject", arity = 0)
    public ScriptObject createGlobalObject(ExecutionContext cx) {
        Realm realm;
        try {
            realm = Realm.InitializeHostDefinedRealm(cx.getRealm().getWorld());
        } catch (IOException e) {
            throw Errors.newError(cx, Objects.toString(e.getMessage(), ""));
        }
        return realm.getGlobalThis();
    }

    @Function(name = "drainMicrotasks", arity = 0)
    public void drainMicrotasks(ExecutionContext cx) {
        cx.getRealm().getWorld().runEventLoop();
    }
}
