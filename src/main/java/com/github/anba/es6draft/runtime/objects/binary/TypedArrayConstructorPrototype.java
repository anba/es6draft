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
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.GetIterator;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorComplete;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorNext;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorValue;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import java.util.ArrayList;
import java.util.List;

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
        /* step 5 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* step 7 */
        if (srcArray.getBuffer() == null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 6, 8-9 */
        ElementType elementType = array.getElementType();
        /* step 10 */
        long elementLength = srcArray.getArrayLength();
        /* step 11-12 */
        ElementType srcType = srcArray.getElementType();
        /* step 13 */
        ArrayBufferObject srcData = srcArray.getBuffer();
        /* step 14 */
        long srcByteOffset = srcArray.getByteOffset();
        /* steps 15-16 */
        ArrayBufferObject data = CloneArrayBuffer(cx, srcData, srcByteOffset, srcType, elementType,
                elementLength);
        /* step 17 */
        int elementSize = elementType.size();
        /* step 18 */
        long byteLength = elementSize * elementLength;
        /* step 19 (empty) */
        /* steps 20-23 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 24 */
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
        /* step 3 */
        ScriptObject srcArray = _array;
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 6-8 */
        ElementType elementType = array.getElementType();
        /* step 9 */
        Object arrayLength = Get(cx, srcArray, "length");
        /* steps 10-12 */
        long elementLength = ToLength(cx, arrayLength);
        if (elementLength < 0) {
            throwRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* steps 13-14 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, cx.getIntrinsic(Intrinsics.ArrayBuffer));
        /* step 15 */
        int elementSize = elementType.size();
        /* step 16 */
        long byteLength = elementSize * elementLength;
        /* steps 17-18 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 19-20 */
        for (long k = 0; k < elementLength; ++k) {
            String pk = ToString(k);
            Object kValue = Get(cx, srcArray, pk);
            double kNumber = ToNumber(cx, kValue);
            SetValueInBuffer(cx, data, k * elementSize, elementType, kNumber);
        }
        /* step 21 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 22-25 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 26 */
        return array;
    }

    /**
     * 15.13.6.1.4 %TypedArray% ( buffer, byteOffset=0, length=undefined )
     */
    private Object callWithArrayBuffer(ExecutionContext cx, Object thisValue,
            ArrayBufferObject buffer, Object byteOffset, Object length) {
        /* step 1 (implicit) */
        /* step 2 */
        Object obj = thisValue;
        /* step 3 */
        if (buffer.getData() == null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 6-8 */
        ElementType elementType = array.getElementType();
        /* step 9 */
        int elementSize = elementType.size();
        /* steps 10-11 */
        double offset = ToInteger(cx, byteOffset);
        /* step 12 */
        if (offset < 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 13 */
        if (offset % elementSize != 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 14 */
        long bufferByteLength = buffer.getByteLength();
        /* step 15 */
        // if (offset + elementSize > bufferByteLength) {
        // throwRangeError(cx, Messages.Key.InvalidBufferSize);
        // }
        /* steps 16-17 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 16 */
            if (bufferByteLength % elementSize != 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = (long) (bufferByteLength - offset);
            if (newByteLength < 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            assert newByteLength >= 0;
        } else {
            /* step 17 */
            long newLength = ToLength(cx, length);
            if (newLength < 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = newLength * elementSize;
            if (offset + newByteLength > bufferByteLength) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        }
        /* step 18 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* steps 19-22 */
        array.setBuffer(buffer);
        array.setByteLength(newByteLength);
        array.setByteOffset((long) offset);
        array.setArrayLength(newByteLength / elementSize);
        /* step 23 */
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
            ScriptObject newObj = OrdinaryConstruct(cx, toFunction(thisValue), new Object[] { len });
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object value = items[k];
                Put(cx, newObj, pk, value, true);
            }
            return newObj;
        }

        /**
         * 15.13.6.2.3 %TypedArray%.from ( source, mapfn=undefined, thisArg=undefined )
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object source,
                Object mapfn, Object thisArg) {
            /* step 1 */
            Object c = thisValue;
            /* step 2 */
            if (!IsConstructor(c) || !IsCallable(thisValue)) {
                throwTypeError(cx, Messages.Key.NotConstructor);
            }
            /* steps 3-4 */
            ScriptObject items = ToObject(cx, source);
            /* steps 5-6 */
            Callable mapper = null;
            boolean mapping;
            if (Type.isUndefined(mapfn)) {
                mapping = false;
            } else {
                if (!IsCallable(mapfn)) {
                    throwTypeError(cx, Messages.Key.NotCallable);
                }
                mapping = true;
                mapper = (Callable) mapfn;
            }
            /* steps 7-8 */
            boolean usingIterator = HasProperty(cx, items, BuiltinSymbol.iterator.get());
            /* step 9 */
            if (usingIterator) {
                ScriptObject iterator = GetIterator(cx, items);
                List<Object> values = new ArrayList<>();
                boolean done = false;
                while (!done) {
                    ScriptObject next = IteratorNext(cx, iterator);
                    done = IteratorComplete(cx, next);
                    if (!done) {
                        Object nextValue = IteratorValue(cx, next);
                        values.add(nextValue);
                    }
                }
                int len = values.size();
                ScriptObject newObj = OrdinaryConstruct(cx, toFunction(c), new Object[] { len });
                for (int k = 0; k < len; ++k) {
                    String pk = ToString(k);
                    Object kValue = values.get(k);
                    Object mappedValue;
                    if (mapping) {
                        mappedValue = mapper.call(cx, thisArg, kValue);
                    } else {
                        mappedValue = kValue;
                    }
                    Put(cx, newObj, pk, mappedValue, true);
                }
                return newObj;
            }
            /* step 10 (?) */
            /* step 11 */
            Object lenValue = Get(cx, items, "length");
            /* steps 12-13 */
            long len = ToLength(cx, lenValue);
            /* steps 14-15 */
            ScriptObject newObj = OrdinaryConstruct(cx, toFunction(c), new Object[] { len });
            /* steps 16-17 */
            for (long k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object kValue = Get(cx, items, pk);
                Object mappedValue;
                if (mapping) {
                    mappedValue = mapper.call(cx, thisArg, kValue, k, items);
                } else {
                    mappedValue = kValue;
                }
                Put(cx, newObj, pk, mappedValue, true);
            }
            /* step 18 */
            return newObj;
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
            obj.preventExtensions(cx);
            /* step 13 */
            return obj;
        }
    }
}
