/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Applies the standard {@code filter} and {@code map} list operations on {@link Iterable}s to form
 * a simple list comprehension:
 * 
 * <pre>
 * Prelude> let filterMap f m = (map m . filter f)
 * Prelude> :t filterMap
 * filterMap :: (a -> Bool) -> (a -> b) -> [a] -> [b]
 * Prelude> filterMap (>3) (*2) [1..6]
 * [8,10,12]
 * Prelude> [x * 2 | x <- [1..6], (x > 3)]
 * [8,10,12]
 * </pre>
 */
public abstract class FilterMap<T, U> implements Iterable<U> {
    private final Iterable<T> base;

    public FilterMap(Iterable<T> base) {
        this.base = base;
    }

    /**
     * If {@code filter()} returns {@code true}, the value will be included in the final result.
     * Otherwise it will be omitted.
     */
    protected abstract boolean filter(T value);

    /**
     * Maps the value to the final result type.
     */
    protected abstract U map(T value);

    @Override
    public Iterator<U> iterator() {
        return new Iterator<U>() {
            Iterator<T> itr = base.iterator();
            boolean hasNext = false;
            T value = null;

            @Override
            public boolean hasNext() {
                if (hasNext) {
                    return true;
                }
                while (itr.hasNext()) {
                    T next = itr.next();
                    if (filter(next)) {
                        hasNext = true;
                        value = next;
                        return true;
                    }
                }
                return false;
            }

            @Override
            public U next() {
                if (hasNext()) {
                    hasNext = false;
                    return map(value);
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
