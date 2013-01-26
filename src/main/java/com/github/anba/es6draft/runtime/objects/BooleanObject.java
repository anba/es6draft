/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.6 Boolean Objects</h2>
 * <ul>
 * <li>15.6.5 Properties of Boolean Instances
 * </ul>
 */
public class BooleanObject extends OrdinaryObject implements Scriptable {
    /**
     * [[BooleanData]]
     */
    private final boolean booleanData;

    public BooleanObject(Realm realm, boolean booleanData) {
        super(realm);
        this.booleanData = booleanData;
    }

    /**
     * [[BooleanData]]
     */
    public boolean getBooleanData() {
        return booleanData;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinBooleanWrapper;
    }
}
