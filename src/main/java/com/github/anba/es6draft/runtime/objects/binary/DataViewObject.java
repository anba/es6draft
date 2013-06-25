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
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.7 DataView Objects</h3>
 * <ul>
 * <li>15.13.7.5 Properties of DataView Instances
 * </ul>
 */
public class DataViewObject extends OrdinaryObject {
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
    public ArrayBufferObject getBuffer() {
        return buffer;
    }

    /**
     * [[ViewedArrayBuffer]]
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
