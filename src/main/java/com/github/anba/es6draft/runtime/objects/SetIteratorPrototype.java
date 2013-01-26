/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToObject;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.LinkedMap;
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
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
    }

    /**
     * 15.16.7.3 Properties of Set Iterator Instances
     */
    private static class SetIterator extends OrdinaryObject {
        /**
         * [[IteratedSet]]
         */
        @SuppressWarnings("unused")
        Scriptable set;

        /**
         * [[SetNextIndex]]
         */
        @SuppressWarnings("unused")
        int nextIndex;

        Iterator<Entry<Object, Void>> iterator;

        SetIterator(Realm realm) {
            super(realm);
        }
    }

    private static SetObject SetObject(Realm realm, Scriptable m) {
        if (m instanceof SetObject) {
            return (SetObject) m;
        }
        throw throwTypeError(realm, "incompatible object");
    }

    /**
     * 15.16.7.1 CreateSetIterator Abstract Operation
     */
    public static OrdinaryObject CreateSetIterator(Realm realm, Object set) {
        Scriptable s = ToObject(realm, set);
        LinkedMap<Object, Void> entries = SetObject(realm, s).getSetData();
        // FIXME: spec bug (variable entries unused)
        // ObjectCreate()
        Scriptable proto = realm.getIntrinsic(Intrinsics.SetIteratorPrototype);
        SetIterator itr = new SetIterator(realm);
        itr.setPrototype(proto);
        itr.set = s;
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
                throw throwTypeError(realm, "");
            }
            Scriptable o = ToObject(realm, thisValue);
            if (!(o instanceof SetIterator)) {
                throw throwTypeError(realm, "");
            }
            // Scriptable m = ((SetIterator) o).set;
            // int index = ((SetIterator) o).nextIndex;
            Iterator<Entry<Object, Void>> itr = ((SetIterator) o).iterator;
            while (itr.hasNext()) {
                Entry<Object, Void> e = itr.next();
                assert e != null;
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
