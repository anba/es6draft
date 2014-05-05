/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.builtins.ExoticIntegerIndexedObject;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.7 Properties of TypedArray instances
 * </ul>
 */
public final class TypedArrayObject extends ExoticIntegerIndexedObject implements ArrayBufferView {
    /** [[ViewedArrayBuffer]] */
    private ArrayBufferObject buffer;

    /** [[ElementType]] */
    private ElementType elementType;

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
     * {@inheritDoc}
     */
    @Override
    protected long getLength() {
        return getArrayLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected long getLength(ExecutionContext cx) {
        ArrayBufferObject buffer = getBuffer();
        if (buffer == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        return getArrayLength();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean elementHas(ExecutionContext cx, double index) {
        // Steps 1-6 of elementGet to support hasOwnProperty
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBufferObject buffer = getBuffer();
        /* step 4 */
        if (buffer == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 5 */
        if (!IsInteger(index)) {
            return false;
        }
        /* step 6 */
        long length = getArrayLength();
        /* step 7 */
        if (index < 0 || index >= length) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object elementGet(ExecutionContext cx, double index) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBufferObject buffer = getBuffer();
        /* step 4 */
        if (buffer == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 5 */
        if (!IsInteger(index)) {
            return UNDEFINED;
        }
        /* step 6 */
        long length = getArrayLength();
        /* step 7 */
        if (index < 0 || index >= length) {
            return UNDEFINED;
        }
        /* step 8 */
        long offset = getByteOffset();
        /* steps 9, 12 */
        ElementType elementType = getElementType();
        /* step 10 */
        int elementSize = elementType.size();
        /* step 11 */
        long indexedPosition = (long) ((index * elementSize) + offset);
        /* step 13 */
        return GetValueFromBuffer(cx, buffer, indexedPosition, elementType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean elementSet(ExecutionContext cx, double index, Object value) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBufferObject buffer = getBuffer();
        /* step 4 */
        if (buffer == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 5 */
        if (!IsInteger(index)) {
            return false;
        }
        /* step 6 */
        long length = getArrayLength();
        /* steps 7-8 */
        double numValue = ToNumber(cx, value);
        /* step 9 */
        if (index < 0 || index >= length) {
            return false;
        }
        /* step 10 */
        long offset = getByteOffset();
        /* steps 11, 14 */
        ElementType elementType = getElementType();
        /* step 12 */
        int elementSize = elementType.size();
        /* step 13 */
        long indexedPosition = (long) ((index * elementSize) + offset);
        /* steps 15-16 */
        SetValueInBuffer(cx, buffer, indexedPosition, elementType, numValue);
        /* step 17 */
        return true;
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
        assert buffer != null && buffer.getData() != null : "ArrayBufferObject not initialized";
        assert this.buffer == null : "TypedArrayObject already initialized";
        this.buffer = buffer;
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
     * [[ElementType]]
     * 
     * @param elementType
     *            the new element type
     */
    public void setElementType(ElementType elementType) {
        assert elementType != null;
        this.elementType = elementType;
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

    /**
     * [[ArrayLength]]
     * 
     * @return the array length
     */
    public long getArrayLength() {
        return arrayLength;
    }

    /**
     * [[ArrayLength]]
     * 
     * @param arrayLength
     *            the new array length
     */
    public void setArrayLength(long arrayLength) {
        assert arrayLength >= 0 : "negative array length: " + arrayLength;
        this.arrayLength = arrayLength;
    }
}
