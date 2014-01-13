/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import com.google.doubleconversion.DoubleConversion;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>Table 36 – The TypedArray Constructors
 * </ul>
 */
public enum ElementType {
    Int8(1), Uint8(1), Uint8C(1), Int16(2), Uint16(2), Int32(4), Uint32(4), Float32(4), Float64(8);

    private int size;

    private ElementType(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

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
            throw new IllegalStateException();
        }
    }

    public static byte ToInt8(double v) {
        return (byte) DoubleConversion.doubleToInt32(v);
    }

    public static byte ToUint8(double v) {
        return (byte) DoubleConversion.doubleToInt32(v);
    }

    public static byte ToUint8Clamp(double v) {
        v = Math.rint(v);
        return (byte) (v >= 0 ? v <= 255 ? v : 255 : 0);
    }

    public static short ToInt16(double v) {
        return (short) DoubleConversion.doubleToInt32(v);
    }

    public static short ToUint16(double v) {
        return (short) DoubleConversion.doubleToInt32(v);
    }

    public static int ToInt32(double v) {
        return (int) DoubleConversion.doubleToInt32(v);
    }

    public static int ToUint32(double v) {
        return (int) DoubleConversion.doubleToInt32(v);
    }
}
