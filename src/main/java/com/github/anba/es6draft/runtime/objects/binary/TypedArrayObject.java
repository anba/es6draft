/**
 * Copyright (c) 2012-2015 André Bargull
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
import com.github.anba.es6draft.runtime.AbstractOperations.CanonicalNumericString;
import com.github.anba.es6draft.runtime.internal.Messages;
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
    private ArrayBufferObject buffer;

    /** [[ElementType]] */
    private ElementType elementType;

    /** [[ByteLength]] */
    private long byteLength;

    /** [[ByteOffset]] */
    private long byteOffset;

    /** [[ArrayLength]] */
    private long arrayLength;

    /**
     * Constructs a new TypedArray object.
     * 
     * @param realm
     *            the realm object
     */
    public TypedArrayObject(Realm realm) {
        super(realm);
    }

    @Override
    public long getLength() {
        return getArrayLength();
    }

    @Override
    protected boolean elementHasOwn(ExecutionContext cx, long index) {
        assert index >= 0;
        // Steps 1-8 of elementGet to support hasOwnProperty
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBufferObject buffer = getBuffer();
        /* step 4 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 5-8 */
        return index < getArrayLength();
    }

    @Override
    protected boolean elementHas(ExecutionContext cx, long index) {
        assert index >= 0;
        // 9.4.5.2 [[HasProperty]](P)
        /* step 3.c.i */
        ArrayBufferObject buffer = getBuffer();
        /* step 3.c.ii */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 3.c.iii-v */
        // FIXME: spec bug - or: index < [[ArrayLength]] ? (bug 3619)
        return true;
    }

    @Override
    protected boolean elementHas(ExecutionContext cx, CanonicalNumericString numericString) {
        // 9.4.5.2 [[HasProperty]](P)
        /* step 3.c.i */
        ArrayBufferObject buffer = getBuffer();
        /* step 3.c.ii */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 3.c.iii-v */
        switch (numericString) {
        case NegativeZero:
        case NonInteger:
            return false;
        case NegativeInteger:
        case PositiveInteger:
            return true;
        case None:
        default:
            throw new AssertionError();
        }
    }

    @Override
    protected Object elementGet(ExecutionContext cx, long index) {
        assert index >= 0;
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBufferObject buffer = getBuffer();
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
        /* step 11 */
        int elementSize = elementType.size();
        /* step 12 */
        long indexedPosition = (index * elementSize) + offset;
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
        ArrayBufferObject buffer = getBuffer();
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
        /* step 13 */
        int elementSize = elementType.size();
        /* step 14 */
        long indexedPosition = (index * elementSize) + offset;
        /* steps 16-17 */
        SetValueInBuffer(buffer, indexedPosition, elementType, numValue);
        /* step 18 */
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
        assert buffer != null : "ArrayBufferObject not initialized";
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
