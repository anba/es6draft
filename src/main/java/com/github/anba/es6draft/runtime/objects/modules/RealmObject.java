/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>26 Reflection</h1><br>
 * <h2>26.2 Realm Objects</h2>
 * <ul>
 * <li>26.2.4 Properties of %Realm% Instances
 * </ul>
 */
public final class RealmObject extends OrdinaryObject {
    /** [[Realm]] */
    private Realm realm;

    public RealmObject(Realm realm) {
        super(realm);
    }

    /**
     * [[Realm]
     *
     * @return the realm instance
     */
    public Realm getRealm() {
        return realm;
    }

    /**
     * [[Realm]]
     *
     * @param realm
     *            the new realm instance
     */
    public void setRealm(Realm realm) {
        assert this.realm == null && realm != null;
        this.realm = realm;
    }
}
