/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Subscription Observer Objects</h1>
 * <ul>
 * <li>Properties of Subscription Observer Instances
 * </ul>
 */
public final class SubscriptionObserverObject extends OrdinaryObject {
    /** [[Subscription]] */
    private final SubscriptionObject subscription;

    /**
     * Constructs a new Subscription Observer object.
     * 
     * @param realm
     *            the realm object
     * @param subscription
     *            the subscription object
     * @param prototype
     *            the prototype object
     */
    SubscriptionObserverObject(Realm realm, SubscriptionObject subscription, ScriptObject prototype) {
        super(realm);
        this.subscription = subscription;
        setPrototype(prototype);
    }

    /**
     * [[Subscription]]
     * 
     * @return the current subscription
     */
    public SubscriptionObject getSubscription() {
        return subscription;
    }
}
