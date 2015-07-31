/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async;

import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromisePrototype.PerformPromiseThen;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * Extension: Async Function Definitions
 */
public final class AsyncAbstractOperations {
    private AsyncAbstractOperations() {
    }

    /**
     * 2.2 AsyncFunctionStart(promiseCapability, asyncFunctionBody)
     * 
     * @param cx
     *            the execution context
     * @param promiseCapability
     *            the promise capability
     * @param asyncFunctionBody
     *            the function body
     * @return the result value
     */
    public static Object AsyncFunctionStart(ExecutionContext cx, PromiseCapability<PromiseObject> promiseCapability,
            RuntimeInfo.Function asyncFunctionBody) {
        /* steps 1-7 */
        AsyncObject asyncObject = new AsyncObject(promiseCapability);
        return asyncObject.start(cx, asyncFunctionBody);
    }

    /**
     * 2.3 AsyncFunctionAwait(value)
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the await value
     * @return the result value
     * @throws ReturnValue
     *             to signal an abrupt Return completion
     */
    public static Object AsyncFunctionAwait(ExecutionContext cx, Object value) throws ReturnValue {
        /* step 1 */
        AsyncObject asyncObject = cx.getCurrentAsync();
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
        /* steps 11-13 */
        return asyncObject.await(value);
    }

    /**
     * 2.4 AsyncFunction Awaited Fulfilled
     */
    public static final class AwaitedFulfilled extends BuiltinFunction {
        private final AsyncObject asyncObject;

        public AwaitedFulfilled(Realm realm, AsyncObject asyncObject) {
            this(realm, asyncObject, null);
            createDefaultFunctionProperties();
        }

        private AwaitedFulfilled(Realm realm, AsyncObject asyncObject, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.asyncObject = asyncObject;
        }

        @Override
        public AwaitedFulfilled clone() {
            return new AwaitedFulfilled(getRealm(), asyncObject, null);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = argument(args, 0);
            /* steps 1-7 */
            return asyncObject.resume(calleeContext, value);
        }
    }

    /**
     * 2.5 AsyncFunction Awaited Rejected
     */
    public static final class AwaitedRejected extends BuiltinFunction {
        private final AsyncObject asyncObject;

        public AwaitedRejected(Realm realm, AsyncObject asyncObject) {
            this(realm, asyncObject, null);
            createDefaultFunctionProperties();
        }

        private AwaitedRejected(Realm realm, AsyncObject asyncObject, Void ignore) {
            super(realm, ANONYMOUS, 1);
            this.asyncObject = asyncObject;
        }

        @Override
        public AwaitedRejected clone() {
            return new AwaitedRejected(getRealm(), asyncObject, null);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object reason = argument(args, 0);
            /* steps 1-7 */
            return asyncObject._throw(calleeContext, reason);
        }
    }
}
