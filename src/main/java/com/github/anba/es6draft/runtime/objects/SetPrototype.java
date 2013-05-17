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
import static com.github.anba.es6draft.runtime.objects.SetIteratorPrototype.CreateSetIterator;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Accessor;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.objects.SetIteratorPrototype.SetIterationKind;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.16 Set Objects</h2>
 * <ul>
 * <li>15.16.4 Properties of the Set Prototype Object
 * </ul>
 */
public class SetPrototype extends OrdinaryObject implements Initialisable {
    public SetPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);

        // 15.16.4.8 Set.prototype.keys ( )
        defineOwnProperty(cx, "keys", new PropertyDescriptor(Get(cx, this, "values"), true, false,
                true));

        // 15.16.4.11 Set.prototype.@@iterator ( )
        defineOwnProperty(cx, BuiltinSymbol.iterator.get(),
                new PropertyDescriptor(Get(cx, this, "values"), true, false, true));
    }

    /**
     * 15.16.4 Properties of the Set Prototype Object
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
         * 15.16.4.1 Set.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Set;

        /**
         * 15.16.4.2 Set.prototype.add (value )
         */
        @Function(name = "add", arity = 1)
        public static Object add(ExecutionContext cx, Object thisValue, Object value) {
            SetObject s = thisSetValue(cx, thisValue);
            LinkedMap<Object, Void> entries = s.getSetData();
            entries.set(value, null);
            return s;
        }

        /**
         * 15.16.4.3 Set.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(ExecutionContext cx, Object thisValue) {
            SetObject s = thisSetValue(cx, thisValue);
            LinkedMap<Object, Void> entries = s.getSetData();
            entries.clear();
            return UNDEFINED;
        }

        /**
         * 15.16.4.4 Set.prototype.delete ( value )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(ExecutionContext cx, Object thisValue, Object value) {
            SetObject s = thisSetValue(cx, thisValue);
            LinkedMap<Object, Void> entries = s.getSetData();
            return entries.delete(value);
        }

        /**
         * 15.16.4.5 Set.prototype.entries ( )
         */
        @Function(name = "entries", arity = 0)
        public static Object entries(ExecutionContext cx, Object thisValue) {
            SetObject s = thisSetValue(cx, thisValue);
            return CreateSetIterator(cx, s, SetIterationKind.KeyValue);
        }

        /**
         * 15.16.4.6 Set.prototype.forEach ( callbackfn , thisArg = undefined )
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(ExecutionContext cx, Object thisValue, Object callbackfn,
                Object thisArg) {
            SetObject s = thisSetValue(cx, thisValue);
            LinkedMap<Object, Void> entries = s.getSetData();
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(cx, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            for (Iterator<Entry<Object, Void>> itr = entries.iterator(); itr.hasNext();) {
                Entry<Object, Void> e = itr.next();
                assert e != null;
                callback.call(cx, thisArg, e.getKey(), s);
            }
            return UNDEFINED;
        }

        /**
         * 15.16.4.7 Set.prototype.has ( value )
         */
        @Function(name = "has", arity = 1)
        public static Object has(ExecutionContext cx, Object thisValue, Object key) {
            SetObject s = thisSetValue(cx, thisValue);
            LinkedMap<Object, Void> entries = s.getSetData();
            return entries.has(key);
        }

        /**
         * 15.16.4.9 get Set.prototype.size
         */
        @Accessor(name = "size", type = Accessor.Type.Getter)
        public static Object size(ExecutionContext cx, Object thisValue) {
            SetObject s = thisSetValue(cx, thisValue);
            LinkedMap<Object, Void> entries = s.getSetData();
            return entries.size();
        }

        /**
         * 15.16.4.10 Set.prototype.values ( )
         */
        @Function(name = "values", arity = 0)
        public static Object values(ExecutionContext cx, Object thisValue) {
            SetObject s = thisSetValue(cx, thisValue);
            return CreateSetIterator(cx, s, SetIterationKind.Value);
        }

        /**
         * 15.16.4.12 Set.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String toStringTag = "Set";

    }
}
