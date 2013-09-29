/**
 * Copyright (c) 2012-2013 André Bargull
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
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.3 Boolean Objects</h2>
 * <ul>
 * <li>19.3.4 Properties of Boolean Instances
 * </ul>
 */
public class BooleanObject extends OrdinaryObject {
    /** [[BooleanData]] */
    private boolean booleanData;

    private boolean initialised = false;

    public BooleanObject(Realm realm) {
        super(realm);
    }

    public boolean isInitialised() {
        return initialised;
    }

    /**
     * [[BooleanData]]
     */
    public boolean getBooleanData() {
        assert this.initialised : "BooleanObject not initialised";
        return booleanData;
    }

    /**
     * [[BooleanData]]
     */
    public void setBooleanData(boolean booleanData) {
        assert !this.initialised : "BooleanObject already initialised";
        this.initialised = true;
        this.booleanData = booleanData;
    }

    /**
     * Custom helper function
     */
    public static BooleanObject BooleanCreate(ExecutionContext cx, boolean booleanData) {
        BooleanObject obj = new BooleanObject(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(Intrinsics.BooleanPrototype));
        obj.setBooleanData(booleanData);
        return obj;
    }
}
