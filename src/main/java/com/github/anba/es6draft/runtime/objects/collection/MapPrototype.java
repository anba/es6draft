/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.collection.MapIteratorPrototype.CreateMapIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.collection.MapIteratorPrototype.MapIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.1 Map Objects</h2>
 * <ul>
 * <li>23.1.3 Properties of the Map Prototype Object
 * </ul>
 */
public final class MapPrototype extends OrdinaryObject implements Initializable {
    public MapPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    /**
     * 23.1.3 Properties of the Map Prototype Object
     */
    public enum Properties {
        ;

        private static MapObject thisMapValue(ExecutionContext cx, Object obj) {
            if (obj instanceof MapObject) {
                MapObject map = (MapObject) obj;
                if (map.isInitialized()) {
                    return map;
                }
                throw newTypeError(cx, Messages.Key.UninitializedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.1.3.2 Map.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Map;

        /**
         * 23.1.3.1 Map.prototype.clear ()
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the undefined value
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            MapObject m = thisMapValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Object> entries = m.getMapData();
            /* step 6 */
            entries.clear();
            /* step 7 */
            return UNDEFINED;
        }

        /**
         * 23.1.3.3 Map.prototype.delete ( key )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param key
         *            the key
         * @return {@code true} if the entry was deleted
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-4 */
            MapObject m = thisMapValue(cx, thisValue);
            /* step 7 */
            LinkedMap<Object, Object> entries = m.getMapData();
            /* steps 5-6, 8-9 */
            return entries.delete(key);
        }

        /**
         * 23.1.3.5 Map.prototype.forEach ( callbackfn [ , thisArg ] )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param callbackfn
         *            the callback function
         * @param thisArg
         *            the optional this-argument
         * @return the undefined value
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-4 */
            MapObject m = thisMapValue(cx, thisValue);
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw newTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* step 7 */
            LinkedMap<Object, Object> entries = m.getMapData();
            /* step 8 */
            for (Iterator<Entry<Object, Object>> iter = entries.iterator(); iter.hasNext();) {
                Entry<Object, Object> e = iter.next();
                assert e != null;
                callback.call(cx, thisArg, e.getValue(), e.getKey(), m);
            }
            /* step 9 */
            return UNDEFINED;
        }

        /**
         * 23.1.3.6 Map.prototype.get ( key )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param key
         *            the key
         * @return the mapped value or undefined
         */
        @Function(name = "get", arity = 1)
        public static Object get(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-4 */
            MapObject m = thisMapValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Object> entries = m.getMapData();
            /* steps 6-10 */
            Object value = entries.get(key);
            return value != null ? value : UNDEFINED;
        }

        /**
         * 23.1.3.7 Map.prototype.has ( key )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param key
         *            the key
         * @return {@code true} if the entry was found
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-4 */
            MapObject m = thisMapValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Object> entries = m.getMapData();
            /* steps 6-9 */
            return entries.has(key);
        }

        /**
         * 23.1.3.4 Map.prototype.entries ( )<br>
         * 23.1.3.12 Map.prototype[ @@iterator ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the entries iterator
         */
        @Function(name = "entries", arity = 0)
        @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return CreateMapIterator(cx, thisValue, MapIterationKind.KeyValue);
        }

        /**
         * 23.1.3.8 Map.prototype.keys ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the keys iterator
         */
        @Function(name = "keys", arity = 0)
        public static Object keys(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return CreateMapIterator(cx, thisValue, MapIterationKind.Key);
        }

        /**
         * 23.1.3.9 Map.prototype.set ( key , value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param key
         *            the key
         * @param value
         *            the value
         * @return this map object
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object key, Object value) {
            /* steps 1-4 */
            MapObject m = thisMapValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Object> entries = m.getMapData();
            /* steps 6-10 */
            entries.set(key, value);
            /* step 8.a.ii, 11 */
            return m;
        }

        /**
         * 23.1.3.10 get Map.prototype.size
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the number of entries
         */
        @Accessor(name = "size", type = Accessor.Type.Getter)
        public static Object size(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            MapObject m = thisMapValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Object> entries = m.getMapData();
            /* steps 6-8 */
            return entries.size();
        }

        /**
         * 23.1.3.11 Map.prototype.values ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the values iterator
         */
        @Function(name = "values", arity = 0)
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            return CreateMapIterator(cx, thisValue, MapIterationKind.Value);
        }

        /**
         * 23.1.3.13 Map.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Map";
    }
}
