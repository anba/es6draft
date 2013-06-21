/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.7 DataView Objects</h3>
 * <ul>
 * <li>15.13.7.4 Properties of the DataView Prototype Object
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
     * 15.13.7.4 Properties of the DataView Prototype Object<br>
     * GetValue(byteOffset, isLittleEndian, type)
     */
    public static double GetValue(ExecutionContext cx, DataViewObject view, long byteOffset,
            boolean isLittleEndian, ElementKind type) {
        // long byteOffsetInt = ToUint32(realm, byteOffset);
        long totalOffset = byteOffset + ToUint32(cx, Get(cx, view, "byteOffset"));
        long byteLength = ToUint32(cx, Get(cx, view, "byteLength"));
        // FIXME: spec bug - range check is invalid
        // if (totalOffset >= byteLength) {
        // throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        // }
        if (byteOffset + type.size() > byteLength) {
            throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        ArrayBufferObject buffer = (ArrayBufferObject) Get(cx, view, "buffer");
        return GetValueFromBuffer(buffer, totalOffset, type, !isLittleEndian);
    }

    /**
     * 15.13.7.4 Properties of the DataView Prototype Object<br>
     * SetValue(byteOffset, isLittleEndian, type, value)
     */
    public static void SetValue(ExecutionContext cx, DataViewObject view, long byteOffset,
            boolean isLittleEndian, ElementKind type, double value) {
        // long byteOffsetInt = ToUint32(realm, byteOffset);
        long totalOffset = byteOffset + ToUint32(cx, Get(cx, view, "byteOffset"));
        long byteLength = ToUint32(cx, Get(cx, view, "byteLength"));
        // FIXME: spec bug - range check is invalid
        // if (totalOffset >= byteLength) {
        // throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        // }
        if (byteOffset + type.size() > byteLength) {
            throwRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        ArrayBufferObject buffer = (ArrayBufferObject) Get(cx, view, "buffer");
        SetValueInBuffer(buffer, totalOffset, type, value, !isLittleEndian);
    }

    /**
     * 15.13.7.4 Properties of the DataView Prototype Object
     */
    public enum Properties {
        ;

        private static DataViewObject DataViewObject(ExecutionContext cx, ScriptObject m) {
            if (m instanceof DataViewObject) {
                return (DataViewObject) m;
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.13.7.4.1 DataView.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.DataView;

        /**
         * 15.13.7.4.2 DataView.prototype.getInt8(byteOffset)
         */
        @Function(name = "getInt8", arity = 1)
        public static Object getInt8(ExecutionContext cx, Object thisValue, Object byteOffset) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), true, ElementKind.Int8);
        }

        /**
         * 15.13.7.4.3 DataView.prototype.getUint8(byteOffset)
         */
        @Function(name = "getUint8", arity = 1)
        public static Object getUint8(ExecutionContext cx, Object thisValue, Object byteOffset) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), true, ElementKind.Uint8);
        }

        /**
         * 15.13.7.4.4 DataView.prototype.getInt16(byteOffset, littleEndian)
         */
        @Function(name = "getInt16", arity = 2)
        public static Object getInt16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            // FIXME: spec bug? (evaluation order vs. web reality)
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Int16);
        }

        /**
         * 15.13.7.4.5 DataView.prototype.getUint16(byteOffset, littleEndian)
         */
        @Function(name = "getUint16", arity = 2)
        public static Object getUint16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            // FIXME: spec bug? (evaluation order vs. web reality)
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Uint16);
        }

        /**
         * 15.13.7.4.6 DataView.prototype.getInt32(byteOffset, littleEndian)
         */
        @Function(name = "getInt32", arity = 2)
        public static Object getInt32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            // FIXME: spec bug? (evaluation order vs. web reality)
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Int32);
        }

        /**
         * 15.13.7.4.7 DataView.prototype.getUint32(byteOffset, littleEndian)
         */
        @Function(name = "getUint32", arity = 2)
        public static Object getUint32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            // FIXME: spec bug? (evaluation order vs. web reality)
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Uint32);
        }

        /**
         * 15.13.7.4.8 DataView.prototype.getFloat32(byteOffset, littleEndian)
         */
        @Function(name = "getFloat32", arity = 2)
        public static Object getFloat32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            // FIXME: spec bug? (evaluation order vs. web reality)
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Float32);
        }

        /**
         * 15.13.7.4.9 DataView.prototype.getFloat64(byteOffset, littleEndian)
         */
        @Function(name = "getFloat64", arity = 2)
        public static Object getFloat64(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            // FIXME: spec bug? (evaluation order vs. web reality)
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            return GetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Float64);
        }

        /**
         * 15.13.7.4.10 DataView.prototype.setInt8(byteOffset, value)
         */
        @Function(name = "setInt8", arity = 2)
        public static Object setInt8(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), true, ElementKind.Int8,
                    ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * 15.13.7.4.11 DataView.prototype.setUint8(byteOffset, value)
         */
        @Function(name = "setUint8", arity = 2)
        public static Object setUint8(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), true, ElementKind.Uint8,
                    ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * 15.13.7.4.12 DataView.prototype.setInt16(byteOffset, value, littleEndian)
         */
        @Function(name = "setInt16", arity = 3)
        public static Object setInt16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Int16, ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * 15.13.7.4.13 DataView.prototype.setUint16(byteOffset, value, littleEndian)
         */
        @Function(name = "setUint16", arity = 3)
        public static Object setUint16(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Uint16, ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * 15.13.7.4.14 DataView.prototype.setInt32(byteOffset, value, littleEndian)
         */
        @Function(name = "setInt32", arity = 3)
        public static Object setInt32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Int32, ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * 15.13.7.4.15 DataView.prototype.setUint32(byteOffset, value, littleEndian)
         */
        @Function(name = "setUint32", arity = 3)
        public static Object setUint32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Uint32, ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * 15.13.7.4.16 DataView.prototype.setFloat32(byteOffset, value, littleEndian)
         */
        @Function(name = "setFloat32", arity = 3)
        public static Object setFloat32(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Float32, ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * 15.13.7.4.17 DataView.prototype.setFloat64(byteOffset, value, littleEndian)
         */
        @Function(name = "setFloat64", arity = 3)
        public static Object setFloat64(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object value, Object littleEndian) {
            DataViewObject view = DataViewObject(cx, ToObject(cx, thisValue));
            // FIXME: spec bug? (evaluation order vs. web reality)
            // FIXME: spec bug? (return value vs. web reality)
            SetValue(cx, view, ToUint32(cx, byteOffset), ToBoolean(littleEndian),
                    ElementKind.Float64, ToNumber(cx, value));
            return UNDEFINED;
        }

        /**
         * FIXME: missing in spec
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "DataView";
    }
}
