/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToLength;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBufferView;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>Shared Memory and Atomics</h1><br>
 * <h2>SharedArrayBuffer Objects</h2>
 * <ul>
 * <li>Abstract Operations For SharedArrayBuffer Objects
 * <li>The SharedArrayBuffer Constructor
 * <li>Properties of the SharedArrayBuffer Constructor
 * </ul>
 */
public final class SharedArrayBufferConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new SharedArrayBuffer constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public SharedArrayBufferConstructor(Realm realm) {
        super(realm, "SharedArrayBuffer", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public SharedArrayBufferConstructor clone() {
        return new SharedArrayBufferConstructor(getRealm());
    }

    /**
     * CreateSharedByteDataBlock( size )
     * 
     * @param cx
     *            the execution context
     * @param size
     *            the byte buffer size in bytes
     * @return the new byte buffer
     */
    public static ByteBuffer CreateSharedByteDataBlock(ExecutionContext cx, long size) {
        /* step 1 */
        assert size >= 0;
        /* step 2 */
        if (size > Integer.MAX_VALUE) {
            throw newRangeError(cx, Messages.Key.OutOfMemory);
        }
        // Align requested size to int32-size to allow access with Unsafe#compareAndSwapInt().
        int requestedSize = ((int) size + 0b11) & ~0b11;
        // Overflow detection.
        if (requestedSize < 0) {
            throw newRangeError(cx, Messages.Key.OutOfMemory);
        }
        try {
            /* steps 3-4 */
            // TODO: Call allocateDirect() if size exceeds predefined limit?
            return ByteBuffer.allocate(requestedSize).order(ByteOrder.nativeOrder());
        } catch (OutOfMemoryError e) {
            /* step 2 */
            throw newRangeError(cx, Messages.Key.OutOfMemoryVM);
        }
    }

    /**
     * SharedDataBlockID( block )
     * 
     * @param block
     *            the byte buffer
     * @return the byte buffer id
     */
    public static Object SharedDataBlockID(ByteBuffer block) {
        return block;
    }

    /**
     * AllocateSharedArrayBuffer( constructor, byteLength )
     * 
     * @param cx
     *            the execution context
     * @param constructor
     *            the constructor function
     * @param byteLength
     *            the buffer byte length
     * @return the new shared array buffer object
     */
    public static SharedArrayBufferObject AllocateSharedArrayBuffer(ExecutionContext cx, Constructor constructor,
            long byteLength) {
        /* steps 1-2 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, Intrinsics.SharedArrayBufferPrototype);
        /* step 3 */
        assert byteLength >= 0;
        /* steps 4-5 */
        ByteBuffer block = CreateSharedByteDataBlock(cx, byteLength);
        /* steps 1-2, 6-8 */
        return new SharedArrayBufferObject(cx.getRealm(), block, byteLength, proto);
    }

    /**
     * IsSharedMemory( obj )
     * 
     * @param obj
     *            the object
     * @return {@code true} if the buffer is shared array buffer object
     */
    public static boolean IsSharedMemory(ArrayBuffer obj) {
        return obj instanceof SharedArrayBufferObject;
    }

    /**
     * SharedArrayBuffer(length)
     */
    @Override
    public SharedArrayBufferObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "SharedArrayBuffer");
    }

    /**
     * SharedArrayBuffer(length)
     */
    @Override
    public SharedArrayBufferObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object length = argument(args, 0);
        /* step 1 (not applicable) */
        /* step 2 */
        double numberLength = ToNumber(calleeContext, length);
        /* steps 3-4 */
        long byteLength = ToLength(numberLength);
        /* step 5 */
        if (numberLength != byteLength) { // SameValueZero
            throw newRangeError(calleeContext, Messages.Key.InvalidBufferSize);
        }
        /* step 6 */
        return AllocateSharedArrayBuffer(calleeContext, newTarget, byteLength);
    }

    /**
     * Properties of the SharedArrayBuffer Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final String name = "SharedArrayBuffer";

        /**
         * SharedArrayBuffer.prototype
         */
        @Value(name = "prototype",
                attributes = @Attributes(writable = false, enumerable = false, configurable = false) )
        public static final Intrinsics prototype = Intrinsics.SharedArrayBufferPrototype;

        /**
         * SharedArrayBuffer.isView ( arg )
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
         * get SharedArrayBuffer [ @@species ]
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
}
