/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.bigint;

import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import java.math.BigInteger;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * <h1>BigInt</h1>
 */
public final class BigIntType {
    private BigIntType() {
    }

    private static final BigInteger TWO_64 = BigInteger.valueOf(2).pow(64);

    public static BigInteger toUnsigned64(long value) {
        BigInteger n = BigInteger.valueOf(value);
        return n.signum() >= 0 ? n : n.add(TWO_64);
    }

    public static BigInteger toUnsigned64(BigInteger n) {
        return n.signum() >= 0 ? n : n.add(TWO_64);
    }

    /**
     * BigInt::unit value is 1n.
     */
    public static final BigInteger UNIT = BigInteger.ONE;

    /**
     * BigInt::unaryMinus (x)
     * 
     * @param x
     *            the BigInt value
     * @return the negated value of {@code x}
     */
    public static BigInteger unaryMinus(BigInteger x) {
        return x.negate();
    }

    /**
     * BigInt::bitwiseNOT (x)
     * 
     * @param x
     *            the BigInt value
     * @return the one's complement of {@code x}
     */
    public static BigInteger bitwiseNOT(BigInteger x) {
        return x.not();
    }

    /**
     * BigInt::exponentiate (base, exponent)
     * 
     * @param cx
     *            the execution context
     * @param base
     *            the base value
     * @param exponent
     *            the exponent value
     * @return the value of {@code base} raised to the power {@code exponent}
     */
    public static BigInteger exponentiate(ExecutionContext cx, BigInteger base, BigInteger exponent) {
        /* step 1 */
        if (exponent.signum() < 0) {
            throw newRangeError(cx, Messages.Key.BigIntNegativeExponent);
        }
        /* step 2 */
        if (base.signum() == 0 && exponent.signum() == 0) {
            return BigInteger.ONE;
        }
        /* step 3 */
        int exponentAsInt;
        try {
            exponentAsInt = exponent.intValueExact();
        } catch (ArithmeticException e) {
            // Exponent too large for BigInteger.
            throw newRangeError(cx, Messages.Key.BigIntExponentTooLarge);
        }
        return base.pow(exponentAsInt);
    }

    /**
     * BigInt::multiply (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of multiplying {@code x} and {@code y}
     */
    public static BigInteger multiply(BigInteger x, BigInteger y) {
        return x.multiply(y);
    }

    /**
     * BigInt::divide (x, y)
     * 
     * @param cx
     *            the execution context
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of dividing {@code x} by {@code y}
     */
    public static BigInteger divide(ExecutionContext cx, BigInteger x, BigInteger y) {
        /* step 1 */
        if (y.signum() == 0) {
            throw newRangeError(cx, Messages.Key.BigIntDivideByZero);
        }
        /* steps 2-3 */
        return x.divide(y);
    }

    /**
     * BigInt::remainder (x, y)
     * 
     * @param cx
     *            the execution context
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the remainder of dividing {@code x} by {@code y}
     */
    public static BigInteger remainder(ExecutionContext cx, BigInteger x, BigInteger y) {
        /* step 1 */
        if (y.signum() == 0) {
            throw newRangeError(cx, Messages.Key.BigIntDivideByZero);
        }
        /* steps 2-3 */
        return x.remainder(y);
    }

    /**
     * BigInt::add (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of adding {@code x} and {@code y}
     */
    public static BigInteger add(BigInteger x, BigInteger y) {
        return x.add(y);
    }

    /**
     * BigInt::subtract (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of subtracting {@code y} from {@code x}
     */
    public static BigInteger subtract(BigInteger x, BigInteger y) {
        return x.subtract(y);
    }

    /**
     * BigInt::leftShift (x, y)
     * 
     * @param cx
     *            the execution context
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of left-shifting {@code x} by {@code y}
     */
    public static BigInteger leftShift(ExecutionContext cx, BigInteger x, BigInteger y) {
        // TODO: Special case x==0n to allow arbitrary large y?
        int yAsInt;
        try {
            yAsInt = y.intValueExact();
        } catch (ArithmeticException e) {
            // Exponent too large for BigInteger.
            throw newRangeError(cx, Messages.Key.BigIntExponentTooLarge);
        }
        return x.shiftLeft(yAsInt);
    }

    /**
     * BigInt::signedRightShift (x, y)
     * 
     * @param cx
     *            the execution context
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of right-shifting {@code x} by {@code y}
     */
    public static BigInteger signedRightShift(ExecutionContext cx, BigInteger x, BigInteger y) {
        // TODO: Special case x==0n to allow arbitrary large y?
        int yAsInt;
        try {
            yAsInt = y.intValueExact();
        } catch (ArithmeticException e) {
            // Exponent too large for BigInteger.
            throw newRangeError(cx, Messages.Key.BigIntExponentTooLarge);
        }
        return x.shiftRight(yAsInt);
    }

    /**
     * BigInt::unsignedRightShift (x, y)
     * 
     * @param cx
     *            the execution context
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return unconditionally throws a {@code TypeError}
     */
    public static BigInteger unsignedRightShift(ExecutionContext cx, BigInteger x, BigInteger y) {
        throw newTypeError(cx, Messages.Key.BigIntUnsignedRightShift);
    }

    /**
     * BigInt::lessThan (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return {@code true} if {@code x} is smaller than {@code y}
     */
    public static boolean lessThan(BigInteger x, BigInteger y) {
        return x.compareTo(y) < 0;
    }

    /**
     * BigInt::equal (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return {@code true} if {@code x} is equal to {@code y}
     */
    public static boolean equal(BigInteger x, BigInteger y) {
        return x.equals(y);
    }

    /**
     * BigInt::sameValue (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return {@code true} if {@code x} is equal to {@code y}
     */
    public static boolean sameValue(BigInteger x, BigInteger y) {
        return x.equals(y);
    }

    /**
     * BigInt::sameValueZero (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return {@code true} if {@code x} is equal to {@code y}
     */
    public static boolean sameValueZero(BigInteger x, BigInteger y) {
        return x.equals(y);
    }

    /**
     * BigInt::bitwiseAND (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of bitwise and-ing {@code x} and {@code y}
     */
    public static BigInteger bitwiseAND(BigInteger x, BigInteger y) {
        return x.and(y);
    }

    /**
     * BigInt::bitwiseXOR (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of bitwise xor-ing {@code x} and {@code y}
     */
    public static BigInteger bitwiseXOR(BigInteger x, BigInteger y) {
        return x.xor(y);
    }

    /**
     * BigInt::bitwiseOR (x, y)
     * 
     * @param x
     *            the x value
     * @param y
     *            the y value
     * @return the result of bitwise or-ing {@code x} and {@code y}
     */
    public static BigInteger bitwiseOR(BigInteger x, BigInteger y) {
        return x.or(y);
    }
}
