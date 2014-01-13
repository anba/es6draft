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
public class DataViewObject extends OrdinaryObject implements ArrayBufferView {
    /** [[ViewedArrayBuffer]] */
    private ArrayBufferObject buffer;

    /** [[ByteLength]] */
    private long byteLength;

    /** [[ByteOffset]] */
    private long byteOffset;

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
     */
    public void setBuffer(ArrayBufferObject buffer) {
        assert buffer.getData() != null;
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
     */
    public void setByteLength(long byteLength) {
        assert byteLength >= 0;
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
     */
    public void setByteOffset(long byteOffset) {
        assert byteOffset >= 0;
        this.byteOffset = byteOffset;
    }
}
