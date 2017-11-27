/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

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
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Observable</h1>
 * <ul>
 * <li>The %SubscriptionPrototype% Object
 * </ul>
 */
public final class SubscriptionPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new %SubscriptionPrototype% object.
     * 
     * @param realm
     *            the realm object
     */
    public SubscriptionPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the %SubscriptionPrototype% Object
     */
    public enum Properties {
        ;

        private static SubscriptionObject thisSubscriptionObject(ExecutionContext cx, Object value, String method) {
            if (!(value instanceof SubscriptionObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
            }
            return (SubscriptionObject) value;
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * get %SubscriptionPrototype%.closed
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
            SubscriptionObject subscription = thisSubscriptionObject(cx, thisValue, "%SubscriptionPrototype%.closed");
            /* step 4 */
            return SubscriptionClosed(subscription);
        }

        /**
         * %SubscriptionPrototype%.unsubscribe ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the undefined value
         */
        @Function(name = "unsubscribe", arity = 0)
        public static Object unsubscribe(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            SubscriptionObject subscription = thisSubscriptionObject(cx, thisValue,
                    "%SubscriptionPrototype%.unsubscribe");
            /* step 4 */
            if (SubscriptionClosed(subscription)) {
                return UNDEFINED;
            }
            /* step 5 */
            subscription.clearObserver();
            /* step 6 */
            CleanupSubscription(cx, subscription);
            return UNDEFINED;
        }
    }
}
