/**
 * Copyright (c) André Bargull
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
 * <h2>23.2 Set Objects</h2>
 * <ul>
 * <li>23.2.4 Properties of Set Instances
 * </ul>
 */
public final class SetObject extends OrdinaryObject {
    /** [[SetData]] */
    private final LinkedMap<Object, Void> setData = new LinkedMapImpl<>();

    /**
     * Constructs a new Set object.
     * 
     * @param realm
     *            the realm object
     */
    public SetObject(Realm realm) {
        super(realm);
    }

    /**
     * [[SetData]]
     * <p>
     * Returns the underlying set data.
     * 
     * @return the underlying set data
     */
    public LinkedMap<Object, Void> getSetData() {
        return setData;
    }
}
