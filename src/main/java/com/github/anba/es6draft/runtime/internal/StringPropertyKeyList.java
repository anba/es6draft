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
 *
 */
public final class StringPropertyKeyList extends AbstractList<String> {
    private final int length;

    public StringPropertyKeyList(int length) {
        assert length >= 0;
        this.length = length;
    }

    @Override
    public String get(int index) {
        return Integer.toString(index);
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iter(length);
    }

    private static final class Iter implements Iterator<String> {
        private final int length;
        private int index = 0;

        Iter(int length) {
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
            return Integer.toString(index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
