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
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

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
     * 22.2.1.1 %TypedArray% ( )<br>
     * 22.2.1.2 %TypedArray% ( length )<br>
     * 22.2.1.3 %TypedArray% ( typedArray )<br>
     * 22.2.1.4 %TypedArray% ( array )<br>
     * 22.2.1.5 %TypedArray% ( buffer [ , byteOffset [ , length ] ] )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "TypedArray");
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
        if (args.length == 0) {
            return callWithLength(calleeContext, newTarget, 0);
        }
        Object arg0 = args[0];
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
     * 22.2.1.1 %TypedArray% ( )<br>
     * 22.2.1.2 %TypedArray% ( length )
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
        /* step 2 (not applicable) */
        /* step 3 */
        if (Type.isUndefined(length)) {
            // FIXME: spec issue - why TypeError?
            throw newTypeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* step 4 */
        double numberLength = ToNumber(cx, length);
        /* steps 5-6 */
        long elementLength = ToLength(numberLength);
        /* step 7 */
        if (numberLength != elementLength) { // SameValueZero
            throw newRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* step 8 */
        return AllocateTypedArray(cx, newTarget, elementLength);
    }

    /**
     * 22.2.1.3 %TypedArray% ( typedArray )
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
        /* step 2 (not applicable) */
        /* steps 3-4 (TypedArray allocation deferred) */
        ElementType elementType = GetElementTypeFromConstructor(cx, newTarget);
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget,
                Intrinsics.TypedArrayPrototype);
        /* step 5 */
        TypedArrayObject srcArray = typedArray;
        /* step 6 */
        ArrayBufferObject srcData = srcArray.getBuffer();
        /* step 7 */
        if (IsDetachedBuffer(srcData)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 8-9 (not applicable) */
        /* step 10 */
        long elementLength = srcArray.getArrayLength();
        /* steps 11-12 */
        ElementType srcType = srcArray.getElementType();
        /* step 13 */
        int srcElementSize = srcType.size();
        /* step 14 */
        long srcByteOffset = srcArray.getByteOffset();
        /* step 15 */
        int elementSize = elementType.size();
        /* step 16 */
        long byteLength = elementSize * elementLength;
        /* steps 17-18 */
        ArrayBufferObject data;
        if (elementType == srcType) {
            /* step 17 */
            data = CloneArrayBuffer(cx, srcData, srcByteOffset);
        } else {
            /* step 18 */
            /* steps 18.a-b */
            Constructor bufferConstructor = SpeciesConstructor(cx, srcData, Intrinsics.ArrayBuffer);
            /* steps 18.c-d */
            data = AllocateArrayBuffer(cx, bufferConstructor, byteLength);
            /* step 18.e */
            if (IsDetachedBuffer(srcData)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* step 18.f */
            long srcByteIndex = srcByteOffset;
            /* step 18.g */
            long targetByteIndex = 0;
            /* steps 18.h-i */
            for (long count = elementLength; count > 0; --count) {
                double value = GetValueFromBuffer(srcData, srcByteIndex, srcType);
                SetValueInBuffer(data, targetByteIndex, elementType, value);
                srcByteIndex += srcElementSize;
                targetByteIndex += elementSize;
            }
        }
        /* steps 19-22 */
        TypedArrayObject array = new TypedArrayObject(cx.getRealm(), elementType, data, byteLength,
                0, elementLength, proto);
        /* step 23 */
        return array;
    }

    /**
     * 22.2.1.4 %TypedArray% ( object )
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
        /* step 2 (not applicable) */
        assert !(object instanceof TypedArrayObject || object instanceof ArrayBufferObject);
        /* step 3 */
        return TypedArrayFrom(cx, newTarget, object, null, null);
    }

    /**
     * 22.2.1.5 %TypedArray% ( buffer [ , byteOffset [ , length ] ] )
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
        /* step 2 (not applicable) */
        /* steps 3-4 (TypedArray allocation deferred) */
        ElementType elementType = GetElementTypeFromConstructor(cx, newTarget);
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget,
                Intrinsics.TypedArrayPrototype);
        /* steps 5-6 */
        int elementSize = elementType.size();
        /* steps 7-8 */
        double offset = ToInteger(cx, byteOffset);
        /* steps 9-10 */
        if (offset < 0 || offset % elementSize != 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        long newByteOffset = (long) offset;
        /* step 11 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 12 */
        long bufferByteLength = buffer.getByteLength();
        /* steps 13-14 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 13 */
            if (bufferByteLength % elementSize != 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = bufferByteLength - newByteOffset;
            if (newByteLength < 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        } else {
            /* step 14 */
            long newLength = ToLength(cx, length);
            newByteLength = newLength * elementSize;
            if (newByteOffset + newByteLength > bufferByteLength) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        }
        /* steps 15-18 */
        TypedArrayObject array = new TypedArrayObject(cx.getRealm(), elementType, buffer,
                newByteLength, newByteOffset, newByteLength / elementSize, proto);
        /* step 19 */
        return array;
    }

    /**
     * 22.2.1.2.1 Runtime Semantics: AllocateTypedArray (newTarget, length )
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
        /* step 1 (not applicable) */
        /* steps 2-6 */
        ElementType elementType = GetElementTypeFromConstructor(cx, newTarget);
        /* steps 7-8 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget,
                Intrinsics.TypedArrayPrototype);
        /* steps 9-11 (moved) */
        /* step 12 (not applicable) */
        assert length >= 0;
        /* step 13 */
        /* step 13.a */
        int elementSize = elementType.size();
        /* steps 13.a-b */
        long byteLength = elementSize * length;
        /* steps 13.c-d */
        ArrayBufferObject data = AllocateArrayBuffer(cx,
                (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer), byteLength);
        /* steps 9-11, 13.e-h */
        TypedArrayObject obj = new TypedArrayObject(cx.getRealm(), elementType, data, byteLength,
                0, length, proto);
        /* step 14 */
        return obj;
    }

    /**
     * 22.2.1.2.1 Runtime Semantics: AllocateTypedArray (newTarget, length )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @return the element type
     */
    private static ElementType GetElementTypeFromConstructor(ExecutionContext cx,
            Constructor newTarget) {
        /* steps 2-6 */
        final OrdinaryObject typedArray = cx.getIntrinsic(Intrinsics.TypedArray);
        for (ScriptObject subclass = newTarget;;) {
            /* steps 2, 6.a-b */
            if (subclass == null || subclass == typedArray) {
                throw newTypeError(cx, Messages.Key.InvalidTypedArrayConstructor);
            }
            /* step 6.c */
            if (subclass instanceof TypedArrayConstructor) {
                return ((TypedArrayConstructor) subclass).getElementType();
            }
            /* steps 6.d-e */
            subclass = subclass.getPrototypeOf(cx);
        }
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
                /* step 8.a */
                Object value = items[k];
                /* step 8.b */
                int pk = k;
                /* steps 8.c-d */
                newObj.elementSetDirect(cx, pk, ToNumber(cx, value));
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
            /* steps 8.a-b */
            ScriptIterator<?> iterator = GetScriptIterator(cx, items, usingIterator);
            /* step 8.c */
            ArrayList<Object> values = new ArrayList<>();
            /* steps 8.d-e */
            while (iterator.hasNext()) {
                Object nextValue = iterator.next();
                values.add(nextValue);
            }
            /* step 8.f */
            int len = values.size();
            /* steps 8.g-h */
            TypedArrayObject targetObj = AllocateTypedArray(cx, constructor, len);
            /* steps 8.i-k */
            for (int k = 0; k < len; ++k) {
                long pk = k;
                Object kValue = values.get(k);
                Object mappedValue;
                if (mapping) {
                    mappedValue = mapfn.call(cx, thisArg, kValue, k);
                } else {
                    mappedValue = kValue;
                }
                targetObj.elementSetDirect(cx, pk, ToNumber(cx, mappedValue));
            }
            /* step 8.l */
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
            /* step 17.a */
            long pk = k;
            /* steps 17.b-c */
            Object kValue = Get(cx, arrayLike, pk);
            /* steps 17.d-e */
            Object mappedValue;
            if (mapping) {
                mappedValue = mapfn.call(cx, thisArg, kValue, k);
            } else {
                mappedValue = kValue;
            }
            /* steps 17.f-g */
            targetObj.elementSetDirect(cx, pk, ToNumber(cx, mappedValue));
        }
        /* step 18 */
        return targetObj;
    }
}
