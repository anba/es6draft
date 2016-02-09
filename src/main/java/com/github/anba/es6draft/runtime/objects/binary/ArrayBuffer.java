/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import java.nio.ByteBuffer;

import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.1 ArrayBuffer Objects</h2>
 * <p>
 * Interface for {@link ArrayBufferObject}.
 */
public interface ArrayBuffer extends ScriptObject {
    /**
     * [[ArrayBufferData]]
     *
     * @return the underlying byte buffer or {@code null} if detached
     */
    ByteBuffer getData();

    /**
     * [[ArrayBufferByteLength]]
     * 
     * @return the array buffer length in bytes or {@code 0} if detached
     */
    long getByteLength();

    /**
     * Detaches this array buffer object.
     */
    void detach();

    /**
     * Returns {@code true} if this array buffer object has been detached.
     * 
     * @return {@code true} if this array buffer object is detached
     */
    boolean isDetached();
}
