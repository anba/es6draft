/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

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
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.17 WeakSet Objects</h2>
 * <ul>
 * <li>15.17.3 Properties of the WeakSet Prototype Object
 * </ul>
 */
public class WeakSetPrototype extends OrdinaryObject implements Initialisable {
    public WeakSetPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 15.17.3 Properties of the WeakSet Prototype Object
     */
    public enum Properties {
        ;

        private static WeakSetObject thisWeakSetValue(ExecutionContext cx, Object obj) {
            if (Type.isObject(obj) && obj instanceof WeakSetObject) {
                WeakSetObject set = (WeakSetObject) obj;
                if (set.isInitialised()) {
                    return set;
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.17.3.1 WeakSet.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.WeakSet;

        /**
         * 15.17.3.2 WeakSet.prototype.add ( value )
         */
        @Function(name = "add", arity = 1)
        public static Object set(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Boolean> entries = s.getWeakSetData();
            /* step ? */
            if (!Type.isObject(value)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 6-7 */
            entries.put(value, Boolean.TRUE);
            /* step 6.a.i, 8 */
            return s;
        }

        /**
         * 15.17.3.3 WeakSet.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Boolean> entries = s.getWeakSetData();
            /* step 6 */
            entries.clear();
            /* step 7 */
            return UNDEFINED;
        }

        /**
         * 15.17.3.4 WeakSet.prototype.delete ( value )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Boolean> entries = s.getWeakSetData();
            /* step ? */
            if (!Type.isObject(value)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 6-7 */
            return entries.remove(value);
        }

        /**
         * 15.17.3.5 WeakSet.prototype.has ( value )
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step 5 */
            WeakHashMap<Object, Boolean> entries = s.getWeakSetData();
            /* step ? */
            if (!Type.isObject(value)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            /* steps 6-7 */
            return entries.containsKey(value);
        }

        /**
         * 15.17.3.6 WeakSet.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "WeakSet";
    }
}
