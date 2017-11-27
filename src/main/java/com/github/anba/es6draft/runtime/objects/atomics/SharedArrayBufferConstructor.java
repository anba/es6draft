/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToIndex;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

import java.nio.ByteBuffer;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Bytes;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.binary.ArrayBuffer;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.2 SharedArrayBuffer Objects</h2>
 * <ul>
 * <li>24.2.1 Abstract Operations for SharedArrayBuffer Objects
 * <li>24.2.2 The SharedArrayBuffer Constructor
 * <li>24.2.3 Properties of the SharedArrayBuffer Constructor
 * </ul>
 */
public final class SharedArrayBufferConstructor extends BuiltinConstructor implements Initializable {
    private static final int DIRECT_LIMIT = 10 * 1024;

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

    /**
     * 6.2.7.2 CreateSharedByteDataBlock( size )
     * 
     * @param cx
     *            the execution context
     * @param size
     *            the byte buffer size in bytes
     * @return the new byte buffer
     */
    public static SharedByteBuffer CreateSharedByteDataBlock(ExecutionContext cx, long size) {
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
            ByteBuffer buffer;
            if (size < DIRECT_LIMIT) {
                buffer = ByteBuffer.allocate(requestedSize);
            } else {
                buffer = ByteBuffer.allocateDirect(requestedSize);
            }
            return new SharedByteBuffer(buffer.order(Bytes.DEFAULT_BYTE_ORDER));
        } catch (OutOfMemoryError e) {
            /* step 2 */
            throw newRangeError(cx, Messages.Key.OutOfMemoryVM);
        }
    }

    /**
     * 24.2.1.1 AllocateSharedArrayBuffer( constructor, byteLength )
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
        /* step 1 */
        ScriptObject proto = GetPrototypeFromConstructor(cx, constructor, Intrinsics.SharedArrayBufferPrototype);
        /* step 2 */
        assert byteLength >= 0;
        /* step 3 */
        SharedByteBuffer block = CreateSharedByteDataBlock(cx, byteLength);
        /* steps 1, 4-6 */
        return new SharedArrayBufferObject(cx.getRealm(), block, byteLength, proto);
    }

    /**
     * 24.2.1.2 IsSharedArrayBuffer( obj )
     * 
     * @param obj
     *            the object
     * @return {@code true} if the buffer is shared array buffer object
     */
    public static boolean IsSharedArrayBuffer(ArrayBuffer obj) {
        /* step 1 (implicit) */
        /* steps 2-6 */
        return obj instanceof SharedArrayBufferObject;
    }

    /**
     * 24.2.2.1 SharedArrayBuffer( length )
     */
    @Override
    public SharedArrayBufferObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "SharedArrayBuffer");
    }

    /**
     * 24.2.2.1 SharedArrayBuffer( length )
     */
    @Override
    public SharedArrayBufferObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object length = argument(args, 0);
        /* step 1 (not applicable) */
        /* step 2 */
        // FIXME: spec bug - typo `numberLength` -> `length`
        long byteLength = ToIndex(calleeContext, length);
        /* step 3 */
        return AllocateSharedArrayBuffer(calleeContext, newTarget, byteLength);
    }

    /**
     * 24.2.3 Properties of the SharedArrayBuffer Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "SharedArrayBuffer";

        /**
         * 24.2.3.1 SharedArrayBuffer.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.SharedArrayBufferPrototype;

        /**
         * 24.2.3.2 get SharedArrayBuffer [ @@species ]
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
