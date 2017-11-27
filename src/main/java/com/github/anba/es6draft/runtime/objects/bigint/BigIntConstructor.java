/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.bigint;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToIndex;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInt32;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitive;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newSyntaxError;
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
import com.github.anba.es6draft.runtime.internal.Strings;
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
         * BigInt.parseInt (string, radix)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param string
         *            the string
         * @param radix
         *            the radix value
         * @return the parsed BigInt
         */
        @Function(name = "parseInt", arity = 2)
        public static Object parseInt(ExecutionContext cx, Object thisValue, Object string, Object radix) {
            /* step 1 */
            String inputString = ToFlatString(cx, string);
            /* step 2 */
            String s = Strings.trimLeft(inputString);
            int len = s.length();
            int index = 0;
            /* steps 3-5 */
            boolean isPos = true;
            if (index < len && (s.charAt(index) == '+' || s.charAt(index) == '-')) {
                isPos = s.charAt(index) == '+';
                index += 1;
            }
            /* step 6 */
            int r = ToInt32(cx, radix);
            /* step 7 */
            boolean stripPrefix = true;
            if (r != 0) {
                /* step 8 */
                if (r < 2 || r > 36) {
                    throw newSyntaxError(cx, Messages.Key.InvalidNumberLiteral);
                }
                stripPrefix = r == 16;
            } else {
                /* step 9 */
                r = 10;
            }
            /* step 10 */
            if (stripPrefix && index + 1 < len && s.charAt(index) == '0'
                    && (s.charAt(index + 1) == 'x' || s.charAt(index + 1) == 'X')) {
                r = 16;
                index += 2;
            }
            /* step 11 */
            int endIndex = index;
            for (; endIndex < s.length(); endIndex++) {
                char c = s.charAt(endIndex);
                if (c > 0x7f || Character.digit(c, r) < 0) {
                    break;
                }
            }
            /* step 12 */
            if (index == endIndex) {
                throw newSyntaxError(cx, Messages.Key.InvalidNumberLiteral);
            }
            /* step 13 */
            BigInteger number = new BigInteger(s.substring(index, endIndex), r);
            /* steps 14-15 */
            return isPos ? number : number.negate();
        }

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
            if (bitsIndex > Integer.MAX_VALUE) {
                throw newRangeError(cx, Messages.Key.BigIntExponentTooLarge);
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
            if (bitsIndex > Integer.MAX_VALUE) {
                throw newRangeError(cx, Messages.Key.BigIntExponentTooLarge);
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
