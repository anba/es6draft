/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromisePrototype.PerformPromiseThen;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.Undefined;
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
    public static ScriptObject GetAsyncIterator(ExecutionContext cx, Object obj) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Callable method = GetMethod(cx, obj, BuiltinSymbol.asyncIterator.get());
        /* step 4 (not applicable) */
        /* step 5 (inlined Call operation) */
        if (method == null) {
            throw newTypeError(cx, Messages.Key.PropertyNotCallable, BuiltinSymbol.asyncIterator.toString());
        }
        Object iterator = method.call(cx, obj);
        /* step 6 */
        if (!Type.isObject(iterator)) {
            throw newTypeError(cx, Messages.Key.NotObjectTypeReturned, "[Symbol.asyncIterator]");
        }
        /* step 7 */
        return Type.objectValue(iterator);
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

    /**
     * AsyncFunctionAwait(value)
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the await value
     */
    public static void AsyncFunctionAwait(ExecutionContext cx, Object value) {
        /* step 1 */
        AsyncGeneratorObject asyncObject = cx.getCurrentAsyncGenerator();
        assert asyncObject != null;
        /* steps 2-3 */
        PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
        /* steps 4-5 */
        promiseCapability.getResolve().call(cx, UNDEFINED, value);
        /* steps 6, 8 */
        AwaitedFulfilled onFulfilled = new AwaitedFulfilled(cx.getRealm(), asyncObject);
        /* steps 7, 8 */
        AwaitedRejected onRejected = new AwaitedRejected(cx.getRealm(), asyncObject);
        /* step 9 */
        PromiseCapability<PromiseObject> throwawayCapability = PromiseBuiltinCapability(cx);
        /* step 10 */
        PerformPromiseThen(cx, promiseCapability.getPromise(), onFulfilled, onRejected, throwawayCapability);
        /* steps 11-13 (implemented in generated code) */
    }

    /**
     * AsyncFunction Awaited Fulfilled
     */
    public static final class AwaitedFulfilled extends BuiltinFunction {
        private final AsyncGeneratorObject asyncObject;

        public AwaitedFulfilled(Realm realm, AsyncGeneratorObject asyncObject) {
            this(realm, asyncObject, null);
            createDefaultFunctionProperties();
        }

        private AwaitedFulfilled(Realm realm, AsyncGeneratorObject asyncObject, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.asyncObject = asyncObject;
        }

        @Override
        public AwaitedFulfilled clone() {
            return new AwaitedFulfilled(getRealm(), asyncObject, null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = argument(args, 0);
            /* steps 1-7 */
            asyncObject.resume(calleeContext, value);
            return UNDEFINED;
        }
    }

    /**
     * AsyncFunction Awaited Rejected
     */
    public static final class AwaitedRejected extends BuiltinFunction {
        private final AsyncGeneratorObject asyncObject;

        public AwaitedRejected(Realm realm, AsyncGeneratorObject asyncObject) {
            this(realm, asyncObject, null);
            createDefaultFunctionProperties();
        }

        private AwaitedRejected(Realm realm, AsyncGeneratorObject asyncObject, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.asyncObject = asyncObject;
        }

        @Override
        public AwaitedRejected clone() {
            return new AwaitedRejected(getRealm(), asyncObject, null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object reason = argument(args, 0);
            /* steps 1-7 */
            asyncObject._throw(calleeContext, reason);
            return UNDEFINED;
        }
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
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator, Object completion) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        if (!(generator instanceof AsyncGeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-10 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, AsyncGeneratorRequest.CompletionType.Normal, completion);
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
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator, ReturnValue completion) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        if (!(generator instanceof AsyncGeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-10 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, AsyncGeneratorRequest.CompletionType.Return, completion);
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
     * @return the promise object
     */
    public static PromiseObject AsyncGeneratorEnqueue(ExecutionContext cx, Object generator,
            ScriptException completion) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!Type.isObject(generator)) {
            throw newTypeError(cx, Messages.Key.NotObjectType);
        }
        /* step 3 */
        if (!(generator instanceof AsyncGeneratorObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-10 */
        return ((AsyncGeneratorObject) generator).enqueue(cx, AsyncGeneratorRequest.CompletionType.Throw, completion);
    }
}
