/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    private static final boolean IS_NATIVE_BYTE_ORDER = Bytes.DEFAULT_BYTE_ORDER == ByteOrder.nativeOrder();

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
}
