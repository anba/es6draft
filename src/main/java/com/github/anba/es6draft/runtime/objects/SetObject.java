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
 * <h2>15.16 Set Objects</h2>
 * <ul>
 * <li>15.16.5 Properties of Set Instances
 * </ul>
 */
public class SetObject extends OrdinaryObject {
    /** [[SetData]] */
    private LinkedMap<Object, Void> setData = null;

    /** [[SetComparator]] */
    private String setComparator;

    public SetObject(Realm realm) {
        super(realm);
    }

    /**
     * [[SetData]]
     */
    public LinkedMap<Object, Void> getSetData() {
        return setData;
    }

    /**
     * [[SetComparator]]
     */
    public String getSetComparator() {
        return setComparator;
    }

    public void initialise(String comparator) {
        assert this.setData == null : "Set already initialised";
        this.setData = new LinkedMap<>(LinkedMap.HashMapBuilder);
        this.setComparator = comparator;
    }

    public boolean isInitialised() {
        return (setData != null);
    }
}
