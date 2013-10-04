/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateIterResultObject;
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
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.1 Map Objects</h2>
 * <ul>
 * <li>23.1.5 Map Iterator Object Structure
 * </ul>
 */
public class MapIteratorPrototype extends OrdinaryObject implements Initialisable {
    public MapIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    public enum MapIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 23.1.5.3 Properties of Map Iterator Instances
     */
    private static class MapIterator extends OrdinaryObject {
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

    private static class MapIteratorAllocator implements ObjectAllocator<MapIterator> {
        static final ObjectAllocator<MapIterator> INSTANCE = new MapIteratorAllocator();

        @Override
        public MapIterator newInstance(Realm realm) {
            return new MapIterator(realm);
        }
    }

    /**
     * 23.1.5.1 CreateMapIterator Abstract Operation
     */
    public static OrdinaryObject CreateMapIterator(ExecutionContext cx, MapObject m,
            MapIterationKind kind) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        LinkedMap<Object, Object> entries = m.getMapData();
        /* step 4 */
        MapIterator iterator = ObjectCreate(cx, Intrinsics.MapIteratorPrototype,
                MapIteratorAllocator.INSTANCE);
        /* steps 5-7 */
        iterator.map = m;
        iterator.nextIndex = 0;
        iterator.iterationKind = kind;
        iterator.iterator = entries.iterator();
        /* step 8 */
        return iterator;
    }

    /**
     * 23.1.5.2 The Map Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.1.5.2.1 MapIterator.prototype.constructor<br>
         * FIXME: spec bug (no description)
         */
        @Value(name = "constructor")
        public static final Object constructor = UNDEFINED;

        /**
         * 23.1.5.2.2 MapIterator.prototype.next( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(ExecutionContext cx, Object thisValue) {
            /* step 2 */
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 3 */
            if (!(thisValue instanceof MapIterator)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
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
            assert m.getMapData() != null;
            /* step 8 */
            Iterator<Entry<Object, Object>> iter = o.iterator;
            /* step 9 */
            if (iter.hasNext()) {
                Entry<Object, Object> e = iter.next();
                assert e != null;
                Object result;
                if (itemKind == MapIterationKind.Key) {
                    result = e.getKey();
                } else if (itemKind == MapIterationKind.Value) {
                    result = e.getValue();
                } else {
                    assert itemKind == MapIterationKind.KeyValue;
                    ScriptObject array = ArrayCreate(cx, 2);
                    CreateOwnDataProperty(cx, array, "0", e.getKey());
                    CreateOwnDataProperty(cx, array, "1", e.getValue());
                    result = array;
                }
                return CreateIterResultObject(cx, result, false);
            }
            /* step 10 */
            return CreateIterResultObject(cx, UNDEFINED, true);
        }

        /**
         * 23.1.5.2.3 MapIterator.prototype[ @@iterator ]()
         */
        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }

        /**
         * 23.1.5.2.4 MapIterator.prototype[ @@toStringTag ]
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Map Iterator";
    }
}
