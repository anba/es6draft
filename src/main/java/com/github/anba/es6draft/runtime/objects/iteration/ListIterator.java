/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateMethodProperty;

import java.util.Iterator;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
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
    /** [[IteratedList]] and [[ListIteratorNextIndex]] */
    private final Iterator<T> iterator;

    /** [[IteratorNext]] */
    private final ListIteratorNext iteratorNext;

    private ListIterator(Realm realm, Iterator<T> iterator, ListIteratorNext iteratorNext,
            ScriptObject prototype) {
        super(realm);
        this.iterator = iterator;
        this.iteratorNext = iteratorNext;
        setPrototype(prototype);
    }

    Iterator<T> getIterator() {
        return iterator;
    }

    ListIteratorNext getIteratorNext() {
        return iteratorNext;
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
        /* steps 1-5 */
        ListIterator<T> iterator = new ListIterator<>(cx.getRealm(), iter, new ListIteratorNext(
                cx.getRealm()), cx.getIntrinsic(Intrinsics.IteratorPrototype));
        /* step 6 */
        CreateMethodProperty(cx, iterator, "next", iterator.iteratorNext);
        /* step 7 */
        return iterator;
    }
}
