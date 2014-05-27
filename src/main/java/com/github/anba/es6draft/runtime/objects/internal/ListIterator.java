/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.Get;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorStep;
import static com.github.anba.es6draft.runtime.AbstractOperations.IteratorValue;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToLength;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.SimpleIterator;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>7 Abstract Operations</h1><br>
 * <h2>7.4 Operations on Iterator Objects</h2>
 * <ul>
 * <li>7.4.8 CreateListIterator (list)
 * </ul>
 */
public final class ListIterator<T> extends OrdinaryObject {
    private Iterator<T> iterator;

    public ListIterator(Realm realm) {
        super(realm);
    }

    Iterator<T> getIterator() {
        return iterator;
    }

    private static final class ListIteratorAllocator implements ObjectAllocator<ListIterator<?>> {
        static final ObjectAllocator<ListIterator<?>> INSTANCE = new ListIteratorAllocator();

        @Override
        public ListIterator<?> newInstance(Realm realm) {
            return new ListIterator<Object>(realm);
        }
    }

    /**
     * 7.4.8 CreateListIterator (list)
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
        ListIterator<T> iterator = (ListIterator<T>) ObjectCreate(cx, Intrinsics.ObjectPrototype,
                ListIteratorAllocator.INSTANCE);
        /* steps 2-3 */
        iterator.iterator = iter;
        /* step 4 */
        ScriptObject listIteratorNext = cx.getIntrinsic(Intrinsics.ListIteratorNext);
        PropertyDescriptor desc = new PropertyDescriptor(listIteratorNext, true, false, true);
        iterator.defineOwnProperty(cx, "next", desc);
        /* step 5 */
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
    public static Iterator<?> FromScriptIterator(ExecutionContext cx, ScriptObject iterator) {
        return new ScriptIterator(cx, iterator);
    }

    /**
     * Returns an {@link Iterator} for {@code arrayLike}.
     * 
     * @param cx
     *            the execution context
     * @param arrayLike
     *            the array-like script object
     * @return the iterator object
     */
    public static Iterator<?> FromScriptArray(ExecutionContext cx, ScriptObject arrayLike) {
        return new ArrayIterator(cx, arrayLike);
    }

    private static final class ScriptIterator extends SimpleIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject object;

        ScriptIterator(ExecutionContext cx, ScriptObject object) {
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

    private static final class ArrayIterator extends SimpleIterator<Object> {
        private final ExecutionContext cx;
        private final ScriptObject arrayLike;
        private final long length;
        private long index = 0;

        ArrayIterator(ExecutionContext cx, ScriptObject arrayLike) {
            this.cx = cx;
            this.arrayLike = arrayLike;
            this.length = ToLength(cx, Get(cx, arrayLike, "length"));
        }

        @Override
        protected Object tryNext() {
            if (index >= length) {
                return null;
            }
            return Get(cx, arrayLike, index++);
        }
    }
}
