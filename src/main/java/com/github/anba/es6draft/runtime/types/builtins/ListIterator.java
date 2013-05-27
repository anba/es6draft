/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Invoke;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateItrResultObject;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorComplete;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorValue;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.IntegrityLevel;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 *
 */
public class ListIterator<T> extends OrdinaryObject {
    private Iterator<T> iterator;

    public ListIterator(Realm realm, Iterator<T> iterator) {
        super(realm);
        this.iterator = iterator;
    }

    public static class ListIteratorPrototype extends OrdinaryObject implements Initialisable {
        public ListIteratorPrototype(Realm realm) {
            super(realm);
        }

        @Override
        public void initialise(ExecutionContext cx) {
            createProperties(this, cx, Properties.class);
            setIntegrity(cx, IntegrityLevel.Frozen);
        }
    }

    public static <T> ListIterator<T> MakeListIterator(ExecutionContext cx, Iterator<T> iterator) {
        ListIterator<T> itr = new ListIterator<>(cx.getRealm(), iterator);
        itr.setPrototype(cx, cx.getIntrinsic(Intrinsics.ListIteratorPrototype));
        itr.setIntegrity(cx, IntegrityLevel.NonExtensible);
        return itr;
    }

    public static Iterator<?> FromListIterator(ExecutionContext cx, ScriptObject obj) {
        if (obj instanceof ListIterator) {
            return ((ListIterator<?>) obj).iterator;
        }
        return new IteratorWrapper(cx, obj);
    }

    public enum Properties {
        ;

        private static ListIterator<?> listIterator(ExecutionContext cx, Object object) {
            if (object instanceof ListIterator) {
                return (ListIterator<?>) object;
            }
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Function(name = "next", arity = 0)
        public static Object send(ExecutionContext cx, Object thisValue) {
            ListIterator<?> itr = listIterator(cx, thisValue);
            if (!itr.iterator.hasNext()) {
                return CreateItrResultObject(cx, Undefined.UNDEFINED, true);
            }
            return CreateItrResultObject(cx, itr.iterator.next(), false);
        }

        @Function(name = "@@iterator", symbol = BuiltinSymbol.iterator, arity = 0)
        public static Object iterator(ExecutionContext cx, Object thisValue) {
            return thisValue;
        }
    }

    private static class IteratorWrapper extends SimpleIterator<Object> {
        private ExecutionContext cx;
        private ScriptObject object;

        IteratorWrapper(ExecutionContext cx, ScriptObject object) {
            this.cx = cx;
            this.object = object;
        }

        @Override
        protected Object tryNext() {
            Object nextResult = Invoke(cx, object, "next");
            if (!Type.isObject(nextResult)) {
                throw throwTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject next = Type.objectValue(nextResult);
            boolean done = IteratorComplete(cx, next);
            if (done) {
                return null;
            }
            return IteratorValue(cx, next);
        }
    }
}
