/**
 * Copyright (c) André Bargull
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
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
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
         * @param errorCallback
         *            the optional error callback
         * @param completeCallback
         *            the optional complete callback
         * @return the new subscription object
         */
        @Function(name = "subscribe", arity = 1)
        public static Object subscribe(ExecutionContext cx, Object thisValue, Object observer, Object errorCallback,
                Object completeCallback) {
            /* steps 2-3 */
            if (!(thisValue instanceof ObservableObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, "Observable.prototype.subscribe",
                        Type.of(thisValue).toString());
            }
            /* step 1 */
            ObservableObject observable = (ObservableObject) thisValue;
            /* steps 4-5 */
            ScriptObject observerObj;
            if (IsCallable(observer)) {
                /* step 4.a-g (omitted) */
                Object nextCallback = observer;
                // FIXME: spec issue - overly complicated, just make both callback optional
                /* step 4.h */
                observerObj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
                /* step 4.i */
                CreateDataProperty(cx, observerObj, "next", nextCallback);
                /* step 4.j */
                CreateDataProperty(cx, observerObj, "error", errorCallback);
                /* step 4.k */
                CreateDataProperty(cx, observerObj, "complete", completeCallback);
            } else if (!Type.isObject(observable)) {
                /* step 5 */
                observerObj = ObjectCreate(cx, Intrinsics.ObjectPrototype);
            } else {
                observerObj = Type.objectValue(observable);
            }
            /* step 6 */
            // FIXME: spec bug - unnecessary ReturnIfAbrupt
            SubscriptionObject subscription = CreateSubscription(cx, observerObj);
            Callable start;
            try {
                /* steps 7-8, 8.a (implicit) */
                start = GetMethod(cx, observerObj, "start");
                /* step 8.b */
                if (start != null) {
                    /* step 8.b.i */
                    start.call(cx, observerObj, subscription);
                    /* step 8.b.iii */
                    if (SubscriptionClosed(subscription)) {
                        return subscription;
                    }
                }
            } catch (ScriptException e) {
                /* steps 9, 8.b.ii */
                cx.getRuntimeContext().getErrorReporter().accept(cx, e);
            }
            /* step 10 */
            // FIXME: spec bug - result not necessarly defined at this point; HostReportErrors already called
            /* step 11 */
            // FIXME: spec bug - unnecessary ReturnIfAbrupt
            SubscriptionObserverObject subscriptionObserver = CreateSubscriptionObserver(cx, subscription);
            /* step 12 */
            Callable subscriber = observable.getSubscriber();
            /* step 13 (implicit) */
            /* steps 14-16 */
            try {
                /* step 14 */
                Callable subscriberResult = ExecuteSubscriber(cx, subscriber, subscriptionObserver);
                /* step 16 */
                // FIXME: spec bug - typo 'observer' -> 'subscription'
                subscription.setCleanup(subscriberResult);
            } catch (ScriptException e) {
                /* step 15 */
                Invoke(cx, subscriptionObserver, "error", e.getValue());
            }
            /* step 17 */
            if (SubscriptionClosed(subscription)) {
                /* step 17.a */
                CleanupSubscription(cx, subscription);
            }
            /* step 18 */
            return subscription;
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
        /* step 3 */
        Object subscriberResult = subscriber.call(cx, UNDEFINED, observer);
        /* step 4 */
        if (Type.isUndefinedOrNull(subscriberResult)) {
            return null;
        }
        /* step 5 */
        if (IsCallable(subscriberResult)) {
            return (Callable) subscriberResult;
        }
        /* step 6 */
        Callable result = GetMethod(cx, subscriberResult, "unsubscribe");
        /* step 7 */
        if (result == null) {
            throw newTypeError(cx, Messages.Key.PropertyNotCallable, "unsubscribe");
        }
        /* steps 8-9 */
        SubscriptionCleanupFunction cleanupFunction = new SubscriptionCleanupFunction(cx.getRealm(), subscriberResult);
        /* step 10 */
        return cleanupFunction;
    }

    /**
     * Subscription Cleanup Functions
     */
    public static final class SubscriptionCleanupFunction extends BuiltinFunction {
        /** [[Subscription]] */
        private final Object subscription;

        public SubscriptionCleanupFunction(Realm realm, Object subscription) {
            super(realm, ANONYMOUS, 0);
            this.subscription = subscription;
            createDefaultFunctionProperties();
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
}
