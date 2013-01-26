/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Simple list implementation with three fixed slots to avoid array allocation
 */
public final class SmallArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess {
    private static final int OFFSET = 3;
    private static final int INIT_SIZE = 10;

    private int size = 0;
    private E fst = null;
    private E snd = null;
    private E thd = null;
    private E[] extended = null;

    @SuppressWarnings("unchecked")
    private static <T> T[] newArray(int len) {
        return (T[]) new Object[len];
    }

    private static boolean eq(Object a, Object b) {
        return Objects.equals(a, b);
    }

    private void ensureCapacity(int c) {
        if (c > OFFSET) {
            if (extended == null) {
                extended = newArray(Math.max(c, INIT_SIZE));
                extended[0] = fst;
                extended[1] = snd;
                extended[2] = thd;
                fst = snd = thd = null;
            } else if (c > extended.length) {
                extended = Arrays.copyOf(extended,
                        Math.max(c, extended.length + (extended.length >> 1)));
            }
        }
    }

    private E uncheckedGet(int index) {
        if (extended != null) {
            return extended[index];
        }
        switch (index) {
        case 0:
            return fst;
        case 1:
            return snd;
        case 2:
            return thd;
        default:
            throw new IllegalStateException();
        }
    }

    private void uncheckedSet(int index, E e) {
        if (extended != null) {
            extended[index] = e;
            return;
        }
        switch (index) {
        case 0:
            fst = e;
            return;
        case 1:
            snd = e;
            return;
        case 2:
            thd = e;
            return;
        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        size = 0;
        fst = null;
        snd = null;
        thd = null;
        extended = null;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i == -1) {
            return false;
        }
        remove(i);
        return true;
    }

    @Override
    public boolean add(E e) {
        int size = this.size;
        ensureCapacity(size + 1);
        uncheckedSet(size, e);
        this.size += 1;
        return true;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        return uncheckedGet(index);
    }

    @Override
    public E set(int index, E element) {
        E prev = get(index);
        uncheckedSet(index, element);
        return prev;
    }

    @Override
    public void add(int index, E element) {
        int size = this.size;
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException();
        ensureCapacity(size + 1);
        if (extended != null) {
            System.arraycopy(extended, index, extended, index + 1, size - index);
        } else {
            switch (index) {
            case 0:
                thd = snd;
                snd = fst;
                break;
            case 1:
                thd = snd;
                break;
            case 2:
                break;
            default:
                throw new IllegalStateException();
            }
        }
        uncheckedSet(index, element);
        this.size += 1;
    }

    @Override
    public E remove(int index) {
        E prev = get(index);
        int size = this.size;
        if (extended != null) {
            int shift = size - index - 1;
            if (shift > 0) {
                System.arraycopy(extended, index + 1, extended, index, shift);
            }
            extended[size - 1] = null;
        } else {
            switch (index) {
            case 0:
                fst = snd;
            case 1:
                snd = thd;
            case 2:
                thd = null;
                break;
            default:
                throw new IllegalStateException();
            }
        }
        this.size -= 1;
        return prev;
    }

    @Override
    public int indexOf(Object o) {
        int size = this.size;
        if (extended != null) {
            for (int i = 0; i < size; ++i) {
                if (eq(extended[i], o))
                    return i;
            }
        } else {
            if (size > 0 && eq(fst, o))
                return 0;
            if (size > 1 && eq(snd, o))
                return 1;
            if (size > 2 && eq(thd, o))
                return 2;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int size = this.size;
        if (extended != null) {
            for (int i = size - 1; i >= 0; --i) {
                if (eq(extended[i], o))
                    return i;
            }
        } else {
            if (size > 2 && eq(thd, o))
                return 2;
            if (size > 1 && eq(snd, o))
                return 1;
            if (size > 0 && eq(fst, o))
                return 0;
        }
        return -1;
    }
}
