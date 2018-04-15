/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.bigint;

import java.math.BigInteger;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>BigInt</h1><br>
 * <h2>BigInt Objects</h2>
 * <ul>
 * <li>Properties of BigInt Instances
 * </ul>
 */
public final class BigIntObject extends OrdinaryObject {
    /** [[BigIntData]] */
    private final BigInteger bigIntData;

    /**
     * Constructs a new BigInt object.
     * 
     * @param realm
     *            the realm object
     * @param bigIntData
     *            the BigInt data
     */
    BigIntObject(Realm realm, BigInteger bigIntData) {
        super(realm);
        this.bigIntData = bigIntData;
    }

    /**
     * Constructs a new BigInt object.
     * 
     * @param realm
     *            the realm object
     * @param bigIntData
     *            the BigInt data
     * @param prototype
     *            the prototype object
     */
    BigIntObject(Realm realm, BigInteger bigIntData, ScriptObject prototype) {
        super(realm, prototype);
        this.bigIntData = bigIntData;
    }

    /**
     * [[BigIntData]]
     * 
     * @return the BigInt value
     */
    public BigInteger getBigIntData() {
        return bigIntData;
    }

    @Override
    public String toString() {
        return String.format("%s, bigIntData=%s", super.toString(), bigIntData);
    }

    /**
     * Creates a new BigInt object with the default %BigIntPrototype% prototype object.
     * 
     * @param cx
     *            the execution context
     * @param bigIntData
     *            the BigInt value
     * @return the new BigInt object
     */
    public static BigIntObject BigIntCreate(ExecutionContext cx, BigInteger bigIntData) {
        return new BigIntObject(cx.getRealm(), bigIntData, cx.getIntrinsic(Intrinsics.BigIntPrototype));
    }

    /**
     * Creates a new BigInt object.
     * 
     * @param cx
     *            the execution context
     * @param bigIntData
     *            the BigInt value
     * @param prototype
     *            the prototype object
     * @return the new BigInt object
     */
    public static BigIntObject BigIntCreate(ExecutionContext cx, BigInteger bigIntData, ScriptObject prototype) {
        return new BigIntObject(cx.getRealm(), bigIntData, prototype);
    }
}
