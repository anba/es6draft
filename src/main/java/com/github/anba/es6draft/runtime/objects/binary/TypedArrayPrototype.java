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
public class TypedArrayPrototype extends OrdinaryObject implements ScriptObject, Initialisable {
    private final ElementKind elementKind;

    public TypedArrayPrototype(Realm realm, ElementKind elementKind) {
        super(realm);
        this.elementKind = elementKind;
    }

    @Override
    public void initialise(Realm realm) {
        switch (elementKind) {
        case Int8:
            createProperties(this, realm, Properties_Int8Array.class);
            break;
        case Uint8:
            createProperties(this, realm, Properties_Uint8Array.class);
            break;
        case Uint8C:
            createProperties(this, realm, Properties_Uint8Clamped.class);
            break;
        case Int16:
            createProperties(this, realm, Properties_Int16Array.class);
            break;
        case Uint16:
            createProperties(this, realm, Properties_Uint16Array.class);
            break;
        case Int32:
            createProperties(this, realm, Properties_Int32Array.class);
            break;
        case Uint32:
            createProperties(this, realm, Properties_Uint32Array.class);
            break;
        case Float32:
            createProperties(this, realm, Properties_Float32Array.class);
            break;
        case Float64:
            createProperties(this, realm, Properties_Float64Array.class);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    private static TypedArrayObject TypedArrayObject(Realm realm, ScriptObject m) {
        if (m instanceof TypedArrayObject) {
            return (TypedArrayObject) m;
        }
        throw throwTypeError(realm, Messages.Key.IncompatibleObject);
    }

    private static Object __buffer(Realm realm, Object thisValue) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject array = TypedArrayObject(realm, obj);
        ArrayBufferObject buffer = array.getData();
        if (buffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        return buffer;
    }

    private static Object __byteLength(Realm realm, Object thisValue) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject array = TypedArrayObject(realm, obj);
        ArrayBufferObject buffer = array.getData();
        if (buffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        return array.getByteLength();
    }

    private static Object __byteOffset(Realm realm, Object thisValue) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject array = TypedArrayObject(realm, obj);
        ArrayBufferObject buffer = array.getData();
        if (buffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        return array.getByteOffset();
    }

    private static Object __length(Realm realm, Object thisValue) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject array = TypedArrayObject(realm, obj);
        ArrayBufferObject buffer = array.getData();
        if (buffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        return array.getArrayLength();
    }

    private static Object __set(Realm realm, Object thisValue, Object array, Object offset) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject target = TypedArrayObject(realm, obj);
        ArrayBufferObject targetBuffer = target.getData();
        if (targetBuffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        long targetLength = target.getArrayLength();
        long targetOffset = ToUint32(realm, offset);
        ElementKind targetType = target.getElementKind();
        int targetElementSize = targetType.size();
        long targetByteOffset = target.getByteOffset();

        if (!(array instanceof TypedArrayObject)) {
            // 15.13.6.6.7
            ScriptObject src = ToObject(realm, array);
            Object srcLen = Get(realm, src, "length");
            long srcLength = ToUint32(realm, srcLen);
            if (srcLength + targetOffset > targetLength) {
                throwRangeError(realm, Messages.Key.ArrayOffsetOutOfRange);
            }
            long targetByteIndex = targetOffset * targetElementSize + targetByteOffset;
            long limit = targetByteIndex + targetElementSize
                    * Math.min(srcLength, targetLength - targetOffset);
            for (long k = 0; targetByteIndex < limit; ++k, targetByteIndex += targetElementSize) {
                String pk = ToString(k);
                Object kValue = Get(realm, src, pk);
                double kNumber = ToNumber(realm, kValue);
                // FIXME: spec bug (variables data, elementSize and elementType not defined)
                SetValueInBuffer(targetBuffer, k * targetElementSize, targetType, kNumber, false);
            }
            return UNDEFINED;
        } else {
            // 15.13.6.6.8
            TypedArrayObject src = (TypedArrayObject) array;
            ArrayBufferObject srcBuffer = src.getData();
            if (srcBuffer == null) {
                throw throwTypeError(realm, Messages.Key.IncompatibleObject);
            }
            ElementKind srcType = src.getElementKind();
            int srcElementSize = srcType.size();
            long srcLength = src.getArrayLength();
            long srcByteOffset = src.getByteOffset();
            if (srcLength + targetOffset > targetLength) {
                throwRangeError(realm, Messages.Key.ArrayOffsetOutOfRange);
            }
            if (SameValue(srcBuffer, targetBuffer)) {
                // FIXME: spec bug (variable srcData not defined)
                srcBuffer = CloneArrayBuffer(realm, srcBuffer, srcType, srcType, srcLength);
            }
            long targetByteIndex = targetOffset * targetElementSize + targetByteOffset;
            long srcByteIndex = srcByteOffset;
            long limit = targetByteIndex + targetElementSize
                    * Math.min(srcLength, targetLength - targetOffset);
            for (; targetByteIndex < limit; srcByteIndex += srcElementSize, targetByteIndex += targetElementSize) {
                double value = GetValueFromBuffer(srcBuffer, srcByteIndex, srcType, false);
                SetValueInBuffer(targetBuffer, targetByteIndex, targetType, value, false);
            }
            return UNDEFINED;
        }
    }

    private static Object __subarray(Realm realm, Object thisValue, Object begin, Object end) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject array = TypedArrayObject(realm, obj);
        ArrayBufferObject buffer = array.getData();
        if (buffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        long srcLength = array.getArrayLength();
        long beginInt = ToInt32(realm, begin);
        if (beginInt < 0) {
            beginInt = srcLength + beginInt;
        }
        long beginIndex = Math.min(srcLength, Math.max(0, beginInt));
        long endInt = (end == UNDEFINED ? srcLength : ToInt32(realm, end));
        if (endInt < 0) {
            endInt = srcLength + endInt;
        }
        long endIndex = Math.max(0, Math.min(srcLength, endInt));
        if (endIndex < beginIndex) {
            endIndex = beginIndex;
        }
        long newLength = endIndex - beginIndex;
        ElementKind elementType = array.getElementKind();
        int elementSize = elementType.size();
        long srcByteOffset = array.getByteOffset();
        long beginByteOffset = srcByteOffset + beginIndex * elementSize;
        Object constructor = Get(realm, array, "constructor");
        if (!IsConstructor(constructor)) {
            throwTypeError(realm, Messages.Key.NotConstructor);
        }
        return ((Constructor) constructor).construct(buffer, beginByteOffset, newLength);
    }

    private static Object __elementGet(Realm realm, Object thisValue, Object index, int elementSize) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject array = TypedArrayObject(realm, obj);
        ArrayBufferObject buffer = array.getData();
        if (buffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        long length = array.getArrayLength();
        long intIndex = ToUint32(realm, index);
        if (intIndex >= length) {
            return UNDEFINED;
        }
        long offset = array.getByteOffset();
        long indexedPosition = (intIndex * elementSize) + offset;
        ElementKind elementType = array.getElementKind();
        // FIXME: spec bug (GetValueFromBuffer instead of GetArrayBuffer)
        return GetValueFromBuffer(buffer, indexedPosition, elementType, false);
    }

    private static Object __elementSet(Realm realm, Object thisValue, Object index, Object value,
            int elementSize) {
        ScriptObject obj = ToObject(realm, thisValue);
        TypedArrayObject array = TypedArrayObject(realm, obj);
        ArrayBufferObject buffer = array.getData();
        if (buffer == null) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        long length = array.getArrayLength();
        long intIndex = ToUint32(realm, index);
        double numValue = ToNumber(realm, value);
        if (intIndex >= length) {
            // FIXME: spec bug (@@elementSet) should return true/false
            // return numValue;
            return false;
        }
        long offset = array.getByteOffset();
        long indexedPosition = (intIndex * elementSize) + offset;
        ElementKind elementType = array.getElementKind();
        // FIXME: spec bug (SetValueInBuffer instead of SetArrayBuffer)
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
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
        public static Object buffer(Realm realm, Object thisValue) {
            return __buffer(realm, thisValue);
        }

        /**
         * 15.13.6.6.4 get TypedArray.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            return __byteLength(realm, thisValue);
        }

        /**
         * 15.13.6.6.5 get TypedArray.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(Realm realm, Object thisValue) {
            return __byteOffset(realm, thisValue);
        }

        /**
         * 15.13.6.6.6 get TypedArray.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(Realm realm, Object thisValue) {
            return __length(realm, thisValue);
        }

        /**
         * 15.13.6.6.7 TypedArray.prototype.set(array, offset = 0 )<br>
         * 15.13.6.6.8 TypedArray.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object array, Object offset) {
            return __set(realm, thisValue, array, offset);
        }

        /**
         * 15.13.6.6.9 TypedArray.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(Realm realm, Object thisValue, Object begin, Object end) {
            return __subarray(realm, thisValue, begin, end);
        }

        /**
         * 15.13.6.6.10 TypedArray.prototype.@@elementGet ( index )
         */
        @Function(name = "@@elementGet", symbol = BuiltinSymbol.elementGet, arity = 1)
        public static Object elementGet(Realm realm, Object thisValue, Object index) {
            return __elementGet(realm, thisValue, index, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.6.11 TypedArray.prototype.@@elementSet ( index, value )
         */
        @Function(name = "@@elementSet", symbol = BuiltinSymbol.elementSet, arity = 2)
        public static Object elementSet(Realm realm, Object thisValue, Object index, Object value) {
            return __elementSet(realm, thisValue, index, value, BYTES_PER_ELEMENT);
        }

        /**
         * 15.13.6.4.11 TypedArray.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = constructor.name();
    }
}
