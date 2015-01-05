/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.number;

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

    private boolean initialized = false;

    /**
     * Constructs a new Number object.
     * 
     * @param realm
     *            the realm object
     */
    public NumberObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns {@code true} if this Number object is initialized.
     * 
     * @return {@code true} if the object is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * [[NumberData]]
     * 
     * @return the number value
     */
    public double getNumberData() {
        return numberData;
    }

    /**
     * [[NumberData]]
     * 
     * @param numberData
     *            the new number value
     */
    public void setNumberData(double numberData) {
        assert !this.initialized : "NumberObject already initialized";
        this.initialized = true;
        this.numberData = numberData;
    }

    /**
     * Custom helper function
     * 
     * @param cx
     *            the execution context
     * @param numberData
     *            the number value
     * @return the new number object
     */
    public static NumberObject NumberCreate(ExecutionContext cx, double numberData) {
        NumberObject obj = new NumberObject(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(Intrinsics.NumberPrototype));
        obj.setNumberData(numberData);
        return obj;
    }
}
