/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.bigint.BigIntAbstractOperations.ToBigInt;

import java.math.BigInteger;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntAbstractOperations;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.google.doubleconversion.DoubleConversion;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>Table 36 – The TypedArray Constructors
 * </ul>
 */
public enum ElementType {
    /**
     * Signed 8-bit integer type
     */
    Int8(1),

    /**
     * Unsigned 8-bit integer type
     */
    Uint8(1),

    /**
     * Unsigned 8-bit integer type (clamped)
     */
    Uint8C(1),

    /**
     * Signed 16-bit integer type
     */
    Int16(2),

    /**
     * Unsigned 16-bit integer type
     */
    Uint16(2),

    /**
     * Signed 32-bit integer type
     */
    Int32(4),

    /**
     * Unsigned 32-bit integer type
     */
    Uint32(4),

    /**
     * Signed 64-bit integer type
     */
    BigInt64(8),

    /**
     * Unsigned 64-bit integer type
     */
    BigUint64(8),

    /**
     * Single precision 32-bit float type
     */
    Float32(4),

    /**
     * Double precision 64-bit float type
     */
    Float64(8);

    private final int size;
    private final int shift;

    private ElementType(int size) {
        assert (size & (size - 1)) == 0;
        this.size = size;
        this.shift = 31 - Integer.numberOfLeadingZeros(size);
    }

    /**
     * Returns the byte size for the element type.
     * 
     * @return the element's byte size
     */
    public int size() {
        return size;
    }

    /**
     * Returns the byte shift for the element type.
     * 
     * @return the element's byte shift
     */
    public int shift() {
        return shift;
    }

    /**
     * Returns the byte offset for a relative index.
     * 
     * @param index
     *            the relative index
     * @return the index as a byte offset
     */
    public int toBytes(int index) {
        return index << shift;
    }

    /**
     * Returns the byte offset for a relative index.
     * 
     * @param index
     *            the relative index
     * @return the index as a byte offset
     */
    public long toBytes(long index) {
        return index << shift;
    }

    /**
     * Returns {@code true} if this element type represents a 64-bit integer.
     * 
     * @return {@code true} if this element type represents a 64-bit integer
     */
    public boolean isInt64() {
        return this == BigInt64 || this == BigUint64;
    }

    /**
     * Returns {@code true} if the given element type is compatible to this element type.
     * 
     * @param sourceType
     *            the source element type
     * @return {@code true} if the given value is compatible to this element type
     */
    public boolean isCompatibleNumericType(ElementType sourceType) {
        return this == sourceType || (isInt64() == sourceType.isInt64());
    }

    /**
     * Throws a {@code TypeError} if the given numeric value is not compatible for this element type.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the numeric type
     */
    public void throwIfIncompatibleNumericType(ExecutionContext cx, Number value) {
        assert Type.isNumber(value) || Type.isBigInt(value);
        if (Type.isNumber(value) != !isInt64()) {
            throw newTypeError(cx, Type.isNumber(value) ? Messages.Key.BigIntFromNumber : Messages.Key.BigIntNumber);
        }
    }

    /**
     * Throws a {@code TypeError} if the given element type is not compatible for this element type.
     * 
     * @param cx
     *            the execution context
     * @param sourceType
     *            the source element type
     */
    public void throwIfIncompatibleNumericType(ExecutionContext cx, ElementType sourceType) {
        if (!isCompatibleNumericType(sourceType)) {
            throw newTypeError(cx, isInt64() ? Messages.Key.BigIntFromNumber : Messages.Key.BigIntNumber);
        }
    }

    /**
     * Converts {@code value} to the numeric value of this element type.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the argument value
     * @return the numeric value
     */
    public Number toElementValue(ExecutionContext cx, Object value) {
        if (!isInt64()) {
            return ToNumber(cx, value);
        }
        return ToBigInt(cx, value);
    }

    /**
     * Returns the constructor name for the element type.
     * 
     * @return the constructor name
     */
    public String getConstructorName() {
        switch (this) {
        case Int8:
            return "Int8Array";
        case Uint8:
            return "Uint8Array";
        case Uint8C:
            return "Uint8ClampedArray";
        case Int16:
            return "Int16Array";
        case Uint16:
            return "Uint16Array";
        case Int32:
            return "Int32Array";
        case Uint32:
            return "Uint32Array";
        case BigInt64:
            return "BigInt64Array";
        case BigUint64:
            return "BigUint64Array";
        case Float32:
            return "Float32Array";
        case Float64:
            return "Float64Array";
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns the constructor for the element type.
     * 
     * @return the constructor intrinsic
     */
    public Intrinsics getConstructor() {
        switch (this) {
        case Int8:
            return Intrinsics.Int8Array;
        case Uint8:
            return Intrinsics.Uint8Array;
        case Uint8C:
            return Intrinsics.Uint8ClampedArray;
        case Int16:
            return Intrinsics.Int16Array;
        case Uint16:
            return Intrinsics.Uint16Array;
        case Int32:
            return Intrinsics.Int32Array;
        case Uint32:
            return Intrinsics.Uint32Array;
        case BigInt64:
            return Intrinsics.BigInt64Array;
        case BigUint64:
            return Intrinsics.BigUint64Array;
        case Float32:
            return Intrinsics.Float32Array;
        case Float64:
            return Intrinsics.Float64Array;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Converts the input value to a signed 8-bit integer.
     * 
     * @param v
     *            the input value
     * @return the signed 8-bit integer
     */
    public static byte ToInt8(float v) {
        return (byte) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to a signed 8-bit integer.
     * 
     * @param v
     *            the input value
     * @return the signed 8-bit integer
     */
    public static byte ToInt8(double v) {
        return (byte) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to an unsigned 8-bit integer.
     * 
     * @param v
     *            the input value
     * @return the unsigned 8-bit integer
     */
    public static byte ToUint8(float v) {
        return (byte) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to an unsigned 8-bit integer.
     * 
     * @param v
     *            the input value
     * @return the unsigned 8-bit integer
     */
    public static byte ToUint8(double v) {
        return (byte) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to an unsigned 8-bit integer (clamped).
     * 
     * @param v
     *            the input value
     * @return the unsigned 8-bit integer (clamped)
     */
    public static byte ToUint8Clamp(int v) {
        return (byte) Math.min(Math.max(v, 0), 255);
    }

    /**
     * Converts the input value to an unsigned 8-bit integer (clamped).
     * 
     * @param v
     *            the input value
     * @return the unsigned 8-bit integer (clamped)
     */
    public static byte ToUint8ClampUnsigned(int v) {
        return (byte) (v >= 0 && v <= 255 ? v : 255);
    }

    /**
     * Converts the input value to an unsigned 8-bit integer (clamped).
     * 
     * @param v
     *            the input value
     * @return the unsigned 8-bit integer (clamped)
     */
    public static byte ToUint8Clamp(float v) {
        return (byte) AbstractOperations.ToUint8Clamp(v);
    }

    /**
     * Converts the input value to an unsigned 8-bit integer (clamped).
     * 
     * @param v
     *            the input value
     * @return the unsigned 8-bit integer (clamped)
     */
    public static byte ToUint8Clamp(double v) {
        return (byte) AbstractOperations.ToUint8Clamp(v);
    }

    /**
     * Converts the input value to a signed 16-bit integer.
     * 
     * @param v
     *            the input value
     * @return the signed 16-bit integer
     */
    public static short ToInt16(float v) {
        return (short) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to a signed 16-bit integer.
     * 
     * @param v
     *            the input value
     * @return the signed 16-bit integer
     */
    public static short ToInt16(double v) {
        return (short) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to an unsigned 16-bit integer.
     * 
     * @param v
     *            the input value
     * @return the unsigned 16-bit integer
     */
    public static short ToUint16(float v) {
        return (short) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to an unsigned 16-bit integer.
     * 
     * @param v
     *            the input value
     * @return the unsigned 16-bit integer
     */
    public static short ToUint16(double v) {
        return (short) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to a signed 32-bit integer.
     * 
     * @param v
     *            the input value
     * @return the signed 32-bit integer
     */
    public static int ToInt32(float v) {
        return (int) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to a signed 32-bit integer.
     * 
     * @param v
     *            the input value
     * @return the signed 32-bit integer
     */
    public static int ToInt32(double v) {
        return (int) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to an unsigned 32-bit integer.
     * 
     * @param v
     *            the input value
     * @return the unsigned 32-bit integer
     */
    public static int ToUint32(float v) {
        return (int) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to an unsigned 32-bit integer.
     * 
     * @param v
     *            the input value
     * @return the unsigned 32-bit integer
     */
    public static int ToUint32(double v) {
        return (int) DoubleConversion.doubleToInt32(v);
    }

    /**
     * Converts the input value to a signed 64-bit integer.
     * 
     * @param v
     *            the input value
     * @return the signed 64-bit integer
     */
    public static long ToBigInt64(BigInteger v) {
        return BigIntAbstractOperations.ToBigInt64(v);
    }

    /**
     * Converts the input value to an unsigned 64-bit integer.
     * 
     * @param v
     *            the input value
     * @return the unsigned 64-bit integer
     */
    public static long ToBigUint64(BigInteger v) {
        return BigIntAbstractOperations.ToBigUint64(v);
    }
}
