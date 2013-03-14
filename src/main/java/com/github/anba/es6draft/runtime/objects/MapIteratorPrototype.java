/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.Iterator;
import java.util.Map.Entry;

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
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.14 Map Objects</h2>
 * <ul>
 * <li>15.14.6 Map Iterator Object Structure
 * </ul>
 */
public class MapIteratorPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public MapIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    public enum MapIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 15.14.6.3 Properties of Map Iterator Instances
     */
    private static class MapIterator extends OrdinaryObject {
        /**
         * [[Map]]
         */
        @SuppressWarnings("unused")
        MapObject map;

        /**
         * [[MapNextIndex]]
         */
        @SuppressWarnings("unused")
        int nextIndex;

        /**
         * [[MapIterationKind]]
         */
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
     * 15.14.6.1 CreateMapIterator Abstract Operation
     */
    public static OrdinaryObject CreateMapIterator(Realm realm, MapObject m, MapIterationKind kind) {
        LinkedMap<Object, Object> entries = m.getMapData();
        MapIterator itr = ObjectCreate(realm, Intrinsics.MapIteratorPrototype,
                MapIteratorAllocator.INSTANCE);
        itr.map = m;
        itr.nextIndex = 0;
        itr.iterationKind = kind;
        itr.iterator = entries.iterator();
        return itr;
    }

    /**
     * 15.14.6.2 The Map Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.14.6.2.1 MapIterator.prototype.constructor<br>
         * FIXME: spec bug (no description)
         */
        @Value(name = "constructor")
        public static final Object constructor = UNDEFINED;

        /**
         * 15.14.6.2.2 MapIterator.prototype.next( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(Realm realm, Object thisValue) {
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(realm, Messages.Key.NotObjectType);
            }
            if (!(thisValue instanceof MapIterator)) {
                throw throwTypeError(realm, Messages.Key.IncompatibleObject);
            }
            MapIterator o = (MapIterator) thisValue;
            // Scriptable m = o.map;
            // int index = o.nextIndex;
            MapIterationKind itemKind = o.iterationKind;
            Iterator<Entry<Object, Object>> itr = o.iterator;
            if (itr.hasNext()) {
                Entry<Object, Object> e = itr.next();
                assert e != null;
                Object result;
                if (itemKind == MapIterationKind.Key) {
                    result = e.getKey();
                } else if (itemKind == MapIterationKind.Value) {
                    result = e.getValue();
                } else {
                    assert itemKind == MapIterationKind.KeyValue;
                    Scriptable array = ArrayCreate(realm, 2);
                    CreateOwnDataProperty(array, "0", e.getKey());
                    CreateOwnDataProperty(array, "1", e.getValue());
                    result = array;
                }
                return result;
            }
            return _throw(realm.getIntrinsic(Intrinsics.StopIteration));
        }

        /**
         * 15.14.6.2.3 MapIterator.prototype.@@iterator ()
         */
        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(Realm realm, Object thisValue) {
            return thisValue;
        }

        /**
         * 15.14.6.2.4 MapIterator.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Map Iterator";
    }
}
