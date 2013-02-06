/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.SetIteratorPrototype.CreateSetIterator;
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
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.16 Set Objects</h2>
 * <ul>
 * <li>15.16.5 Properties of the Set Prototype Object
 * </ul>
 */
public class SetPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public SetPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public Scriptable newInstance(Realm realm) {
        return new SetObject(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);

        // 15.16.5.9 Set.prototype.@@iterator ( )
        defineOwnProperty(BuiltinSymbol.iterator.get(), new PropertyDescriptor(Get(this, "values"),
                true, false, true));
    }

    /**
     * 15.16.5 Properties of the Set Prototype Object
     */
    public enum Properties {
        ;

        private static SetObject SetObject(Realm realm, Scriptable m) {
            if (m instanceof SetObject) {
                return (SetObject) m;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.16.5.1 Set.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Set;

        /**
         * 15.16.5.2 Set.prototype.add (value )
         */
        @Function(name = "add", arity = 1)
        public static Object add(Realm realm, Object thisValue, Object value) {
            Scriptable s = ToObject(realm, thisValue);
            LinkedMap<Object, Void> entries = SetObject(realm, s).getSetData();
            entries.set(value, null);
            return UNDEFINED;
        }

        /**
         * 15.16.5.3 Set.prototype.clear ()
         */
        @Function(name = "clear", arity = 0)
        public static Object clear(Realm realm, Object thisValue) {
            Scriptable s = ToObject(realm, thisValue);
            LinkedMap<Object, Void> entries = SetObject(realm, s).getSetData();
            // FIXME: spec bug?! -> bad interaction with iterator methods!
            // (calling clear() instead of adding new map for now...)
            entries.clear();
            return UNDEFINED;
        }

        /**
         * 15.16.5.4 Set.prototype.delete ( value )
         */
        @Function(name = "delete", arity = 1)
        public static Object delete(Realm realm, Object thisValue, Object value) {
            Scriptable s = ToObject(realm, thisValue);
            LinkedMap<Object, Void> entries = SetObject(realm, s).getSetData();
            return entries.delete(value);
        }

        /**
         * 15.16.5.5 Set.prototype.forEach ( callbackfn , thisArg = undefined )
         */
        @Function(name = "forEach", arity = 1)
        public static Object forEach(Realm realm, Object thisValue, Object callbackfn,
                Object thisArg) {
            Scriptable s = ToObject(realm, thisValue);
            LinkedMap<Object, Void> entries = SetObject(realm, s).getSetData();
            if (!IsCallable(callbackfn)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            Callable callback = (Callable) callbackfn;
            for (Iterator<Entry<Object, Void>> itr = entries.iterator(); itr.hasNext();) {
                Entry<Object, Void> e = itr.next();
                assert e != null;
                callback.call(thisArg, e.getKey(), s);
            }
            return UNDEFINED;
        }

        /**
         * 15.16.5.6 Set.prototype.has ( value )
         */
        @Function(name = "has", arity = 1)
        public static Object has(Realm realm, Object thisValue, Object key) {
            Scriptable s = ToObject(realm, thisValue);
            LinkedMap<Object, Void> entries = SetObject(realm, s).getSetData();
            return entries.has(key);
        }

        /**
         * 15.16.5.7 get Set.prototype.size
         */
        @Accessor(name = "size", type = Accessor.Type.Getter)
        public static Object size(Realm realm, Object thisValue) {
            Scriptable s = ToObject(realm, thisValue);
            LinkedMap<Object, Void> entries = SetObject(realm, s).getSetData();
            return entries.size();
        }

        /**
         * 15.16.5.8 Set.prototype.values ( )
         */
        @Function(name = "values", arity = 0)
        public static Object values(Realm realm, Object thisValue) {
            Scriptable s = ToObject(realm, thisValue);
            return CreateSetIterator(realm, s);
        }

        /**
         * 15.16.5.10 Set.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Set";

    }
}
