/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.SpeciesConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CopyDataBlockBytes;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;

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
import com.github.anba.es6draft.runtime.types.ScriptObject;
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
    /**
     * Constructs a new ArrayBuffer prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ArrayBufferPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 24.1.4 Properties of the ArrayBuffer Prototype Object
     */
    public enum Properties {
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
            ArrayBufferObject obj = thisArrayBufferObject(cx, thisValue, "ArrayBuffer.prototype.byteLength");
            /* steps 5-6 */
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
            ArrayBufferObject obj = thisArrayBufferObject(cx, thisValue, "ArrayBuffer.prototype.slice");
            /* step 5 */
            long len = obj.getByteLength();
            /* step 6 */
            long relativeStart = (long) ToNumber(cx, start); // ToInteger
            /* step 7 */
            long first = (relativeStart < 0 ? Math.max((len + relativeStart), 0) : Math.min(relativeStart, len));
            /* step 8 */
            long relativeEnd = Type.isUndefined(end) ? len : (long) ToNumber(cx, end); // ToInteger
            /* step 9 */
            long _final = (relativeEnd < 0 ? Math.max((len + relativeEnd), 0) : Math.min(relativeEnd, len));
            /* step 10 */
            long newLen = Math.max(_final - first, 0);
            /* step 11 */
            Constructor ctor = SpeciesConstructor(cx, obj, Intrinsics.ArrayBuffer);
            /* step 12 */
            ScriptObject newObj = ctor.construct(cx, newLen);
            /* step 13 */
            if (!(newObj instanceof ArrayBufferObject)) {
                throw newTypeError(cx, Messages.Key.IncompatibleNewObject, "ArrayBuffer.prototype.slice",
                        Type.of(newObj).toString());
            }
            ArrayBufferObject _new = (ArrayBufferObject) newObj;
            /* step 14 */
            if (IsDetachedBuffer(_new)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* step 15 */
            if (_new == obj) {
                throw newTypeError(cx, Messages.Key.BufferInvalid);
            }
            /* step 16 */
            if (_new.getByteLength() < newLen) {
                throw newTypeError(cx, Messages.Key.InvalidBufferSize);
            }
            /* steps 17-18 */
            if (IsDetachedBuffer(obj)) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
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
         * 24.1.4.4 ArrayBuffer.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "ArrayBuffer";
    }
}
