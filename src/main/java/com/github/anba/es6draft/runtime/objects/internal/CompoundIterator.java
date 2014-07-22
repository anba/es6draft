/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.internal;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>7 Abstract Operations</h1><br>
 * <h2>7.4 Operations on Iterator Objects</h2>
 * <ul>
 * <li>7.4.10 CreateCompoundIterator ( iterator1, iterator2 )
 * </ul>
 */
public final class CompoundIterator<T> extends OrdinaryObject {
    private Iterator<T> iterator1;
    private Iterator<T> iterator2;
    private CompoundIteratorNext iteratorNext;
    private boolean firstIteratorActive = true;

    public CompoundIterator(Realm realm) {
        super(realm);
    }

    Iterator<T> getFirstIterator() {
        assert firstIteratorActive;
        return iterator1;
    }

    Iterator<T> getSecondIterator() {
        assert !firstIteratorActive;
        return iterator2;
    }

    CompoundIteratorNext getIteratorNext() {
        return iteratorNext;
    }

    boolean isFirstIteratorActive() {
        return firstIteratorActive;
    }

    void setFirstIteratorActive(boolean firstIteratorActive) {
        this.firstIteratorActive = firstIteratorActive;
    }

    private static final class CompoundIteratorAllocator implements
            ObjectAllocator<CompoundIterator<?>> {
        static final ObjectAllocator<CompoundIterator<?>> INSTANCE = new CompoundIteratorAllocator();

        @Override
        public CompoundIterator<?> newInstance(Realm realm) {
            return new CompoundIterator<Object>(realm);
        }
    }

    /**
     * 7.4.10 CreateCompoundIterator ( iterator1, iterator2 )
     * <p>
     * Returns a new {@link CompoundIterator} object for the internal list {@code iterator}
     * 
     * @param <T>
     *            the iteration type
     * @param cx
     *            the execution context
     * @param iterator1
     *            the first source iterator
     * @param iterator2
     *            the second source iterator
     * @return a new script object iterator
     */
    public static <T> CompoundIterator<T> CreateCompoundIterator(ExecutionContext cx,
            Iterator<T> iterator1, Iterator<T> iterator2) {
        /* step 1 */
        @SuppressWarnings("unchecked")
        CompoundIterator<T> iterator = (CompoundIterator<T>) ObjectCreate(cx,
                Intrinsics.ObjectPrototype, CompoundIteratorAllocator.INSTANCE);
        /* steps 2-4 */
        iterator.iterator1 = iterator1;
        iterator.iterator2 = iterator2;
        /* step 5 */
        CompoundIteratorNext next = new CompoundIteratorNext(cx.getRealm());
        /* step 6 */
        iterator.iteratorNext = next;
        /* step 7 */
        CreateDataProperty(cx, iterator, "next", next);
        /* step 8 */
        return iterator;
    }
}
