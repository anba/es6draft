/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * 
 */
final class UnsafeHolder {
    private UnsafeHolder() {
    }

    private static final VarHandle vhInt = MethodHandles.byteBufferViewVarHandle(int[].class, Bytes.DEFAULT_BYTE_ORDER);
    private static final VarHandle vhLong = MethodHandles.byteBufferViewVarHandle(long[].class,
            Bytes.DEFAULT_BYTE_ORDER);

    private static void checkIndexAndAccess(ByteBuffer buffer, int index, int size) {
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
     * Calls {@link VarHandle#getVolatile(Object...)}.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @return the value
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
}
