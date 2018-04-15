/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToIndex;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Bytes;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntType;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
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
public final class ArrayBufferConstructor extends BuiltinConstructor implements Initializable {
    private static final boolean RANDOM_NAN_PAYLOAD_ON_SET = false;
    private static final boolean MODIFIED_NAN_PAYLOAD_ON_SET = false;
    private static final boolean IS_LITTLE_ENDIAN = Bytes.DEFAULT_BYTE_ORDER == ByteOrder.LITTLE_ENDIAN;
    private static final int DIRECT_LIMIT = 10 * 1024;

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
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, AdditionalProperties.class);
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
            ByteBuffer buffer;
            if (size < DIRECT_LIMIT) {
                buffer = ByteBuffer.allocate((int) size);
            } else {
                buffer = ByteBuffer.allocateDirect((int) size);
            }
            return buffer.order(Bytes.DEFAULT_BYTE_ORDER);
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
    public static void CopyDataBlockBytes(ByteBuffer toBlock, long toIndex, ByteBuffer fromBlock, long fromIndex,
            long count) {
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
     * 24.1.1.1 AllocateArrayBuffer( constructor, byteLength )
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @param byteLength
     *            the buffer byte length
     * @return the new array buffer object
     */
    public static ArrayBufferObject AllocateArrayBuffer(ExecutionContext cx, Constructor constructor, long byteLength) {
        /* step 1 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, Intrinsics.ArrayBufferPrototype);
        /* step 2 */
        assert byteLength >= 0;
        /* step 3 */
        ByteBuffer block = CreateByteDataBlock(cx, byteLength);
        /* steps 1, 4-6 */
        return new ArrayBufferObject(cx.getRealm(), block, byteLength, proto);
    }

    /**
     * 24.1.1.2 IsDetachedBuffer( arrayBuffer )
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @return {@code true} if the array buffer is detached
     */
    public static boolean IsDetachedBuffer(ArrayBuffer arrayBuffer) {
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
    public static void DetachArrayBuffer(ExecutionContext cx, ArrayBuffer arrayBuffer) {
        // TODO: Perform any checks here? E.g. already detached?
        /* step 1 (not applicable) */
        /* steps 2-3 */
        arrayBuffer.detach();
        /* step 4 (return) */
    }

    /**
     * 24.1.1.4 CloneArrayBuffer ( srcBuffer, srcByteOffset, srcLength, cloneConstructor )
     * 
     * @param cx
     *            the execution context
     * @param srcBuffer
     *            the source buffer
     * @param srcByteOffset
     *            the source offset
     * @param srcLength
     *            the source length
     * @param cloneConstructor
     *            the constructor function
     * @return the new array buffer object
     */
    public static ArrayBufferObject CloneArrayBuffer(ExecutionContext cx, ArrayBuffer srcBuffer, long srcByteOffset,
            long srcLength, Constructor cloneConstructor) {
        /* step 1 (implicit) */
        /* step 2 (implicit) */
        /* step 3 */
        ArrayBufferObject targetBuffer = AllocateArrayBuffer(cx, cloneConstructor, srcLength);
        /* step 4 */
        if (IsDetachedBuffer(srcBuffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 5 */
        ByteBuffer srcBlock = srcBuffer.getData();
        /* step 7 */
        ByteBuffer targetBlock = targetBuffer.getData();
        /* step 8 */
        CopyDataBlockBytes(targetBlock, 0, srcBlock, srcByteOffset, srcLength);
        /* step 9 */
        return targetBuffer;
    }

    /**
     * 24.1.1.5 GetValueFromBuffer (arrayBuffer, byteIndex, type, isLittleEndian)
     * 
     * @param arrayBuffer
     *            the array buffer object
     * @param byteIndex
     *            the byte index
     * @param type
     *            the element type
     * @return the buffer value
     */
    public static Number GetValueFromBuffer(ArrayBuffer arrayBuffer, long byteIndex, ElementType type) {
        return GetValueFromBuffer(arrayBuffer, byteIndex, type, IS_LITTLE_ENDIAN);
    }

    /**
     * 24.1.1.5 GetValueFromBuffer (arrayBuffer, byteIndex, type, isLittleEndian)
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
    public static Number GetValueFromBuffer(ArrayBuffer arrayBuffer, long byteIndex, ElementType type,
            boolean isLittleEndian) {
        /* step 1 */
        assert !IsDetachedBuffer(arrayBuffer) : "ArrayBuffer is detached";
        /* steps 2-3 */
        assert byteIndex >= 0 && (byteIndex + type.size() <= arrayBuffer.getByteLength());
        /* steps 4, 7-8 */
        ByteBuffer block = arrayBuffer.getData(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        int index = (int) byteIndex;
        switch (type) {
        /* steps 5-6, 9 */
        case Float32: {
            double rawValue = block.getFloat(index);
            return Double.isNaN(rawValue) ? Double.NaN : rawValue;
        }
        /* steps 5-6, 10 */
        case Float64: {
            double rawValue = block.getDouble(index);
            return Double.isNaN(rawValue) ? Double.NaN : rawValue;
        }
        /* steps 5-6, 11-12 */
        case Uint8:
        case Uint8C:
            return (int) (block.get(index) & 0xff);
        case Uint16:
            return (int) (block.getShort(index) & 0xffff);
        case Uint32:
            return (long) (block.getInt(index) & 0xffff_ffffL);
        case Int8:
            return (int) block.get(index);
        case Int16:
            return (int) block.getShort(index);
        case Int32:
            return (int) block.getInt(index);
        case BigInt64:
            return BigInteger.valueOf(block.getLong(index));
        case BigUint64:
            return BigIntType.toUnsigned64(block.getLong(index));
        default:
            throw new AssertionError();
        }
    }

    /**
     * 24.1.1.6 SetValueInBuffer (arrayBuffer, byteIndex, type, value, isLittleEndian)
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
    public static void SetValueInBuffer(ArrayBuffer arrayBuffer, long byteIndex, ElementType type, Number value) {
        SetValueInBuffer(arrayBuffer, byteIndex, type, value, IS_LITTLE_ENDIAN);
    }

    /**
     * 24.1.1.6 SetValueInBuffer (arrayBuffer, byteIndex, type, value, isLittleEndian)
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
    public static void SetValueInBuffer(ArrayBuffer arrayBuffer, long byteIndex, ElementType type, Number value,
            boolean isLittleEndian) {
        /* step 1 */
        assert !IsDetachedBuffer(arrayBuffer) : "ArrayBuffer is detached";
        /* steps 2-3 */
        assert byteIndex >= 0 && (byteIndex + type.size() <= arrayBuffer.getByteLength());
        /* step 4 (not applicable) */
        /* steps 5-7 */
        ByteBuffer block = arrayBuffer.getData(isLittleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        // Extension: BigInt
        assert type.isCompatibleNumericValue(value);

        int index = (int) byteIndex;
        switch (type) {
        /* steps 8, 11-12 */
        case Float32: {
            double numValue = value.doubleValue();
            if (RANDOM_NAN_PAYLOAD_ON_SET) {
                if (Double.isNaN(numValue)) {
                    // 51 bits random payload data.
                    long payload = Double.doubleToRawLongBits(Math.random()) & 0x7FFFFFFFFFFFFL;
                    numValue = Double
                            .longBitsToDouble((Double.doubleToRawLongBits(numValue) & 0xFFF8000000000000L) | payload);
                }
            }
            if (MODIFIED_NAN_PAYLOAD_ON_SET) {
                if (Double.isNaN(numValue)) {
                    // 51 bits modified payload data.
                    long payload = ~(Double.doubleToRawLongBits(numValue) & 0x7FFFFFFFFFFFFL) | 0xFFFFL;
                    numValue = Double
                            .longBitsToDouble((Double.doubleToRawLongBits(numValue) & 0xFFF8000000000000L) | payload);
                }
            }
            block.putFloat(index, (float) numValue);
            return;
        }
        /* steps 9, 11-12 */
        case Float64: {
            double numValue = value.doubleValue();
            if (RANDOM_NAN_PAYLOAD_ON_SET) {
                if (Double.isNaN(numValue)) {
                    // 51 bits random payload data.
                    long payload = Double.doubleToRawLongBits(Math.random()) & 0x7FFFFFFFFFFFFL;
                    numValue = Double
                            .longBitsToDouble((Double.doubleToRawLongBits(numValue) & 0xFFF8000000000000L) | payload);
                }
            }
            if (MODIFIED_NAN_PAYLOAD_ON_SET) {
                if (Double.isNaN(numValue)) {
                    // 51 bits modified payload data.
                    long payload = ~(Double.doubleToRawLongBits(numValue) & 0x7FFFFFFFFFFFFL) | 0xFFFFL;
                    numValue = Double
                            .longBitsToDouble((Double.doubleToRawLongBits(numValue) & 0xFFF8000000000000L) | payload);
                }
            }
            block.putDouble(index, numValue);
            return;
        }
        /* steps 10-12 */
        case Int8:
            block.put(index, ElementType.ToInt8(value.doubleValue()));
            return;
        case Uint8:
            block.put(index, ElementType.ToUint8(value.doubleValue()));
            return;
        case Uint8C:
            block.put(index, ElementType.ToUint8Clamp(value.doubleValue()));
            return;
        case Int16:
            block.putShort(index, ElementType.ToInt16(value.doubleValue()));
            return;
        case Uint16:
            block.putShort(index, ElementType.ToUint16(value.doubleValue()));
            return;
        case Int32:
            block.putInt(index, ElementType.ToInt32(value.doubleValue()));
            return;
        case Uint32:
            block.putInt(index, ElementType.ToUint32(value.doubleValue()));
            return;
        case BigInt64:
            block.putLong(index, ElementType.ToBigInt64((BigInteger) value));
            return;
        case BigUint64:
            block.putLong(index, ElementType.ToBigUint64((BigInteger) value));
            return;
        default:
            throw new AssertionError();
        }
    }

    /**
     * 24.1.2.1 ArrayBuffer ( [ length ] )
     */
    @Override
    public ArrayBufferObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "ArrayBuffer");
    }

    /**
     * 24.1.2.1 ArrayBuffer ( [ length ] )
     */
    @Override
    public ArrayBufferObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object length = argument(args, 0);
        /* step 1 (not applicable) */
        /* step 2 */
        long byteLength = ToIndex(calleeContext, length);
        /* step 3 */
        return AllocateArrayBuffer(calleeContext, newTarget, byteLength);
    }

    /**
     * 24.1.3 Properties of the ArrayBuffer Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "ArrayBuffer";

        /**
         * 24.1.3.2 ArrayBuffer.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
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
        @Accessor(name = "get [Symbol.species]", symbol = BuiltinSymbol.species, type = Accessor.Type.Getter)
        public static Object species(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            return thisValue;
        }
    }

    /**
     * Proposed ECMAScript 7 additions
     */
    @CompatibilityExtension(CompatibilityOption.ArrayBufferTransfer)
    public enum AdditionalProperties {
        ;

        private static ArrayBufferObject thisArrayBufferObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof ArrayBufferObject) {
                ArrayBufferObject buffer = (ArrayBufferObject) value;
                if (IsDetachedBuffer(buffer)) {
                    throw newTypeError(cx, Messages.Key.BufferDetached);
                }
                return buffer;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        /**
         * ArrayBuffer.transfer(oldBuffer [, newByteLength])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param oldBuffer
         *            the old array buffer
         * @param newByteLength
         *            the optional new byte length
         * @return the result index
         */
        @Function(name = "transfer", arity = 1)
        public static Object transfer(ExecutionContext cx, Object thisValue, Object oldBuffer, Object newByteLength) {
            ArrayBufferObject oldArrayBuffer = thisArrayBufferObject(cx, oldBuffer, "ArrayBuffer.transfer");
            long byteLength;
            if (!Type.isUndefined(newByteLength)) {
                // Perform the same length conversion as in new ArrayBuffer(length).
                byteLength = ToIndex(cx, newByteLength);
            } else {
                // newByteLength defaults to oldBuffer.byteLength
                byteLength = oldArrayBuffer.getByteLength();
            }
            Constructor ctor;
            if (IsConstructor(thisValue)) {
                ctor = (Constructor) thisValue;
            } else {
                ctor = (Constructor) cx.getIntrinsic(Intrinsics.ArrayBuffer);
            }
            ScriptObject proto = GetPrototypeFromConstructor(cx, ctor, Intrinsics.ArrayBufferPrototype);
            if (IsDetachedBuffer(oldArrayBuffer)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            // Grab old array buffer data and call detach.
            ByteBuffer oldData = oldArrayBuffer.getData();
            long oldByteLength = oldArrayBuffer.getByteLength();
            DetachArrayBuffer(cx, oldArrayBuffer);
            // Extend byte buffer.
            if (byteLength > oldByteLength) {
                ByteBuffer newData = CreateByteDataBlock(cx, byteLength);
                CopyDataBlockBytes(newData, 0, oldData, 0, oldByteLength);
                return new ArrayBufferObject(cx.getRealm(), newData, byteLength, proto);
            }
            // Truncate byte buffer.
            if (byteLength < oldByteLength) {
                // Possible improvement: Use limit() or slice() to reduce buffer allocations.
                ByteBuffer newData = CreateByteDataBlock(cx, byteLength);
                CopyDataBlockBytes(newData, 0, oldData, 0, byteLength);
                return new ArrayBufferObject(cx.getRealm(), newData, byteLength, proto);
            }
            // Create new array buffer with the same byte buffer if length did not change.
            return new ArrayBufferObject(cx.getRealm(), oldData, byteLength, proto);
        }
    }
}
