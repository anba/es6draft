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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.1 ArrayBuffer Objects</h2>
 * <ul>
 * <li>24.1.1 Abstract Operations For ArrayBuffer Objects
 * <li>24.1.2 The ArrayBuffer Constructor
 * <li>24.1.3 Properties of the ArrayBuffer Constructor
 * </ul>
 */
public final class ArrayBufferConstructor extends BuiltinConstructor implements Initializable,
        Creatable<ArrayBufferObject> {
    // set default byte-order to little-endian - implementation specific choice
    private static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final boolean IS_LITTLE_ENDIAN = true;
    static {
        assert IS_LITTLE_ENDIAN == (DEFAULT_BYTE_ORDER == ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Constructs a new ArrayBuffer constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public ArrayBufferConstructor(Realm realm) {
        super(realm, "ArrayBuffer", 1);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public ArrayBufferConstructor clone() {
        return new ArrayBufferConstructor(getRealm());
    }

    private static final class ArrayBufferObjectAllocator implements
            ObjectAllocator<ArrayBufferObject> {
        static final ObjectAllocator<ArrayBufferObject> INSTANCE = new ArrayBufferObjectAllocator();

        @Override
        public ArrayBufferObject newInstance(Realm realm) {
            return new ArrayBufferObject(realm);
        }
    }

    private static final class ArrayBufferCreate implements CreateAction<ArrayBufferObject> {
        static final CreateAction<ArrayBufferObject> INSTANCE = new ArrayBufferCreate();

        @Override
        public ArrayBufferObject create(ExecutionContext cx, Constructor constructor,
                Object... args) {
            return AllocateArrayBuffer(cx, constructor);
        }
    }

    /**
     * 6.2.6.1 CreateByteDataBlock(size)
     * 
     * @param cx
     *            the execution context
     * @param size
     *            the byte buffer size in bytes
     * @return the new byte buffer
     */
    public static ByteBuffer CreateByteDataBlock(ExecutionContext cx, long size) {
        /* step 1 */
        assert size >= 0;
        /* step 2 */
        if (size > Integer.MAX_VALUE) {
            throw newRangeError(cx, Messages.Key.OutOfMemory);
        }
        try {
            /* step 3 */
            return ByteBuffer.allocate((int) size).order(DEFAULT_BYTE_ORDER);
        } catch (OutOfMemoryError e) {
            /* step 2 */
            throw newRangeError(cx, Messages.Key.OutOfMemoryVM);
        }
    }

    /**
     * 6.2.6.2 CopyDataBlockBytes(toBlock, toIndex, fromBlock, fromIndex, count)
     * 
     * @param toBlock
     *            the target byte buffer
     * @param toIndex
     *            the target offset
     * @param fromBlock
     *            the source byte buffer
     * @param fromIndex
     *            the source offset
     * @param count
     *            the number of bytes to copy
     */
    public static void CopyDataBlockBytes(ByteBuffer toBlock, long toIndex, ByteBuffer fromBlock,
            long fromIndex, long count) {
        /* step 1 */
        assert fromBlock != toBlock;
        /* step 2 */
        assert fromIndex >= 0 && toIndex >= 0 && count >= 0;
        /* steps 3-4 */
        assert fromIndex + count <= fromBlock.capacity();
        /* steps 5-6 */
        assert toIndex + count <= toBlock.capacity();

        /* steps 7-8 */
        fromBlock.limit((int) (fromIndex + count)).position((int) fromIndex);
        toBlock.limit((int) (toIndex + count)).position((int) toIndex);
        toBlock.put(fromBlock);
        toBlock.clear();
        fromBlock.clear();
    }

    /**
     * 24.1.1.1 AllocateArrayBuffer (constructor)
     * 
     * @param cx
     *            the execution context
     * @return the new array buffer object
     */
    public static ArrayBufferObject AllocateArrayBuffer(ExecutionContext cx) {
        /* steps 1-4 */
        return AllocateArrayBuffer(cx, (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer));
    }

    /**
     * 24.1.1.1 AllocateArrayBuffer (constructor)
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @return the new array buffer object
     */
    public static ArrayBufferObject AllocateArrayBuffer(ExecutionContext cx, Object constructor) {
        // TODO: Moved check from GetPrototypeFromConstructor
        if (!IsConstructor(constructor)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        /* steps 1-4 */
        return AllocateArrayBuffer(cx, (Constructor) constructor);
    }

    /**
     * 24.1.1.1 AllocateArrayBuffer (constructor)
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @return the new array buffer object
     */
    public static ArrayBufferObject AllocateArrayBuffer(ExecutionContext cx, Constructor constructor) {
        /* steps 1-4 */
        return OrdinaryCreateFromConstructor(cx, constructor, Intrinsics.ArrayBufferPrototype,
                ArrayBufferObjectAllocator.INSTANCE);
    }

    /**
     * 24.1.1.2 IsDetachedBuffer( arrayBuffer )
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @return {@code true} if the array buffer is detached
     */
    public static boolean IsDetachedBuffer(ArrayBufferObject arrayBuffer) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        return arrayBuffer.isDetached();
    }

    /**
     * 24.1.1.3 DetachArrayBuffer( arrayBuffer )
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     */
    public static void DetachArrayBuffer(ExecutionContext cx, ArrayBufferObject arrayBuffer) {
        // TODO: Perform any checks here? E.g. initialized or already detached?
        /* step 1 (not applicable) */
        /* steps 2-3 */
        arrayBuffer.detach();
        /* step 4 (return) */
    }

    /**
     * 24.1.1.4 SetArrayBufferData (arrayBuffer, bytes)
     * 
     * @param cx
     *            the execution context
     * @param arrayBuffer
     *            the array buffer object
     * @param bytes
     *            the buffer size in bytes
     * @return the array buffer object
     */
    public static ArrayBufferObject SetArrayBufferData(ExecutionContext cx,
            ArrayBufferObject arrayBuffer, long bytes) {
        /* step 1 (not applicable) */
        /* step 2 (implicit) */
        /* step 3 */
        assert bytes >= 0;
        /* steps 4-5 */
        ByteBuffer block = CreateByteDataBlock(cx, bytes);
        /* step 6 */
        arrayBuffer.setData(block);
        /* step 7 */
        arrayBuffer.setByteLength(bytes);
        /* step 8 */
        return arrayBuffer;
    }

    /**
     * 24.1.1.5 CloneArrayBuffer (srcBuffer, srcByteOffset)
     * 
     * @param cx
     *            the execution context
     * @param srcBuffer
     *            the source buffer
     * @param srcByteOffset
     *            the source offset
     * @return the new array buffer object
     */
    public static ArrayBufferObject CloneArrayBuffer(ExecutionContext cx,
            ArrayBufferObject srcBuffer, long srcByteOffset) {
        /* step 1 (implicit) */
        /* step 2 */
        ByteBuffer srcBlock = srcBuffer.getData();
        /* step 3 */
        if (!srcBuffer.isInitialized()) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 4 */
        if (IsDetachedBuffer(srcBuffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* steps 5-6 */
        Constructor bufferConstructor = SpeciesConstructor(cx, srcBuffer, Intrinsics.ArrayBuffer);
        /* step 7 */
        ArrayBufferObject targetBuffer = AllocateArrayBuffer(cx, bufferConstructor);
        /* steps 8-9 */
        if (IsDetachedBuffer(srcBuffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 10 */
        long srcLength = srcBuffer.getByteLength();
        /* step 11 */
        assert srcByteOffset <= srcLength;
        /* step 12 */
        long cloneLength = srcLength - srcByteOffset;
        /* step 13 */
        assert srcBlock == srcBuffer.getData();
        /* steps 14-15 */
        SetArrayBufferData(cx, targetBuffer, cloneLength);
        /* step 16 */
        ByteBuffer targetBlock = targetBuffer.getData();
        /* step 17 */
        CopyDataBlockBytes(targetBlock, 0, srcBlock, srcByteOffset, cloneLength);
        /* step 18 */
        return targetBuffer;
    }

    /**
     * 24.1.1.6 GetValueFromBuffer (arrayBuffer, byteIndex, type, isLittleEndian)
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @param byteIndex
     *            the byte index
     * @param type
     *            the element type
     * @return the buffer value
     */
    public static double GetValueFromBuffer(ArrayBufferObject arrayBuffer, long byteIndex,
            ElementType type) {
        return GetValueFromBuffer(arrayBuffer, byteIndex, type, IS_LITTLE_ENDIAN);
    }

    /**
     * 24.1.1.6 GetValueFromBuffer (arrayBuffer, byteIndex, type, isLittleEndian)
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @param byteIndex
     *            the byte index
     * @param type
     *            the element type
     * @param isLittleEndian
     *            the little endian flag
     * @return the buffer value
     */
    public static double GetValueFromBuffer(ArrayBufferObject arrayBuffer, long byteIndex,
            ElementType type, boolean isLittleEndian) {
        /* step 1 */
        assert arrayBuffer.isInitialized() : "ArrayBuffer not initialized";
        /* step 2 */
        assert !IsDetachedBuffer(arrayBuffer) : "ArrayBuffer is detached";
        /* steps 3-4 */
        assert (byteIndex >= 0 && (byteIndex + type.size()) <= arrayBuffer.getByteLength());
        /* step 5 */
        ByteBuffer block = arrayBuffer.getData();
        /* steps 8-9 */
        if ((block.order() == ByteOrder.LITTLE_ENDIAN) != isLittleEndian) {
            block.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        }

        int index = (int) byteIndex;
        switch (type) {
        case Float32: {
            /* steps 6-7, 10 */
            double rawValue = block.getFloat(index);
            return Double.isNaN(rawValue) ? Double.NaN : rawValue;
        }
        case Float64: {
            /* steps 6-7, 11 */
            double rawValue = block.getDouble(index);
            return Double.isNaN(rawValue) ? Double.NaN : rawValue;
        }

        /* steps 6-7, 12, 14 */
        case Uint8:
        case Uint8C:
            return block.get(index) & 0xffL;
        case Uint16:
            return block.getShort(index) & 0xffffL;
        case Uint32:
            return block.getInt(index) & 0xffffffffL;

            /* steps 6-7, 13-14 */
        case Int8:
            return (long) block.get(index);
        case Int16:
            return (long) block.getShort(index);
        case Int32:
            return (long) block.getInt(index);

        default:
            throw new AssertionError();
        }
    }

    /**
     * 24.1.1.7 SetValueInBuffer (arrayBuffer, byteIndex, type, value, isLittleEndian)
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @param byteIndex
     *            the byte index
     * @param type
     *            the element type
     * @param value
     *            the new element value
     */
    public static void SetValueInBuffer(ArrayBufferObject arrayBuffer, long byteIndex,
            ElementType type, double value) {
        SetValueInBuffer(arrayBuffer, byteIndex, type, value, IS_LITTLE_ENDIAN);
    }

    /**
     * 24.1.1.7 SetValueInBuffer (arrayBuffer, byteIndex, type, value, isLittleEndian)
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @param byteIndex
     *            the byte index
     * @param type
     *            the element type
     * @param value
     *            the new element value
     * @param isLittleEndian
     *            the little endian flag
     */
    public static void SetValueInBuffer(ArrayBufferObject arrayBuffer, long byteIndex,
            ElementType type, double value, boolean isLittleEndian) {
        /* step 1 */
        assert arrayBuffer.isInitialized() : "ArrayBuffer not initialized";
        /* step 2 */
        assert !IsDetachedBuffer(arrayBuffer) : "ArrayBuffer is detached";
        /* steps 3-4 */
        assert (byteIndex >= 0 && (byteIndex + type.size()) <= arrayBuffer.getByteLength());
        /* step 5 (not applicable) */
        /* step 6 */
        ByteBuffer block = arrayBuffer.getData();
        /* step 7 */
        assert block != null;
        /* step 9 */
        if ((block.order() == ByteOrder.LITTLE_ENDIAN) != isLittleEndian) {
            block.order(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        }

        int index = (int) byteIndex;
        switch (type) {
        case Float32:
            /* steps 6, 10, 13-14 */
            block.putFloat(index, (float) value);
            return;
        case Float64:
            /* steps 6, 11, 13-14 */
            block.putDouble(index, value);
            return;

            /* steps 6, 12-14 */
        case Int8:
            block.put(index, ElementType.ToInt8(value));
            return;
        case Uint8:
            block.put(index, ElementType.ToUint8(value));
            return;
        case Uint8C:
            block.put(index, ElementType.ToUint8Clamp(value));
            return;

        case Int16:
            block.putShort(index, ElementType.ToInt16(value));
            return;
        case Uint16:
            block.putShort(index, ElementType.ToUint16(value));
            return;

        case Int32:
            block.putInt(index, ElementType.ToInt32(value));
            return;
        case Uint32:
            block.putInt(index, ElementType.ToUint32(value));
            return;

        default:
            throw new AssertionError();
        }
    }

    /**
     * 24.1.2.1 ArrayBuffer(length)
     */
    @Override
    public ArrayBufferObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object length = argument(args, 0);
        /* step 1 (omitted) */
        /* step 2 */
        if (!(thisValue instanceof ArrayBufferObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        ArrayBufferObject buf = (ArrayBufferObject) thisValue;
        if (buf.isInitialized()) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        // FIXME: spec issue? - undefined length is same as 0 for bwcompat?
        if (Type.isUndefined(length)) {
            length = 0;
        }
        /* step 3 */
        double numberLength = ToNumber(calleeContext, length);
        /* steps 4-5 */
        long byteLength = ToLength(numberLength);
        /* step 6 */
        if (!SameValueZero(numberLength, byteLength)) {
            throw newRangeError(calleeContext, Messages.Key.InvalidBufferSize);
        }
        /* step 7 */
        if (buf.isInitialized()) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* step 8 */
        return SetArrayBufferData(calleeContext, buf, byteLength);
    }

    /**
     * 24.1.2.2 new ArrayBuffer(...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    @Override
    public CreateAction<ArrayBufferObject> createAction() {
        return ArrayBufferCreate.INSTANCE;
    }

    /**
     * 24.1.3 Properties of the ArrayBuffer Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "ArrayBuffer";

        /**
         * 24.1.3.2 ArrayBuffer.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ArrayBufferPrototype;

        /**
         * 24.1.3.1 ArrayBuffer.isView ( arg )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param arg
         *            the argument object
         * @return {@code true} if the argument is an array buffer view object
         */
        @Function(name = "isView", arity = 1)
        public static Object isView(ExecutionContext cx, Object thisValue, Object arg) {
            /* steps 1-3 */
            return arg instanceof ArrayBufferView;
        }

        /**
         * 24.1.3.3 get ArrayBuffer [ @@species ]
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
}
