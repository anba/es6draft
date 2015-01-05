/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;

import java.util.Iterator;
import java.util.Map.Entry;

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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.1 Map Objects</h2>
 * <ul>
 * <li>23.1.5 Map Iterator Objects
 * </ul>
 */
public final class MapIteratorPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new Map Iterator prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public MapIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    public enum MapIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 23.1.5.3 Properties of Map Iterator Instances
     */
    private static final class MapIterator extends OrdinaryObject {
        /** [[Map]] */
        MapObject map;

        /** [[MapNextIndex]] */
        @SuppressWarnings("unused")
        int nextIndex;

        /** [[MapIterationKind]] */
        MapIterationKind iterationKind;

        Iterator<Entry<Object, Object>> iterator;

        MapIterator(Realm realm) {
            super(realm);
        }
    }

    private static final class MapIteratorAllocator implements ObjectAllocator<MapIterator> {
        static final ObjectAllocator<MapIterator> INSTANCE = new MapIteratorAllocator();

        @Override
        public MapIterator newInstance(Realm realm) {
            return new MapIterator(realm);
        }
    }

    /**
     * 23.1.5.1 CreateMapIterator Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param obj
     *            the map object
     * @param kind
     *            the map iteration kind
     * @return the new map iterator
     */
    public static OrdinaryObject CreateMapIterator(ExecutionContext cx, Object obj,
            MapIterationKind kind) {
        /* steps 1-2 */
        if (!(obj instanceof MapObject)) {
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }
        MapObject map = (MapObject) obj;
        /* step 3 */
        if (!map.isInitialized()) {
            throw newTypeError(cx, Messages.Key.UninitializedObject);
        }
        /* step 4 */
        MapIterator iterator = ObjectCreate(cx, Intrinsics.MapIteratorPrototype,
                MapIteratorAllocator.INSTANCE);
        /* steps 5-7 */
        iterator.map = map;
        iterator.nextIndex = 0;
        iterator.iterationKind = kind;
        iterator.iterator = map.getMapData().iterator();
        /* step 8 */
        return iterator;
    }

    /**
     * 23.1.5.2 The %MapIteratorPrototype% Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.IteratorPrototype;

        /**
         * 23.1.5.2.1 %MapIteratorPrototype%.next( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the next iterator result object
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof MapIterator)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            MapIterator o = (MapIterator) thisValue;
            /* step 4 */
            MapObject m = o.map;
            /* step 5 */
            // int index = o.nextIndex;
            /* step 6 */
            MapIterationKind itemKind = o.iterationKind;
            /* step 7 */
            if (m == null) {
                return CreateIterResultObject(cx, UNDEFINED, true);
            }
            /* step 8 */
            assert m.getMapData() != null;
            /* step 9 */
            Iterator<Entry<Object, Object>> iter = o.iterator;
            /* step 10 */
            if (iter.hasNext()) {
                Entry<Object, Object> e = iter.next();
                Object result;
                if (itemKind == MapIterationKind.Key) {
                    result = e.getKey();
                } else if (itemKind == MapIterationKind.Value) {
                    result = e.getValue();
                } else {
                    assert itemKind == MapIterationKind.KeyValue;
                    ArrayObject array = ArrayCreate(cx, 2);
                    CreateDataProperty(cx, array, 0, e.getKey());
                    CreateDataProperty(cx, array, 1, e.getValue());
                    result = array;
                }
                return CreateIterResultObject(cx, result, false);
            }
            /* step 11 */
            o.map = null;
            o.iterator = null;
            /* step 12 */
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        /**
         * 23.1.5.2.2 %MapIteratorPrototype% [ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Map Iterator";
    }
}
