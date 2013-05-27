/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Abstract {@link Iterator} base class
 */
public abstract class SimpleIterator<E> implements Iterator<E> {
    private E next = null;

    /**
     * Returns the next element or {@code null} if the iterator is drained
     */
    protected abstract E tryNext();

    @Override
    public final boolean hasNext() {
        if (next == null) {
            next = tryNext();
        }
        return (next != null);
    }

    @Override
    public final E next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        E next = this.next;
        this.next = null;
        return next;
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
