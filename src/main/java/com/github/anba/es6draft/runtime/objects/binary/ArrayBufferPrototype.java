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
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.5 ArrayBuffer Objects</h3>
 * <ul>
 * <li>15.13.5.4 Properties of the ArrayBuffer Prototype Object
 * </ul>
 */
public class ArrayBufferPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public ArrayBufferPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    @Override
    public Scriptable newInstance(Realm realm) {
        return new ArrayBufferObject(realm);
    }

    /**
     * 15.13.5.4 Properties of the ArrayBuffer Prototype Object
     */
    public enum Properties {
        ;

        private static ArrayBufferObject ArrayBufferObject(Realm realm, Scriptable m) {
            if (m instanceof ArrayBufferObject) {
                return (ArrayBufferObject) m;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.5.4.1 ArrayBuffer.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.ArrayBuffer;

        /**
         * 15.13.5.4.2 get ArrayBuffer.prototype.byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(Realm realm, Object thisValue) {
            Scriptable obj = ToObject(realm, thisValue);
            long length = ArrayBufferObject(realm, obj).getByteLength();
            return length;
        }

        /**
         * 15.13.5.4.3 ArrayBuffer.prototype.slice (start , end)
         */
        @Function(name = "slice", arity = 2)
        public static Object slice(Realm realm, Object thisValue, Object start, Object end) {
            Scriptable obj = ToObject(realm, thisValue);
            ArrayBufferObject buf = ArrayBufferObject(realm, obj);
            long len = buf.getByteLength();
            double relativeStart = ToInteger(realm, start);
            double first = relativeStart < 0 ? Math.max((len + relativeStart), 0) : Math.min(
                    relativeStart, len);
            double relativeEnd = (Type.isUndefined(end) ? len : ToInteger(realm, end));
            double _final = relativeEnd < 0 ? Math.max((len + relativeEnd), 0) : Math.min(
                    relativeEnd, len);
            // FIXME: spec bug (check for negative, cf. SpiderMonkey/V8)
            double newLen = _final - first;
            Callable ctor = GetMethod(realm, obj, "constructor");
            if (ctor == null || !IsConstructor(ctor)) {
                throwTypeError(realm, Messages.Key.NotConstructor);
            }
            ArrayBufferObject _new = ArrayBufferObject(realm,
                    ToObject(realm, ((Constructor) ctor).construct(newLen)));
            CopyBlockElements(buf.getData(), first, _new.getData(), 0, newLen);
            return _new;
        }

        /**
         * 15.13.5.4.4 ArrayBuffer.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "ArrayBuffer";
    }
}
