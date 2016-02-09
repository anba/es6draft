/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.AbstractOperations.Invoke;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.CleanupSubscription;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.CreateSubscription;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.SubscriptionClosed;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionObserverAbstractOperations.CreateSubscriptionObserver;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseCapability.IfAbruptRejectPromise;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Observable</h1>
 * <ul>
 * <li>Properties of the Observable Prototype Object
 * </ul>
 */
public final class ObservablePrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Observable prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ObservablePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the Observable Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Observable.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Observable;

        /**
         * Observable.prototype.subscribe ( observer )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param observer
         *            the observer object
         * @return the new subscription object
         */
        @Function(name = "subscribe", arity = 1)
        public static Object subscribe(ExecutionContext cx, Object thisValue, Object observer) {
            /* steps 2-3 */
            if (!(thisValue instanceof ObservableObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            ObservableObject observable = (ObservableObject) thisValue;
            /* step 4 */
            if (!Type.isObject(observer)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject observerObj = Type.objectValue(observer);
            /* steps 5-6 */
            // FIXME: spec bug - unnecessary ReturnIfAbrupt
            SubscriptionObject subscription = CreateSubscription(cx, observerObj);
            /* steps 7-8 */
            // FIXME: spec bug - unnecessary ReturnIfAbrupt
            SubscriptionObserverObject subscriptionObserver = CreateSubscriptionObserver(cx, subscription);
            /* step 9 */
            Callable subscriber = observable.getSubscriber();
            /* step 10 (implicit) */
            /* steps 11-13 */
            try {
                /* step 11 */
                Callable subscriberResult = ExecuteSubscriber(cx, subscriber, subscriptionObserver);
                /* step 13 */
                // FIXME: spec bug - typo 'observer'
                subscription.setCleanup(subscriberResult);
            } catch (ScriptException e) {
                /* step 12 */
                Invoke(cx, subscriptionObserver, "error", e.getValue());
            }
            /* step 14 */
            if (SubscriptionClosed(subscription)) {
                CleanupSubscription(cx, subscription);
            }
            /* step 15 */
            return subscription;
        }

        /**
         * Observable.prototype.forEach ( callbackFn [, thisArg] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackFn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the new promise object
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackFn, Object thisArg) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 1 */
            ScriptObject o = Type.objectValue(thisValue);
            /* steps 3-4 */
            PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
            /* step 5 */
            if (!IsCallable(callbackFn)) {
                /* step 5.a */
                ScriptException r = newTypeError(cx, Messages.Key.NotCallable);
                /* step 5.b */
                // FIXME: spec bug - missing ThisValue in Call()
                promiseCapability.getReject().call(cx, UNDEFINED, r.getValue());
                /* step 5.c */
                return promiseCapability.getPromise();
            }
            /* step 6 */
            OrdinaryObject observer = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            /* steps 7-9 */
            ObserverablePrototypeForEachNextFunction next = new ObserverablePrototypeForEachNextFunction(cx.getRealm(),
                    (Callable) callbackFn, thisArg, promiseCapability.getReject());
            /* step 12 */
            CreateDataProperty(cx, observer, "next", next);
            /* step 13 */
            CreateDataProperty(cx, observer, "error", promiseCapability.getReject());
            /* step 14 */
            CreateDataProperty(cx, observer, "complete", promiseCapability.getResolve());
            /* steps 15-16 */
            try {
                /* step 15 */
                Invoke(cx, o, "subscribe", observer);
            } catch (ScriptException e) {
                /* step 16 */
                return IfAbruptRejectPromise(cx, e, promiseCapability);
            }
            /* step 17 */
            return promiseCapability.getPromise();
        }

        /**
         * Observable.prototype [ @@observable ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the this value
         */
        @Function(name = "[Symbol.observable]", symbol = BuiltinSymbol.observable, arity = 0)
        public static Object observable(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }

    /**
     * Runtime Semantics: ExecuteSubscriber ( subscriber, observer )
     * 
     * @param cx
     *            the execution context
     * @param subscriber
     *            the subscriber function
     * @param observer
     *            the observer object
     * @return the subscriber result value
     */
    public static Callable ExecuteSubscriber(ExecutionContext cx, Callable subscriber,
            SubscriptionObserverObject observer) {
        /* steps 1-2 (implicit) */
        /* steps 3-4 */
        Object subscriberResult = subscriber.call(cx, UNDEFINED, observer);
        /* step 5 */
        if (Type.isUndefinedOrNull(subscriberResult)) {
            return null;
        }
        /* step 6 */
        if (IsCallable(subscriberResult)) {
            return (Callable) subscriberResult;
        }
        /* steps 7-8 */
        Callable result = GetMethod(cx, subscriberResult, "unsubscribe");
        /* step 9 */
        if (result == null) {
            throw newTypeError(cx, Messages.Key.PropertyNotCallable, "unsubscribe");
        }
        /* steps 10-11 */
        SubscriptionCleanupFunction cleanupFunction = new SubscriptionCleanupFunction(cx.getRealm(), subscriberResult);
        /* step 12 */
        // FIXME: spec bug - typo 'cancelFunction'
        return cleanupFunction;
    }

    /**
     * Subscription Cleanup Functions
     */
    public static final class SubscriptionCleanupFunction extends BuiltinFunction {
        /** [[Subscription]] */
        private final Object subscription;

        public SubscriptionCleanupFunction(Realm realm, Object subscription) {
            this(realm, subscription, null);
            createDefaultFunctionProperties();
        }

        private SubscriptionCleanupFunction(Realm realm, Object subscription, Void ignore) {
            super(realm, ANONYMOUS, 0);
            this.subscription = subscription;
        }

        @Override
        protected SubscriptionCleanupFunction clone() {
            return new SubscriptionCleanupFunction(getRealm(), subscription, null);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            /* step 1 */
            // FIXME: spec bug - invalid assertion
            /* step 2 */
            Object subscription = this.subscription;
            /* step 3 */
            return Invoke(calleeContext, subscription, "unsubscribe");
        }
    }

    /**
     * Observable.prototype.forEach Next Functions
     */
    public static final class ObserverablePrototypeForEachNextFunction extends BuiltinFunction {
        /** [[Subscription]] */
        private final Callable callbackFn;
        /** [[ThisArg]] */
        private Object thisArg;
        /** [[Subscription]] */
        private final Callable reject;

        public ObserverablePrototypeForEachNextFunction(Realm realm, Callable callbackFn, Object thisArg,
                Callable reject) {
            this(realm, callbackFn, thisArg, reject, null);
            createDefaultFunctionProperties();
        }

        private ObserverablePrototypeForEachNextFunction(Realm realm, Callable callbackFn, Object thisArg,
                Callable reject, Void ignore) {
            // FIXME: spec bug - missing length definition
            super(realm, ANONYMOUS, 1);
            this.callbackFn = callbackFn;
            this.thisArg = thisArg;
            this.reject = reject;
        }

        @Override
        protected ObserverablePrototypeForEachNextFunction clone() {
            return new ObserverablePrototypeForEachNextFunction(getRealm(), callbackFn, thisArg, reject, null);
        }

        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object x = argument(args, 0);
            /* step 1 */
            Callable callbackFn = this.callbackFn;
            /* step 2 */
            Object thisArg = this.thisArg;
            /* step 3 */
            Callable promiseReject = this.reject;
            /* steps 4-5 */
            Object result;
            try {
                /* step 4 */
                result = callbackFn.call(calleeContext, thisArg, x);
            } catch (ScriptException e) {
                /* steps 5.a-b */
                promiseReject.call(calleeContext, UNDEFINED, e.getValue());
                /* step 5.c */
                return UNDEFINED;
            }
            /* step 6 */
            return result;
        }
    }
}
