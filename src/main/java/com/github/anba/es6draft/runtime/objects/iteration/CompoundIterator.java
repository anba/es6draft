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
 * <li>7.4.9 CreateCompoundIterator ( iterator1, iterator2 )
 * </ul>
 */
public final class CompoundIterator<T> extends OrdinaryObject {
    /** [[Iterator1]] */
    private final Iterator<T> iterator1;

    /** [[Iterator2]] */
    private final Iterator<T> iterator2;

    /** [[IteratorNext]] */
    private final CompoundIteratorNext iteratorNext;

    /** [[State]] */
    private State state = State.First;

    private CompoundIterator(Realm realm, Iterator<T> iterator1, Iterator<T> iterator2,
            CompoundIteratorNext iteratorNext, ScriptObject prototype) {
        super(realm);
        this.iterator1 = iterator1;
        this.iterator2 = iterator2;
        this.iteratorNext = iteratorNext;
        setPrototype(prototype);
    }

    enum State {
        First, Second
    }

    Iterator<T> getFirstIterator() {
        assert state == State.First;
        return iterator1;
    }

    Iterator<T> getSecondIterator() {
        assert state == State.Second;
        return iterator2;
    }

    CompoundIteratorNext getIteratorNext() {
        return iteratorNext;
    }

    State getState() {
        return state;
    }

    void setState(State state) {
        this.state = state;
    }

    /**
     * 7.4.9 CreateCompoundIterator ( iterator1, iterator2 )
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
        /* steps 1-6 */
        CompoundIterator<T> iterator = new CompoundIterator<>(cx.getRealm(), iterator1, iterator2,
                new CompoundIteratorNext(cx.getRealm()),
                cx.getIntrinsic(Intrinsics.IteratorPrototype));
        /* step 7 */
        CreateMethodProperty(cx, iterator, "next", iterator.iteratorNext);
        /* step 8 */
        return iterator;
    }
}
