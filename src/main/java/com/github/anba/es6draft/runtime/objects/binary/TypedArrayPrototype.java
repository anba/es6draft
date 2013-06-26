/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CloneArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.6 TypedArray Object Structures</h3>
 * <ul>
 * <li>15.13.6.4 Properties of the TypedArray Prototype Object
 * </ul>
 */
public class TypedArrayPrototype extends OrdinaryObject implements Initialisable {
    private final ElementKind elementKind;

    public TypedArrayPrototype(Realm realm, ElementKind elementKind) {
        super(realm);
        this.elementKind = elementKind;
    }

    @Override
    public void initialise(ExecutionContext cx) {
        switch (elementKind) {
        case Int8:
            createProperties(this, cx, Properties_Int8Array.class);
            break;
        case Uint8:
            createProperties(this, cx, Properties_Uint8Array.class);
            break;
        case Uint8C:
            createProperties(this, cx, Properties_Uint8Clamped.class);
            break;
        case Int16:
            createProperties(this, cx, Properties_Int16Array.class);
            break;
        case Uint16:
            createProperties(this, cx, Properties_Uint16Array.class);
            break;
        case Int32:
            createProperties(this, cx, Properties_Int32Array.class);
            break;
        case Uint32:
            createProperties(this, cx, Properties_Uint32Array.class);
            break;
        case Float32:
            createProperties(this, cx, Properties_Float32Array.class);
            break;
        case Float64:
            createProperties(this, cx, Properties_Float64Array.class);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    private static TypedArrayObject TypedArrayObject(ExecutionContext cx, ScriptObject m) {
        if (m instanceof TypedArrayObject) {
            return (TypedArrayObject) m;
        }
        throw throwTypeError(cx, Messages.Key.IncompatibleObject);
    }

    /**
     * 15.13.6.4.3 get TypedArray.prototype.buffer
     */
    private static Object __buffer(ExecutionContext cx, Object thisValue) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject array = TypedArrayObject(cx, obj);
        ArrayBufferObject buffer = array.getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        return buffer;
    }

    /**
     * 15.13.6.4.4 get TypedArray.prototype.byteLength
     */
    private static Object __byteLength(ExecutionContext cx, Object thisValue) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject array = TypedArrayObject(cx, obj);
        ArrayBufferObject buffer = array.getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        return array.getByteLength();
    }

    /**
     * 15.13.6.4.5 get TypedArray.prototype.byteOffset
     */
    private static Object __byteOffset(ExecutionContext cx, Object thisValue) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject array = TypedArrayObject(cx, obj);
        ArrayBufferObject buffer = array.getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        return array.getByteOffset();
    }

    /**
     * 15.13.6.4.6 get TypedArray.prototype.length
     */
    private static Object __length(ExecutionContext cx, Object thisValue) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject array = TypedArrayObject(cx, obj);
        ArrayBufferObject buffer = array.getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        return array.getArrayLength();
    }

    /**
     * 15.13.6.4.7 TypedArray.prototype.set(array, offset = 0 )<br>
     * 15.13.6.4.8 TypedArray.prototype.set(typedArray, offset = 0 )
     */
    private static Object __set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject target = TypedArrayObject(cx, obj);
        ArrayBufferObject targetBuffer = target.getBuffer();
        if (targetBuffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        long targetLength = target.getArrayLength();
        double targetOffset = (offset == UNDEFINED ? 0 : ToInteger(cx, offset));
        if (targetOffset < 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        ElementKind targetType = target.getElementKind();
        int targetElementSize = targetType.size();
        long targetByteOffset = target.getByteOffset();

        if (!(array instanceof TypedArrayObject)) {
            // 15.13.6.6.7
            ScriptObject src = ToObject(cx, array);
            Object srcLen = Get(cx, src, "length");
            double numberLength = ToNumber(cx, srcLen);
            double srcLength = ToInteger(numberLength);
            if (numberLength != srcLength || srcLength < 0) {
                throwRangeError(cx, Messages.Key.InvalidByteOffset);
            }
            if (srcLength + targetOffset > targetLength) {
                throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
            }
            long targetByteIndex = (long) (targetOffset * targetElementSize + targetByteOffset);
            long limit = (long) (targetByteIndex + targetElementSize
                    * Math.min(srcLength, targetLength - targetOffset));
            for (long k = 0; targetByteIndex < limit; ++k, targetByteIndex += targetElementSize) {
                String pk = ToString(k);
                Object kValue = Get(cx, src, pk);
                double kNumber = ToNumber(cx, kValue);
                // FIXME: spec bug "k * targetElementSize" => "targetByteIndex"
                SetValueInBuffer(targetBuffer, targetByteIndex, targetType, kNumber, false);
            }
            return UNDEFINED;
        } else {
            // 15.13.6.6.8
            TypedArrayObject src = (TypedArrayObject) array;
            ArrayBufferObject srcBuffer = src.getBuffer();
            if (srcBuffer == null) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            ElementKind srcType = src.getElementKind();
            int srcElementSize = srcType.size();
            long srcLength = src.getArrayLength();
            long srcByteOffset = src.getByteOffset();
            if (srcLength + targetOffset > targetLength) {
                throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
            }
            if (SameValue(srcBuffer, targetBuffer)) {
                // FIXME: spec bug - either make full copy or adjust srcByteOffset!
                srcBuffer = CloneArrayBuffer(cx, srcBuffer, srcType, srcType, srcByteOffset,
                        srcLength);
                assert srcBuffer.getByteLength() == srcLength * srcType.size();
                srcByteOffset = 0;
            }
            long targetByteIndex = (long) (targetOffset * targetElementSize + targetByteOffset);
            long srcByteIndex = srcByteOffset;
            long limit = (long) (targetByteIndex + targetElementSize
                    * Math.min(srcLength, targetLength - targetOffset));
            for (; targetByteIndex < limit; srcByteIndex += srcElementSize, targetByteIndex += targetElementSize) {
                double value = GetValueFromBuffer(srcBuffer, srcByteIndex, srcType, false);
                SetValueInBuffer(targetBuffer, targetByteIndex, targetType, value, false);
            }
            return UNDEFINED;
        }
    }

    /**
     * 15.13.6.4.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
     */
    private static Object __subarray(ExecutionContext cx, Object thisValue, Object begin, Object end) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject array = TypedArrayObject(cx, obj);
        ArrayBufferObject buffer = array.getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        long srcLength = array.getArrayLength();
        double beginInt = (begin == UNDEFINED ? 0 : ToInteger(cx, begin));
        if (beginInt < 0) {
            beginInt = srcLength + beginInt;
        }
        double beginIndex = Math.min(srcLength, Math.max(0, beginInt));
        double endInt = (end == UNDEFINED ? srcLength : ToInteger(cx, end));
        if (endInt < 0) {
            endInt = srcLength + endInt;
        }
        double endIndex = Math.max(0, Math.min(srcLength, endInt));
        if (endIndex < beginIndex) {
            endIndex = beginIndex;
        }
        double newLength = endIndex - beginIndex;
        ElementKind elementType = array.getElementKind();
        int elementSize = elementType.size();
        double srcByteOffset = array.getByteOffset();
        double beginByteOffset = srcByteOffset + beginIndex * elementSize;
        Object constructor = Get(cx, array, "constructor");
        if (!IsConstructor(constructor)) {
            throwTypeError(cx, Messages.Key.NotConstructor);
        }
        return ((Constructor) constructor).construct(cx, buffer, beginByteOffset, newLength);
    }

    /**
     * 15.13.6.4.10 TypedArray.prototype.@@elementGet ( index )
     */
    private static Object __elementGet(ExecutionContext cx, Object thisValue, Object index,
            int elementSize) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject array = TypedArrayObject(cx, obj);
        ArrayBufferObject buffer = array.getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        long length = array.getArrayLength();
        double intIndex = ToInteger(cx, index);
        if (intIndex < 0 || intIndex >= length) {
            return UNDEFINED;
        }
        long offset = array.getByteOffset();
        long indexedPosition = (long) ((intIndex * elementSize) + offset);
        ElementKind elementType = array.getElementKind();
        return GetValueFromBuffer(buffer, indexedPosition, elementType, false);
    }

    /**
     * 15.13.6.4.11 TypedArray.prototype.@@elementSet ( index, value )
     */
    private static Object __elementSet(ExecutionContext cx, Object thisValue, Object index,
            Object value, int elementSize) {
        ScriptObject obj = ToObject(cx, thisValue);
        TypedArrayObject array = TypedArrayObject(cx, obj);
        ArrayBufferObject buffer = array.getBuffer();
        if (buffer == null) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        long length = array.getArrayLength();
        double intIndex = ToInteger(cx, index);
        double numValue = ToNumber(cx, value);
        if (intIndex < 0 || intIndex >= length) {
            // FIXME: spec bug (@@elementSet) should return true/false
            // return numValue;
            return false;
        }
        long offset = array.getByteOffset();
        long indexedPosition = (long) ((intIndex * elementSize) + offset);
        ElementKind elementType = array.getElementKind();
        SetValueInBuffer(buffer, indexedPosition, elementType, numValue, false);
        // FIXME: spec bug (@@elementSet) should return true/false
        // return numValue;
        return true;
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Int8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Int8Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Int8.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint8Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Uint8.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint8Clamped {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint8ClampedArray;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Uint8C.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Int16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Int16Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Int16.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint16Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Uint16.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Int32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Int32Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Int32.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Uint32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Uint32Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Uint32.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Float32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Float32Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Float32.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }

    /**
     * 15.13.6.4 Properties of the TypedArray Prototype Object
     */
    public enum Properties_Float64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.4.1 TypedArray.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Float64Array;

        /**
         * 15.13.6.4.2 TypedArray.prototype.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT", attributes = @Attributes(writable = false,
                enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementKind.Float64.size();

        /**
         * 15.13.6.4.3 get TypedArray.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return __buffer(cx, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return __byteLength(cx, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return __byteOffset(cx, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return __length(cx, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            return __set(cx, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            return __subarray(cx, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(ExecutionContext cx, Object thisValue, Object index) {
            return __elementGet(cx, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(ExecutionContext cx, Object thisValue, Object index,
                Object value) {
            return __elementSet(cx, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }
}
