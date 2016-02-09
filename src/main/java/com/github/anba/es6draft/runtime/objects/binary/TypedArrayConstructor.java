/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.SpeciesConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToLength;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.*;
import static com.github.anba.es6draft.runtime.objects.binary.TypedArrayConstructorPrototype.IterableToArrayLike;
import static com.github.anba.es6draft.runtime.objects.binary.TypedArrayPrototypePrototype.ValidateTypedArray;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
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

    @Override
    public TypedArrayConstructor clone() {
        return new TypedArrayConstructor(getRealm(), elementType);
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
        if (Type.isUndefined(length)) {
            throw newTypeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* step 4 */
        double numberLength = ToNumber(cx, length);
        /* step 5 */
        long elementLength = ToLength(numberLength);
        /* step 6 */
        if (numberLength != elementLength) { // SameValueZero
            throw newRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* steps 7-8 */
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
            /* step 18.a */
            // FIXME: spec bug - SharedArrayBuffers not handled correctly!
            Constructor bufferConstructor = SpeciesConstructor(cx, srcData, Intrinsics.ArrayBuffer);
            /* step 18.b */
            data = AllocateArrayBuffer(cx, bufferConstructor, byteLength);
            /* step 18.c */
            if (IsDetachedBuffer(srcData)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* step 18.d */
            long srcByteIndex = srcByteOffset;
            /* step 18.e */
            long targetByteIndex = 0;
            /* steps 18.f-g */
            for (long count = elementLength; count > 0; --count) {
                double value = GetValueFromBuffer(srcData, srcByteIndex, srcType);
                SetValueInBuffer(data, targetByteIndex, elementType, value);
                srcByteIndex += srcElementSize;
                targetByteIndex += elementSize;
            }
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
        List<Object> arrayLike = IterableToArrayLike(cx, object);
        /* step 6 */
        int len = arrayLike.size();
        /* step 7 */
        TypedArrayObject targetObj = AllocateTypedArray(cx, elementType, proto, len);
        /* steps 8-9 */
        for (int k = 0; k < len; ++k) {
            /* step 9.a */
            int pk = k;
            /* step 9.b */
            Object kValue = arrayLike.get(k);
            /* step 9.c */
            targetObj.elementSetDirect(cx, pk, ToNumber(cx, kValue));
        }
        /* step 10 */
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
        /* steps 5-6 */
        int elementSize = elementType.size();
        /* step 7 */
        double offset = ToInteger(cx, byteOffset);
        /* steps 8-10 */
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
        /* steps 4, 15-19 */
        long newLength = newByteLength / elementSize;
        return new TypedArrayObject(cx.getRealm(), elementType, buffer, newByteLength, newByteOffset, newLength, proto);
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
        /* steps 4-5 */
        int elementSize = elementType.size();
        /* step 6 */
        long byteLength = elementSize * length;
        /* steps 7 */
        /* steps 8-12 (not applicable) */
        return AllocateArrayBuffer(cx, (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer), byteLength);
    }

    /**
     * TypedArrayCreate( constructor, argumentList )
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @param length
     *            the new typed array length
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArrayCreate(ExecutionContext cx, Constructor constructor, long length) {
        /* step 1 */
        ScriptObject newObject = constructor.construct(cx, length);
        /* step 2 */
        TypedArrayObject newTypedArray = ValidateTypedArray(cx, newObject);
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
     * @param constructor
     *            the constructor function
     * @param args
     *            the constructor function arguments
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArrayCreate(ExecutionContext cx, Constructor constructor, Object... args) {
        /* step 1 */
        ScriptObject newObject = constructor.construct(cx, args);
        /* step 2 */
        TypedArrayObject newTypedArray = ValidateTypedArray(cx, newObject);
        /* step 3 (not applicable) */
        /* step 4 */
        return newTypedArray;
    }

    /**
     * TypedArraySpeciesCreate( exemplar, argumentList )
     * 
     * @param cx
     *            the execution context
     * @param exemplar
     *            the typed array object
     * @param length
     *            the new typed array length
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArraySpeciesCreate(ExecutionContext cx, TypedArrayObject exemplar,
            long length) {
        /* step 1 (implicit) */
        /* step 2 */
        Intrinsics defaultConstructor = exemplar.getElementType().getConstructor();
        /* step 3 */
        Constructor constructor = SpeciesConstructor(cx, exemplar, defaultConstructor);
        /* step 4 */
        return TypedArrayCreate(cx, constructor, length);
    }

    /**
     * TypedArraySpeciesCreate( exemplar, argumentList )
     * 
     * @param cx
     *            the execution context
     * @param exemplar
     *            the typed array object
     * @param args
     *            the constructor function arguments
     * @return the new typed array object
     */
    public static TypedArrayObject TypedArraySpeciesCreate(ExecutionContext cx, TypedArrayObject exemplar,
            Object... args) {
        /* step 1 (implicit) */
        /* step 2 */
        Intrinsics defaultConstructor = exemplar.getElementType().getConstructor();
        /* step 3 */
        Constructor constructor = SpeciesConstructor(cx, exemplar, defaultConstructor);
        /* step 4 */
        return TypedArrayCreate(cx, constructor, args);
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
}
