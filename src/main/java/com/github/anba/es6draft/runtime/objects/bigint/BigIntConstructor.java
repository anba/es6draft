/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.bigint;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToIndex;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitive;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.bigint.BigIntAbstractOperations.NumberToBigInt;
import static com.github.anba.es6draft.runtime.objects.bigint.BigIntAbstractOperations.ToBigInt;

import java.math.BigInteger;

import com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitiveHint;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>BigInt</h1><br>
 * <h2>BigInt Objects</h2>
 * <ul>
 * <li>The BigInt Constructor
 * <li>Properties of the BigInt Constructor
 * </ul>
 */
public final class BigIntConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new BigInt constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public BigIntConstructor(Realm realm) {
        super(realm, "BigInt", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * BigInt ( value )
     */
    @Override
    public BigInteger call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object value = argument(args, 0);

        /* step 1 (not applicable) */
        /* step 2 */
        Object prim = ToPrimitive(calleeContext, value, ToPrimitiveHint.Number);
        /* step 3 */
        if (Type.isNumber(prim)) {
            return NumberToBigInt(calleeContext, Type.numberValue(prim));
        }
        /* step 4 */
        return ToBigInt(calleeContext, prim);
    }

    /**
     * BigInt ( value )
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.BigIntCreate);
    }

    /**
     * Properties of the BigInt Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "BigInt";

        /**
         * BigInt.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.BigIntPrototype;

        /**
         * BigInt.asUintN ( bits, bigint )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param bits
         *            the bits value
         * @param bigint
         *            the bigint value
         * @return the result BigInt value
         */
        @Function(name = "asUintN", arity = 2)
        public static Object asUintN(ExecutionContext cx, Object thisValue, Object bits, Object bigint) {
            /* step 1 */
            long bitsIndex = ToIndex(cx, bits);
            /* step 2 */
            BigInteger bigIntValue = ToBigInt(cx, bigint);
            /* step 3 */
            if (bitsIndex >= Integer.MAX_VALUE) {
                return bigIntValue;
            }
            BigInteger m = BigInteger.valueOf(2).pow((int) bitsIndex);
            return bigIntValue.mod(m);
        }

        /**
         * BigInt.asIntN ( bits, bigint )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param bits
         *            the bits value
         * @param bigint
         *            the bigint value
         * @return the result BigInt value
         */
        @Function(name = "asIntN", arity = 2)
        public static Object asIntN(ExecutionContext cx, Object thisValue, Object bits, Object bigint) {
            /* step 1 */
            long bitsIndex = ToIndex(cx, bits);
            /* step 2 */
            BigInteger bigIntValue = ToBigInt(cx, bigint);
            /* steps 3-4 */
            if (bitsIndex >= Integer.MAX_VALUE) {
                return bigIntValue;
            }
            if (bitsIndex == 0) {
                return BigInteger.ZERO;
            }
            BigInteger m = BigInteger.valueOf(2).pow((int) bitsIndex);
            BigInteger mod = bigIntValue.mod(m);
            if (mod.compareTo(m.shiftRight(1)) >= 0)
                return mod.subtract(m);
            return mod;
        }
    }
}
