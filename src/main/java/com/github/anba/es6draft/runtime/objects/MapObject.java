/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.14 Map Objects</h2>
 * <ul>
 * <li>15.14.6 Properties of Map Instances
 * </ul>
 */
public class MapObject extends OrdinaryObject {
    /**
     * [[MapData]]
     */
    private LinkedMap<Object, Object> mapData;

    public MapObject(Realm realm) {
        super(realm);
        mapData = new LinkedMap<>(LinkedMap.HashMapBuilder);
    }

    public LinkedMap<Object, Object> getMapData() {
        return mapData;
    }
}
