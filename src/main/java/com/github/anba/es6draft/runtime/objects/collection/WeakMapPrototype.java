/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.WeakHashMap;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.3 WeakMap Objects</h2>
 * <ul>
 * <li>23.3.3 Properties of the WeakMap Prototype Object
 * </ul>
 */
public class WeakMapPrototype extends OrdinaryObject implements Initialisable {
    public WeakMapPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 23.3.3 Properties of the WeakMap Prototype Object
     */
    public enum Properties {
        ;

        private static WeakMapObject thisWeakMapValue(ExecutionContext cx, Object obj) {
            if (Type.isObject(obj) && obj instanceof WeakMapObject) {
                WeakMapObject map = (WeakMapObject) obj;
                if (map.isInitialised()) {
                    return map;
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.3.3.2 WeakMap.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.WeakMap;

        /**
         * 23.3.3.1 WeakMap.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            WeakMapObject m = thisWeakMapValue(cx, thisValue);
            /* step 5 */
            m.getWeakMapData().clear();
            /* step 6 */
            return UNDEFINED;
        }

        /**
         * 23.3.3.3 WeakMap.prototype.delete ( key )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-4 */
            WeakMapObject m = thisWeakMapValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Object> entries = m.getWeakMapData();
            /* step 6 */
            if (!Type.isObject(key)) {
                return false;
            }
            /* steps 7-8 */
            return entries.remove(key);
        }

        /**
         * 23.3.3.4 WeakMap.prototype.get ( key )
         */
        @Function(name = "get", arity = 1)
        public static Object get(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-4 */
            WeakMapObject m = thisWeakMapValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Object> entries = m.getWeakMapData();
            /* step 6 */
            if (!Type.isObject(key)) {
                return UNDEFINED;
            }
            /* steps 7-8 */
            Object value = entries.get(key);
            return value != null ? value : UNDEFINED;
        }

        /**
         * 23.3.3.5 WeakMap.prototype.has ( key )
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-4 */
            WeakMapObject m = thisWeakMapValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Object> entries = m.getWeakMapData();
            /* step 6 */
            if (!Type.isObject(key)) {
                return false;
            }
            /* steps 7-8 */
            return entries.containsKey(key);
        }

        /**
         * 23.3.3.6 WeakMap.prototype.set ( key , value )
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object key, Object value) {
            /* steps 1-4 */
            WeakMapObject m = thisWeakMapValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Object> entries = m.getWeakMapData();
            /* step 6 */
            if (!Type.isObject(key)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 7-9 */
            entries.put(key, value);
            /* step 7.a.ii, 10 */
            return m;
        }

        /**
         * 23.3.3.7 WeakMap.prototype[ @@toStringTag ]
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "WeakMap";
    }
}
