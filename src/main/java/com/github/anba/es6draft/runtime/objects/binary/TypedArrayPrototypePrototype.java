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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Optional;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
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
public class TypedArrayPrototypePrototype extends OrdinaryObject implements Initialisable {
    public TypedArrayPrototypePrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 22.2.3 Properties of the %TypedArrayPrototype% Object
     */
    public enum Properties {
        ;

        private static ArrayBufferView thisArrayBufferView(ExecutionContext cx, Object m) {
            if (m instanceof ArrayBufferView) {
                ArrayBufferView view = (ArrayBufferView) m;
                if (view.getBuffer() != null) {
                    return view;
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

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
         * 22.2.3.4 %TypedArray%.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.TypedArray;

        /**
         * 22.2.3.1 get %TypedArray%.prototype.buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return thisArrayBufferView(cx, thisValue).getBuffer();
        }

        /**
         * 22.2.3.2 get %TypedArray%.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return thisArrayBufferView(cx, thisValue).getByteLength();
        }

        /**
         * 22.2.3.3 get %TypedArray%.prototype.byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return thisArrayBufferView(cx, thisValue).getByteOffset();
        }

        /**
         * 22.2.3.17 get %TypedArray%.prototype.length
         */
        @Accessor(name = "length", type = Accessor.Type.Getter)
        public static Object length(ExecutionContext cx, Object thisValue) {
            return thisTypedArrayObject(cx, thisValue).getArrayLength();
        }

        /**
         * 22.2.3.22 %TypedArray%.prototype.set(array, offset = 0 )<br>
         * 22.2.3.23 %TypedArray%.prototype.set(typedArray, offset = 0 )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            if (!(array instanceof TypedArrayObject)) {
                // 22.2.3.22
                /* steps 1-5, 7 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue);
                /* step 6 */
                ArrayBufferObject targetBuffer = target.getBuffer();
                /* step 8 */
                long targetLength = target.getArrayLength();
                /* steps 9-10 */
                double targetOffset = (offset == UNDEFINED ? 0 : ToInteger(cx, offset));
                /* step 11 */
                if (targetOffset < 0) {
                    throwRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* steps 12, 14 */
                ElementType targetType = target.getElementType();
                /* step 13 */
                int targetElementSize = targetType.size();
                /* step 15 */
                long targetByteOffset = target.getByteOffset();
                /* steps 16-17 */
                ScriptObject src = ToObject(cx, array);
                /* step 18 */
                Object srcLen = Get(cx, src, "length");
                /* step 19 */
                double numberLength = ToNumber(cx, srcLen);
                /* steps 20-21 */
                double srcLength = ToInteger(numberLength);
                /* step 22 */
                if (numberLength != srcLength || srcLength < 0) {
                    throwRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* step 23 */
                if (srcLength + targetOffset > targetLength) {
                    throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
                }
                /* step 24 */
                long targetByteIndex = (long) (targetOffset * targetElementSize + targetByteOffset);
                /* step 26 */
                long limit = (long) (targetByteIndex + targetElementSize
                        * Math.min(srcLength, targetLength - targetOffset));
                /* steps 24, 27 */
                for (long k = 0; targetByteIndex < limit; ++k, targetByteIndex += targetElementSize) {
                    String pk = ToString(k);
                    Object kValue = Get(cx, src, pk);
                    double kNumber = ToNumber(cx, kValue);
                    SetValueInBuffer(cx, targetBuffer, targetByteIndex, targetType, kNumber);
                }
                /* step 28 */
                return UNDEFINED;
            } else {
                // 22.2.3.23
                TypedArrayObject typedArray = (TypedArrayObject) array;
                /* steps 1-6, 8 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue);
                /* step 7 */
                ArrayBufferObject targetBuffer = target.getBuffer();
                /* step 9 */
                ArrayBufferObject srcBuffer = typedArray.getBuffer();
                /* step 10 */
                if (srcBuffer == null) {
                    throw throwTypeError(cx, Messages.Key.IncompatibleObject);
                }
                /* step 11 */
                long targetLength = target.getArrayLength();
                /* steps 12-13 */
                double targetOffset = (offset == UNDEFINED ? 0 : ToInteger(cx, offset));
                /* step 14 */
                if (targetOffset < 0) {
                    throwRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* steps 15-16 */
                ElementType targetType = target.getElementType();
                /* step 17 */
                int targetElementSize = targetType.size();
                /* step 18 */
                long targetByteOffset = target.getByteOffset();
                /* steps 19-20 */
                ElementType srcType = typedArray.getElementType();
                /* step 21 */
                int srcElementSize = srcType.size();
                /* step 22 */
                long srcLength = typedArray.getArrayLength();
                /* step 23 */
                long srcByteOffset = typedArray.getByteOffset();
                /* step 24 */
                if (srcLength + targetOffset > targetLength) {
                    throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
                }
                /* steps 25-26 */
                long srcByteIndex;
                if (SameValue(srcBuffer, targetBuffer)) {
                    srcBuffer = CloneArrayBuffer(cx, srcBuffer, srcByteOffset, srcType, srcType,
                            srcLength);
                    assert srcBuffer.getByteLength() == srcLength * srcType.size();
                    srcByteIndex = 0;
                } else {
                    srcByteIndex = srcByteOffset;
                }
                /* step 27 */
                long targetByteIndex = (long) (targetOffset * targetElementSize + targetByteOffset);
                /* step 28 */
                long limit = (long) (targetByteIndex + targetElementSize
                        * Math.min(srcLength, targetLength - targetOffset));
                /* step 29 */
                for (; targetByteIndex < limit; srcByteIndex += srcElementSize, targetByteIndex += targetElementSize) {
                    double value = GetValueFromBuffer(cx, srcBuffer, srcByteIndex, srcType);
                    SetValueInBuffer(cx, targetBuffer, targetByteIndex, targetType, value);
                }
                /* step 30 */
                return UNDEFINED;
            }
        }

        /**
         * 22.2.3.27 %TypedArray%.prototype.subarray(begin = 0, end = this.length )
         */
        @Function(name = "subarray", arity = 2)
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin,
                Object end) {
            /* steps 1-4, 6 */
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            /* step 5 */
            ArrayBufferObject buffer = array.getBuffer();
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
                throwTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 27-28 */
            return ((Constructor) constructor).construct(cx, buffer, beginByteOffset, newLength);
        }

        /**
         * 22.2.3.29 %TypedArray%.prototype.toString ( )
         */
        @Value(name = "toString")
        public static Object toString(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "toString");
        }

        /**
         * 22.2.3.28 %TypedArray%.prototype.toLocaleString ( )
         */
        @Value(name = "toLocaleString")
        public static Object toLocaleString(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "toLocaleString");
        }

        /**
         * 22.2.3.14 %TypedArray%.prototype.join ( separator )
         */
        @Function(name = "join", arity = 1)
        public static Object join(ExecutionContext cx, Object thisValue, Object separator) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.join(cx, array, separator);
        }

        /**
         * 22.2.3.21 %TypedArray%.prototype.reverse ( )
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(ExecutionContext cx, Object thisValue) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.reverse(cx, array);
        }

        /**
         * 22.2.3.24 %TypedArray%.prototype.slice ( start, end )
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            /* steps 1-3 */
            ScriptObject o = thisTypedArrayObject(cx, thisValue);
            /* step 4 */
            Object lenVal = Get(cx, o, "length");
            /* steps 5-6 */
            long len = ToLength(cx, lenVal);
            /* steps 7-8 */
            double relativeStart = ToInteger(cx, start);
            /* step 9 */
            long k;
            if (relativeStart < 0) {
                k = (long) Math.max(len + relativeStart, 0);
            } else {
                k = (long) Math.min(relativeStart, len);
            }
            /* steps 10-11 */
            double relativeEnd;
            if (Type.isUndefined(end)) {
                relativeEnd = len;
            } else {
                relativeEnd = ToInteger(cx, end);
            }
            /* step 12 */
            long finall;
            if (relativeEnd < 0) {
                finall = (long) Math.max(len + relativeEnd, 0);
            } else {
                finall = (long) Math.min(relativeEnd, len);
            }
            /* step 13 */
            long count = Math.max(finall - k, 0);
            /* steps 14-15 */
            Object c = Get(cx, o, "constructor");
            /* steps 16-17 */
            ScriptObject a;
            if (IsConstructor(c)) {
                a = ((Constructor) c).construct(cx, count);
            } else {
                throw throwTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 18-19 */
            long n = 0;
            for (; k < finall; ++k, ++n) {
                String pk = ToString(k);
                Object kvalue = Get(cx, o, pk);
                Put(cx, a, ToString(n), kvalue, true);
            }
            /* step 20 */
            return a;
        }

        private static class DefaultComparator implements Comparator<Double> {
            @Override
            public int compare(Double x, Double y) {
                return Double.compare(x, y);
            }
        }

        private static class FunctionComparator implements Comparator<Double> {
            private final ExecutionContext cx;
            private final Callable comparefn;

            FunctionComparator(ExecutionContext cx, Callable comparefn) {
                this.cx = cx;
                this.comparefn = comparefn;
            }

            @Override
            public int compare(Double x, Double y) {
                double c = ToInteger(cx, comparefn.call(cx, UNDEFINED, x, y));
                return (c == 0 ? 0 : c < 0 ? -1 : 1);
            }
        }

        /**
         * 22.2.3.26 %TypedArray%.prototype.sort ( comparefn )
         */
        @Function(name = "sort", arity = 1)
        public static Object sort(ExecutionContext cx, Object thisValue, Object comparefn) {
            ScriptObject obj = thisTypedArrayObject(cx, thisValue);
            long len = ToUint32(cx, Get(cx, obj, "length"));

            int undefCount = 0;
            List<Double> elements = new ArrayList<>((int) Math.min(len, 1024));
            for (int i = 0; i < len; ++i) {
                String index = ToString(i);
                Object e = Get(cx, obj, index);
                if (!Type.isUndefined(e)) {
                    elements.add(ToNumber(cx, e));
                } else {
                    undefCount += 1;
                }
            }

            int count = elements.size();
            if (count > 1) {
                Comparator<Double> comparator;
                if (!Type.isUndefined(comparefn)) {
                    if (!IsCallable(comparefn)) {
                        throw throwTypeError(cx, Messages.Key.NotCallable);
                    }
                    comparator = new FunctionComparator(cx, (Callable) comparefn);
                } else {
                    comparator = new DefaultComparator();
                }
                try {
                    Collections.sort(elements, comparator);
                } catch (IllegalArgumentException e) {
                    // `IllegalArgumentException: Comparison method violates its general contract!`
                    // just ignore this exception...
                }
            }

            for (int i = 0, offset = 0; i < count; ++i) {
                String p = ToString(offset + i);
                Put(cx, obj, p, elements.get(i), true);
            }
            for (int i = 0, offset = count; i < undefCount; ++i) {
                String p = ToString(offset + i);
                Put(cx, obj, p, UNDEFINED, true);
            }

            return obj;
        }

        /**
         * 22.2.3.13 %TypedArray%.prototype.indexOf (searchElement, fromIndex = 0 )
         */
        @Function(name = "indexOf", arity = 1)
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.indexOf(cx, array, searchElement, fromIndex);
        }

        /**
         * 22.2.3.16 %TypedArray%.prototype.lastIndexOf (searchElement, fromIndex=this.length-1)
         */
        @Function(name = "lastIndexOf", arity = 1)
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue,
                Object searchElement, @Optional(Optional.Default.NONE) Object fromIndex) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.lastIndexOf(cx, array, searchElement, fromIndex);
        }

        /**
         * 22.2.3.7 %TypedArray%.prototype.every ( callbackfn, thisArg = undefined )
         */
        @Function(name = "every", arity = 1)
        public static Object every(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.every(cx, array, callbackfn, thisArg);
        }

        /**
         * 22.2.3.25 %TypedArray%.prototype.some ( callbackfn, thisArg = undefined )
         */
        @Function(name = "some", arity = 1)
        public static Object some(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.some(cx, array, callbackfn, thisArg);
        }

        /**
         * 22.2.3.12 %TypedArray%.prototype.forEach ( callbackfn, thisArg = undefined )
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.forEach(cx, array, callbackfn, thisArg);
        }

        /**
         * 22.2.3.18 %TypedArray%.prototype.map ( callbackfn, thisArg = undefined )
         */
        @Function(name = "map", arity = 1)
        public static Object map(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-3 */
            TypedArrayObject o = thisTypedArrayObject(cx, thisValue);
            /* step 4 */
            Object lenValue = Get(cx, o, "length");
            /* steps 5-6 */
            long len = ToLength(cx, lenValue);
            /* step 7 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 8 (omitted) */
            /* steps 9-10 */
            Object c = Get(cx, o, "constructor");
            /* steps 11-12 */
            ScriptObject a;
            if (IsConstructor(c)) {
                a = ((Constructor) c).construct(cx, len);
            } else {
                throw throwTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 13-14 */
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object kvalue = Get(cx, o, pk);
                Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                Put(cx, a, pk, mappedValue, true);
            }
            /* step 15 */
            return a;
        }

        /**
         * 22.2.3.9 %TypedArray%.prototype.filter ( callbackfn, thisArg = undefined )
         */
        @Function(name = "filter", arity = 1)
        public static Object filter(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-3 */
            TypedArrayObject o = thisTypedArrayObject(cx, thisValue);
            /* step 4 */
            Object lenValue = Get(cx, o, "length");
            /* steps 5-6 */
            long len = ToLength(cx, lenValue);
            /* step 7 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 8 (omitted) */
            /* steps 9-10 */
            Object c = Get(cx, o, "constructor");
            /* step 11 */
            if (!IsConstructor(c)) {
                throw throwTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 12, 14 */
            List<Object> kept = new ArrayList<>();
            /* steps 13, 15 */
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object kvalue = Get(cx, o, pk);
                Object selected = callback.call(cx, thisArg, kvalue, k, o);
                if (ToBoolean(selected)) {
                    kept.add(kvalue);
                }
            }
            /* steps 16-17 */
            ScriptObject a = ((Constructor) c).construct(cx, kept.size());
            /* steps 18-19 */
            for (int n = 0; n < kept.size(); ++n) {
                Object e = kept.get(n);
                Put(cx, a, ToString(n), e, true);
            }
            /* step 20 */
            return a;
        }

        /**
         * 22.2.3.19 %TypedArray%.prototype.reduce ( callbackfn [, initialValue] )
         */
        @Function(name = "reduce", arity = 1)
        public static Object reduce(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.reduce(cx, array, callbackfn, initialValue);
        }

        /**
         * 22.2.3.20 %TypedArray%.prototype.reduceRight ( callbackfn [, initialValue] )
         */
        @Function(name = "reduceRight", arity = 1)
        public static Object reduceRight(ExecutionContext cx, Object thisValue, Object callbackfn,
                @Optional(Optional.Default.NONE) Object initialValue) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.reduceRight(cx, array, callbackfn, initialValue);
        }

        /**
         * 22.2.3.10 %TypedArray%.prototype.find (predicate, thisArg = undefined)
         */
        @Function(name = "find", arity = 1)
        public static Object find(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.find(cx, array, predicate, thisArg);
        }

        /**
         * 22.2.3.11 %TypedArray%.prototype.findIndex ( predicate, thisArg = undefined )
         */
        @Function(name = "findIndex", arity = 1)
        public static Object findIndex(ExecutionContext cx, Object thisValue, Object predicate,
                Object thisArg) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.findIndex(cx, array, predicate, thisArg);
        }

        /**
         * 22.2.3.8 %TypedArray%.prototype.fill (value, start = 0, end = this.length )
         */
        @Function(name = "fill", arity = 1)
        public static Object fill(ExecutionContext cx, Object thisValue, Object value,
                Object start, Object end) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.fill(cx, array, value, start, end);
        }

        /**
         * 22.2.3.5 %TypedArray%.prototype.copyWithin (target, start, end = this.length )
         */
        @Function(name = "copyWithin", arity = 2)
        public static Object copyWithin(ExecutionContext cx, Object thisValue, Object target,
                Object start, Object end) {
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue);
            return ArrayPrototype.Properties.copyWithin(cx, array, target, start, end);
        }

        /**
         * 22.2.3.6 %TypedArray%.prototype.entries ( )
         */
        @Value(name = "entries")
        public static Object entries(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "entries");
        }

        /**
         * 22.2.3.15 %TypedArray%.prototype.keys ( )
         */
        @Value(name = "keys")
        public static Object keys(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "keys");
        }

        /**
         * 22.2.3.30 %TypedArray%.prototype.values ( )
         */
        @Value(name = "values")
        public static Object values(ExecutionContext cx) {
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "values");
        }

        /**
         * 22.2.3.31 %TypedArray%.prototype [ @@iterator ] ( )
         */
        @Value(name = "@@iterator", symbol = BuiltinSymbol.iterator)
        public static Object iterator(ExecutionContext cx) {
            // same as %TypedArray%.prototype.values
            return Get(cx, cx.getIntrinsic(Intrinsics.ArrayPrototype), "values");
        }

        /**
         * 22.2.3.32 get %TypedArray%.prototype [ @@toStringTag ]
         */
        @Accessor(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag,
                type = Accessor.Type.Getter, attributes = @Attributes(writable = false,
                        enumerable = false, configurable = true))
        public static Object toStringTag(ExecutionContext cx, Object thisValue) {
            if (!(thisValue instanceof TypedArrayObject)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            return ((TypedArrayObject) thisValue).getTypedArrayName();
        }
    }
}
