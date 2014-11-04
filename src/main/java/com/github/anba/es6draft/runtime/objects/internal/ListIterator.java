/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorStep;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorValue;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptIterator;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>7 Abstract Operations</h1><br>
 * <h2>7.4 Operations on Iterator Objects</h2>
 * <ul>
 * <li>7.4.9 CreateListIterator (list)
 * </ul>
 */
public final class ListIterator<T> extends OrdinaryObject {
    /** [[IteratedList]] and [[ListIteratorNextIndex]] */
    private Iterator<T> iterator;

    /** [[IteratorNext]] */
    private ListIteratorNext iteratorNext;

    public ListIterator(Realm realm) {
        super(realm);
    }

    Iterator<T> getIterator() {
        return iterator;
    }

    ListIteratorNext getIteratorNext() {
        return iteratorNext;
    }

    private static final class ListIteratorAllocator implements ObjectAllocator<ListIterator<?>> {
        static final ObjectAllocator<ListIterator<?>> INSTANCE = new ListIteratorAllocator();

        @Override
        public ListIterator<?> newInstance(Realm realm) {
            return new ListIterator<Object>(realm);
        }
    }

    /**
     * 7.4.9 CreateListIterator (list)
     * <p>
     * Returns a new {@link ListIterator} object for the internal list {@code iterator}
     * 
     * @param <T>
     *            the iteration type
     * @param cx
     *            the execution context
     * @param iter
     *            the source iterator
     * @return a new script object iterator
     */
    public static <T> ListIterator<T> CreateListIterator(ExecutionContext cx, Iterator<T> iter) {
        /* step 1 */
        @SuppressWarnings("unchecked")
        ListIterator<T> iterator = (ListIterator<T>) ObjectCreate(cx, Intrinsics.IteratorPrototype,
                ListIteratorAllocator.INSTANCE);
        /* steps 2-3 */
        iterator.iterator = iter;
        /* step 4 */
        ListIteratorNext next = new ListIteratorNext(cx.getRealm());
        /* step 5 */
        iterator.iteratorNext = next;
        /* step 6 */
        CreateDataProperty(cx, iterator, "next", next);
        /* step 7 */
        return iterator;
    }

    /**
     * Returns an {@link Iterator} for {@code iterator}. {@code iterator} is expected to comply to
     * the <code>"25.1.2 The Iterator Interface"</code>.
     * 
     * @param cx
     *            the execution context
     * @param iterator
     *            the script iterator object
     * @return the iterator object
     */
    public static ScriptIterator<?> FromScriptIterator(ExecutionContext cx, ScriptObject iterator) {
        return new ScriptIteratorImpl(cx, iterator);
    }

    private static final class ScriptIteratorImpl extends SimpleIterator<Object> implements
            ScriptIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject iterator;

        ScriptIteratorImpl(ExecutionContext cx, ScriptObject iterator) {
            this.cx = cx;
            this.iterator = iterator;
        }

        @Override
        protected Object findNext() {
            ScriptObject next = IteratorStep(cx, iterator);
            if (next == null) {
                return null;
            }
            return IteratorValue(cx, next);
        }

        @Override
        public ScriptObject getScriptObject() {
            return iterator;
        }
    }
}
