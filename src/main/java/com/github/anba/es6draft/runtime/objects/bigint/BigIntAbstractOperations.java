/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.bigint;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitive;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import java.math.BigInteger;

import com.github.anba.es6draft.runtime.AbstractOperations.ToPrimitiveHint;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>BigInt</h1>
 */
public final class BigIntAbstractOperations {
    private BigIntAbstractOperations() {
    }

    /**
     * StringToBigInt ( argument )
     * 
     * @param argument
     *            the argument value
     * @return the BigInt result value or {@code  null} if it cannot be parsed as a BigInt
     */
    public static BigInteger StringToBigInt(CharSequence argument) {
        return ToBigIntParser.parse(argument.toString());
    }

    /**
     * 7.1.3.1 ToNumber Applied to the String Type, adjusted for BigInt.
     */
    private static final class ToBigIntParser {
        private ToBigIntParser() {
        }

        static BigInteger parse(String input) {
            String s = Strings.trim(input);
            int len = s.length();
            if (len == 0) {
                return BigInteger.ZERO;
            }
            if (s.charAt(0) == '0' && len > 2) {
                char c = s.charAt(1);
                if (c == 'x' || c == 'X') {
                    return readHexIntegerLiteral(s);
                }
                if (c == 'b' || c == 'B') {
                    return readBinaryIntegerLiteral(s);
                }
                if (c == 'o' || c == 'O') {
                    return readOctalIntegerLiteral(s);
                }
            }
            return readDecimalLiteral(s);
        }

        private static BigInteger readHexIntegerLiteral(String s) {
            assert s.length() > 2;
            final int start = 2; // "0x" prefix
            for (int index = start, end = s.length(); index < end; ++index) {
                char c = s.charAt(index);
                if (!(('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f'))) {
                    return null;
                }
            }
            return new BigInteger(s.substring(2), 16);
        }

        private static BigInteger readBinaryIntegerLiteral(String s) {
            assert s.length() > 2;
            final int start = 2; // "0b" prefix
            for (int index = start, end = s.length(); index < end; ++index) {
                char c = s.charAt(index);
                if (!(c == '0' || c == '1')) {
                    return null;
                }
            }
            return new BigInteger(s.substring(2), 2);
        }

        private static BigInteger readOctalIntegerLiteral(String s) {
            assert s.length() > 2;
            final int start = 2; // "0o" prefix
            for (int index = start, end = s.length(); index < end; ++index) {
                char c = s.charAt(index);
                if (!('0' <= c && c <= '7')) {
                    return null;
                }
            }
            return new BigInteger(s.substring(2), 8);
        }

        static BigInteger readDecimalLiteral(String s) {
            assert !s.isEmpty();
            int start = 0;
            if (s.length() > 1 && (s.charAt(0) == '+' || s.charAt(0) == '-')) {
                start += 1;
            }
            for (int index = start, end = s.length(); index < end; ++index) {
                char c = s.charAt(index);
                if (!('0' <= c && c <= '9')) {
                    return null;
                }
            }
            return new BigInteger(s, 10);
        }
    }

    /**
     * ToBigInt ( argument )
     * 
     * @param cx
     *            the execution context
     * @param argument
     *            the argument value
     * @return the BigInt result value
     */
    public static BigInteger ToBigInt(ExecutionContext cx, Object argument) {
        /* step 1 */
        Object prim = ToPrimitive(cx, argument, ToPrimitiveHint.Number);
        /* step 2 */
        switch (Type.of(prim)) {
        case Undefined:
        case Null:
            throw newTypeError(cx, Messages.Key.BigIntFromUndefinedOrNull);
        case Boolean:
            return Type.booleanValue(prim) ? BigInteger.ONE : BigInteger.ZERO;
        case Number:
            throw newTypeError(cx, Messages.Key.BigIntFromNumber);
        case String: {
            BigInteger n = StringToBigInt(Type.stringValue(prim));
            if (n == null) {
                throw newSyntaxError(cx, Messages.Key.BigIntFromInvalidString);
            }
            return n;
        }
        case Symbol:
            throw newTypeError(cx, Messages.Key.BigIntFromSymbol);
        case SIMD:
            throw newTypeError(cx, Messages.Key.BigIntFromSIMD);
        case BigInt:
            return Type.bigIntValue(prim);
        case Object:
        default:
            throw new AssertionError();
        }
    }

    private static final BigInteger TWO_64 = BigInteger.valueOf(2).pow(64);
    private static final BigInteger TWO_63 = BigInteger.valueOf(2).pow(63);

    /**
     * ToBigInt64 ( argument )
     * 
     * @param cx
     *            the execution context
     * @param argument
     *            the argument value
     * @return the BigInt result value
     */
    public static BigInteger ToBigInt64(ExecutionContext cx, Object argument) {
        // TODO: spec issue - maybe assert argument is a number/bigint?
        /* step 1 */
        BigInteger n = ToBigInt(cx, argument);
        /* step 2 */
        BigInteger int64bit = n.mod(TWO_64);
        /* step 3 */
        // TODO: Directly return long value.
        return int64bit.compareTo(TWO_64) >= 0 ? int64bit.subtract(TWO_63) : int64bit;
    }

    /**
     * ToBigInt64 ( argument )
     * 
     * @param n
     *            the argument value
     * @return the {@code long} result value
     */
    public static long ToBigInt64(BigInteger n) {
        /* step 1 (not applicable) */
        /* step 2 */
        BigInteger int64bit = n.mod(TWO_64);
        /* step 3 */
        return int64bit.compareTo(TWO_64) >= 0 ? int64bit.subtract(TWO_63).longValue() : int64bit.longValue();
    }

    /**
     * ToBigUint64 ( argument )
     * 
     * @param cx
     *            the execution context
     * @param argument
     *            the argument value
     * @return the BigInt result value
     */
    public static BigInteger ToBigUint64(ExecutionContext cx, Object argument) {
        // TODO: spec issue - maybe assert argument is a number/bigint?
        /* step 1 */
        BigInteger n = ToBigInt(cx, argument);
        /* step 2 */
        BigInteger int64bit = n.mod(TWO_64);
        /* step 3 */
        // TODO: Directly return long value.
        return int64bit;
    }

    /**
     * ToBigUint64 ( argument )
     * 
     * @param n
     *            the argument value
     * @return the {@code long} result value
     */
    public static long ToBigUint64(BigInteger n) {
        /* step 1 (not applicable) */
        /* step 2 */
        BigInteger int64bit = n.mod(TWO_64);
        /* step 3 */
        return int64bit.longValue();
    }

    /**
     * IsSafeInteger ( number )
     * 
     * @param number
     *            the number value
     * @return {@code true} if the argument is a safe integer
     */
    public static boolean IsSafeInteger(double number) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            return false;
        }
        /* steps 3-4 */
        if (Math.rint(number) != number) {
            return false;
        }
        /* steps 5-6 */
        return Math.abs(number) <= 0x1F_FFFF_FFFF_FFFFp0;
    }

    /**
     * NumberToBigInt ( number )
     * 
     * @param cx
     *            the execution context
     * @param number
     *            the number value
     * @return the BigInt result value
     */
    public static BigInteger NumberToBigInt(ExecutionContext cx, double number) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (!IsSafeInteger(number)) {
            throw newRangeError(cx, Messages.Key.BigIntFromInvalidNumber);
        }
        /* step 3 */
        return BigInteger.valueOf((long) Math.rint(number));
    }
}
