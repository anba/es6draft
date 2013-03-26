/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.MapIteratorPrototype.CreateMapIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.MapIteratorPrototype.MapIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.14 Map Objects</h2>
 * <ul>
 * <li>15.14.4 Properties of the Map Prototype Object
 * </ul>
 */
public class MapPrototype extends OrdinaryObject implements ScriptObject, Initialisable {
    public MapPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);

        // 15.14.4.12 Map.prototype.@@iterator ( )
        defineOwnProperty(realm, BuiltinSymbol.iterator.get(),
                new PropertyDescriptor(Get(realm, this, "entries"), true, false, true));
    }

    /**
     * 15.14.4 Properties of the Map Prototype Object
     */
    public enum Properties {
        ;

        private static MapObject thisMapValue(Realm realm, Object obj) {
            if (Type.isObject(obj) && obj instanceof MapObject) {
                MapObject map = (MapObject) obj;
                if (map.isInitialised()) {
                    return map;
                }
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.14.4.1 Map.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Map;

        /**
         * 15.14.4.2 Map.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(Realm realm, Object thisValue) {
            MapObject m = thisMapValue(realm, thisValue);
            LinkedMap<Object, Object> entries = m.getMapData();
            entries.clear();
            return UNDEFINED;
        }

        /**
         * 15.14.4.3 Map.prototype.delete ( key )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(Realm realm, Object thisValue, Object key) {
            MapObject m = thisMapValue(realm, thisValue);
            LinkedMap<Object, Object> entries = m.getMapData();
            return entries.delete(key);
        }

        /**
         * 15.14.4.4 Map.prototype.forEach ( callbackfn , thisArg = undefined )
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(Realm realm, Object thisValue, Object callbackfn,
                Object thisArg) {
            MapObject m = thisMapValue(realm, thisValue);
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            LinkedMap<Object, Object> entries = m.getMapData();
            for (Iterator<Entry<Object, Object>> itr = entries.iterator(); itr.hasNext();) {
                Entry<Object, Object> e = itr.next();
                assert e != null;
                callback.call(thisArg, e.getValue(), e.getKey(), m);
            }
            return UNDEFINED;
        }

        /**
         * 15.14.4.5 Map.prototype.get ( key )
         */
        @Function(name = "get", arity = 1)
        public static Object get(Realm realm, Object thisValue, Object key) {
            MapObject m = thisMapValue(realm, thisValue);
            LinkedMap<Object, Object> entries = m.getMapData();
            Object value = entries.get(key);
            return (value != null ? value : UNDEFINED);
        }

        /**
         * 15.14.4.6 Map.prototype.has ( key )
         */
        @Function(name = "has", arity = 1)
        public static Object has(Realm realm, Object thisValue, Object key) {
            MapObject m = thisMapValue(realm, thisValue);
            LinkedMap<Object, Object> entries = m.getMapData();
            return entries.has(key);
        }

        /**
         * 15.14.4.7 Map.prototype.entries ( )
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(Realm realm, Object thisValue) {
            MapObject m = thisMapValue(realm, thisValue);
            return CreateMapIterator(realm, m, MapIterationKind.KeyValue);
        }

        /**
         * 15.14.4.8 Map.prototype.keys ( )
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(Realm realm, Object thisValue) {
            MapObject m = thisMapValue(realm, thisValue);
            return CreateMapIterator(realm, m, MapIterationKind.Key);
        }

        /**
         * 15.14.4.9 Map.prototype.set ( key , value )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object key, Object value) {
            MapObject m = thisMapValue(realm, thisValue);
            LinkedMap<Object, Object> entries = m.getMapData();
            entries.set(key, value);
            return m;
        }

        /**
         * 15.14.4.10 get Map.prototype.size
         */
        @Accessor(name = "size", type = Accessor.Type.Getter)
        public static Object size(Realm realm, Object thisValue) {
            MapObject m = thisMapValue(realm, thisValue);
            LinkedMap<Object, Object> entries = m.getMapData();
            return entries.size();
        }

        /**
         * 15.14.4.11 Map.prototype.values ( )
         */
        @Function(name = "values", arity = 0)
        public static Object values(Realm realm, Object thisValue) {
            MapObject m = thisMapValue(realm, thisValue);
            return CreateMapIterator(realm, m, MapIterationKind.Value);
        }

        /**
         * 15.14.4.13 Map.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Map";
    }
}
