/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Subscription Abstract Operations
 */
public final class SubscriptionAbstractOperations {
    private SubscriptionAbstractOperations() {
    }

    /**
     * CreateSubscription ( observer ) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param observer
     *            the observer object
     * @return the new subscription object
     */
    public static SubscriptionObject CreateSubscription(ExecutionContext cx, ScriptObject observer) {
        /* step 1 (implicit) */
        /* steps 2-4 */
        SubscriptionObject subscription = new SubscriptionObject(cx.getRealm(), observer,
                cx.getIntrinsic(Intrinsics.SubscriptionPrototype));
        /* step 5 */
        return subscription;
    }

    /**
     * CleanupSubscription ( subscription ) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param subscription
     *            the subscription object
     */
    public static void CleanupSubscription(ExecutionContext cx, SubscriptionObject subscription) {
        /* step 1 (implicit) */
        /* step 2 */
        Callable cleanup = subscription.getCleanup();
        /* step 3 */
        if (cleanup == null) {
            return;
        }
        /* step 4 (implicit) */
        /* step 5 */
        subscription.setCleanup(null);
        /* steps 6-7 */
        cleanup.call(cx, UNDEFINED);
        /* step 8 (return) */
    }

    /**
     * SubscriptionClosed ( subscription ) Abstract Operation
     * 
     * @param subscription
     *            the subscription object
     * @return {@code true} if the subscription object is closed
     */
    public static boolean SubscriptionClosed(SubscriptionObject subscription) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        return subscription.getObserver() == null;
    }

    /**
     * CloseSubscription ( subscription ) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param subscription
     *            the subscription object
     */
    // FIXME: spec bug - not defined
    public static void CloseSubscription(ExecutionContext cx, SubscriptionObject subscription) {
        if (SubscriptionClosed(subscription)) {
            return;
        }
        subscription.clearObserver();
        CleanupSubscription(cx, subscription);
    }
}
