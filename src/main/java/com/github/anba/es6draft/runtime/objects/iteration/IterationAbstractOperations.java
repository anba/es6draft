/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.19 The "std:iteration" Module</h2><br>
 * <h3>15.19.4 Generator Objects</h3>
 * <ul>
 * <li>15.19.4.3 Iteration Related Abstract Operations
 * </ul>
 */
public final class IterationAbstractOperations {
    private IterationAbstractOperations() {
    }

    /**
     * 15.19.4.3.1 GeneratorStart (generator, generatorBody)
     */
    public static GeneratorObject GeneratorStart(ExecutionContext cx, GeneratorObject generator,
            RuntimeInfo.Code generatorBody) {
        generator.start(cx, generatorBody);
        return generator;
    }

    /**
     * 15.19.4.3.2 GeneratorResume (generator, value)
     */
    public static Object GeneratorResume(ExecutionContext cx, Object generator, Object value) {
        if (!Type.isObject(generator)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        if (!(generator instanceof GeneratorObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        return ((GeneratorObject) generator).resume(cx, value);
    }

    /**
     * 15.19.4.3.3 GeneratorYield (itrNextObj)
     */
    public static Object GeneratorYield(ExecutionContext genContext, ScriptObject itrNextObj) {
        GeneratorObject generator = genContext.getCurrentGenerator();
        assert generator != null;
        return generator.yield(itrNextObj);
    }

    /**
     * 15.19.4.3.4 CreateItrResultObject (value, done)
     */
    public static ScriptObject CreateItrResultObject(ExecutionContext cx, Object value, boolean done) {
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        CreateOwnDataProperty(cx, obj, "value", value);
        CreateOwnDataProperty(cx, obj, "done", done);
        return obj;
    }

    /**
     * 15.19.4.3.5 IteratorComplete (itrResult)
     */
    public static boolean IteratorComplete(ExecutionContext cx, ScriptObject itrResult) {
        Object done = Get(cx, itrResult, "done");
        return ToBoolean(done);
    }

    /**
     * 15.19.4.3.6 IteratorValue (itrResult)
     */
    public static Object IteratorValue(ExecutionContext cx, ScriptObject itrResult) {
        return Get(cx, itrResult, "value");
    }
}
