/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.DataViewConstructor.GetViewValue;
import static com.github.anba.es6draft.runtime.objects.binary.DataViewConstructor.SetViewValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h3>24.2 DataView Objects</h3>
 * <ul>
 * <li>24.2.4 Properties of the DataView Prototype Object
 * </ul>
 */
public class DataViewPrototype extends OrdinaryObject implements Initialisable {
    public DataViewPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 24.2.4 Properties of the DataView Prototype Object
     */
    public enum Properties {
        ;

        private static ArrayBufferView thisArrayBufferView(ExecutionContext cx, Object m) {
            if (m instanceof ArrayBufferView) {
                ArrayBufferView view = (ArrayBufferView) m;
                if (view.getBuffer() != null) {
                    return view;
                }
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 24.2.4.4 DataView.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.DataView;

        /**
         * 24.2.4.1 buffer
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            return thisArrayBufferView(cx, thisValue).getBuffer();
        }

        /**
         * 24.2.4.2 byteLength
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            return thisArrayBufferView(cx, thisValue).getByteLength();
        }

        /**
         * 24.2.4.3 byteOffset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            return thisArrayBufferView(cx, thisValue).getByteOffset();
        }

        /**
         * 24.2.4.7 DataView.prototype.getInt8(byteOffset)
         */
        @Function(name = "getInt8", arity = 1)
        public static Object getInt8(ExecutionContext cx, Object thisValue, Object byteOffset) {
            return GetViewValue(cx, thisValue, byteOffset, UNDEFINED, ElementType.Int8);
        }

        /**
         * 24.2.4.10 DataView.prototype.getUint8(byteOffset)
         */
        @Function(name = "getUint8", arity = 1)
        public static Object getUint8(ExecutionContext cx, Object thisValue, Object byteOffset) {
            return GetViewValue(cx, thisValue, byteOffset, UNDEFINED, ElementType.Uint8);
        }

        /**
         * 24.2.4.8 DataView.prototype.getInt16(byteOffset, littleEndian=false)
         */
        @Function(name = "getInt16", arity = 2)
        public static Object getInt16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int16);
        }

        /**
         * 24.2.4.11 DataView.prototype.getUint16(byteOffset, littleEndian=false)
         */
        @Function(name = "getUint16", arity = 2)
        public static Object getUint16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint16);
        }

        /**
         * 24.2.4.9 DataView.prototype.getInt32(byteOffset, littleEndian=false)
         */
        @Function(name = "getInt32", arity = 2)
        public static Object getInt32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int32);
        }

        /**
         * 24.2.4.12 DataView.prototype.getUint32(byteOffset, littleEndian=false)
         */
        @Function(name = "getUint32", arity = 2)
        public static Object getUint32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint32);
        }

        /**
         * 24.2.4.5 DataView.prototype.getFloat32(byteOffset, littleEndian=false)
         */
        @Function(name = "getFloat32", arity = 2)
        public static Object getFloat32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float32);
        }

        /**
         * 24.2.4.6 DataView.prototype.getFloat64(byteOffset, littleEndian=false)
         */
        @Function(name = "getFloat64", arity = 2)
        public static Object getFloat64(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float64);
        }

        /**
         * 24.2.4.15 DataView.prototype.setInt8(byteOffset, value)
         */
        @Function(name = "setInt8", arity = 2)
        public static Object setInt8(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value) {
            SetViewValue(cx, thisValue, byteOffset, UNDEFINED, ElementType.Int8, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.18 DataView.prototype.setUint8(byteOffset, value)
         */
        @Function(name = "setUint8", arity = 2)
        public static Object setUint8(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value) {
            SetViewValue(cx, thisValue, byteOffset, UNDEFINED, ElementType.Uint8, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.16 DataView.prototype.setInt16(byteOffset, value, littleEndian=false)
         */
        @Function(name = "setInt16", arity = 3)
        public static Object setInt16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int16, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.19 DataView.prototype.setUint16(byteOffset, value, littleEndian=false)
         */
        @Function(name = "setUint16", arity = 3)
        public static Object setUint16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint16, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.17 DataView.prototype.setInt32(byteOffset, value, littleEndian=false)
         */
        @Function(name = "setInt32", arity = 3)
        public static Object setInt32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int32, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.20 DataView.prototype.setUint32(byteOffset, value, littleEndian=false)
         */
        @Function(name = "setUint32", arity = 3)
        public static Object setUint32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint32, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.13 DataView.prototype.setFloat32(byteOffset, value, littleEndian=false)
         */
        @Function(name = "setFloat32", arity = 3)
        public static Object setFloat32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float32, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.14 DataView.prototype.setFloat64(byteOffset, value, littleEndian=false)
         */
        @Function(name = "setFloat64", arity = 3)
        public static Object setFloat64(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float64, value);
            return UNDEFINED;
        }

        /**
         * 24.2.4.21 DataView.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "DataView";
    }
}
