/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.atomics.SharedArrayBufferConstructor.IsSharedArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.AllocateArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CloneArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructorPrototype.IterableToList;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
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
 * <li>22.2.4 The TypedArray Constructors
 * <li>22.2.5 Properties of the TypedArray Constructors
 * </ul>
 */
public final class TypedArrayConstructor extends BuiltinConstructor implements Initializable {
    /** [[ElementType]] */
    private final ElementType elementType;

    /**
     * Constructs a new TypedArray constructor function.
     * 
     * @param realm
     *            the realm object
     * @param elementType
     *            the typed array element type
     */
    public TypedArrayConstructor(Realm realm, ElementType elementType) {
        super(realm, elementType.getConstructorName(), 3);
        this.elementType = elementType;
    }

    /**
     * [[ElementType]]
     *
     * @return the typed array constructor element type
     */
    public ElementType getElementType() {
        return elementType;
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, propertiesForType(elementType));
    }

    /**
     * 22.2.4.1 TypedArray ( )<br>
     * 22.2.4.2 TypedArray ( length )<br>
     * 22.2.4.3 TypedArray ( typedArray )<br>
     * 22.2.4.4 TypedArray ( object )<br>
     * 22.2.4.5 TypedArray ( buffer [ , byteOffset [ , length ] ] )<br>
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, elementType.getConstructorName());
    }

    /**
     * 22.2.4.1 TypedArray ( )<br>
     * 22.2.4.2 TypedArray ( length )<br>
     * 22.2.4.3 TypedArray ( typedArray )<br>
     * 22.2.4.4 TypedArray ( object )<br>
     * 22.2.4.5 TypedArray ( buffer [ , byteOffset [ , length ] ] )<br>
     */
    @Override
    public TypedArrayObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        if (args.length == 0) {
            return constructWithNoArguments(calleeContext, newTarget);
        }
        Object arg0 = args[0];
        if (!Type.isObject(arg0)) {
            return constructWithLength(calleeContext, newTarget, arg0);
        }
        if (arg0 instanceof TypedArrayObject) {
            return constructWithTypedArray(calleeContext, newTarget, (TypedArrayObject) arg0);
        }
        if (arg0 instanceof ArrayBuffer) {
            Object byteOffset = argument(args, 1, 0);
            Object length = argument(args, 2);
            return constructWithArrayBuffer(calleeContext, newTarget, (ArrayBuffer) arg0, byteOffset, length);
        }
        return constructWithObject(calleeContext, newTarget, (ScriptObject) arg0);
    }

    /**
     * 22.2.4.1 TypedArray ( )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param length
     *            the typed array length
     * @return the typed array object
     */
    private TypedArrayObject constructWithNoArguments(ExecutionContext cx, Constructor newTarget) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        return AllocateTypedArray(cx, elementType, newTarget, prototypeForType(elementType), 0);
    }

    /**
     * 22.2.4.2 TypedArray ( length )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param length
     *            the typed array length
     * @return the typed array object
     */
    private TypedArrayObject constructWithLength(ExecutionContext cx, Constructor newTarget, Object length) {
        /* step 1 */
        assert !Type.isObject(length);
        /* step 2 (not applicable) */
        /* step 3 */
        long elementLength = ToIndex(cx, length);
        /* steps 4-5 */
        return AllocateTypedArray(cx, elementType, newTarget, prototypeForType(elementType), elementLength);
    }

    /**
     * 22.2.4.3 TypedArray ( typedArray )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param typedArray
     *            the source typed array object
     * @return the typed array object
     */
    private TypedArrayObject constructWithTypedArray(ExecutionContext cx, Constructor newTarget,
            TypedArrayObject typedArray) {
        /* step 1 (implicit) */
        /* step 2 (not applicable) */
        /* steps 3-4 (TypedArray allocation deferred) */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, prototypeForType(elementType));
        /* step 5 */
        TypedArrayObject srcArray = typedArray;
        /* step 6 */
        ArrayBuffer srcData = srcArray.getBuffer();
        /* step 7 */
        if (IsDetachedBuffer(srcData)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 8-9 */
        ElementType elementType = this.elementType;
        /* step 10 */
        long elementLength = srcArray.getArrayLength();
        /* steps 11-12 */
        ElementType srcType = srcArray.getElementType();
        /* step 13 (omitted) */
        /* step 14 */
        long srcByteOffset = srcArray.getByteOffset();
        /* steps 15-16 */
        long byteLength = elementType.toBytes(elementLength);
        /* steps 17-18 */
        ArrayBufferObject data;
        if (elementType == srcType) {
            /* step 17.a */
            if (!IsSharedArrayBuffer(srcData)) {
                data = CloneArrayBuffer(cx, srcData, srcByteOffset, byteLength);
            } else {
                data = CloneArrayBuffer(cx, srcData, srcByteOffset, byteLength,
                        (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer));
            }
        } else {
            /* step 18 */
            /* step 18.a */
            Constructor bufferConstructor;
            if (!IsSharedArrayBuffer(srcData)) {
                bufferConstructor = SpeciesConstructor(cx, srcData, Intrinsics.ArrayBuffer);
            } else {
                bufferConstructor = (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer);
            }
            /* step 18.b */
            data = AllocateArrayBuffer(cx, bufferConstructor, byteLength);
            /* step 18.c */
            if (IsDetachedBuffer(srcData)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* steps 18.d-g */
            srcArray.functions().construct(cx, srcArray, data, elementType);
        }
        /* steps 4, 19-23 */
        return new TypedArrayObject(cx.getRealm(), elementType, data, byteLength, 0, elementLength, proto);
    }

    /**
     * 22.2.4.4 TypedArray ( object )
     * 
     * @param cx
     *            the execution context
     * @param newTarget
     *            the NewTarget constructor object
     * @param object
     *            the source object
     * @return the typed array object
     */
    private TypedArrayObject constructWithObject(ExecutionContext cx, Constructor newTarget, ScriptObject object) {
        /* step 1 */
        assert !(object instanceof TypedArrayObject || object instanceof ArrayBuffer);
        /* step 2 (not applicable) */
        /* steps 3-4 (TypedArray allocation deferred) */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, prototypeForType(elementType));
        /* step 5 */
        Callable usingIterator = GetMethod(cx, object, BuiltinSymbol.iterator.get());
        /* step 6 */
        if (usingIterator != null) {
            /* step 6.a */
            List<?> values = IterableToList(cx, object, usingIterator);
            /* step 6.b */
            int len = values.size();
            /* step 6.c */
            TypedArrayObject targetObj = AllocateTypedArray(cx, elementType, proto, len);
            /* steps 6.d-e */
            for (int k = 0; k < len; ++k) {
                /* step 6.e.i */
                int pk = k;
                /* step 6.e.ii */
                Object kValue = values.get(pk);
                /* step 6.e.iii */
                targetObj.elementSetUnchecked(cx, pk, kValue);
            }
            /* step 6.f (not applicable) */
            /* step 6.g */
            return targetObj;
        }
        /* step 7 (note) */
        /* step 8 */
        ScriptObject arrayLike = object;
        /* step 9 */
        long len = ToLength(cx, Get(cx, arrayLike, "length"));
        /* step 10 */
        TypedArrayObject targetObj = AllocateTypedArray(cx, elementType, proto, len);
        /* steps 11-12 */
        for (long k = 0; k < len; ++k) {
            /* step 12.a */
            long pk = k;
            /* step 12.b */
            Object kValue = Get(cx, arrayLike, pk);
            /* step 12.c */
            targetObj.elementSetUnchecked(cx, pk, kValue);
        }
        /* step 13 */
        return targetObj;
    }

    /**
     * 22.2.4.5 TypedArray ( buffer [ , byteOffset [ , length ] ] )
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
    private TypedArrayObject constructWithArrayBuffer(ExecutionContext cx, Constructor newTarget, ArrayBuffer buffer,
            Object byteOffset, Object length) {
        /* step 1 (implicit) */
        /* step 2 (not applicable) */
        /* steps 3-4 (TypedArray allocation deferred) */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, prototypeForType(elementType));
        /* step 5 */
        int elementSize = elementType.size();
        /* step 6 */
        long offset = ToIndex(cx, byteOffset);
        /* steps 7 */
        if (offset % elementSize != 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 8 */
        // FIXME: spec issue - DataView constructor not updated
        // FIXME: spec issue - branching not needed, ToIndex(length) is always ok.
        long newLength = ToIndex(cx, length);
        /* step 9 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 10 */
        long bufferByteLength = buffer.getByteLength();
        /* steps 11-12 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 11 */
            if (bufferByteLength % elementSize != 0) {
                throw newRangeError(cx, Messages.Key.InvalidByteLength);
            }
            newByteLength = bufferByteLength - offset;
            if (newByteLength < 0) {
                throw newRangeError(cx, Messages.Key.InvalidByteLength);
            }
        } else {
            /* step 12 */
            newByteLength = newLength * elementSize;
            if (offset + newByteLength > bufferByteLength) {
                throw newRangeError(cx, Messages.Key.InvalidByteLength);
            }
        }
        /* steps 4, 13-17 */
        long actualLength = newByteLength / elementSize;
        return new TypedArrayObject(cx.getRealm(), elementType, buffer, newByteLength, offset, actualLength, proto);
    }

    /**
     * 22.2.4.2.1 Runtime Semantics: AllocateTypedArray (constructorName, newTarget, defaultProto, length )
     * 
     * @param cx
     *            the execution context
     * @param elementType
     *            the constructor element type
     * @param newTarget
     *            the NewTarget constructor object
     * @param defaultProto
     *            the intrinsic default prototype
     * @param length
     *            the byte length
     * @return the new typed array instance
     */
    public static TypedArrayObject AllocateTypedArray(ExecutionContext cx, ElementType elementType,
            Constructor newTarget, Intrinsics defaultProto, long length) {
        /* step 1 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, newTarget, defaultProto);
        /* step 2 (moved) */
        /* step 3 (not applicable) */
        /* step 4 (moved) */
        /* step 5 (not applicable) */
        /* step 6 */
        ArrayBufferObject data = AllocateTypedArrayBuffer(cx, elementType, length);
        long byteLength = data.getByteLength();
        /* steps 2, 4, 6-7 */
        return new TypedArrayObject(cx.getRealm(), elementType, data, byteLength, 0, length, proto);
    }

    /**
     * 22.2.4.2.1 Runtime Semantics: AllocateTypedArray (constructorName, newTarget, defaultProto, length )
     * 
     * @param cx
     *            the execution context
     * @param elementType
     *            the constructor element type
     * @param proto
     *            the prototype object
     * @param length
     *            the byte length
     * @return the new typed array instance
     */
    public static TypedArrayObject AllocateTypedArray(ExecutionContext cx, ElementType elementType, ScriptObject proto,
            long length) {
        /* step 1 (not applicable) */
        /* step 2 (moved) */
        /* step 3 (not applicable) */
        /* step 4 (moved) */
        /* step 5 (not applicable) */
        /* step 6 */
        ArrayBufferObject data = AllocateTypedArrayBuffer(cx, elementType, length);
        long byteLength = data.getByteLength();
        /* steps 2, 4, 6-7 */
        return new TypedArrayObject(cx.getRealm(), elementType, data, byteLength, 0, length, proto);
    }

    /**
     * 22.2.4.2.2 Runtime Semantics: AllocateTypedArrayBuffer ( O, length )
     * 
     * @param cx
     *            the execution context
     * @param elementType
     *            the constructor element type
     * @param length
     *            the byte length
     * @return the new array buffer instance
     */
    public static ArrayBufferObject AllocateTypedArrayBuffer(ExecutionContext cx, ElementType elementType,
            long length) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        assert length >= 0;
        /* steps 4-6 */
        long byteLength = elementType.toBytes(length);
        /* step 7 */
        /* steps 8-12 (not applicable) */
        return AllocateArrayBuffer(cx, (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer), byteLength);
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
    private static TypedArrayObject ValidateTypedArray(ExecutionContext cx, Object thisValue, String method) {
        /* steps 1-3 */
        if (!(thisValue instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleNewObject, method, Type.of(thisValue).toString());
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
     * TypedArrayCreate( constructor, argumentList )
     * 
     * @param cx
     *            the execution context
     * @param method
     *            the method name
     * @param constructor
     *            the constructor function
     * @param length
     *            the new typed array length
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArrayCreate(ExecutionContext cx, String method, Constructor constructor,
            long length) {
        /* step 1 */
        ScriptObject newObject = constructor.construct(cx, length);
        /* step 2 */
        TypedArrayObject newTypedArray = ValidateTypedArray(cx, newObject, method);
        /* step 3 */
        if (newTypedArray.getArrayLength() < length) {
            throw newTypeError(cx, Messages.Key.InvalidTypedArrayLength);
        }
        /* step 4 */
        return newTypedArray;
    }

    /**
     * TypedArrayCreate( constructor, argumentList )
     * 
     * @param cx
     *            the execution context
     * @param method
     *            the method name
     * @param constructor
     *            the constructor function
     * @param args
     *            the constructor function arguments
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArrayCreate(ExecutionContext cx, String method, Constructor constructor,
            Object... args) {
        /* step 1 */
        ScriptObject newObject = constructor.construct(cx, args);
        /* step 2 */
        TypedArrayObject newTypedArray = ValidateTypedArray(cx, newObject, method);
        /* step 3 (not applicable) */
        /* step 4 */
        return newTypedArray;
    }

    /**
     * TypedArraySpeciesCreate( exemplar, argumentList )
     * 
     * @param cx
     *            the execution context
     * @param method
     *            the method name
     * @param exemplar
     *            the typed array object
     * @param length
     *            the new typed array length
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArraySpeciesCreate(ExecutionContext cx, String method,
            TypedArrayObject exemplar, long length) {
        /* step 1 (implicit) */
        /* step 2 */
        Intrinsics defaultConstructor = exemplar.getElementType().getConstructor();
        /* step 3 */
        Constructor constructor = SpeciesConstructor(cx, exemplar, defaultConstructor);
        /* step 4 */
        return TypedArrayCreate(cx, method, constructor, length);
    }

    /**
     * TypedArraySpeciesCreate( exemplar, argumentList )
     * 
     * @param cx
     *            the execution context
     * @param method
     *            the method name
     * @param exemplar
     *            the typed array object
     * @param args
     *            the constructor function arguments
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArraySpeciesCreate(ExecutionContext cx, String method,
            TypedArrayObject exemplar, Object... args) {
        /* step 1 (implicit) */
        /* step 2 */
        Intrinsics defaultConstructor = exemplar.getElementType().getConstructor();
        /* step 3 */
        Constructor constructor = SpeciesConstructor(cx, exemplar, defaultConstructor);
        /* step 4 */
        return TypedArrayCreate(cx, method, constructor, args);
    }

    private static Intrinsics prototypeForType(ElementType elementType) {
        switch (elementType) {
        case Int8:
            return Intrinsics.Int8ArrayPrototype;
        case Uint8:
            return Intrinsics.Uint8ArrayPrototype;
        case Uint8C:
            return Intrinsics.Uint8ClampedArrayPrototype;
        case Int16:
            return Intrinsics.Int16ArrayPrototype;
        case Uint16:
            return Intrinsics.Uint16ArrayPrototype;
        case Int32:
            return Intrinsics.Int32ArrayPrototype;
        case Uint32:
            return Intrinsics.Uint32ArrayPrototype;
        case BigInt64:
            return Intrinsics.BigInt64ArrayPrototype;
        case BigUint64:
            return Intrinsics.BigUint64ArrayPrototype;
        case Float32:
            return Intrinsics.Float32ArrayPrototype;
        case Float64:
            return Intrinsics.Float64ArrayPrototype;
        default:
            throw new AssertionError();
        }
    }

    private static Class<?> propertiesForType(ElementType elementType) {
        switch (elementType) {
        case Int8:
            return Properties_Int8Array.class;
        case Uint8:
            return Properties_Uint8Array.class;
        case Uint8C:
            return Properties_Uint8Clamped.class;
        case Int16:
            return Properties_Int16Array.class;
        case Uint16:
            return Properties_Uint16Array.class;
        case Int32:
            return Properties_Int32Array.class;
        case Uint32:
            return Properties_Uint32Array.class;
        case BigInt64:
            return Properties_BigInt64Array.class;
        case BigUint64:
            return Properties_BigUint64Array.class;
        case Float32:
            return Properties_Float32Array.class;
        case Float64:
            return Properties_Float64Array.class;
        default:
            throw new AssertionError();
        }
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Int8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Int8.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Int8ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int8.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint8Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Uint8.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint8ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint8.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint8Clamped {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Uint8C.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint8ClampedArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint8C.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Int16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Int16.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Int16ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int16.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint16Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Uint16.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint16ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint16.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Int32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Int32.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Int32ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Int32.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Uint32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Uint32.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Uint32ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Uint32.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Float32Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Float32.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Float32ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Float32.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_Float64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.Float64.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.Float64ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.Float64.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_BigInt64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.BigInt64.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.BigInt64ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.BigInt64.size();
    }

    /**
     * 22.2.5 Properties of the TypedArray Constructors
     */
    public enum Properties_BigUint64Array {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.TypedArray;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = ElementType.BigUint64.getConstructorName();

        /**
         * 22.2.5.2 TypedArray.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.BigUint64ArrayPrototype;

        /**
         * 22.2.5.1 TypedArray.BYTES_PER_ELEMENT
         */
        @Value(name = "BYTES_PER_ELEMENT",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final int BYTES_PER_ELEMENT = ElementType.BigUint64.size();
    }
}
