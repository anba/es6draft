/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.WeakHashMap;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
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
 * <h2>23.3 WeakMap Objects</h2>
 * <ul>
 * <li>23.3.3 Properties of the WeakMap Prototype Object
 * </ul>
 */
public final class WeakMapPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new WeakMap prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public WeakMapPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * 23.3.3 Properties of the WeakMap Prototype Object
     */
    public enum Properties {
        ;

        private static WeakMapObject thisWeakMapObject(ExecutionContext cx, Object obj) {
            if (obj instanceof WeakMapObject) {
                return (WeakMapObject) obj;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.3.3.1 WeakMap.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.WeakMap;

        /**
         * 23.3.3.2 WeakMap.prototype.delete ( key )
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
            /* steps 1-3 */
            WeakMapObject m = thisWeakMapObject(cx, thisValue);
            /* step 4 */
            WeakHashMap<ScriptObject, Object> entries = m.getWeakMapData();
            /* step 5 */
            if (!Type.isObject(key)) {
                return false;
            }
            /* steps 6-7 */
            return entries.remove(key) != null;
        }

        /**
         * 23.3.3.3 WeakMap.prototype.get ( key )
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
            /* steps 1-3 */
            WeakMapObject m = thisWeakMapObject(cx, thisValue);
            /* step 4 */
            WeakHashMap<ScriptObject, Object> entries = m.getWeakMapData();
            /* step 5 */
            if (!Type.isObject(key)) {
                return UNDEFINED;
            }
            /* steps 6-7 */
            Object value = entries.get(key);
            return value != null ? value : UNDEFINED;
        }

        /**
         * 23.3.3.4 WeakMap.prototype.has ( key )
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
            /* steps 1-3 */
            WeakMapObject m = thisWeakMapObject(cx, thisValue);
            /* step 4 */
            WeakHashMap<ScriptObject, Object> entries = m.getWeakMapData();
            /* step 5 */
            if (!Type.isObject(key)) {
                return false;
            }
            /* steps 6-7 */
            return entries.containsKey(key);
        }

        /**
         * 23.3.3.5 WeakMap.prototype.set ( key , value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param key
         *            the key
         * @param value
         *            the value
         * @return this weak map object
         */
        @Function(name = "set", arity = 2)
        public static Object set(ExecutionContext cx, Object thisValue, Object key, Object value) {
            /* steps 1-3 */
            WeakMapObject m = thisWeakMapObject(cx, thisValue);
            /* step 4 */
            WeakHashMap<ScriptObject, Object> entries = m.getWeakMapData();
            /* step 5 */
            if (!Type.isObject(key)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 6-9 */
            entries.put(Type.objectValue(key), value);
            return m;
        }

        /**
         * 23.3.3.6 WeakMap.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "WeakMap";
    }
}
