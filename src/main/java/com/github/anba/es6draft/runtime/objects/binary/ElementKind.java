/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import com.google.doubleconversion.DoubleConversion;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.6 TypedArray Object Structures</h3>
 * <ul>
 * <li>Table 36 – The TypedArray Constructors
 * </ul>
 */
public enum ElementKind {
    Int8(1), Uint8(1), Uint8C(1), Int16(2), Uint16(2), Int32(4), Uint32(4), Float32(4), Float64(8);

    private int size;

    private ElementKind(int size) {
        this.size = size;
    }

    public int size() {
        return size;
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
