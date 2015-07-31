/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToLength;
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
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
        super(realm, "DataView", 3);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public DataViewConstructor clone() {
        return new DataViewConstructor(getRealm());
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
        /* step 7 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 8 */
        ArrayBufferObject buffer = dataView.getBuffer();
        /* step 9 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 10 */
        long viewOffset = dataView.getByteOffset();
        /* step 11 */
        long viewSize = dataView.getByteLength();
        /* step 12 */
        int elementSize = type.size();
        /* step 13 */
        if (getIndex + elementSize > viewSize) {
            throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 14 */
        long bufferIndex = (long) getIndex + viewOffset;
        /* step 15 */
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
        /* step 7 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 8 */
        ArrayBufferObject buffer = dataView.getBuffer();
        /* step 9 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 10 */
        long viewOffset = dataView.getByteOffset();
        /* step 11 */
        long viewSize = dataView.getByteLength();
        /* step 12 */
        int elementSize = type.size();
        /* step 13 */
        if (getIndex + elementSize > viewSize) {
            throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 14 */
        long bufferIndex = (long) getIndex + viewOffset;
        /* step 15 */
        SetValueInBuffer(buffer, bufferIndex, type, ToNumber(cx, value), littleEndian);
    }

    /**
     * 24.2.2.1 DataView (buffer [, byteOffset [, byteLength ] ])
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "DataView");
    }

    /**
     * 24.2.2.1 DataView (buffer [, byteOffset [, byteLength ] ])
     */
    @Override
    public DataViewObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object buffer = argument(args, 0);
        // FIXME: spec bug - missing/undefined byteOffset parameter not handled.
        Object byteOffset = argument(args, 1, 0);
        Object byteLength = argument(args, 2);
        /* step 1 (not applicable)*/
        /* steps 2-3 */
        if (!(buffer instanceof ArrayBufferObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        ArrayBufferObject bufferObj = (ArrayBufferObject) buffer;
        /* step 4 */
        double numberOffset = ToNumber(calleeContext, byteOffset);
        /* steps 5-6 */
        double offset = ToInteger(numberOffset);
        /* step 7 */
        if (numberOffset != offset || offset < 0) {
            throw newRangeError(calleeContext, Messages.Key.InvalidByteOffset);
        }
        /* step 8 */
        if (IsDetachedBuffer(bufferObj)) {
            throw newTypeError(calleeContext, Messages.Key.BufferDetached);
        }
        /* step 9 */
        long bufferByteLength = bufferObj.getByteLength();
        /* step 10 */
        if (offset > bufferByteLength) {
            throw newRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* steps 11-12 */
        long viewByteLength, viewByteOffset = (long) offset;
        if (Type.isUndefined(byteLength)) {
            viewByteLength = bufferByteLength - viewByteOffset;
        } else {
            viewByteLength = ToLength(calleeContext, byteLength);
            if (offset + viewByteLength > bufferByteLength) {
                throw newRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
            }
        }
        /* steps 13-19 */
        return new DataViewObject(calleeContext.getRealm(), bufferObj, viewByteLength,
                viewByteOffset, GetPrototypeFromConstructor(calleeContext, newTarget,
                        Intrinsics.DataViewPrototype));
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
    }
}
