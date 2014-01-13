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
public class ArrayBufferObject extends OrdinaryObject {
    /** [[ArrayBufferData]] */
    private ByteBuffer data = null;

    /** [[ArrayBufferByteLength]] */
    private long byteLength = 0;

    public ArrayBufferObject(Realm realm) {
        super(realm);
    }

    /**
     * [[ArrayBufferData]]
     */
    public ByteBuffer getData() {
        return data;
    }

    /**
     * [[ArrayBufferData]]
     */
    public void setData(ByteBuffer data) {
        assert this.data == null : "ArrayBuffer already initialised";
        this.data = data;
    }

    /**
     * [[ArrayBufferByteLength]]
     */
    public long getByteLength() {
        return byteLength;
    }

    /**
     * [[ArrayBufferByteLength]]
     */
    public void setByteLength(long byteLength) {
        this.byteLength = byteLength;
    }
}
