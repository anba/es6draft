/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.1 The %TypedArray% Intrinsic Object
 * <li>22.2.2 Properties of the %TypedArray% Intrinsic Object
 * </ul>
 */
public final class TypedArrayConstructorPrototype extends BuiltinFunction implements Initializable {
    public TypedArrayConstructorPrototype(Realm realm) {
        super(realm, "TypedArray");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public TypedArrayConstructorPrototype clone() {
        return new TypedArrayConstructorPrototype(getRealm());
    }

    /**
     * 22.2.1.1 %TypedArray% ( length )<br>
     * 22.2.1.2 %TypedArray% ( typedArray )<br>
     * 22.2.1.3 %TypedArray% ( array )<br>
     * 22.2.1.4 %TypedArray% ( buffer [ , byteOffset [ , length ] ] )<br>
     * 22.2.1.5 %TypedArray% ( all other argument combinations )<br>
     */
    @Override
    public TypedArrayObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object arg0 = args.length > 0 ? args[0] : UNDEFINED;
        if (!Type.isObject(arg0)) {
            return callWithLength(calleeContext, thisValue, arg0);
        }
        if (arg0 instanceof TypedArrayObject) {
            return callWithTypedArray(calleeContext, thisValue, (TypedArrayObject) arg0);
        }
        if (arg0 instanceof ArrayBufferObject) {
            Object byteOffset = args.length > 1 ? args[1] : 0;
            Object length = args.length > 2 ? args[2] : UNDEFINED;
            return callWithArrayBuffer(calleeContext, thisValue, (ArrayBufferObject) arg0,
                    byteOffset, length);
        }
        return callWithObject(calleeContext, thisValue, (ScriptObject) arg0);
    }

    /**
     * 22.2.1.1 %TypedArray% ( length )
     * 
     * @param cx
     *            the execution context
     * @param thisValue
     *            the this-value
     * @param length
     *            the typed array length
     * @return the typed array object
     */
    private TypedArrayObject callWithLength(ExecutionContext cx, Object thisValue, Object length) {
        /* step 1 */
        assert !Type.isObject(length);
        /* step 2 */
        Object obj = thisValue;
        /* steps 3-4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getElementType() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 8-9 */
        ElementType elementType = array.getElementType();
        // FIXME: spec issue? - undefined length is same as 0 for bwcompat?
        if (Type.isUndefined(length)) {
            length = 0;
        }
        /* step 10 */
        double numberLength = ToNumber(cx, length);
        /* steps 11-12 */
        long elementLength = ToLength(numberLength);
        /* step 13 */
        if (!SameValueZero(numberLength, elementLength)) {
            throw newRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* steps 14-16 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, Intrinsics.ArrayBuffer);
        /* step 16 */
        int elementSize = elementType.size();
        /* step 17 */
        long byteLength = elementSize * elementLength;
        /* steps 18-19 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 20-23 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 24 */
        return array;
    }

    /**
     * 22.2.1.2 %TypedArray% ( typedArray )
     * 
     * @param cx
     *            the execution context
     * @param thisValue
     *            the this-value
     * @param typedArray
     *            the source typed array object
     * @return the typed array object
     */
    private TypedArrayObject callWithTypedArray(ExecutionContext cx, Object thisValue,
            TypedArrayObject typedArray) {
        /* step 1 (implicit) */
        /* step 2 */
        TypedArrayObject srcArray = typedArray;
        /* step 3 */
        Object obj = thisValue;
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getElementType() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* step 8 */
        if (srcArray.getBuffer() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* steps 9-10 */
        ElementType elementType = array.getElementType();
        /* step 11 */
        long elementLength = srcArray.getArrayLength();
        /* steps 12-13 */
        ElementType srcType = srcArray.getElementType();
        /* step 14 */
        int srcElementSize = srcType.size();
        /* step 15 */
        ArrayBufferObject srcData = srcArray.getBuffer();
        /* step 16 */
        long srcByteOffset = srcArray.getByteOffset();
        /* step 17 */
        int elementSize = elementType.size();
        /* step 18 */
        long byteLength = elementSize * elementLength;
        /* steps 19-20 */
        ArrayBufferObject data;
        if (elementType == srcType) {
            /* step 19 */
            data = CloneArrayBuffer(cx, srcData, srcByteOffset);
        } else {
            /* step 20 */
            Object bufferConstructor = Get(cx, srcData, "constructor");
            if (Type.isUndefined(bufferConstructor)) {
                bufferConstructor = cx.getIntrinsic(Intrinsics.ArrayBuffer);
            }
            data = AllocateArrayBuffer(cx, bufferConstructor);
            SetArrayBufferData(cx, data, byteLength);
            long srcByteIndex = srcByteOffset;
            long targetByteIndex = 0;
            for (long count = elementLength; count > 0; --count) {
                double value = GetValueFromBuffer(cx, srcData, srcByteIndex, srcType);
                SetValueInBuffer(cx, data, targetByteIndex, elementType, value);
                srcByteIndex += srcElementSize;
                targetByteIndex += elementSize;
            }
        }
        /* steps 21-22 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 23-26 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 27 */
        return array;
    }

    /**
     * 22.2.1.3 %TypedArray% ( object )
     * 
     * @param cx
     *            the execution context
     * @param thisValue
     *            the this-value
     * @param object
     *            the source object
     * @return the typed array object
     */
    private TypedArrayObject callWithObject(ExecutionContext cx, Object thisValue,
            ScriptObject object) {
        /* step 1 */
        assert !(object instanceof TypedArrayObject || object instanceof ArrayBufferObject);
        /* step 2 */
        Object obj = thisValue;
        /* step 3 */
        // Constructor constructor = null;
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getElementType() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 8-10 */
        return (TypedArrayObject) TypedArrayFrom(cx, null, array, object, null, null);
    }

    /**
     * 22.2.1.4 %TypedArray% ( buffer [ , byteOffset [ , length ] ] )
     * 
     * @param cx
     *            the execution context
     * @param thisValue
     *            the this-value
     * @param buffer
     *            the source array buffer object
     * @param byteOffset
     *            the source byte offset
     * @param length
     *            the array length
     * @return the typed array object
     */
    private TypedArrayObject callWithArrayBuffer(ExecutionContext cx, Object thisValue,
            ArrayBufferObject buffer, Object byteOffset, Object length) {
        /* step 1 (implicit) */
        /* step 2 */
        Object obj = thisValue;
        /* step 3 */
        if (buffer.getData() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getElementType() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 8-9 */
        ElementType elementType = array.getElementType();
        /* step 10 */
        int elementSize = elementType.size();
        /* steps 11-12 */
        double offset = ToInteger(cx, byteOffset);
        /* step 13 */
        if (offset < 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 14 */
        if (offset % elementSize != 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 15 */
        long bufferByteLength = buffer.getByteLength();
        /* steps 16-17 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 16 */
            if (bufferByteLength % elementSize != 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = (long) (bufferByteLength - offset);
            if (newByteLength < 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        } else {
            /* step 17 */
            long newLength = ToLength(cx, length);
            newByteLength = newLength * elementSize;
            if (offset + newByteLength > bufferByteLength) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        }
        /* step 18 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 19-22 */
        array.setBuffer(buffer);
        array.setByteLength(newByteLength);
        array.setByteOffset((long) offset);
        array.setArrayLength(newByteLength / elementSize);
        /* step 23 */
        return array;
    }

    /**
     * 22.2.2 Properties of the %TypedArray% Intrinsic Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "TypedArray";

        /**
         * 22.2.2.3 %TypedArray%.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.2.2 %TypedArray%.of ( ...items )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param items
         *            the element values
         * @return the new typed array object
         */
        @Function(name = "of", arity = 0)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            int len = items.length;
            /* step 3 */
            Object c = thisValue;
            /* step 5 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* step 4 */
            ScriptObject newObj = ((Constructor) c).construct(cx, len);
            /* steps 6-7 */
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object value = items[k];
                Put(cx, newObj, pk, value, true);
            }
            /* step 8 */
            return newObj;
        }

        /**
         * 22.2.2.1 %TypedArray%.from ( source [ , mapfn [ , thisArg ] ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param source
         *            the source object
         * @param mapfn
         *            the optional mapper function
         * @param thisArg
         *            the optional this-argument for the mapper
         * @return the new typed array object
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object source,
                Object mapfn, Object thisArg) {
            /* step 1 */
            Object c = thisValue;
            /* step 2 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 3-4 */
            ScriptObject items = ToObject(cx, source);
            /* steps 5-7 */
            Callable mapper = null;
            if (!Type.isUndefined(mapfn)) {
                if (!IsCallable(mapfn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                mapper = (Callable) mapfn;
            }
            return TypedArrayFrom(cx, (Constructor) c, null, items, mapper, thisArg);
        }

        /**
         * 22.2.3.4 %TypedArray%[ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new uninitialized typed array object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            Object f = thisValue;
            /* step 2 */
            if (!Type.isObject(f)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 3-4 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, f, Intrinsics.TypedArrayPrototype);
            /* steps 5 */
            TypedArrayObject obj = new TypedArrayObject(cx.getRealm());
            obj.setPrototype(proto);
            /* steps 6-7 (implicit) */
            // obj.setBuffer(null);
            // obj.setElementType(null);
            /* steps 8-10 */
            obj.setByteLength(0);
            obj.setByteOffset(0);
            obj.setArrayLength(0);
            /* step 11 */
            return obj;
        }
    }

    /**
     * TODO: remove method
     * 
     * @param cx
     *            the execution context
     * @param target
     *            the typed array object or {@code null}
     * @param items
     *            the source object
     * @return the new typed array object
     */
    @SuppressWarnings("unused")
    private static TypedArrayObject TypedArrayFrom(ExecutionContext cx, TypedArrayObject array,
            ScriptObject items) {
        /* step 3 */
        ScriptObject srcArray = items;
        /* steps 8-9 */
        ElementType elementType = array.getElementType();
        /* step 10 */
        Object arrayLength = Get(cx, srcArray, "length");
        /* steps 11-12 */
        long elementLength = ToLength(cx, arrayLength);
        /* steps 13-14 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, Intrinsics.ArrayBuffer);
        /* step 15 */
        int elementSize = elementType.size();
        /* step 16 */
        long byteLength = elementSize * elementLength;
        /* steps 17-18 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 19-20 */
        for (long k = 0; k < elementLength; ++k) {
            String pk = ToString(k);
            Object kValue = Get(cx, srcArray, pk);
            double kNumber = ToNumber(cx, kValue);
            SetValueInBuffer(cx, data, k * elementSize, elementType, kNumber);
        }
        /* steps 21-22 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 23-26 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 27 */
        return array;
    }

    /**
     * 22.2.2.1.1 Runtime Semantics: TypedArrayFrom( constructor, target, items, mapfn, thisArg )
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function or {@code null}
     * @param target
     *            the typed array object or {@code null}
     * @param items
     *            the source object
     * @param mapfn
     *            the mapping function or {@code null}
     * @param thisArg
     *            the this-argument for the mapping function or {@code null}
     * @return the new typed array object
     */
    public static ScriptObject TypedArrayFrom(ExecutionContext cx, Constructor constructor,
            TypedArrayObject target, ScriptObject items, Callable mapfn, Object thisArg) {
        /* step 1 (omitted) */
        /* step 2 (not applicable) */
        // FIXME: spec bug - %TypedArray% is not a constructor
        assert constructor != null ^ target != null;
        /* step 3 */
        assert target == null || (target.getElementType() != null && target.getBuffer() == null);
        /* step 4 (not applicable) */
        /* step 5 (not applicable) */
        /* steps 6-7 (Type check moved to caller) */
        boolean mapping = mapfn != null;
        /* steps 8-9 */
        Object usingIterator = CheckIterable(cx, items);
        /* step 10 */
        if (!Type.isUndefined(usingIterator)) {
            /* steps 10.a-10.b */
            ScriptObject iterator = GetIterator(cx, items, usingIterator);
            /* steps 10.c-10.f */
            List<Object> values = new ArrayList<>();
            for (;;) {
                ScriptObject next = IteratorStep(cx, iterator);
                if (next == null) {
                    break;
                }
                Object nextValue = IteratorValue(cx, next);
                values.add(nextValue);
            }
            /* step 10.g */
            int len = values.size();
            /* steps 10.h-10.i */
            ScriptObject targetObj;
            if (target == null) {
                /* step 10.h */
                targetObj = constructor.construct(cx, len);
            } else {
                /* step 10.i */
                // FIXME: spec bug - assert is invalid, side-effects may have init'ed target
                // assert target.getElementType() != null && target.getBuffer() == null;
                targetObj = initializeTypedArray(cx, target, len);
            }
            /* steps 10.j-10.l */
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object kValue = values.get(k);
                Object mappedValue;
                if (mapping) {
                    mappedValue = mapfn.call(cx, thisArg, kValue, k);
                } else {
                    mappedValue = kValue;
                }
                Put(cx, targetObj, pk, mappedValue, true);
            }
            /* step 10.m */
            return targetObj;
        }
        /* step 11 (?) */
        /* step 12 */
        Object lenValue = Get(cx, items, "length");
        /* steps 13-14 */
        long len = ToLength(cx, lenValue);
        /* steps 15-16 */
        // FIXME: spec bug - target is not initialized for this path
        // ScriptObject newObj = constructor.construct(cx, len);
        ScriptObject newObj;
        if (target == null) {
            newObj = constructor.construct(cx, len);
        } else {
            newObj = initializeTypedArray(cx, target, len);
        }
        /* steps 17-18 */
        for (long k = 0; k < len; ++k) {
            String pk = ToString(k);
            Object kValue = Get(cx, items, pk);
            Object mappedValue;
            if (mapping) {
                mappedValue = mapfn.call(cx, thisArg, kValue, k);
            } else {
                mappedValue = kValue;
            }
            Put(cx, newObj, pk, mappedValue, true);
        }
        /* step 19 */
        return newObj;
    }

    /**
     * Initializes the typed array object. (Not in spec)
     * 
     * @param cx
     *            the execution context
     * @param array
     *            the typed array object
     * @param length
     *            the typed array object length
     * @return the typed array object
     */
    private static TypedArrayObject initializeTypedArray(ExecutionContext cx,
            TypedArrayObject array, long length) {
        /* step 1 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 2-3 */
        ElementType elementType = array.getElementType();
        /* step 4 */
        long elementLength = length;
        /* steps 5-6 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, Intrinsics.ArrayBuffer);
        /* step 7 */
        int elementSize = elementType.size();
        /* step 8 */
        long byteLength = elementSize * elementLength;
        /* steps 9-10 */
        SetArrayBufferData(cx, data, byteLength);
        /* step 11 */
        assert array.getBuffer() == null;
        /* steps 12-15 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 16 */
        return array;
    }
}
