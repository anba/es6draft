/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import java.util.WeakHashMap;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.17 WeakSet Objects</h2>
 * <ul>
 * <li>15.17.4 Properties of WeakSet Instances
 * </ul>
 */
public class WeakSetObject extends OrdinaryObject {
    /** [[WeakSetData]] */
    private WeakHashMap<Object, Boolean> weakSetData = null;

    public WeakSetObject(Realm realm) {
        super(realm);
    }

    public WeakHashMap<Object, Boolean> getWeakSetData() {
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
