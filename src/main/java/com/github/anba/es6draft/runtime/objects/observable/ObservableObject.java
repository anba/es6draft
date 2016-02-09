/**
 * Copyright (c) 2012-2016 André Bargull
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
 * <h1>Observable</h1>
 * <ul>
 * <li>Properties of Observable Instances
 * </ul>
 */
public final class ObservableObject extends OrdinaryObject {
    /** [[Subscriber]] */
    private final Callable subscriber;

    /**
     * Constructs a new Observable object.
     * 
     * @param realm
     *            the realm object
     * @param subscriber
     *            the subscriber function
     * @param prototype
     *            the prototype object
     */
    ObservableObject(Realm realm, Callable subscriber, ScriptObject prototype) {
        super(realm);
        this.subscriber = subscriber;
        setPrototype(prototype);
    }

    /**
     * [[Subscriber]]
     * 
     * @return the current subscriber
     */
    public Callable getSubscriber() {
        return subscriber;
    }
}
