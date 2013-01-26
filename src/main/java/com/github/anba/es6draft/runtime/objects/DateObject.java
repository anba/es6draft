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
 * <h2>15.9 Date Objects</h2>
 * <ul>
 * <li>15.9.6 Properties of Date Instances
 * </ul>
 */
public class DateObject extends OrdinaryObject implements Scriptable {
    /**
     * [[DateValue]]
     */
    private double dateValue;

    public DateObject(Realm realm, double dateValue) {
        super(realm);
        this.dateValue = dateValue;
    }

    /**
     * [[DateValue]]
     */
    public double getDateValue() {
        return dateValue;
    }

    /**
     * [[DateValue]]
     */
    public void setDateValue(double dateValue) {
        this.dateValue = dateValue;
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinDate;
    }
}
