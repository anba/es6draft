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
 * <h2>15.15 WeakMap Objects</h2>
 * <ul>
 * <li>15.15.6 Properties of WeakMap Instances
 * </ul>
 */
public class WeakMapObject extends OrdinaryObject {
    /**
     * [[WeakMapData]]
     */
    private WeakHashMap<Object, Object> weakMapData;

    public WeakMapObject(Realm realm) {
        super(realm);
        // no ephemeron tables in java :(
        weakMapData = new WeakHashMap<>();
    }

    public WeakHashMap<Object, Object> getWeakMapData() {
        return weakMapData;
    }
}
