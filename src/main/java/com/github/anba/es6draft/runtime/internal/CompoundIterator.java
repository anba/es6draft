/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 */
public final class CompoundIterator<E> implements Iterator<E> {
    private final Iterator<? extends E> firstIterator;
    private final Iterator<? extends E> secondIterator;

    public CompoundIterator(Iterator<? extends E> firstIterator,
            Iterator<? extends E> secondIterator) {
        this.firstIterator = firstIterator;
        this.secondIterator = secondIterator;
    }

    @Override
    public boolean hasNext() {
        return firstIterator.hasNext() || secondIterator.hasNext();
    }

    @Override
    public E next() {
        if (firstIterator.hasNext()) {
            return firstIterator.next();
        }
        if (secondIterator.hasNext()) {
            return secondIterator.next();
        }
        throw new NoSuchElementException();
    }
}
