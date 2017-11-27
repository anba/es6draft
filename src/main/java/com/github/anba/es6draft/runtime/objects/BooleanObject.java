/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
    private final boolean booleanData;

    /**
     * Constructs a new Boolean object.
     * 
     * @param realm
     *            the realm object
     * @param booleanData
     *            the boolean data
     */
    BooleanObject(Realm realm, boolean booleanData) {
        super(realm);
        this.booleanData = booleanData;
    }

    /**
     * Constructs a new Boolean object.
     * 
     * @param realm
     *            the realm object
     * @param booleanData
     *            the boolean data
     * @param prototype
     *            the prototype object
     */
    public BooleanObject(Realm realm, boolean booleanData, ScriptObject prototype) {
        super(realm, prototype);
        this.booleanData = booleanData;
    }

    /**
     * [[BooleanData]]
     * 
     * @return the boolean value
     */
    public final boolean getBooleanData() {
        return booleanData;
    }

    @Override
    public final String className() {
        return "Boolean";
    }

    @Override
    public String toString() {
        return String.format("%s, booleanData=%b", super.toString(), booleanData);
    }

    /**
     * Creates a new Boolean object with the default %BooleanPrototype% prototype object.
     * 
     * @param cx
     *            the execution context
     * @param booleanData
     *            the boolean value
     * @return the new boolean object
     */
    public static BooleanObject BooleanCreate(ExecutionContext cx, boolean booleanData) {
        return new BooleanObject(cx.getRealm(), booleanData, cx.getIntrinsic(Intrinsics.BooleanPrototype));
    }

    /**
     * Creates a new Boolean object.
     * 
     * @param cx
     *            the execution context
     * @param booleanData
     *            the boolean value
     * @param prototype
     *            the prototype object
     * @return the new boolean object
     */
    public static BooleanObject BooleanCreate(ExecutionContext cx, boolean booleanData, ScriptObject prototype) {
        return new BooleanObject(cx.getRealm(), booleanData, prototype);
    }
}
