/**
 * Copyright (c) 2012-2013 André Bargull
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
public class DataViewObject extends OrdinaryObject {
    /** [[DataArrayBuffer]] */
    private ArrayBufferObject buffer;

    /** [[ByteLength]] */
    private long byteLength;

    /** [[ByteOffset]] */
    private long byteOffset;

    public DataViewObject(Realm realm) {
        super(realm);
    }

    /**
     * [[DataArrayBuffer]]
     */
    public ArrayBufferObject getBuffer() {
        return buffer;
    }

    /**
     * [[DataArrayBuffer]]
     */
    public void setBuffer(ArrayBufferObject data) {
        this.buffer = data;
    }

    /**
     * [[ByteLength]]
     */
    public long getByteLength() {
        return byteLength;
    }

    /**
     * [[ByteLength]]
     */
    public void setByteLength(long byteLength) {
        this.byteLength = byteLength;
    }

    /**
     * [[ByteOffset]]
     */
    public long getByteOffset() {
        return byteOffset;
    }

    /**
     * [[ByteOffset]]
     */
    public void setByteOffset(long byteOffset) {
        this.byteOffset = byteOffset;
    }
}
