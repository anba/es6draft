/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;

import com.github.anba.es6draft.runtime.internal.Bytes;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * 
 */
@SuppressWarnings("restriction")
final class Atomics {
    private Atomics() {
    }

    private static final Unsafe UNSAFE = initializeUnsafe();
    private static final long BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    private static final boolean IS_NATIVE_BYTE_ORDER = Bytes.DEFAULT_BYTE_ORDER == ByteOrder.nativeOrder();
    private static final boolean LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private static Unsafe initializeUnsafe() {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Unsafe>) () -> {
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                return (Unsafe) f.get(null);
            });
        } catch (PrivilegedActionException e) {
            throw new ExceptionInInitializerError(e.getException());
        }
    }

    private static Object baseObject(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return buffer.array();
        }
        return null;
    }

    private static long offsetOrMemoryAddress(ByteBuffer buffer, int index, int size) {
        if ((index & (size - 1)) != 0) {
            throw new IllegalArgumentException("Misaligned access");
        }
        if (index < 0 || size > buffer.limit() - index) {
            throw new IndexOutOfBoundsException(indexOutOfBoundsMessage(buffer, index, size));
        }
        if (buffer.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (buffer.hasArray()) {
            return BYTE_ARRAY_BASE_OFFSET + buffer.arrayOffset() + index;
        }
        assert buffer.isDirect();
        return ((DirectBuffer) buffer).address() + index;
    }

    private static String indexOutOfBoundsMessage(ByteBuffer buffer, int index, int size) {
        return String.format("limit=%d, index=%d, size=%d", buffer.limit(), index, size);
    }

    /**
     * Performs a full memory fence.
     * 
     * @see Unsafe#fullFence()
     */
    static void fullFence() {
        UNSAFE.fullFence();
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
     * @see Unsafe#getAndAddInt(Object, long, int)
     */
    static int getAndAdd(ByteBuffer buffer, int index, int value) {
        if (IS_NATIVE_BYTE_ORDER) {
            Object base = baseObject(buffer);
            long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
            return UNSAFE.getAndAddInt(base, offset, value);
        }
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
     * @see Unsafe#getAndAddLong(Object, long, long)
     */
    static long getAndAdd(ByteBuffer buffer, int index, long value) {
        if (IS_NATIVE_BYTE_ORDER) {
            Object base = baseObject(buffer);
            long offset = offsetOrMemoryAddress(buffer, index, Long.BYTES);
            return UNSAFE.getAndAddLong(base, offset, value);
        }
        return getAndAccumulate(buffer, index, value, (r, v) -> r + v);
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
     * @see Unsafe#getAndAddInt(Object, long, int)
     */
    static int getAndSub(ByteBuffer buffer, int index, int value) {
        if (IS_NATIVE_BYTE_ORDER) {
            Object base = baseObject(buffer);
            long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
            return UNSAFE.getAndAddInt(base, offset, -value);
        }
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
     * @see Unsafe#getAndAddLong(Object, long, long)
     */
    static long getAndSub(ByteBuffer buffer, int index, long value) {
        if (IS_NATIVE_BYTE_ORDER) {
            Object base = baseObject(buffer);
            long offset = offsetOrMemoryAddress(buffer, index, Long.BYTES);
            return UNSAFE.getAndAddLong(base, offset, -value);
        }
        return getAndAccumulate(buffer, index, value, (r, v) -> r - v);
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
    static long getAndBitwiseAnd(ByteBuffer buffer, int index, long value) {
        return getAndAccumulate(buffer, index, value, (r, v) -> r & v);
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
    static long getAndBitwiseOr(ByteBuffer buffer, int index, long value) {
        return getAndAccumulate(buffer, index, value, (r, v) -> r | v);
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
    static long getAndBitwiseXor(ByteBuffer buffer, int index, long value) {
        return getAndAccumulate(buffer, index, value, (r, v) -> r ^ v);
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

        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, byteIndex, Integer.BYTES);
        int current, newValue, result;
        do {
            current = UNSAFE.getIntVolatile(base, offset);
            result = 0xff & op.applyAsInt((current >>> shift) & 0xff, value);
            newValue = (current & mask) | (result << shift);
        } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
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

        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, byteIndex, Integer.BYTES);
        int current, newValue, result;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = UNSAFE.getIntVolatile(base, offset);
                result = 0xffff & op.applyAsInt((current >>> shift) & 0xffff, value);
                newValue = (current & mask) | (result << shift);
            } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
            return (short) (current >>> shift);
        }
        do {
            current = UNSAFE.getIntVolatile(base, offset);
            result = 0xffff & Short.reverseBytes(
                    (short) op.applyAsInt(Short.reverseBytes((short) (current >>> shift)) & 0xffff, value));
            newValue = (current & mask) | (result << shift);
        } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
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
    private static int getAndAccumulate(ByteBuffer buffer, int index, int value, IntBinaryOperator op) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        int current, newValue;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = UNSAFE.getIntVolatile(base, offset);
                newValue = op.applyAsInt(current, value);
            } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
            return current;
        }
        do {
            current = UNSAFE.getIntVolatile(base, offset);
            newValue = Integer.reverseBytes(op.applyAsInt(Integer.reverseBytes(current), value));
        } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
        return Integer.reverseBytes(current);
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
    private static long getAndAccumulate(ByteBuffer buffer, int index, long value, LongBinaryOperator op) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Long.BYTES);
        long current, newValue;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = UNSAFE.getLongVolatile(base, offset);
                newValue = op.applyAsLong(current, value);
            } while (!UNSAFE.compareAndSwapLong(base, offset, current, newValue));
            return current;
        }
        do {
            current = UNSAFE.getLongVolatile(base, offset);
            newValue = Long.reverseBytes(op.applyAsLong(Long.reverseBytes(current), value));
        } while (!UNSAFE.compareAndSwapLong(base, offset, current, newValue));
        return Long.reverseBytes(current);
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

        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, byteIndex, Integer.BYTES);
        int current, newValue;
        do {
            current = UNSAFE.getIntVolatile(base, offset);
            newValue = (current & mask) | (value << shift);
        } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
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

        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, byteIndex, Integer.BYTES);
        int current, newValue;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = UNSAFE.getIntVolatile(base, offset);
                newValue = (current & mask) | (value << shift);
            } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
            return (short) (current >>> shift);
        }
        do {
            current = UNSAFE.getIntVolatile(base, offset);
            newValue = (current & mask) | (Short.reverseBytes(value) << shift);
        } while (!UNSAFE.compareAndSwapInt(base, offset, current, newValue));
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
     * @see Unsafe#getAndSetInt(Object, long, int)
     */
    static int getAndSet(ByteBuffer buffer, int index, int value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            return UNSAFE.getAndSetInt(base, offset, value);
        }
        return Integer.reverseBytes(UNSAFE.getAndSetInt(base, offset, Integer.reverseBytes(value)));
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
     * @see Unsafe#getAndSetLong(Object, long, long)
     */
    static long getAndSet(ByteBuffer buffer, int index, long value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Long.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            return UNSAFE.getAndSetLong(base, offset, value);
        }
        return Long.reverseBytes(UNSAFE.getAndSetLong(base, offset, Long.reverseBytes(value)));
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

        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, byteIndex, Integer.BYTES);
        int current, actualExpected, actualUpdate;
        do {
            current = UNSAFE.getIntVolatile(base, offset);
            actualExpected = (current & mask) | (expected << shift);
            actualUpdate = (current & mask) | (update << shift);
            if (UNSAFE.compareAndSwapInt(base, offset, actualExpected, actualUpdate)) {
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

        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, byteIndex, Integer.BYTES);
        int current, actualExpected, actualUpdate;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                current = UNSAFE.getIntVolatile(base, offset);
                actualExpected = (current & mask) | (expected << shift);
                actualUpdate = (current & mask) | (update << shift);
                if (UNSAFE.compareAndSwapInt(base, offset, actualExpected, actualUpdate)) {
                    return expected;
                }
            } while (current == actualExpected);
            return (short) (current >>> shift);
        }
        do {
            current = UNSAFE.getIntVolatile(base, offset);
            actualExpected = (current & mask) | (Short.reverseBytes(expected) << shift);
            actualUpdate = (current & mask) | (Short.reverseBytes(update) << shift);
            if (UNSAFE.compareAndSwapInt(base, offset, actualExpected, actualUpdate)) {
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
     */
    static int compareAndExchange(ByteBuffer buffer, int index, int expected, int update) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        int current;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                if (UNSAFE.compareAndSwapInt(base, offset, expected, update)) {
                    return expected;
                }
                current = UNSAFE.getIntVolatile(base, offset);
            } while (current == expected);
            return current;
        }
        do {
            if (UNSAFE.compareAndSwapInt(base, offset, Integer.reverseBytes(expected), Integer.reverseBytes(update))) {
                return expected;
            }
            current = Integer.reverseBytes(UNSAFE.getIntVolatile(base, offset));
        } while (current == expected);
        return current;
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
    static long compareAndExchange(ByteBuffer buffer, int index, long expected, long update) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Long.BYTES);
        long current;
        if (IS_NATIVE_BYTE_ORDER) {
            do {
                if (UNSAFE.compareAndSwapLong(base, offset, expected, update)) {
                    return expected;
                }
                current = UNSAFE.getLongVolatile(base, offset);
            } while (current == expected);
            return current;
        }
        do {
            if (UNSAFE.compareAndSwapLong(base, offset, Long.reverseBytes(expected), Long.reverseBytes(update))) {
                return expected;
            }
            current = Long.reverseBytes(UNSAFE.getLongVolatile(base, offset));
        } while (current == expected);
        return current;
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see Unsafe#getByteVolatile(Object, long)
     */
    static byte getByteVolatile(ByteBuffer buffer, int index) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Byte.BYTES);
        return UNSAFE.getByteVolatile(base, offset);
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see Unsafe#getShortVolatile(Object, long)
     */
    static short getShortVolatile(ByteBuffer buffer, int index) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Short.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            return UNSAFE.getShortVolatile(base, offset);
        }
        return Short.reverseBytes(UNSAFE.getShortVolatile(base, offset));
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see Unsafe#getIntVolatile(Object, long)
     */
    static int getIntVolatile(ByteBuffer buffer, int index) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            return UNSAFE.getIntVolatile(base, offset);
        }
        return Integer.reverseBytes(UNSAFE.getIntVolatile(base, offset));
    }

    /**
     * Gets the value with {@code volatile} semantics.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     * @see Unsafe#getLongVolatile(Object, long)
     */
    static long getLongVolatile(ByteBuffer buffer, int index) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Long.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            return UNSAFE.getLongVolatile(base, offset);
        }
        return Long.reverseBytes(UNSAFE.getLongVolatile(base, offset));
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
     * @see Unsafe#putByteVolatile(Object, long, int)
     */
    static void setByteVolatile(ByteBuffer buffer, int index, byte value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Byte.BYTES);
        UNSAFE.putByteVolatile(base, offset, value);
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
     * @see Unsafe#putShortVolatile(Object, long, int)
     */
    static void setShortVolatile(ByteBuffer buffer, int index, short value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Short.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            UNSAFE.putShortVolatile(base, offset, value);
        } else {
            UNSAFE.putShortVolatile(base, offset, Short.reverseBytes(value));
        }
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
     * @see Unsafe#putIntVolatile(Object, long, int)
     */
    static void setIntVolatile(ByteBuffer buffer, int index, int value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            UNSAFE.putIntVolatile(base, offset, value);
        } else {
            UNSAFE.putIntVolatile(base, offset, Integer.reverseBytes(value));
        }
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
     * @see Unsafe#putLongVolatile(Object, long, long)
     */
    static void setLongVolatile(ByteBuffer buffer, int index, long value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Long.BYTES);
        if (IS_NATIVE_BYTE_ORDER) {
            UNSAFE.putLongVolatile(base, offset, value);
        } else {
            UNSAFE.putLongVolatile(base, offset, Long.reverseBytes(value));
        }
    }
}
