/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.CleanupSubscription;
import static com.github.anba.es6draft.runtime.objects.observable.SubscriptionAbstractOperations.SubscriptionClosed;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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

        private static SubscriptionObserverObject thisSubscriptionObserverObject(ExecutionContext cx, Object value,
                String method) {
            if (!(value instanceof SubscriptionObserverObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
            }
            return (SubscriptionObserverObject) value;
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * get %SubscriptionObserverPrototype%.closed
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the species object
         */
        @Accessor(name = "closed", type = Accessor.Type.Getter)
        public static Object closed(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            SubscriptionObserverObject o = thisSubscriptionObserverObject(cx, thisValue,
                    "%SubscriptionObserverPrototype%.closed");
            /* step 4 */
            SubscriptionObject subscription = o.getSubscription();
            /* step 5 */
            return SubscriptionClosed(subscription);
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
         * @return the {@code undefined} value
         */
        @Function(name = "next", arity = 1)
        public static Object next(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            SubscriptionObserverObject o = thisSubscriptionObserverObject(cx, thisValue,
                    "%SubscriptionObserverPrototype%.next");
            /* step 4 */
            SubscriptionObject subscription = o.getSubscription();
            /* step 5 */
            if (SubscriptionClosed(subscription)) {
                return UNDEFINED;
            }
            /* step 6 */
            ScriptObject observer = subscription.getObserver();
            /* step 7 (implicit) */
            /* steps 8-10 */
            try {
                /* steps 8, 9.a */
                Callable nextMethod = GetMethod(cx, observer, "next");
                /* step 9.b */
                if (nextMethod != null) {
                    nextMethod.call(cx, observer, value);
                }
            } catch (ScriptException e) {
                /* steps 9.b.ii, 10 */
                cx.getRuntimeContext().getErrorReporter().accept(cx, e);
            }
            /* step 11 */
            return UNDEFINED;
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
         * @return the {@code undefined} value
         */
        @Function(name = "error", arity = 1)
        public static Object error(ExecutionContext cx, Object thisValue, Object exception) {
            /* steps 1-3 */
            SubscriptionObserverObject o = thisSubscriptionObserverObject(cx, thisValue,
                    "%SubscriptionObserverPrototype%.error");
            /* step 4 */
            SubscriptionObject subscription = o.getSubscription();
            /* step 5 */
            if (SubscriptionClosed(subscription)) {
                throw ScriptException.create(exception);
            }
            /* step 6 */
            ScriptObject observer = subscription.getObserver();
            /* step 7 (implicit) */
            /* steps 8-10 */
            try {
                /* step 8, 9.a */
                Callable errorMethod = GetMethod(cx, observer, "error");
                /* step 9.b */
                if (errorMethod != null) {
                    errorMethod.call(cx, observer, exception);
                }
            } catch (ScriptException e) {
                /* steps 9.b.ii, 10 */
                cx.getRuntimeContext().getErrorReporter().accept(cx, e);
            }
            /* step 11 */
            CleanupSubscription(cx, subscription);
            /* step 12 */
            return UNDEFINED;
        }

        /**
         * %SubscriptionObserverPrototype%.complete ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the {@code undefined} value
         */
        @Function(name = "complete", arity = 1)
        public static Object complete(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            SubscriptionObserverObject o = thisSubscriptionObserverObject(cx, thisValue,
                    "%SubscriptionObserverPrototype%.complete");
            /* step 4 */
            SubscriptionObject subscription = o.getSubscription();
            /* step 5 */
            if (SubscriptionClosed(subscription)) {
                return UNDEFINED;
            }
            /* step 6 */
            ScriptObject observer = subscription.getObserver();
            /* step 7 (implicit) */
            /* steps 8-10 */
            try {
                /* step 8, 9.a */
                Callable errorMethod = GetMethod(cx, observer, "complete");
                /* step 9.b */
                if (errorMethod != null) {
                    errorMethod.call(cx, observer);
                }
            } catch (ScriptException e) {
                /* steps 9.b.ii, 10 */
                cx.getRuntimeContext().getErrorReporter().accept(cx, e);
            }
            /* step 11 */
            CleanupSubscription(cx, subscription);
            /* step 12 */
            return UNDEFINED;
        }
    }
}
