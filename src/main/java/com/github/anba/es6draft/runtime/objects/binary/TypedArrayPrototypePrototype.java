/**
 * Copyright (c) 2012-2015 André Bargull
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
import static com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructorPrototype.AllocateTypedArray;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype.ArrayIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
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
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, AdditionalProperties.class);
    }

    /**
     * Marker class for {@code %TypedArray%.prototype.values}.
     */
    private static final class TypedArrayPrototypeValues {
    }

    public static boolean isBuiltinValues(Object next) {
        return next instanceof NativeFunction
                && ((NativeFunction) next).getId() == TypedArrayPrototypeValues.class;
    }

    /**
     * 22.2.3.5.1 Runtime Semantics: ValidateTypedArray ( O )
     * 
     * @param cx
     * @param thisValue
     * @return
     */
    private static TypedArrayObject ValidateTypedArray(ExecutionContext cx, Object thisValue) {
        /* steps 1-3 */
        if (!(thisValue instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject typedArray = (TypedArrayObject) thisValue;
        /* step 4 */
        ArrayBufferObject buffer = typedArray.getBuffer();
        /* step 5 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 6 (not applicable) */
        return typedArray;
    }

    /**
     * 22.2.3 Properties of the %TypedArrayPrototype% Object
     */
    public enum Properties {
        ;

        private static ArrayBufferView thisArrayBufferView(ExecutionContext cx, Object m) {
            if (m instanceof ArrayBufferView) {
                return (ArrayBufferView) m;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        private static TypedArrayObject thisTypedArrayObject(ExecutionContext cx, Object thisValue) {
            if (thisValue instanceof TypedArrayObject) {
                return (TypedArrayObject) thisValue;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        private static long ToArrayIndex(ExecutionContext cx, Object index, long length) {
            double relativeIndex = ToInteger(cx, index);
            if (relativeIndex < 0) {
                return (long) Math.max(length + relativeIndex, 0);
            }
            return (long) Math.min(relativeIndex, length);
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
            /* steps 1-3 */
            ArrayBufferView view = thisArrayBufferView(cx, thisValue);
            /* steps 4-5 */
            return view.getBuffer();
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
            /* steps 1-3 */
            ArrayBufferView view = thisArrayBufferView(cx, thisValue);
            /* steps 4-5 */
            if (IsDetachedBuffer(view.getBuffer())) {
                return 0;
            }
            /* steps 6-7 */
            return view.getByteLength();
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
            /* steps 1-3 */
            ArrayBufferView view = thisArrayBufferView(cx, thisValue);
            /* steps 4-5 */
            if (IsDetachedBuffer(view.getBuffer())) {
                return 0;
            }
            /* steps 6-7 */
            return view.getByteOffset();
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
            /* steps 5-6 */
            if (IsDetachedBuffer(typedArray.getBuffer())) {
                return 0;
            }
            /* steps 7-8 */
            return typedArray.getArrayLength();
        }

        /**
         * 22.2.3.22 %TypedArray%.prototype.set ( overloaded [ , offset ])
         * <ul>
         * <li>22.2.3.22.1 %TypedArray%.prototype.set (array [ , offset ] )
         * <li>22.2.3.22.2 %TypedArray%.prototype.set(typedArray [, offset ] )
         * </ul>
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
                // 22.2.3.22.1
                /* steps 1-5 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue);
                /* steps 6-7 */
                double targetOffset = ToInteger(cx, offset);
                /* step 8 */
                if (targetOffset < 0) {
                    throw newRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* step 9 */
                ArrayBufferObject targetBuffer = target.getBuffer();
                /* step 10 */
                if (IsDetachedBuffer(targetBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 11 */
                long targetLength = target.getArrayLength();
                /* steps 12, 14 */
                ElementType targetType = target.getElementType();
                /* step 13 */
                int targetElementSize = targetType.size();
                /* step 15 */
                long targetByteOffset = target.getByteOffset();
                /* steps 16-17 */
                ScriptObject src = ToObject(cx, array);
                /* steps 18-19 */
                long srcLength = ToLength(cx, Get(cx, src, "length"));
                /* step 20 */
                if (srcLength + targetOffset > targetLength) {
                    throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
                }
                long targetIndex = (long) targetOffset;
                /* step 21 */
                long targetByteIndex = targetIndex * targetElementSize + targetByteOffset;
                /* step 23 */
                long limit = targetByteIndex + targetElementSize * srcLength;
                /* steps 22, 24 */
                for (long k = 0; targetByteIndex < limit; ++k, targetByteIndex += targetElementSize) {
                    /* step 24.a */
                    long pk = k;
                    /* step 24.b-c */
                    double kNumber = ToNumber(cx, Get(cx, src, pk));
                    /* step 24.d */
                    if (IsDetachedBuffer(targetBuffer)) {
                        throw newTypeError(cx, Messages.Key.BufferDetached);
                    }
                    /* step 24.e */
                    SetValueInBuffer(targetBuffer, targetByteIndex, targetType, kNumber);
                }
                /* step 25 */
                return UNDEFINED;
            } else {
                // 22.2.3.22.2
                TypedArrayObject typedArray = (TypedArrayObject) array;
                /* steps 1-5 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue);
                /* steps 6-7 */
                double targetOffset = ToInteger(cx, offset);
                /* step 8 */
                if (targetOffset < 0) {
                    throw newRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* step 9 */
                ArrayBufferObject targetBuffer = target.getBuffer();
                /* step 10 */
                if (IsDetachedBuffer(targetBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 12 */
                ArrayBufferObject srcBuffer = typedArray.getBuffer();
                /* step 13 */
                if (IsDetachedBuffer(srcBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 11 */
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
                long targetIndex = (long) targetOffset;
                /* steps 24-25 */
                long srcByteIndex;
                if (srcBuffer == targetBuffer) {
                    srcBuffer = CloneArrayBuffer(cx, targetBuffer, srcByteOffset,
                            Intrinsics.ArrayBuffer);
                    assert !IsDetachedBuffer(targetBuffer);
                    srcByteIndex = 0;
                } else {
                    srcByteIndex = srcByteOffset;
                }
                /* step 26 */
                long targetByteIndex = targetIndex * targetElementSize + targetByteOffset;
                /* steps 27-29 */
                if (srcType != targetType) {
                    /* step 27 */
                    long limit = targetByteIndex + targetElementSize * srcLength;
                    /* step 28 */
                    for (; targetByteIndex < limit; srcByteIndex += srcElementSize, targetByteIndex += targetElementSize) {
                        /* step 28.a */
                        double value = GetValueFromBuffer(srcBuffer, srcByteIndex, srcType);
                        /* step 28.b */
                        SetValueInBuffer(targetBuffer, targetByteIndex, targetType, value);
                    }
                } else {
                    /* steps 27, 29 */
                    long countByteLength = targetElementSize * srcLength;
                    ByteBuffer srcData = srcBuffer.getData();
                    ByteBuffer targetData = targetBuffer.getData();
                    assert (srcByteIndex + countByteLength) <= srcData.capacity();
                    assert (targetByteIndex + countByteLength) <= targetData.capacity();
                    assert srcData != targetData;

                    srcData.limit((int) (srcByteIndex + countByteLength)).position(
                            (int) srcByteIndex);
                    targetData.limit((int) (targetByteIndex + countByteLength)).position(
                            (int) targetByteIndex);
                    targetData.put(srcData);
                    srcData.clear();
                    targetData.clear();
                }
                /* step 30 */
                return UNDEFINED;
            }
        }

        /**
         * 22.2.3.26 %TypedArray%.prototype.subarray( [ begin [ , end ] ] )
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
            long srcLength = array.getArrayLength();
            /* steps 7-9 */
            long beginIndex = ToArrayIndex(cx, begin, srcLength);
            /* steps 10-12 */
            long endIndex = Type.isUndefined(end) ? srcLength : ToArrayIndex(cx, end, srcLength);
            /* step 13 */
            long newLength = Math.max(endIndex - beginIndex, 0);
            /* steps 14-15 */
            int elementSize = array.getElementType().size();
            /* step 16 */
            long srcByteOffset = array.getByteOffset();
            /* step 17 */
            long beginByteOffset = srcByteOffset + beginIndex * elementSize;
            /* step 18 */
            Intrinsics defaultConstructor = array.getElementType().getConstructor();
            /* steps 19-20 */
            Constructor constructor = SpeciesConstructor(cx, array, defaultConstructor);
            /* steps 21-22 */
            return constructor.construct(cx, constructor, buffer, beginByteOffset, newLength);
        }

        /**
         * 22.2.3.28 %TypedArray%.prototype.toString ( )
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
         * 22.2.3.27 %TypedArray%.prototype.toLocaleString ([ reserved1 [ , reserved2 ] ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the optional locales array
         * @param options
         *            the optional options object
         * @return the locale specific string representation
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales,
                Object options) {
            // 22.1.3.26 Array.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] )<br>
            // 13.4.1 Array.prototype.toLocaleString([locales [, options ]])
            /* steps 1-2 */
            TypedArrayObject array = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = array.getArrayLength();
            /* step 5 */
            String separator = cx.getRealm().getListSeparator();
            /* step 6 */
            if (len == 0) {
                return "";
            }
            /* steps 7-8 */
            Double firstElement = array.elementGetDirect(cx, 0);
            /* steps 9-10 */
            StringBuilder r = new StringBuilder();
            r.append(ToString(cx, Invoke(cx, firstElement, "toLocaleString", locales, options)));
            /* steps 11-12 */
            for (long k = 1; k < len; ++k) {
                Double nextElement = array.elementGetDirect(cx, k);
                r.append(separator).append(
                        ToString(cx, Invoke(cx, nextElement, "toLocaleString", locales, options)));
            }
            /* step 13 */
            return r.toString();
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
            // 22.1.3.12 Array.prototype.join (separator)
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (Type.isUndefined(separator)) {
                separator = ",";
            }
            /* steps 6-7 */
            String sep = ToFlatString(cx, separator);
            /* step 8 */
            if (len == 0) {
                return "";
            }
            /* step 9 */
            double element0 = o.elementGetDirect(cx, 0);
            /* steps 10-11 */
            StringBuilder r = new StringBuilder();
            r.append(ToString(element0));
            /* steps 12-13 */
            for (long k = 1; k < len; ++k) {
                double element = o.elementGetDirect(cx, k);
                r.append(sep).append(ToString(element));
            }
            /* step 14 */
            return r.toString();
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
            // 22.1.3.20 Array.prototype.reverse ( )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            final long middle = len / 2L;
            /* steps 6-7 */
            for (long lower = 0; lower != middle; ++lower) {
                long upper = len - lower - 1;
                long upperP = upper;
                long lowerP = lower;
                double lowerValue = o.elementGetDirect(cx, lowerP);
                double upperValue = o.elementGetDirect(cx, upperP);

                o.elementSetDirect(cx, lowerP, upperValue);
                o.elementSetDirect(cx, upperP, lowerValue);
            }
            /* step 8 */
            return o;
        }

        /**
         * 22.2.3.23 %TypedArray%.prototype.slice ( start, end )
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
            /* steps 1-3 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            long len = o.getArrayLength();
            /* steps 5-7 */
            long k = ToArrayIndex(cx, start, len);
            /* steps 8-10 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 11 */
            long count = Math.max(finall - k, 0);
            /* step 12 */
            Intrinsics defaultConstructor = o.getElementType().getConstructor();
            /* steps 13-14 */
            Constructor c = SpeciesConstructor(cx, o, defaultConstructor);
            /* steps 15-16 */
            TypedArrayObject a = AllocateTypedArray(cx, c, count);
            /* steps 17-18 */
            ElementType srcType = o.getElementType();
            /* steps 19-20 */
            ElementType targetType = a.getElementType();
            /* steps 21-22 */
            if (srcType != targetType) {
                /* step 21 */
                /* steps 21.a-b */
                for (long n = 0; k < finall; ++k, ++n) {
                    /* step 21.b.i */
                    long pk = k;
                    /* step 21.b.ii-iii */
                    double kvalue = o.elementGetDirect(cx, pk);
                    /* step 21.b.iv-v */
                    a.elementSetDirect(cx, n, kvalue);
                }
            } else if (count > 0) {
                /* step 22 */
                /* step 22.a */
                ArrayBufferObject srcBuffer = o.getBuffer();
                /* step 22.b */
                if (IsDetachedBuffer(srcBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 22.c */
                ArrayBufferObject targetBuffer = a.getBuffer();
                /* step 22.d */
                int elementSize = srcType.size();
                /* step 22.e (note) */
                /* step 22.f */
                long srcByteOffset = o.getByteOffset();
                /* step 22.g */
                long targetByteIndex = 0;
                /* step 22.h */
                long srcByteIndex = srcByteOffset + k * elementSize;
                /* step 22.i */
                ByteBuffer srcData = srcBuffer.getData();
                ByteBuffer targetData = targetBuffer.getData();
                long countByteLength = count * elementSize;
                assert (srcByteIndex + countByteLength) <= srcData.capacity();
                assert countByteLength <= targetData.capacity();
                assert srcData != targetData;

                srcData.limit((int) (srcByteIndex + countByteLength)).position((int) srcByteIndex);
                targetData.limit((int) countByteLength).position((int) targetByteIndex);
                targetData.put(srcData);
                srcData.clear();
                targetData.clear();
            }
            /* step 23 */
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

        /**
         * 22.2.3.25 %TypedArray%.prototype.sort ( comparefn )
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
            /* steps 1-3 */
            TypedArrayObject obj = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            long len = obj.getArrayLength();

            // return if array is empty or has only one element
            if (len <= 1) {
                return obj;
            }
            // handle OOM early
            if (len > Integer.MAX_VALUE) {
                throw newInternalError(cx, Messages.Key.OutOfMemory);
            }
            int length = (int) len;

            if (!Type.isUndefined(comparefn)) {
                if (!IsCallable(comparefn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                Double[] elements = new Double[length];
                for (int i = 0; i < length; ++i) {
                    elements[i] = obj.elementGetDirect(cx, i);
                }

                Comparator<Double> comparator = new FunctionComparator(cx, (Callable) comparefn,
                        obj.getBuffer());
                try {
                    Arrays.sort(elements, comparator);
                } catch (IllegalArgumentException e) {
                    // `IllegalArgumentException: Comparison method violates its general contract!`
                    // just ignore this exception...
                }

                for (int i = 0; i < length; ++i) {
                    obj.elementSetDirect(cx, i, (double) elements[i]);
                }
            } else {
                double[] elements = new double[length];
                for (int i = 0; i < length; ++i) {
                    elements[i] = obj.elementGetDirect(cx, i);
                }

                Arrays.sort(elements);

                for (int i = 0; i < length; ++i) {
                    obj.elementSetDirect(cx, i, elements[i]);
                }
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
            // 22.1.3.11 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (len == 0) {
                return -1;
            }
            /* steps 6-7 */
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(cx, fromIndex);
            } else {
                n = 0;
            }
            /* step 8 */
            if (n >= len) {
                return -1;
            }
            /* steps 9-10 */
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = len - Math.abs(n);
                if (k < 0) {
                    k = 0;
                }
            }
            /* step 11 */
            if (Type.isNumber(searchElement) && !Double.isNaN(Type.numberValue(searchElement))) {
                double needle = Type.numberValue(searchElement);
                for (; k < len; ++k) {
                    long pk = k;
                    double elementk = o.elementGetDirect(cx, pk);
                    boolean same = needle == elementk; // StrictEqualityComparison
                    if (same) {
                        return k;
                    }
                }
            } else if (k < len && IsDetachedBuffer(o.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* step 12 */
            return -1;
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
            // 22.1.3.14 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (len == 0) {
                return -1;
            }
            /* steps 6-7 */
            long n;
            if (fromIndex != null) {
                n = (long) ToInteger(cx, fromIndex);
            } else {
                n = len - 1;
            }
            /* steps 8-9 */
            long k;
            if (n >= 0) {
                k = Math.min(n, len - 1);
            } else {
                k = len - Math.abs(n);
            }
            /* step 10 */
            if (Type.isNumber(searchElement) && !Double.isNaN(Type.numberValue(searchElement))) {
                double needle = Type.numberValue(searchElement);
                for (; k >= 0; --k) {
                    long pk = k;
                    double elementk = o.elementGetDirect(cx, pk);
                    boolean same = needle == elementk; // StrictEqualityComparison
                    if (same) {
                        return k;
                    }
                }
            } else if (k >= 0 && IsDetachedBuffer(o.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* step 11 */
            return -1;
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
            // 22.1.3.5 Array.prototype.every ( callbackfn [ , thisArg ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* steps 7-8 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                boolean testResult = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                if (!testResult) {
                    return false;
                }
            }
            /* step 9 */
            return true;
        }

        /**
         * 22.2.3.24 %TypedArray%.prototype.some ( callbackfn [ , thisArg ] )
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
            // 22.1.3.23 Array.prototype.some ( callbackfn [ , thisArg ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* steps 7-8 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                boolean testResult = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                if (testResult) {
                    return true;
                }
            }
            /* step 9 */
            return false;
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
            // 22.1.3.10 Array.prototype.forEach ( callbackfn [ , thisArg ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* steps 7-8 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                callback.call(cx, thisArg, kvalue, k, o);
            }
            /* step 9 */
            return UNDEFINED;
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
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* step 7 */
            Intrinsics defaultConstructor = o.getElementType().getConstructor();
            /* steps 8-9 */
            Constructor c = SpeciesConstructor(cx, o, defaultConstructor);
            /* steps 10-11 */
            TypedArrayObject a = AllocateTypedArray(cx, c, len);
            /* steps 12-13 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                a.elementSetDirect(cx, pk, ToNumber(cx, mappedValue));
            }
            /* step 14 */
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
            /* steps 1-3 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* step 7 */
            Intrinsics defaultConstructor = o.getElementType().getConstructor();
            /* steps 8-9 */
            Constructor c = SpeciesConstructor(cx, o, defaultConstructor);
            /* steps 10, 12 */
            int captured = 0;
            double[] kept = new double[(int) Math.min(len, 1024)];
            /* steps 11, 13 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                double kvalue = o.elementGetDirect(cx, pk);
                boolean selected = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                if (selected) {
                    if (captured == kept.length) {
                        kept = Arrays.copyOf(kept, captured + (captured >> 1));
                    }
                    kept[captured++] = kvalue;
                }
            }
            /* steps 14-15 */
            TypedArrayObject a = AllocateTypedArray(cx, c, captured);
            /* steps 16-17 */
            for (int n = 0; n < captured; ++n) {
                double e = kept[n];
                a.elementSetDirect(cx, n, e);
            }
            /* step 18 */
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
            // 22.1.3.18 Array.prototype.reduce ( callbackfn [ , initialValue ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 7 */
            long k = 0;
            /* steps 8-9 */
            Object accumulator;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                accumulator = o.elementGetDirect(cx, k++);
            }
            /* step 10 */
            for (; k < len; ++k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
            }
            /* step 11 */
            return accumulator;
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
            // 22.1.3.19 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 7 */
            long k = len - 1;
            /* steps 8-9 */
            Object accumulator;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                accumulator = o.elementGetDirect(cx, k--);
            }
            /* step 10 */
            for (; k >= 0; --k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
            }
            /* step 11 */
            return accumulator;
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
            // 22.1.3.8 Array.prototype.find ( predicate [ , thisArg ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 6 (omitted) */
            /* steps 7-8 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                boolean testResult = ToBoolean(pred.call(cx, thisArg, kvalue, k, o));
                if (testResult) {
                    return kvalue;
                }
            }
            /* step 9 */
            return UNDEFINED;
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
            // 22.1.3.9 Array.prototype.findIndex ( predicate [ , thisArg ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 6 (omitted) */
            /* steps 7-8 */
            for (long k = 0; k < len; ++k) {
                long pk = k;
                Double kvalue = o.elementGetDirect(cx, pk);
                boolean testResult = ToBoolean(pred.call(cx, thisArg, kvalue, k, o));
                if (testResult) {
                    return k;
                }
            }
            /* step 9 */
            return -1;
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
            // 22.1.3.6 Array.prototype.fill (value [ , start [ , end ] ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* steps 5-7 */
            long k = ToArrayIndex(cx, start, len);
            /* steps 8-10 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 11 */
            for (; k < finall; ++k) {
                long pk = k;
                o.elementSetDirect(cx, pk, ToNumber(cx, value));
            }
            /* step 12 */
            return o;
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
            // 22.1.3.3 Array.prototype.copyWithin (target, start [, end ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* steps 3-4 */
            long len = o.getArrayLength();
            /* steps 5-7 */
            long to = ToArrayIndex(cx, target, len);
            /* steps 8-10 */
            long from = ToArrayIndex(cx, start, len);
            /* steps 11-13 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 14 */
            long count = Math.min(finall - from, len - to);
            /* steps 15-17 */
            if (count > 0) {
                ArrayBufferObject buffer = o.getBuffer();
                if (IsDetachedBuffer(buffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                int elementSize = o.getElementType().size();
                long toByteIndex = to * elementSize;
                long fromByteIndex = from * elementSize;
                long countByteLength = count * elementSize;
                ByteBuffer data = buffer.getData();
                assert toByteIndex < data.capacity();
                assert (fromByteIndex + countByteLength) <= data.capacity();

                ByteBuffer dup = data.duplicate();
                data.limit((int) (toByteIndex + countByteLength)).position((int) toByteIndex);
                dup.limit((int) (fromByteIndex + countByteLength)).position((int) fromByteIndex);
                data.put(dup).clear();
            }
            /* step 18 */
            return o;
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
            /* steps 1-3 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.KeyValue);
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
            /* steps 1-3 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Key);
        }

        /**
         * 22.2.3.29 %TypedArray%.prototype.values ( )<br>
         * 22.2.3.30 %TypedArray%.prototype [ @@iterator ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the values iterator
         */
        @Function(name = "values", arity = 0, nativeId = TypedArrayPrototypeValues.class)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
        }

        /**
         * 22.2.3.31 get %TypedArray%.prototype [ @@toStringTag ]
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
            /* steps 4-6 */
            return array.getTypedArrayName();
        }
    }

    /**
     * Proposed ECMAScript 7 additions
     */
    @CompatibilityExtension(CompatibilityOption.ArrayIncludes)
    public enum AdditionalProperties {
        ;

        /**
         * %TypedArray%.prototype.includes ( searchElement [ , fromIndex ] )
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
        @Function(name = "includes", arity = 1)
        public static Object includes(ExecutionContext cx, Object thisValue, Object searchElement,
                Object fromIndex) {
            /* steps 1-3 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue);
            /* step 4 */
            long len = o.getArrayLength();
            /* step 5 */
            if (len == 0) {
                return false;
            }
            /* steps 6-7 */
            long n = (long) ToInteger(cx, fromIndex);
            /* steps 8-9 */
            long k;
            if (n >= 0) {
                k = n;
            } else {
                k = len + n;
                if (k < 0) {
                    k = 0;
                }
            }
            /* step 10 */
            for (; k < len; ++k) {
                /* steps 10.a-b */
                Object element = o.elementGetDirect(cx, k);
                /* step 10.c */
                if (SameValueZero(searchElement, element)) {
                    return true;
                }
            }
            /* step 11 */
            return false;
        }
    }
}
