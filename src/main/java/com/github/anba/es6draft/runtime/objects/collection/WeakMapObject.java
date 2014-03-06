/**
 * Copyright (c) 2012-2014 André Bargull
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
    private WeakHashMap<ScriptObject, Object> weakMapData = null;

    public WeakMapObject(Realm realm) {
        super(realm);
    }

    public WeakHashMap<ScriptObject, Object> getWeakMapData() {
        return weakMapData;
    }

    public void initialise() {
        assert this.weakMapData == null : "WeakMap already initialised";
        // no ephemeron tables in java :(
        this.weakMapData = new WeakHashMap<>();
    }

    public boolean isInitialised() {
        return (weakMapData != null);
    }
}
