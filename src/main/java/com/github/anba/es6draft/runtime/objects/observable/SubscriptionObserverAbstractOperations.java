/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Intrinsics;

/**
 * Subscription Observer Abstract Operations
 */
public final class SubscriptionObserverAbstractOperations {
    private SubscriptionObserverAbstractOperations() {
    }

    /**
     * CreateSubscriptionObserver ( subscription ) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param subscription
     *            the subscription object
     * @return the new subscription observer object
     */
    public static SubscriptionObserverObject CreateSubscriptionObserver(ExecutionContext cx,
            SubscriptionObject subscription) {
        /* step 1 (implicit) */
        // FIXME: spec bug - typo 'observer'
        /* steps 2-3 */
        SubscriptionObserverObject subscriptionObserver = new SubscriptionObserverObject(cx.getRealm(), subscription,
                cx.getIntrinsic(Intrinsics.SubscriptionObserverPrototype));
        /* step 4 */
        return subscriptionObserver;
    }
}
