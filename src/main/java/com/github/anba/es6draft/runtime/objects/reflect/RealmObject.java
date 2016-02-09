/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.reflect;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.? Realm Objects</h2>
 * <ul>
 * <li>26.?.4 Properties of Reflect.Realm Instances
 * </ul>
 */
public final class RealmObject extends OrdinaryObject {
    /** [[RealmRecord]] */
    private Realm realm;

    /**
     * Constructs a new Realm object.
     * 
     * @param realm
     *            the realm object
     */
    public RealmObject(Realm realm) {
        super(realm);
    }

    /**
     * [[RealmRecord]
     *
     * @return the realm instance
     */
    public Realm getRealm() {
        return realm;
    }

    /**
     * [[RealmRecord]]
     *
     * @param realm
     *            the new realm instance
     */
    public void setRealm(Realm realm) {
        assert this.realm == null && realm != null;
        this.realm = realm;
    }
}
