/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.Invoke;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.NewPromiseCapability;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseReactionTask;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
public final class PromisePrototype extends OrdinaryObject implements Initialisable {
    public PromisePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
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
        public static Object newThen(ExecutionContext cx, Object thisValue, Object onFulfilled,
                Object onRejected) {
            Realm realm = cx.getRealm();
            /* step 2 */
            if (!IsPromise(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            PromiseObject promise = (PromiseObject) thisValue;
            /* step 3 */
            if (Type.isUndefinedOrNull(onFulfilled)) {
                onFulfilled = new IdentityFunction(cx.getRealm());
            }
            /* step 4 */
            if (Type.isUndefinedOrNull(onRejected)) {
                onRejected = new ThrowerFunction(cx.getRealm());
            }
            /* step 5 */
            if (!IsCallable(onFulfilled) || !IsCallable(onRejected)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            /* steps 6-7 */
            Object c = Get(cx, promise, "constructor");
            /* steps 8-9 */
            PromiseCapability<?> promiseCapability = NewPromiseCapability(cx, c);
            /* step 10 */
            PromiseReaction fulfillReaction = new PromiseReaction(promiseCapability,
                    (Callable) onFulfilled);
            /* step 11 */
            PromiseReaction rejectReaction = new PromiseReaction(promiseCapability,
                    (Callable) onRejected);
            /* step 12 */
            if (promise.getState() == PromiseObject.State.Pending) {
                promise.addFulfillReaction(fulfillReaction);
                promise.addRejectReaction(rejectReaction);
            }
            /* step 13 */
            else if (promise.getState() == PromiseObject.State.Fulfilled) {
                Object value = promise.getResult();
                realm.enqueuePromiseTask(new PromiseReactionTask(realm, fulfillReaction, value));
            }
            /* step 14 */
            else if (promise.getState() == PromiseObject.State.Rejected) {
                Object reason = promise.getResult();
                realm.enqueuePromiseTask(new PromiseReactionTask(realm, rejectReaction, reason));
            }
            /* step 15 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.5.4 Promise.prototype [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Promise";
    }

    /**
     * 25.4.5.3.1 Identity Functions
     */
    public static final class IdentityFunction extends BuiltinFunction {
        public IdentityFunction(Realm realm) {
            super(realm, ANONYMOUS);
            createDefaultFunctionProperties(ANONYMOUS, 1);
        }

        private IdentityFunction(Realm realm, Void ignore) {
            super(realm, ANONYMOUS);
        }

        @Override
        public IdentityFunction clone(ExecutionContext cx) {
            IdentityFunction f = new IdentityFunction(getRealm(), null);
            f.setPrototype(getPrototype());
            f.addRestrictedFunctionProperties(cx);
            return f;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            Object x = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            return x;
        }
    }

    /**
     * 25.4.5.3.3 Thrower Functions
     */
    public static final class ThrowerFunction extends BuiltinFunction {
        public ThrowerFunction(Realm realm) {
            super(realm, ANONYMOUS);
            createDefaultFunctionProperties(ANONYMOUS, 1);
        }

        private ThrowerFunction(Realm realm, Void ignore) {
            super(realm, ANONYMOUS);
        }

        @Override
        public ThrowerFunction clone(ExecutionContext cx) {
            ThrowerFunction f = new ThrowerFunction(getRealm(), null);
            f.setPrototype(getPrototype());
            f.addRestrictedFunctionProperties(cx);
            return f;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            Object e = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            throw ScriptException.create(e);
        }
    }
}
