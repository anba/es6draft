/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.DataViewConstructor.GetViewValue;
import static com.github.anba.es6draft.runtime.objects.binary.DataViewConstructor.SetViewValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.CompatibilityExtension;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.2 DataView Objects</h2>
 * <ul>
 * <li>24.2.4 Properties of the DataView Prototype Object
 * </ul>
 */
public final class DataViewPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new DataView prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public DataViewPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
        createProperties(realm, this, Properties64.class);
    }

    /**
     * 24.2.4 Properties of the DataView Prototype Object
     */
    public enum Properties {
        ;

        private static DataViewObject thisDataViewObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof DataViewObject) {
                return (DataViewObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 24.2.4.4 DataView.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.DataView;

        /**
         * 24.2.4.1 get DataView.prototype.buffer
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the array buffer object
         */
        @Accessor(name = "buffer", type = Accessor.Type.Getter)
        public static Object buffer(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            DataViewObject view = thisDataViewObject(cx, thisValue, "DataView.prototype.buffer");
            /* steps 4-6 */
            return view.getBuffer();
        }

        /**
         * 24.2.4.2 get DataView.prototype.byteLength
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the view length in bytes
         */
        @Accessor(name = "byteLength", type = Accessor.Type.Getter)
        public static Object byteLength(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            DataViewObject view = thisDataViewObject(cx, thisValue, "DataView.prototype.byteLength");
            /* steps 4-6 */
            if (IsDetachedBuffer(view.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* steps 7-8 */
            return view.getByteLength();
        }

        /**
         * 24.2.4.3 get DataView.prototype.byteOffset
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the byte offset
         */
        @Accessor(name = "byteOffset", type = Accessor.Type.Getter)
        public static Object byteOffset(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            DataViewObject view = thisDataViewObject(cx, thisValue, "DataView.prototype.byteOffset");
            /* steps 4-6 */
            if (IsDetachedBuffer(view.getBuffer())) {
                throw newTypeError(cx, Messages.Key.BufferDetached);
            }
            /* steps 7-8 */
            return view.getByteOffset();
        }

        /**
         * 24.2.4.7 DataView.prototype.getInt8(byteOffset)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @return the int8 value from the requested byte offset
         */
        @Function(name = "getInt8", arity = 1)
        public static Object getInt8(ExecutionContext cx, Object thisValue, Object byteOffset) {
            return GetViewValue(cx, thisValue, byteOffset, true, ElementType.Int8, "DataView.prototype.getInt8");
        }

        /**
         * 24.2.4.10 DataView.prototype.getUint8(byteOffset)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @return the uint8 value from the requested byte offset
         */
        @Function(name = "getUint8", arity = 1)
        public static Object getUint8(ExecutionContext cx, Object thisValue, Object byteOffset) {
            return GetViewValue(cx, thisValue, byteOffset, true, ElementType.Uint8, "DataView.prototype.getUint8");
        }

        /**
         * 24.2.4.8 DataView.prototype.getInt16(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the int16 value from the requested byte offset
         */
        @Function(name = "getInt16", arity = 1)
        public static Object getInt16(ExecutionContext cx, Object thisValue, Object byteOffset, Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int16,
                    "DataView.prototype.getInt16");
        }

        /**
         * 24.2.4.11 DataView.prototype.getUint16(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the uint16 value from the requested byte offset
         */
        @Function(name = "getUint16", arity = 1)
        public static Object getUint16(ExecutionContext cx, Object thisValue, Object byteOffset, Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint16,
                    "DataView.prototype.getUint16");
        }

        /**
         * 24.2.4.9 DataView.prototype.getInt32(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the int32 value from the requested byte offset
         */
        @Function(name = "getInt32", arity = 1)
        public static Object getInt32(ExecutionContext cx, Object thisValue, Object byteOffset, Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int32,
                    "DataView.prototype.getInt32");
        }

        /**
         * 24.2.4.12 DataView.prototype.getUint32(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the uint32 value from the requested byte offset
         */
        @Function(name = "getUint32", arity = 1)
        public static Object getUint32(ExecutionContext cx, Object thisValue, Object byteOffset, Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint32,
                    "DataView.prototype.getUint32");
        }

        /**
         * 24.2.4.5 DataView.prototype.getFloat32(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the float32 value from the requested byte offset
         */
        @Function(name = "getFloat32", arity = 1)
        public static Object getFloat32(ExecutionContext cx, Object thisValue, Object byteOffset, Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float32,
                    "DataView.prototype.getFloat32");
        }

        /**
         * 24.2.4.6 DataView.prototype.getFloat64(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the float64 value from the requested byte offset
         */
        @Function(name = "getFloat64", arity = 1)
        public static Object getFloat64(ExecutionContext cx, Object thisValue, Object byteOffset, Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float64,
                    "DataView.prototype.getFloat64");
        }

        /**
         * 24.2.4.15 DataView.prototype.setInt8(byteOffset, value)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @return the undefined value
         */
        @Function(name = "setInt8", arity = 2)
        public static Object setInt8(ExecutionContext cx, Object thisValue, Object byteOffset, Object value) {
            SetViewValue(cx, thisValue, byteOffset, true, ElementType.Int8, value, "DataView.prototype.setInt8");
            return UNDEFINED;
        }

        /**
         * 24.2.4.18 DataView.prototype.setUint8(byteOffset, value)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @return the undefined value
         */
        @Function(name = "setUint8", arity = 2)
        public static Object setUint8(ExecutionContext cx, Object thisValue, Object byteOffset, Object value) {
            SetViewValue(cx, thisValue, byteOffset, true, ElementType.Uint8, value, "DataView.prototype.setUint8");
            return UNDEFINED;
        }

        /**
         * 24.2.4.16 DataView.prototype.setInt16(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setInt16", arity = 2)
        public static Object setInt16(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int16, value,
                    "DataView.prototype.setInt16");
            return UNDEFINED;
        }

        /**
         * 24.2.4.19 DataView.prototype.setUint16(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setUint16", arity = 2)
        public static Object setUint16(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint16, value,
                    "DataView.prototype.setUint16");
            return UNDEFINED;
        }

        /**
         * 24.2.4.17 DataView.prototype.setInt32(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setInt32", arity = 2)
        public static Object setInt32(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Int32, value,
                    "DataView.prototype.setInt32");
            return UNDEFINED;
        }

        /**
         * 24.2.4.20 DataView.prototype.setUint32(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setUint32", arity = 2)
        public static Object setUint32(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Uint32, value,
                    "DataView.prototype.setUint32");
            return UNDEFINED;
        }

        /**
         * 24.2.4.13 DataView.prototype.setFloat32(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setFloat32", arity = 2)
        public static Object setFloat32(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float32, value,
                    "DataView.prototype.setFloat32");
            return UNDEFINED;
        }

        /**
         * 24.2.4.14 DataView.prototype.setFloat64(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setFloat64", arity = 2)
        public static Object setFloat64(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.Float64, value,
                    "DataView.prototype.setFloat64");
            return UNDEFINED;
        }

        /**
         * 24.2.4.21 DataView.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "DataView";
    }

    /**
     * Extension: BigInt
     */
    @CompatibilityExtension(CompatibilityOption.BigInt)
    public enum Properties64 {
        ;

        /**
         * DataView.prototype.getBigInt64(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the int64 value from the requested byte offset
         */
        @Function(name = "getBigInt64", arity = 1)
        public static Object getBigInt64(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.BigInt64,
                    "DataView.prototype.getBigInt64");
        }

        /**
         * DataView.prototype.getBigUint64(byteOffset [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param littleEndian
         *            the little endian flag
         * @return the uint64 value from the requested byte offset
         */
        @Function(name = "getBigUint64", arity = 1)
        public static Object getBigUint64(ExecutionContext cx, Object thisValue, Object byteOffset,
                Object littleEndian) {
            return GetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.BigUint64,
                    "DataView.prototype.getBigUint64");
        }

        /**
         * DataView.prototype.setBigInt64(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setBigInt64", arity = 2)
        public static Object setBigInt64(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.BigInt64, value,
                    "DataView.prototype.setBigInt64");
            return UNDEFINED;
        }

        /**
         * DataView.prototype.setBigUint64(byteOffset, value [, littleEndian ])
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param byteOffset
         *            the byte offset
         * @param value
         *            the new value
         * @param littleEndian
         *            the little endian flag
         * @return the undefined value
         */
        @Function(name = "setBigUint64", arity = 2)
        public static Object setBigUint64(ExecutionContext cx, Object thisValue, Object byteOffset, Object value,
                Object littleEndian) {
            SetViewValue(cx, thisValue, byteOffset, littleEndian, ElementType.BigUint64, value,
                    "DataView.prototype.setBigUint64");
            return UNDEFINED;
        }
    }
}
