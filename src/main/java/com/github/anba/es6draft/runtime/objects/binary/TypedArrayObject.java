/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

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
public class TypedArrayObject extends ExoticIntegerIndexedObject implements ArrayBufferView {
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

    // FIXME: spec incomplete
    private boolean elementsWritable = true;

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
    protected boolean getWritable() {
        return elementsWritable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setNonWritable() {
        assert elementsWritable;
        elementsWritable = false;
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
            throw newTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* step 5 */
        long length = getArrayLength();
        /* step 6 */
        if (index < 0 || index >= length) {
            return UNDEFINED;
        }
        /* step 7 */
        long offset = getByteOffset();
        /* steps 8, 11 */
        ElementType elementType = getElementType();
        /* step 9 */
        int elementSize = elementType.size();
        /* step 10 */
        long indexedPosition = (long) ((index * elementSize) + offset);
        /* step 12 */
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
            throw newTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* step 5 */
        long length = getArrayLength();
        /* steps 6-7 */
        double numValue = ToNumber(cx, value);
        /* step 8 */
        if (index < 0 || index >= length) {
            return false;
        }
        /* step 9 */
        long offset = getByteOffset();
        /* steps 10, 13 */
        ElementType elementType = getElementType();
        /* step 11 */
        int elementSize = elementType.size();
        /* step 12 */
        long indexedPosition = (long) ((index * elementSize) + offset);
        /* steps 14-15 */
        SetValueInBuffer(cx, buffer, indexedPosition, elementType, numValue);
        /* step 16 */
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
     */
    public void setBuffer(ArrayBufferObject buffer) {
        assert buffer != null && buffer.getData() != null;
        assert this.buffer == null;
        this.buffer = buffer;
    }

    /**
     * [[ElementType]]
     */
    public ElementType getElementType() {
        return elementType;
    }

    /**
     * [[ElementType]]
     */
    public void setElementType(ElementType elementType) {
        assert elementType != null;
        this.elementType = elementType;
    }

    /**
     * [[TypedArrayName]]
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
        assert arrayLength >= 0;
        this.arrayLength = arrayLength;
    }
}
