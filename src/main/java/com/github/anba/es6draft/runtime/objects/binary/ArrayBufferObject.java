/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import java.nio.ByteBuffer;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.1 ArrayBuffer Objects</h2>
 * <ul>
 * <li>24.1.5 Properties of the ArrayBuffer Instances
 * </ul>
 */
public final class ArrayBufferObject extends OrdinaryObject {
    /** [[ArrayBufferData]] */
    private ByteBuffer data = null;

    /** [[ArrayBufferByteLength]] */
    private long byteLength = 0;

    private boolean neutered = false;

    public ArrayBufferObject(Realm realm) {
        super(realm);
    }

    /**
     * [[ArrayBufferData]]
     * 
     * @return the underlying byte buffer
     */
    public ByteBuffer getData() {
        return data;
    }

    /**
     * [[ArrayBufferData]]
     * 
     * @param data
     *            the byte buffer
     */
    public void setData(ByteBuffer data) {
        assert data != null : "cannot initialize ArrayBuffer with null";
        assert this.data == null : "ArrayBuffer already initialized";
        this.data = data;
    }

    /**
     * [[ArrayBufferByteLength]]
     * 
     * @return the array buffer length in bytes
     */
    public long getByteLength() {
        return byteLength;
    }

    /**
     * [[ArrayBufferByteLength]]
     * 
     * @param byteLength
     *            the new byte length
     */
    public void setByteLength(long byteLength) {
        assert byteLength >= 0 : "negative byte length: " + byteLength;
        this.byteLength = byteLength;
    }

    /**
     * Neuters this array buffer object.
     */
    public void neuter() {
        data = null;
        byteLength = 0;
        neutered = true;
    }

    /**
     * Returns {@code true} if this array buffer object has been neutered.
     * 
     * @return {@code true} if this array buffer object is neutered
     */
    public boolean isNeutered() {
        return neutered;
    }
}
