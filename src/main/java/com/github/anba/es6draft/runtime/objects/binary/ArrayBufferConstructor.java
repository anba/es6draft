/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.throwInternalError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.5 ArrayBuffer Objects</h3>
 * <ul>
 * <li>15.13.5.1 Abstract Operations For ArrayBuffer Objects
 * <li>15.13.5.2 The ArrayBuffer Object Called as a Function
 * <li>15.13.5.3 The ArrayBuffer Constructor
 * <li>15.13.5.4 Properties of the ArrayBuffer Constructor
 * </ul>
 */
public class ArrayBufferConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public ArrayBufferConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    private static class ArrayBufferObjectAllocator implements ObjectAllocator<ArrayBufferObject> {
        static final ObjectAllocator<ArrayBufferObject> INSTANCE = new ArrayBufferObjectAllocator();

        @Override
        public ArrayBufferObject newInstance(Realm realm) {
            return new ArrayBufferObject(realm);
        }
    }

    /**
     * FIXME: spec bug (function CreateByteArrayBlock not defined)
     */
    public static ByteBuffer CreateByteArrayBlock(ExecutionContext cx, double bytes) {
        if (bytes > Integer.MAX_VALUE) {
            throwInternalError(cx, Messages.Key.OutOfMemory);
        }
        try {
            // default byte-order is little-endian
            return ByteBuffer.allocate((int) bytes).order(ByteOrder.LITTLE_ENDIAN);
        } catch (OutOfMemoryError e) {
            throw throwInternalError(cx, Messages.Key.OutOfMemoryVM);
        }
    }

    /**
     * FIXME: spec bug (function CopyBlockElements not defined)
     */
    public static void CopyBlockElements(ByteBuffer fromBuf, double fromPos, ByteBuffer toBuf,
            double toPos, double length) {
        assert length >= 0;
        assert fromPos >= 0 && fromPos + length <= fromBuf.capacity();
        assert toPos >= 0 && toPos + length <= toBuf.capacity();

        fromBuf.limit((int) (fromPos + length));
        fromBuf.position((int) fromPos);
        toBuf.limit((int) (toPos + length));
        toBuf.position((int) toPos);
        toBuf.put(fromBuf);
    }

    /**
     * FIXME: spec bug (not defined in spec)
     */
    public static ArrayBufferObject CloneArrayBuffer(ExecutionContext cx,
            ArrayBufferObject srcData, ElementKind srcType, ElementKind destType, long length) {
        ArrayBufferObject destData = AllocateArrayBuffer(cx,
                cx.getIntrinsic(Intrinsics.ArrayBuffer));
        SetArrayBufferData(cx, destData, length * destType.size());

        for (long index = 0; index < length; ++index) {
            double value = GetValueFromBuffer(srcData, index * srcType.size(), srcType, false);
            SetValueInBuffer(destData, index * destType.size(), destType, value, false);
        }

        return destData;
    }

    /**
     * 15.13.5.1.1 AllocateArrayBuffer(constructor)
     */
    public static ArrayBufferObject AllocateArrayBuffer(ExecutionContext cx, Object constructor) {
        /* step 1-2 */
        ArrayBufferObject obj = OrdinaryCreateFromConstructor(cx, constructor,
                Intrinsics.ArrayBufferPrototype, ArrayBufferObjectAllocator.INSTANCE);
        /* step 3 */
        obj.setByteLength(0);
        /* step 4 */
        return obj;
    }

    /**
     * 15.13.5.1.2 (arrayBuffer, bytes)
     */
    public static ArrayBufferObject SetArrayBufferData(ExecutionContext cx,
            ArrayBufferObject arrayBuffer, double bytes) {
        /* step 1 (implicit) */
        /* step 2 */
        assert bytes >= 0;
        /* step 3-4 */
        ByteBuffer block = CreateByteArrayBlock(cx, bytes);
        /* step 5 */
        arrayBuffer.setData(block);
        /* step 6 */
        arrayBuffer.setByteLength((long) bytes);
        /* step 7 */
        return arrayBuffer;
    }

    /**
     * 15.13.5.1.3 GetValueFromBuffer (arrayBuffer, byteIndex, type, isBigEndian)
     */
    public static double GetValueFromBuffer(ArrayBufferObject arrayBuffer, long byteIndex,
            ElementKind type, boolean isBigEndian) {
        assert (byteIndex >= 0 && (byteIndex + type.size()) <= arrayBuffer.getByteLength());
        ByteBuffer block = arrayBuffer.getData();
        if ((block.order() == ByteOrder.BIG_ENDIAN) != isBigEndian) {
            block.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        }

        int index = (int) byteIndex;
        switch (type) {
        case Float32: {
            double rawValue = block.getFloat(index);
            return Double.isNaN(rawValue) ? Double.NaN : rawValue;
        }
        case Float64: {
            double rawValue = block.getDouble(index);
            return Double.isNaN(rawValue) ? Double.NaN : rawValue;
        }

        case Uint8:
        case Uint8C:
            return block.get(index) & 0xffL;
        case Uint16:
            return block.getShort(index) & 0xffffL;
        case Uint32:
            return block.getInt(index) & 0xffffffffL;

        case Int8:
            return (long) block.get(index);
        case Int16:
            return (long) block.getShort(index);
        case Int32:
            return (long) block.getInt(index);

        default:
            throw new IllegalStateException();
        }
    }

    /**
     * 15.13.5.1.4 SetValueInBuffer (arrayBuffer, byteIndex, type, value, isBigEndian)
     */
    public static void SetValueInBuffer(ArrayBufferObject arrayBuffer, long byteIndex,
            ElementKind type, double value, boolean isBigEndian) {
        assert (byteIndex >= 0 && (byteIndex + type.size()) <= arrayBuffer.getByteLength());
        ByteBuffer block = arrayBuffer.getData();
        if ((block.order() == ByteOrder.BIG_ENDIAN) != isBigEndian) {
            block.order(isBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        }

        int index = (int) byteIndex;
        switch (type) {
        case Float32:
            block.putFloat(index, (float) value);
            return;
        case Float64:
            block.putDouble(index, value);
            return;

        case Int8:
            block.put(index, ElementKind.ToInt8(value));
            return;
        case Uint8:
            block.put(index, ElementKind.ToUint8(value));
            return;
        case Uint8C:
            block.put(index, ElementKind.ToUint8Clamp(value));
            return;

        case Int16:
            block.putShort(index, ElementKind.ToInt16(value));
            return;
        case Uint16:
            block.putShort(index, ElementKind.ToUint16(value));
            return;

        case Int32:
            block.putInt(index, ElementKind.ToInt32(value));
            return;
        case Uint32:
            block.putInt(index, ElementKind.ToUint32(value));
            return;

        default:
            throw new IllegalStateException();
        }
    }

    /**
     * 15.13.5.2.1 ArrayBuffer(length)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = realm().defaultContext();
        Object length = args.length > 0 ? args[0] : UNDEFINED;
        if (!(Type.isUndefined(thisValue) || Type.isObject(thisValue))) {
            throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        if (Type.isUndefined(thisValue) || !(thisValue instanceof ArrayBufferObject)) {
            return OrdinaryConstruct(calleeContext, this, args);
        }
        ArrayBufferObject buf = (ArrayBufferObject) thisValue;
        if (buf.getData() != null) {
            throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        // FIXME: spec issue? - undefined length is same as 0 for bwcompat?
        if (Type.isUndefined(length)) {
            length = 0;
        }
        double numberLength = ToNumber(calleeContext, length);
        double byteLength = ToInteger(numberLength);
        if (numberLength != byteLength || byteLength < 0) {
            throwRangeError(calleeContext, Messages.Key.InvalidBufferSize);
        }
        return SetArrayBufferData(calleeContext, buf, (long) byteLength);
    }

    /**
     * 15.13.5.3.1 new ArrayBuffer( ...args )
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.13.5.4 Properties of the ArrayBuffer Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "ArrayBuffer";

        /**
         * 15.13.5.4.1 ArrayBuffer.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.ArrayBufferPrototype;

        /**
         * 15.13.5.4.2 ArrayBuffer.isView ( arg )
         */
        @Function(name = "isView", arity = 1)
        public static Object isView(ExecutionContext cx, Object thisValue, Object arg) {
            if (!Type.isObject(arg)) {
                return false;
            }
            if (arg instanceof ExoticArray) {
                return true;
            }
            if (arg instanceof TypedArrayObject) {
                return true;
            }
            return false;
        }

        /**
         * 15.13.5.4.3 @@create ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return AllocateArrayBuffer(cx, thisValue);
        }
    }
}
