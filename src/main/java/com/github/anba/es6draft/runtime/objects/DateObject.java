/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.9 Date Objects</h2>
 * <ul>
 * <li>15.9.5 Properties of Date Instances
 * </ul>
 */
public class DateObject extends OrdinaryObject {
    /** [[DateValue]] */
    private double dateValue;

    private boolean initialised = false;

    public DateObject(Realm realm) {
        super(realm);
    }

    public boolean isInitialised() {
        return initialised;
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
        this.initialised = true;
        this.dateValue = dateValue;
    }
}
