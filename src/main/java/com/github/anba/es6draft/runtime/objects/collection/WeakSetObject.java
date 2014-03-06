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
 * <h2>23.4 WeakSet Objects</h2>
 * <ul>
 * <li>23.4.4 Properties of WeakSet Instances
 * </ul>
 */
public final class WeakSetObject extends OrdinaryObject {
    /** [[WeakSetData]] */
    private WeakHashMap<ScriptObject, Boolean> weakSetData = null;

    public WeakSetObject(Realm realm) {
        super(realm);
    }

    public WeakHashMap<ScriptObject, Boolean> getWeakSetData() {
        return weakSetData;
    }

    public void initialise() {
        assert this.weakSetData == null : "WeakSet already initialised";
        // no ephemeron tables in java :(
        this.weakSetData = new WeakHashMap<>();
    }

    public boolean isInitialised() {
        return (weakSetData != null);
    }
}
