/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.bigint.BigIntAbstractOperations.ToBigInt;
import static com.github.anba.es6draft.runtime.objects.bigint.BigIntAbstractOperations.ToBigInt64;
import static com.github.anba.es6draft.runtime.objects.bigint.BigIntAbstractOperations.ToBigUint64;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Futex;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.bigint.BigIntType;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.objects.binary.ElementType;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.4 The Atomics Object</h2>
 * <ul>
 * <li>24.4.1 Abstract Operations for Atomics
 * <li>Function Properties of the Atomics Object
 * </ul>
 */
public final class AtomicsObject extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Atomics object.
     * 
     * @param realm
     *            the realm object
     */
    public AtomicsObject(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, FenceFunction.class);
    }

    /**
     * 24.4.1.1 ValidateSharedIntegerTypedArray(typedArray [ , onlyInt32 ] )
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @param method
     *            the method name
     * @return the typed array object
     */
    public static TypedArrayObject ValidateSharedIntegerTypedArray(ExecutionContext cx, Object typedArray,
            String method) {
        return ValidateSharedIntegerTypedArray(cx, typedArray, false, method);
    }

    /**
     * 24.4.1.1 ValidateSharedIntegerTypedArray(typedArray [ , onlyInt32 ] )
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @param onlyInt32
     *            if {@code true} only Int32 typed arrays are accepted
     * @param method
     *            the method name
     * @return the typed array object
     */
    public static TypedArrayObject ValidateSharedIntegerTypedArray(ExecutionContext cx, Object typedArray,
            boolean onlyInt32, String method) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        if (!(typedArray instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleArgument, method, Type.of(typedArray).toString());
        }
        TypedArrayObject array = (TypedArrayObject) typedArray;
        /* step 4 */
        ElementType typeName = array.getElementType();
        /* steps 5-6 */
        if (onlyInt32) {
            /* step 5 */
            if (typeName != ElementType.Int32 && typeName != ElementType.BigInt64) {
                throw newTypeError(cx, Messages.Key.AtomicsInt32ArrayType);
            }
        } else {
            /* step 6 */
            switch (typeName) {
            case Int8:
            case Uint8:
            case Int16:
            case Uint16:
            case Int32:
            case Uint32:
            case BigInt64:
            case BigUint64:
                break;
            default:
                throw newTypeError(cx, Messages.Key.AtomicsInvalidArrayType);
            }
        }
        /* steps 7-8 */
        ArrayBuffer buffer = array.getBuffer();
        /* step 9 */
        if (!(buffer instanceof SharedArrayBufferObject)) {
            throw newTypeError(cx, Messages.Key.AtomicsNotSharedBuffer);
        }
        /* step 10 */
        return array;
    }

    /**
     * 24.4.1.2 ValidateAtomicAccess( typedArray, requestIndex )
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @param requestIndex
     *            the property name
     * @return the typed array index
     */
    public static long ValidateAtomicAccess(ExecutionContext cx, TypedArrayObject typedArray, Object requestIndex) {
        /* step 1 (implicit) */
        /* step 2 */
        long accessIndex = ToIndex(cx, requestIndex);
        /* step 3 */
        long length = typedArray.getArrayLength();
        /* step 4 */
        assert accessIndex >= 0;
        /* step 5 */
        if (accessIndex >= length) {
            throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 6 */
        return accessIndex;
    }

    @FunctionalInterface
    private interface AtomicByteOperator {
        byte apply(ByteBuffer buffer, int index, byte value);
    }

    @FunctionalInterface
    private interface AtomicShortOperator {
        short apply(ByteBuffer buffer, int index, short value);
    }

    @FunctionalInterface
    private interface AtomicIntOperator {
        int apply(ByteBuffer buffer, int index, int value);
    }

    @FunctionalInterface
    private interface AtomicLongOperator {
        long apply(ByteBuffer buffer, int index, long value);
    }

    private static int toByteIndex(TypedArrayObject array, long index) {
        long indexedPosition = array.getElementType().toBytes(index) + array.getByteOffset();
        assert indexedPosition == (int) indexedPosition;
        return (int) indexedPosition;
    }

    /**
     * 24.4.1.11 AtomicReadModifyWrite( typedArray, index, value, op )
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @param index
     *            the typed array index
     * @param value
     *            the new value
     * @param byteOp
     *            the {@code byte} modify operation
     * @param shortOp
     *            the {@code short} modify operation
     * @param intOp
     *            the {@code int} modify operation
     * @param longOp
     *            the {@code long} modify operation
     * @param method
     *            the method name
     * @return the result value
     */
    public static Number AtomicReadModifyWrite(ExecutionContext cx, Object typedArray, Object index, Object value,
            AtomicByteOperator byteOp, AtomicShortOperator shortOp, AtomicIntOperator intOp, AtomicLongOperator longOp,
            String method) {
        /* step 1 */
        TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, method);
        /* step 2 */
        long i = ValidateAtomicAccess(cx, array, index);

        // Extension: BigInt
        if (array.getElementType().isInt64()) {
            /* step 3 */
            BigInteger v = ToBigInt(cx, value);
            /* steps 4-8 */
            int indexedPosition = toByteIndex(array, i);
            /* step 9 */
            return GetModifySetValueInBuffer(array, indexedPosition, v, longOp);
        }

        /* step 3 */
        double v = ToInteger(cx, value);
        /* steps 4-8 */
        int indexedPosition = toByteIndex(array, i);
        /* step 9 */
        return GetModifySetValueInBuffer(array, indexedPosition, v, byteOp, shortOp, intOp);
    }

    /**
     * 24.1.1.9 GetModifySetValueInBuffer( arrayBuffer, byteIndex, type, value, op [ , isLittleEndian ] )
     * 
     * @param array
     *            the typed array object
     * @param index
     *            the array buffer byte index
     * @param value
     *            the new value
     * @param byteOp
     *            the {@code byte} modify operation
     * @param shortOp
     *            the {@code short} modify operation
     * @param intOp
     *            the {@code int} modify operation
     * @return the result value
     */
    private static double GetModifySetValueInBuffer(TypedArrayObject array, int index, double value,
            AtomicByteOperator byteOp, AtomicShortOperator shortOp, AtomicIntOperator intOp) {
        /* steps 1-4 (omitted) */
        /* step 5 */
        ByteBuffer buffer = array.getBuffer().getData();
        /* steps 6-16 */
        switch (array.getElementType()) {
        case Int32:
            return intOp.apply(buffer, index, ToInt32(value));
        case Int16:
            return (int) shortOp.apply(buffer, index, ToInt16(value));
        case Int8:
            return (int) byteOp.apply(buffer, index, ToInt8(value));
        case Uint32:
            return 0xffff_ffffL & intOp.apply(buffer, index, ToInt32(value));
        case Uint16:
            return 0xffff & shortOp.apply(buffer, index, ToInt16(value));
        case Uint8:
            return 0xff & byteOp.apply(buffer, index, ToInt8(value));
        default:
            throw new AssertionError();
        }
    }

    private static BigInteger GetModifySetValueInBuffer(TypedArrayObject array, int index, BigInteger value,
            AtomicLongOperator longOp) {
        /* steps 1-4 (omitted) */
        /* step 5 */
        ByteBuffer buffer = array.getBuffer().getData();
        /* steps 6-16 */
        switch (array.getElementType()) {
        case BigInt64:
            return BigInteger.valueOf(longOp.apply(buffer, index, ToBigInt64(value)));
        case BigUint64:
            return BigIntType.toUnsigned64(longOp.apply(buffer, index, ToBigUint64(value)));
        default:
            throw new AssertionError();
        }
    }

    private static double compareExchange(TypedArrayObject array, int index, double expected, double replacement) {
        ByteBuffer buffer = array.getBuffer().getData();
        switch (array.getElementType()) {
        case Int32:
            return Atomics.compareAndExchange(buffer, index, ToInt32(expected), ToInt32(replacement));
        case Int16:
            return (int) Atomics.compareAndExchange(buffer, index, ToInt16(expected), ToInt16(replacement));
        case Int8:
            return (int) Atomics.compareAndExchange(buffer, index, ToInt8(expected), ToInt8(replacement));
        case Uint32:
            return 0xffff_ffffL & Atomics.compareAndExchange(buffer, index, ToInt32(expected), ToInt32(replacement));
        case Uint16:
            return 0xffff & Atomics.compareAndExchange(buffer, index, ToInt16(expected), ToInt16(replacement));
        case Uint8:
            return 0xff & Atomics.compareAndExchange(buffer, index, ToInt8(expected), ToInt8(replacement));
        default:
            throw new AssertionError();
        }
    }

    private static BigInteger compareExchange(TypedArrayObject array, int index, BigInteger expected,
            BigInteger replacement) {
        ByteBuffer buffer = array.getBuffer().getData();
        switch (array.getElementType()) {
        case BigInt64:
            return BigInteger
                    .valueOf(Atomics.compareAndExchange(buffer, index, ToBigInt64(expected), ToBigInt64(replacement)));
        case BigUint64:
            return BigIntType.toUnsigned64(
                    Atomics.compareAndExchange(buffer, index, ToBigUint64(expected), ToBigUint64(replacement)));
        default:
            throw new AssertionError();
        }
    }

    private static Number load(TypedArrayObject array, int index) {
        ByteBuffer buffer = array.getBuffer().getData();
        switch (array.getElementType()) {
        case BigInt64:
            return BigInteger.valueOf(Atomics.getLongVolatile(buffer, index));
        case Int32:
            return Atomics.getIntVolatile(buffer, index);
        case Int16:
            return (int) Atomics.getShortVolatile(buffer, index);
        case Int8:
            return (int) Atomics.getByteVolatile(buffer, index);
        case BigUint64:
            return BigIntType.toUnsigned64(Atomics.getLongVolatile(buffer, index));
        case Uint32:
            return 0xffff_ffffL & Atomics.getIntVolatile(buffer, index);
        case Uint16:
            return (int) (0xffff & Atomics.getShortVolatile(buffer, index));
        case Uint8:
            return (int) (0xff & Atomics.getByteVolatile(buffer, index));
        default:
            throw new AssertionError();
        }
    }

    private static void store(TypedArrayObject array, int index, double value) {
        ByteBuffer buffer = array.getBuffer().getData();
        switch (array.getElementType()) {
        case Int32:
        case Uint32:
            Atomics.setIntVolatile(buffer, index, ToInt32(value));
            break;
        case Int16:
        case Uint16:
            Atomics.setShortVolatile(buffer, index, ToInt16(value));
            break;
        case Int8:
        case Uint8:
            Atomics.setByteVolatile(buffer, index, ToInt8(value));
            break;
        default:
            throw new AssertionError();
        }
    }

    private static void store(TypedArrayObject array, int index, BigInteger value) {
        ByteBuffer buffer = array.getBuffer().getData();
        switch (array.getElementType()) {
        case BigInt64:
            Atomics.setLongVolatile(buffer, index, ToBigInt64(value));
            break;
        case BigUint64:
            Atomics.setLongVolatile(buffer, index, ToBigUint64(value));
            break;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Function Properties of the Atomics Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 24.4.2 Atomics.add( typedArray, index, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @return the previous typed array value
         */
        @Function(name = "add", arity = 3)
        public static Object add(ExecutionContext cx, Object thisValue, Object typedArray, Object index, Object value) {
            /* step 1 */
            return AtomicReadModifyWrite(cx, typedArray, index, value, Atomics::getAndAdd, Atomics::getAndAdd,
                    Atomics::getAndAdd, Atomics::getAndAdd, "Atomics.add");
        }

        /**
         * 24.4.3 Atomics.and( typedArray, index, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @return the previous typed array value
         */
        @Function(name = "and", arity = 3)
        public static Object and(ExecutionContext cx, Object thisValue, Object typedArray, Object index, Object value) {
            /* step 1 */
            return AtomicReadModifyWrite(cx, typedArray, index, value, Atomics::getAndBitwiseAnd,
                    Atomics::getAndBitwiseAnd, Atomics::getAndBitwiseAnd, Atomics::getAndBitwiseAnd, "Atomics.and");
        }

        /**
         * 24.4.4 Atomics.compareExchange( typedArray, index, expectedValue, replacementValue )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param expectedValue
         *            the expected array value
         * @param replacementValue
         *            the replacement array value
         * @return the previous typed array value
         */
        @Function(name = "compareExchange", arity = 4)
        public static Object compareExchange(ExecutionContext cx, Object thisValue, Object typedArray, Object index,
                Object expectedValue, Object replacementValue) {
            /* step 1 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, "Atomics.compareExchange");
            /* step 2 */
            long i = ValidateAtomicAccess(cx, array, index);

            // Extension: BigInt
            if (array.getElementType().isInt64()) {
                /* step 3 */
                BigInteger expected = ToBigInt(cx, expectedValue);
                /* step 4 */
                BigInteger replacement = ToBigInt(cx, replacementValue);
                /* steps 9-11 */
                int indexedPosition = toByteIndex(array, i);
                /* steps 5-8, 12-13 */
                return AtomicsObject.compareExchange(array, indexedPosition, expected, replacement);
            }

            /* step 3 */
            double expected = ToInteger(cx, expectedValue);
            /* step 4 */
            double replacement = ToInteger(cx, replacementValue);
            /* steps 9-11 */
            int indexedPosition = toByteIndex(array, i);
            /* steps 5-8, 12-13 */
            return AtomicsObject.compareExchange(array, indexedPosition, expected, replacement);
        }

        /**
         * 24.4.5 Atomics.exchange( typedArray, index, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @return the previous typed array value
         */
        @Function(name = "exchange", arity = 3)
        public static Object exchange(ExecutionContext cx, Object thisValue, Object typedArray, Object index,
                Object value) {
            /* step 1 */
            return AtomicReadModifyWrite(cx, typedArray, index, value, Atomics::getAndSet, Atomics::getAndSet,
                    Atomics::getAndSet, Atomics::getAndSet, "Atomics.exchange");
        }

        /**
         * 24.4.6 Atomics.isLockFree( size )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param size
         *            the size value
         * @return {@code true} if <var>size</var> bytes can be changed atomically without additional locking
         */
        @Function(name = "isLockFree", arity = 1)
        public static Object isLockFree(ExecutionContext cx, Object thisValue, Object size) {
            /* step 1 */
            double n = ToInteger(cx, size);
            /* steps 3-6 */
            // TODO(BigInt): spec issue - will lockfree be extended to eight bytes?
            return (n == 1 || n == 2 || n == 4);
        }

        /**
         * 24.4.7 Atomics.load( typedArray, index )
         * <p>
         * 24.4.1.12 AtomicLoad( typedArray, index )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @return the typed array value
         */
        @Function(name = "load", arity = 2)
        public static Object load(ExecutionContext cx, Object thisValue, Object typedArray, Object index) {
            /* step 1 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, "Atomics.load");
            /* step 2 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 3-7 */
            int indexedPosition = toByteIndex(array, i);
            /* step 8 */
            return AtomicsObject.load(array, indexedPosition);
        }

        /**
         * 24.4.8 Atomics.or( typedArray, index, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @return the previous typed array value
         */
        @Function(name = "or", arity = 3)
        public static Object or(ExecutionContext cx, Object thisValue, Object typedArray, Object index, Object value) {
            /* step 1 */
            return AtomicReadModifyWrite(cx, typedArray, index, value, Atomics::getAndBitwiseOr,
                    Atomics::getAndBitwiseOr, Atomics::getAndBitwiseOr, Atomics::getAndBitwiseOr, "Atomics.or");
        }

        /**
         * 24.4.9 Atomics.store( typedArray, index, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @return the new typed array value
         */
        @Function(name = "store", arity = 3)
        public static Object store(ExecutionContext cx, Object thisValue, Object typedArray, Object index,
                Object value) {
            /* step 1 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, "Atomics.store");
            /* step 2 */
            long i = ValidateAtomicAccess(cx, array, index);

            // Extension: BigInt
            if (array.getElementType().isInt64()) {
                /* step 3 */
                BigInteger v = ToBigInt(cx, value);
                /* steps 4-8 */
                int indexedPosition = toByteIndex(array, i);
                /* step 9 */
                AtomicsObject.store(array, indexedPosition, v);
                /* step 10 */
                return v;
            }

            /* step 3 */
            double v = ToInteger(cx, value);
            /* steps 4-8 */
            int indexedPosition = toByteIndex(array, i);
            /* step 9 */
            AtomicsObject.store(array, indexedPosition, v);
            /* step 10 */
            return v;
        }

        /**
         * 24.4.10 Atomics.sub( typedArray, index, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @return the previous typed array value
         */
        @Function(name = "sub", arity = 3)
        public static Object sub(ExecutionContext cx, Object thisValue, Object typedArray, Object index, Object value) {
            /* step 1 */
            return AtomicReadModifyWrite(cx, typedArray, index, value, Atomics::getAndSub, Atomics::getAndSub,
                    Atomics::getAndSub, Atomics::getAndSub, "Atomics.sub");
        }

        /**
         * 24.4.11 Atomics.wait( typedArray, index, value, timeout )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @param timeout
         *            the optional timeout value
         * @return the wait result state
         * @throws InterruptedException
         *             if interrupted while waiting
         */
        @Function(name = "wait", arity = 4)
        public static Object wait(ExecutionContext cx, Object thisValue, Object typedArray, Object index, Object value,
                Object timeout) throws InterruptedException {
            /* step 1 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, true, "Atomics.wait");
            /* step 2 */
            long i = ValidateAtomicAccess(cx, array, index);

            // Extension: BigInt
            if (array.getElementType().isInt64()) {
                /* step 3 */
                long v = ToBigInt64(ToBigInt64(cx, value));
                /* steps 4-5 */
                double q = ToNumber(cx, timeout);
                double t = Double.isNaN(q) ? Double.POSITIVE_INFINITY : Math.max(0, q);
                /* steps 6-7 */
                // AgentCanSuspend()
                /* step 8 */
                SharedByteBuffer bufferVal = ((SharedArrayBufferObject) array.getBuffer()).getSharedData();
                /* steps 9-10 */
                int indexedPosition = toByteIndex(array, i);
                /* steps 11-21 */
                Futex futex = cx.getRuntimeContext().getFutex();
                switch (futex.wait(bufferVal, indexedPosition, v, (long) t, TimeUnit.MILLISECONDS)) {
                case OK:
                    return "ok";
                case NotEqual:
                    return "not-equal";
                case Timedout:
                    return "timed-out";
                default:
                    throw new AssertionError();
                }
            }

            /* step 3 */
            int v = ToInt32(cx, value);
            /* steps 4-5 */
            double q = ToNumber(cx, timeout);
            double t = Double.isNaN(q) ? Double.POSITIVE_INFINITY : Math.max(0, q);
            /* steps 6-7 */
            // AgentCanSuspend()
            /* step 8 */
            SharedByteBuffer bufferVal = ((SharedArrayBufferObject) array.getBuffer()).getSharedData();
            /* steps 9-10 */
            int indexedPosition = toByteIndex(array, i);
            /* steps 11-21 */
            Futex futex = cx.getRuntimeContext().getFutex();
            switch (futex.wait(bufferVal, indexedPosition, v, (long) t, TimeUnit.MILLISECONDS)) {
            case OK:
                return "ok";
            case NotEqual:
                return "not-equal";
            case Timedout:
                return "timed-out";
            default:
                throw new AssertionError();
            }
        }

        /**
         * 24.4.12 Atomics.wake( typedArray, index, count )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param count
         *            the maximum number of workers to wake up
         * @return the number of woken up workers
         */
        @Function(name = "wake", arity = 3)
        public static Object wake(ExecutionContext cx, Object thisValue, Object typedArray, Object index,
                Object count) {
            /* step 1 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, true, "Atomics.wake");
            /* step 2 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 3-4 */
            int c;
            if (Type.isUndefined(count)) {
                c = Integer.MAX_VALUE;
            } else {
                c = (int) Math.max(0, ToInteger(cx, count));
            }
            /* step 5 */
            SharedByteBuffer bufferVal = ((SharedArrayBufferObject) array.getBuffer()).getSharedData();
            /* steps 6-7 */
            int indexedPosition = toByteIndex(array, i);
            /* steps 8-14 */
            Futex futex = cx.getRuntimeContext().getFutex();
            return futex.wake(bufferVal, indexedPosition, c);
        }

        /**
         * 24.4.13 Atomics.xor( typedArray, index, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index
         *            the typed array index
         * @param value
         *            the new typed array value
         * @return the previous typed array value
         */
        @Function(name = "xor", arity = 3)
        public static Object xor(ExecutionContext cx, Object thisValue, Object typedArray, Object index, Object value) {
            /* step 1 */
            return AtomicReadModifyWrite(cx, typedArray, index, value, Atomics::getAndBitwiseXor,
                    Atomics::getAndBitwiseXor, Atomics::getAndBitwiseXor, Atomics::getAndBitwiseXor, "Atomics.xor");
        }

        /**
         * 24.4.14 Atomics [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Atomics";
    }

    @CompatibilityExtension(CompatibilityOption.AtomicsFence)
    public enum FenceFunction {
        ;

        /**
         * Atomics.fence( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the undefined value
         */
        @Function(name = "fence", arity = 0)
        public static Object fence(ExecutionContext cx, Object thisValue) {
            Atomics.fullFence();
            return UNDEFINED;
        }
    }
}
