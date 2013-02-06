/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.MakeObjectSecure;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime._throw;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.StopIterationObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;

/**
 *
 */
public class ListIterator<T> extends OrdinaryObject implements Scriptable {
    private Iterator<T> iterator;

    public ListIterator(Realm realm, Iterator<T> iterator) {
        super(realm);
        this.iterator = iterator;
    }

    public static class ListIteratorPrototype extends OrdinaryObject implements Scriptable,
            Initialisable {
        public ListIteratorPrototype(Realm realm) {
            super(realm);
        }

        @Override
        public void initialise(Realm realm) {
            createProperties(this, realm, Properties.class);
            MakeObjectSecure(realm, this, true);
        }
    }

    public static <T> ListIterator<T> MakeListIterator(Realm realm, Iterator<T> iterator) {
        ListIterator<T> itr = new ListIterator<>(realm, iterator);
        // createProperties(itr, realm, Properties.class);
        itr.setPrototype(realm.getIntrinsic(Intrinsics.ListIteratorPrototype));
        itr.preventExtensions();
        return itr;
    }

    public static Iterator<?> FromListIterator(Realm realm, Object obj) {
        if (obj instanceof ListIterator) {
            return ((ListIterator<?>) obj).iterator;
        }
        return new IteratorWrapper(realm, obj);
    }

    public enum Properties {
        ;

        private static ListIterator<?> listIterator(Realm realm, Object object) {
            if (object instanceof ListIterator) {
                return (ListIterator<?>) object;
            }
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Function(name = "next", arity = 0)
        public static Object send(Realm realm, Object thisValue) {
            ListIterator<?> itr = listIterator(realm, thisValue);
            if (!itr.iterator.hasNext()) {
                return _throw(realm.getIntrinsic(Intrinsics.StopIteration));
            }
            return itr.iterator.next();
        }

        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(Realm realm, Object thisValue) {
            return thisValue;
        }
    }

    private static class IteratorWrapper implements Iterator<Object> {
        private Realm realm;
        private Object object;
        private Object next = null;

        IteratorWrapper(Realm realm, Object object) {
            this.realm = realm;
            this.object = object;
        }

        private Object tryNext() {
            try {
                return AbstractOperations.Invoke(realm, object, "next");
            } catch (ScriptException e) {
                if (StopIterationObject.IteratorComplete(realm, e)) {
                    return null;
                }
                throw e;
            }
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = tryNext();
            }
            return (next != null);
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                assert false : "hasNext() call";
                throw new NoSuchElementException();
            }
            Object next = this.next;
            this.next = null;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
