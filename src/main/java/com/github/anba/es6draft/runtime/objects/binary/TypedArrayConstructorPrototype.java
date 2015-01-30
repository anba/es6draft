/**
 * Copyright (c) 2012-2015 André Bargull
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

import java.util.ArrayList;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
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
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.1 The %TypedArray% Intrinsic Object
 * <li>22.2.2 Properties of the %TypedArray% Intrinsic Object
 * </ul>
 */
public final class TypedArrayConstructorPrototype extends BuiltinConstructor implements
        Initializable {
    /**
     * Constructs a new TypedArray constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public TypedArrayConstructorPrototype(Realm realm) {
        super(realm, "TypedArray", 3);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
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
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        // FIXME: spec issue - directly throw TypeError if NewTarget == null. (bug 3654)
        ExecutionContext calleeContext = calleeContext();
        Object arg0 = argument(args, 0);
        if (!Type.isObject(arg0)) {
            return callWithLength(calleeContext, null, arg0);
        }
        if (arg0 instanceof TypedArrayObject) {
            return callWithTypedArray(calleeContext, null, (TypedArrayObject) arg0);
        }
        if (arg0 instanceof ArrayBufferObject) {
            Object byteOffset = argument(args, 1, 0);
            Object length = argument(args, 2);
            return callWithArrayBuffer(calleeContext, null, (ArrayBufferObject) arg0, byteOffset,
                    length);
        }
        return callWithObject(calleeContext, null, (ScriptObject) arg0);
    }

    /**
     * 22.2.1.1 %TypedArray% ( length )<br>
     * 22.2.1.2 %TypedArray% ( typedArray )<br>
     * 22.2.1.3 %TypedArray% ( array )<br>
     * 22.2.1.4 %TypedArray% ( buffer [ , byteOffset [ , length ] ] )<br>
     * 22.2.1.5 %TypedArray% ( all other argument combinations )<br>
     */
    @Override
    public TypedArrayObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object arg0 = argument(args, 0);
        if (!Type.isObject(arg0)) {
            return callWithLength(calleeContext, newTarget, arg0);
        }
        if (arg0 instanceof TypedArrayObject) {
            return callWithTypedArray(calleeContext, newTarget, (TypedArrayObject) arg0);
        }
        if (arg0 instanceof ArrayBufferObject) {
            Object byteOffset = argument(args, 1, 0);
            Object length = argument(args, 2);
            return callWithArrayBuffer(calleeContext, newTarget, (ArrayBufferObject) arg0,
                    byteOffset, length);
        }
        return callWithObject(calleeContext, newTarget, (ScriptObject) arg0);
    }

    /**
     * 22.2.1.1 %TypedArray% ( length )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param length
     *            the typed array length
     * @return the typed array object
     */
    private TypedArrayObject callWithLength(ExecutionContext cx, Constructor newTarget,
            Object length) {
        /* step 1 */
        assert !Type.isObject(length);
        // FIXME: spec issue? - undefined length is same as 0 for bwcompat?
        if (Type.isUndefined(length)) {
            length = 0;
        }
        /* step 2 */
        double numberLength = ToNumber(cx, length);
        /* steps 3-4 */
        long elementLength = ToLength(numberLength);
        /* step 5 */
        if (!SameValueZero(numberLength, elementLength)) {
            throw newRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* steps 6-8 */
        // FIXME: spec bug
        /* step 9 */
        return AllocateTypedArray(cx, newTarget, elementLength);
    }

    /**
     * 22.2.1.2 %TypedArray% ( typedArray )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param typedArray
     *            the source typed array object
     * @return the typed array object
     */
    private TypedArrayObject callWithTypedArray(ExecutionContext cx, Constructor newTarget,
            TypedArrayObject typedArray) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        TypedArrayObject array = AllocateTypedArray(cx, newTarget, -1);
        /* step 4 */
        TypedArrayObject srcArray = typedArray;
        /* step 5 */
        ArrayBufferObject srcData = srcArray.getBuffer();
        /* step 6 */
        if (IsDetachedBuffer(srcData)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 7-8 */
        ElementType elementType = array.getElementType();
        /* step 9 */
        long elementLength = srcArray.getArrayLength();
        /* steps 10-11 */
        ElementType srcType = srcArray.getElementType();
        /* step 12 */
        int srcElementSize = srcType.size();
        /* step 13 */
        long srcByteOffset = srcArray.getByteOffset();
        /* step 14 */
        int elementSize = elementType.size();
        /* step 15 */
        long byteLength = elementSize * elementLength;
        /* steps 16-17 */
        ArrayBufferObject data;
        if (elementType == srcType) {
            /* step 16 */
            data = CloneArrayBuffer(cx, srcData, srcByteOffset);
        } else {
            /* step 17 */
            /* steps 17.a-17.b */
            Constructor bufferConstructor = SpeciesConstructor(cx, srcData, Intrinsics.ArrayBuffer);
            /* step 17.d */
            data = AllocateArrayBuffer(cx, bufferConstructor, byteLength);
            // FIXME: spec bug - 17.c-d need to be swapped (bug 3677)
            /* step 17.c */
            if (IsDetachedBuffer(srcData)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* step 17.e */
            long srcByteIndex = srcByteOffset;
            /* step 17.f */
            long targetByteIndex = 0;
            /* steps 17.g-17.h */
            for (long count = elementLength; count > 0; --count) {
                double value = GetValueFromBuffer(srcData, srcByteIndex, srcType);
                SetValueInBuffer(data, targetByteIndex, elementType, value);
                srcByteIndex += srcElementSize;
                targetByteIndex += elementSize;
            }
        }
        /* steps 18-21 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 22 */
        return array;
    }

    /**
     * 22.2.1.3 %TypedArray% ( object )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param object
     *            the source object
     * @return the typed array object
     */
    private TypedArrayObject callWithObject(ExecutionContext cx, Constructor newTarget,
            ScriptObject object) {
        /* step 1 */
        assert !(object instanceof TypedArrayObject || object instanceof ArrayBufferObject);
        /* step 2 */
        return TypedArrayFrom(cx, newTarget, object, null, null);
    }

    /**
     * 22.2.1.4 %TypedArray% ( buffer [ , byteOffset [ , length ] ] )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param buffer
     *            the source array buffer object
     * @param byteOffset
     *            the source byte offset
     * @param length
     *            the array length
     * @return the typed array object
     */
    private TypedArrayObject callWithArrayBuffer(ExecutionContext cx, Constructor newTarget,
            ArrayBufferObject buffer, Object byteOffset, Object length) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        TypedArrayObject array = AllocateTypedArray(cx, newTarget, -1);
        /* steps 4-5 */
        int elementSize = array.getElementType().size();
        /* steps 6-7 */
        // TODO: spec issue? - does not follow the ToNumber(v) == ToInteger(v) pattern
        double offset = ToInteger(cx, byteOffset);
        /* step 8 */
        if (offset < 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 9 */
        if (offset % elementSize != 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 10 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 11 */
        long bufferByteLength = buffer.getByteLength();
        /* steps 12-13 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 12 */
            if (bufferByteLength % elementSize != 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = (long) (bufferByteLength - offset);
            if (newByteLength < 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        } else {
            /* step 13 */
            long newLength = ToLength(cx, length);
            newByteLength = newLength * elementSize;
            if (offset + newByteLength > bufferByteLength) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        }
        /* steps 14-17 */
        array.setBuffer(buffer);
        array.setByteLength(newByteLength);
        array.setByteOffset((long) offset);
        array.setArrayLength(newByteLength / elementSize);
        /* step 18 */
        return array;
    }

    /**
     * 22.2.1.1.1 Runtime Semantics: AllocateTypedArray (newTarget, length )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param length
     *            the byte length
     * @return the new typed array instance
     */
    public static TypedArrayObject AllocateTypedArray(ExecutionContext cx, Constructor newTarget,
            long length) {
        /* step 1 */
        if (newTarget == null) {
            throw newTypeError(cx, Messages.Key.InvalidCall, "TypedArray");
        }
        /* step 2 */
        if (SameValue(cx.getIntrinsic(Intrinsics.TypedArray), newTarget)) {
            throw newTypeError(cx, Messages.Key.InvalidTypedArrayConstructor);
        }
        /* step 3 */
        ElementType elementType = null;
        /* step 4 */
        ScriptObject subclass = newTarget;
        /* step 5 */
        while (elementType == null) {
            /* step 5.a */
            if (subclass == null) {
                throw newTypeError(cx, Messages.Key.InvalidTypedArrayConstructor);
            }
            /* step 5.b (FIXME: spec bug 'abstractConstructor' not defined) */
            /* step 5.c */
            if (subclass instanceof TypedArrayConstructor) {
                elementType = ((TypedArrayConstructor) subclass).getElementType();
            }
            /* steps 5.d-e */
            subclass = subclass.getPrototypeOf(cx);
        }
        /* steps 6-7 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget,
                Intrinsics.TypedArrayPrototype);
        /* steps 8-10 */
        TypedArrayObject obj = new TypedArrayObject(cx.getRealm());
        obj.setPrototype(proto);
        obj.setElementType(elementType);
        /* steps 11-12 */
        if (length < 0) {
            /* step 11 */
            /* steps 11.a-c */
            obj.setByteLength(0);
            obj.setByteOffset(0);
            obj.setArrayLength(0);
        } else {
            /* step 12 */
            /* step 12.a */
            int elementSize = elementType.size();
            /* step 12.b */
            long byteLength = elementSize * length;
            /* steps 12.c-d */
            ArrayBufferObject data = AllocateArrayBuffer(cx,
                    (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer), byteLength);
            /* steps 12.e-h */
            obj.setBuffer(data);
            obj.setByteLength(byteLength);
            obj.setByteOffset(0);
            obj.setArrayLength(length);
        }
        /* step 13 */
        return obj;
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
            Callable f = null;
            if (!Type.isUndefined(mapfn)) {
                if (!IsCallable(mapfn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                f = (Callable) mapfn;
            }
            /* step 5 (omitted) */
            /* step 6 */
            return TypedArrayFrom(cx, (Constructor) c, source, f, thisArg);
        }

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
            /* step 4 */
            if (!IsConstructor(c)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 5-6 */
            TypedArrayObject newObj = AllocateTypedArray(cx, (Constructor) c, len);
            /* steps 7-8 */
            for (int k = 0; k < len; ++k) {
                int pk = k;
                Object value = items[k];
                Put(cx, newObj, pk, value, true);
            }
            /* step 9 */
            return newObj;
        }

        /**
         * 22.2.2.4 get %TypedArray% [ @@species ]
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the species object
         */
        @Accessor(name = "get [Symbol.species]", symbol = BuiltinSymbol.species,
                type = Accessor.Type.Getter)
        public static Object species(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }

    /**
     * 22.2.2.1.1 Runtime Semantics: TypedArrayFrom( constructor, items, mapfn, thisArg )
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function or {@code null}
     * @param items
     *            the source object
     * @param mapfn
     *            the mapping function or {@code null}
     * @param thisArg
     *            the this-argument for the mapping function or {@code null}
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArrayFrom(ExecutionContext cx, Constructor constructor,
            Object items, Callable mapfn, Object thisArg) {
        /* step 1 (omitted) */
        /* steps 2-3 (not applicable) */
        /* steps 4-5 */
        boolean mapping = mapfn != null;
        /* steps 6-7 */
        Callable usingIterator = GetMethod(cx, items, BuiltinSymbol.iterator.get());
        /* step 8 */
        if (usingIterator != null) {
            /* steps 8.a-8.b */
            ScriptObject iterator = GetIterator(cx, items, usingIterator);
            /* steps 8.c-8.f */
            ArrayList<Object> values = new ArrayList<>();
            for (;;) {
                ScriptObject next = IteratorStep(cx, iterator);
                if (next == null) {
                    break;
                }
                Object nextValue = IteratorValue(cx, next);
                values.add(nextValue);
            }
            /* step 8.g */
            int len = values.size();
            /* steps 8.h-8.i */
            TypedArrayObject targetObj = AllocateTypedArray(cx, constructor, len);
            /* steps 8.j-8.l */
            for (int k = 0; k < len; ++k) {
                long pk = k;
                Object kValue = values.get(k);
                Object mappedValue;
                if (mapping) {
                    mappedValue = mapfn.call(cx, thisArg, kValue, k);
                } else {
                    mappedValue = kValue;
                }
                Put(cx, targetObj, pk, mappedValue, true);
            }
            /* step 8.m */
            return targetObj;
        }
        /* step 9 (?) */
        /* steps 10-11 */
        ScriptObject arrayLike = ToObject(cx, items);
        /* steps 12-13 */
        long len = ToLength(cx, Get(cx, arrayLike, "length"));
        /* steps 14-15 */
        TypedArrayObject targetObj = AllocateTypedArray(cx, constructor, len);
        /* steps 16-17 */
        for (long k = 0; k < len; ++k) {
            long pk = k;
            Object kValue = Get(cx, arrayLike, pk);
            Object mappedValue;
            if (mapping) {
                mappedValue = mapfn.call(cx, thisArg, kValue, k);
            } else {
                mappedValue = kValue;
            }
            Put(cx, targetObj, pk, mappedValue, true);
        }
        /* step 18 */
        return targetObj;
    }
}
