/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.intl;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>12 DateTimeFormat Objects</h1>
 * <ul>
 * <li>12.4 Properties of Intl.DateTimeFormat Instances
 * </ul>
 */
public class DateTimeFormatObject extends OrdinaryObject implements Scriptable {
    public DateTimeFormatObject(Realm realm) {
        super(realm);
    }
}
