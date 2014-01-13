/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * Simple list implementation with three fixed slots to avoid array allocation
 */
public final class SmallArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess {
    private static final int INIT_SIZE = 10;

    private int capacity = 3;
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
        if (c > capacity) {
            E[] ext = this.extended;
            if (ext == null) {
                int len = Math.max(c, INIT_SIZE);
                E[] array = newArray(len);
                array[0] = fst;
                array[1] = snd;
                array[2] = thd;
                capacity = len;
                extended = array;
                fst = snd = thd = null;
            } else {
                int len = Math.max(c, ext.length + (ext.length >> 1));
                E[] array = Arrays.copyOf(ext, len);
                capacity = len;
                extended = array;
            }
        }
    }

    private E uncheckedGet(int index) {
        E[] ext = this.extended;
        if (ext != null) {
            return ext[index];
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
        E[] ext = this.extended;
        if (ext != null) {
            ext[index] = e;
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
        modCount++;
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
        modCount++;
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
        E[] ext = this.extended;
        if (ext != null) {
            System.arraycopy(ext, index, ext, index + 1, size - index);
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
        modCount++;
        this.size += 1;
    }

    @Override
    public E remove(int index) {
        E prev = get(index);
        int size = this.size;
        E[] ext = this.extended;
        if (ext != null) {
            int shift = size - index - 1;
            if (shift > 0) {
                System.arraycopy(ext, index + 1, ext, index, shift);
            }
            ext[size - 1] = null;
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
        modCount++;
        this.size -= 1;
        return prev;
    }

    @Override
    public int indexOf(Object o) {
        int size = this.size;
        E[] ext = this.extended;
        if (ext != null) {
            for (int i = 0; i < size; ++i) {
                if (eq(ext[i], o))
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
        E[] ext = this.extended;
        if (ext != null) {
            for (int i = size - 1; i >= 0; --i) {
                if (eq(ext[i], o))
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

    @Override
    public Object[] toArray() {
        int size = this.size;
        E[] ext = this.extended;
        if (ext != null) {
            return Arrays.copyOf(ext, size);
        }
        Object[] array = new Object[size];
        switch (size) {
        default:
            throw new IllegalStateException();
        case 3:
            array[2] = thd;
        case 2:
            array[1] = snd;
        case 1:
            array[0] = fst;
        case 0:
            return array;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new IteratorImpl();
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ListIteratorImpl(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException();
        return new ListIteratorImpl(index);
    }

    private final class IteratorImpl implements Iterator<E> {
        private final int expectedModCount = SmallArrayList.this.modCount;
        private int cursor = 0;

        @Override
        public boolean hasNext() {
            return (cursor < size);
        }

        @Override
        public E next() {
            if (expectedModCount != SmallArrayList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (cursor >= size) {
                throw new NoSuchElementException();
            }
            return uncheckedGet(cursor++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final class ListIteratorImpl implements ListIterator<E> {
        private final int expectedModCount = SmallArrayList.this.modCount;
        private int cursor;

        ListIteratorImpl(int cursor) {
            this.cursor = cursor;
        }

        @Override
        public boolean hasNext() {
            return (cursor < size);
        }

        @Override
        public E next() {
            if (expectedModCount != SmallArrayList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (cursor >= size) {
                throw new NoSuchElementException();
            }
            return uncheckedGet(cursor++);
        }

        @Override
        public boolean hasPrevious() {
            return (cursor > 0);
        }

        @Override
        public E previous() {
            if (expectedModCount != SmallArrayList.this.modCount) {
                throw new ConcurrentModificationException();
            }
            if (cursor <= 0) {
                throw new NoSuchElementException();
            }
            return uncheckedGet(--cursor);
        }

        @Override
        public int nextIndex() {
            return cursor;
        }

        @Override
        public int previousIndex() {
            return cursor - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }
}
