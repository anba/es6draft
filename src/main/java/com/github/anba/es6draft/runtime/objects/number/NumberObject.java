/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.number;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>20 Numbers and Dates</h1><br>
 * <h2>20.1 Number Objects</h2>
 * <ul>
 * <li>20.1.4 Properties of Number Instances
 * </ul>
 */
public class NumberObject extends OrdinaryObject {
    /** [[NumberData]] */
    private final double numberData;

    /**
     * Constructs a new Number object.
     * 
     * @param realm
     *            the realm object
     * @param numberData
     *            the number data
     */
    NumberObject(Realm realm, double numberData) {
        super(realm);
        this.numberData = numberData;
    }

    /**
     * Constructs a new Number object.
     * 
     * @param realm
     *            the realm object
     * @param numberData
     *            the number data
     * @param prototype
     *            the prototype object
     */
    public NumberObject(Realm realm, double numberData, ScriptObject prototype) {
        super(realm);
        this.numberData = numberData;
        setPrototype(prototype);
    }

    /**
     * [[NumberData]]
     * 
     * @return the number value
     */
    public double getNumberData() {
        return numberData;
    }

    @Override
    public String className() {
        return "Number";
    }

    /**
     * Creates a new Number object with the default %NumberPrototype% prototype object.
     * 
     * @param cx
     *            the execution context
     * @param numberData
     *            the number value
     * @return the new number object
     */
    public static NumberObject NumberCreate(ExecutionContext cx, double numberData) {
        return new NumberObject(cx.getRealm(), numberData,
                cx.getIntrinsic(Intrinsics.NumberPrototype));
    }

    /**
     * Creates a new Number object.
     * 
     * @param cx
     *            the execution context
     * @param numberData
     *            the number value
     * @param prototype
     *            the prototype object
     * @return the new number object
     */
    public static NumberObject NumberCreate(ExecutionContext cx, double numberData,
            ScriptObject prototype) {
        return new NumberObject(cx.getRealm(), numberData, prototype);
    }
}
