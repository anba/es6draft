/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.atomics;

import static com.github.anba.es6draft.runtime.AbstractOperations.SpeciesConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.atomics.SharedArrayBufferConstructor.SharedDataBlockID;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CopyDataBlockBytes;

import java.nio.ByteBuffer;

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
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Shared Memory and Atomics</h1><br>
 * <h2>SharedArrayBuffer Objects</h2>
 * <ul>
 * <li>Properties of the SharedArrayBuffer Prototype Object
 * </ul>
 */
public final class SharedArrayBufferPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new SharedArrayBuffer prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public SharedArrayBufferPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Properties of the SharedArrayBuffer Prototype Object
     */
    public enum Properties {
        ;

        private static SharedArrayBufferObject thisSharedArrayBufferObject(ExecutionContext cx, Object m) {
            if (m instanceof SharedArrayBufferObject) {
                return (SharedArrayBufferObject) m;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * SharedArrayBuffer.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.SharedArrayBuffer;

        /**
         * get SharedArrayBuffer.prototype.byteLength
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the shared array buffer length in bytes
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            SharedArrayBufferObject obj = thisSharedArrayBufferObject(cx, thisValue);
            /* steps 4-5 */
            return obj.getByteLength();
        }

        /**
         * SharedArrayBuffer.prototype.slice (start, end)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return the new shared array buffer object
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            /* steps 1-3 */
            SharedArrayBufferObject obj = thisSharedArrayBufferObject(cx, thisValue);
            /* step 4 */
            long len = obj.getByteLength();
            /* steps 5-6 */
            double relativeStart = ToInteger(cx, start);
            /* step 7 */
            long first = (long) (relativeStart < 0 ? Math.max((len + relativeStart), 0) : Math.min(relativeStart, len));
            /* steps 8-9 */
            double relativeEnd = Type.isUndefined(end) ? len : ToInteger(cx, end);
            /* step 10 */
            long _final = (long) (relativeEnd < 0 ? Math.max((len + relativeEnd), 0) : Math.min(relativeEnd, len));
            /* step 11 */
            long newLen = Math.max(_final - first, 0);
            /* steps 12-13 */
            Constructor ctor = SpeciesConstructor(cx, obj, Intrinsics.SharedArrayBuffer);
            /* steps 14-16 */
            SharedArrayBufferObject _new = thisSharedArrayBufferObject(cx, ctor.construct(cx, ctor, newLen));
            /* step 17 */
            if (_new == obj) {
                // TODO: better error message
                throw newTypeError(cx, Messages.Key.BufferInvalid);
            }
            /* step 18 */
            if (_new.getByteLength() < newLen) {
                throw newTypeError(cx, Messages.Key.InvalidBufferSize);
            }
            // FIXME: spec bug - missing shareddata-block-id check
            Object srcId = SharedDataBlockID(obj.getData());
            Object targetId = SharedDataBlockID(_new.getData());
            if (srcId == targetId) {
                // TODO: better error message
                throw newTypeError(cx, Messages.Key.BufferInvalid);
            }
            /* step 19 */
            ByteBuffer fromBuf = obj.getData();
            /* step 20 */
            ByteBuffer toBuf = _new.getData();
            /* step 21 */
            CopyDataBlockBytes(toBuf, 0, fromBuf, first, newLen);
            /* step 22 */
            return _new;
        }

        /**
         * SharedArrayBuffer.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true) )
        public static final String toStringTag = "SharedArrayBuffer";
    }
}
