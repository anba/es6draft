/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Bytes;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.2 SharedArrayBuffer Objects</h2>
 * <ul>
 * <li>24.2.5 Properties of the SharedArrayBuffer Instances
 * </ul>
 */
public final class SharedArrayBufferObject extends OrdinaryObject implements ArrayBuffer {
    /** [[SharedArrayBufferData]] */
    private final SharedByteBuffer data;
    private ByteBuffer nonNativeData;

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
    public SharedArrayBufferObject(Realm realm, SharedByteBuffer data, long byteLength, ScriptObject prototype) {
        super(realm, prototype);
        assert data != null : "cannot initialize SharedArrayBuffer with null";
        assert data.get().order() == Bytes.DEFAULT_BYTE_ORDER : "unexpected byte order";
        assert byteLength >= 0 : "negative byte length: " + byteLength;
        assert byteLength <= data.get().capacity() : "invalid byte length: " + byteLength;
        this.data = data;
        this.byteLength = byteLength;
    }

    /**
     * [[SharedArrayBufferData]]
     * 
     * @return the wrapper around the underlying byte buffer
     */
    public SharedByteBuffer getSharedData() {
        return data;
    }

    /**
     * [[SharedArrayBufferData]]
     * 
     * @return the underlying byte buffer
     */
    @Override
    public ByteBuffer getData() {
        return data.get();
    }

    @Override
    public ByteBuffer getData(ByteOrder byteOrder) {
        ByteBuffer rawData = getData();
        if (rawData.order() == byteOrder) {
            return rawData;
        }
        ByteBuffer view = nonNativeData;
        if (view == null) {
            view = rawData.duplicate().order(byteOrder);
            nonNativeData = view;
        }
        return view;
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

    @Override
    public boolean sameData(ArrayBuffer other) {
        if (other == this) {
            return true;
        }
        return other.getClass() == SharedArrayBufferObject.class
                && ((SharedArrayBufferObject) other).data.sameData(data);
    }

    @Override
    public String toString() {
        return String.format("%s, data=%s, byteLength=%d", super.toString(), data, byteLength);
    }
}
