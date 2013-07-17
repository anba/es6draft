/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.builtins.ExoticIntegerIndexedObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.6 TypedArray Object Structures</h3>
 * <ul>
 * <li>15.13.6.7 Properties of TypedArray instances
 * </ul>
 */
public class TypedArrayObject extends ExoticIntegerIndexedObject {
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
        ArrayBufferObject buffer = getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        long length = getArrayLength();
        if (index < 0 || index >= length) {
            return UNDEFINED;
        }
        long offset = getByteOffset();
        ElementType elementType = getElementType();
        int elementSize = elementType.size();
        long indexedPosition = (long) ((index * elementSize) + offset);
        return GetValueFromBuffer(cx, buffer, indexedPosition, elementType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean elementSet(ExecutionContext cx, double index, Object value) {
        ArrayBufferObject buffer = getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        long length = getArrayLength();
        double numValue = ToNumber(cx, value);
        if (index < 0 || index >= length) {
            // FIXME: spec bug (elementSet) should return true/false
            // return numValue;
            return false;
        }
        long offset = getByteOffset();
        ElementType elementType = getElementType();
        int elementSize = elementType.size();
        long indexedPosition = (long) ((index * elementSize) + offset);
        SetValueInBuffer(cx, buffer, indexedPosition, elementType, numValue);
        // FIXME: spec bug (elementSet) should return true/false
        // return numValue;
        return true;
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
     * [[ElementType]]
     */
    public ElementType getElementType() {
        return elementType;
    }

    /**
     * [[ElementType]]
     */
    public void setElementType(ElementType elementType) {
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
