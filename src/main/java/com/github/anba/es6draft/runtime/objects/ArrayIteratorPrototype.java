/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateArrayFromList;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToLength;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>22 Indexed Collections</h1><br>
 * <h2>22.1 Array Objects</h2>
 * <ul>
 * <li>22.1.5 Array Iterator Objects
 * </ul>
 */
public final class ArrayIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Array Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public ArrayIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 22.1.5.3 Properties of Array Iterator Instances
     */
    public enum ArrayIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 22.1.5.3 Properties of Array Iterator Instances
     */
    private static final class ArrayIterator extends OrdinaryObject {
        /** [[IteratedObject]] */
        ScriptObject iteratedObject;

        /** [[ArrayIteratorNextIndex]] */
        long nextIndex;

        /** [[ArrayIterationKind]] */
        ArrayIterationKind kind;

        ArrayIterator(Realm realm) {
            super(realm);
        }
    }

    private static final class ArrayIteratorAllocator implements ObjectAllocator<ArrayIterator> {
        static final ObjectAllocator<ArrayIterator> INSTANCE = new ArrayIteratorAllocator();

        @Override
        public ArrayIterator newInstance(Realm realm) {
            return new ArrayIterator(realm);
        }
    }

    /**
     * 22.1.5.1 CreateArrayIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param array
     *            the array-like object
     * @param kind
     *            the array iteration kind
     * @return the new array iterator
     */
    public static OrdinaryObject CreateArrayIterator(ExecutionContext cx, ScriptObject array,
            ArrayIterationKind kind) {
        /* step 1 (not applicable) */
        /* steps 2-5 */
        ArrayIterator iterator = ObjectCreate(cx, Intrinsics.ArrayIteratorPrototype,
                ArrayIteratorAllocator.INSTANCE);
        iterator.iteratedObject = array;
        iterator.nextIndex = 0;
        iterator.kind = kind;
        /* step 6 */
        return iterator;
    }

    /**
     * Marker class for {@code %ArrayIteratorPrototype%.next}.
     */
    private static final class ArrayIteratorPrototypeNext {
    }

    public static boolean isBuiltinNext(Object next) {
        return next instanceof NativeFunction
                && ((NativeFunction) next).getId() == ArrayIteratorPrototypeNext.class;
    }

    /**
     * 22.1.5.2 The %ArrayIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * 22.1.5.2.1 %ArrayIteratorPrototype%.next( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the next iterator result object
         */
        @Function(name = "next", arity = 0, nativeId = ArrayIteratorPrototypeNext.class)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof ArrayIterator)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            ArrayIterator iter = (ArrayIterator) thisValue;
            /* step 4 */
            ScriptObject array = iter.iteratedObject;
            /* step 5 */
            if (array == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 6 */
            long index = iter.nextIndex;
            /* step 7 */
            ArrayIterationKind itemKind = iter.kind;
            /* steps 8-9 */
            long len;
            if (array instanceof TypedArrayObject) {
                len = ((TypedArrayObject) array).getArrayLength();
            } else {
                len = ToLength(cx, Get(cx, array, "length"));
            }
            /* step 10 */
            if (index >= len) {
                iter.iteratedObject = null;
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 11 */
            iter.nextIndex = index + 1;
            /* steps 12-17 */
            Object result;
            if (itemKind == ArrayIterationKind.Key) {
                /* step 12 */
                result = index;
            } else {
                /* step 13 */
                long elementKey = index;
                /* steps 14-15 */
                Object elementValue = Get(cx, array, elementKey);
                if (itemKind == ArrayIterationKind.Value) {
                    /* step 16 */
                    result = elementValue;
                } else {
                    /* step 17 */
                    assert itemKind == ArrayIterationKind.KeyValue;
                    result = CreateArrayFromList(cx, index, elementValue);
                }
            }
            /* steps 12, 18 */
            return CreateIterResultObject(cx, result, false);
        }

        /**
         * 22.1.5.2.2 %ArrayIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Array Iterator";
    }
}
