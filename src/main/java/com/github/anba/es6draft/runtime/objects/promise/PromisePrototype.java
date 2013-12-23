/**
 * Copyright (c) 2012-2013 André Bargull
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
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.GetDeferred;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IsPromise;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.QueueMicrotask;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.ExecutePromiseReaction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.IdentityFunction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseResolutionHandlerFunction;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.ThrowerFunction;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Promise Objects</h1><br>
 * <ul>
 * <li>The Promise Constructor
 * <li>Properties of the Promise Prototype Object
 * </ul>
 */
public class PromisePrototype extends OrdinaryObject implements Initialisable {
    public PromisePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * Properties of the Promise Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Promise.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Promise;

        /**
         * Promise.prototype.catch ( onRejected )
         */
        @Function(name = "catch", arity = 1)
        public static Object _catch(ExecutionContext cx, Object thisValue, Object onRejected) {
            /* step 1 */
            Object promise = thisValue;
            /* step 2 */
            return Invoke(cx, promise, "then", UNDEFINED, onRejected);
        }

        /**
         * Promise.prototype.then ( onFulfilled , onRejected )
         */
        @Function(name = "then", arity = 2)
        public static Object then(ExecutionContext cx, Object thisValue, Object onFulfilled,
                Object onRejected) {
            /* step 2 */
            if (!IsPromise(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            PromiseObject promise = (PromiseObject) thisValue;
            /* steps 3-4 */
            Object c = Get(cx, promise, "constructor");
            /* steps 5-6 */
            Deferred deferred = GetDeferred(cx, c);
            /* step 7 */
            Callable rejectionHandler = new ThrowerFunction(cx.getRealm());
            /* step 8 */
            if (IsCallable(onRejected)) {
                rejectionHandler = (Callable) onRejected;
            }
            /* step 9 */
            Callable fulfillmentHandler = new IdentityFunction(cx.getRealm());
            /* step 10 */
            if (IsCallable(onFulfilled)) {
                fulfillmentHandler = (Callable) onFulfilled;
            }
            /* steps 11-14 */
            PromiseResolutionHandlerFunction resolutionHandler = new PromiseResolutionHandlerFunction(
                    cx.getRealm(), promise, fulfillmentHandler, rejectionHandler);
            /* step 15 */
            PromiseReaction resolveReaction = new PromiseReaction(deferred, resolutionHandler);
            /* step 16 */
            PromiseReaction rejectReaction = new PromiseReaction(deferred, rejectionHandler);
            /* step 17 */
            if (promise.getStatus() == PromiseObject.Status.Unresolved) {
                promise.addResolveReaction(resolveReaction);
                promise.addRejectReaction(rejectReaction);
            }
            /* step 18 */
            if (promise.getStatus() == PromiseObject.Status.HasResolution) {
                Object resolution = promise.getResult();
                QueueMicrotask(cx, new ExecutePromiseReaction(resolveReaction, resolution));
            }
            /* step 19 */
            if (promise.getStatus() == PromiseObject.Status.HasRejection) {
                Object reason = promise.getResult();
                QueueMicrotask(cx, new ExecutePromiseReaction(rejectReaction, reason));
            }
            /* step 20 */
            return deferred.getPromise();
        }
    }
}
