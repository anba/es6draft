/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 */
public final class CompoundList<E> extends AbstractList<E> {
    private final List<? extends E> firstList;
    private final List<? extends E> secondList;

    public CompoundList(List<? extends E> firstList, List<? extends E> secondList) {
        this.firstList = firstList;
        this.secondList = secondList;
    }

    @Override
    public E get(int index) {
        final int firstListSize = firstList.size();
        if (index < firstListSize) {
            return firstList.get(index);
        }
        return secondList.get(index - firstListSize);
    }

    @Override
    public int size() {
        return firstList.size() + secondList.size();
    }

    @Override
    public Iterator<E> iterator() {
        return new CompoundIterator<>(firstList.iterator(), secondList.iterator());
    }
}
