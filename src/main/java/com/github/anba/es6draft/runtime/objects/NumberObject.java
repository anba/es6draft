/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.1 Number Objects</h2>
 * <ul>
 * <li>20.1.4 Properties of Number Instances
 * </ul>
 */
public final class NumberObject extends OrdinaryObject {
    /** [[NumberData]] */
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
     * Custom helper function
     */
    public static NumberObject NumberCreate(ExecutionContext cx, double numberData) {
        NumberObject obj = new NumberObject(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(Intrinsics.NumberPrototype));
        obj.setNumberData(numberData);
        return obj;
    }
}
