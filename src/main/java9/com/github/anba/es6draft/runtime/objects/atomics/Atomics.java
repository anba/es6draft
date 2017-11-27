/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.util.function.IntBinaryOperator;

import com.github.anba.es6draft.runtime.internal.Bytes;

/**
 * 
 */
final class Atomics {
    private Atomics() {
    }

    private static final VarHandle vhByteArray = MethodHandles.arrayElementVarHandle(byte[].class);
    private static final VarHandle vhShort = MethodHandles.byteBufferViewVarHandle(short[].class,
            Bytes.DEFAULT_BYTE_ORDER);
    private static final VarHandle vhInt = MethodHandles.byteBufferViewVarHandle(int[].class, Bytes.DEFAULT_BYTE_ORDER);
    private static final VarHandle vhLong = MethodHandles.byteBufferViewVarHandle(long[].class,
            Bytes.DEFAULT_BYTE_ORDER);
    private static final VarHandle vhIntNative = MethodHandles.byteBufferViewVarHandle(int[].class,
            ByteOrder.nativeOrder());
    private static final boolean IS_NATIVE_BYTE_ORDER = Bytes.DEFAULT_BYTE_ORDER == ByteOrder.nativeOrder();
    private static final boolean LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private static void checkIndexAndAccess(ByteBuffer buffer, int index, int size) {
        if ((index & (size - 1)) != 0) {
            throw new IllegalArgumentException("Misaligned access");
        }
        if (index < 0 || size > buffer.limit() - index) {
            throw new IndexOutOfBoundsException(indexOutOfBoundsMessage(buffer, index, size));
        }
        if (buffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (buffer.alignmentOffset(index, size) != 0) {
            throw new IllegalArgumentException();
        }
    }

    private static String indexOutOfBoundsMessage(ByteBuffer buffer, int index, int size) {
        return String.format("limit=%d, index=%d, size=%d", buffer.limit(), index, size);
    }

    /**
     * Performs a full memory fence.
     * 
     * @see VarHandle#fullFence()
     */
    static void fullFence() {
        VarHandle.fullFence();
    }

    /**
     * Atomically performs an addition.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the increment value
     * @return the previous value
     */
    static byte getAndAdd(ByteBuffer buffer, int index, byte value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r + v);
    }

    /**
     * Atomically performs an addition.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the increment value
     * @return the previous value
     */
    static short getAndAdd(ByteBuffer buffer, int index, short value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r + v);
    }

    /**
     * Atomically performs an addition.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the increment value
     * @return the previous value
     * @see VarHandle#getAndAdd(Object...)
     */
    static int getAndAdd(ByteBuffer buffer, int index, int value) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.getAndAdd(buffer, index, value);
    }

    /**
     * Atomically performs an addition.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the increment value
     * @return the previous value
     * @see VarHandle#getAndAdd(Object...)
     */
    static long getAndAdd(ByteBuffer buffer, int index, long value) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.getAndAdd(buffer, index, value);
    }

    /**
     * Atomically performs a subtraction.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the decrement value
     * @return the previous value
     */
    static byte getAndSub(ByteBuffer buffer, int index, byte value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r - v);
    }

    /**
     * Atomically performs a subtraction.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the decrement value
     * @return the previous value
     */
    static short getAndSub(ByteBuffer buffer, int index, short value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r - v);
    }

    /**
     * Atomically performs a subtraction.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the decrement value
     * @return the previous value
     * @see VarHandle#getAndAdd(Object...)
     */
    static int getAndSub(ByteBuffer buffer, int index, int value) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.getAndAdd(buffer, index, -value);
    }

    /**
     * Atomically performs a subtraction.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the decrement value
     * @return the previous value
     * @see VarHandle#getAndAdd(Object...)
     */
    static long getAndSub(ByteBuffer buffer, int index, long value) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.getAndAdd(buffer, index, -value);
    }

    /**
     * Atomically performs a bit-and operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static byte getAndBitwiseAnd(ByteBuffer buffer, int index, byte value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r & v);
    }

    /**
     * Atomically performs a bit-and operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static short getAndBitwiseAnd(ByteBuffer buffer, int index, short value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r & v);
    }

    /**
     * Atomically performs a bit-and operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static int getAndBitwiseAnd(ByteBuffer buffer, int index, int value) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.getAndBitwiseAnd(buffer, index, value);
    }

    /**
     * Atomically performs a bit-and operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static long getAndBitwiseAnd(ByteBuffer buffer, int index, long value) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.getAndBitwiseAnd(buffer, index, value);
    }

    /**
     * Atomically performs a bit-or operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static byte getAndBitwiseOr(ByteBuffer buffer, int index, byte value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r | v);
    }

    /**
     * Atomically performs a bit-or operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static short getAndBitwiseOr(ByteBuffer buffer, int index, short value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r | v);
    }

    /**
     * Atomically performs a bit-or operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static int getAndBitwiseOr(ByteBuffer buffer, int index, int value) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.getAndBitwiseOr(buffer, index, value);
    }

    /**
     * Atomically performs a bit-or operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static long getAndBitwiseOr(ByteBuffer buffer, int index, long value) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.getAndBitwiseOr(buffer, index, value);
    }

    /**
     * Atomically performs a bit-xor operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static byte getAndBitwiseXor(ByteBuffer buffer, int index, byte value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r ^ v);
    }

    /**
     * Atomically performs a bit-xor operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static short getAndBitwiseXor(ByteBuffer buffer, int index, short value) {
        return getAndAccumulate(buffer, index, value, (IntBinaryOperator) (r, v) -> r ^ v);
    }

    /**
     * Atomically performs a bit-xor operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static int getAndBitwiseXor(ByteBuffer buffer, int index, int value) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.getAndBitwiseXor(buffer, index, value);
    }

    /**
     * Atomically performs a bit-xor operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the operand value
     * @return the previous value
     */
    static long getAndBitwiseXor(ByteBuffer buffer, int index, long value) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.getAndBitwiseXor(buffer, index, value);
    }

    /**
     * Atomically performs the supplied update operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the update value
     * @param op
     *            the update operation
     * @return the previous value
     */
    private static byte getAndAccumulate(ByteBuffer buffer, int index, byte value, IntBinaryOperator op) {
        final int byteIndex = index & ~0b11;
        final int shift = 8 * (LE ? (index & 0b11) : (3 - (index & 0b11)));
        final int mask = ~(0xff << shift);

        checkIndexAndAccess(buffer, byteIndex, Integer.BYTES);
        int current, newValue, result;
        do {
            current = (int) vhIntNative.getVolatile(buffer, byteIndex);
            result = 0xff & op.applyAsInt((current >>> shift) & 0xff, value);
            newValue = (current & mask) | (result << shift);
        } while (!vhIntNative.weakCompareAndSet(buffer, byteIndex, current, newValue));
        return (byte) (current >>> shift);
    }

    /**
     * Atomically performs the supplied update operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the update value
     * @param op
     *            the update operation
     * @return the previous value
     */
    private static short getAndAccumulate(ByteBuffer buffer, int index, short value, IntBinaryOperator op) {
        final int byteIndex = index & ~0b10;
        final int shift = 8 * (LE ? (index & 0b10) : (2 - (index & 0b10)));
        final int mask = ~(0xffff << shift);

        checkIndexAndAccess(buffer, byteIndex, Integer.BYTES);
        int current, newValue, result;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = (int) vhIntNative.getVolatile(buffer, byteIndex);
                result = 0xffff & op.applyAsInt((current >>> shift) & 0xffff, value);
                newValue = (current & mask) | (result << shift);
            } while (!vhIntNative.weakCompareAndSet(buffer, byteIndex, current, newValue));
            return (short) (current >>> shift);
        }
        do {
            current = (int) vhIntNative.getVolatile(buffer, byteIndex);
            result = 0xffff & Short.reverseBytes(
                    (short) op.applyAsInt(Short.reverseBytes((short) (current >>> shift)) & 0xffff, value));
            newValue = (current & mask) | (result << shift);
        } while (!vhIntNative.weakCompareAndSet(buffer, byteIndex, current, newValue));
        return Short.reverseBytes((short) (current >>> shift));
    }

    /**
     * Atomically performs the supplied update operation.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the update value
     * @param op
     *            the update operation
     * @return the previous value
     */
    @SuppressWarnings("unused")
    private static int getAndAccumulate(ByteBuffer buffer, int index, int value, IntBinaryOperator op) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        int current, newValue;
        do {
            current = (int) vhInt.getVolatile(buffer, index);
            newValue = op.applyAsInt(current, value);
        } while (!vhInt.weakCompareAndSet(buffer, index, current, newValue));
        return current;
    }

    /**
     * Atomically sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the new value
     * @return the previous value
     */
    static byte getAndSet(ByteBuffer buffer, int index, byte value) {
        final int byteIndex = index & ~0b11;
        final int shift = 8 * (LE ? (index & 0b11) : (3 - (index & 0b11)));
        final int mask = ~(0xff << shift);

        checkIndexAndAccess(buffer, byteIndex, Integer.BYTES);
        int current, newValue;
        do {
            current = (int) vhIntNative.getVolatile(buffer, byteIndex);
            newValue = (current & mask) | (value << shift);
        } while (!vhIntNative.weakCompareAndSet(buffer, byteIndex, current, newValue));
        return (byte) (current >>> shift);
    }

    /**
     * Atomically sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the new value
     * @return the previous value
     */
    static short getAndSet(ByteBuffer buffer, int index, short value) {
        final int byteIndex = index & ~0b10;
        final int shift = 8 * (LE ? (index & 0b10) : (2 - (index & 0b10)));
        final int mask = ~(0xffff << shift);

        checkIndexAndAccess(buffer, byteIndex, Integer.BYTES);
        int current, newValue;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = (int) vhIntNative.getVolatile(buffer, byteIndex);
                newValue = (current & mask) | (value << shift);
            } while (!vhIntNative.weakCompareAndSet(buffer, byteIndex, current, newValue));
            return (short) (current >>> shift);
        }
        do {
            current = (int) vhIntNative.getVolatile(buffer, byteIndex);
            newValue = (current & mask) | (Short.reverseBytes(value) << shift);
        } while (!vhIntNative.weakCompareAndSet(buffer, byteIndex, current, newValue));
        return Short.reverseBytes((short) (current >>> shift));
    }

    /**
     * Atomically sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the new value
     * @return the previous value
     * @see VarHandle#getAndSet(Object...)
     */
    static int getAndSet(ByteBuffer buffer, int index, int value) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.getAndSet(buffer, index, value);
    }

    /**
     * Atomically sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the new value
     * @return the previous value
     * @see VarHandle#getAndSet(Object...)
     */
    static long getAndSet(ByteBuffer buffer, int index, long value) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.getAndSet(buffer, index, value);
    }

    /**
     * Atomically sets the value with {@code volatile} semantics, if the current value matches the expected value.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param expected
     *            the expected value
     * @param update
     *            the new value
     * @return the witnessed value
     */
    static byte compareAndExchange(ByteBuffer buffer, int index, byte expected, byte update) {
        final int byteIndex = index & ~0b11;
        final int shift = 8 * (LE ? (index & 0b11) : (3 - (index & 0b11)));
        final int mask = ~(0xff << shift);

        checkIndexAndAccess(buffer, byteIndex, Integer.BYTES);
        int current, actualExpected, actualUpdate;
        do {
            current = (int) vhIntNative.getVolatile(buffer, byteIndex);
            actualExpected = (current & mask) | (expected << shift);
            actualUpdate = (current & mask) | (update << shift);
            if (vhIntNative.weakCompareAndSet(buffer, byteIndex, actualExpected, actualUpdate)) {
                return expected;
            }
        } while (current == actualExpected);
        return (byte) (current >>> shift);
    }

    /**
     * Atomically sets the value with {@code volatile} semantics, if the current value matches the expected value.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param expected
     *            the expected value
     * @param update
     *            the new value
     * @return the witnessed value
     */
    static short compareAndExchange(ByteBuffer buffer, int index, short expected, short update) {
        final int byteIndex = index & ~0b10;
        final int shift = 8 * (LE ? (index & 0b10) : (2 - (index & 0b10)));
        final int mask = ~(0xffff << shift);

        checkIndexAndAccess(buffer, byteIndex, Integer.BYTES);
        int current, actualExpected, actualUpdate;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = (int) vhIntNative.getVolatile(buffer, byteIndex);
                actualExpected = (current & mask) | (expected << shift);
                actualUpdate = (current & mask) | (update << shift);
                if (vhIntNative.weakCompareAndSet(buffer, byteIndex, actualExpected, actualUpdate)) {
                    return expected;
                }
            } while (current == actualExpected);
            return (short) (current >>> shift);
        }
        do {
            current = (int) vhIntNative.getVolatile(buffer, byteIndex);
            actualExpected = (current & mask) | (Short.reverseBytes(expected) << shift);
            actualUpdate = (current & mask) | (Short.reverseBytes(update) << shift);
            if (vhIntNative.weakCompareAndSet(buffer, byteIndex, actualExpected, actualUpdate)) {
                return expected;
            }
        } while (current == actualExpected);
        return Short.reverseBytes((short) (current >>> shift));
    }

    /**
     * Atomically sets the value with {@code volatile} semantics, if the current value matches the expected value.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param expected
     *            the expected value
     * @param update
     *            the new value
     * @return the witnessed value
     * @see VarHandle#compareAndExchange(Object...)
     */
    static int compareAndExchange(ByteBuffer buffer, int index, int expected, int update) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.compareAndExchange(buffer, index, expected, update);
    }

    /**
     * Atomically sets the value with {@code volatile} semantics, if the current value matches the expected value.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param expected
     *            the expected value
     * @param update
     *            the new value
     * @return the witnessed value
     * @see VarHandle#compareAndExchange(Object...)
     */
    static long compareAndExchange(ByteBuffer buffer, int index, long expected, long update) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.compareAndExchange(buffer, index, expected, update);
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see VarHandle#getVolatile(Object...)
     */
    static byte getByteVolatile(ByteBuffer buffer, int index) {
        checkIndexAndAccess(buffer, index, Byte.BYTES);
        if (!buffer.hasArray()) {
            return buffer.get(index);
        }
        return (byte) vhByteArray.getVolatile(buffer.array(), index);
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see VarHandle#getVolatile(Object...)
     */
    static short getShortVolatile(ByteBuffer buffer, int index) {
        checkIndexAndAccess(buffer, index, Short.BYTES);
        return (short) vhShort.getVolatile(buffer, index);
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see VarHandle#getVolatile(Object...)
     */
    static int getIntVolatile(ByteBuffer buffer, int index) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        return (int) vhInt.getVolatile(buffer, index);
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see VarHandle#getVolatile(Object...)
     */
    static long getLongVolatile(ByteBuffer buffer, int index) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        return (long) vhLong.getVolatile(buffer, index);
    }

    /**
     * Sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the value
     * @see VarHandle#setVolatile(Object...)
     */
    static void setByteVolatile(ByteBuffer buffer, int index, byte value) {
        checkIndexAndAccess(buffer, index, Byte.BYTES);
        if (!buffer.hasArray()) {
            buffer.put(index, value);
            return;
        }
        vhByteArray.setVolatile(buffer.array(), index, value);
    }

    /**
     * Sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the value
     * @see VarHandle#setVolatile(Object...)
     */
    static void setShortVolatile(ByteBuffer buffer, int index, short value) {
        checkIndexAndAccess(buffer, index, Short.BYTES);
        vhShort.setVolatile(buffer, index, value);
    }

    /**
     * Sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the value
     * @see VarHandle#setVolatile(Object...)
     */
    static void setIntVolatile(ByteBuffer buffer, int index, int value) {
        checkIndexAndAccess(buffer, index, Integer.BYTES);
        vhInt.setVolatile(buffer, index, value);
    }

    /**
     * Sets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the value
     * @see VarHandle#setVolatile(Object...)
     */
    static void setLongVolatile(ByteBuffer buffer, int index, long value) {
        checkIndexAndAccess(buffer, index, Long.BYTES);
        vhLong.setVolatile(buffer, index, value);
    }
}
