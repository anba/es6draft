/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateOwnDataProperty;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArray.ArrayCreate;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArray;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.14 Map Objects</h2>
 * <ul>
 * <li>15.16.7 Set Iterator Object Structure
 * </ul>
 */
public class SetIteratorPrototype extends OrdinaryObject implements Scriptable, Initialisable {
    public SetIteratorPrototype(Realm realm) {
        super(realm);
    }

    @Override
    public Scriptable newInstance(Realm realm) {
        return new SetIterator(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    public enum SetIterationKind {
        Key, Value, KeyValue
    }

    /**
     * 15.16.7.3 Properties of Set Iterator Instances
     */
    private static class SetIterator extends OrdinaryObject {
        /** [[IteratedSet]] */
        @SuppressWarnings("unused")
        SetObject set;

        /** [[SetNextIndex]] */
        @SuppressWarnings("unused")
        int nextIndex;

        /** [[SetIterationKind]] */
        SetIterationKind iterationKind;

        Iterator<Entry<Object, Void>> iterator;

        SetIterator(Realm realm) {
            super(realm);
        }
    }

    /**
     * 15.16.7.1 CreateSetIterator Abstract Operation
     */
    public static OrdinaryObject CreateSetIterator(Realm realm, SetObject set, SetIterationKind kind) {
        LinkedMap<Object, Void> entries = set.getSetData();
        Scriptable proto = realm.getIntrinsic(Intrinsics.SetIteratorPrototype);
        SetIterator itr = (SetIterator) ObjectCreate(realm, proto, proto);
        itr.set = set;
        itr.nextIndex = 0;
        itr.iterator = entries.iterator();
        return itr;
    }

    /**
     * 15.16.7.2 The Set Iterator Prototype
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        /**
         * 15.16.7.2.1 SetIterator.prototype.constructor<br>
         * FIXME: spec bug (no description)
         */
        @Value(name = "constructor")
        public static final Object constructor = UNDEFINED;

        /**
         * 15.16.7.2.2 SetIterator.prototype.next( )
         */
        @Function(name = "next", arity = 0)
        public static Object next(Realm realm, Object thisValue) {
            if (!Type.isObject(thisValue)) {
                throw throwTypeError(realm, Messages.Key.NotObjectType);
            }
            if (!(thisValue instanceof SetIterator)) {
                throw throwTypeError(realm, Messages.Key.IncompatibleObject);
            }
            SetIterator o = (SetIterator) thisValue;
            // Scriptable m = o.set;
            // int index = o.nextIndex;
            SetIterationKind itemKind = o.iterationKind;
            Iterator<Entry<Object, Void>> itr = o.iterator;
            if (itr.hasNext()) {
                Entry<Object, Void> e = itr.next();
                assert e != null;
                if (itemKind == SetIterationKind.KeyValue) {
                    ExoticArray result = ArrayCreate(realm, 2);
                    CreateOwnDataProperty(result, "0", e.getKey());
                    CreateOwnDataProperty(result, "1", e.getKey());
                    return result;
                }
                return e.getKey();
            }
            return _throw(realm.getIntrinsic(Intrinsics.StopIteration));
        }

        /**
         * 15.16.7.2.3 SetIterator.prototype.@@iterator()
         */
        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(Realm realm, Object thisValue) {
            return thisValue;
        }

        /**
         * 15.16.7.2.4 SetIterator.prototype.@@toStringTag
         */
        @Value(name = "@@toStringTag", symbol = BuiltinSymbol.toStringTag)
        public static final String toStringTag = "Set Iterator";
    }
}
