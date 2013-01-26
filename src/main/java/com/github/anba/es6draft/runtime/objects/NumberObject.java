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
 * <h2>15.7 Number Objects</h2>
 * <ul>
 * <li>15.7.5 Properties of Number Instances
 * </ul>
 */
public class NumberObject extends OrdinaryObject implements Scriptable {
    /**
     * [[NumberData]]
     */
    // FIXME: spec bug [[NumberValue]] (bug 1062)
    private final double numberData;

    public NumberObject(Realm realm, double numberData) {
        super(realm);
        this.numberData = numberData;
    }

    /**
     * [[NumberData]]
     */
    public double getNumberData() {
        return numberData;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinNumberWrapper;
    }
}
