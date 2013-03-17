/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.4 Properties of Intl.DateTimeFormat Instances
 * </ul>
 */
public class DateTimeFormatObject extends OrdinaryObject implements ScriptObject {
    /**
     * [[boundFormat]]
     */
    private Callable boundFormat;

    public DateTimeFormatObject(Realm realm) {
        super(realm);
    }

    /**
     * [[boundFormat]]
     */
    public boolean hasBoundFormat() {
        return boundFormat != null;
    }

    /**
     * [[boundFormat]]
     */
    public Callable getBoundFormat() {
        return boundFormat;
    }

    /**
     * [[boundFormat]]
     */
    public void setBoundFormat(Callable boundFormat) {
        this.boundFormat = boundFormat;
    }

    public String getLocale() {
        // TODO Auto-generated method stub
        return null;
    }
}
