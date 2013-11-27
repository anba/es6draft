/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.2 Set Objects</h2>
 * <ul>
 * <li>23.2.5 Set Iterator Objects
 * </ul>
 */
public class SetIteratorPrototype extends OrdinaryObject implements Initialisable {
    public SetIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    public enum SetIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 23.2.5.3 Properties of Set Iterator Instances
     */
    private static class SetIterator extends OrdinaryObject {
        /** [[IteratedSet]] */
        SetObject set;

        /** [[SetNextIndex]] */
        @SuppressWarnings("unused")
        int nextIndex;

        /** [[SetIterationKind]] */
        SetIterationKind iterationKind;

        Iterator<Entry<Object, Void>> iterator;

        SetIterator(Realm realm) {
            super(realm);
        }
    }

    private static class SetIteratorAllocator implements ObjectAllocator<SetIterator> {
        static final ObjectAllocator<SetIterator> INSTANCE = new SetIteratorAllocator();

        @Override
        public SetIterator newInstance(Realm realm) {
            return new SetIterator(realm);
        }
    }

    /**
     * 23.2.5.1 CreateSetIterator Abstract Operation
     */
    public static OrdinaryObject CreateSetIterator(ExecutionContext cx, Object obj,
            SetIterationKind kind) {
        /* steps 1-2 */
        if (!(obj instanceof SetObject)) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        SetObject set = (SetObject) obj;
        /* step 3 */
        if (!set.isInitialised()) {
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        /* step 4 */
        LinkedMap<Object, Void> entries = set.getSetData();
        /* step 5 */
        SetIterator iterator = ObjectCreate(cx, Intrinsics.SetIteratorPrototype,
                SetIteratorAllocator.INSTANCE);
        /* steps 6-8 */
        iterator.set = set;
        iterator.nextIndex = 0;
        iterator.iterationKind = kind;
        iterator.iterator = entries.iterator();
        /* step 9 */
        return iterator;
    }

    /**
     * 23.2.5.2 The %SetIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.2.5.2.1 %SetIteratorPrototype%.next( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof SetIterator)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            SetIterator o = (SetIterator) thisValue;
            /* step 4 */
            SetObject s = o.set;
            /* step 5 */
            // int index = o.nextIndex;
            /* step 6 */
            SetIterationKind itemKind = o.iterationKind;
            /* step 7 */
            if (s == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 8 */
            assert s.getSetData() != null;
            /* step 9 */
            Iterator<Entry<Object, Void>> iter = o.iterator;
            /* step 10 */
            if (iter.hasNext()) {
                Entry<Object, Void> e = iter.next();
                assert e != null;
                if (itemKind == SetIterationKind.KeyValue) {
                    ExoticArray result = ArrayCreate(cx, 2);
                    CreateDataProperty(cx, result, "0", e.getKey());
                    CreateDataProperty(cx, result, "1", e.getKey());
                    return CreateIterResultObject(cx, result, false);
                }
                return CreateIterResultObject(cx, e.getKey(), false);
            }
            /* step 11 */
            o.set = null;
            /* step 12 */
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        /**
         * 23.2.5.2.2 %SetIteratorPrototype% [ @@iterator ]()
         */
        @Function(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 23.2.5.2.3 %SetIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Set Iterator";
    }
}
