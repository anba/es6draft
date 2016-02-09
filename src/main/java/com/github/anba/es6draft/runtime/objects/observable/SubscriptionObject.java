/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.observable;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Subscription Objects</h1>
 * <ul>
 * <li>Properties of Subscription Instances
 * </ul>
 */
public final class SubscriptionObject extends OrdinaryObject {
    /** [[Observer]] */
    private ScriptObject observer;
    /** [[Cleanup]] */
    private Callable cleanup;

    /**
     * Constructs a new Subscription object.
     * 
     * @param realm
     *            the realm object
     * @param observer
     *            the observer object
     * @param prototype
     *            the prototype object
     */
    SubscriptionObject(Realm realm, ScriptObject observer, ScriptObject prototype) {
        super(realm);
        this.observer = observer;
        setPrototype(prototype);
    }

    /**
     * [[Observer]]
     * 
     * @return the current observer
     */
    public ScriptObject getObserver() {
        return observer;
    }

    /**
     * [[Observer]]
     */
    public void clearObserver() {
        this.observer = null;
    }

    /**
     * [[Cleanup]]
     * 
     * @return the current cleanup
     */
    public Callable getCleanup() {
        return cleanup;
    }

    /**
     * [[Cleanup]]
     * 
     * @param cleanup
     *            the new cleanup
     */
    public void setCleanup(Callable cleanup) {
        this.cleanup = cleanup;
    }
}
