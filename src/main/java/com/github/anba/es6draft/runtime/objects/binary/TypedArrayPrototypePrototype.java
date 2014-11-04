/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.CreateArrayIterator;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CloneArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.ArrayIterationKind;
import com.github.anba.es6draft.runtime.objects.ArrayPrototype;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.3 Properties of the %TypedArrayPrototype% Object
 * </ul>
 */
public final class TypedArrayPrototypePrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new TypedArray prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public TypedArrayPrototypePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    /**
     * 22.2.3 Properties of the %TypedArrayPrototype% Object
     */
    public enum Properties {
        ;

        private static ArrayBufferView thisArrayBufferView(ExecutionContext cx, Object m) {
            if (m instanceof ArrayBufferView) {
                ArrayBufferView view = (ArrayBufferView) m;
                ArrayBufferObject buffer = view.getBuffer();
                if (buffer == null) {
                    throw newTypeError(cx, Messages.Key.UninitializedObject);
                }
                return view;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        private static TypedArrayObject thisTypedArrayObject(ExecutionContext cx, Object thisValue) {
            if (thisValue instanceof TypedArrayObject) {
                // initialization status intentionally not checked here!
                return (TypedArrayObject) thisValue;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        private static TypedArrayObject thisTypedArrayObjectChecked(ExecutionContext cx,
                Object thisValue) {
            if (thisValue instanceof TypedArrayObject) {
                TypedArrayObject typedArray = (TypedArrayObject) thisValue;
                ArrayBufferObject buffer = typedArray.getBuffer();
                if (buffer == null) {
                    throw newTypeError(cx, Messages.Key.UninitializedObject);
                }
                if (IsDetachedBuffer(buffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                return typedArray;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 22.2.3.4 %TypedArray%.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.TypedArray;

        /**
         * 22.2.3.1 get %TypedArray%.prototype.buffer
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the array buffer object
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            /* steps 1-6 */
            return thisArrayBufferView(cx, thisValue).getBuffer();
        }

        /**
         * 22.2.3.2 get %TypedArray%.prototype.byteLength
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the typed array length in bytes
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            ArrayBufferView view = thisArrayBufferView(cx, thisValue);
            /* steps 6-8 */
            return IsDetachedBuffer(view.getBuffer()) ? 0 : view.getByteLength();
        }

        /**
         * 22.2.3.3 get %TypedArray%.prototype.byteOffset
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the byte offset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            /* steps 1-5 */
            ArrayBufferView view = thisArrayBufferView(cx, thisValue);
            /* steps 6-8 */
            return IsDetachedBuffer(view.getBuffer()) ? 0 : view.getByteOffset();
        }

        /**
         * 22.2.3.17 get %TypedArray%.prototype.length
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the typed array length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            TypedArrayObject typedArray = thisTypedArrayObject(cx, thisValue);
            /* step 5 */
            ArrayBufferObject buffer = typedArray.getBuffer();
            /* step 6 */
            if (buffer == null) {
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            /* step 7 */
            if (IsDetachedBuffer(buffer)) {
                return 0;
            }
            /* steps 8-9 */
            return typedArray.getArrayLength();
        }

        /**
         * 22.2.3.22 %TypedArray%.prototype.set(array [ , offset ] ) <br>
         * 22.2.3.23 %TypedArray%.prototype.set(typedArray [ offset ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param array
         *            the source array
         * @param offset
         *            the target offset
         * @return the undefined value
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            if (!(array instanceof TypedArrayObject)) {
                // 22.2.3.22
                /* steps 1-5 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue);
                /* steps 6-7 */
                // TODO: spec issue? - does not follow the ToNumber(v) == ToInteger(v) pattern
                double targetOffset = (offset == UNDEFINED ? 0 : ToInteger(cx, offset));
                /* step 8 */
                if (targetOffset < 0) {
                    throw newRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* step 9 */
                ArrayBufferObject targetBuffer = target.getBuffer();
                /* step 10 */
                if (targetBuffer == null) {
                    throw newTypeError(cx, Messages.Key.UninitializedObject);
                }
                /* step 11 */
                if (IsDetachedBuffer(targetBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 12 */
                long targetLength = target.getArrayLength();
                /* steps 13, 15 */
                ElementType targetType = target.getElementType();
                /* step 14 */
                int targetElementSize = targetType.size();
                /* step 16 */
                long targetByteOffset = target.getByteOffset();
                /* steps 17-18 */
                ScriptObject src = ToObject(cx, array);
                /* step 19 */
                Object srcLen = Get(cx, src, "length");
                /* step 20 */
                double numberLength = ToNumber(cx, srcLen);
                /* steps 21-22 */
                double srcLength = ToInteger(numberLength);// TODO: spec bug - call ToLength()?
                /* step 23 */
                if (numberLength != srcLength || srcLength < 0) {
                    throw newRangeError(cx, Messages.Key.InvalidByteLength);
                }
                /* step 24 */
                if (srcLength + targetOffset > targetLength) {
                    throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
                }
                /* step 25 */
                long targetByteIndex = (long) (targetOffset * targetElementSize + targetByteOffset);
                /* step 27 */
                long limit = (long) (targetByteIndex + targetElementSize
                        * Math.min(srcLength, targetLength - targetOffset));
                /* steps 26, 28 */
                for (long k = 0; targetByteIndex < limit; ++k, targetByteIndex += targetElementSize) {
                    long pk = k;
                    Object kValue = Get(cx, src, pk);
                    double kNumber = ToNumber(cx, kValue);
                    if (IsDetachedBuffer(targetBuffer)) {
                        throw newTypeError(cx, Messages.Key.BufferDetached);
                    }
                    SetValueInBuffer(targetBuffer, targetByteIndex, targetType, kNumber);
                }
                /* step 29 */
                return UNDEFINED;
            } else {
                // 22.2.3.23
                TypedArrayObject typedArray = (TypedArrayObject) array;
                /* steps 1-5 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue);
                /* steps 6-7 */
                // TODO: spec issue? - does not follow the ToNumber(v) == ToInteger(v) pattern
                double targetOffset = (offset == UNDEFINED ? 0 : ToInteger(cx, offset));
                /* step 8 */
                if (targetOffset < 0) {
                    throw newRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* step 9 */
                ArrayBufferObject targetBuffer = target.getBuffer();
                /* step 10 */
                if (targetBuffer == null) {
                    throw newTypeError(cx, Messages.Key.UninitializedObject);
                }
                /* step 11 */
                if (IsDetachedBuffer(targetBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 13 */
                ArrayBufferObject srcBuffer = typedArray.getBuffer();
                /* step 14 */
                if (srcBuffer == null) {
                    throw newTypeError(cx, Messages.Key.UninitializedObject);
                }
                /* step 15 */
                if (IsDetachedBuffer(srcBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 12 */
                long targetLength = target.getArrayLength();
                /* steps 14-15 */
                ElementType targetType = target.getElementType();
                /* step 16 */
                int targetElementSize = targetType.size();
                /* step 17 */
                long targetByteOffset = target.getByteOffset();
                /* steps 18-19 */
                ElementType srcType = typedArray.getElementType();
                /* step 20 */
                int srcElementSize = srcType.size();
                /* step 21 */
                long srcLength = typedArray.getArrayLength();
                /* step 22 */
                long srcByteOffset = typedArray.getByteOffset();
                /* step 23 */
                if (srcLength + targetOffset > targetLength) {
                    throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
                }
                /* steps 24-25 */
                long srcByteIndex;
                if (SameValue(srcBuffer, targetBuffer)) {
                    srcBuffer = CloneArrayBuffer(cx, srcBuffer, srcByteOffset);
                    if (IsDetachedBuffer(targetBuffer)) {
                        throw newTypeError(cx, Messages.Key.BufferDetached);
                    }
                    srcByteIndex = 0;
                } else {
                    srcByteIndex = srcByteOffset;
                }
                /* step 26 */
                long targetByteIndex = (long) (targetOffset * targetElementSize + targetByteOffset);
                /* step 27 */
                long limit = (long) (targetByteIndex + targetElementSize
                        * Math.min(srcLength, targetLength - targetOffset));
                /* step 28 */
                for (; targetByteIndex < limit; srcByteIndex += srcElementSize, targetByteIndex += targetElementSize) {
                    double value = GetValueFromBuffer(srcBuffer, srcByteIndex, srcType);
                    SetValueInBuffer(targetBuffer, targetByteIndex, targetType, value);
                }
                /* step 29 */
                return UNDEFINED;
            }
        }

        /**
         * 22.2.3.27 %TypedArray%.prototype.subarray( [ begin [ , end ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param begin
         *            the begin position
         * @param end
         *            the end position
         * @return the new typed array
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            /* steps 1-4 */
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            /* step 5 */
            ArrayBufferObject buffer = array.getBuffer();
            /* step 6 */
            if (buffer == null) {
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            /* step 7 */
            long srcLength = array.getArrayLength();
            /* steps 8-9 */
            double beginInt = (begin == UNDEFINED ? 0 : ToInteger(cx, begin));
            /* step 10 */
            if (beginInt < 0) {
                beginInt = srcLength + beginInt;
            }
            /* step 11 */
            double beginIndex = Math.min(srcLength, Math.max(0, beginInt));
            /* steps 12-14 */
            double endInt = (end == UNDEFINED ? srcLength : ToInteger(cx, end));
            /* step 15 */
            if (endInt < 0) {
                endInt = srcLength + endInt;
            }
            /* step 16 */
            double endIndex = Math.max(0, Math.min(srcLength, endInt));
            /* step 17 */
            if (endIndex < beginIndex) {
                endIndex = beginIndex;
            }
            /* step 18 */
            double newLength = endIndex - beginIndex;
            /* steps 19-20 */
            ElementType elementType = array.getElementType();
            /* step 21 */
            int elementSize = elementType.size();
            /* step 22 */
            double srcByteOffset = array.getByteOffset();
            /* step 23 */
            double beginByteOffset = srcByteOffset + beginIndex * elementSize;
            /* steps 24-25 */
            Object constructor = Get(cx, array, "constructor");
            /* step 26 */
            if (!IsConstructor(constructor)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 27-28 */
            return ((Constructor) constructor).construct(cx, buffer, beginByteOffset, newLength);
        }

        /**
         * 22.2.3.29 %TypedArray%.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @return the string representation
         */
        @Value(name = "toString")
        public static Object toString(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "toString");
        }

        /**
         * 22.2.3.28 %TypedArray%.prototype.toLocaleString ([ reserved1 [ , reserved2 ] ])
         * 
         * @param cx
         *            the execution context
         * @return the locale specific string representation
         */
        @Value(name = "toLocaleString")
        public static Object toLocaleString(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "toLocaleString");
        }

        /**
         * 22.2.3.14 %TypedArray%.prototype.join ( separator )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param separator
         *            the separator string
         * @return the result string
         */
        @Function(name = "join", arity = 1)
        public static Object join(ExecutionContext cx, Object thisValue, Object separator) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.join(cx, array, len, separator);
        }

        /**
         * 22.2.3.21 %TypedArray%.prototype.reverse ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return this typed array object
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(ExecutionContext cx, Object thisValue) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.reverse(cx, array, len);
        }

        /**
         * 22.2.3.24 %TypedArray%.prototype.slice ( start, end )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start position
         * @param end
         *            the end position
         * @return the new typed array
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            /* steps 1-7 */
            TypedArrayObject o = thisTypedArrayObjectChecked(cx, thisValue);
            /* step 8 */
            long len = o.getArrayLength();
            /* steps 9-10 */
            double relativeStart = ToInteger(cx, start);
            /* step 11 */
            long k;
            if (relativeStart < 0) {
                k = (long) Math.max(len + relativeStart, 0);
            } else {
                k = (long) Math.min(relativeStart, len);
            }
            /* steps 12-13 */
            double relativeEnd;
            if (Type.isUndefined(end)) {
                relativeEnd = len;
            } else {
                relativeEnd = ToInteger(cx, end);
            }
            /* step 14 */
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max(len + relativeEnd, 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            /* step 15 */
            long count = Math.max(finall - k, 0);
            /* steps 16-17 */
            Object c = Get(cx, o, "constructor");
            /* steps 18-19 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            ScriptObject a = ((Constructor) c).construct(cx, count);
            /* steps 20-21 */
            for (long n = 0; k < finall; ++k, ++n) {
                long pk = k;
                Object kvalue = Get(cx, o, pk);
                Put(cx, a, n, kvalue, true);
            }
            /* step 22 */
            return a;
        }

        private static final class FunctionComparator implements Comparator<Double> {
            private final ExecutionContext cx;
            private final Callable comparefn;
            private final ArrayBufferObject buffer;

            FunctionComparator(ExecutionContext cx, Callable comparefn, ArrayBufferObject buffer) {
                this.cx = cx;
                this.comparefn = comparefn;
                this.buffer = buffer;
            }

            @Override
            public int compare(Double x, Double y) {
                double c = ToNumber(cx, comparefn.call(cx, UNDEFINED, x, y));
                if (IsDetachedBuffer(buffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                return (c < 0 ? -1 : c > 0 ? 1 : 0);
            }
        }

        private static Double[] toDoubleArray(double[] elements) {
            int length = elements.length;
            Double[] array = new Double[length];
            for (int i = 0; i < length; ++i) {
                array[i] = elements[i];
            }
            return array;
        }

        /**
         * 22.2.3.26 %TypedArray%.prototype.sort ( comparefn )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param comparefn
         *            the comparator function
         * @return this typed array object
         */
        @Function(name = "sort", arity = 1)
        public static Object sort(ExecutionContext cx, Object thisValue, Object comparefn) {
            /* steps 1-5 */
            TypedArrayObject obj = thisTypedArrayObjectChecked(cx, thisValue);
            /* step 6 */
            long len = obj.getArrayLength();

            // handle OOM early
            if (len > Integer.MAX_VALUE) {
                throw newInternalError(cx, Messages.Key.OutOfMemory);
            }
            int length = (int) len;
            if (length == 0) {
                return obj;
            }
            if (length == 1) {
                // just to trigger possible side-effects (which shouldn't actually be possible
                // unless there's a spec bug...)
                Object e = Get(cx, obj, 0);
                assert Type.isNumber(e);
                Put(cx, obj, 0, e, true);
                return obj;
            }

            double[] elements = new double[length];
            for (int i = 0; i < length; ++i) {
                int index = i;
                Object e = Get(cx, obj, index);
                assert Type.isNumber(e);
                elements[i] = Type.numberValue(e);
            }

            Double[] array;
            if (!Type.isUndefined(comparefn)) {
                if (!IsCallable(comparefn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                Comparator<Double> comparator = new FunctionComparator(cx, (Callable) comparefn,
                        obj.getBuffer());
                array = toDoubleArray(elements);
                try {
                    Arrays.sort(array, comparator);
                } catch (IllegalArgumentException e) {
                    // `IllegalArgumentException: Comparison method violates its general contract!`
                    // just ignore this exception...
                }
            } else {
                Arrays.sort(elements);
                array = toDoubleArray(elements);
            }

            for (int i = 0; i < length; ++i) {
                int p = i;
                Put(cx, obj, p, array[i], true);
            }

            return obj;
        }

        /**
         * 22.2.3.13 %TypedArray%.prototype.indexOf (searchElement [ , fromIndex ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchElement
         *            the search element
         * @param fromIndex
         *            the optional start index
         * @return the result index
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.indexOf(cx, array, len, searchElement, fromIndex);
        }

        /**
         * 22.2.3.16 %TypedArray%.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param searchElement
         *            the search element
         * @param fromIndex
         *            the optional start index
         * @return the result index
         */
        @Function(name = "lastIndexOf", arity = 1)
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue,
                Object searchElement, @Optional(Optional.Default.NONE) Object fromIndex) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.lastIndexOf(cx, array, len, searchElement, fromIndex);
        }

        /**
         * 22.2.3.7 %TypedArray%.prototype.every ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return {@code true} if every element matches
         */
        @Function(name = "every", arity = 1)
        public static Object every(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.every(cx, array, len, callbackfn, thisArg);
        }

        /**
         * 22.2.3.25 %TypedArray%.prototype.some ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return {@code true} if some elements match
         */
        @Function(name = "some", arity = 1)
        public static Object some(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.some(cx, array, len, callbackfn, thisArg);
        }

        /**
         * 22.2.3.12 %TypedArray%.prototype.forEach ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the undefined value
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.forEach(cx, array, len, callbackfn, thisArg);
        }

        /**
         * 22.2.3.18 %TypedArray%.prototype.map ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the mapped value
         */
        @Function(name = "map", arity = 1)
        public static Object map(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-3 */
            TypedArrayObject o = thisTypedArrayObjectChecked(cx, thisValue);
            /* step 4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* steps 7-8 */
            Object c = Get(cx, o, "constructor");
            /* steps 9-10 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            ScriptObject a = ((Constructor) c).construct(cx, len);
            /* steps 11-12 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Object kvalue = Get(cx, o, pk);
                Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                Put(cx, a, pk, mappedValue, true);
            }
            /* step 13 */
            return a;
        }

        /**
         * 22.2.3.9 %TypedArray%.prototype.filter ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument for the callback function
         * @return the filtered value
         */
        @Function(name = "filter", arity = 1)
        public static Object filter(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-6 */
            TypedArrayObject o = thisTypedArrayObjectChecked(cx, thisValue);
            /* step 7 */
            long len = o.getArrayLength();
            /* step 8 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 9 (omitted) */
            /* steps 10-11 */
            Object c = Get(cx, o, "constructor");
            /* step 12 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 13, 15 */
            ArrayList<Object> kept = new ArrayList<>();
            /* steps 14, 16 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Object kvalue = Get(cx, o, pk);
                Object selected = callback.call(cx, thisArg, kvalue, k, o);
                if (ToBoolean(selected)) {
                    kept.add(kvalue);
                }
            }
            /* steps 17-18 */
            ScriptObject a = ((Constructor) c).construct(cx, kept.size());
            /* steps 19-20 */
            for (int n = 0; n < kept.size(); ++n) {
                Object e = kept.get(n);
                Put(cx, a, n, e, true);
            }
            /* step 21 */
            return a;
        }

        /**
         * 22.2.3.19 %TypedArray%.prototype.reduce ( callbackfn [, initialValue] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param initialValue
         *            the initial value
         * @return the reduced value
         */
        @Function(name = "reduce", arity = 1)
        public static Object reduce(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.reduce(cx, array, len, callbackfn, initialValue);
        }

        /**
         * 22.2.3.20 %TypedArray%.prototype.reduceRight ( callbackfn [, initialValue] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param initialValue
         *            the initial value
         * @return the reduced value
         */
        @Function(name = "reduceRight", arity = 1)
        public static Object reduceRight(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.reduceRight(cx, array, len, callbackfn, initialValue);
        }

        /**
         * 22.2.3.10 %TypedArray%.prototype.find (predicate [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param predicate
         *            the predicate function
         * @param thisArg
         *            the optional this-argument for the predicate function
         * @return the result value
         */
        @Function(name = "find", arity = 1)
        public static Object find(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.find(cx, array, len, predicate, thisArg);
        }

        /**
         * 22.2.3.11 %TypedArray%.prototype.findIndex ( predicate [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param predicate
         *            the predicate function
         * @param thisArg
         *            the optional this-argument for the predicate function
         * @return the result index
         */
        @Function(name = "findIndex", arity = 1)
        public static Object findIndex(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.findIndex(cx, array, len, predicate, thisArg);
        }

        /**
         * 22.2.3.8 %TypedArray%.prototype.fill (value [ , start [ , end ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the fill value
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return this typed array object
         */
        @Function(name = "fill", arity = 1)
        public static Object fill(ExecutionContext cx, Object thisValue, Object value,
                Object start, Object end) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.fill(cx, array, len, value, start, end);
        }

        /**
         * 22.2.3.5 %TypedArray%.prototype.copyWithin (target, start [, end ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param target
         *            the target index
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return this typed array object
         */
        @Function(name = "copyWithin", arity = 2)
        public static Object copyWithin(ExecutionContext cx, Object thisValue, Object target,
                Object start, Object end) {
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            long len = array.getArrayLength();
            return ArrayPrototype.Properties.copyWithin(cx, array, len, target, start, end);
        }

        /**
         * 22.2.3.6 %TypedArray%.prototype.entries ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the entries iterator
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* steps 1-6 */
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            /* step 7 */
            return CreateArrayIterator(cx, array, ArrayIterationKind.KeyValue);
        }

        /**
         * 22.2.3.15 %TypedArray%.prototype.keys ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the keys iterator
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            /* steps 1-6 */
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            /* step7 */
            return CreateArrayIterator(cx, array, ArrayIterationKind.Key);
        }

        /**
         * 22.2.3.30 %TypedArray%.prototype.values ( )<br>
         * 22.2.3.31 %TypedArray%.prototype [ @@iterator ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the values iterator
         */
        @Function(name = "values", arity = 0)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-6 */
            TypedArrayObject array = thisTypedArrayObjectChecked(cx, thisValue);
            /* step 7 */
            return CreateArrayIterator(cx, array, ArrayIterationKind.Value);
        }

        /**
         * 22.2.3.32 get %TypedArray%.prototype [ @@toStringTag ]
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the toString tag
         */
        @Accessor(name = "get [Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                type = Accessor.Type.Getter, attributes = @Attributes(writable = false,
                        enumerable = false, configurable = true))
        public static Object toStringTag(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!(thisValue instanceof TypedArrayObject)) {
                return UNDEFINED;
            }
            TypedArrayObject array = (TypedArrayObject) thisValue;
            /* steps 4-5 */
            if (array.getElementType() == null) {
                return UNDEFINED;
            }
            /* step 6 (not applicable) */
            /* step 7 */
            return array.getTypedArrayName();
        }
    }
}
