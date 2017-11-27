/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;

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
import com.github.anba.es6draft.runtime.types.builtins.NativeFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.4 WeakSet Objects</h2>
 * <ul>
 * <li>23.4.3 Properties of the WeakSet Prototype Object
 * </ul>
 */
public final class WeakSetPrototype extends OrdinaryObject implements Initializable {
    /**
     * Constructs a new WeakSet prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public WeakSetPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    /**
     * Marker class for {@code WeakSet.prototype.add}.
     */
    private static final class WeakSetPrototypeAdd {
    }

    /**
     * Returns {@code true} if <var>add</var> is the built-in {@code WeakSet.prototype.add} function.
     * 
     * @param add
     *            the add function
     * @return {@code true} if <var>add</var> is the built-in {@code WeakSet.prototype.add} function
     */
    static boolean isBuiltinAdd(Object add) {
        return NativeFunction.isNative(add, WeakSetPrototypeAdd.class);
    }

    /**
     * 23.4.3 Properties of the WeakSet Prototype Object
     */
    public enum Properties {
        ;

        private static WeakSetObject thisWeakSetObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof WeakSetObject) {
                return (WeakSetObject) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.4.3.2 WeakSet.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.WeakSet;

        /**
         * 23.4.3.1 WeakSet.prototype.add ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the new value
         * @return this weak set object
         */
        @Function(name = "add", arity = 1, nativeId = WeakSetPrototypeAdd.class)
        public static Object add(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            WeakSetObject s = thisWeakSetObject(cx, thisValue, "WeakSet.prototype.add");
            /* step 4 */
            if (!Type.isObject(value)) {
                throw newTypeError(cx, Messages.Key.WeakSetKeyNotObject);
            }
            /* step 5 */
            WeakHashMap<ScriptObject, Boolean> entries = s.getWeakSetData();
            /* steps 6-7 */
            entries.put(Type.objectValue(value), Boolean.TRUE);
            /* step 8 */
            return s;
        }

        /**
         * 23.4.3.3 WeakSet.prototype.delete ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return {@code true} if the entry was deleted
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            WeakSetObject s = thisWeakSetObject(cx, thisValue, "WeakSet.prototype.delete");
            /* step 4 */
            if (!Type.isObject(value)) {
                return false;
            }
            /* step 5 */
            WeakHashMap<ScriptObject, Boolean> entries = s.getWeakSetData();
            /* steps 6-7 */
            return entries.remove(value) != null;
        }

        /**
         * 23.4.3.4 WeakSet.prototype.has ( value )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param value
         *            the value
         * @return {@code true} if the entry was found
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-3 */
            WeakSetObject s = thisWeakSetObject(cx, thisValue, "WeakSet.prototype.has");
            /* step 5 */
            if (!Type.isObject(value)) {
                return false;
            }
            /* step 4 */
            WeakHashMap<ScriptObject, Boolean> entries = s.getWeakSetData();
            /* steps 6-7 */
            return entries.containsKey(value);
        }

        /**
         * 23.4.3.5 WeakSet.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "WeakSet";
    }
}
