/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.2 DataView Objects</h2>
 * <ul>
 * <li>24.2.5 Properties of DataView Instances
 * </ul>
 */
public final class DataViewObject extends OrdinaryObject implements ArrayBufferView {
    /** [[ViewedArrayBuffer]] */
    private ArrayBufferObject buffer;

    /** [[ByteLength]] */
    private long byteLength;

    /** [[ByteOffset]] */
    private long byteOffset;

    /**
     * Constructs a new DataView object.
     * 
     * @param realm
     *            the realm object
     */
    public DataViewObject(Realm realm) {
        super(realm);
    }

    /**
     * [[ViewedArrayBuffer]]
     */
    @Override
    public ArrayBufferObject getBuffer() {
        return buffer;
    }

    /**
     * [[ViewedArrayBuffer]]
     * 
     * @param buffer
     *            the new array buffer object
     */
    public void setBuffer(ArrayBufferObject buffer) {
        assert buffer != null && (buffer.getData() != null || buffer.isNeutered()) : "ArrayBufferObject not initialized";
        assert this.buffer == null : "DataViewObject already initialized";
        this.buffer = buffer;
    }

    /**
     * [[ByteLength]]
     */
    @Override
    public long getByteLength() {
        return byteLength;
    }

    /**
     * [[ByteLength]]
     * 
     * @param byteLength
     *            the new byte length
     */
    public void setByteLength(long byteLength) {
        assert byteLength >= 0 : "negative byte length: " + byteLength;
        this.byteLength = byteLength;
    }

    /**
     * [[ByteOffset]]
     */
    @Override
    public long getByteOffset() {
        return byteOffset;
    }

    /**
     * [[ByteOffset]]
     * 
     * @param byteOffset
     *            the new byte offset
     */
    public void setByteOffset(long byteOffset) {
        assert byteOffset >= 0 : "negative byte offset: " + byteOffset;
        this.byteOffset = byteOffset;
    }
}
