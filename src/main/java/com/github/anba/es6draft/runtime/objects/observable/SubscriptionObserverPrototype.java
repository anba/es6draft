/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.CleanupSubscription;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.CloseSubscription;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.SubscriptionClosed;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Observable</h1>
 * <ul>
 * <li>The %SubscriptionObserverPrototype% Object
 * </ul>
 */
public final class SubscriptionObserverPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new %SubscriptionObserverPrototype% object.
     * 
     * @param realm
     *            the realm object
     */
    public SubscriptionObserverPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the %SubscriptionObserverPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        private static SubscriptionObserverObject thisSubscriptionObserverObject(ExecutionContext cx, Object o) {
            if (!(o instanceof SubscriptionObserverObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            return (SubscriptionObserverObject) o;
        }

        /**
         * %SubscriptionObserverPrototype%.next ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the next value
         * @return the result {@code this.[[Observer]].next()} or {@code undefined} if this subscription is closed
         */
        @Function(name = "next", arity = 1)
        public static Object next(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            SubscriptionObserverObject o = thisSubscriptionObserverObject(cx, thisValue);
            /* step 4 */
            SubscriptionObject subscription = o.getSubscription();
            /* step 5 */
            if (SubscriptionClosed(subscription)) {
                return UNDEFINED;
            }
            /* step 6 */
            ScriptObject observer = subscription.getObserver();
            /* step 7 (implicit) */
            /* steps 8-11 */
            try {
                /* steps 8, 9.a */
                Callable nextMethod = GetMethod(cx, observer, "next");
                /* steps 9.b-c */
                Object result;
                if (nextMethod == null) {
                    result = UNDEFINED;
                } else {
                    result = nextMethod.call(cx, observer, value);
                }
                /* step 11 */
                return result;
            } catch (ScriptException e) {
                /* step 10 */
                // FIXME: spec bug - CloseSubscription is not defined
                // FIXME: spec bug - incorrect ReturnIfAbrupt does not match tests
                try {
                    CloseSubscription(cx, subscription);
                } catch (ScriptException ignore) {
                }
                /* step 11 */
                throw e;
            }
        }

        /**
         * %SubscriptionObserverPrototype%.error ( exception )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param exception
         *            the exception value
         * @return the result {@code this.[[Observer]].error()}
         */
        @Function(name = "error", arity = 1)
        public static Object error(ExecutionContext cx, Object thisValue, Object exception) {
            /* steps 1-3 */
            SubscriptionObserverObject o = thisSubscriptionObserverObject(cx, thisValue);
            /* step 4 */
            SubscriptionObject subscription = o.getSubscription();
            /* step 5 */
            if (SubscriptionClosed(subscription)) {
                throw ScriptException.create(exception);
            }
            /* step 6 */
            ScriptObject observer = subscription.getObserver();
            /* step 7 (implicit) */
            /* step 8 */
            subscription.clearObserver();
            /* steps 9-13 */
            Object result;
            try {
                /* steps 9, 10.a */
                Callable errorMethod = GetMethod(cx, observer, "error");
                /* steps 10.b-c */
                if (errorMethod == null) {
                    /* step 10.b */
                    result = null;
                } else {
                    /* step 10.c */
                    result = errorMethod.call(cx, observer, exception);
                }
            } catch (ScriptException e) {
                /* steps 11-12 */
                // FIXME: spec bug - incorrect ReturnIfAbrupt does not match tests
                try {
                    CleanupSubscription(cx, subscription);
                } catch (ScriptException ignore) {
                }
                /* step 13 */
                throw e;
            }
            /* step 10.b */
            if (result == null) {
                // FIXME: spec bug - tests expect CleanupSubscription is called if no error method found
                CleanupSubscription(cx, subscription);
                throw ScriptException.create(exception);
            }
            /* steps 11-12 */
            CleanupSubscription(cx, subscription);
            /* step 13 */
            return result;
        }

        /**
         * %SubscriptionObserverPrototype%.complete ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return the result {@code this.[[Observer]].complete()}
         */
        @Function(name = "complete", arity = 1)
        public static Object complete(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            SubscriptionObserverObject o = thisSubscriptionObserverObject(cx, thisValue);
            /* step 4 */
            SubscriptionObject subscription = o.getSubscription();
            /* step 5 */
            if (SubscriptionClosed(subscription)) {
                return UNDEFINED;
            }
            /* step 6 */
            ScriptObject observer = subscription.getObserver();
            /* step 7 (implicit) */
            /* step 8 */
            subscription.clearObserver();
            /* steps 9-13 */
            Object result;
            try {
                /* steps 9, 10.a */
                Callable completeMethod = GetMethod(cx, observer, "complete");
                /* steps 10.b-c */
                if (completeMethod == null) {
                    /* step 10.b */
                    result = UNDEFINED;
                } else {
                    /* step 10.c */
                    result = completeMethod.call(cx, observer, value);
                }
            } catch (ScriptException e) {
                /* steps 11-12 */
                // FIXME: spec bug - incorrect ReturnIfAbrupt does not match tests
                try {
                    CleanupSubscription(cx, subscription);
                } catch (ScriptException ignore) {
                }
                /* step 13 */
                throw e;
            }
            /* steps 11-12 */
            CleanupSubscription(cx, subscription);
            /* step 13 */
            return result;
        }
    }
}
