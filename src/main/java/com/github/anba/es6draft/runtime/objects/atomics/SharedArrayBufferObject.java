/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import java.nio.ByteBuffer;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Shared Memory and Atomics</h1><br>
 * <h2>SharedArrayBuffer Objects</h2>
 * <ul>
 * <li>Properties of the SharedArrayBuffer Instances
 * </ul>
 */
public final class SharedArrayBufferObject extends OrdinaryObject implements ArrayBuffer {
    /** [[SharedArrayBufferData]] */
    private final ByteBuffer data;

    /** [[SharedArrayBufferByteLength]] */
    private final long byteLength;

    /**
     * Constructs a new SharedArrayBuffer object.
     * 
     * @param realm
     *            the realm object
     * @param data
     *            the byte buffer
     * @param byteLength
     *            the byte length
     * @param prototype
     *            the prototype object
     */
    public SharedArrayBufferObject(Realm realm, ByteBuffer data, long byteLength, ScriptObject prototype) {
        super(realm);
        assert data != null : "cannot initialize SharedArrayBuffer with null";
        assert byteLength >= 0 : "negative byte length: " + byteLength;
        assert byteLength <= data.capacity() : "invalid byte length: " + byteLength;
        this.data = data;
        this.byteLength = byteLength;
        setPrototype(prototype);
    }

    /**
     * [[SharedArrayBufferData]]
     * 
     * @return the underlying byte buffer
     */
    @Override
    public ByteBuffer getData() {
        return data;
    }

    /**
     * [[SharedArrayBufferByteLength]]
     * 
     * @return the array buffer length in bytes
     */
    @Override
    public long getByteLength() {
        return byteLength;
    }

    @Override
    public void detach() {
        // SharedArrayBuffers cannot be detached.
    }

    @Override
    public boolean isDetached() {
        return false;
    }
}
