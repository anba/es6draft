/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
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
        super(realm, prototype);
        assert elementType != null && buffer != null : "cannot initialize TypedArrayObject with null";
        assert byteLength >= 0 : "negative byte length: " + byteLength;
        assert byteOffset >= 0 : "negative byte offset: " + byteOffset;
        assert arrayLength >= 0 : "negative array length: " + arrayLength;
        assert buffer.isDetached() || (byteOffset + byteLength <= buffer.getByteLength());
        assert arrayLength * elementType.size() == byteLength : "invalid length: " + byteLength;
        this.elementType = elementType;
        this.buffer = buffer;
        this.byteLength = byteLength;
        this.byteOffset = byteOffset;
        this.arrayLength = arrayLength;
    }

    private long byteIndex(long index) {
        return (index << elementType.shift()) + byteOffset;
    }

    @Override
    public long getLength() {
        return getArrayLength();
    }

    @Override
    protected boolean elementHas(ExecutionContext cx, long index) {
        assert index >= 0;
        // 9.4.5.2 [[HasProperty]](P)
        /* step 3.b.i */
        ArrayBuffer buffer = getBuffer();
        /* step 3.b.ii */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 3.b.iii-vii */
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
        /* steps 9-14 */
        return GetValueFromBuffer(buffer, byteIndex(index), getElementType());
    }

    Number elementGetMaybeDetached(ExecutionContext cx, long index) {
        assert 0 <= index && index < getArrayLength();
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBuffer buffer = getBuffer();
        /* step 4 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 5-8 (not applicable) */
        /* steps 9-14 */
        return GetValueFromBuffer(buffer, byteIndex(index), getElementType());
    }

    Number elementGetUnchecked(long index) {
        assert 0 <= index && index < getArrayLength();
        /* steps 1-2 (not applicable) */
        /* step 3 */
        ArrayBuffer buffer = getBuffer();
        /* step 4 */
        assert !IsDetachedBuffer(buffer);
        /* steps 5-8 (not applicable) */
        /* steps 9-14 */
        return GetValueFromBuffer(buffer, byteIndex(index), getElementType());
    }

    @Override
    protected boolean elementSet(ExecutionContext cx, long index, Object value) {
        assert index >= 0;
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Number numValue = elementType.toElementValue(cx, value);
        /* step 4 */
        ArrayBuffer buffer = getBuffer();
        /* step 5 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 6-9 */
        if (index >= getArrayLength()) {
            return false;
        }
        /* steps 10-15 */
        SetValueInBuffer(buffer, byteIndex(index), getElementType(), numValue);
        /* step 16 */
        return true;
    }

    void elementSetMaybeDetached(ExecutionContext cx, long index, Object value) {
        assert 0 <= index && index < getArrayLength();
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Number numValue = elementType.toElementValue(cx, value);
        /* step 4 */
        ArrayBuffer buffer = getBuffer();
        /* step 5 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 6-9 (not applicable) */
        /* steps 10-15 */
        SetValueInBuffer(buffer, byteIndex(index), getElementType(), numValue);
        /* step 16 (return) */
    }

    void elementSetUnchecked(ExecutionContext cx, long index, Object value) {
        assert 0 <= index && index < getArrayLength();
        /* steps 1-2 (not applicable) */
        /* step 3 */
        Number numValue = elementType.toElementValue(cx, value);
        /* step 4 */
        ArrayBuffer buffer = getBuffer();
        /* step 5 */
        assert !IsDetachedBuffer(buffer);
        /* steps 6-9 (not applicable) */
        /* steps 10-15 */
        SetValueInBuffer(buffer, byteIndex(index), getElementType(), numValue);
        /* step 16 (return) */
    }

    void elementSetUnchecked(long index, Number numValue) {
        assert 0 <= index && index < getArrayLength();
        /* steps 1-3 (not applicable) */
        /* step 4 */
        ArrayBuffer buffer = getBuffer();
        /* step 5 */
        assert !IsDetachedBuffer(buffer);
        /* steps 6-9 (not applicable) */
        /* steps 10-15 */
        assert elementType.isInt64() ? Type.isBigInt(numValue) : Type.isNumber(numValue);
        SetValueInBuffer(buffer, byteIndex(index), getElementType(), numValue);
        /* step 16 (return) */
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

    @Override
    public String toString() {
        return String.format(
                "%s, elementType=%s, buffer={data=%s, byteLength=%s, detached=%b}, byteLength=%d, byteOffset=%d, arrayLength=%d",
                super.toString(), elementType, buffer.getData(), buffer.getByteLength(), buffer.isDetached(),
                byteLength, byteOffset, arrayLength);
    }

    TypedArrayFunctions functions() {
        return TypedArrayFunctions.functions(this);
    }

    List<? extends Number> toList() {
        return functions().toList(this);
    }
}
