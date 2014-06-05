/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CopyDataBlockBytes;

import java.nio.ByteBuffer;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.1 ArrayBuffer Objects</h2>
 * <ul>
 * <li>24.1.4 Properties of the ArrayBuffer Prototype Object
 * </ul>
 */
public final class ArrayBufferPrototype extends OrdinaryObject implements Initializable {
    public ArrayBufferPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    /**
     * 24.1.4 Properties of the ArrayBuffer Prototype Object
     */
    public enum Properties {
        ;

        private static ArrayBufferObject thisArrayBufferObject(ExecutionContext cx, Object m) {
            if (m instanceof ArrayBufferObject) {
                ArrayBufferObject buffer = (ArrayBufferObject) m;
                if (buffer.getData() != null || buffer.isNeutered()) {
                    return buffer;
                }
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 24.1.4.2 ArrayBuffer.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.ArrayBuffer;

        /**
         * 24.1.4.1 get ArrayBuffer.prototype.byteLength
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the array buffer length in bytes
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            ArrayBufferObject obj = thisArrayBufferObject(cx, thisValue);
            /* steps 5-7 */
            return obj.getByteLength();
        }

        /**
         * 24.1.4.3 ArrayBuffer.prototype.slice (start, end)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param start
         *            the start index
         * @param end
         *            the end index
         * @return the new array buffer object
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            /* steps 1-4 */
            ArrayBufferObject obj = thisArrayBufferObject(cx, thisValue);
            /* step 5 */
            long len = obj.getByteLength();
            /* steps 6-7 */
            double relativeStart = ToInteger(cx, start);
            /* step 8 */
            long first = (long) (relativeStart < 0 ? Math.max((len + relativeStart), 0) : Math.min(
                    relativeStart, len));
            /* steps 9-10 */
            double relativeEnd = Type.isUndefined(end) ? len : ToInteger(cx, end);
            /* step 11 */
            long _final = (long) (relativeEnd < 0 ? Math.max((len + relativeEnd), 0) : Math.min(
                    relativeEnd, len));
            /* step 12 */
            long newLen = Math.max(_final - first, 0);
            /* steps 13-14 */
            Object ctor = Get(cx, obj, "constructor");
            /* step 15 */
            if (!IsConstructor(ctor)) {
                throw newTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 16-19 */
            ArrayBufferObject _new = thisArrayBufferObject(cx,
                    ((Constructor) ctor).construct(cx, newLen));
            /* step 20 */
            if (_new.getByteLength() < newLen) {
                // FIXME: spec bug - throw RangeError instead of TypeError?
                throw newTypeError(cx, Messages.Key.InvalidBufferSize);
            }
            /* step 21 */
            ByteBuffer fromBuf = obj.getData();
            /* step 22 */
            ByteBuffer toBuf = _new.getData();
            // FIXME: spec bug - need to check for neutered buffers (bug 2964)
            if (fromBuf == null || toBuf == null) {
                return _new;
            }
            /* steps 23 */
            CopyDataBlockBytes(toBuf, 0, fromBuf, first, newLen);
            /* step 24 */
            return _new;
        }

        /**
         * 24.1.4.4 ArrayBuffer.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "ArrayBuffer";
    }
}
