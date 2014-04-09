/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.date;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.3 Date Objects</h2>
 * <ul>
 * <li>20.3.5 Properties of Date Instances
 * </ul>
 */
public final class DateObject extends OrdinaryObject {
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
     * 
     * @return the date value
     */
    public double getDateValue() {
        return dateValue;
    }

    /**
     * [[DateValue]]
     * 
     * @param dateValue
     *            the new date value
     */
    public void setDateValue(double dateValue) {
        this.initialised = true;
        this.dateValue = dateValue;
    }
}
