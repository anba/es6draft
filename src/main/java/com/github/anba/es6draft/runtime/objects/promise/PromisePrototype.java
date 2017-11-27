/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.Invoke;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.SpeciesConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.NewPromiseCapability;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseReactionJob;
import com.github.anba.es6draft.runtime.objects.zone.ZoneObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2>
 * <ul>
 * <li>25.4.5 Properties of the Promise Prototype Object
 * </ul>
 */
public final class PromisePrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Promise prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public PromisePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, FinallyProperty.class);
    }

    /**
     * 25.4.5 Properties of the Promise Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 25.4.5.2 Promise.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Promise;

        /**
         * 25.4.5.1 Promise.prototype.catch ( onRejected )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param onRejected
         *            the onRejected handler
         * @return the new promise object
         */
        @Function(name = "catch", arity = 1)
        public static Object _catch(ExecutionContext cx, Object thisValue, Object onRejected) {
            /* step 1 */
            Object promise = thisValue;
            /* step 2 */
            return Invoke(cx, promise, "then", UNDEFINED, onRejected);
        }

        /**
         * 25.4.5.3 Promise.prototype.then ( onFulfilled , onRejected )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param onFulfilled
         *            the onFulfilled handler
         * @param onRejected
         *            the onRejected handler
         * @return the new promise object
         */
        @Function(name = "then", arity = 2)
        public static Object then(ExecutionContext cx, Object thisValue, Object onFulfilled, Object onRejected) {
            /* step 2 */
            if (!IsPromise(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "Promise.prototype.then",
                        Type.of(thisValue).toString());
            }
            /* step 1 */
            PromiseObject promise = (PromiseObject) thisValue;
            /* step 3 */
            Constructor c = SpeciesConstructor(cx, promise, Intrinsics.Promise);
            /* step 4 */
            PromiseCapability<ScriptObject> resultCapability = NewPromiseCapability(cx, c);
            /* step 5 */
            return PerformPromiseThen(cx, promise, onFulfilled, onRejected, resultCapability);
        }

        /**
         * 25.4.5.4 Promise.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Promise";
    }

    /**
     * 25.4.5.3.1 PerformPromiseThen ( promise, onFulfilled, onRejected, resultCapability )
     * 
     * @param <PROMISE>
     *            the promise type
     * @param cx
     *            the execution context
     * @param promise
     *            the promise object
     * @param onFulfilled
     *            the onFulfilled handler
     * @param onRejected
     *            the onRejected handler
     * @param resultCapability
     *            the new promise capability record
     * @return the new promise object
     */
    public static <PROMISE extends ScriptObject> PROMISE PerformPromiseThen(ExecutionContext cx, PromiseObject promise,
            Object onFulfilled, Object onRejected, PromiseCapability<PROMISE> resultCapability) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (!IsCallable(onFulfilled)) {
            onFulfilled = null;
        }
        /* step 4 */
        if (!IsCallable(onRejected)) {
            onRejected = null;
        }
        ZoneObject currentZone = cx.getRealm().getCurrentZone();
        /* step 5 */
        PromiseReaction fulfillReaction = new PromiseReaction(resultCapability, PromiseReaction.Type.Fulfill,
                (Callable) onFulfilled, currentZone);
        /* step 6 */
        PromiseReaction rejectReaction = new PromiseReaction(resultCapability, PromiseReaction.Type.Reject,
                (Callable) onRejected, currentZone);
        /* step 7 */
        if (promise.getState() == PromiseObject.State.Pending) {
            promise.addFulfillReaction(fulfillReaction);
            promise.addRejectReaction(rejectReaction);
            promise.notifyRejectReaction(rejectReaction);
        }
        /* step 8 */
        else if (promise.getState() == PromiseObject.State.Fulfilled) {
            Object value = promise.getResult();
            Realm realm = cx.getRealm();
            realm.enqueuePromiseJob(new PromiseReactionJob(realm, fulfillReaction, value));
        }
        /* step 9 */
        else {
            assert promise.getState() == PromiseObject.State.Rejected;
            Object reason = promise.getResult();
            Realm realm = cx.getRealm();
            realm.enqueuePromiseJob(new PromiseReactionJob(realm, rejectReaction, reason));
            promise.notifyRejectReaction(rejectReaction);
        }
        /* step 10 (not applicable) */
        /* step 11 */
        return resultCapability.getPromise();
    }

    /**
     * Properties of the Promise Prototype Object
     */
    @CompatibilityExtension(CompatibilityOption.PromiseFinally)
    public enum FinallyProperty {
        ;

        /**
         * Promise.prototype.finally ( onFinally )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param onFinally
         *            the onFinally handler
         * @return the new promise object
         */
        @Function(name = "finally", arity = 1)
        public static Object _finally(ExecutionContext cx, Object thisValue, Object onFinally) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "Promise.prototype.finally",
                        Type.of(thisValue).toString());
            }
            /* step 1 */
            ScriptObject promise = Type.objectValue(thisValue);
            /* steps 3-4 */
            Constructor constructor = SpeciesConstructor(cx, promise, Intrinsics.Promise);
            /* steps 5-6 */
            Object thenFinally;
            Object catchFinally;
            if (!IsCallable(onFinally)) {
                thenFinally = onFinally;
                catchFinally = onFinally;
            } else {
                thenFinally = new ThenFinallyFunction(cx.getRealm(), constructor, (Callable) onFinally);
                catchFinally = new CatchFinallyFunction(cx.getRealm(), constructor, (Callable) onFinally);
            }
            /* step 7 */
            return Invoke(cx, promise, "then", thenFinally, catchFinally);
        }
    }

    public static final class ThenFinallyFunction extends BuiltinFunction {
        private final Constructor constructor;
        private final Callable onFinally;

        public ThenFinallyFunction(Realm realm, Constructor constructor, Callable onFinally) {
            super(realm, ANONYMOUS, 1);
            this.constructor = constructor;
            this.onFinally = onFinally;
            createDefaultFunctionProperties();
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = argument(args, 0);
            /* steps 1-3 */
            Object result = onFinally.call(calleeContext, UNDEFINED);
            /* steps 4-6 */
            ScriptObject promise = PromiseResolve(calleeContext, constructor, result);
            /* step 7 */
            ValueThunkFunction valueThunk = new ValueThunkFunction(calleeContext.getRealm(), value);
            /* step 8 */
            return Invoke(calleeContext, promise, "then", valueThunk);
        }
    }

    public static final class ValueThunkFunction extends BuiltinFunction {
        private final Object value;

        public ValueThunkFunction(Realm realm, Object value) {
            super(realm, ANONYMOUS, 0);
            this.value = value;
            createDefaultFunctionProperties();
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return value;
        }
    }

    public static final class CatchFinallyFunction extends BuiltinFunction {
        private final Constructor constructor;
        private final Callable onFinally;

        public CatchFinallyFunction(Realm realm, Constructor constructor, Callable onFinally) {
            super(realm, ANONYMOUS, 1);
            this.constructor = constructor;
            this.onFinally = onFinally;
            createDefaultFunctionProperties();
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object reason = argument(args, 0);
            /* steps 1-3 */
            Object result = onFinally.call(calleeContext, UNDEFINED);
            /* steps 4-6 */
            ScriptObject promise = PromiseResolve(calleeContext, constructor, result);
            /* step 7 */
            ThrowerFunction thrower = new ThrowerFunction(calleeContext.getRealm(), reason);
            /* step 8 */
            return Invoke(calleeContext, promise, "then", thrower);
        }
    }

    public static final class ThrowerFunction extends BuiltinFunction {
        private final Object reason;

        public ThrowerFunction(Realm realm, Object reason) {
            super(realm, ANONYMOUS, 0);
            this.reason = reason;
            createDefaultFunctionProperties();
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            throw ScriptException.create(reason);
        }
    }

    private static ScriptObject PromiseResolve(ExecutionContext cx, ScriptObject constructor, Object x) {
        /* step 1 (implicit) */
        /* step 2 */
        if (IsPromise(x)) {
            Object xConstructor = Get(cx, (PromiseObject) x, "constructor");
            if (xConstructor == constructor) { // SameValue
                return (PromiseObject) x;
            }
        }
        /* step 3 */
        PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, constructor);
        /* step 4 */
        promiseCapability.getResolve().call(cx, UNDEFINED, x);
        /* step 5 */
        return promiseCapability.getPromise();
    }
}
