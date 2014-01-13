/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.LinkedMapImpl;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.1 Map Objects</h2>
 * <ul>
 * <li>23.1.4 Properties of Map Instances
 * </ul>
 */
public class MapObject extends OrdinaryObject {
    /** [[MapData]] */
    private LinkedMap<Object, Object> mapData = null;

    /** [[MapComparator]] */
    private Comparator mapComparator;

    public enum Comparator {
        SameValue, SameValueZero
    }

    public MapObject(Realm realm) {
        super(realm);
    }

    /**
     * [[MapData]]
     */
    public LinkedMap<Object, Object> getMapData() {
        return mapData;
    }

    /**
     * [[MapComparator]]
     */
    public Comparator getMapComparator() {
        return mapComparator;
    }

    public void initialise(Comparator comparator) {
        assert this.mapData == null : "Map already initialised";
        this.mapData = new LinkedMapImpl<Object>(comparator == Comparator.SameValueZero);
        this.mapComparator = comparator;
    }

    public boolean isInitialised() {
        return (mapData != null);
    }
}
