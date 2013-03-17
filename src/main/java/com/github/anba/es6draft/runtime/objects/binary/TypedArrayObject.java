/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.IndexedDelegationExoticObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.6 TypedArray Object Structures</h3>
 * <ul>
 * <li>15.13.6.5 Properties of TypedArray instances
 * </ul>
 */
public class TypedArrayObject extends IndexedDelegationExoticObject implements ScriptObject {

    /** [[TypedArrayData]] */
    private ArrayBufferObject data;

    /** [[TypedArrayElementKind]] */
    private ElementKind elementKind;

    /** [[ByteLength]] */
    private long byteLength;

    /** [[ByteOffset]] */
    private long byteOffset;

    /** [[ArrayLength]] */
    private long arrayLength;

    public TypedArrayObject(Realm realm) {
        super(realm);
    }

    /**
     * [[TypedArrayData]]
     */
    public ArrayBufferObject getData() {
        return data;
    }

    /**
     * [[TypedArrayData]]
     */
    public void setData(ArrayBufferObject data) {
        this.data = data;
    }

    /**
     * [[TypedArrayElementKind]]
     */
    public ElementKind getElementKind() {
        return elementKind;
    }

    /**
     * [[TypedArrayElementKind]]
     */
    public void setElementKind(ElementKind elementKind) {
        this.elementKind = elementKind;
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

    /**
     * [[ArrayLength]]
     */
    public long getArrayLength() {
        return arrayLength;
    }

    /**
     * [[ArrayLength]]
     */
    public void setArrayLength(long arrayLength) {
        this.arrayLength = arrayLength;
    }
}
