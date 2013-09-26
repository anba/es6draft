/**
 * Copyright (c) 2012-2013 André Bargull
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
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.16 Set Objects</h2>
 * <ul>
 * <li>15.16.4 Properties of Set Instances
 * </ul>
 */
public class SetObject extends OrdinaryObject {
    /** [[SetData]] */
    private LinkedMap<Object, Void> setData = null;

    /** [[SetComparator]] */
    private Comparator setComparator;

    public enum Comparator {
        SameValue, SameValueZero
    }

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
    public Comparator getSetComparator() {
        return setComparator;
    }

    public void initialise(Comparator comparator) {
        assert this.setData == null : "Set already initialised";
        this.setData = new LinkedMapImpl<Void>(comparator == Comparator.SameValueZero);
        this.setComparator = comparator;
    }

    public boolean isInitialised() {
        return (setData != null);
    }
}
