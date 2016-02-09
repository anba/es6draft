/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.IntBinaryOperator;

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
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.objects.binary.ElementType;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Shared Memory and Atomics</h1><br>
 * <h2>The Atomics Object</h2>
 * <ul>
 * <li>Value Properties of the Atomics Object
 * <li>Runtime semantics
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
     * Runtime semantics: ValidateSharedIntegerTypedArray( typedArray [, onlyInt32] )
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @return the typed array object
     */
    public static TypedArrayObject ValidateSharedIntegerTypedArray(ExecutionContext cx, Object typedArray) {
        return ValidateSharedIntegerTypedArray(cx, typedArray, false);
    }

    /**
     * Runtime semantics: ValidateSharedIntegerTypedArray( typedArray [, onlyInt32] )
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @param onlyInt32
     *            if {@code true} only Int32 typed arrays are accepted
     * @return the typed array object
     */
    public static TypedArrayObject ValidateSharedIntegerTypedArray(ExecutionContext cx, Object typedArray,
            boolean onlyInt32) {
        // FIXME: spec bug - type checks not ordered correctly
        /* step 1, step 5 */
        if (!(typedArray instanceof TypedArrayObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* step 2 */
        TypedArrayObject array = (TypedArrayObject) typedArray;
        /* steps 3-4 */
        if (onlyInt32) {
            if (array.getElementType() != ElementType.Int32) {
                throw newTypeError(cx, Messages.Key.AtomicsInt32ArrayType);
            }
        } else {
            switch (array.getElementType()) {
            case Int8:
            case Uint8:
            case Int16:
            case Uint16:
            case Int32:
            case Uint32:
                break;
            default:
                throw newTypeError(cx, Messages.Key.AtomicsInvalidArrayType);
            }
        }
        /* step 6 */
        ArrayBuffer buffer = array.getBuffer();
        /* step 7 */
        // FIXME: spec bug - type check not needed
        /* step 8 */
        if (!(buffer instanceof SharedArrayBufferObject)) {
            throw newTypeError(cx, Messages.Key.AtomicsNotSharedBuffer);
        }
        /* step 9 */
        return array;
    }

    /**
     * Runtime semantics: ValidateAtomicAccess( typedArray, index )
     * 
     * @param cx
     *            the execution context
     * @param typedArray
     *            the typed array object
     * @param index
     *            the typed array index
     * @return the typed array index
     */
    public static long ValidateAtomicAccess(ExecutionContext cx, TypedArrayObject typedArray, Object index) {
        /* step 1 (implicit) */
        /* steps 2-9 */
        if (Type.isString(index)) {
            /* steps 2, 5-9 */
            long numValue = CanonicalNumericIndexString(Type.stringValue(index).toString());
            if (numValue < 0 || numValue >= typedArray.getArrayLength()) {
                throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
            }
            return numValue;
        }
        if (Type.isNumber(index)) {
            /* steps 3, 5-9 */
            double numValue = Type.numberValue(index);
            if (!IsInteger(numValue) || Double.compare(numValue, -0d) == 0) {
                throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
            }
            if (numValue < 0 || numValue >= typedArray.getArrayLength()) {
                throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
            }
            return (long) numValue;
        }
        /* step 4 */
        throw newRangeError(cx, Messages.Key.AtomicsInvalidArrayIndex);
    }

    private static byte getAndAccumulate(TypedArrayObject array, int index, byte value, IntBinaryOperator op) {
        ByteBuffer buffer = array.getBuffer().getData();
        final int byteIndex = index & ~0b11, shift = 8 * (index & 0b11), mask = ~(0xff << shift);
        int current, newValue, result;
        do {
            current = UnsafeHolder.getIntVolatile(buffer, byteIndex);
            result = 0xff & op.applyAsInt((current >>> shift) & 0xff, value);
            newValue = (current & mask) | (result << shift);
        } while (!UnsafeHolder.compareAndSwapInt(buffer, byteIndex, current, newValue));
        return (byte) ((current >>> shift) & 0xff);
    }

    private static short getAndAccumulate(TypedArrayObject array, int index, short value, IntBinaryOperator op) {
        ByteBuffer buffer = array.getBuffer().getData();
        final int byteIndex = index & ~0b11, shift = 8 * (index & 0b11), mask = ~(0xffff << shift);
        int current, newValue, result;
        do {
            current = UnsafeHolder.getIntVolatile(buffer, byteIndex);
            result = 0xffff & op.applyAsInt((current >>> shift) & 0xffff, value);
            newValue = (current & mask) | (result << shift);
        } while (!UnsafeHolder.compareAndSwapInt(buffer, byteIndex, current, newValue));
        return (short) ((current >>> shift) & 0xffff);
    }

    private static int getAndAccumulate(TypedArrayObject array, int index, int value, IntBinaryOperator op) {
        ByteBuffer buffer = array.getBuffer().getData();
        int current, newValue;
        do {
            current = UnsafeHolder.getIntVolatile(buffer, index);
            newValue = op.applyAsInt(current, value);
        } while (!UnsafeHolder.compareAndSwapInt(buffer, index, current, newValue));
        return current;
    }

    private static byte compareAndSet(TypedArrayObject array, int index, byte expected, byte update) {
        ByteBuffer buffer = array.getBuffer().getData();
        final int byteIndex = index & ~0b11, shift = 8 * (index & 0b11), mask = ~(0xff << shift);
        int current, actualExpected, actualUpdate;
        do {
            current = UnsafeHolder.getIntVolatile(buffer, byteIndex);
            actualExpected = (current & mask) | (expected << shift);
            actualUpdate = (current & mask) | (update << shift);
            if (UnsafeHolder.compareAndSwapInt(buffer, byteIndex, actualExpected, actualUpdate)) {
                return expected;
            }
        } while (current == actualExpected);
        return (byte) ((current >>> shift) & 0xff);
    }

    private static short compareAndSet(TypedArrayObject array, int index, short expected, short update) {
        ByteBuffer buffer = array.getBuffer().getData();
        final int byteIndex = index & ~0b11, shift = 8 * (index & 0b11), mask = ~(0xffff << shift);
        int current, actualExpected, actualUpdate;
        do {
            current = UnsafeHolder.getIntVolatile(buffer, byteIndex);
            actualExpected = (current & mask) | (expected << shift);
            actualUpdate = (current & mask) | (update << shift);
            if (UnsafeHolder.compareAndSwapInt(buffer, byteIndex, actualExpected, actualUpdate)) {
                return expected;
            }
        } while (current == actualExpected);
        return (short) ((current >>> shift) & 0xffff);
    }

    private static int compareAndSet(TypedArrayObject array, int index, int expected, int update) {
        ByteBuffer buffer = array.getBuffer().getData();
        int current;
        do {
            if (UnsafeHolder.compareAndSwapInt(buffer, index, expected, update)) {
                return expected;
            }
            current = UnsafeHolder.getIntVolatile(buffer, index);
        } while (current == expected);
        return current;
    }

    /**
     * Function Properties of the Atomics Object
     */
    public enum Properties {
        ;

        private static double compute(ExecutionContext cx, Object typedArray, Object index, Object value,
                IntBinaryOperator op) {
            /* steps 1-2 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray);
            /* steps 3-4 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 5-6 */
            double v = ToNumber(cx, value);
            /* steps 7, 9 */
            ElementType elementType = array.getElementType();
            /* step 8 */
            int elementSize = elementType.size();
            /* step 10 */
            long offset = array.getByteOffset();
            /* step 11 */
            long indexedPosition = (i * elementSize) + offset;
            int byteIndex = (int) indexedPosition;
            assert indexedPosition == byteIndex;
            /* steps 12-13 */
            switch (elementType) {
            case Int32:
                return getAndAccumulate(array, byteIndex, ToInt32(v), op);
            case Int16:
                return (int) getAndAccumulate(array, byteIndex, ToInt16(v), op);
            case Int8:
                return (int) getAndAccumulate(array, byteIndex, ToInt8(v), op);
            case Uint32:
                return 0xffff_ffffL & getAndAccumulate(array, byteIndex, ToInt32(v), op);
            case Uint16:
                return 0xffff & getAndAccumulate(array, byteIndex, ToInt16(v), op);
            case Uint8:
                return 0xff & getAndAccumulate(array, byteIndex, ToInt8(v), op);
            default:
                throw new AssertionError();
            }
        }

        private static int byteBufferIndex(TypedArrayObject array, long index) {
            return (int) (index * array.getElementType().size() + array.getByteOffset());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * Atomics.OK
         */
        @Value(name = "OK", attributes = @Attributes(writable = false, enumerable = false, configurable = false) )
        public static final int OK = 0;

        /**
         * Atomics.NOTEQUAL
         */
        @Value(name = "NOTEQUAL", attributes = @Attributes(writable = false, enumerable = false, configurable = false) )
        public static final int NOTEQUAL = -1;

        /**
         * Atomics.TIMEDOUT
         */
        @Value(name = "TIMEDOUT", attributes = @Attributes(writable = false, enumerable = false, configurable = false) )
        public static final int TIMEDOUT = -2;

        /**
         * Atomics.add( typedArray, index, value )
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
            // FIXME: spec issue - explicitly specify IEEE-754-2008 behaviour for addition?
            return compute(cx, typedArray, index, value, (r, v) -> r + v);
        }

        /**
         * Atomics.and( typedArray, index, value )
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
            // FIXME: spec issue - &-operator not defined
            return compute(cx, typedArray, index, value, (r, v) -> ToInt32(r) & ToInt32(v));
        }

        /**
         * Atomics.compareExchange( typedArray, index, expectedValue, replacementValue )
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
            /* steps 1-2 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray);
            /* steps 3-4 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 5-6 */
            double expected = ToNumber(cx, expectedValue);
            /* steps 7-8 */
            double replacement = ToNumber(cx, replacementValue);
            /* steps 9, 13 */
            ElementType elementType = array.getElementType();
            /* steps 10-11 (moved) */
            /* step 12 */
            int elementSize = elementType.size();
            /* step 14 */
            long offset = array.getByteOffset();
            /* step 15 */
            long indexedPosition = (i * elementSize) + offset;
            int byteIndex = (int) indexedPosition;
            assert indexedPosition == byteIndex;
            /* steps 10-11, 16-17 */
            switch (elementType) {
            case Int32:
                return compareAndSet(array, byteIndex, ToInt32(expected), ToInt32(replacement));
            case Int16:
                return (int) compareAndSet(array, byteIndex, ToInt16(expected), ToInt16(replacement));
            case Int8:
                return (int) compareAndSet(array, byteIndex, ToInt8(expected), ToInt8(replacement));
            case Uint32:
                return 0xffff_ffffL & compareAndSet(array, byteIndex, ToInt32(expected), ToInt32(replacement));
            case Uint16:
                return 0xffff & compareAndSet(array, byteIndex, ToInt16(expected), ToInt16(replacement));
            case Uint8:
                return 0xff & compareAndSet(array, byteIndex, ToInt8(expected), ToInt8(replacement));
            default:
                throw new AssertionError();
            }
        }

        /**
         * Atomics.exchange( typedArray, index, value )
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
            return compute(cx, typedArray, index, value, (r, v) -> v);
        }

        /**
         * Atomics.futexWait( typedArray, index, value, timeout )
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
        @Function(name = "futexWait", arity = 4)
        public static Object futexWait(ExecutionContext cx, Object thisValue, Object typedArray, Object index,
                Object value, Object timeout) throws InterruptedException {
            /* steps 1-2 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, true);
            /* steps 3-4 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 5-6 */
            int v = ToInt32(cx, value);
            /* step 7 */
            double t;
            if (Type.isUndefined(timeout)) {
                t = Double.POSITIVE_INFINITY;
            } else {
                // FIXME: spec bug - apply ToInteger?
                double q = ToNumber(cx, timeout);
                t = Double.isNaN(q) ? Double.POSITIVE_INFINITY : Math.max(0, q);
            }
            /* steps 8-9 */
            // AgentCanSuspend()
            /* step 10 */
            ByteBuffer bufferVal = array.getBuffer().getData();
            /* steps 11-14 */
            Futex futex = cx.getRuntimeContext().getFutex();
            switch (futex.wait(bufferVal, byteBufferIndex(array, i), v, (long) t, TimeUnit.MILLISECONDS)) {
            case OK:
                return AtomicsObject.Properties.OK;
            case NotEqual:
                return AtomicsObject.Properties.NOTEQUAL;
            case Timedout:
                return AtomicsObject.Properties.TIMEDOUT;
            default:
                throw new AssertionError();
            }
        }

        /**
         * Atomics.futexWake( typedArray, index, count )
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
        @Function(name = "futexWake", arity = 3)
        public static Object futexWake(ExecutionContext cx, Object thisValue, Object typedArray, Object index,
                Object count) {
            /* steps 1-2 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, true);
            /* steps 3-4 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 5-7 */
            int c = (int) Math.max(0, ToInteger(cx, count));
            /* step 8 */
            ByteBuffer bufferVal = array.getBuffer().getData();
            /* steps 9-12 */
            Futex futex = cx.getRuntimeContext().getFutex();
            return futex.wake(bufferVal, byteBufferIndex(array, i), c);
        }

        // FIXME: spec bug - parameters order (typedArray, index1, count, value, index2) in SM/V8
        /**
         * Atomics.futexWakeOrRequeue( typedArray, index1, count, index2, value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param typedArray
         *            the typed array object
         * @param index1
         *            the first typed array index
         * @param count
         *            the maximum number of workers to wake up
         * @param index2
         *            the second typed array index
         * @param value
         *            the new typed array value
         * @return the number of woken up workers
         */
        @Function(name = "futexWakeOrRequeue", arity = 5)
        public static Object futexWakeOrRequeue(ExecutionContext cx, Object thisValue, Object typedArray, Object index1,
                Object count, Object index2, Object value) {
            /* steps 1-2 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray, true);
            /* steps 3-4 */
            long i = ValidateAtomicAccess(cx, array, index1);
            /* steps 5-7 */
            int c = (int) Math.max(0, ToInteger(cx, count));
            /* steps 8-9 */
            long j = ValidateAtomicAccess(cx, array, index2);
            /* step 10 */
            // FIXME: Missing ReturnIfAbrupt
            int v = ToInt32(cx, value);
            /* step 11 */
            ByteBuffer bufferVal = array.getBuffer().getData();
            /* steps 12-15 */
            Futex futex = cx.getRuntimeContext().getFutex();
            int n = futex.wakeOrRequeue(bufferVal, byteBufferIndex(array, i), c, byteBufferIndex(array, j), v);
            if (n < 0) {
                return AtomicsObject.Properties.NOTEQUAL;
            }
            return n;
        }

        /**
         * Atomics.isLockFree( size )
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
            /* steps 1-2 */
            double n = ToInteger(cx, size);
            /* steps 3-5 */
            return (n == 1 || n == 2 || n == 4);
        }

        /**
         * Atomics.load( typedArray, index )
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
            /* steps 1-2 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray);
            /* steps 3-4 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 5, 7 */
            ElementType elementType = array.getElementType();
            /* step 6 */
            int elementSize = elementType.size();
            /* step 8 */
            long offset = array.getByteOffset();
            /* step 9 */
            long indexedPosition = (i * elementSize) + offset;
            int byteIndex = (int) indexedPosition;
            assert indexedPosition == byteIndex;
            /* steps 10-11 */
            ByteBuffer buffer = array.getBuffer().getData();
            switch (elementType) {
            case Int32:
                return UnsafeHolder.getIntVolatile(buffer, byteIndex);
            case Int16:
                return (int) UnsafeHolder.getShortVolatile(buffer, byteIndex);
            case Int8:
                return (int) UnsafeHolder.getByteVolatile(buffer, byteIndex);
            case Uint32:
                return 0xffff_ffffL & UnsafeHolder.getIntVolatile(buffer, byteIndex);
            case Uint16:
                return 0xffff & UnsafeHolder.getShortVolatile(buffer, byteIndex);
            case Uint8:
                return 0xff & UnsafeHolder.getByteVolatile(buffer, byteIndex);
            default:
                throw new AssertionError();
            }
        }

        /**
         * Atomics.or( typedArray, index, value )
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
            // FIXME: spec issue - |-operator not defined
            return compute(cx, typedArray, index, value, (r, v) -> ToInt32(r) | ToInt32(v));
        }

        /**
         * Atomics.store( typedArray, index, value )
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
            /* steps 1-2 */
            TypedArrayObject array = ValidateSharedIntegerTypedArray(cx, typedArray);
            /* steps 3-4 */
            long i = ValidateAtomicAccess(cx, array, index);
            /* steps 5-6 */
            double v = ToNumber(cx, value);
            /* steps 7, 9 */
            ElementType elementType = array.getElementType();
            /* step 8 */
            int elementSize = elementType.size();
            /* step 10 */
            long offset = array.getByteOffset();
            /* step 11 */
            long indexedPosition = (i * elementSize) + offset;
            int byteIndex = (int) indexedPosition;
            assert indexedPosition == byteIndex;
            /* step 12 */
            ByteBuffer buffer = array.getBuffer().getData();
            switch (elementType) {
            case Int32:
            case Uint32:
                UnsafeHolder.putIntVolatile(buffer, byteIndex, ToInt32(v));
                break;
            case Int16:
            case Uint16:
                UnsafeHolder.putShortVolatile(buffer, byteIndex, ToInt16(v));
                break;
            case Int8:
            case Uint8:
                UnsafeHolder.putByteVolatile(buffer, byteIndex, ToInt8(v));
                break;
            default:
                throw new AssertionError();
            }
            /* step 13 */
            return v;
        }

        /**
         * Atomics.sub( typedArray, index, value )
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
            // FIXME: spec issue - explicitly specify IEEE-754-2008 behaviour for subtraction?
            return compute(cx, typedArray, index, value, (r, v) -> r - v);
        }

        /**
         * Atomics.xor( typedArray, index, value )
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
            // FIXME: spec issue - ^-operator not defined
            return compute(cx, typedArray, index, value, (r, v) -> ToInt32(r) ^ ToInt32(v));
        }
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
            UnsafeHolder.fullFence();
            return UNDEFINED;
        }
    }
}
