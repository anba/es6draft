/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorStep;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorValue;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * Object to iterate over an internal list.
 */
public class ListIterator<T> extends OrdinaryObject {
    private Iterator<T> iterator;

    public ListIterator(Realm realm, Iterator<T> iterator) {
        super(realm);
        this.iterator = iterator;
    }

    public Iterator<T> getIterator() {
        return iterator;
    }

    /**
     * Returns a new {@link ListIterator} object for the internal list {@code iterator}
     */
    public static <T> ListIterator<T> MakeListIterator(ExecutionContext cx, Iterator<T> iterator) {
        ListIterator<T> iter = new ListIterator<>(cx.getRealm(), iterator);
        iter.setPrototype(cx.getIntrinsic(Intrinsics.ListIteratorPrototype));
        iter.preventExtensions(cx);
        return iter;
    }

    /**
     * Returns an iterator for {@code obj} which is expected to comply to the
     * <code>"25.1.2 The Iterator Interface"</code>
     */
    public static Iterator<?> FromListIterator(ExecutionContext cx, ScriptObject obj) {
        if (obj instanceof ListIterator) {
            return ((ListIterator<?>) obj).getIterator();
        }
        return new IteratorWrapper(cx, obj);
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
            ScriptObject next = IteratorStep(cx, object);
            if (next == null) {
                return null;
            }
            return IteratorValue(cx, next);
        }
    }
}
