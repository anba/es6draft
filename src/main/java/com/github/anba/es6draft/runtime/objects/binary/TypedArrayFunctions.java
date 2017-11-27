/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.SameValueZero;
import static com.github.anba.es6draft.runtime.AbstractOperations.StrictEqualityComparison;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CloneArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Bytes;
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntType;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * 
 */
abstract class TypedArrayFunctions {
    private TypedArrayFunctions() {
    }

    static final TypedArrayFunctions functions(TypedArrayObject typedArray) {
        switch (typedArray.getElementType()) {
        case Int8:
            return INT8;
        case Uint8:
            return UINT8;
        case Uint8C:
            return UINT8C;
        case Int16:
            return INT16;
        case Uint16:
            return UINT16;
        case Int32:
            return INT32;
        case Uint32:
            return UINT32;
        case BigInt64:
            return INT64;
        case BigUint64:
            return UINT64;
        case Float32:
            return FLOAT32;
        case Float64:
            return FLOAT64;
        default:
            throw new AssertionError();
        }
    }

    @FunctionalInterface
    private interface ByteToIntFunction {
        int applyAsInt(byte value);
    }

    @FunctionalInterface
    private interface ByteToFloatFunction {
        float applyAsFloat(byte value);
    }

    @FunctionalInterface
    private interface ByteToDoubleFunction {
        double applyAsDouble(byte value);
    }

    @FunctionalInterface
    private interface ShortToIntFunction {
        int applyAsInt(short value);
    }

    @FunctionalInterface
    private interface ShortToFloatFunction {
        float applyAsFloat(short value);
    }

    @FunctionalInterface
    private interface ShortToDoubleFunction {
        double applyAsDouble(short value);
    }

    @FunctionalInterface
    private interface IntToByteFunction {
        byte applyAsByte(int value);
    }

    @FunctionalInterface
    private interface IntToShortFunction {
        short applyAsShort(int value);
    }

    @FunctionalInterface
    private interface IntToFloatFunction {
        float applyAsFloat(int value);
    }

    @FunctionalInterface
    private interface FloatToIntFunction {
        int applyAsInt(float value);
    }

    @FunctionalInterface
    private interface FloatToDoubleFunction {
        double applyAsDouble(float value);
    }

    @FunctionalInterface
    private interface FloatUnaryOperator {
        float applyAsFloat(float value);
    }

    private static final ByteBuffer byteBuffer(TypedArrayObject typedArray) {
        ByteBuffer data = typedArray.getBuffer().getData();
        assert data != null && data.order() == Bytes.DEFAULT_BYTE_ORDER;
        return data;
    }

    private static final ByteBuffer byteBuffer(ArrayBuffer arrayBuffer) {
        ByteBuffer data = arrayBuffer.getData();
        assert data != null && data.order() == Bytes.DEFAULT_BYTE_ORDER;
        return data;
    }

    private static final int length(TypedArrayObject typedArray) {
        long length = typedArray.getArrayLength();
        assert 0 <= length && length <= Integer.MAX_VALUE;
        return (int) length;
    }

    private static final int byteOffset(TypedArrayObject typedArray) {
        long byteOffset = typedArray.getByteOffset();
        assert 0 <= byteOffset && byteOffset <= Integer.MAX_VALUE;
        return (int) byteOffset;
    }

    private static final int byteLength(TypedArrayObject typedArray) {
        long byteLength = typedArray.getByteLength();
        assert 0 <= byteLength && byteLength <= Integer.MAX_VALUE;
        return (int) byteLength;
    }

    private static final int byteLength(ArrayBuffer arrayBuffer) {
        long byteLength = arrayBuffer.getByteLength();
        assert 0 <= byteLength && byteLength <= Integer.MAX_VALUE;
        return (int) byteLength;
    }

    private static final int index(long index) {
        assert 0 <= index && index <= Integer.MAX_VALUE;
        return (int) index;
    }

    final void construct(ExecutionContext cx, TypedArrayObject typedArray, ArrayBufferObject data,
            ElementType elementType) {
        if (typedArray.getArrayLength() > 0) {
            elementType.throwIfIncompatibleNumericType(cx, typedArray.getElementType());
        }
        switch (elementType) {
        case Int8:
            constructInt8(typedArray, data);
            break;
        case Uint8:
            constructUint8(typedArray, data);
            break;
        case Uint8C:
            constructUint8C(typedArray, data);
            break;
        case Int16:
            constructInt16(typedArray, data);
            break;
        case Uint16:
            constructUint16(typedArray, data);
            break;
        case Int32:
            constructInt32(typedArray, data);
            break;
        case Uint32:
            constructUint32(typedArray, data);
            break;
        case BigInt64:
            constructInt64(typedArray, data);
            break;
        case BigUint64:
            constructUint64(typedArray, data);
            break;
        case Float32:
            constructFloat32(typedArray, data);
            break;
        case Float64:
            constructFloat64(typedArray, data);
            break;
        default:
            throw new AssertionError();
        }
    }

    protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Int8, typedArray, target);
    }

    protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Uint8, typedArray, target);
    }

    protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Uint8C, typedArray, target);
    }

    protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Int16, typedArray, target);
    }

    protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Uint16, typedArray, target);
    }

    protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Int32, typedArray, target);
    }

    protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Uint32, typedArray, target);
    }

    protected void constructInt64(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.BigInt64, typedArray, target);
    }

    protected void constructUint64(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.BigUint64, typedArray, target);
    }

    protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Float32, typedArray, target);
    }

    protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
        constructGeneric(ElementType.Float64, typedArray, target);
    }

    protected final void constructGeneric(ElementType elementType, TypedArrayObject typedArray,
            ArrayBufferObject data) {
        /* step 6 */
        ArrayBuffer srcData = typedArray.getBuffer();
        /* step 10 */
        long elementLength = typedArray.getArrayLength();
        /* steps 11-12 */
        ElementType srcType = typedArray.getElementType();
        /* step 13 */
        int srcElementSize = typedArray.getElementType().size();
        /* step 14 */
        long srcByteOffset = typedArray.getByteOffset();
        /* step 15 */
        int elementSize = elementType.size();

        /* step 18.d */
        long srcByteIndex = srcByteOffset;
        /* step 18.e */
        long targetByteIndex = 0;
        /* steps 18.f-g */
        assert elementLength == 0 || elementType.isCompatibleNumericType(srcType);
        for (long count = elementLength; count > 0; --count) {
            Number value = GetValueFromBuffer(srcData, srcByteIndex, srcType);
            SetValueInBuffer(data, targetByteIndex, elementType, value);
            srcByteIndex += srcElementSize;
            targetByteIndex += elementSize;
        }
    }

    protected final void constructBitwise(TypedArrayObject typedArray, ArrayBufferObject target) {
        int countByteLength = typedArray.getElementType().toBytes(length(typedArray));
        memmove(byteBuffer(typedArray), byteOffset(typedArray), byteBuffer(target), 0, countByteLength);
    }

    final void copyWithin(TypedArrayObject typedArray, long to, long from, long count) {
        assert 0 <= to && to + count <= typedArray.getArrayLength();
        assert 0 <= from && from + count <= typedArray.getArrayLength();

        int byteOffset = byteOffset(typedArray);
        int elementShift = typedArray.getElementType().shift();
        int toByteIndex = index(byteOffset + (to << elementShift));
        int fromByteIndex = index(byteOffset + (from << elementShift));
        int countByteLength = index(count << elementShift);
        ByteBuffer data = byteBuffer(typedArray);

        memmove(data, fromByteIndex, data, toByteIndex, countByteLength);
    }

    void fill(TypedArrayObject typedArray, Number value, long start, long end) {
        int k = index(start), finall = index(end);
        for (; k < finall; ++k) {
            typedArray.elementSetUnchecked(k, value);
        }
    }

    boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
        int length = length(typedArray);
        for (int k = index(fromIndex); k < length; ++k) {
            if (SameValueZero(searchElement, typedArray.elementGetUnchecked(k))) {
                return true;
            }
        }
        return false;
    }

    int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
        int length = length(typedArray);
        for (int k = index(fromIndex); k < length; ++k) {
            if (StrictEqualityComparison(searchElement, typedArray.elementGetUnchecked(k))) {
                return k;
            }
        }
        return -1;
    }

    String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
        int length = length(typedArray);
        /* step 6 */
        Number element0 = typedArray.elementGetUnchecked(0);
        /* step 7 */
        r.append(ToString(element0));
        /* steps 8-9 */
        for (int k = 1; k < length; ++k) {
            Number element = typedArray.elementGetUnchecked(k);
            r.append(separator).append(ToString(element));
        }
        /* step 10 */
        return r.toString();
    }

    int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
        for (int k = index(fromIndex); k >= 0; --k) {
            if (StrictEqualityComparison(searchElement, typedArray.elementGetUnchecked(k))) {
                return k;
            }
        }
        return -1;
    }

    void reverse(TypedArrayObject typedArray) {
        /* step 2 */
        int len = length(typedArray);
        /* step 3 */
        final int middle = len >>> 1;
        /* steps 4-5 */
        for (int lower = 0; lower != middle; ++lower) {
            int upper = len - lower - 1;
            int upperP = upper;
            int lowerP = lower;
            Number lowerValue = typedArray.elementGetUnchecked(lowerP);
            Number upperValue = typedArray.elementGetUnchecked(upperP);

            typedArray.elementSetUnchecked(lowerP, upperValue);
            typedArray.elementSetUnchecked(upperP, lowerValue);
        }
    }

    final void set(ExecutionContext cx, TypedArrayObject typedArray, TypedArrayObject target, long targetOffset) {
        /* step 8 */
        ArrayBuffer targetBuffer = target.getBuffer();
        /* step 11 */
        ArrayBuffer srcBuffer = typedArray.getBuffer();
        /* steps 13-19 (omitted) */
        /* step 20 */
        int srcLength = length(typedArray);
        /* step 21 */
        long srcByteOffset = typedArray.getByteOffset();
        /* steps 23-24 */
        ArrayBuffer clonedSrcBuffer = cloneArrayBuffer(cx, typedArray, target, targetOffset);
        assert !IsDetachedBuffer(targetBuffer);
        int srcByteIndex;
        if (clonedSrcBuffer != srcBuffer) {
            srcBuffer = clonedSrcBuffer;
            srcByteIndex = 0;
        } else {
            srcByteIndex = index(srcByteOffset);
        }
        int targetIndex = index(targetOffset);
        /* steps 26-28 */
        if (srcLength > 0) {
            target.getElementType().throwIfIncompatibleNumericType(cx, typedArray.getElementType());
        }
        switch (target.getElementType()) {
        case Int8:
            setInt8(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Uint8:
            setUint8(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Uint8C:
            setUint8C(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Int16:
            setInt16(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Uint16:
            setUint16(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Int32:
            setInt32(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Uint32:
            setUint32(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case BigInt64:
            setInt64(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case BigUint64:
            setUint64(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Float32:
            setFloat32(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        case Float64:
            setFloat64(srcBuffer, srcByteIndex, target, targetIndex, srcLength);
            break;
        default:
            throw new AssertionError();
        }
    }

    private static ArrayBuffer cloneArrayBuffer(ExecutionContext cx, TypedArrayObject typedArray,
            TypedArrayObject target, long targetOffset) {
        ArrayBuffer targetBuffer = target.getBuffer();
        ArrayBuffer srcBuffer = typedArray.getBuffer();

        if (srcBuffer.sameData(targetBuffer) && isOverlappingSet(typedArray, target, targetOffset)) {
            return CloneArrayBuffer(cx, srcBuffer, typedArray.getByteOffset(), typedArray.getByteLength(),
                    (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer));
        }
        return srcBuffer;
    }

    private static boolean isOverlappingSet(TypedArrayObject typedArray, TypedArrayObject target, long targetOffset) {
        long length = typedArray.getArrayLength();
        long srcBytes = typedArray.getElementType().toBytes(length);
        long targetBytes = target.getElementType().toBytes(length);
        long srcByteOffset = typedArray.getByteOffset();
        long targetByteOffset = target.getByteOffset() + target.getElementType().toBytes(targetOffset);

        if (srcByteOffset < targetByteOffset && (srcByteOffset + srcBytes) <= targetByteOffset) {
            return false;
        }
        if (targetByteOffset < srcByteOffset && (targetByteOffset + targetBytes) <= srcByteOffset) {
            return false;
        }
        return true;
    }

    protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Int8, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Uint8, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Uint8C, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Int16, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Uint16, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Int32, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Uint32, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setInt64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.BigInt64, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setUint64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.BigUint64, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Float32, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
        setGeneric(ElementType.Float64, srcBuffer, srcIndex, dest, destPos, length);
    }

    protected final void setGeneric(ElementType srcType, ArrayBuffer srcBuffer, int srcByteIndex,
            TypedArrayObject target, int targetOffset, int length) {
        /* step 8 */
        ArrayBuffer targetBuffer = target.getBuffer();
        /* steps 13-14 */
        ElementType targetType = target.getElementType();
        /* step 15 */
        int targetElementSize = targetType.size();
        int targetElementShift = targetType.shift();
        /* step 16 */
        long targetByteOffset = target.getByteOffset();
        /* step 19 */
        int srcElementSize = srcType.size();
        /* step 25 */
        long targetByteIndex = targetByteOffset + (targetOffset << targetElementShift);
        /* step 26 */
        long limit = targetByteIndex + (length << targetElementShift);
        /* step 28 */
        assert length == 0 || targetType.isCompatibleNumericType(srcType);
        for (; targetByteIndex < limit; srcByteIndex += srcElementSize, targetByteIndex += targetElementSize) {
            /* step 28.a */
            Number value = GetValueFromBuffer(srcBuffer, srcByteIndex, srcType);
            /* step 28.b */
            SetValueInBuffer(targetBuffer, targetByteIndex, targetType, value);
        }
    }

    protected final void setBitwise(ArrayBuffer srcBuffer, int srcByteIndex, TypedArrayObject target, int targetOffset,
            int length) {
        /* steps 13-15 */
        int targetElementShift = target.getElementType().shift();
        /* step 16 */
        int targetByteOffset = byteOffset(target);
        /* step 25 */
        int targetByteIndex = index(targetByteOffset + (targetOffset << targetElementShift));
        int countByteLength = index(length << targetElementShift);
        memmove(byteBuffer(srcBuffer), index(srcByteIndex), byteBuffer(target), targetByteIndex, countByteLength);
    }

    final void slice(TypedArrayObject source, TypedArrayObject target, long start, long end) {
        assert 0 <= start && start < end && end <= source.getArrayLength();
        assert (end - start) <= target.getArrayLength();
        assert !IsDetachedBuffer(source.getBuffer());
        assert !IsDetachedBuffer(target.getBuffer());
        assert target.getElementType().isCompatibleNumericType(source.getElementType());

        int startIndex = index(start), targetIndex = 0, length = index(end - start);
        switch (target.getElementType()) {
        case Int8:
            sliceInt8(source, startIndex, target, targetIndex, length);
            break;
        case Uint8:
            sliceUint8(source, startIndex, target, targetIndex, length);
            break;
        case Uint8C:
            sliceUint8C(source, startIndex, target, targetIndex, length);
            break;
        case Int16:
            sliceInt16(source, startIndex, target, targetIndex, length);
            break;
        case Uint16:
            sliceUint16(source, startIndex, target, targetIndex, length);
            break;
        case Int32:
            sliceInt32(source, startIndex, target, targetIndex, length);
            break;
        case Uint32:
            sliceUint32(source, startIndex, target, targetIndex, length);
            break;
        case BigInt64:
            sliceInt64(source, startIndex, target, targetIndex, length);
            break;
        case BigUint64:
            sliceUint64(source, startIndex, target, targetIndex, length);
            break;
        case Float32:
            sliceFloat32(source, startIndex, target, targetIndex, length);
            break;
        case Float64:
            sliceFloat64(source, startIndex, target, targetIndex, length);
            break;
        default:
            throw new AssertionError();
        }
    }

    protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceInt64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceUint64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
        sliceGeneric(src, srcPos, dest, destPos, length);
    }

    protected final void sliceGeneric(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int len) {
        assert dest.getElementType().isCompatibleNumericType(src.getElementType());
        for (int n = destPos, k = srcPos; k < srcPos + len; ++k, ++n) {
            /* steps 14.b.i-ii */
            Number kvalue = src.elementGetUnchecked(k);
            /* step 14.b.iii */
            dest.elementSetUnchecked(n, kvalue);
        }
    }

    protected final void sliceBitwise(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int len) {
        /* step 15 */
        /* steps 15.a-c (not applicable) */
        /* step 15.d */
        int elementShift = src.getElementType().shift();
        /* step 15.e (note) */
        /* steps 15.f, 15.h */
        int srcByteIndex = index(byteOffset(src) + (srcPos << elementShift));
        /* step 15.g */
        int targetByteIndex = index(byteOffset(dest) + (destPos << elementShift));
        /* steps 15.i-j */
        int countByteLength = index(len << elementShift);
        if (countByteLength > 0) {
            ByteBuffer srcBuffer = byteBuffer(src);
            ByteBuffer destBuffer = byteBuffer(dest);
            if (!src.getBuffer().sameData(dest.getBuffer())) {
                memmove(srcBuffer, srcByteIndex, destBuffer, targetByteIndex, countByteLength);
            } else {
                int limit = targetByteIndex + countByteLength;
                while (targetByteIndex < limit) {
                    destBuffer.put(targetByteIndex++, srcBuffer.get(srcByteIndex++));
                }
            }
        }
    }

    private static final void memmove(ByteBuffer src, int srcIndex, ByteBuffer dest, int destIndex, int length) {
        assert (srcIndex + length) <= src.capacity();
        assert (destIndex + length) <= dest.capacity();

        if (src == dest) {
            src = src.duplicate();
        }
        src.limit(srcIndex + length).position(srcIndex);
        dest.limit(destIndex + length).position(destIndex);
        dest.put(src);
        src.clear();
        dest.clear();
    }

    private static final void put(IntUnaryOperator src, int srcPos, ByteBuffer dest, int destPos, int length) {
        for (int n = destPos, k = srcPos; k < srcPos + length;) {
            dest.put(n++, (byte) src.applyAsInt(k++));
        }
    }

    private static final void put(IntUnaryOperator src, int srcPos, ShortBuffer dest, int destPos, int length) {
        for (int n = destPos, k = srcPos; k < srcPos + length;) {
            dest.put(n++, (short) src.applyAsInt(k++));
        }
    }

    private static final void put(IntUnaryOperator src, int srcPos, IntBuffer dest, int destPos, int length) {
        for (int n = destPos, k = srcPos; k < srcPos + length;) {
            dest.put(n++, src.applyAsInt(k++));
        }
    }

    private static final void put(IntToFloatFunction src, int srcPos, FloatBuffer dest, int destPos, int length) {
        for (int n = destPos, k = srcPos; k < srcPos + length;) {
            dest.put(n++, src.applyAsFloat(k++));
        }
    }

    private static final void put(IntToDoubleFunction src, int srcPos, DoubleBuffer dest, int destPos, int length) {
        for (int n = destPos, k = srcPos; k < srcPos + length;) {
            dest.put(n++, src.applyAsDouble(k++));
        }
    }

    void sort(TypedArrayObject typedArray) {
        int length = length(typedArray);
        Number[] elements = new Number[length];
        for (int i = 0; i < length; ++i) {
            elements[i] = typedArray.elementGetUnchecked(i);
        }

        Arrays.sort(elements);

        for (int i = 0; i < length; ++i) {
            typedArray.elementSetUnchecked(i, elements[i]);
        }
    }

    void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
        int length = length(typedArray);
        Number[] elements = new Number[(int) length];
        for (int i = 0; i < length; ++i) {
            elements[i] = typedArray.elementGetUnchecked(i);
        }

        try {
            Arrays.sort(elements, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }

        assert !IsDetachedBuffer(typedArray.getBuffer());
        for (int i = 0; i < length; ++i) {
            typedArray.elementSetUnchecked(i, elements[i]);
        }
    }

    private static final void sortArray(byte[] array, Comparator<? super Integer> comparator) {
        Integer[] boxed = new Integer[array.length];
        for (int i = 0; i < array.length; ++i) {
            boxed[i] = (int) array[i];
        }
        try {
            Arrays.sort(boxed, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }
        for (int i = 0; i < array.length; ++i) {
            array[i] = (byte) (int) boxed[i];
        }
    }

    private static final void sortArray(short[] array, Comparator<? super Integer> comparator) {
        Integer[] boxed = new Integer[array.length];
        for (int i = 0; i < array.length; ++i) {
            boxed[i] = (int) array[i];
        }
        try {
            Arrays.sort(boxed, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }
        for (int i = 0; i < array.length; ++i) {
            array[i] = (short) (int) boxed[i];
        }
    }

    private static final void sortArray(int[] array, Comparator<? super Integer> comparator) {
        Integer[] boxed = new Integer[array.length];
        for (int i = 0; i < array.length; ++i) {
            boxed[i] = array[i];
        }
        try {
            Arrays.sort(boxed, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }
        for (int i = 0; i < array.length; ++i) {
            array[i] = boxed[i];
        }
    }

    private static final void sortArray(long[] array, Comparator<? super Long> comparator) {
        Long[] boxed = new Long[array.length];
        for (int i = 0; i < array.length; ++i) {
            boxed[i] = array[i];
        }
        try {
            Arrays.sort(boxed, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }
        for (int i = 0; i < array.length; ++i) {
            array[i] = boxed[i];
        }
    }

    private static final void sortArrayAsBigInt(long[] array, LongFunction<BigInteger> toBigInt,
            Comparator<? super BigInteger> comparator) {
        BigInteger[] boxed = new BigInteger[array.length];
        for (int i = 0; i < array.length; ++i) {
            boxed[i] = toBigInt.apply(array[i]);
        }
        try {
            Arrays.sort(boxed, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }
        for (int i = 0; i < array.length; ++i) {
            array[i] = boxed[i].longValue();
        }
    }

    private static final void sortArray(float[] array, Comparator<? super Double> comparator) {
        Double[] boxed = new Double[array.length];
        for (int i = 0; i < array.length; ++i) {
            boxed[i] = (double) array[i];
        }
        try {
            Arrays.sort(boxed, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }
        for (int i = 0; i < array.length; ++i) {
            array[i] = (float) (double) boxed[i];
        }
    }

    private static final void sortArray(double[] array, Comparator<? super Double> comparator) {
        Double[] boxed = new Double[array.length];
        for (int i = 0; i < array.length; ++i) {
            boxed[i] = array[i];
        }
        try {
            Arrays.sort(boxed, comparator);
        } catch (IllegalArgumentException e) {
            // Ignore: `IllegalArgumentException: Comparison method violates its general contract!`
        }
        for (int i = 0; i < array.length; ++i) {
            array[i] = boxed[i];
        }
    }

    private static final void sortArrayUnsigned(byte[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Byte.MIN_VALUE;
        }
        Arrays.sort(array);
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Byte.MIN_VALUE;
        }
    }

    private static final void sortArrayUnsigned(byte[] array, Comparator<? super Integer> comparator) {
        int[] elements = new int[array.length];
        for (int i = 0; i < array.length; ++i) {
            elements[i] = array[i] & 0xff;
        }
        sortArray(elements, comparator);
        for (int i = 0; i < array.length; ++i) {
            array[i] = (byte) elements[i];
        }
    }

    private static final void sortArrayUnsigned(short[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Short.MIN_VALUE;
        }
        Arrays.sort(array);
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Short.MIN_VALUE;
        }
    }

    private static final void sortArrayUnsigned(short[] array, Comparator<? super Integer> comparator) {
        int[] elements = new int[array.length];
        for (int i = 0; i < array.length; ++i) {
            elements[i] = array[i] & 0xffff;
        }
        sortArray(elements, comparator);
        for (int i = 0; i < array.length; ++i) {
            array[i] = (short) elements[i];
        }
    }

    private static final void sortArrayUnsigned(int[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Integer.MIN_VALUE;
        }
        Arrays.sort(array);
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Integer.MIN_VALUE;
        }
    }

    private static final void sortArrayUnsigned(int[] array, Comparator<? super Long> comparator) {
        long[] elements = new long[array.length];
        for (int i = 0; i < array.length; ++i) {
            elements[i] = array[i] & 0xffff_ffffL;
        }
        sortArray(elements, comparator);
        for (int i = 0; i < array.length; ++i) {
            array[i] = (int) elements[i];
        }
    }

    private static final void sortArrayUnsigned(long[] array) {
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Long.MIN_VALUE;
        }
        Arrays.sort(array);
        for (int i = 0; i < array.length; ++i) {
            array[i] ^= Long.MIN_VALUE;
        }
    }

    private static final ByteBuffer asByteBuffer(TypedArrayObject typedArray) {
        ByteBuffer data = byteBuffer(typedArray);
        int byteOffset = byteOffset(typedArray);
        int byteLength = byteLength(typedArray);

        data.limit(byteOffset + byteLength).position(byteOffset);
        ByteBuffer view = data.slice();
        data.clear();
        return view;
    }

    private static final ByteBuffer asByteBuffer(ArrayBuffer buffer) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = byteLength(buffer);

        data.limit(byteLength).position(0);
        ByteBuffer view = data.slice();
        data.clear();
        return view;
    }

    private static final ByteBuffer asByteBuffer(ArrayBuffer buffer, int byteOffset, int length) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = Byte.BYTES * length;

        data.limit(byteOffset + byteLength).position(byteOffset);
        ByteBuffer view = data.slice();
        data.clear();
        return view;
    }

    private static final ShortBuffer asShortBuffer(TypedArrayObject typedArray) {
        ByteBuffer data = byteBuffer(typedArray);
        int byteOffset = byteOffset(typedArray);
        int byteLength = byteLength(typedArray);

        data.limit(byteOffset + byteLength).position(byteOffset);
        ShortBuffer view = data.asShortBuffer();
        data.clear();
        return view;
    }

    private static final ShortBuffer asShortBuffer(ArrayBuffer buffer) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = byteLength(buffer);

        data.limit(byteLength).position(0);
        ShortBuffer view = data.asShortBuffer();
        data.clear();
        return view;
    }

    private static final ShortBuffer asShortBuffer(ArrayBuffer buffer, int byteOffset, int length) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = Short.BYTES * length;

        data.limit(byteOffset + byteLength).position(byteOffset);
        ShortBuffer view = data.asShortBuffer();
        data.clear();
        return view;
    }

    private static final IntBuffer asIntBuffer(TypedArrayObject typedArray) {
        ByteBuffer data = byteBuffer(typedArray);
        int byteOffset = byteOffset(typedArray);
        int byteLength = byteLength(typedArray);

        data.limit(byteOffset + byteLength).position(byteOffset);
        IntBuffer view = data.asIntBuffer();
        data.clear();
        return view;
    }

    private static final IntBuffer asIntBuffer(ArrayBuffer buffer) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = byteLength(buffer);

        data.limit(byteLength).position(0);
        IntBuffer view = data.asIntBuffer();
        data.clear();
        return view;
    }

    private static final IntBuffer asIntBuffer(ArrayBuffer buffer, int byteOffset, int length) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = Integer.BYTES * length;

        data.limit(byteOffset + byteLength).position(byteOffset);
        IntBuffer view = data.asIntBuffer();
        data.clear();
        return view;
    }

    private static final LongBuffer asLongBuffer(TypedArrayObject typedArray) {
        ByteBuffer data = byteBuffer(typedArray);
        int byteOffset = byteOffset(typedArray);
        int byteLength = byteLength(typedArray);

        data.limit(byteOffset + byteLength).position(byteOffset);
        LongBuffer view = data.asLongBuffer();
        data.clear();
        return view;
    }

    private static final FloatBuffer asFloatBuffer(TypedArrayObject typedArray) {
        ByteBuffer data = byteBuffer(typedArray);
        int byteOffset = byteOffset(typedArray);
        int byteLength = byteLength(typedArray);

        data.limit(byteOffset + byteLength).position(byteOffset);
        FloatBuffer view = data.asFloatBuffer();
        data.clear();
        return view;
    }

    private static final FloatBuffer asFloatBuffer(ArrayBuffer buffer) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = byteLength(buffer);

        data.limit(byteLength).position(0);
        FloatBuffer view = data.asFloatBuffer();
        data.clear();
        return view;
    }

    private static final FloatBuffer asFloatBuffer(ArrayBuffer buffer, int byteOffset, int length) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = Float.BYTES * length;

        data.limit(byteOffset + byteLength).position(byteOffset);
        FloatBuffer view = data.asFloatBuffer();
        data.clear();
        return view;
    }

    private static final DoubleBuffer asDoubleBuffer(TypedArrayObject typedArray) {
        ByteBuffer data = byteBuffer(typedArray);
        int byteOffset = byteOffset(typedArray);
        int byteLength = byteLength(typedArray);

        data.limit(byteOffset + byteLength).position(byteOffset);
        DoubleBuffer view = data.asDoubleBuffer();
        data.clear();
        return view;
    }

    private static final DoubleBuffer asDoubleBuffer(ArrayBuffer buffer) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = byteLength(buffer);

        data.limit(byteLength).position(0);
        DoubleBuffer view = data.asDoubleBuffer();
        data.clear();
        return view;
    }

    abstract List<? extends Number> toList(TypedArrayObject typedArray);

    private static final DoubleBuffer asDoubleBuffer(ArrayBuffer buffer, int byteOffset, int length) {
        ByteBuffer data = byteBuffer(buffer);
        int byteLength = Double.BYTES * length;

        data.limit(byteOffset + byteLength).position(byteOffset);
        DoubleBuffer view = data.asDoubleBuffer();
        data.clear();
        return view;
    }

    private static final class ByteArrayList extends AbstractList<Integer> implements RandomAccess {
        private final byte[] array;
        private final ByteToIntFunction mapper;

        ByteArrayList(byte[] array) {
            this(array, v -> v);
        }

        ByteArrayList(byte[] array, ByteToIntFunction mapper) {
            this.array = array;
            this.mapper = mapper;
        }

        @Override
        public Integer get(int index) {
            return mapper.applyAsInt(array[index]);
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static final class ShortArrayList extends AbstractList<Integer> implements RandomAccess {
        private final short[] array;
        private final ShortToIntFunction mapper;

        ShortArrayList(short[] array) {
            this(array, v -> v);
        }

        ShortArrayList(short[] array, ShortToIntFunction mapper) {
            this.array = array;
            this.mapper = mapper;
        }

        @Override
        public Integer get(int index) {
            return mapper.applyAsInt(array[index]);
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static final class IntArrayList extends AbstractList<Integer> implements RandomAccess {
        private final int[] array;

        IntArrayList(int[] array) {
            this.array = array;
        }

        @Override
        public Integer get(int index) {
            return array[index];
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static final class UnsignedIntArrayList extends AbstractList<Long> implements RandomAccess {
        private final int[] array;

        UnsignedIntArrayList(int[] array) {
            this.array = array;
        }

        @Override
        public Long get(int index) {
            return Integer.toUnsignedLong(array[index]);
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static final class BigIntArrayList extends AbstractList<BigInteger> implements RandomAccess {
        private final long[] array;
        private final LongFunction<BigInteger> mapper;

        BigIntArrayList(long[] array) {
            this(array, BigInteger::valueOf);
        }

        BigIntArrayList(long[] array, LongFunction<BigInteger> mapper) {
            this.array = array;
            this.mapper = mapper;
        }

        @Override
        public BigInteger get(int index) {
            return mapper.apply(array[index]);
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static final class FloatArrayList extends AbstractList<Double> implements RandomAccess {
        private final float[] array;

        FloatArrayList(float[] array) {
            this.array = array;
        }

        @Override
        public Double get(int index) {
            return (double) array[index];
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static final class DoubleArrayList extends AbstractList<Double> implements RandomAccess {
        private final double[] array;

        DoubleArrayList(double[] array) {
            this.array = array;
        }

        @Override
        public Double get(int index) {
            return array[index];
        }

        @Override
        public int size() {
            return array.length;
        }
    }

    private static final TypedArrayFunctions INT8 = new TypedArrayFunctions() {
        private final boolean isInt8(double v, byte b) {
            return !(v < Byte.MIN_VALUE || v > Byte.MAX_VALUE || b != v);
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(((IntUnaryOperator) asByteBuffer(typedArray)::get).andThen(ElementType::ToUint8Clamp), 0,
                    asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asByteBuffer(typedArray)::get, 0, asShortBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt16(typedArray, target);
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asByteBuffer(typedArray)::get, 0, asIntBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt32(typedArray, target);
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asByteBuffer(typedArray)::get, 0, asFloatBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asByteBuffer(typedArray)::get, 0, asDoubleBuffer(target), 0, length(typedArray));
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte v = ElementType.ToInt8(value.doubleValue());
            if (data.hasArray()) {
                int arrayOffset = data.arrayOffset();
                Arrays.fill(data.array(), arrayOffset + index(start), arrayOffset + index(end), v);
            } else {
                for (int k = index(start), e = index(end); k < e; ++k) {
                    data.put(k, v);
                }
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            byte element = ElementType.ToInt8(searchElement.doubleValue());
            if (isInt8(searchElement.doubleValue(), element)) {
                ByteBuffer data = asByteBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            ByteBuffer data = asByteBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(data.get());
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            byte element = ElementType.ToInt8(searchElement.doubleValue());
            if (isInt8(searchElement.doubleValue(), element)) {
                ByteBuffer data = asByteBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            ByteBuffer data = asByteBuffer(typedArray);
            final int len = length(typedArray), middle = len >>> 1;
            for (int lower = 0, upper = len - 1; lower != middle; ++lower, --upper) {
                byte lowerValue = data.get(lower);
                byte upperValue = data.get(upper);
                data.put(lower, upperValue);
                data.put(upper, lowerValue);
            }
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asByteBuffer(srcBuffer, srcIndex, length)::get).andThen(ElementType::ToUint8Clamp),
                    0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(srcBuffer, srcIndex, length)::get, 0, asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt16(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(srcBuffer, srcIndex, length)::get, 0, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt32(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(srcBuffer, srcIndex, length)::get, 0, asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(srcBuffer, srcIndex, length)::get, 0, asDoubleBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asByteBuffer(src)::get).andThen(ElementType::ToUint8Clamp), srcPos,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(src)::get, srcPos, asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt16(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(src)::get, srcPos, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt32(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(src)::get, srcPos, asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asByteBuffer(src)::get, srcPos, asDoubleBuffer(dest), destPos, length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            ByteBuffer data = asByteBuffer(typedArray);
            if (data.hasArray()) {
                int arrayOffset = data.arrayOffset();
                Arrays.sort(data.array(), arrayOffset, arrayOffset + data.limit());
            } else {
                byte[] ba = new byte[data.remaining()];
                data.get(ba);
                Arrays.sort(ba);
                data.rewind();
                data.put(ba);
            }
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte[] ba = new byte[data.remaining()];
            data.get(ba);
            sortArray(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Integer> toList(TypedArrayObject typedArray) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte[] ba = new byte[data.remaining()];
            data.get(ba);
            return new ByteArrayList(ba);
        }
    };

    private static final TypedArrayFunctions UINT8 = new TypedArrayFunctions() {
        private final boolean isUint8(double v, byte b) {
            return !(v < 0 || v > 255 || Byte.toUnsignedInt(b) != v);
        }

        private final IntUnaryOperator andThenInt(IntToByteFunction fn, ByteToIntFunction after) {
            return t -> after.applyAsInt(fn.applyAsByte(t));
        }

        private final IntToFloatFunction andThenFloat(IntToByteFunction fn, ByteToFloatFunction after) {
            return t -> after.applyAsFloat(fn.applyAsByte(t));
        }

        private final IntToDoubleFunction andThenDouble(IntToByteFunction fn, ByteToDoubleFunction after) {
            return t -> after.applyAsDouble(fn.applyAsByte(t));
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asByteBuffer(typedArray)::get, Byte::toUnsignedInt), 0, asShortBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt16(typedArray, target);
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asByteBuffer(typedArray)::get, Byte::toUnsignedInt), 0, asIntBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt32(typedArray, target);
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenFloat(asByteBuffer(typedArray)::get, Byte::toUnsignedInt), 0, asFloatBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenDouble(asByteBuffer(typedArray)::get, Byte::toUnsignedInt), 0, asDoubleBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte v = ElementType.ToUint8(value.doubleValue());
            if (data.hasArray()) {
                int arrayOffset = data.arrayOffset();
                Arrays.fill(data.array(), arrayOffset + index(start), arrayOffset + index(end), v);
            } else {
                for (int k = index(start), e = index(end); k < e; ++k) {
                    data.put(k, v);
                }
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            byte element = ElementType.ToUint8(searchElement.doubleValue());
            if (isUint8(searchElement.doubleValue(), element)) {
                ByteBuffer data = asByteBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            ByteBuffer data = asByteBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(Byte.toUnsignedInt(data.get()));
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            byte element = ElementType.ToUint8(searchElement.doubleValue());
            if (isUint8(searchElement.doubleValue(), element)) {
                ByteBuffer data = asByteBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            INT8.reverse(typedArray);
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asByteBuffer(srcBuffer, srcIndex, length)::get, Byte::toUnsignedInt), 0, asShortBuffer(dest),
                    destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt16(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asByteBuffer(srcBuffer, srcIndex, length)::get, Byte::toUnsignedInt), 0, asIntBuffer(dest),
                    destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt32(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asByteBuffer(srcBuffer, srcIndex, length)::get, Byte::toUnsignedInt), 0,
                    asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asByteBuffer(srcBuffer, srcIndex, length)::get, Byte::toUnsignedInt), 0,
                    asDoubleBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asByteBuffer(src)::get, Byte::toUnsignedInt), srcPos, asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt16(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asByteBuffer(src)::get, Byte::toUnsignedInt), srcPos, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt32(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asByteBuffer(src)::get, Byte::toUnsignedInt), srcPos, asFloatBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asByteBuffer(src)::get, Byte::toUnsignedInt), srcPos, asDoubleBuffer(dest), destPos,
                    length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte[] ba = new byte[data.remaining()];
            data.get(ba);
            sortArrayUnsigned(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte[] ba = new byte[data.remaining()];
            data.get(ba);
            sortArrayUnsigned(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Integer> toList(TypedArrayObject typedArray) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte[] ba = new byte[data.remaining()];
            data.get(ba);
            return new ByteArrayList(ba, Byte::toUnsignedInt);
        }
    };

    private static final TypedArrayFunctions UINT8C = new TypedArrayFunctions() {
        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructInt8(typedArray, target);
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructUint8(typedArray, target);
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructUint8C(typedArray, target);
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructInt16(typedArray, target);
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructUint16(typedArray, target);
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructInt32(typedArray, target);
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructUint32(typedArray, target);
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructFloat32(typedArray, target);
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            UINT8.constructFloat64(typedArray, target);
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            ByteBuffer data = asByteBuffer(typedArray);
            byte v = ElementType.ToUint8Clamp(value.doubleValue());
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return UINT8.indexOf(typedArray, searchElement, fromIndex);
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            return UINT8.join(typedArray, separator, r);
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return UINT8.lastIndexOf(typedArray, searchElement, fromIndex);
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            UINT8.reverse(typedArray);
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setInt8(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setUint8(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setUint8C(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setInt16(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setUint16(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setInt32(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setUint32(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setFloat32(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            UINT8.setFloat64(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceInt8(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceUint8(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceUint8C(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceInt16(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceUint16(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceInt32(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceUint32(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceFloat32(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            UINT8.sliceFloat64(src, srcPos, dest, destPos, length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            UINT8.sort(typedArray);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            UINT8.sort(typedArray, comparator);
        }

        @SuppressWarnings("unchecked")
        @Override
        List<Integer> toList(TypedArrayObject typedArray) {
            return (List<Integer>) UINT8.toList(typedArray);
        }
    };

    private static final TypedArrayFunctions INT16 = new TypedArrayFunctions() {
        private final boolean isInt16(double v, short b) {
            return !(v < Short.MIN_VALUE || v > Short.MAX_VALUE || b != v);
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asShortBuffer(typedArray)::get, 0, asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt8(typedArray, target);
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(((IntUnaryOperator) asShortBuffer(typedArray)::get).andThen(ElementType::ToUint8Clamp), 0,
                    asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asShortBuffer(typedArray)::get, 0, asIntBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt32(typedArray, target);
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asShortBuffer(typedArray)::get, 0, asFloatBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asShortBuffer(typedArray)::get, 0, asDoubleBuffer(target), 0, length(typedArray));
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            ShortBuffer data = asShortBuffer(typedArray);
            short v = ElementType.ToInt16(value.doubleValue());
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            short element = ElementType.ToInt16(searchElement.doubleValue());
            if (isInt16(searchElement.doubleValue(), element)) {
                ShortBuffer data = asShortBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            ShortBuffer data = asShortBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(data.get());
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            short element = ElementType.ToInt16(searchElement.doubleValue());
            if (isInt16(searchElement.doubleValue(), element)) {
                ShortBuffer data = asShortBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            ShortBuffer data = asShortBuffer(typedArray);
            final int len = length(typedArray), middle = len >>> 1;
            for (int lower = 0, upper = len - 1; lower != middle; ++lower, --upper) {
                short lowerValue = data.get(lower);
                short upperValue = data.get(upper);
                data.put(lower, upperValue);
                data.put(upper, lowerValue);
            }
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(srcBuffer, srcIndex, length)::get, 0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt8(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asShortBuffer(srcBuffer, srcIndex, length)::get).andThen(ElementType::ToUint8Clamp),
                    0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(srcBuffer, srcIndex, length)::get, 0, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt32(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(srcBuffer, srcIndex, length)::get, 0, asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(srcBuffer, srcIndex, length)::get, 0, asDoubleBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(src)::get, srcPos, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt8(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asShortBuffer(src)::get).andThen(ElementType::ToUint8Clamp), srcPos,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(src)::get, srcPos, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt32(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(src)::get, srcPos, asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(src)::get, srcPos, asDoubleBuffer(dest), destPos, length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            ShortBuffer data = asShortBuffer(typedArray);
            short[] ba = new short[data.remaining()];
            data.get(ba);
            Arrays.sort(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            ShortBuffer data = asShortBuffer(typedArray);
            short[] ba = new short[data.remaining()];
            data.get(ba);
            sortArray(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Integer> toList(TypedArrayObject typedArray) {
            ShortBuffer data = asShortBuffer(typedArray);
            short[] ba = new short[data.remaining()];
            data.get(ba);
            return new ShortArrayList(ba);
        }
    };

    private static final TypedArrayFunctions UINT16 = new TypedArrayFunctions() {
        private final boolean isUint16(double v, short b) {
            return !(v < 0 || v > 65535 || Short.toUnsignedInt(b) != v);
        }

        private final IntUnaryOperator andThenInt(IntToShortFunction fn, ShortToIntFunction after) {
            return t -> after.applyAsInt(fn.applyAsShort(t));
        }

        private final IntToFloatFunction andThenFloat(IntToShortFunction fn, ShortToFloatFunction after) {
            return t -> after.applyAsFloat(fn.applyAsShort(t));
        }

        private final IntToDoubleFunction andThenDouble(IntToShortFunction fn, ShortToDoubleFunction after) {
            return t -> after.applyAsDouble(fn.applyAsShort(t));
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asShortBuffer(typedArray)::get, 0, asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt8(typedArray, target);
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(((IntUnaryOperator) asShortBuffer(typedArray)::get).andThen(ElementType::ToUint8ClampUnsigned), 0,
                    asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asShortBuffer(typedArray)::get, Short::toUnsignedInt), 0, asIntBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt32(typedArray, target);
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenFloat(asShortBuffer(typedArray)::get, Short::toUnsignedInt), 0, asFloatBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenDouble(asShortBuffer(typedArray)::get, Short::toUnsignedInt), 0, asDoubleBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            ShortBuffer data = asShortBuffer(typedArray);
            short v = ElementType.ToUint16(value.doubleValue());
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            short element = ElementType.ToUint16(searchElement.doubleValue());
            if (isUint16(searchElement.doubleValue(), element)) {
                ShortBuffer data = asShortBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            ShortBuffer data = asShortBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(Short.toUnsignedInt(data.get()));
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            short element = ElementType.ToUint16(searchElement.doubleValue());
            if (isUint16(searchElement.doubleValue(), element)) {
                ShortBuffer data = asShortBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            INT16.reverse(typedArray);
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(srcBuffer, srcIndex, length)::get, 0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt8(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asShortBuffer(srcBuffer, srcIndex, length)::get)
                    .andThen(ElementType::ToUint8ClampUnsigned), 0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asShortBuffer(srcBuffer, srcIndex, length)::get, Short::toUnsignedInt), 0, asIntBuffer(dest),
                    destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt32(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asShortBuffer(srcBuffer, srcIndex, length)::get, Short::toUnsignedInt), 0,
                    asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asShortBuffer(srcBuffer, srcIndex, length)::get, Short::toUnsignedInt), 0,
                    asDoubleBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asShortBuffer(src)::get, srcPos, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt8(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asShortBuffer(src)::get).andThen(ElementType::ToUint8ClampUnsigned), srcPos,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asShortBuffer(src)::get, Short::toUnsignedInt), srcPos, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt32(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asShortBuffer(src)::get, Short::toUnsignedInt), srcPos, asFloatBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asShortBuffer(src)::get, Short::toUnsignedInt), srcPos, asDoubleBuffer(dest), destPos,
                    length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            ShortBuffer data = asShortBuffer(typedArray);
            short[] ba = new short[data.remaining()];
            data.get(ba);
            sortArrayUnsigned(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            ShortBuffer data = asShortBuffer(typedArray);
            short[] ba = new short[data.remaining()];
            data.get(ba);
            sortArrayUnsigned(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Integer> toList(TypedArrayObject typedArray) {
            ShortBuffer data = asShortBuffer(typedArray);
            short[] ba = new short[data.remaining()];
            data.get(ba);
            return new ShortArrayList(ba, Short::toUnsignedInt);
        }
    };

    private static final TypedArrayFunctions INT32 = new TypedArrayFunctions() {
        private final boolean isInt32(double v, int b) {
            return !(v < Integer.MIN_VALUE || v > Integer.MAX_VALUE || b != v);
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asIntBuffer(typedArray)::get, 0, asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt8(typedArray, target);
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(((IntUnaryOperator) asIntBuffer(typedArray)::get).andThen(ElementType::ToUint8Clamp), 0,
                    asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asIntBuffer(typedArray)::get, 0, asShortBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt16(typedArray, target);
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asIntBuffer(typedArray)::get, 0, asFloatBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asIntBuffer(typedArray)::get, 0, asDoubleBuffer(target), 0, length(typedArray));
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            IntBuffer data = asIntBuffer(typedArray);
            int v = ElementType.ToInt32(value.doubleValue());
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            int element = ElementType.ToInt32(searchElement.doubleValue());
            if (isInt32(searchElement.doubleValue(), element)) {
                IntBuffer data = asIntBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            IntBuffer data = asIntBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(data.get());
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            int element = ElementType.ToInt32(searchElement.doubleValue());
            if (isInt32(searchElement.doubleValue(), element)) {
                IntBuffer data = asIntBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            IntBuffer data = asIntBuffer(typedArray);
            final int len = length(typedArray), middle = len >>> 1;
            for (int lower = 0, upper = len - 1; lower != middle; ++lower, --upper) {
                int lowerValue = data.get(lower);
                int upperValue = data.get(upper);
                data.put(lower, upperValue);
                data.put(upper, lowerValue);
            }
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(srcBuffer, srcIndex, length)::get, 0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt8(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asIntBuffer(srcBuffer, srcIndex, length)::get).andThen(ElementType::ToUint8Clamp),
                    0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(srcBuffer, srcIndex, length)::get, 0, asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt16(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(srcBuffer, srcIndex, length)::get, 0, asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(srcBuffer, srcIndex, length)::get, 0, asDoubleBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(src)::get, srcPos, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt8(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asIntBuffer(src)::get).andThen(ElementType::ToUint8Clamp), srcPos,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(src)::get, srcPos, asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt16(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(src)::get, srcPos, asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(src)::get, srcPos, asDoubleBuffer(dest), destPos, length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            IntBuffer data = asIntBuffer(typedArray);
            int[] ba = new int[data.remaining()];
            data.get(ba);
            Arrays.sort(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            IntBuffer data = asIntBuffer(typedArray);
            int[] ba = new int[data.remaining()];
            data.get(ba);
            sortArray(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Integer> toList(TypedArrayObject typedArray) {
            IntBuffer data = asIntBuffer(typedArray);
            int[] ba = new int[data.remaining()];
            data.get(ba);
            return new IntArrayList(ba);
        }
    };

    private static final TypedArrayFunctions UINT32 = new TypedArrayFunctions() {
        private final boolean isUint32(double v, int b) {
            return !(v < 0 || v > 4294967295L || Integer.toUnsignedLong(b) != v);
        }

        private final IntToFloatFunction andThenFloat(IntUnaryOperator fn, IntToFloatFunction after) {
            return t -> after.applyAsFloat(fn.applyAsInt(t));
        }

        private final IntToDoubleFunction andThenDouble(IntUnaryOperator fn, IntToDoubleFunction after) {
            return t -> after.applyAsDouble(fn.applyAsInt(t));
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asIntBuffer(typedArray)::get, 0, asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt8(typedArray, target);
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(((IntUnaryOperator) asIntBuffer(typedArray)::get).andThen(ElementType::ToUint8ClampUnsigned), 0,
                    asByteBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(asIntBuffer(typedArray)::get, 0, asShortBuffer(target), 0, length(typedArray));
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            constructInt16(typedArray, target);
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenFloat(asIntBuffer(typedArray)::get, Integer::toUnsignedLong), 0, asFloatBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenDouble(asIntBuffer(typedArray)::get, Integer::toUnsignedLong), 0, asDoubleBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            IntBuffer data = asIntBuffer(typedArray);
            int v = ElementType.ToUint32(value.doubleValue());
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            int element = ElementType.ToUint32(searchElement.doubleValue());
            if (isUint32(searchElement.doubleValue(), element)) {
                IntBuffer data = asIntBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            IntBuffer data = asIntBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(Integer.toUnsignedLong(data.get()));
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            int element = ElementType.ToUint32(searchElement.doubleValue());
            if (isUint32(searchElement.doubleValue(), element)) {
                IntBuffer data = asIntBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            INT32.reverse(typedArray);
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(srcBuffer, srcIndex, length)::get, 0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt8(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asIntBuffer(srcBuffer, srcIndex, length)::get)
                    .andThen(ElementType::ToUint8ClampUnsigned), 0, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(srcBuffer, srcIndex, length)::get, 0, asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            setInt16(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asIntBuffer(srcBuffer, srcIndex, length)::get, Integer::toUnsignedLong), 0,
                    asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asIntBuffer(srcBuffer, srcIndex, length)::get, Integer::toUnsignedLong), 0,
                    asDoubleBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(src)::get, srcPos, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt8(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(((IntUnaryOperator) asIntBuffer(src)::get).andThen(ElementType::ToUint8ClampUnsigned), srcPos,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(asIntBuffer(src)::get, srcPos, asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            sliceInt16(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asIntBuffer(src)::get, Integer::toUnsignedLong), srcPos, asFloatBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asIntBuffer(src)::get, Integer::toUnsignedLong), srcPos, asDoubleBuffer(dest), destPos,
                    length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            IntBuffer data = asIntBuffer(typedArray);
            int[] ba = new int[data.remaining()];
            data.get(ba);
            sortArrayUnsigned(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            IntBuffer data = asIntBuffer(typedArray);
            int[] ba = new int[data.remaining()];
            data.get(ba);
            sortArrayUnsigned(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Long> toList(TypedArrayObject typedArray) {
            IntBuffer data = asIntBuffer(typedArray);
            int[] ba = new int[data.remaining()];
            data.get(ba);
            return new UnsignedIntArrayList(ba);
        }
    };

    private static final TypedArrayFunctions INT64 = new TypedArrayFunctions() {
        private final boolean isInt64(BigInteger searchElement, long element) {
            return searchElement.bitLength() <= 63;
        }

        @Override
        protected void constructInt64(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint64(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            LongBuffer data = asLongBuffer(typedArray);
            long v = ElementType.ToBigInt64((BigInteger) value);
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isBigInt(searchElement)) {
                return -1;
            }
            long element = ElementType.ToBigInt64((BigInteger) searchElement);
            if (isInt64((BigInteger) searchElement, element)) {
                LongBuffer data = asLongBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            LongBuffer data = asLongBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(data.get());
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isBigInt(searchElement)) {
                return -1;
            }
            long element = ElementType.ToBigInt64((BigInteger) searchElement);
            if (isInt64((BigInteger) searchElement, element)) {
                LongBuffer data = asLongBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            LongBuffer data = asLongBuffer(typedArray);
            final int len = length(typedArray), middle = len >>> 1;
            for (int lower = 0, upper = len - 1; lower != middle; ++lower, --upper) {
                long lowerValue = data.get(lower);
                long upperValue = data.get(upper);
                data.put(lower, upperValue);
                data.put(upper, lowerValue);
            }
        }

        @Override
        protected void setInt64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void sliceInt64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            LongBuffer data = asLongBuffer(typedArray);
            long[] ba = new long[data.remaining()];
            data.get(ba);
            Arrays.sort(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            LongBuffer data = asLongBuffer(typedArray);
            long[] ba = new long[data.remaining()];
            data.get(ba);
            sortArrayAsBigInt(ba, BigInteger::valueOf, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<BigInteger> toList(TypedArrayObject typedArray) {
            LongBuffer data = asLongBuffer(typedArray);
            long[] ba = new long[data.remaining()];
            data.get(ba);
            return new BigIntArrayList(ba);
        }
    };

    private static final TypedArrayFunctions UINT64 = new TypedArrayFunctions() {
        private final boolean isUint64(BigInteger searchElement, long element) {
            return searchElement.signum() >= 0 && searchElement.bitLength() <= 64;
        }

        @Override
        protected void constructInt64(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructUint64(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            LongBuffer data = asLongBuffer(typedArray);
            long v = ElementType.ToBigUint64((BigInteger) value);
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            return indexOf(typedArray, searchElement, fromIndex) >= 0;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isBigInt(searchElement)) {
                return -1;
            }
            long element = ElementType.ToBigUint64((BigInteger) searchElement);
            if (isUint64((BigInteger) searchElement, element)) {
                LongBuffer data = asLongBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            LongBuffer data = asLongBuffer(typedArray);
            if (data.hasRemaining()) {
                while (true) {
                    r.append(BigIntType.toUnsigned64(data.get()).toString());
                    if (!data.hasRemaining())
                        break;
                    r.append(separator);
                }
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isBigInt(searchElement)) {
                return -1;
            }
            long element = ElementType.ToBigUint64((BigInteger) searchElement);
            if (isUint64((BigInteger) searchElement, element)) {
                LongBuffer data = asLongBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            INT64.reverse(typedArray);
        }

        @Override
        protected void setInt64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setUint64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void sliceInt64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceUint64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            LongBuffer data = asLongBuffer(typedArray);
            long[] ba = new long[data.remaining()];
            data.get(ba);
            sortArrayUnsigned(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            LongBuffer data = asLongBuffer(typedArray);
            long[] ba = new long[data.remaining()];
            data.get(ba);
            sortArrayAsBigInt(ba, BigIntType::toUnsigned64, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<BigInteger> toList(TypedArrayObject typedArray) {
            LongBuffer data = asLongBuffer(typedArray);
            long[] ba = new long[data.remaining()];
            data.get(ba);
            return new BigIntArrayList(ba, BigIntType::toUnsigned64);
        }
    };

    private static final TypedArrayFunctions FLOAT32 = new TypedArrayFunctions() {
        private final boolean isFloat32(double v, float b) {
            return b == v || Float.isNaN(b);
        }

        private final float normalizeNaN(float v) {
            if (Float.isNaN(v)) {
                return Float.NaN;
            }
            return v;
        }

        private final void normalizeNaN(float[] array) {
            for (int i = 0; i < array.length; ++i) {
                if (Float.isNaN(array[i])) {
                    array[i] = Float.NaN;
                }
            }
        }

        private final IntUnaryOperator andThenInt(IntToFloatFunction fn, FloatToIntFunction after) {
            return t -> after.applyAsInt(fn.applyAsFloat(t));
        }

        private final IntToDoubleFunction andThenDouble(IntToFloatFunction fn, FloatUnaryOperator after) {
            return t -> (double) after.applyAsFloat(fn.applyAsFloat(t));
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asFloatBuffer(typedArray)::get, ElementType::ToInt8), 0, asByteBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asFloatBuffer(typedArray)::get, ElementType::ToUint8), 0, asByteBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asFloatBuffer(typedArray)::get, ElementType::ToUint8Clamp), 0, asByteBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asFloatBuffer(typedArray)::get, ElementType::ToInt16), 0, asShortBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asFloatBuffer(typedArray)::get, ElementType::ToUint16), 0, asShortBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asFloatBuffer(typedArray)::get, ElementType::ToInt32), 0, asIntBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asFloatBuffer(typedArray)::get, ElementType::ToUint32), 0, asIntBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenDouble(asFloatBuffer(typedArray)::get, this::normalizeNaN), 0, asDoubleBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            FloatBuffer data = asFloatBuffer(typedArray);
            float v = (float) value.doubleValue();
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return false;
            }
            float element = (float) searchElement.doubleValue();
            if (isFloat32(searchElement.doubleValue(), element)) {
                FloatBuffer data = asFloatBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (SameValueZero(element, data.get(k))) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            float element = (float) searchElement.doubleValue();
            if (isFloat32(searchElement.doubleValue(), element)) {
                FloatBuffer data = asFloatBuffer(typedArray);
                for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            FloatBuffer data = asFloatBuffer(typedArray);
            assert data.hasRemaining();
            r.append(ToString(data.get()));
            while (data.hasRemaining()) {
                r.append(separator).append(ToString(data.get()));
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            float element = (float) searchElement.doubleValue();
            if (isFloat32(searchElement.doubleValue(), element)) {
                FloatBuffer data = asFloatBuffer(typedArray);
                for (int k = index(fromIndex); k >= 0; --k) {
                    if (element == data.get(k)) { // StrictEqualityComparison
                        return k;
                    }
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            FloatBuffer data = asFloatBuffer(typedArray);
            final int len = length(typedArray), middle = len >>> 1;
            for (int lower = 0, upper = len - 1; lower != middle; ++lower, --upper) {
                float lowerValue = normalizeNaN(data.get(lower));
                float upperValue = normalizeNaN(data.get(upper));
                data.put(lower, upperValue);
                data.put(upper, lowerValue);
            }
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToInt8), 0, asByteBuffer(dest),
                    destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint8), 0,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint8Clamp), 0,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToInt16), 0,
                    asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint16), 0,
                    asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToInt32), 0, asIntBuffer(dest),
                    destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint32), 0,
                    asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asFloatBuffer(srcBuffer, srcIndex, length)::get, this::normalizeNaN), 0,
                    asDoubleBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(src)::get, ElementType::ToInt8), srcPos, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(src)::get, ElementType::ToUint8), srcPos, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(src)::get, ElementType::ToUint8Clamp), srcPos, asByteBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(src)::get, ElementType::ToInt16), srcPos, asShortBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(src)::get, ElementType::ToUint16), srcPos, asShortBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(src)::get, ElementType::ToInt32), srcPos, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asFloatBuffer(src)::get, ElementType::ToUint32), srcPos, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenDouble(asFloatBuffer(src)::get, this::normalizeNaN), srcPos, asDoubleBuffer(dest), destPos,
                    length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            FloatBuffer data = asFloatBuffer(typedArray);
            float[] ba = new float[data.remaining()];
            data.get(ba);
            normalizeNaN(ba);
            Arrays.sort(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            FloatBuffer data = asFloatBuffer(typedArray);
            float[] ba = new float[data.remaining()];
            data.get(ba);
            // Canonicalize NaNs to ensure users only see a single distinct NaN value.
            normalizeNaN(ba);
            sortArray(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Double> toList(TypedArrayObject typedArray) {
            FloatBuffer data = asFloatBuffer(typedArray);
            float[] ba = new float[data.remaining()];
            data.get(ba);
            // Strictly speaking NaN canonicalization isn't required by the spec, but we perform it nonetheless.
            normalizeNaN(ba);
            return new FloatArrayList(ba);
        }
    };

    private static final TypedArrayFunctions FLOAT64 = new TypedArrayFunctions() {
        private final double normalizeNaN(double v) {
            if (Double.isNaN(v)) {
                return Double.NaN;
            }
            return v;
        }

        private final void normalizeNaN(double[] array) {
            for (int i = 0; i < array.length; ++i) {
                if (Double.isNaN(array[i])) {
                    array[i] = Double.NaN;
                }
            }
        }

        private final IntUnaryOperator andThenInt(IntToDoubleFunction fn, DoubleToIntFunction after) {
            return t -> after.applyAsInt(fn.applyAsDouble(t));
        }

        private final IntToFloatFunction andThenFloat(IntToDoubleFunction fn, DoubleUnaryOperator after) {
            return t -> (float) after.applyAsDouble(fn.applyAsDouble(t));
        }

        @Override
        protected void constructInt8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asDoubleBuffer(typedArray)::get, ElementType::ToInt8), 0, asByteBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint8(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asDoubleBuffer(typedArray)::get, ElementType::ToUint8), 0, asByteBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint8C(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asDoubleBuffer(typedArray)::get, ElementType::ToUint8Clamp), 0, asByteBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructInt16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asDoubleBuffer(typedArray)::get, ElementType::ToInt16), 0, asShortBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint16(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asDoubleBuffer(typedArray)::get, ElementType::ToUint16), 0, asShortBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructInt32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asDoubleBuffer(typedArray)::get, ElementType::ToInt32), 0, asIntBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructUint32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenInt(asDoubleBuffer(typedArray)::get, ElementType::ToUint32), 0, asIntBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructFloat32(TypedArrayObject typedArray, ArrayBufferObject target) {
            put(andThenFloat(asDoubleBuffer(typedArray)::get, this::normalizeNaN), 0, asFloatBuffer(target), 0,
                    length(typedArray));
        }

        @Override
        protected void constructFloat64(TypedArrayObject typedArray, ArrayBufferObject target) {
            super.constructBitwise(typedArray, target);
        }

        @Override
        void fill(TypedArrayObject typedArray, Number value, long start, long end) {
            DoubleBuffer data = asDoubleBuffer(typedArray);
            double v = value.doubleValue();
            for (int k = index(start), e = index(end); k < e; ++k) {
                data.put(k, v);
            }
        }

        @Override
        boolean includes(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return false;
            }
            double element = searchElement.doubleValue();
            DoubleBuffer data = asDoubleBuffer(typedArray);
            for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                if (SameValueZero(element, data.get(k))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        int indexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            double element = searchElement.doubleValue();
            DoubleBuffer data = asDoubleBuffer(typedArray);
            for (int k = index(fromIndex), length = length(typedArray); k < length; ++k) {
                if (element == data.get(k)) { // StrictEqualityComparison
                    return k;
                }
            }
            return -1;
        }

        @Override
        String join(TypedArrayObject typedArray, String separator, StrBuilder r) {
            DoubleBuffer data = asDoubleBuffer(typedArray);
            assert data.hasRemaining();
            r.append(ToString(data.get()));
            while (data.hasRemaining()) {
                r.append(separator).append(ToString(data.get()));
            }
            return r.toString();
        }

        @Override
        int lastIndexOf(TypedArrayObject typedArray, Number searchElement, long fromIndex) {
            if (!Type.isNumber(searchElement)) {
                return -1;
            }
            double element = searchElement.doubleValue();
            DoubleBuffer data = asDoubleBuffer(typedArray);
            for (int k = index(fromIndex); k >= 0; --k) {
                if (element == data.get(k)) { // StrictEqualityComparison
                    return k;
                }
            }
            return -1;
        }

        @Override
        void reverse(TypedArrayObject typedArray) {
            DoubleBuffer data = asDoubleBuffer(typedArray);
            final int len = length(typedArray), middle = len >>> 1;
            for (int lower = 0, upper = len - 1; lower != middle; ++lower, --upper) {
                double lowerValue = normalizeNaN(data.get(lower));
                double upperValue = normalizeNaN(data.get(upper));
                data.put(lower, upperValue);
                data.put(upper, lowerValue);
            }
        }

        @Override
        protected void setInt8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToInt8), 0,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint8(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint8), 0,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint8C(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint8Clamp), 0,
                    asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToInt16), 0,
                    asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint16(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint16), 0,
                    asShortBuffer(dest), destPos, length);
        }

        @Override
        protected void setInt32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToInt32), 0,
                    asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void setUint32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(srcBuffer, srcIndex, length)::get, ElementType::ToUint32), 0,
                    asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat32(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asDoubleBuffer(srcBuffer, srcIndex, length)::get, this::normalizeNaN), 0,
                    asFloatBuffer(dest), destPos, length);
        }

        @Override
        protected void setFloat64(ArrayBuffer srcBuffer, int srcIndex, TypedArrayObject dest, int destPos, int length) {
            super.setBitwise(srcBuffer, srcIndex, dest, destPos, length);
        }

        @Override
        protected void sliceInt8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(src)::get, ElementType::ToInt8), srcPos, asByteBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint8(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(src)::get, ElementType::ToUint8), srcPos, asByteBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceUint8C(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(src)::get, ElementType::ToUint8Clamp), srcPos, asByteBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceInt16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(src)::get, ElementType::ToInt16), srcPos, asShortBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceUint16(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(src)::get, ElementType::ToUint16), srcPos, asShortBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceInt32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(src)::get, ElementType::ToInt32), srcPos, asIntBuffer(dest), destPos, length);
        }

        @Override
        protected void sliceUint32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenInt(asDoubleBuffer(src)::get, ElementType::ToUint32), srcPos, asIntBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceFloat32(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            put(andThenFloat(asDoubleBuffer(src)::get, this::normalizeNaN), srcPos, asFloatBuffer(dest), destPos,
                    length);
        }

        @Override
        protected void sliceFloat64(TypedArrayObject src, int srcPos, TypedArrayObject dest, int destPos, int length) {
            super.sliceBitwise(src, srcPos, dest, destPos, length);
        }

        @Override
        void sort(TypedArrayObject typedArray) {
            DoubleBuffer data = asDoubleBuffer(typedArray);
            double[] ba = new double[data.remaining()];
            data.get(ba);
            normalizeNaN(ba);
            Arrays.sort(ba);
            data.rewind();
            data.put(ba);
        }

        @Override
        void sort(TypedArrayObject typedArray, Comparator<Number> comparator) {
            DoubleBuffer data = asDoubleBuffer(typedArray);
            double[] ba = new double[data.remaining()];
            data.get(ba);
            // Canonicalize NaNs to ensure users only see a single distinct NaN value.
            normalizeNaN(ba);
            sortArray(ba, comparator);
            data.rewind();
            data.put(ba);
        }

        @Override
        List<Double> toList(TypedArrayObject typedArray) {
            DoubleBuffer data = asDoubleBuffer(typedArray);
            double[] ba = new double[data.remaining()];
            data.get(ba);
            // Strictly speaking NaN canonicalization isn't required by the spec, but we perform it nonetheless.
            normalizeNaN(ba);
            return new DoubleArrayList(ba);
        }
    };
}
