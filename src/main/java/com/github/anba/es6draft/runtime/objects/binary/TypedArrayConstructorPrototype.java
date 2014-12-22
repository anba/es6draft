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
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
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
public final class TypedArrayConstructorPrototype extends BuiltinFunction implements Initializable,
        Creatable<TypedArrayObject> {
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
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
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
        Object arg0 = argument(args, 0);
        if (!Type.isObject(arg0)) {
            return callWithLength(calleeContext, thisValue, arg0);
        }
        if (arg0 instanceof TypedArrayObject) {
            return callWithTypedArray(calleeContext, thisValue, (TypedArrayObject) arg0);
        }
        if (arg0 instanceof ArrayBufferObject) {
            Object byteOffset = argument(args, 1, 0);
            Object length = argument(args, 2);
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
        /* steps 14-15 */
        ArrayBufferObject data = AllocateArrayBuffer(cx);
        /* step 16 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* step 17 */
        int elementSize = elementType.size();
        /* step 18 */
        long byteLength = elementSize * elementLength;
        /* steps 19-20 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 21-24 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 25 */
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
        ArrayBufferObject srcData = srcArray.getBuffer();
        /* step 9 */
        if (srcData == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 10 */
        if (IsDetachedBuffer(srcData)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 11-12 */
        ElementType elementType = array.getElementType();
        /* step 13 */
        long elementLength = srcArray.getArrayLength();
        /* steps 14-15 */
        ElementType srcType = srcArray.getElementType();
        /* step 16 */
        int srcElementSize = srcType.size();
        /* step 17 */
        long srcByteOffset = srcArray.getByteOffset();
        /* step 18 */
        int elementSize = elementType.size();
        /* step 19 */
        long byteLength = elementSize * elementLength;
        /* steps 20-21 */
        ArrayBufferObject data;
        if (elementType == srcType) {
            /* step 20 */
            data = CloneArrayBuffer(cx, srcData, srcByteOffset);
        } else {
            /* step 21 */
            /* steps 21.a-21.b */
            Constructor bufferConstructor = SpeciesConstructor(cx, srcData, Intrinsics.ArrayBuffer);
            /* step 21.c */
            data = AllocateArrayBuffer(cx, bufferConstructor);
            /* step 21.d */
            if (IsDetachedBuffer(srcData)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* steps 21.e-21.f */
            SetArrayBufferData(cx, data, byteLength);
            /* step 21.g */
            long srcByteIndex = srcByteOffset;
            /* step 21.h */
            long targetByteIndex = 0;
            /* steps 21.i-21.j */
            for (long count = elementLength; count > 0; --count) {
                double value = GetValueFromBuffer(srcData, srcByteIndex, srcType);
                SetValueInBuffer(data, targetByteIndex, elementType, value);
                srcByteIndex += srcElementSize;
                targetByteIndex += elementSize;
            }
        }
        /* steps 23-23 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 24-27 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 28 */
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
        if (!(obj instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 4 */
        if (array.getElementType() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* steps 5-6 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 7-9 */
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
        if (!buffer.isInitialized()) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 4 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 5 */
        if (!(obj instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 6 */
        if (array.getElementType() == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* steps 7-8 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 9-10 */
        ElementType elementType = array.getElementType();
        /* step 11 */
        int elementSize = elementType.size();
        /* steps 12-13 */
        // TODO: spec issue? - does not follow the ToNumber(v) == ToInteger(v) pattern
        double offset = ToInteger(cx, byteOffset);
        /* step 14 */
        if (offset < 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 15 */
        if (offset % elementSize != 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 16 */
        long bufferByteLength = buffer.getByteLength();
        /* steps 17-18 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 17 */
            if (bufferByteLength % elementSize != 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = (long) (bufferByteLength - offset);
            if (newByteLength < 0) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        } else {
            /* step 18 */
            long newLength = ToLength(cx, length);
            newByteLength = newLength * elementSize;
            if (offset + newByteLength > bufferByteLength) {
                throw newRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        }
        /* step 19 */
        if (array.getBuffer() != null) {
            throw newTypeError(cx, Messages.Key.InitializedObject);
        }
        /* steps 20-23 */
        array.setBuffer(buffer);
        array.setByteLength(newByteLength);
        array.setByteOffset((long) offset);
        array.setArrayLength(newByteLength / elementSize);
        /* step 24 */
        return array;
    }

    @Override
    public CreateAction<TypedArrayObject> createAction() {
        return TypedArrayCreate.INSTANCE;
    }

    static final class TypedArrayCreate implements CreateAction<TypedArrayObject> {
        static final CreateAction<TypedArrayObject> INSTANCE = new TypedArrayCreate();

        @Override
        public TypedArrayObject create(ExecutionContext cx, Constructor constructor, Object... args) {
            /* steps 1-2 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, constructor,
                    Intrinsics.TypedArrayPrototype);
            /* steps 3 */
            TypedArrayObject obj = new TypedArrayObject(cx.getRealm());
            obj.setPrototype(proto);
            /* steps 4-8 (implicit) */
            /* step 9 */
            return obj;
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
                int pk = k;
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
            Callable f = null;
            if (!Type.isUndefined(mapfn)) {
                if (!IsCallable(mapfn)) {
                    throw newTypeError(cx, Messages.Key.NotCallable);
                }
                f = (Callable) mapfn;
            }
            /* step 5 (omitted) */
            /* step 6 */
            return TypedArrayFrom(cx, (Constructor) c, null, source, f, thisArg);
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
            TypedArrayObject target, Object items, Callable mapfn, Object thisArg) {
        /* step 1 (omitted) */
        /* step 2 */
        assert constructor == null ^ target == null;
        /* step 3 (not applicable) */
        /* step 4 */
        assert target == null || (target.getElementType() != null && target.getBuffer() == null);
        /* step 5 (not applicable) */
        /* steps 6-7 */
        boolean mapping = mapfn != null;
        /* steps 8-9 */
        Object usingIterator = CheckIterable(cx, items);
        /* step 10 */
        if (!Type.isUndefined(usingIterator)) {
            /* steps 10.a-10.b */
            ScriptObject iterator = GetIterator(cx, items, usingIterator);
            /* steps 10.c-10.f */
            ArrayList<Object> values = new ArrayList<>();
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
            ScriptObject targetObj = TypedArrayAllocOrInit(cx, constructor, target, len);
            /* steps 10.j-10.l */
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
            /* step 10.m */
            return targetObj;
        }
        /* step 11 (?) */
        /* steps 12-13 */
        ScriptObject arrayLike = ToObject(cx, items);
        /* step 14 */
        Object lenValue = Get(cx, arrayLike, "length");
        /* steps 15-16 */
        long len = ToLength(cx, lenValue);
        /* steps 17-18 */
        ScriptObject targetObj = TypedArrayAllocOrInit(cx, constructor, target, len);
        /* steps 19-20 */
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
        /* step 21 */
        return targetObj;
    }

    /**
     * 22.2.2.1.2 Runtime Semantics: TypedArrayAllocOrInit( constructor, target, length)
     * <p>
     * Initializes the typed array object.
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function or {@code null}
     * @param target
     *            the typed array object or {@code null}
     * @param length
     *            the typed array object length
     * @return the new typed array object
     */
    public static ScriptObject TypedArrayAllocOrInit(ExecutionContext cx, Constructor constructor,
            TypedArrayObject target, long length) {
        /* step 1 */
        assert constructor == null ^ target == null;
        /* step 2 (not applicable) */
        /* step 3 */
        assert target == null || target.getElementType() != null;
        /* step 4 (not applicable) */
        /* steps 5-6 */
        ScriptObject targetObj;
        if (target == null) {
            /* step 5 */
            targetObj = constructor.construct(cx, length);
        } else {
            /* step 6 */
            /* steps 6.j-6.k */
            if (target.getBuffer() != null) {
                throw newTypeError(cx, Messages.Key.InitializedObject);
            }
            /* step 6.a */
            targetObj = target;
            /* steps 6.b-6.c */
            ElementType elementType = target.getElementType();
            /* steps 6.d-6.e */
            ArrayBufferObject data = AllocateArrayBuffer(cx);
            /* step 6.f */
            int elementSize = elementType.size();
            /* step 6.g */
            long byteLength = elementSize * length;
            /* steps 6.h-6.i */
            SetArrayBufferData(cx, data, byteLength);
            /* steps 6.j-6.k */
            assert target.getBuffer() == null;
            /* steps 6.l-6.o */
            target.setBuffer(data);
            target.setByteLength(byteLength);
            target.setByteOffset(0);
            target.setArrayLength(length);
        }
        /* step 7 */
        return targetObj;
    }
}
