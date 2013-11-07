/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.collection.SetIteratorPrototype.CreateSetIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.*;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunction;
import com.github.anba.es6draft.runtime.internal.Properties.AliasFunctions;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.collection.SetIteratorPrototype.SetIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.2 Set Objects</h2>
 * <ul>
 * <li>23.2.3 Properties of the Set Prototype Object
 * </ul>
 */
public class SetPrototype extends OrdinaryObject implements Initialisable {
    public SetPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
    }

    /**
     * 23.2.3 Properties of the Set Prototype Object
     */
    public enum Properties {
        ;

        private static SetObject thisSetValue(ExecutionContext cx, Object obj) {
            if (Type.isObject(obj) && obj instanceof SetObject) {
                SetObject set = (SetObject) obj;
                if (set.isInitialised()) {
                    return set;
                }
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 23.2.3.3 Set.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Set;

        /**
         * 23.2.3.1 Set.prototype.add (value )
         */
        @Function(name = "add", arity = 1)
        public static Object add(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 6-9 */
            entries.set(value, null);
            /* step 8.a.i, 10 */
            return s;
        }

        /**
         * 23.2.3.2 Set.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* step 6 */
            entries.clear();
            /* step 7 */
            return UNDEFINED;
        }

        /**
         * 23.2.3.4 Set.prototype.delete ( value )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object value) {
            /* steps 1-4 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 6-9 */
            return entries.delete(value);
        }

        /**
         * 23.2.3.5 Set.prototype.entries ( )
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 3 */
            return CreateSetIterator(cx, s, SetIterationKind.KeyValue);
        }

        /**
         * 23.2.3.6 Set.prototype.forEach ( callbackfn , thisArg = undefined )
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            /* steps 1-4 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 5 */
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            /* step 6 (omitted) */
            /* step 7 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* step 8 */
            for (Iterator<Entry<Object, Void>> iter = entries.iterator(); iter.hasNext();) {
                Entry<Object, Void> e = iter.next();
                assert e != null;
                callback.call(cx, thisArg, e.getKey(), e.getKey(), s);
            }
            /* step 9 */
            return UNDEFINED;
        }

        /**
         * 23.2.3.7 Set.prototype.has ( value )
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object key) {
            /* steps 1-4 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 6-9 */
            return entries.has(key);
        }

        /**
         * 23.2.3.9 get Set.prototype.size
         */
        @Accessor(name = "size", type = Accessor.Type.Getter)
        public static Object size(ExecutionContext cx, Object thisValue) {
            /* steps 1-4 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 5 */
            LinkedMap<Object, Void> entries = s.getSetData();
            /* steps 6-8 */
            return entries.size();
        }

        /**
         * 23.2.3.8 Set.prototype.keys ( )<br>
         * 23.2.3.10 Set.prototype.values ( )<br>
         * 23.2.3.11 Set.prototype[ @@iterator ] ( )
         */
        @Function(name = "values", arity = 0)
        @AliasFunctions({ @AliasFunction(name = "keys"),
                @AliasFunction(name = "[Symbol.iterator]", symbol = BuiltinSymbol.iterator) })
        public static Object values(ExecutionContext cx, Object thisValue) {
            /* steps 1-2 */
            SetObject s = thisSetValue(cx, thisValue);
            /* step 3 */
            return CreateSetIterator(cx, s, SetIterationKind.Value);
        }

        /**
         * 23.2.3.12 Set.prototype[ @@toStringTag ]
         */
        @Value(name = "[Symbol.toStringTag]", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Set";
    }
}
