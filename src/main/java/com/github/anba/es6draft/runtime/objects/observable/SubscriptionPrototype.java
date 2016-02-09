/**
 * Copyright (c) 2012-2015 André Bargull
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
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

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
            /* steps 2-3 */
            if (!(thisValue instanceof SubscriptionObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            SubscriptionObject subscription = (SubscriptionObject) thisValue;
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
