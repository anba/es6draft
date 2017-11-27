/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 *
 */
public final class SharedByteBuffer {
    private final ByteBuffer byteBuffer;
    private final Object uniqueKey;

    public SharedByteBuffer(ByteBuffer byteBuffer) {
        this(byteBuffer, new Object());
    }

    private SharedByteBuffer(ByteBuffer byteBuffer, Object uniqueKey) {
        this.byteBuffer = Objects.requireNonNull(byteBuffer);
        this.uniqueKey = uniqueKey;
    }

    /**
     * Returns the byte buffer.
     * 
     * @return the byte buffer
     */
    public ByteBuffer get() {
        return byteBuffer;
    }

    /**
     * Returns {@code true} if both shared byte buffers use the same underlying memory space.
     * 
     * @param other
     *            the other shared byte buffer
     * @return {@code true} if the same memory space is used
     */
    public boolean sameData(SharedByteBuffer other) {
        return other.uniqueKey == uniqueKey;
    }

    /**
     * Duplicates this shared byte buffer.
     * 
     * @return the duplicated shared byte buffer
     * @see ByteBuffer#duplicate()
     */
    public SharedByteBuffer duplicate() {
        return new SharedByteBuffer(byteBuffer.duplicate().order(byteBuffer.order()), uniqueKey);
    }

    @Override
    public String toString() {
        return byteBuffer.toString();
    }
}
