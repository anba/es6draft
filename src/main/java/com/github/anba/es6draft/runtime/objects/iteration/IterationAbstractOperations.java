/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.Invoke;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 The "std:iteration" Module</h1><br>
 * <h2>25.4 Generator Objects</h2>
 * <ul>
 * <li>25.4.3 Iteration Related Abstract Operations
 * </ul>
 */
public final class IterationAbstractOperations {
    private IterationAbstractOperations() {
    }

    /**
     * 25.4.3.1 GeneratorStart (generator, generatorBody)
     */
    public static GeneratorObject GeneratorStart(ExecutionContext cx, GeneratorObject generator,
            RuntimeInfo.Code generatorBody) {
        /* steps 1-6 */
        generator.start(cx, generatorBody);
        /* step 7 */
        return generator;
    }

    /**
     * 25.4.3.2 GeneratorResume (generator, value)
     */
    public static Object GeneratorResume(ExecutionContext cx, Object generator, Object value) {
        /* step 1 */
        if (!Type.isObject(generator)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 2 */
        if (!(generator instanceof GeneratorObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 3-14 */
        return ((GeneratorObject) generator).resume(cx, value);
    }

    /**
     * 25.4.3.3 GeneratorYield (iterNextObj)
     */
    public static Object GeneratorYield(ExecutionContext genContext, ScriptObject iterNextObj) {
        /* step 1 (?) */
        /* steps 2-4 */
        GeneratorObject generator = genContext.getCurrentGenerator();
        assert generator != null;
        /* steps 5-11 */
        return generator.yield(iterNextObj);
    }

    /**
     * 25.4.3.4 CreateIterResultObject (value, done)
     */
    public static ScriptObject CreateIterResultObject(ExecutionContext cx, Object value,
            boolean done) {
        /* step 1 (not applicable) */
        /* step 2 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 3 */
        CreateOwnDataProperty(cx, obj, "value", value);
        /* step 4 */
        CreateOwnDataProperty(cx, obj, "done", done);
        /* step 5 */
        return obj;
    }

    /**
     * 25.4.3.5 GetIterator ( obj )
     */
    public static ScriptObject GetIterator(ExecutionContext cx, Object obj) {
        /* steps 1-2 */
        Object iterator = Invoke(cx, obj, BuiltinSymbol.iterator.get());
        /* step 3 */
        if (!Type.isObject(iterator)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 4 */
        return Type.objectValue(iterator);
    }

    /**
     * 25.4.3.6 IteratorNext ( iterator, value )
     */
    public static ScriptObject IteratorNext(ExecutionContext cx, ScriptObject iterator) {
        return IteratorNext(cx, iterator, UNDEFINED);
    }

    /**
     * 25.4.3.6 IteratorNext ( iterator, value )
     */
    public static ScriptObject IteratorNext(ExecutionContext cx, ScriptObject iterator, Object value) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        Object result = Invoke(cx, iterator, "next", value);
        /* step 4 */
        if (!Type.isObject(result)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 5 */
        return Type.objectValue(result);
    }

    /**
     * FIXME: Not in spec<br>
     * 25.4.3.? IteratorThrow ( iterator, value )
     */
    public static ScriptObject IteratorThrow(ExecutionContext cx, ScriptObject iterator,
            Object value) {
        Object result = Invoke(cx, iterator, "throw", value);
        if (!Type.isObject(result)) {
            throw throwTypeError(cx, Messages.Key.NotObjectType);
        }
        return Type.objectValue(result);
    }

    /**
     * 25.4.3.7 IteratorComplete (iterResult)
     */
    public static boolean IteratorComplete(ExecutionContext cx, ScriptObject iterResult) {
        /* step 1 (not applicable) */
        /* step 2 */
        Object done = Get(cx, iterResult, "done");
        /* step 3 */
        return ToBoolean(done);
    }

    /**
     * 25.4.3.8 IteratorValue (iterResult)
     */
    public static Object IteratorValue(ExecutionContext cx, ScriptObject iterResult) {
        /* step 1 (not applicable) */
        /* step 2 */
        return Get(cx, iterResult, "value");
    }

    /**
     * 25.4.3.9 IteratorStep ( iterator, value )
     */
    public static ScriptObject IteratorStep(ExecutionContext cx, ScriptObject iterator) {
        return IteratorStep(cx, iterator, UNDEFINED);
    }

    /**
     * 25.4.3.9 IteratorStep ( iterator, value )
     */
    public static ScriptObject IteratorStep(ExecutionContext cx, ScriptObject iterator, Object value) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        ScriptObject result = IteratorNext(cx, iterator, value);
        /* steps 4-5 */
        boolean done = IteratorComplete(cx, result);
        /* step 6 */
        if (done) {
            return null;
        }
        /* step 7 */
        return result;
    }

    /**
     * 25.4.3.10 CreateEmptyIterator ( )
     */
    public static ScriptObject CreateEmptyIterator(ExecutionContext cx) {
        /* step 1 */
        OrdinaryObject obj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        /* step 2 */
        BuiltinFunction emptyNextMethod = new EmptyIteratorNextMethod(cx.getRealm());
        /* step 3 */
        CreateOwnDataProperty(cx, obj, "next", emptyNextMethod);
        /* step 4 */
        return obj;
    }

    private static final class EmptyIteratorNextMethod extends BuiltinFunction {
        public EmptyIteratorNextMethod(Realm realm) {
            super(realm);
            setupDefaultFunctionProperties("next", 0);
        }

        @Override
        public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
            /* steps 1-2 */
            return CreateIterResultObject(calleeContext(), UNDEFINED, true);
        }
    }
}
