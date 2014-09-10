/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.GetValueFromBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.IsDetachedBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.2 DataView Objects</h2>
 * <ul>
 * <li>24.2.1 Abstract Operations For DataView Objects
 * <li>24.2.2 The DataView Constructor
 * <li>24.2.3 Properties of the DataView Constructor
 * </ul>
 */
public final class DataViewConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new DataView constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public DataViewConstructor(Realm realm) {
        super(realm, "DataView");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    @Override
    public DataViewConstructor clone() {
        return new DataViewConstructor(getRealm());
    }

    private static final class DataViewObjectAllocator implements ObjectAllocator<DataViewObject> {
        static final ObjectAllocator<DataViewObject> INSTANCE = new DataViewObjectAllocator();

        @Override
        public DataViewObject newInstance(Realm realm) {
            return new DataViewObject(realm);
        }
    }

    /**
     * 24.2.1 Abstract Operations For DataView Objects <br>
     * 24.2.1.1 GetViewValue(view, requestIndex, isLittleEndian, type)
     * 
     * @param cx
     *            the execution context
     * @param view
     *            the data view object
     * @param requestIndex
     *            the element index
     * @param isLittleEndian
     *            the little endian flag
     * @param type
     *            the element type
     * @return the view element value
     */
    public static double GetViewValue(ExecutionContext cx, Object view, Object requestIndex,
            Object isLittleEndian, ElementType type) {
        /* steps 1-2 */
        if (!(view instanceof DataViewObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        DataViewObject dataView = (DataViewObject) view;
        /* step 3 */
        double numberIndex = ToNumber(cx, requestIndex);
        /* steps 4-5 */
        double getIndex = ToInteger(numberIndex);
        /* step 6 */
        if (numberIndex != getIndex || getIndex < 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* steps 7-8 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 9 */
        ArrayBufferObject buffer = dataView.getBuffer();
        /* step 10 */
        if (buffer == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 11 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 12 */
        long viewOffset = dataView.getByteOffset();
        /* step 13 */
        long viewSize = dataView.getByteLength();
        /* step 14 */
        int elementSize = type.size();
        /* step 15 */
        if (getIndex + elementSize > viewSize) {
            throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 16 */
        long bufferIndex = (long) getIndex + viewOffset;
        /* step 17 */
        return GetValueFromBuffer(buffer, bufferIndex, type, littleEndian);
    }

    /**
     * 24.2.1 Abstract Operations For DataView Objects <br>
     * 24.2.1.2 SetViewValue(view, requestIndex, isLittleEndian, type, value)
     * 
     * @param cx
     *            the execution context
     * @param view
     *            the data view object
     * @param requestIndex
     *            the element index
     * @param isLittleEndian
     *            the little endian flag
     * @param type
     *            the element type
     * @param value
     *            the new view element value
     */
    public static void SetViewValue(ExecutionContext cx, Object view, Object requestIndex,
            Object isLittleEndian, ElementType type, Object value) {
        /* steps 1-2 */
        if (!(view instanceof DataViewObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        DataViewObject dataView = (DataViewObject) view;
        /* step 3 */
        double numberIndex = ToNumber(cx, requestIndex);
        /* steps 4-5 */
        double getIndex = ToInteger(numberIndex);
        /* step 6 */
        if (numberIndex != getIndex || getIndex < 0) {
            throw newRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* steps 7-8 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 9 */
        ArrayBufferObject buffer = dataView.getBuffer();
        /* step 10 */
        if (buffer == null) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 11 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 11 */
        long viewOffset = dataView.getByteOffset();
        /* step 12 */
        long viewSize = dataView.getByteLength();
        /* step 13 */
        int elementSize = type.size();
        /* step 14 */
        if (getIndex + elementSize > viewSize) {
            throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 15 */
        long bufferIndex = (long) getIndex + viewOffset;
        /* step 16 */
        SetValueInBuffer(buffer, bufferIndex, type, ToNumber(cx, value), littleEndian);
    }

    /**
     * 24.2.2.1 DataView (buffer [, byteOffset [, byteLength ] ])
     */
    @Override
    public DataViewObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object buffer = argument(args, 0);
        Object byteOffset = argument(args, 1, 0);
        Object byteLength = argument(args, 2);
        /* step 1 (implicit) */
        /* step 2 */
        if (!(thisValue instanceof DataViewObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        DataViewObject dataView = (DataViewObject) thisValue;
        /* step 3 (not applicable) */
        /* step 4 */
        if (dataView.getBuffer() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* steps 5-6 */
        if (!(buffer instanceof ArrayBufferObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        ArrayBufferObject bufferObj = (ArrayBufferObject) buffer;
        /* step 7 */
        if (!bufferObj.isInitialized()) {
            throw newTypeError(calleeContext, Messages.Key.UninitializedObject);
        }
        /* step 8 */
        double numberOffset = ToNumber(calleeContext, byteOffset);
        /* steps 9-10 */
        double offset = ToInteger(numberOffset);
        /* step 11 */
        if (numberOffset != offset || offset < 0) {
            throw newRangeError(calleeContext, Messages.Key.InvalidByteOffset);
        }
        /* step 12 */
        long bufferByteLength = bufferObj.getByteLength();
        /* step 13 */
        if (offset > bufferByteLength) {
            throw newRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* steps 14-15 */
        long viewByteLength, viewByteOffset = (long) offset;
        if (Type.isUndefined(byteLength)) {
            viewByteLength = bufferByteLength - viewByteOffset;
        } else {
            double numberLength = ToNumber(calleeContext, byteLength);
            // TODO: call ToLength() instead of ToInteger() in spec?
            double viewLength = ToInteger(numberLength);
            if (numberLength != viewLength || viewLength < 0) {
                throw newRangeError(calleeContext, Messages.Key.InvalidByteLength);
            }
            viewByteLength = (long) viewLength;
            if (offset + viewByteLength > bufferByteLength) {
                throw newRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
            }
        }
        /* step 16 */
        if (dataView.getBuffer() != null) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }
        /* steps 17-19 */
        dataView.setBuffer(bufferObj);
        dataView.setByteLength(viewByteLength);
        dataView.setByteOffset(viewByteOffset);
        /* step 20 */
        return dataView;
    }

    /**
     * 24.2.2.2 new DataView( ... argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 24.2.3 Properties of the DataView Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "DataView";

        /**
         * 24.2.3.1 DataView.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.DataViewPrototype;

        /**
         * 24.2.3.2 DataView [ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new uninitialized data view object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.DataViewPrototype,
                    DataViewObjectAllocator.INSTANCE);
        }
    }
}
