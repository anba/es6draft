/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Observable.prototype.forEach
 */
public final class ObserverObject extends OrdinaryObject {
    /** [[CallbackFn]] */
    private final Callable callbackFn;
    /** [[PromiseCapability]] */
    private final PromiseCapability<PromiseObject> promiseCapability;
    /** [[Subscription]] */
    private ScriptObject subscription;

    /**
     * Constructs a new Observer object.
     * 
     * @param realm
     *            the realm object
     * @param callbackFn
     *            the callback function
     * @param promiseCapability
     *            the promise capability
     * @param prototype
     *            the prototype object
     */
    ObserverObject(Realm realm, Callable callbackFn, PromiseCapability<PromiseObject> promiseCapability,
            ScriptObject prototype) {
        super(realm, prototype);
        this.callbackFn = callbackFn;
        this.promiseCapability = promiseCapability;
    }

    /**
     * [[CallbackFn]]
     * 
     * @return the callback function
     */
    public Callable getCallbackFn() {
        return callbackFn;
    }

    /**
     * [[PromiseCapability]]
     * 
     * @return the promise capability record
     */
    public PromiseCapability<PromiseObject> getPromiseCapability() {
        return promiseCapability;
    }

    /**
     * [[Subscription]]
     * 
     * @return the subscription object
     */
    public ScriptObject getSubscription() {
        return subscription;
    }

    /**
     * [[Subscription]]
     * 
     * @param subscription
     *            the new subscription object
     */
    public void setSubscription(ScriptObject subscription) {
        assert subscription != null;
        this.subscription = subscription;
    }
}
