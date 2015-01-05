/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import com.github.anba.es6draft.runtime.types.Intrinsics;
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
     * Single precision 32-bit float type
     */
    Float32(4),

    /**
     * Double precision 64-bit float type
     */
    Float64(8);

    private int size;

    private ElementType(int size) {
        this.size = size;
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
    public static byte ToUint8Clamp(double v) {
        v = Math.rint(v);
        return (byte) (v >= 0 ? v <= 255 ? v : 255 : 0);
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
    public static int ToUint32(double v) {
        return (int) DoubleConversion.doubleToInt32(v);
    }
}
