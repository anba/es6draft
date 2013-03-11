/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
    private double numberData;

    private boolean initialised = false;

    public NumberObject(Realm realm) {
        super(realm);
    }

    public boolean isInitialised() {
        return initialised;
    }

    /**
     * [[NumberData]]
     */
    public double getNumberData() {
        return numberData;
    }

    /**
     * [[NumberData]]
     */
    public void setNumberData(double numberData) {
        assert !this.initialised : "NumberObject already initialised";
        this.initialised = true;
        this.numberData = numberData;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinNumberWrapper;
    }

    /**
     * Custom helper function
     */
    public static NumberObject NumberCreate(Realm realm, double numberData) {
        NumberObject obj = new NumberObject(realm);
        obj.setPrototype(realm.getIntrinsic(Intrinsics.NumberPrototype));
        obj.setNumberData(numberData);
        return obj;
    }
}
