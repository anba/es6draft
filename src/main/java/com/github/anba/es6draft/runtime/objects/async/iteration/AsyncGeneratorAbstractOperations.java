/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.async.iteration.AsyncFromSyncIteratorObject.CreateAsyncFromSyncIterator;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.ScriptIterators;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * AsyncGenerator Abstract Operations
 */
public final class AsyncGeneratorAbstractOperations {
    private AsyncGeneratorAbstractOperations() {
    }

    /**
     * GetIterator ( obj [ , hint ] )
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the script object
     * @return the script iterator object
     */
    public static ScriptIterator<?> GetAsyncIterator(ExecutionContext cx, Object obj) {
        /* step 1 (not applicable) */
        /* steps 2, 3.a */
        Callable method = GetMethod(cx, obj, BuiltinSymbol.asyncIterator.get());
        /* step 3.b */
        if (method == null) {
            /* step 3.b.i */
            Callable syncMethod = GetMethod(cx, obj, BuiltinSymbol.iterator.get());
            /* step 3.b.ii (inlined Call operation) */
            if (syncMethod == null) {
                throw newTypeError(cx, Messages.Key.PropertyNotCallable, BuiltinSymbol.iterator.toString());
            }
            Object syncIterator = syncMethod.call(cx, obj);
            /* step 3.b.iii */
            AsyncFromSyncIteratorObject iteratorObj = CreateAsyncFromSyncIterator(cx, syncIterator);
            return ScriptIterators.GetAsyncScriptIterator(cx, iteratorObj);
        }
        /* step 4 (not applicable) */
        /* step 5 */
        Object iterator = method.call(cx, obj);
        /* step 6 */
        if (!Type.isObject(iterator)) {
            throw newTypeError(cx, Messages.Key.NotObjectTypeReturned, BuiltinSymbol.asyncIterator.toString());
        }
        ScriptObject iteratorObj = Type.objectValue(iterator);
        /* step 7 */
        Object nextMethod = Get(cx, iteratorObj, "next");
        /* steps 8-9 */
        return ScriptIterators.ToScriptIterator(cx, iteratorObj, nextMethod);
    }

    /**
     * AsyncGeneratorStart ( generator, generatorBody )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param generatorBody
     *            the runtime function code
     */
    public static void AsyncGeneratorStart(ExecutionContext cx, AsyncGeneratorObject generator,
            RuntimeInfo.Function generatorBody) {
        generator.start(cx, generatorBody);
    }

    private static PromiseObject RejectAsyncGeneratorEnqueue(ExecutionContext cx, Object generator, String method) {
        /* step 2 */
        PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
        /* step 3.a */
        ScriptException error = newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(generator).toString());
        /* step 3.b */
        promiseCapability.getReject().call(cx, UNDEFINED, error.getValue());
        /* step 3.c */
        return promiseCapability.getPromise();
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param completion
     *            the completion record
     * @param method
     *            the method name
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator, Object completion,
            String method) {
        /* step 1 (not applicable) */
        /* step 3 (reordered) */
        if (!(generator instanceof AsyncGeneratorObject)) {
            return RejectAsyncGeneratorEnqueue(cx, generator, method);
        }
        /* steps 2, 4-9 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, completion);
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param completion
     *            the completion record
     * @param method
     *            the method name
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator, ReturnValue completion,
            String method) {
        /* step 1 (not applicable) */
        /* step 3 (reordered) */
        if (!(generator instanceof AsyncGeneratorObject)) {
            return RejectAsyncGeneratorEnqueue(cx, generator, method);
        }
        /* steps 2, 4-9 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, completion);
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the async generator object
     * @param completion
     *            the completion record
     * @param method
     *            the method name
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator, ScriptException completion,
            String method) {
        /* step 1 (not applicable) */
        /* step 3 (reordered) */
        if (!(generator instanceof AsyncGeneratorObject)) {
            return RejectAsyncGeneratorEnqueue(cx, generator, method);
        }
        /* steps 2, 4-9 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, completion);
    }

    /**
     * Async-from-Sync Iterator Value Unwrap Functions
     */
    public static final class AsyncFromSyncIteratorValueUnwrapFunction extends BuiltinFunction {
        /** [[Done]] */
        private final boolean done;

        public AsyncFromSyncIteratorValueUnwrapFunction(Realm realm, boolean done) {
            super(realm, ANONYMOUS, 1);
            this.done = done;
            createDefaultFunctionProperties();
        }

        @Override
        public ScriptObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = argument(args, 0);
            /* step 1 */
            return CreateIterResultObject(calleeContext, value, this.done);
        }
    }
}
