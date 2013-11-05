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
import static com.github.anba.es6draft.runtime.objects.binary.ArrayBufferConstructor.*;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

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
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.2 TypedArray Objects</h2>
 * <ul>
 * <li>22.2.1 The %TypedArray% Intrinsic Object
 * <li>22.2.2 Properties of the %TypedArray% Intrinsic Object
 * </ul>
 */
public class TypedArrayConstructorPrototype extends BuiltinFunction implements Initialisable {
    public TypedArrayConstructorPrototype(Realm realm) {
        super(realm, "TypedArray");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 22.2.1.1 %TypedArray% ( length )<br>
     * 22.2.1.2 %TypedArray% ( typedArray )<br>
     * 22.2.1.3 %TypedArray% ( array )<br>
     * 22.2.1.4 %TypedArray% ( buffer, byteOffset=0, length=undefined )<br>
     * 22.2.1.5 %TypedArray% ( binary data stuff ) [TODO]<br>
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
     * 22.2.1.1 %TypedArray% ( length )
     */
    private Object callWithLength(ExecutionContext cx, Object thisValue, Object length) {
        /* step 1 */
        assert !Type.isObject(length);
        /* step 2 */
        Object obj = thisValue;
        /* steps 3-4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getElementType() == null) {
            throw throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throw throwTypeError(cx, Messages.Key.InitialisedObject);
        }
        /* steps 8-9 */
        ElementType elementType = array.getElementType();
        // FIXME: spec issue? - undefined length is same as 0 for bwcompat?
        if (Type.isUndefined(length)) {
            length = 0;
        }
        /* step 10 */
        double numberLength = ToNumber(cx, length);
        /* steps 11-12 */
        long elementLength = ToLength(numberLength);
        /* step 13 */
        if (numberLength != elementLength) {
            throw throwRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* steps 14-16 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, Intrinsics.ArrayBuffer);
        /* step 16 */
        int elementSize = elementType.size();
        /* step 17 */
        long byteLength = elementSize * elementLength;
        /* steps 18-19 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 20-23 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 24 */
        return array;
    }

    /**
     * 22.2.1.2 %TypedArray% ( typedArray )
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
        if (array.getElementType() == null) {
            throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.InitialisedObject);
        }
        /* step 8 */
        if (srcArray.getBuffer() == null) {
            throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* steps 9-10 */
        ElementType elementType = array.getElementType();
        /* step 11 */
        long elementLength = srcArray.getArrayLength();
        /* steps 12-13 */
        ElementType srcType = srcArray.getElementType();
        /* step 14 */
        int srcElementSize = srcType.size();
        /* step 15 */
        ArrayBufferObject srcData = srcArray.getBuffer();
        /* step 16 */
        long srcByteOffset = srcArray.getByteOffset();
        /* step 17 */
        int elementSize = elementType.size();
        /* step 18 */
        long byteLength = elementSize * elementLength;
        /* steps 19-21 */
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* steps 22-23 */
        ArrayBufferObject data;
        if (elementType == srcType) {
            /* step 22 */
            data = CloneArrayBuffer(cx, srcData, srcByteOffset);
        } else {
            /* step 23 */
            data = AllocateArrayBuffer(cx, Intrinsics.ArrayBuffer);
            SetArrayBufferData(cx, data, byteLength);
            long srcByteIndex = srcByteOffset;
            long targetByteIndex = 0;
            for (long count = elementLength; count > 0; --count) {
                double value = GetValueFromBuffer(cx, srcData, srcByteIndex, srcType);
                SetValueInBuffer(cx, data, targetByteIndex, elementType, value);
                srcByteIndex += srcElementSize;
                targetByteIndex += elementSize;
            }
        }
        // FIXME: side-effects -> CloneArrayBuffer
        /* step 24 */
        array.setBuffer(data);
        /* step 25 */
        return array;
    }

    /**
     * 22.2.1.3 %TypedArray% ( array )
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
        if (array.getElementType() == null) {
            throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.InitialisedObject);
        }
        /* steps 8-9 */
        ElementType elementType = array.getElementType();
        /* step 10 */
        Object arrayLength = Get(cx, srcArray, "length");
        /* steps 11-12 */
        long elementLength = ToLength(cx, arrayLength);
        /* step 13 */
        if (elementLength < 0) {
            throwRangeError(cx, Messages.Key.InvalidBufferSize);
        }
        /* steps 14-15 */
        ArrayBufferObject data = AllocateArrayBuffer(cx, Intrinsics.ArrayBuffer);
        /* step 16 */
        int elementSize = elementType.size();
        /* step 17 */
        long byteLength = elementSize * elementLength;
        /* steps 18-19 */
        SetArrayBufferData(cx, data, byteLength);
        /* steps 20-21 */
        for (long k = 0; k < elementLength; ++k) {
            String pk = ToString(k);
            Object kValue = Get(cx, srcArray, pk);
            double kNumber = ToNumber(cx, kValue);
            SetValueInBuffer(cx, data, k * elementSize, elementType, kNumber);
        }
        /* steps 22-23 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.InitialisedObject);
        }
        /* steps 24-27 */
        array.setBuffer(data);
        array.setByteLength(byteLength);
        array.setByteOffset(0);
        array.setArrayLength(elementLength);
        /* step 28 */
        return array;
    }

    /**
     * 22.2.1.4 %TypedArray% ( buffer, byteOffset=0, length=undefined )
     */
    private Object callWithArrayBuffer(ExecutionContext cx, Object thisValue,
            ArrayBufferObject buffer, Object byteOffset, Object length) {
        /* step 1 (implicit) */
        /* step 2 */
        Object obj = thisValue;
        /* step 3 */
        if (buffer.getData() == null) {
            throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* step 4 */
        if (!(obj instanceof TypedArrayObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        TypedArrayObject array = (TypedArrayObject) obj;
        /* step 5 */
        if (array.getElementType() == null) {
            throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        /* steps 6-7 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.InitialisedObject);
        }
        /* steps 8-9 */
        ElementType elementType = array.getElementType();
        /* step 10 */
        int elementSize = elementType.size();
        /* steps 11-12 */
        double offset = ToInteger(cx, byteOffset);
        /* step 13 */
        if (offset < 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 14 */
        if (offset % elementSize != 0) {
            throwRangeError(cx, Messages.Key.InvalidByteOffset);
        }
        /* step 15 */
        long bufferByteLength = buffer.getByteLength();
        /* step 16 */
        // if (offset + elementSize >= bufferByteLength) {
        // throwRangeError(cx, Messages.Key.InvalidBufferSize);
        // }
        /* steps 17-18 */
        long newByteLength;
        if (Type.isUndefined(length)) {
            /* step 17 */
            if (bufferByteLength % elementSize != 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = (long) (bufferByteLength - offset);
            if (newByteLength < 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        } else {
            /* step 18 */
            long newLength = ToLength(cx, length);
            if (newLength < 0) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
            newByteLength = newLength * elementSize;
            if (offset + newByteLength > bufferByteLength) {
                throwRangeError(cx, Messages.Key.InvalidBufferSize);
            }
        }
        /* step 19 */
        if (array.getBuffer() != null) {
            throwTypeError(cx, Messages.Key.InitialisedObject);
        }
        /* steps 20-23 */
        array.setBuffer(buffer);
        array.setByteLength(newByteLength);
        array.setByteOffset((long) offset);
        array.setArrayLength(newByteLength / elementSize);
        /* step 24 */
        return array;
    }

    /**
     * 22.2.2 Properties of the %TypedArray% Intrinsic Object
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
        public static final String name = "TypedArray";

        /**
         * 22.2.2.3 %TypedArray%.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.TypedArrayPrototype;

        /**
         * 22.2.2.2 %TypedArray%.of ( ...items )
         */
        @Function(name = "of", arity = 0)
        public static Object of(ExecutionContext cx, Object thisValue, Object... items) {
            /* steps 1-2 */
            int len = items.length;
            /* step 3 */
            Object c = thisValue;
            /* step 5 */
            if (!IsConstructor(c)) {
                throwTypeError(cx, Messages.Key.NotConstructor);
            }
            /* step 4 */
            ScriptObject newObj = ((Constructor) c).construct(cx, len);
            /* steps 6-7 */
            for (int k = 0; k < len; ++k) {
                String pk = ToString(k);
                Object value = items[k];
                Put(cx, newObj, pk, value, true);
            }
            /* step 8 */
            return newObj;
        }

        /**
         * 22.2.2.1 %TypedArray%.from ( source, mapfn=undefined, thisArg=undefined )
         */
        @Function(name = "from", arity = 1)
        public static Object from(ExecutionContext cx, Object thisValue, Object source,
                Object mapfn, Object thisArg) {
            /* step 1 */
            Object c = thisValue;
            /* step 2 */
            if (!IsConstructor(c)) {
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
                for (;;) {
                    ScriptObject next = IteratorStep(cx, iterator);
                    if (next == null) {
                        break;
                    }
                    Object nextValue = IteratorValue(cx, next);
                    values.add(nextValue);
                }
                int len = values.size();
                ScriptObject newObj = ((Constructor) c).construct(cx, len);
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
            ScriptObject newObj = ((Constructor) c).construct(cx, len);
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
         * 22.2.3.4 %TypedArray%[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            /* step 1 */
            Object f = thisValue;
            /* step 2 */
            if (!Type.isObject(f)) {
                throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 3-4 */
            ScriptObject proto = GetPrototypeFromConstructor(cx, f, Intrinsics.TypedArrayPrototype);
            /* steps 5 */
            TypedArrayObject obj = new TypedArrayObject(cx.getRealm());
            obj.setPrototype(proto);
            /* steps 6-7 (implicit) */
            // obj.setBuffer(null);
            // obj.setElementType(null);
            /* steps 8-10 */
            obj.setByteLength(0);
            obj.setByteOffset(0);
            obj.setArrayLength(0);
            /* step 11 */
            return obj;
        }
    }
}
