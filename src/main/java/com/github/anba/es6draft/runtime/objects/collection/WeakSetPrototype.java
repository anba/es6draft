/**
 * Copyright (c) 2012-2014 André Bargull
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
import com.github.anba.es6draft.runtime.internal.Initialisable;
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
 * <h2>23.4 WeakSet Objects</h2>
 * <ul>
 * <li>23.4.3 Properties of the WeakSet Prototype Object
 * </ul>
 */
public final class WeakSetPrototype extends OrdinaryObject implements Initialisable {
    public WeakSetPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(cx, this, Properties.class);
    }

    /**
     * 23.4.3 Properties of the WeakSet Prototype Object
     */
    public enum Properties {
        ;

        private static WeakSetObject thisWeakSetValue(ExecutionContext cx, Object obj) {
            if (obj instanceof WeakSetObject) {
                WeakSetObject set = (WeakSetObject) obj;
                if (set.isInitialised()) {
                    return set;
                }
                throw newTypeError(cx, Messages.Key.UninitialisedObject);
            }
            throw newTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.4.3.3 WeakSet.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.WeakSet;

        /**
         * 23.4.3.1 WeakSet.prototype.add ( value )
         */
        @Function(name = "add", arity = 1)
        public static Object set(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step 5 */
            if (!Type.isObject(value)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            /* step 6 */
            WeakHashMap<ScriptObject, Boolean> entries = s.getWeakSetData();
            /* steps 7-8 */
            entries.put(Type.objectValue(value), Boolean.TRUE);
            /* step 7.a.i, 9 */
            return s;
        }

        /**
         * 23.4.3.2 WeakSet.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step 5 */
            s.getWeakSetData().clear();
            /* step 6 */
            return UNDEFINED;
        }

        /**
         * 23.4.3.4 WeakSet.prototype.delete ( value )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step 5 */
            if (!Type.isObject(value)) {
                return false;
            }
            /* step 6 */
            WeakHashMap<ScriptObject, Boolean> entries = s.getWeakSetData();
            /* steps 7-8 */
            return entries.remove(value);
        }

        /**
         * 23.4.3.5 WeakSet.prototype.has ( value )
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            WeakSetObject s = thisWeakSetValue(cx, thisValue);
            /* step ? */
            if (!Type.isObject(value)) {
                return false;
            }
            /* step 5 */
            WeakHashMap<ScriptObject, Boolean> entries = s.getWeakSetData();
            /* steps 6-7 */
            return entries.containsKey(value);
        }

        /**
         * 23.4.3.6 WeakSet.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "WeakSet";
    }
}
