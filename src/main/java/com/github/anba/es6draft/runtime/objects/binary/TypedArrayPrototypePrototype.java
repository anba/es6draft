/**
 * Copyright (c) André Bargull
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
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructor.TypedArraySpeciesCreate;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

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
import com.github.anba.es6draft.runtime.internal.StrBuilder;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorObject.ArrayIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
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
    }

    /**
     * Marker class for {@code %TypedArray%.prototype.values}.
     */
    private static final class TypedArrayPrototypeValues {
    }

    /**
     * Returns {@code true} if <var>values</var> is the built-in {@code %TypedArray%.prototype.values} function for the
     * requested realm.
     * 
     * @param realm
     *            the function realm
     * @param values
     *            the values function
     * @return {@code true} if <var>values</var> is the built-in {@code %TypedArray%.prototype.values} function
     */
    public static boolean isBuiltinValues(Realm realm, Object values) {
        return NativeFunction.isNative(realm, values, TypedArrayPrototypeValues.class);
    }

    /**
     * 22.2.3.5.1 Runtime Semantics: ValidateTypedArray ( O )
     * 
     * @param cx
     *            the execution context
     * @param thisValue
     *            the function this-value
     * @param method
     *            the method name
     * @return the validated typed array object
     */
    public static TypedArrayObject ValidateTypedArray(ExecutionContext cx, Object thisValue, String method) {
        /* steps 1-3 */
        if (!(thisValue instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(thisValue).toString());
        }
        TypedArrayObject typedArray = (TypedArrayObject) thisValue;
        /* step 4 */
        ArrayBuffer buffer = typedArray.getBuffer();
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

        private static TypedArrayObject thisTypedArrayObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof TypedArrayObject) {
                return (TypedArrayObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        private static long ToArrayIndex(ExecutionContext cx, Object index, long length) {
            long relativeIndex = (long) ToNumber(cx, index); // ToInteger
            if (relativeIndex < 0) {
                return Math.max(length + relativeIndex, 0);
            }
            return Math.min(relativeIndex, length);
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
            TypedArrayObject typedArray = thisTypedArrayObject(cx, thisValue, "%TypedArray%.prototype.buffer");
            /* steps 4-6 */
            return typedArray.getBuffer();
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
            TypedArrayObject typedArray = thisTypedArrayObject(cx, thisValue, "%TypedArray%.prototype.byteLength");
            /* steps 4-6 */
            if (IsDetachedBuffer(typedArray.getBuffer())) {
                return 0;
            }
            /* steps 7-8 */
            return typedArray.getByteLength();
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
            TypedArrayObject typedArray = thisTypedArrayObject(cx, thisValue, "%TypedArray%.prototype.byteOffset");
            /* steps 4-6 */
            if (IsDetachedBuffer(typedArray.getBuffer())) {
                return 0;
            }
            /* steps 7-8 */
            return typedArray.getByteOffset();
        }

        /**
         * 22.2.3.18 get %TypedArray%.prototype.length
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
            TypedArrayObject typedArray = thisTypedArrayObject(cx, thisValue, "%TypedArray%.prototype.length");
            /* steps 5-6 */
            if (IsDetachedBuffer(typedArray.getBuffer())) {
                return 0;
            }
            /* steps 7-8 */
            return typedArray.getArrayLength();
        }

        /**
         * 22.2.3.23 %TypedArray%.prototype.set ( overloaded [ , offset ])
         * <ul>
         * <li>22.2.3.23.1 %TypedArray%.prototype.set (array [ , offset ] )
         * <li>22.2.3.23.2 %TypedArray%.prototype.set(typedArray [, offset ] )
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
        @Function(name = "set", arity = 1)
        public static Object set(ExecutionContext cx, Object thisValue, Object array, Object offset) {
            if (!(array instanceof TypedArrayObject)) {
                // 22.2.3.23.1
                /* steps 1-5 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue, "%TypedArray%.prototype.set");
                /* step 6 */
                double targetOffset = ToInteger(cx, offset);
                /* step 7 */
                if (targetOffset < 0) {
                    throw newRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* step 8 */
                ArrayBuffer targetBuffer = target.getBuffer();
                /* step 9 */
                if (IsDetachedBuffer(targetBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 10 */
                long targetLength = target.getArrayLength();
                /* steps 11, 13 */
                ElementType targetType = target.getElementType();
                /* step 12 */
                int targetElementSize = targetType.size();
                /* step 14 */
                long targetByteOffset = target.getByteOffset();
                /* step 15 */
                ScriptObject src = ToObject(cx, array);
                /* step 16 */
                long srcLength = ToLength(cx, Get(cx, src, "length"));
                /* step 17 */
                if (srcLength + targetOffset > targetLength) {
                    throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
                }
                /* step 18 */
                long targetByteIndex = ((long) targetOffset) * targetElementSize + targetByteOffset;
                /* step 20 */
                long limit = targetByteIndex + targetElementSize * srcLength;
                /* steps 19, 21 */
                for (long k = 0; targetByteIndex < limit; ++k, targetByteIndex += targetElementSize) {
                    /* step 21.a */
                    long pk = k;
                    /* step 21.b */
                    Number kNumber = target.getElementType().toElementValue(cx, Get(cx, src, pk));
                    /* step 21.c */
                    if (IsDetachedBuffer(targetBuffer)) {
                        throw newTypeError(cx, Messages.Key.BufferDetached);
                    }
                    /* step 21.d */
                    SetValueInBuffer(targetBuffer, targetByteIndex, targetType, kNumber);
                }
                /* step 22 */
                return UNDEFINED;
            } else {
                // 22.2.3.23.2
                TypedArrayObject typedArray = (TypedArrayObject) array;
                /* steps 1-5 */
                TypedArrayObject target = thisTypedArrayObject(cx, thisValue, "%TypedArray%.prototype.set");
                /* step 6 */
                double targetOffset = ToInteger(cx, offset);
                /* step 7 */
                if (targetOffset < 0) {
                    throw newRangeError(cx, Messages.Key.InvalidByteOffset);
                }
                /* step 8 */
                ArrayBuffer targetBuffer = target.getBuffer();
                /* step 9 */
                if (IsDetachedBuffer(targetBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 10 */
                long targetLength = target.getArrayLength();
                /* step 11 */
                ArrayBuffer srcBuffer = typedArray.getBuffer();
                /* step 12 */
                if (IsDetachedBuffer(srcBuffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* step 20 */
                long srcLength = typedArray.getArrayLength();
                /* step 22 */
                if (targetOffset + srcLength > targetLength) {
                    throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
                }

                // Extension: BigInt
                if (!typedArray.getElementType().isCompatibleNumericType(target.getElementType())) {
                    throw newTypeError(cx, Messages.Key.IncompatibleElementTypes);
                }

                /* steps 13-19, 21, 23-28 */
                typedArray.functions().set(cx, typedArray, target, (long) targetOffset);
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
        public static Object subarray(ExecutionContext cx, Object thisValue, Object begin, Object end) {
            /* steps 1-4 */
            TypedArrayObject array = thisTypedArrayObject(cx, thisValue, "%TypedArray%.prototype.subarray");
            /* step 5 */
            ArrayBuffer buffer = array.getBuffer();
            /* step 6 */
            long srcLength = array.getArrayLength();
            /* steps 7-8 */
            long beginIndex = ToArrayIndex(cx, begin, srcLength);
            /* steps 9-10 */
            long endIndex = Type.isUndefined(end) ? srcLength : ToArrayIndex(cx, end, srcLength);
            /* step 11 */
            long newLength = Math.max(endIndex - beginIndex, 0);
            /* step 14 */
            long srcByteOffset = array.getByteOffset();
            /* steps 12-13, 15 */
            long beginByteOffset = srcByteOffset + array.getElementType().toBytes(beginIndex);
            /* steps 16-17 */
            return TypedArraySpeciesCreate(cx, "%TypedArray%.prototype.subarray", array, buffer, beginByteOffset,
                    newLength);
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
         * @param thisValue
         *            the function this-value
         * @param locales
         *            the optional locales array
         * @param options
         *            the optional options object
         * @return the locale specific string representation
         */
        @Function(name = "toLocaleString", arity = 0)
        public static Object toLocaleString(ExecutionContext cx, Object thisValue, Object locales, Object options) {
            // 22.1.3.27 Array.prototype.toLocaleString ( [ reserved1 [ , reserved2 ] ] )<br>
            // 13.4.1 Array.prototype.toLocaleString([locales [, options ]])
            /* step 1 */
            TypedArrayObject array = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.toLocaleString");
            /* step 2 */
            long len = array.getArrayLength();
            /* step 3 */
            String separator = cx.getRealm().getListSeparator();
            /* step 4 */
            if (len == 0) {
                return "";
            }
            /* step 5 */
            Number firstElement = array.elementGetMaybeDetached(cx, 0);
            /* steps 6-7 */
            StrBuilder r = new StrBuilder(cx);
            r.append(ToString(cx, Invoke(cx, firstElement, "toLocaleString", locales, options)));
            /* steps 8-9 */
            for (long k = 1; k < len; ++k) {
                Number nextElement = array.elementGetMaybeDetached(cx, k);
                r.append(separator).append(ToString(cx, Invoke(cx, nextElement, "toLocaleString", locales, options)));
            }
            /* step 10 */
            return r.toString();
        }

        /**
         * 22.2.3.15 %TypedArray%.prototype.join ( separator )
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
            // 22.1.3.13 Array.prototype.join (separator)
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.join");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (Type.isUndefined(separator)) {
                separator = ",";
            }
            /* step 4 */
            String sep = ToFlatString(cx, separator);
            /* step 5 */
            if (len == 0) {
                return "";
            }
            if (IsDetachedBuffer(o.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* steps 6-10 */
            return o.functions().join(o, sep, new StrBuilder(cx));
        }

        /**
         * 22.2.3.22 %TypedArray%.prototype.reverse ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return this typed array object
         */
        @Function(name = "reverse", arity = 0)
        public static Object reverse(ExecutionContext cx, Object thisValue) {
            // 22.1.3.21 Array.prototype.reverse ( )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.reverse");
            /* steps 2-5 */
            o.functions().reverse(o);
            /* step 6 */
            return o;
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
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.slice");
            /* step 3 */
            long len = o.getArrayLength();
            /* steps 4-5 */
            long k = ToArrayIndex(cx, start, len);
            /* steps 6-7 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 8 */
            long count = Math.max(finall - k, 0);
            /* step 9 */
            TypedArrayObject a = TypedArraySpeciesCreate(cx, "%TypedArray%.prototype.slice", o, count);
            /* steps 10-15 */
            if (count > 0) {
                if (IsDetachedBuffer(o.getBuffer())) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                o.functions().slice(o, a, k, finall);
            }
            /* step 16 */
            return a;
        }

        private static final class FunctionComparator implements Comparator<Number> {
            private final ExecutionContext cx;
            private final Callable comparefn;
            private final ArrayBuffer buffer;

            FunctionComparator(ExecutionContext cx, Callable comparefn, ArrayBuffer buffer) {
                this.cx = cx;
                this.comparefn = comparefn;
                this.buffer = buffer;
            }

            @Override
            public int compare(Number x, Number y) {
                double c = ToNumber(cx, comparefn.call(cx, UNDEFINED, x, y));
                if (IsDetachedBuffer(buffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                return (c < 0 ? -1 : c > 0 ? 1 : 0);
            }
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
            /* step 1 */
            Callable compareFunction = null;
            if (!Type.isUndefined(comparefn)) {
                if (!IsCallable(comparefn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                compareFunction = (Callable) comparefn;
            }
            /* steps 2-3 */
            TypedArrayObject obj = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.sort");
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

            if (compareFunction != null) {
                Comparator<Number> comparator = new FunctionComparator(cx, compareFunction, obj.getBuffer());
                obj.functions().sort(obj, comparator);
            } else {
                obj.functions().sort(obj);
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
        public static Object indexOf(ExecutionContext cx, Object thisValue, Object searchElement, Object fromIndex) {
            // 22.1.3.12 Array.prototype.indexOf ( searchElement [ , fromIndex ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.indexOf");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (len == 0) {
                return -1;
            }
            /* step 4 */
            long n = (long) ToNumber(cx, fromIndex); // ToInteger
            /* step 5 */
            if (n >= len) {
                return -1;
            }
            /* steps 6-7 */
            long k;
            if (n >= 0) {
                k = n;
            } else {
                /* step 7.a */
                k = len + n;
                /* step 7.b */
                if (k < 0) {
                    k = 0;
                }
            }
            assert 0 <= k && k < len;
            /* step 8 */
            if (IsDetachedBuffer(o.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            if (Type.isNumber(searchElement) && !Double.isNaN(Type.numberValue(searchElement))) {
                return o.functions().indexOf(o, Type.numberValue(searchElement), k);
            }
            if (Type.isBigInt(searchElement)) {
                return o.functions().indexOf(o, Type.bigIntValue(searchElement), k);
            }
            /* step 9 */
            return -1;
        }

        /**
         * 22.2.3.17 %TypedArray%.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
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
        public static Object lastIndexOf(ExecutionContext cx, Object thisValue, Object searchElement,
                @Optional(Optional.Default.NONE) Object fromIndex) {
            // 22.1.3.15 Array.prototype.lastIndexOf ( searchElement [ , fromIndex ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.lastIndexOf");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (len == 0) {
                return -1;
            }
            /* step 4 */
            long n;
            if (fromIndex != null) {
                n = (long) ToNumber(cx, fromIndex); // ToInteger
            } else {
                n = len - 1;
            }
            /* steps 5-6 */
            long k;
            if (n >= 0) {
                k = Math.min(n, len - 1);
            } else {
                k = len + n;
                if (k < 0) {
                    return -1;
                }
            }
            assert 0 <= k && k < len;
            /* step 7 */
            if (IsDetachedBuffer(o.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            if (Type.isNumber(searchElement) && !Double.isNaN(Type.numberValue(searchElement))) {
                return o.functions().lastIndexOf(o, Type.numberValue(searchElement), k);
            }
            if (Type.isBigInt(searchElement)) {
                return o.functions().lastIndexOf(o, Type.bigIntValue(searchElement), k);
            }
            /* step 8 */
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
        public static Object every(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            // 22.1.3.5 Array.prototype.every ( callbackfn [ , thisArg ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.every");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                boolean testResult = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                if (!testResult) {
                    return false;
                }
            }
            /* step 7 */
            return true;
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
        public static Object some(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            // 22.1.3.24 Array.prototype.some ( callbackfn [ , thisArg ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.some");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                boolean testResult = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                if (testResult) {
                    return true;
                }
            }
            /* step 7 */
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
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            // 22.1.3.10 Array.prototype.forEach ( callbackfn [ , thisArg ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.forEach");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                callback.call(cx, thisArg, kvalue, k, o);
            }
            /* step 7 */
            return UNDEFINED;
        }

        /**
         * 22.2.3.19 %TypedArray%.prototype.map ( callbackfn [ , thisArg ] )
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
        public static Object map(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.map");
            /* step 3 */
            long len = o.getArrayLength();
            /* step 4 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 5 (omitted) */
            /* step 6 */
            TypedArrayObject a = TypedArraySpeciesCreate(cx, "%TypedArray%.prototype.map", o, len);
            /* steps 7-8 */
            for (long k = 0; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                Object mappedValue = callback.call(cx, thisArg, kvalue, k, o);
                a.elementSetMaybeDetached(cx, k, mappedValue);
            }
            /* step 9 */
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
        public static Object filter(ExecutionContext cx, Object thisValue, Object callbackfn, Object thisArg) {
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.filter");
            /* step 3 */
            long len = o.getArrayLength();
            /* step 4 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 5 (omitted) */
            /* steps 6, 8 */
            int captured = 0;
            Number[] kept = new Number[(int) Math.min(len, 1024)];
            /* steps 7, 9 */
            for (long k = 0; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                boolean selected = ToBoolean(callback.call(cx, thisArg, kvalue, k, o));
                if (selected) {
                    if (captured == kept.length) {
                        kept = Arrays.copyOf(kept, captured + (captured >> 1));
                    }
                    kept[captured++] = kvalue;
                }
            }
            /* step 10 */
            TypedArrayObject a = TypedArraySpeciesCreate(cx, "%TypedArray%.prototype.filter", o, captured);
            /* steps 11-12 */
            for (int n = 0; n < captured; ++n) {
                Number e = kept[n];
                a.elementSetUnchecked(n, e);
            }
            /* step 13 */
            return a;
        }

        /**
         * 22.2.3.20 %TypedArray%.prototype.reduce ( callbackfn [, initialValue] )
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
            // 22.1.3.19 Array.prototype.reduce ( callbackfn [ , initialValue ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.reduce");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 5 */
            long k = 0;
            /* steps 6-7 */
            Object accumulator;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                accumulator = o.elementGetUnchecked(k++);
            }
            /* step 8 */
            for (; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
            }
            /* step 9 */
            return accumulator;
        }

        /**
         * 22.2.3.21 %TypedArray%.prototype.reduceRight ( callbackfn [, initialValue] )
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
            // 22.1.3.20 Array.prototype.reduceRight ( callbackfn [ , initialValue ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.reduceRight");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 4 */
            if (len == 0 && initialValue == null) {
                throw newTypeError(cx, Messages.Key.ReduceInitialValue);
            }
            /* step 5 */
            long k = len - 1;
            /* steps 6-7 */
            Object accumulator;
            if (initialValue != null) {
                accumulator = initialValue;
            } else {
                accumulator = o.elementGetUnchecked(k--);
            }
            /* step 8 */
            for (; k >= 0; --k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                accumulator = callback.call(cx, UNDEFINED, accumulator, kvalue, k, o);
            }
            /* step 9 */
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
        public static Object find(ExecutionContext cx, Object thisValue, Object predicate, Object thisArg) {
            // 22.1.3.8 Array.prototype.find ( predicate [ , thisArg ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.find");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                boolean testResult = ToBoolean(pred.call(cx, thisArg, kvalue, k, o));
                if (testResult) {
                    return kvalue;
                }
            }
            /* step 7 */
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
        public static Object findIndex(ExecutionContext cx, Object thisValue, Object predicate, Object thisArg) {
            // 22.1.3.9 Array.prototype.findIndex ( predicate [ , thisArg ] )
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.findIndex");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (!IsCallable(predicate)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable pred = (Callable) predicate;
            /* step 4 (omitted) */
            /* steps 5-6 */
            for (long k = 0; k < len; ++k) {
                Number kvalue = o.elementGetMaybeDetached(cx, k);
                boolean testResult = ToBoolean(pred.call(cx, thisArg, kvalue, k, o));
                if (testResult) {
                    return k;
                }
            }
            /* step 7 */
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
        public static Object fill(ExecutionContext cx, Object thisValue, Object value, Object start, Object end) {
            // 22.1.3.6 Array.prototype.fill (value [ , start [ , end ] ] )
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.fill");
            /* step 3 */
            long len = o.getArrayLength();
            /* step 4 */
            Number fillValue = o.getElementType().toElementValue(cx, value);
            /* steps 5-6 */
            long k = ToArrayIndex(cx, start, len);
            /* steps 7-8 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 9 */
            if (IsDetachedBuffer(o.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* step 10 */
            if (k < finall) {
                o.functions().fill(o, fillValue, k, finall);
            }
            /* step 11 */
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
        public static Object copyWithin(ExecutionContext cx, Object thisValue, Object target, Object start,
                Object end) {
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.copyWithin");
            /* step 3 */
            long len = o.getArrayLength();
            /* steps 4-5 */
            long to = ToArrayIndex(cx, target, len);
            /* steps 6-7 */
            long from = ToArrayIndex(cx, start, len);
            /* steps 8-9 */
            long finall = Type.isUndefined(end) ? len : ToArrayIndex(cx, end, len);
            /* step 10 */
            long count = Math.min(finall - from, len - to);
            /* step 11 */
            if (count > 0) {
                /* steps 11.a-b */
                if (IsDetachedBuffer(o.getBuffer())) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                /* steps 11.c-l */
                o.functions().copyWithin(o, to, from, count);
            }
            /* step 12 */
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
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.entries");
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.KeyValue);
        }

        /**
         * 22.2.3.16 %TypedArray%.prototype.keys ( )
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
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.keys");
            /* step 4 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Key);
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
        @Function(name = "values", arity = 0, nativeId = TypedArrayPrototypeValues.class)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.values");
            /* step 3 */
            return CreateArrayIterator(cx, o, ArrayIterationKind.Value);
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
        @Accessor(name = "get [Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag, type = Accessor.Type.Getter,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object toStringTag(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            if (!(thisValue instanceof TypedArrayObject)) {
                return UNDEFINED;
            }
            TypedArrayObject array = (TypedArrayObject) thisValue;
            /* steps 4-6 */
            return array.getTypedArrayName();
        }

        /**
         * 22.2.3.14 %TypedArray%.prototype.includes ( searchElement [ , fromIndex ] )
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
        public static Object includes(ExecutionContext cx, Object thisValue, Object searchElement, Object fromIndex) {
            // 22.1.3.11 Array.prototype.includes
            /* step 1 */
            TypedArrayObject o = ValidateTypedArray(cx, thisValue, "%TypedArray%.prototype.includes");
            /* step 2 */
            long len = o.getArrayLength();
            /* step 3 */
            if (len == 0) {
                return false;
            }
            /* step 4 */
            long n = (long) ToNumber(cx, fromIndex); // ToInteger
            /* steps 5-6 */
            long k;
            if (n >= 0) {
                k = n;
                if (k >= len) {
                    return false;
                }
            } else {
                /* step 6.a */
                k = len + n;
                /* step 6.b */
                if (k < 0) {
                    k = 0;
                }
            }
            assert 0 <= k && k < len;
            /* step 7 */
            if (IsDetachedBuffer(o.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            if (Type.isNumber(searchElement)) {
                return o.functions().includes(o, Type.numberValue(searchElement), k);
            }
            if (Type.isBigInt(searchElement)) {
                return o.functions().includes(o, Type.bigIntValue(searchElement), k);
            }
            /* step 8 */
            return false;
        }
    }
}
