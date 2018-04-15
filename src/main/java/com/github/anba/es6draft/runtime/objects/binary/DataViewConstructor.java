/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.binary;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToIndex;
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
        super(realm, "DataView", 1);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
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
     * @param method
     *            the method name
     * @return the view element value
     */
    public static Number GetViewValue(ExecutionContext cx, Object view, Object requestIndex, Object isLittleEndian,
            ElementType type, String method) {
        /* steps 1-2 */
        if (!(view instanceof DataViewObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(view).toString());
        }
        DataViewObject dataView = (DataViewObject) view;
        /* step 3 (not applicable) */
        /* step 4 */
        long getIndex = ToIndex(cx, requestIndex);
        /* step 5 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 6 */
        ArrayBuffer buffer = dataView.getBuffer();
        /* step 7 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 8 */
        long viewOffset = dataView.getByteOffset();
        /* step 9 */
        long viewSize = dataView.getByteLength();
        /* step 10 */
        int elementSize = type.size();
        /* step 11 */
        if (getIndex + elementSize > viewSize) {
            throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 12 */
        long bufferIndex = getIndex + viewOffset;
        /* step 13 */
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
     * @param method
     *            the method name
     */
    public static void SetViewValue(ExecutionContext cx, Object view, Object requestIndex, Object isLittleEndian,
            ElementType type, Object value, String method) {
        /* steps 1-2 */
        if (!(view instanceof DataViewObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(view).toString());
        }
        DataViewObject dataView = (DataViewObject) view;
        /* step 3 (not applicable) */
        /* step 4 */
        long getIndex = ToIndex(cx, requestIndex);
        /* step 5 */
        Number numberValue = type.toElementValue(cx, value);
        /* step 6 */
        boolean littleEndian = ToBoolean(isLittleEndian);
        /* step 7 */
        ArrayBuffer buffer = dataView.getBuffer();
        /* step 8 */
        if (IsDetachedBuffer(buffer)) {
            throw newTypeError(cx, Messages.Key.BufferDetached);
        }
        /* step 9 */
        long viewOffset = dataView.getByteOffset();
        /* step 10 */
        long viewSize = dataView.getByteLength();
        /* step 11 */
        int elementSize = type.size();
        /* step 12 */
        if (getIndex + elementSize > viewSize) {
            throw newRangeError(cx, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* step 13 */
        long bufferIndex = getIndex + viewOffset;
        /* step 14 */
        SetValueInBuffer(buffer, bufferIndex, type, numberValue, littleEndian);
    }

    /**
     * 24.2.2.1 DataView ( buffer [ , byteOffset [ , byteLength ] ] )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        /* step 1 */
        throw newTypeError(calleeContext(), Messages.Key.InvalidCall, "DataView");
    }

    /**
     * 24.2.2.1 DataView ( buffer [ , byteOffset [ , byteLength ] ] )
     */
    @Override
    public DataViewObject construct(ExecutionContext callerContext, Constructor newTarget, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object buffer = argument(args, 0);
        Object byteOffset = argument(args, 1);
        Object byteLength = argument(args, 2);
        /* step 1 (not applicable)*/
        /* steps 2-3 */
        if (!(buffer instanceof ArrayBuffer)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleArgument, "DataView",
                    Type.of(buffer).toString());
        }
        ArrayBuffer bufferObj = (ArrayBuffer) buffer;
        /* step 4 */
        long offset = ToIndex(calleeContext, byteOffset);
        /* step 5 */
        if (IsDetachedBuffer(bufferObj)) {
            throw newTypeError(calleeContext, Messages.Key.BufferDetached);
        }
        /* step 6 */
        long bufferByteLength = bufferObj.getByteLength();
        /* step 7 */
        if (offset > bufferByteLength) {
            throw newRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
        }
        /* steps 8-9 */
        long viewByteLength;
        if (Type.isUndefined(byteLength)) {
            /* step 8 */
            viewByteLength = bufferByteLength - offset;
        } else {
            /* step 9 */
            viewByteLength = ToIndex(calleeContext, byteLength);
            if (offset + viewByteLength > bufferByteLength) {
                throw newRangeError(calleeContext, Messages.Key.ArrayOffsetOutOfRange);
            }
        }
        /* PR1025 */
        ScriptObject prototype = GetPrototypeFromConstructor(calleeContext, newTarget, Intrinsics.DataViewPrototype);
        if (IsDetachedBuffer(bufferObj)) {
            throw newTypeError(calleeContext, Messages.Key.BufferDetached);
        }
        /* steps 10-15 */
        return new DataViewObject(calleeContext.getRealm(), bufferObj, viewByteLength, offset, prototype);
    }

    /**
     * 24.2.3 Properties of the DataView Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "DataView";

        /**
         * 24.2.3.1 DataView.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static final Intrinsics prototype = Intrinsics.DataViewPrototype;
    }
}
