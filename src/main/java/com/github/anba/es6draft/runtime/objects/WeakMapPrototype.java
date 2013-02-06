/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
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
 * <h2>15.15 WeakMap Objects</h2>
 * <ul>
 * <li>15.15.5 Properties of the WeakMap Prototype Object
 * </ul>
 */
public class WeakMapPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public WeakMapPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public Scriptable newInstance(Realm realm) {
        return new WeakMapObject(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 15.15.5 Properties of the WeakMap Prototype Object
     */
    public enum Properties {
        ;

        private static WeakMapObject WeakMap(Realm realm, Scriptable m) {
            if (m instanceof WeakMapObject) {
                return (WeakMapObject) m;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.15.5.1 WeakMap.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.WeakMap;

        /**
         * 15.15.5.2 WeakMap.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(Realm realm, Object thisValue) {
            Scriptable m = ToObject(realm, thisValue);
            WeakHashMap<Object, Object> entries = WeakMap(realm, m).getWeakMapData();
            // FIXME: spec bug?! -> bad interaction with iterator methods!
            // (calling clear() instead of adding new map for now...)
            entries.clear();
            return UNDEFINED;
        }

        /**
         * 15.15.5.3 WeakMap.prototype.delete ( key )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(Realm realm, Object thisValue, Object key) {
            Scriptable m = ToObject(realm, thisValue);
            WeakHashMap<Object, Object> entries = WeakMap(realm, m).getWeakMapData();
            return entries.remove(key);
        }

        /**
         * 15.15.5.4 WeakMap.prototype.get ( key )
         */
        @Function(name = "get", arity = 1)
        public static Object get(Realm realm, Object thisValue, Object key) {
            Scriptable m = ToObject(realm, thisValue);
            WeakHashMap<Object, Object> entries = WeakMap(realm, m).getWeakMapData();
            Object value = entries.get(key);
            return (value != null ? value : UNDEFINED);
        }

        /**
         * 15.15.5.5 WeakMap.prototype.has ( key )
         */
        @Function(name = "has", arity = 1)
        public static Object has(Realm realm, Object thisValue, Object key) {
            Scriptable m = ToObject(realm, thisValue);
            WeakHashMap<Object, Object> entries = WeakMap(realm, m).getWeakMapData();
            return entries.containsKey(key);
        }

        /**
         * 15.15.5.6 WeakMap.prototype.set ( key , value )
         */
        @Function(name = "set", arity = 2)
        public static Object set(Realm realm, Object thisValue, Object key, Object value) {
            Scriptable m = ToObject(realm, thisValue);
            WeakHashMap<Object, Object> entries = WeakMap(realm, m).getWeakMapData();
            if (!Type.isObject(key)) {
                throw throwTypeError(realm, Messages.Key.NotObjectType);
            }
            entries.put(key, new WeakReference<>(value));
            return UNDEFINED;
        }

        /**
         * 15.15.5.7 WeakMap.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "WeakMap";
    }

}
