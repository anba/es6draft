/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorComplete;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorValue;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseCapability.IfAbruptRejectPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromisePrototype.PerformPromiseThen;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorAbstractOperations.AsyncFromSyncIteratorValueUnwrapFunction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Async-from-Sync Iterator Objects
 */
public final class AsyncFromSyncIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Async-from-Sync Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public AsyncFromSyncIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of Async-from-Sync Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.AsyncIteratorPrototype;

        /**
         * %AsyncFromSyncIteratorPrototype%.next ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the next value
         * @return the promise object
         */
        @Function(name = "next", arity = 1)
        public static Object next(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 (omitted) */
            /* step 3 */
            // FIXME: spec issue - Unreachable
            // https://github.com/tc39/proposal-async-iteration/issues/105
            assert thisValue instanceof AsyncFromSyncIteratorObject;
            /* step 2, 4-16 */
            return ((AsyncFromSyncIteratorObject) thisValue).next(cx, value);
        }

        /**
         * %AsyncFromSyncIteratorPrototype%.return ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the return value
         * @return the promise object
         */
        @Function(name = "return", arity = 1)
        public static Object _return(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 (omitted) */
            /* step 2 */
            PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
            /* step 3 */
            // FIXME: spec issue - Unreachable
            // https://github.com/tc39/proposal-async-iteration/issues/105
            assert thisValue instanceof AsyncFromSyncIteratorObject;
            /* step 4 */
            ScriptObject syncIterator = ((AsyncFromSyncIteratorObject) thisValue).getSyncIterator();
            /* steps 5-6 */
            Callable _return;
            try {
                /* step 5 */
                _return = GetMethod(cx, syncIterator, "return");
            } catch (ScriptException e) {
                /* step 6  */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 7 */
            if (_return == null) {
                /* step 7.a */
                OrdinaryObject iterResult = CreateIterResultObject(cx, value, true);
                /* step 7.b */
                promiseCapability.getResolve().call(cx, UNDEFINED, iterResult);
                /* step 7.c */
                return promiseCapability.getPromise();
            }
            /* steps 8-13 */
            boolean returnDone;
            Object returnValue;
            try {
                /* step 8 */
                Object returnResult = _return.call(cx, syncIterator, value);
                /* step 10 */
                if (!Type.isObject(returnResult)) {
                    /* step 10.a */
                    promiseCapability.getReject().call(cx, UNDEFINED,
                            newTypeError(cx, Messages.Key.NotObjectTypeReturned, "return"));
                    /* step 10.b */
                    return promiseCapability.getPromise();
                }
                /* step 11 */
                returnDone = IteratorComplete(cx, Type.objectValue(returnResult));
                /* step 13 */
                returnValue = IteratorValue(cx, Type.objectValue(returnResult));
            } catch (ScriptException e) {
                /* steps 9, 12, 14  */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 15 */
            PromiseCapability<PromiseObject> valueWrapperCapability = PromiseBuiltinCapability(cx);
            /* step 16 */
            valueWrapperCapability.getResolve().call(cx, UNDEFINED, returnValue);
            /* steps 17-18 */
            AsyncFromSyncIteratorValueUnwrapFunction onFulfilled = new AsyncFromSyncIteratorValueUnwrapFunction(
                    cx.getRealm(), returnDone);
            /* step 19 */
            PerformPromiseThen(cx, valueWrapperCapability.getPromise(), onFulfilled, UNDEFINED, promiseCapability);
            /* step 20 */
            return promiseCapability.getPromise();
        }

        /**
         * %AsyncFromSyncIteratorPrototype%.throw ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the throw value
         * @return the promise object
         */
        @Function(name = "throw", arity = 1)
        public static Object _throw(ExecutionContext cx, Object thisValue, Object value) {
            /* step 1 (omitted) */
            /* step 2 */
            PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
            /* step 3 */
            // FIXME: spec issue - Unreachable
            // https://github.com/tc39/proposal-async-iteration/issues/105
            assert thisValue instanceof AsyncFromSyncIteratorObject;
            /* step 4 */
            ScriptObject syncIterator = ((AsyncFromSyncIteratorObject) thisValue).getSyncIterator();
            /* steps 5-6 */
            Callable _throw;
            try {
                /* step 5 */
                _throw = GetMethod(cx, syncIterator, "throw");
            } catch (ScriptException e) {
                /* step 6  */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 7 */
            if (_throw == null) {
                /* step 7.a */
                promiseCapability.getReject().call(cx, UNDEFINED, value);
                /* step 7.b */
                return promiseCapability.getPromise();
            }
            /* steps 8-13 */
            boolean throwDone;
            Object throwValue;
            try {
                /* step 8 */
                Object throwResult = _throw.call(cx, syncIterator, value);
                /* step 10 */
                if (!Type.isObject(throwResult)) {
                    /* step 10.a */
                    promiseCapability.getReject().call(cx, UNDEFINED,
                            newTypeError(cx, Messages.Key.NotObjectTypeReturned, "throw"));
                    /* step 10.b */
                    return promiseCapability.getPromise();
                }
                /* step 10 */
                throwDone = IteratorComplete(cx, Type.objectValue(throwResult));
                /* step 12 */
                throwValue = IteratorValue(cx, Type.objectValue(throwResult));
            } catch (ScriptException e) {
                /* steps 9, 12, 14  */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 15 */
            PromiseCapability<PromiseObject> valueWrapperCapability = PromiseBuiltinCapability(cx);
            /* step 16 */
            valueWrapperCapability.getResolve().call(cx, UNDEFINED, throwValue);
            /* steps 17-18 */
            AsyncFromSyncIteratorValueUnwrapFunction onFulfilled = new AsyncFromSyncIteratorValueUnwrapFunction(
                    cx.getRealm(), throwDone);
            /* step 19 */
            PerformPromiseThen(cx, valueWrapperCapability.getPromise(), onFulfilled, UNDEFINED, promiseCapability);
            /* step 20 */
            return promiseCapability.getPromise();
        }

        /**
         * %AsyncFromSyncIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Async-from-Sync Iterator";
    }
}
