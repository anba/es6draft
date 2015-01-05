/**
 * Copyright (c) 2012-2015 André Bargull
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
public final class BooleanObject extends OrdinaryObject {
    /** [[BooleanData]] */
    private boolean booleanData;

    private boolean initialized = false;

    /**
     * Constructs a new Boolean object.
     * 
     * @param realm
     *            the realm object
     */
    public BooleanObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns {@code true} if this Boolean object is initialized.
     * 
     * @return {@code true} if the object is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * [[BooleanData]]
     * 
     * @return the boolean value
     */
    public boolean getBooleanData() {
        assert this.initialized : "BooleanObject not initialized";
        return booleanData;
    }

    /**
     * [[BooleanData]]
     * 
     * @param booleanData
     *            the new boolean value
     */
    public void setBooleanData(boolean booleanData) {
        assert !this.initialized : "BooleanObject already initialized";
        this.initialized = true;
        this.booleanData = booleanData;
    }

    /**
     * Custom helper function
     * 
     * @param cx
     *            the execution context
     * @param booleanData
     *            the boolean value
     * @return the new boolean object
     */
    public static BooleanObject BooleanCreate(ExecutionContext cx, boolean booleanData) {
        BooleanObject obj = new BooleanObject(cx.getRealm());
        obj.setPrototype(cx.getIntrinsic(Intrinsics.BooleanPrototype));
        obj.setBooleanData(booleanData);
        return obj;
    }
}
