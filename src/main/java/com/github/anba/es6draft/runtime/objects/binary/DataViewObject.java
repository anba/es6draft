/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
    private final ArrayBuffer buffer;

    /** [[ByteLength]] */
    private final long byteLength;

    /** [[ByteOffset]] */
    private final long byteOffset;

    /**
     * Constructs a new DataView object.
     * 
     * @param realm
     *            the realm object
     * @param buffer
     *            the array buffer
     * @param byteLength
     *            the byte length
     * @param byteOffset
     *            the byte offset
     * @param prototype
     *            the prototype object
     */
    public DataViewObject(Realm realm, ArrayBuffer buffer, long byteLength, long byteOffset, ScriptObject prototype) {
        super(realm, prototype);
        assert buffer != null : "cannot initialize DataView with null";
        assert byteLength >= 0 : "negative byte length: " + byteLength;
        assert byteOffset >= 0 : "negative byte offset: " + byteOffset;
        assert buffer.isDetached() || (byteOffset + byteLength <= buffer.getByteLength());
        this.buffer = buffer;
        this.byteLength = byteLength;
        this.byteOffset = byteOffset;
    }

    /**
     * [[ViewedArrayBuffer]]
     */
    @Override
    public ArrayBuffer getBuffer() {
        return buffer;
    }

    /**
     * [[ByteLength]]
     */
    @Override
    public long getByteLength() {
        return byteLength;
    }

    /**
     * [[ByteOffset]]
     */
    @Override
    public long getByteOffset() {
        return byteOffset;
    }

    @Override
    public String toString() {
        return String.format("%s, buffer={data=%s, byteLength=%s, detached=%b}, byteLength=%d, byteOffset=%d",
                super.toString(), buffer.getData(), buffer.getByteLength(), buffer.isDetached(), byteLength,
                byteOffset);
    }
}
