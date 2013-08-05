/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.HasProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateItrResultObject;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.4 Array Objects</h2>
 * <ul>
 * <li>15.4.5 Array Iterator Object Structure
 * </ul>
 */
public class ArrayIteratorPrototype extends OrdinaryObject implements Initialisable {
    public ArrayIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.4.5.3 Properties of Array Iterator Instances
     */
    public enum ArrayIterationKind {
        Key, Value, KeyValue, SparseKey, SparseValue, SparseKeyValue
    }

    /**
     * 15.4.5.3 Properties of Array Iterator Instances
     */
    private static class ArrayIterator extends OrdinaryObject {
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

    private static class ArrayIteratorAllocator implements ObjectAllocator<ArrayIterator> {
        static final ObjectAllocator<ArrayIterator> INSTANCE = new ArrayIteratorAllocator();

        @Override
        public ArrayIterator newInstance(Realm realm) {
            return new ArrayIterator(realm);
        }
    }

    /**
     * 15.4.5.1 CreateArrayIterator Abstract Operation
     */
    public static OrdinaryObject CreateArrayIterator(ExecutionContext cx, ScriptObject array,
            ArrayIterationKind kind) {
        /* steps 1-2 (omitted) */
        /* steps 3-6 */
        ArrayIterator iterator = ObjectCreate(cx, Intrinsics.ArrayIteratorPrototype,
                ArrayIteratorAllocator.INSTANCE);
        iterator.iteratedObject = array;
        iterator.nextIndex = 0;
        iterator.kind = kind;
        /* step 7 */
        return iterator;
    }

    /**
     * 15.4.5.2 The Array Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.4.5.2.1 ArrayIterator.prototype.constructor FIXME: spec bug (no description)
         */
        @Value(name = "constructor")
        public static final Object constructor = UNDEFINED;

        /**
         * 15.4.5.2.2 ArrayIterator.prototype.next( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof ArrayIterator)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            ArrayIterator itr = (ArrayIterator) thisValue;
            /* step 4-6 */
            ScriptObject array = itr.iteratedObject;
            long index = itr.nextIndex;
            ArrayIterationKind itemKind = itr.kind;
            /* step 7 */
            Object lenValue = Get(cx, array, "length");
            /* step 8-9 */
            long len = ToUint32(cx, lenValue);
            /* step 10 */
            if (itemKind == ArrayIterationKind.SparseKey
                    || itemKind == ArrayIterationKind.SparseValue
                    || itemKind == ArrayIterationKind.SparseKeyValue) {
                boolean found = false;
                while (!found && index < len) {
                    String elementKey = ToString(index);
                    found = HasProperty(cx, array, elementKey);
                    if (!found) {
                        index += 1;
                    }
                }
            }
            /* step 11 */
            if (index >= len) {
                itr.nextIndex = Long.MAX_VALUE; // = +Infinity
                return CreateItrResultObject(cx, UNDEFINED, true);
            }
            /* step 12 */
            String elementKey = ToString(index);
            /* step 13 */
            itr.nextIndex = index + 1;
            /* step 14 */
            Object elementValue = null;
            if (itemKind == ArrayIterationKind.Value || itemKind == ArrayIterationKind.KeyValue
                    || itemKind == ArrayIterationKind.SparseValue
                    || itemKind == ArrayIterationKind.SparseKeyValue) {
                elementValue = Get(cx, array, elementKey);
            }
            if (itemKind == ArrayIterationKind.KeyValue
                    || itemKind == ArrayIterationKind.SparseKeyValue) {
                /* step 15 */
                assert elementValue != null;
                ScriptObject result = ArrayCreate(cx, 2);
                result.defineOwnProperty(cx, "0", new PropertyDescriptor(elementKey, true, true,
                        true));
                result.defineOwnProperty(cx, "1", new PropertyDescriptor(elementValue, true, true,
                        true));
                return CreateItrResultObject(cx, result, false);
            } else if (itemKind == ArrayIterationKind.Key
                    || itemKind == ArrayIterationKind.SparseKey) {
                /* step 16 */
                return CreateItrResultObject(cx, elementKey, false);
            } else {
                /* steps 17-18 */
                assert itemKind == ArrayIterationKind.Value
                        || itemKind == ArrayIterationKind.SparseValue;
                assert elementValue != null;
                return CreateItrResultObject(cx, elementValue, false);
            }
        }

        /**
         * 15.4.5.2.3 ArrayIterator.prototype.@@iterator ()
         */
        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 15.4.5.2.4 ArrayIterator.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Array Iterator";
    }
}
