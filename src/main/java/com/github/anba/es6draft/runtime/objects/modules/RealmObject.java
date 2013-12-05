/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.modules;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>1 Modules: Semantics</h1><br>
 * <h2>1.5 Realm Objects</h2>
 */
public class RealmObject extends OrdinaryObject {
    /** [[Realm]] */
    private Realm realm;

    public RealmObject(Realm realm) {
        super(realm);
    }

    /** [[Realm]] */
    public Realm getRealm() {
        return realm;
    }

    /** [[Realm]] */
    public void setRealm(Realm realm) {
        assert this.realm == null && realm != null;
        this.realm = realm;
    }
}
