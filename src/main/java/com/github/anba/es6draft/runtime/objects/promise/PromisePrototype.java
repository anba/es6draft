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
import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.NewPromiseCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.UpdatePromiseFromPotentialThenable;
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
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.Thenable;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
        createProperties(this, cx, Properties.class);
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
         */
        @Function(name = "then", arity = 2)
        public static Object then(ExecutionContext cx, Object thisValue, Object onFulfilled,
                Object onRejected) {
            Realm realm = cx.getRealm();
            /* step 2 */
            if (!IsPromise(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            PromiseObject promise = (PromiseObject) thisValue;
            /* steps 3-4 */
            Object c = Get(cx, promise, "constructor");
            /* steps 5-6 */
            PromiseCapability promiseCapability = NewPromiseCapability(cx, c);
            /* steps 7-8 */
            Callable rejectionHandler;
            if (IsCallable(onRejected)) {
                rejectionHandler = (Callable) onRejected;
            } else {
                rejectionHandler = new ThrowerFunction(cx.getRealm());
            }
            /* steps 9-10 */
            Callable fulfillmentHandler;
            if (IsCallable(onFulfilled)) {
                fulfillmentHandler = (Callable) onFulfilled;
            } else {
                fulfillmentHandler = new IdentityFunction(cx.getRealm());
            }
            /* steps 11-14 */
            PromiseResolutionHandlerFunction resolutionHandler = new PromiseResolutionHandlerFunction(
                    realm, promise, fulfillmentHandler, rejectionHandler);
            /* step 15 */
            PromiseReaction resolveReaction = new PromiseReaction(promiseCapability,
                    resolutionHandler);
            /* step 16 */
            PromiseReaction rejectReaction = new PromiseReaction(promiseCapability,
                    rejectionHandler);
            /* step 17 */
            if (promise.getStatus() == PromiseObject.Status.Unresolved) {
                promise.addResolveReaction(resolveReaction);
                promise.addRejectReaction(rejectReaction);
            }
            /* step 18 */
            else if (promise.getStatus() == PromiseObject.Status.HasResolution) {
                Object resolution = promise.getResult();
                realm.enqueuePromiseTask(new PromiseReactionTask(realm, resolveReaction, resolution));
            }
            /* step 19 */
            else if (promise.getStatus() == PromiseObject.Status.HasRejection) {
                Object reason = promise.getResult();
                realm.enqueuePromiseTask(new PromiseReactionTask(realm, rejectReaction, reason));
            }
            /* step 20 */
            return promiseCapability.getPromise();
        }

        /**
         * 25.4.5.1 Promise.prototype [ @@toStringTag ]
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
            super(realm, ANONYMOUS, 1);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            Object x = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            return x;
        }
    }

    /**
     * 25.4.5.3.2 Promise Resolution Handler Functions
     */
    public static final class PromiseResolutionHandlerFunction extends BuiltinFunction {
        /** [[Promise]] */
        private final PromiseObject promise;

        /** [[FulfillmentHandler]] */
        private final Callable fulfillmentHandler;

        /** [[RejectionHandler]] */
        private final Callable rejectionHandler;

        public PromiseResolutionHandlerFunction(Realm realm, PromiseObject promise,
                Callable fulfillmentHandler, Callable rejectionHandler) {
            super(realm, ANONYMOUS, 1);
            this.promise = promise;
            this.fulfillmentHandler = fulfillmentHandler;
            this.rejectionHandler = rejectionHandler;
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object x = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            PromiseObject promise = this.promise;
            /* step 2 */
            Callable fulfillmentHandler = this.fulfillmentHandler;
            /* step 3 */
            Callable rejectionHandler = this.rejectionHandler;
            /* step 4 */
            if (SameValue(x, promise)) {
                ScriptException selfResolutionError = newTypeError(calleeContext,
                        Messages.Key.PromiseSelfResolution);
                return rejectionHandler.call(calleeContext, UNDEFINED,
                        selfResolutionError.getValue());
            }
            /* step 5 */
            Constructor c = promise.getConstructor();
            /* steps 6-7 */
            PromiseCapability promiseCapability = NewPromiseCapability(calleeContext, c);
            /* steps 8-9 */
            Thenable updateResult = UpdatePromiseFromPotentialThenable(calleeContext, x,
                    promiseCapability);
            /* step 10 */
            if (updateResult != Thenable.NotThenable) {
                return Invoke(calleeContext, promiseCapability.getPromise(), "then",
                        fulfillmentHandler, rejectionHandler);
            }
            /* step 11 */
            return fulfillmentHandler.call(calleeContext, UNDEFINED, x);
        }
    }

    /**
     * 25.4.5.3.3 Thrower Functions
     */
    public static final class ThrowerFunction extends BuiltinFunction {
        public ThrowerFunction(Realm realm) {
            super(realm, ANONYMOUS, 1);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            Object e = args.length > 0 ? args[0] : UNDEFINED;
            /* step 1 */
            throw ScriptException.create(e);
        }
    }
}
