/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.zone;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Zones</h1><br>
 * <h2>Zone Objects</h2>
 * <ul>
 * <li>Properties of Zone Instances
 * </ul>
 */
public final class ZoneObject extends OrdinaryObject {
    /** [[ParentZone]] */
    private final ZoneObject parentZone;
    /** [[Realm]] */
    private final Realm realm;

    /**
     * Constructs a new Map object.
     * 
     * @param realm
     *            the realm object
     * @param parentZone
     *            the parent zone or {@code null}
     * @param prototype
     *            the prototype object
     */
    public ZoneObject(Realm realm, ZoneObject parentZone, ScriptObject prototype) {
        super(realm, prototype);
        this.realm = realm;
        this.parentZone = parentZone;
    }

    /**
     * [[ParentZone]]
     * <p>
     * Returns the parent zone.
     * 
     * @return the parent zone or {@code null}
     */
    public ZoneObject getParentZone() {
        return parentZone;
    }

    /**
     * [[Realm]]
     * <p>
     * Returns the realm instance.
     * 
     * @return the realm instance
     */
    public Realm getRealm() {
        return realm;
    }
}
