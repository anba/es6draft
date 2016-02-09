/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * 
 */
@SuppressWarnings("restriction")
final class UnsafeHolder {
    private UnsafeHolder() {
    }

    private static final Unsafe UNSAFE = initializeUnsafe();
    private static final long BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);

    private static Unsafe initializeUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException e) {
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<Unsafe>) () -> {
                    Field f = Unsafe.class.getDeclaredField("theUnsafe");
                    f.setAccessible(true);
                    return (Unsafe) f.get(null);
                });
            } catch (PrivilegedActionException e2) {
                throw new ExceptionInInitializerError(e2.getException());
            }
        }
    }

    private static Object baseObject(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            return buffer.array();
        }
        return null;
    }

    private static long offsetOrMemoryAddress(ByteBuffer buffer, int index, int size) {
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
     * Calls {@link Unsafe#fullFence()}.
     */
    static void fullFence() {
        UNSAFE.fullFence();
    }

    /**
     * Calls {@link Unsafe#compareAndSwapInt(Object, long, int, int)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     */
    static boolean compareAndSwapInt(ByteBuffer buffer, int index, int expected, int update) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        return UNSAFE.compareAndSwapInt(base, offset, expected, update);
    }

    /**
     * Calls {@link Unsafe#getByteVolatile(Object, long)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     */
    static byte getByteVolatile(ByteBuffer buffer, int index) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Byte.BYTES);
        return UNSAFE.getByteVolatile(base, offset);
    }

    /**
     * Calls {@link Unsafe#getShortVolatile(Object, long)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     */
    static short getShortVolatile(ByteBuffer buffer, int index) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Short.BYTES);
        return UNSAFE.getShortVolatile(base, offset);
    }

    /**
     * Calls {@link Unsafe#getIntVolatile(Object, long)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
     */
    static int getIntVolatile(ByteBuffer buffer, int index) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        return UNSAFE.getIntVolatile(base, offset);
    }

    /**
     * Calls {@link Unsafe#putByteVolatile(Object, long, byte)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the value
     */
    static void putByteVolatile(ByteBuffer buffer, int index, byte value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Byte.BYTES);
        UNSAFE.putByteVolatile(base, offset, value);
    }

    /**
     * Calls {@link Unsafe#putShortVolatile(Object, long, int)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the value
     */
    static void putShortVolatile(ByteBuffer buffer, int index, short value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Short.BYTES);
        UNSAFE.putShortVolatile(base, offset, value);
    }

    /**
     * Calls {@link Unsafe#putIntVolatile(Object, long, int)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the value
     */
    static void putIntVolatile(ByteBuffer buffer, int index, int value) {
        Object base = baseObject(buffer);
        long offset = offsetOrMemoryAddress(buffer, index, Integer.BYTES);
        UNSAFE.putIntVolatile(base, offset, value);
    }
}
