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
 * <li>15.16.6 Properties of Set Instances
 * </ul>
 */
public class SetObject extends OrdinaryObject {
    /**
     * [[SetData]]
     */
    private LinkedMap<Object, Void> setData;

    public SetObject(Realm realm) {
        super(realm);
        setData = new LinkedMap<>(LinkedMap.HashMapBuilder);
    }

    public LinkedMap<Object, Void> getSetData() {
        return setData;
    }
}
