/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.IntegerIndexedObject;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.7 Properties of TypedArray instances
 * </ul>
 */
public final class TypedArrayObject extends IntegerIndexedObject implements ArrayBufferView {
    /** [[ViewedArrayBuffer]] */
    private final ArrayBuffer buffer;

    /** [[ElementType]] */
    private final ElementType elementType;
    private final int elementShift;

    /** [[ByteLength]] */
    private final long byteLength;

    /** [[ByteOffset]] */
    private final long byteOffset;

    /** [[ArrayLength]] */
    private final long arrayLength;

    /**
     * Constructs a new TypedArray object.
     * 
     * @param realm
     *            the realm object
     * @param elementType
     *            the element type
     * @param buffer
     *            the array buffer
     * @param byteLength
     *            the byte length
     * @param byteOffset
     *            the byte offset
     * @param arrayLength
     *            the array length
     * @param prototype
     *            the prototype object
     */
    public TypedArrayObject(Realm realm, ElementType elementType, ArrayBuffer buffer, long byteLength, long byteOffset,
            long arrayLength, ScriptObject prototype) {
        super(realm);
        assert elementType != null && buffer != null : "cannot initialize TypedArrayObject with null";
        assert byteLength >= 0 : "negative byte length: " + byteLength;
        assert byteOffset >= 0 : "negative byte offset: " + byteOffset;
        assert arrayLength >= 0 : "negative array length: " + arrayLength;
        assert buffer.isDetached() || (byteOffset + byteLength <= buffer.getByteLength());
        assert arrayLength * elementType.size() == byteLength : "invalid length: " + byteLength;
        this.elementType = elementType;
        this.elementShift = 31 - Integer.numberOfLeadingZeros(elementType.size());
        this.buffer = buffer;
        this.byteLength = byteLength;
        this.byteOffset = byteOffset;
        this.arrayLength = arrayLength;
        setPrototype(prototype);
    }

    @Override
    public long getLength() {
        return getArrayLength();
    }

    @Override
    protected boolean elementHas(ExecutionContext cx, long index) {
        assert index >= 0;
        // 9.4.5.2 [[HasProperty]](P)
        /* step 3.c.i */
        ArrayBuffer buffer = getBuffer();
        /* step 3.c.ii */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 3.c.iii-vii */
        return index < getArrayLength();
    }

    @Override
    protected Object elementGet(ExecutionContext cx, long index) {
        assert index >= 0;
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBuffer buffer = getBuffer();
        /* step 4 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 5-8 */
        if (index >= getArrayLength()) {
            return UNDEFINED;
        }
        /* step 9 */
        long offset = getByteOffset();
        /* steps 10, 13 */
        ElementType elementType = getElementType();
        /* steps 11-12 */
        long indexedPosition = (index << elementShift) + offset;
        /* step 14 */
        return GetValueFromBuffer(buffer, indexedPosition, elementType);
    }

    double elementGetDirect(ExecutionContext cx, long index) {
        assert 0 <= index && index < getArrayLength();
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBuffer buffer = getBuffer();
        /* step 4 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 5-8 (not applicable) */
        /* step 9 */
        long offset = getByteOffset();
        /* steps 10, 13 */
        ElementType elementType = getElementType();
        /* steps 11-12 */
        long indexedPosition = (index << elementShift) + offset;
        /* step 14 */
        return GetValueFromBuffer(buffer, indexedPosition, elementType);
    }

    @Override
    protected boolean elementSet(ExecutionContext cx, long index, Object value) {
        assert index >= 0;
        /* steps 1-2 (not applicable) */
        /* steps 3-4 */
        double numValue = ToNumber(cx, value);
        /* step 5 */
        ArrayBuffer buffer = getBuffer();
        /* step 6 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 7-10 */
        if (index >= getArrayLength()) {
            return false;
        }
        /* step 11 */
        long offset = getByteOffset();
        /* steps 12, 15 */
        ElementType elementType = getElementType();
        /* steps 13-14 */
        long indexedPosition = (index << elementShift) + offset;
        /* step 16 */
        SetValueInBuffer(buffer, indexedPosition, elementType, numValue);
        /* step 17 */
        return true;
    }

    void elementSetDirect(ExecutionContext cx, long index, double numValue) {
        assert 0 <= index && index < getArrayLength();
        /* steps 1-4 (not applicable) */
        /* step 5 */
        ArrayBuffer buffer = getBuffer();
        /* step 6 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 7-10 (not applicable) */
        /* step 11 */
        long offset = getByteOffset();
        /* steps 12, 15 */
        ElementType elementType = getElementType();
        /* steps 13-14 */
        long indexedPosition = (index << elementShift) + offset;
        /* steps 16-17 */
        SetValueInBuffer(buffer, indexedPosition, elementType, numValue);
        /* step 18 (return) */
    }

    /**
     * [[ViewedArrayBuffer]]
     */
    @Override
    public ArrayBuffer getBuffer() {
        return buffer;
    }

    /**
     * [[ElementType]]
     * 
     * @return the element type
     */
    public ElementType getElementType() {
        return elementType;
    }

    /**
     * [[TypedArrayName]]
     * 
     * @return the typed array name
     */
    public String getTypedArrayName() {
        return elementType.getConstructorName();
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

    /**
     * [[ArrayLength]]
     * 
     * @return the array length
     */
    public long getArrayLength() {
        return arrayLength;
    }
}
