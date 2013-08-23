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
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.AllocateArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.CloneArrayBuffer;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetArrayBufferData;
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.SetValueInBuffer;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.13 Binary Data Objects</h2><br>
 * <h3>15.13.6 TypedArray Objects</h3>
 * <ul>
 * <li>15.13.6.1 The %TypedArray% Intrinsic Object
 * <li>15.13.6.2 Properties of the %TypedArray% Intrinsic Object
 * </ul>
 */
public class TypedArrayConstructorPrototype extends BuiltinFunction implements Initialisable {
    public TypedArrayConstructorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.13.6.1.1 %TypedArray% ( length )<br>
     * 15.13.6.1.2 %TypedArray% ( typedArray )<br>
     * 15.13.6.1.3 %TypedArray% ( array )<br>
     * 15.13.6.1.4 %TypedArray% ( buffer, byteOffset=0, length=undefined )<br>
     * 15.13.6.1.5 %TypedArray% ( binary data stuff ) [TODO]<br>
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object arg0 = args.length > 0 ? args[0] : UNDEFINED;
        // FIXME: spec bug - constructor selection is unclear
        if (!Type.isObject(arg0)) {
            return callWithLength(calleeContext, thisValue, arg0);
        }
        if (arg0 instanceof TypedArrayObject) {
            return callWithTypedArray(calleeContext, thisValue, (TypedArrayObject) arg0);
        }
        if (arg0 instanceof ArrayBufferObject) {
            Object byteOffset = args.length > 1 ? args[1] : 0;
            Object length = args.length > 2 ? args[2] : UNDEFINED;
            return callWithArrayBuffer(calleeContext, thisValue, (ArrayBufferObject) arg0,
                    byteOffset, length);
        }
        return callWithArray(calleeContext, thisValue, (ScriptObject) arg0);
    }

    /**
     * 15.13.6.1.1 %TypedArray% ( length )
     */
    private Object callWithLength(ExecutionContext cx, Object thisValue, Object length) {
        /* step 1 */
        assert !Type.isObject(length);
        /* step 2 (empty) */
        /* step 3 */
        Object obj = thisValue;
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 5-7 */
        ElementType elementType = array.getElementType();
        // FIXME: spec issue? - undefined length is same as 0 for bwcompat?
        if (Type.isUndefined(length)) {
            length = 0;
        }
        /* steps 8-11 */
        double numberLength = ToNumber(cx, length);
        double elementLength = ToInteger(numberLength);
        if (numberLength != elementLength || elementLength < 0) {
            throwRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* step 12-13 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, cx.getIntrinsic(Intrinsics.ArrayBuffer));
        /* step 14 */
        int elementSize = elementType.size();
        /* step 15 */
        long byteLength = (long) (elementSize * elementLength);
        /* steps 16-17 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 18-21 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength((long) elementLength);
        /* step 22 */
        return array;
    }

    /**
     * 15.13.6.1.2 %TypedArray% ( typedArray )
     */
    private Object callWithTypedArray(ExecutionContext cx, Object thisValue,
            TypedArrayObject typedArray) {
        /* FIXME: missing check in spec */
        if (typedArray.getBuffer() == null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        /* step 1 (implicit) */
        /* step 2 */
        TypedArrayObject srcArray = typedArray;
        /* step 3 */
        Object obj = thisValue;
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 5-7 */
        ElementType elementType = array.getElementType();
        /* step 8 */
        long elementLength = srcArray.getArrayLength();
        /* FIXME: incomplete spec */
        long srcByteOffset = srcArray.getByteOffset();
        /* step 9-10 */
        ElementType srcType = srcArray.getElementType();
        /* step 11 */
        ArrayBufferObject srcData = srcArray.getBuffer();
        /* steps 12-13 */
        ArrayBufferObject data = CloneArrayBuffer(cx, srcData, srcType, elementType, srcByteOffset,
                elementLength);
        /* step 14 */
        int elementSize = elementType.size();
        /* step 15 */
        long byteLength = elementSize * elementLength;
        // FIXME: spec bug (remove this call <-> CloneArrayBuffer?)
        /* steps 16-17 */
        // data = SetArrayBufferData(realm, data, byteLength);
        /* step 18 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 19-22 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 23 */
        return array;
    }

    /**
     * 15.13.6.1.3 %TypedArray% ( array )
     */
    private Object callWithArray(ExecutionContext cx, Object thisValue, ScriptObject _array) {
        /* step 1 */
        assert !(_array instanceof TypedArrayObject || _array instanceof ArrayBufferObject);
        /* step 2 */
        Object obj = thisValue;
        if (!(Type.isObject(obj) || Type.isUndefined(obj))) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        // FIXME: spec bug (variable srcArray unused)
        /* step 3 */
        @SuppressWarnings("unused")
        ScriptObject srcArray = _array;
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 5-7 */
        ElementType elementType = array.getElementType();
        /* step 8 */
        Object arrayLength = Get(cx, _array, "length");
        // FIXME: spec issue? - undefined length is same as 0 for bwcompat?
        if (Type.isUndefined(arrayLength)) {
            arrayLength = 0;
        }
        /* steps 9-12 */
        double numberLength = ToNumber(cx, arrayLength);
        double elementLength = ToInteger(numberLength);
        if (numberLength != elementLength || elementLength < 0) {
            throwRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* steps 13-14 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, cx.getIntrinsic(Intrinsics.ArrayBuffer));
        /* step 15 */
        int elementSize = elementType.size();
        /* step 16 */
        long byteLength = (long) (elementSize * elementLength);
        /* step 17 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 18-19 */
        for (long k = 0; k < elementLength; ++k) {
            String pk = ToString(k);
            // FIXME: spec bug (`Get(array, Pk)` instead of `Get(O, Pk)`)
            Object kValue = Get(cx, _array, pk);
            double kNumber = ToNumber(cx, kValue);
            SetValueInBuffer(cx, data, k * elementSize, elementType, kNumber);
        }
        /* step 20 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 21-24 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength((long) elementLength);
        /* step 25 */
        return array;
    }

    /**
     * 15.13.6.1.4 %TypedArray% ( buffer, byteOffset=0, length=undefined )
     */
    private Object callWithArrayBuffer(ExecutionContext cx, Object thisValue,
            ArrayBufferObject buffer, Object byteOffset, Object length) {
        /* FIXME: missing check in spec (?) */
        if (buffer.getData() == null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        /* step 1 (implicit) */
        /* step 2 */
        Object obj = thisValue;
        /* step 3 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 4-6 */
        ElementType elementType = array.getElementType();
        /* step 7 */
        int elementSize = elementType.size();
        /* step 8 */
        assert array.getBuffer() == null;
        /* steps 9-10 */
        double offset = ToInteger(cx, byteOffset);
        /* step 11 */
        if (offset < 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 12 */
        if (offset % elementSize != 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 13 */
        long bufferByteLength = buffer.getByteLength();
        /* steps 15-16 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 15 */
            if (bufferByteLength % elementSize != 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = (long) (bufferByteLength - offset);
            if (newByteLength < 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        } else {
            /* step 16 */
            double numberLength = ToNumber(cx, length);
            double newLength = ToInteger(numberLength);
            if (numberLength != newLength || newLength < 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = (long) (newLength * elementSize);
            if (offset + newByteLength > bufferByteLength) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        }
        /* step 17 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 18-21 */
        array.setBuffer(buffer);
        array.setByteLength(newByteLength);
        array.setByteOffset((long) offset);
        array.setArrayLength(newByteLength / elementSize);
        /* step 22 */
        return array;
    }

    @SuppressWarnings("unchecked")
    private static <FUNCTION extends ScriptObject & Callable & Constructor> FUNCTION toFunction(
            Object f) {
        return (FUNCTION) f;
    }

    /**
     * 15.13.6.2 Properties of the %TypedArray% Intrinsic Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 3;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "TypedArray";

        /**
         * 15.13.6.2.1 %TypedArray%.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.TypedArrayPrototype;

        /**
         * 15.13.6.2.2 %TypedArray%.of ( ...items )
         */
        @Function(name = "of", arity = 0)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            int len = items.length;
            if (!IsConstructor(thisValue) || !IsCallable(thisValue)) {
                throwTypeError(cx, Messages.Key.NotConstructor);
            }
            ScriptObject array = OrdinaryConstruct(cx, toFunction(thisValue), new Object[] { len });
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object value = items[k];
                Put(cx, array, pk, value, true);
            }
            return array;
        }

        /**
         * 15.13.6.3.4 %TypedArray%[ @@create ] ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof TypedArrayConstructor)) {
                throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 (out-of-order) */
            TypedArrayConstructor f = (TypedArrayConstructor) thisValue;
            /* steps 4-5 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, f, Intrinsics.TypedArrayPrototype);
            /* steps 6-7 (implicit) */
            TypedArrayObject obj = new TypedArrayObject(cx.getRealm());
            obj.setPrototype(proto);
            /* step 8  */
            obj.setElementType(f.getElementType());
            /* steps 9-11 */
            obj.setByteLength(0);
            obj.setByteOffset(0);
            obj.setArrayLength(0);
            /* step 12 */
            return obj;
        }
    }
}
