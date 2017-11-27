/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import java.util.WeakHashMap;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.3 WeakMap Objects</h2>
 * <ul>
 * <li>23.3.4 Properties of WeakMap Instances
 * </ul>
 */
public final class WeakMapObject extends OrdinaryObject {
    /** [[WeakMapData]] */
    // no ephemeron tables in java :(
    private final WeakHashMap<ScriptObject, Object> weakMapData = new WeakHashMap<>();

    /**
     * Constructs a new WeakMap object.
     * 
     * @param realm
     *            the realm object
     */
    public WeakMapObject(Realm realm) {
        super(realm);
    }

    /**
     * [[WeakMapData]]
     * <p>
     * Returns the underlying map data.
     * 
     * @return the underlying map data
     */
    public WeakHashMap<ScriptObject, Object> getWeakMapData() {
        return weakMapData;
    }
}
