/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.GetMethod;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CopyBlockElements;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.5 ArrayBuffer Objects</h3>
 * <ul>
 * <li>15.13.5.5 Properties of the ArrayBuffer Prototype Object
 * </ul>
 */
public class ArrayBufferPrototype extends OrdinaryObject implements Initialisable {
    public ArrayBufferPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.13.5.5 Properties of the ArrayBuffer Prototype Object
     */
    public enum Properties {
        ;

        private static ArrayBufferObject ArrayBufferObject(ExecutionContext cx, ScriptObject m) {
            if (m instanceof ArrayBufferObject) {
                return (ArrayBufferObject) m;
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.5.5.1 ArrayBuffer.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.ArrayBuffer;

        /**
         * 15.13.5.5.2 get ArrayBuffer.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            ScriptObject obj = ToObject(cx, thisValue);
            long length = ArrayBufferObject(cx, obj).getByteLength();
            return length;
        }

        /**
         * 15.13.5.5.3 ArrayBuffer.prototype.slice (start , end)
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(ExecutionContext cx, Object thisValue, Object start, Object end) {
            ScriptObject obj = ToObject(cx, thisValue);
            ArrayBufferObject buf = ArrayBufferObject(cx, obj);
            long len = buf.getByteLength();
            double relativeStart = ToInteger(cx, start);
            double first = relativeStart < 0 ? Math.max((len + relativeStart), 0) : Math.min(
                    relativeStart, len);
            double relativeEnd = (Type.isUndefined(end) ? len : ToInteger(cx, end));
            double _final = relativeEnd < 0 ? Math.max((len + relativeEnd), 0) : Math.min(
                    relativeEnd, len);
            // FIXME: spec bug (check for negative, cf. SpiderMonkey/V8)
            double newLen = _final - first;
            Callable ctor = GetMethod(cx, obj, "constructor");
            if (ctor == null || !IsConstructor(ctor)) {
                throw throwTypeError(cx, Messages.Key.NotConstructor);
            }
            ArrayBufferObject _new = ArrayBufferObject(cx,
                    ToObject(cx, ((Constructor) ctor).construct(cx, newLen)));
            CopyBlockElements(buf.getData(), first, _new.getData(), 0, newLen);
            return _new;
        }

        /**
         * 15.13.5.5.4 ArrayBuffer.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "ArrayBuffer";
    }
}
