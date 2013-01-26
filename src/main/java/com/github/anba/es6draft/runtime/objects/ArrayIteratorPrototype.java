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
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.4 Array Objects</h2>
 * <ul>
 * <li>15.4.6 Array Iterator Object Structure
 * </ul>
 */
public class ArrayIteratorPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public ArrayIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 15.4.6.3 Properties of Array Iterator Instances
     */
    public enum ArrayIterationKind {
        Key, Value, KeyValue, SparseKey, SparseValue, SparseKeyValue
    }

    /**
     * 15.4.6.3 Properties of Array Iterator Instances
     */
    private static class ArrayIterator extends OrdinaryObject {
        /**
         * [[IteratedObject]]
         */
        Scriptable iteratedObject;

        /**
         * [[ArrayIteratorNextIndex]]
         */
        long nextIndex;

        /**
         * [[ArrayIterationKind]]
         */
        ArrayIterationKind kind;

        ArrayIterator(Realm realm) {
            super(realm);
        }
    }

    /**
     * 15.4.6.1 CreateArrayIterator Abstract Operation
     */
    public static OrdinaryObject CreateArrayIterator(Realm realm, Scriptable array,
            ArrayIterationKind kind) {
        // ObjectCreate()
        ArrayIterator itr = new ArrayIterator(realm);
        itr.setPrototype(realm.getIntrinsic(Intrinsics.ArrayIteratorPrototype));
        itr.iteratedObject = array;
        itr.nextIndex = 0;
        itr.kind = kind;
        return itr;
    }

    /**
     * 15.4.6.2 The Array Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.4.6.2.1 ArrayIterator.prototype.constructor FIXME: spec bug (no description)
         */
        @Value(name = "constructor")
        public static final Object constructor = UNDEFINED;

        /**
         * 15.4.6.2.2 ArrayIterator.prototype.next( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(Realm realm, Object thisValue) {
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(realm, "");
            }
            if (!(thisValue instanceof ArrayIterator)) {
                throw throwTypeError(realm, "");
            }
            ArrayIterator itr = (ArrayIterator) thisValue;
            Scriptable array = itr.iteratedObject;
            long index = itr.nextIndex;
            ArrayIterationKind itemKind = itr.kind;
            Object lenValue = Get(array, "length");
            long len = ToUint32(realm, lenValue);

            // index == +Infinity => index == -1
            if (index < 0) {
                return _throw(realm.getIntrinsic(Intrinsics.StopIteration));
            }

            if (itemKind == ArrayIterationKind.SparseKey
                    || itemKind == ArrayIterationKind.SparseValue
                    || itemKind == ArrayIterationKind.SparseKeyValue) {
                boolean found = false;
                while (!found && index < len) {
                    String elementKey = ToString(index);
                    found = HasProperty(array, elementKey);
                    if (!found) {
                        index += 1;
                    }
                }
            }
            if (index >= len) {
                index = -1; // actually +Infinity!
                return _throw(realm.getIntrinsic(Intrinsics.StopIteration));
            }
            String elementKey = ToString(index);
            itr.nextIndex = index + 1;
            Object elementValue = null;
            if (itemKind == ArrayIterationKind.Value || itemKind == ArrayIterationKind.KeyValue
                    || itemKind == ArrayIterationKind.SparseValue
                    || itemKind == ArrayIterationKind.SparseKeyValue) {
                elementValue = Get(array, elementKey);
            }
            if (itemKind == ArrayIterationKind.KeyValue
                    || itemKind == ArrayIterationKind.SparseKeyValue) {
                assert elementValue != null;
                Scriptable result = ArrayCreate(realm, 2);
                result.defineOwnProperty("0", new PropertyDescriptor(elementKey, true, true, true));
                result.defineOwnProperty("1",
                        new PropertyDescriptor(elementValue, true, true, true));
                return result;
            } else if (itemKind == ArrayIterationKind.Key
                    || itemKind == ArrayIterationKind.SparseKey) {
                return elementKey;
            } else {
                // FIXME: spec bug (wrong assertion, itemKind is not "value" -> it's either "value"
                // or "sparse-value")
                assert itemKind == ArrayIterationKind.Value
                        || itemKind == ArrayIterationKind.SparseValue;
                assert elementValue != null;
                return elementValue;
            }
        }

        /**
         * 15.4.6.2.3 ArrayIterator.prototype.@@iterator ()
         */
        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(Realm realm, Object thisValue) {
            return thisValue;
        }

        /**
         * 15.4.6.2.4 ArrayIterator.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Array Iterator";
    }
}
