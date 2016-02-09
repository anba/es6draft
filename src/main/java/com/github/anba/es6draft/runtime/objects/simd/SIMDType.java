/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.simd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <h1>SIMD</h1>
 * <h2>SIMD objects</h2>
 * <ul>
 * <li>SIMD type descriptors
 * </ul>
 */
public enum SIMDType {
    /**
     * Float64x2
     */
    Float64x2(2, 8),

    /**
     * Float32x4
     */
    Float32x4(4, 4),

    /**
     * Int32x4
     */
    Int32x4(4, 4),

    /**
     * Int16x8
     */
    Int16x8(8, 2),

    /**
     * Int8x16
     */
    Int8x16(16, 1),

    /**
     * Uint32x4
     */
    Uint32x4(4, 4),

    /**
     * Uint16x8
     */
    Uint16x8(8, 2),

    /**
     * Uint8x16
     */
    Uint8x16(16, 1),

    /**
     * Bool64x2
     */
    Bool64x2(2, 0),

    /**
     * Bool32x4
     */
    Bool32x4(4, 0),

    /**
     * Bool16x8
     */
    Bool16x8(8, 0),

    /**
     * Bool8x16
     */
    Bool8x16(16, 0);

    private final int vectorLength;
    private final int elementSize;

    private SIMDType(int vectorLength, int elementSize) {
        this.vectorLength = vectorLength;
        this.elementSize = elementSize;
    }

    /**
     * [[VectorLength]]
     * 
     * @return the vector length
     */
    public int getVectorLength() {
        return vectorLength;
    }

    /**
     * [[ElementSize]]
     * 
     * @return the element size
     */
    public int getElementSize() {
        return elementSize;
    }

    /**
     * Returns the {@code typeof} description for this SIMD type.
     * 
     * @return the {@code typeof} description
     */
    public String typeof() {
        switch (this) {
        case Float64x2:
            return "float64x2";
        case Float32x4:
            return "float32x4";
        case Int32x4:
            return "int32x4";
        case Int16x8:
            return "int16x8";
        case Int8x16:
            return "int8x16";
        case Uint32x4:
            return "uint32x4";
        case Uint16x8:
            return "uint16x8";
        case Uint8x16:
            return "uint8x16";
        case Bool64x2:
            return "bool64x2";
        case Bool32x4:
            return "bool32x4";
        case Bool16x8:
            return "bool16x8";
        case Bool8x16:
            return "bool8x16";
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns the SIMD type for a {@code typeof} name.
     * 
     * @param typeName
     *            the SIMD typeof name
     * @return the SIMD type
     */
    public static SIMDType from(String typeName) {
        switch (typeName) {
        case "float64x2":
            return Float64x2;
        case "float32x4":
            return Float32x4;
        case "int32x4":
            return Int32x4;
        case "int16x8":
            return Int16x8;
        case "int8x16":
            return Int8x16;
        case "uint32x4":
            return Uint32x4;
        case "uint16x8":
            return Uint16x8;
        case "uint8x16":
            return Uint8x16;
        case "bool64x2":
            return Bool64x2;
        case "bool32x4":
            return Bool32x4;
        case "bool16x8":
            return Bool16x8;
        case "bool8x16":
            return Bool8x16;
        default:
            throw new IllegalArgumentException(typeName);
        }
    }

    /**
     * Returns {@code true} for floating point SIMD types.
     * 
     * @return {@code true} if the {@code this} object describes a floating point SIMD type.
     */
    public boolean isFloatingPoint() {
        switch (this) {
        case Float64x2:
        case Float32x4:
            return true;
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
        case Int32x4:
        case Int16x8:
        case Int8x16:
        case Uint32x4:
        case Uint16x8:
        case Uint8x16:
            return false;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns {@code true} for integer SIMD types.
     * 
     * @return {@code true} if the {@code this} object describes an integer SIMD type.
     */
    public boolean isInteger() {
        switch (this) {
        case Int32x4:
        case Int16x8:
        case Int8x16:
        case Uint32x4:
        case Uint16x8:
        case Uint8x16:
            return true;
        case Float64x2:
        case Float32x4:
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
            return false;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns {@code true} for boolean SIMD types.
     * 
     * @return {@code true} if the {@code this} object describes a boolean SIMD type.
     */
    public boolean isBoolean() {
        switch (this) {
        case Bool64x2:
        case Bool32x4:
        case Bool16x8:
        case Bool8x16:
            return true;
        case Float64x2:
        case Float32x4:
        case Int32x4:
        case Int16x8:
        case Int8x16:
        case Uint32x4:
        case Uint16x8:
        case Uint8x16:
            return false;
        default:
            throw new AssertionError();
        }
    }

    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private static ByteBuffer byteBuffer(ByteBuffer block, boolean isLittleEndian) {
        // NB: Byte order is not reset after this call.
        if ((block.order() == ByteOrder.LITTLE_ENDIAN) != isLittleEndian) {
            block.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        }
        return block;
    }

    /**
     * SerializeFloat64( block, offset, value, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeFloat64(ByteBuffer block, int offset, double value) {
        SerializeFloat64(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeFloat64( block, offset, value, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeFloat64(ByteBuffer block, int offset, double value, boolean isLittleEndian) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        assert 0 <= offset && offset + 8 <= block.capacity();
        /* steps 5-6 */
        byteBuffer(block, isLittleEndian).putDouble(offset, value);
    }

    /**
     * DeserializeFloat64( block, offset, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static double DeserializeFloat64(ByteBuffer block, int offset) {
        return DeserializeFloat64(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeFloat64( block, offset, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return value the number value
     */
    public static double DeserializeFloat64(ByteBuffer block, int offset, boolean isLittleEndian) {
        // FIXME: spec bug - isLittleEndian never used as argument to [[DeserializeElement]]
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 8 <= block.capacity();
        /* steps 4-6 */
        double value = byteBuffer(block, isLittleEndian).getDouble(offset);
        /* steps 7-8 */
        // FIXME: spec issue? - canonicalization breaks moz-tests
        // return Double.isNaN(value) ? Double.NaN : value;
        return value;
    }

    /**
     * SerializeFloat32( block, offset, value, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeFloat32(ByteBuffer block, int offset, double value) {
        SerializeFloat32(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeFloat32( block, offset, value, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeFloat32(ByteBuffer block, int offset, double value, boolean isLittleEndian) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        assert 0 <= offset && offset + 4 <= block.capacity();
        /* steps 5-6 */
        byteBuffer(block, isLittleEndian).putFloat(offset, (float) value);
    }

    /**
     * DeserializeFloat32( block, offset, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static double DeserializeFloat32(ByteBuffer block, int offset) {
        return DeserializeFloat32(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeFloat32( block, offset, isLittleEndian )
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return value the number value
     */
    public static double DeserializeFloat32(ByteBuffer block, int offset, boolean isLittleEndian) {
        // FIXME: spec bug - isLittleEndian never used as argument to [[DeserializeElement]]
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 4 <= block.capacity();
        /* steps 4-6 */
        float value = byteBuffer(block, isLittleEndian).getFloat(offset);
        /* steps 7-8 */
        // FIXME: spec issue? - canonicalization breaks moz-tests
        // return Float.isNaN(value) ? Float.NaN : value;
        return value;
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Int32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeInt32(ByteBuffer block, int offset, int value) {
        SerializeInt32(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Int32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeInt32(ByteBuffer block, int offset, int value, boolean isLittleEndian) {
        /* steps 1-4 (not applicable) */
        /* step 5 */
        assert 0 <= offset && offset + 4 <= block.capacity();
        /* steps 6-7 */
        byteBuffer(block, isLittleEndian).putInt(offset, value);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Int32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static int DeserializeInt32(ByteBuffer block, int offset) {
        return DeserializeInt32(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Int32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return the number value
     */
    public static int DeserializeInt32(ByteBuffer block, int offset, boolean isLittleEndian) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 4 <= block.capacity();
        /* steps 4-7 */
        return byteBuffer(block, isLittleEndian).getInt(offset);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Int16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeInt16(ByteBuffer block, int offset, int value) {
        SerializeInt16(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Int16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeInt16(ByteBuffer block, int offset, int value, boolean isLittleEndian) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        assert value == (short) value;
        /* step 5 */
        assert 0 <= offset && offset + 2 <= block.capacity();
        /* steps 6-7 */
        byteBuffer(block, isLittleEndian).putShort(offset, (short) value);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Int16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static int DeserializeInt16(ByteBuffer block, int offset) {
        return DeserializeInt16(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Int16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return the number value
     */
    public static int DeserializeInt16(ByteBuffer block, int offset, boolean isLittleEndian) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 2 <= block.capacity();
        /* steps 4-7 */
        return byteBuffer(block, isLittleEndian).getShort(offset);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Int8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeInt8(ByteBuffer block, int offset, int value) {
        SerializeInt8(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Int8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeInt8(ByteBuffer block, int offset, int value, boolean isLittleEndian) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        assert value == (byte) value;
        /* step 5 */
        assert 0 <= offset && offset + 1 <= block.capacity();
        /* steps 6-7 */
        block.put(offset, (byte) value);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Int8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static int DeserializeInt8(ByteBuffer block, int offset) {
        return DeserializeInt8(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Int8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return the number value
     */
    public static int DeserializeInt8(ByteBuffer block, int offset, boolean isLittleEndian) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 1 <= block.capacity();
        /* steps 4-7 */
        return block.get(offset);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Uint32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeUint32(ByteBuffer block, int offset, int value) {
        SerializeUint32(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Uint32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeUint32(ByteBuffer block, int offset, int value, boolean isLittleEndian) {
        /* steps 1-4 (not applicable) */
        /* step 5 */
        assert 0 <= offset && offset + 4 <= block.capacity();
        /* steps 6-7 */
        byteBuffer(block, isLittleEndian).putInt(offset, value);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Uint32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static int DeserializeUint32(ByteBuffer block, int offset) {
        return DeserializeUint32(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Uint32x4
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return the number value
     */
    public static int DeserializeUint32(ByteBuffer block, int offset, boolean isLittleEndian) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 4 <= block.capacity();
        /* steps 4-7 */
        return byteBuffer(block, isLittleEndian).getInt(offset); // Value returned as signed int32.
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Uint16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeUint16(ByteBuffer block, int offset, int value) {
        SerializeUint16(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Uint16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeUint16(ByteBuffer block, int offset, int value, boolean isLittleEndian) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        assert value == (value & 0xffff);
        /* step 5 */
        assert 0 <= offset && offset + 2 <= block.capacity();
        /* steps 6-7 */
        byteBuffer(block, isLittleEndian).putShort(offset, (short) value);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Uint16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static int DeserializeUint16(ByteBuffer block, int offset) {
        return DeserializeUint16(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Uint16x8
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return the number value
     */
    public static int DeserializeUint16(ByteBuffer block, int offset, boolean isLittleEndian) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 2 <= block.capacity();
        /* steps 4-7 */
        return byteBuffer(block, isLittleEndian).getShort(offset) & 0xffff;
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Uint8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     */
    public static void SerializeUint8(ByteBuffer block, int offset, int value) {
        SerializeUint8(block, offset, value, IS_LITTLE_ENDIAN);
    }

    /**
     * SerializeInt( descriptor )( block, offset, n, isLittleEndian ), descriptor = Uint8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param value
     *            the number value
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     */
    public static void SerializeUint8(ByteBuffer block, int offset, int value, boolean isLittleEndian) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        assert value == (value & 0xff);
        /* step 5 */
        assert 0 <= offset && offset + 1 <= block.capacity();
        /* steps 6-7 */
        block.put(offset, (byte) value);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Uint8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @return the number value
     */
    public static int DeserializeUint8(ByteBuffer block, int offset) {
        return DeserializeUint8(block, offset, IS_LITTLE_ENDIAN);
    }

    /**
     * DeserializeInt( descriptor )( block, offset, isLittleEndian ), descriptor = Uint8x16
     * 
     * @param block
     *            the data block
     * @param offset
     *            the data block offset
     * @param isLittleEndian
     *            {@code true} if little endian order, otherwise big endian order
     * @return the number value
     */
    public static int DeserializeUint8(ByteBuffer block, int offset, boolean isLittleEndian) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert 0 <= offset && offset + 1 <= block.capacity();
        /* steps 4-7 */
        return block.get(offset) & 0xff;
    }
}
