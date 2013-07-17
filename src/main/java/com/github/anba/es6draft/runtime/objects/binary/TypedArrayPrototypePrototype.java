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
import static com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.CreateArrayIterator;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CloneArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.ArrayIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.6 TypedArray Object Structures</h3>
 * <ul>
 * <li>15.13.6.3 Properties of the %TypedArrayPrototype% Object
 * </ul>
 */
public class TypedArrayPrototypePrototype extends OrdinaryObject implements Initialisable {
    public TypedArrayPrototypePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);

        // shared functions from Array.prototype
        ScriptObject arrayPrototype = cx.getIntrinsic(Intrinsics.ArrayPrototype);

        // 15.13.6.3.9 %TypedArray%.prototype.toString ()
        Object toString = Get(cx, arrayPrototype, "toString");
        defineOwnProperty(cx, "toString", new PropertyDescriptor(toString, true, false, true));

        // 15.13.6.3.10 %TypedArray%.prototype.toLocaleString ()
        Object toLocaleString = Get(cx, arrayPrototype, "toLocaleString");
        defineOwnProperty(cx, "toLocaleString", new PropertyDescriptor(toLocaleString, true, false,
                true));

        // 15.13.6.3.29 %TypedArray%.prototype [ @@iterator ] ()
        defineOwnProperty(cx, BuiltinSymbol.iterator.get(),
                new PropertyDescriptor(Get(cx, this, "entries"), true, false, true));
    }

    /**
     * 15.13.6.3 Properties of the %TypedArrayPrototype% Object
     */
    public enum Properties {
        ;

        private static TypedArrayObject thisTypedArrayObject(ExecutionContext cx, Object thisValue) {
            if (thisValue instanceof TypedArrayObject) {
                TypedArrayObject array = (TypedArrayObject) thisValue;
                if (array.getBuffer() != null) {
                    return array;
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.6.3.1 %TypedArray%.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.TypedArray;

        /**
         * 15.13.6.3.2 get %TypedArray%.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return array.getBuffer();
        }

        /**
         * 15.13.6.3.3 get %TypedArray%.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return array.getByteLength();
        }

        /**
         * 15.13.6.3.4 get %TypedArray%.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return array.getByteOffset();
        }

        /**
         * 15.13.6.3.5 get %TypedArray%.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return array.getArrayLength();
        }

        /**
         * 15.13.6.3.6 %TypedArray%.prototype.set(array, offset = 0 )<br>
         * 15.13.6.3.7 %TypedArray%.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            TypedArrayObject target = thisTypedArrayObject(cx, thisValue);
            ArrayBufferObject targetBuffer = target.getBuffer();
            long targetLength = target.getArrayLength();
            double targetOffset = (offset == UNDEFINED ? 0 : ToInteger(cx, offset));
            if (targetOffset < 0) {
                throwRangeError(cx, Messages.Key.InvalidByteOffset);
            }
            ElementType targetType = target.getElementType();
            int targetElementSize = targetType.size();
            long targetByteOffset = target.getByteOffset();

            if (!(array instanceof TypedArrayObject)) {
                // 15.13.6.3.6
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
                // 15.13.6.3.7
                TypedArrayObject src = (TypedArrayObject) array;
                ArrayBufferObject srcBuffer = src.getBuffer();
                if (srcBuffer == null) {
                    throw throwTypeError(cx, Messages.Key.IncompatibleObject);
                }
                ElementType srcType = src.getElementType();
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
         * 15.13.6.3.8 %TypedArray%.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            ArrayBufferObject buffer = array.getBuffer();
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
            ElementType elementType = array.getElementType();
            int elementSize = elementType.size();
            double srcByteOffset = array.getByteOffset();
            double beginByteOffset = srcByteOffset + beginIndex * elementSize;
            Object constructor = Get(cx, array, "constructor");
            if (!IsConstructor(constructor)) {
                throwTypeError(cx, Messages.Key.NotConstructor);
            }
            return ((Constructor) constructor).construct(cx, buffer, beginByteOffset, newLength);
        }

        // FIXME: spec does not define 15.13.6.3.11 - 15.13.6.3.25

        /**
         * 15.13.6.3.26 %TypedArray%.prototype.entries ( )
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            return CreateArrayIterator(cx, o, ArrayIterationKind.KeyValue);
        }

        /**
         * 15.13.6.3.27 %TypedArray%.prototype.keys ( )
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            return CreateArrayIterator(cx, o, ArrayIterationKind.Key);
        }

        /**
         * 15.13.6.3.28 %TypedArray%.prototype.values ( )
         */
        @Function(name = "values", arity = 0)
        public static Object values(ExecutionContext cx, Object thisValue) {
            ScriptObject o = ToObject(cx, thisValue);
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
        }

        /**
         * 15.13.6.3.30 get %TypedArray%.prototype [ @@toStringTag ]
         */
        @Accessor(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag,
                type = Accessor.Type.Getter)
        public static Object toStringTag(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof TypedArrayObject)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            return ((TypedArrayObject) thisValue).getTypedArrayName();
        }
    }
}
