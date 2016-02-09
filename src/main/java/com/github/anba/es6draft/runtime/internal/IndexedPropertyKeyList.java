/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An unmodifiable list of string valued integer keys.
 */
public final class IndexedPropertyKeyList extends AbstractList<String> {
    private final long length;

    /**
     * Constructs a new IndexedPropertyKeyList instance.
     * 
     * @param length
     *            the end index
     */
    public IndexedPropertyKeyList(long length) {
        assert length >= 0;
        this.length = length;
    }

    @Override
    public String get(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException();
        }
        return Integer.toString(index);
    }

    @Override
    public int size() {
        return (int) Math.min(length, Integer.MAX_VALUE);
    }

    @Override
    public Iterator<String> iterator() {
        return new Iter(length);
    }

    private static final class Iter implements Iterator<String> {
        private final long length;
        private long index = 0;

        Iter(long length) {
            this.length = length;
        }

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public String next() {
            if (index >= length) {
                throw new NoSuchElementException();
            }
            return Long.toString(index++);
        }
    }
}
