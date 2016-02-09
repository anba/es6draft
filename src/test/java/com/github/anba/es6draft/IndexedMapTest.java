/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.github.anba.es6draft.runtime.internal.IndexedMap;

/**
 * 
 */
public final class IndexedMapTest {
    private static IndexedMap<String> putAll(IndexedMap<String> indexed, long... indices) {
        for (long index : indices) {
            indexed.put(index, Long.toString(index));
        }
        return indexed;
    }

    private static <T> List<T> ascendingList(IndexedMap<T> indexed, long from, long to) {
        return collect(indexed.ascendingIterator(from, to));
    }

    private static <T> List<T> descendingList(IndexedMap<T> indexed, long from, long to) {
        return collect(indexed.descendingIterator(from, to));
    }

    private static <K, V> List<V> collect(Iterator<Map.Entry<K, V>> iterator) {
        ArrayList<V> list = new ArrayList<>();
        while (iterator.hasNext()) {
            list.add(iterator.next().getValue());
        }
        return list;
    }

    @Test
    public void denseContainsKey() {
        IndexedMap<String> indexed = putAll(new IndexedMap<String>(), 0, 1, 2, 3, 4, 5);

        assertTrue(indexed.containsKey(0));
        assertTrue(indexed.containsKey(1));
        assertTrue(indexed.containsKey(2));
        assertTrue(indexed.containsKey(3));
        assertTrue(indexed.containsKey(4));
        assertTrue(indexed.containsKey(5));
        assertFalse(indexed.containsKey(6));
        assertFalse(indexed.containsKey(7));
        assertFalse(indexed.containsKey(8));
        assertFalse(indexed.containsKey(9));
        assertFalse(indexed.containsKey(10));
    }

    @Test
    public void sparseContainsKey() {
        IndexedMap<String> indexed = putAll(new IndexedMap<String>(), 0, 1, 2, 3, 4, 5);
        indexed.put(10000, "");

        assertTrue(indexed.containsKey(0));
        assertTrue(indexed.containsKey(1));
        assertTrue(indexed.containsKey(2));
        assertTrue(indexed.containsKey(3));
        assertTrue(indexed.containsKey(4));
        assertTrue(indexed.containsKey(5));
        assertFalse(indexed.containsKey(6));
        assertFalse(indexed.containsKey(7));
        assertFalse(indexed.containsKey(8));
        assertFalse(indexed.containsKey(9));
        assertFalse(indexed.containsKey(10));

        assertTrue(indexed.containsKey(10000));
    }

    @Test
    public void denseAscendingRange() {
        IndexedMap<String> indexed = putAll(new IndexedMap<String>(), 0, 1, 2, 3, 4, 5);

        assertThat(ascendingList(indexed, 0, 0), Matchers.empty());
        assertThat(ascendingList(indexed, 1, 1), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 6), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 10, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 10, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(ascendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(ascendingList(indexed, 5, 6), Matchers.contains("5"));

        assertThat(ascendingList(indexed, 0, 2), Matchers.contains("0", "1"));
        assertThat(ascendingList(indexed, 1, 3), Matchers.contains("1", "2"));

        assertThat(ascendingList(indexed, 0, 0), Matchers.empty());
        assertThat(ascendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(ascendingList(indexed, 0, 2), Matchers.contains("0", "1"));
        assertThat(ascendingList(indexed, 0, 3), Matchers.contains("0", "1", "2"));
        assertThat(ascendingList(indexed, 0, 4), Matchers.contains("0", "1", "2", "3"));
        assertThat(ascendingList(indexed, 0, 5), Matchers.contains("0", "1", "2", "3", "4"));
        assertThat(ascendingList(indexed, 0, 6), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 7), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 8), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 9), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 10), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 100), Matchers.contains("0", "1", "2", "3", "4", "5"));

        assertThat(ascendingList(indexed, 1, 1), Matchers.empty());
        assertThat(ascendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(ascendingList(indexed, 1, 3), Matchers.contains("1", "2"));
        assertThat(ascendingList(indexed, 1, 4), Matchers.contains("1", "2", "3"));
        assertThat(ascendingList(indexed, 1, 5), Matchers.contains("1", "2", "3", "4"));
        assertThat(ascendingList(indexed, 1, 6), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 7), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 8), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 9), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 10), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 100), Matchers.contains("1", "2", "3", "4", "5"));

        assertThat(ascendingList(indexed, 2, 2), Matchers.empty());
        assertThat(ascendingList(indexed, 2, 3), Matchers.contains("2"));
        assertThat(ascendingList(indexed, 2, 4), Matchers.contains("2", "3"));
        assertThat(ascendingList(indexed, 2, 5), Matchers.contains("2", "3", "4"));
        assertThat(ascendingList(indexed, 2, 6), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 7), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 8), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 9), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 10), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 100), Matchers.contains("2", "3", "4", "5"));

        assertThat(ascendingList(indexed, 3, 3), Matchers.empty());
        assertThat(ascendingList(indexed, 3, 4), Matchers.contains("3"));
        assertThat(ascendingList(indexed, 3, 5), Matchers.contains("3", "4"));
        assertThat(ascendingList(indexed, 3, 6), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 7), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 8), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 9), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 10), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 100), Matchers.contains("3", "4", "5"));

        assertThat(ascendingList(indexed, 4, 4), Matchers.empty());
        assertThat(ascendingList(indexed, 4, 5), Matchers.contains("4"));
        assertThat(ascendingList(indexed, 4, 6), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 7), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 8), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 9), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 10), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 100), Matchers.contains("4", "5"));

        assertThat(ascendingList(indexed, 5, 5), Matchers.empty());
        assertThat(ascendingList(indexed, 5, 6), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 7), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 8), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 9), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 10), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 100), Matchers.contains("5"));

        assertThat(ascendingList(indexed, 6, 6), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 7, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 8, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 9, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 10, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 10, 100), Matchers.empty());
    }

    @Test
    public void denseDescendingRange() {
        IndexedMap<String> indexed = putAll(new IndexedMap<String>(), 0, 1, 2, 3, 4, 5);

        assertThat(descendingList(indexed, 0, 0), Matchers.empty());
        assertThat(descendingList(indexed, 1, 1), Matchers.empty());
        assertThat(descendingList(indexed, 6, 6), Matchers.empty());
        assertThat(descendingList(indexed, 6, 7), Matchers.empty());
        assertThat(descendingList(indexed, 7, 7), Matchers.empty());
        assertThat(descendingList(indexed, 7, 8), Matchers.empty());
        assertThat(descendingList(indexed, 8, 8), Matchers.empty());
        assertThat(descendingList(indexed, 8, 9), Matchers.empty());
        assertThat(descendingList(indexed, 9, 9), Matchers.empty());
        assertThat(descendingList(indexed, 9, 10), Matchers.empty());
        assertThat(descendingList(indexed, 10, 10), Matchers.empty());
        assertThat(descendingList(indexed, 10, 100), Matchers.empty());

        assertThat(descendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(descendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(descendingList(indexed, 5, 6), Matchers.contains("5"));

        assertThat(descendingList(indexed, 0, 2), Matchers.contains("1", "0"));
        assertThat(descendingList(indexed, 1, 3), Matchers.contains("2", "1"));

        assertThat(descendingList(indexed, 0, 0), Matchers.empty());
        assertThat(descendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(descendingList(indexed, 0, 2), Matchers.contains("1", "0"));
        assertThat(descendingList(indexed, 0, 3), Matchers.contains("2", "1", "0"));
        assertThat(descendingList(indexed, 0, 4), Matchers.contains("3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 5), Matchers.contains("4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 6), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 7), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 8), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 9), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 10), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 100), Matchers.contains("5", "4", "3", "2", "1", "0"));

        assertThat(descendingList(indexed, 1, 1), Matchers.empty());
        assertThat(descendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(descendingList(indexed, 1, 3), Matchers.contains("2", "1"));
        assertThat(descendingList(indexed, 1, 4), Matchers.contains("3", "2", "1"));
        assertThat(descendingList(indexed, 1, 5), Matchers.contains("4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 6), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 7), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 8), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 9), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 10), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 100), Matchers.contains("5", "4", "3", "2", "1"));

        assertThat(descendingList(indexed, 2, 2), Matchers.empty());
        assertThat(descendingList(indexed, 2, 3), Matchers.contains("2"));
        assertThat(descendingList(indexed, 2, 4), Matchers.contains("3", "2"));
        assertThat(descendingList(indexed, 2, 5), Matchers.contains("4", "3", "2"));
        assertThat(descendingList(indexed, 2, 6), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 7), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 8), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 9), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 10), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 100), Matchers.contains("5", "4", "3", "2"));

        assertThat(descendingList(indexed, 3, 3), Matchers.empty());
        assertThat(descendingList(indexed, 3, 4), Matchers.contains("3"));
        assertThat(descendingList(indexed, 3, 5), Matchers.contains("4", "3"));
        assertThat(descendingList(indexed, 3, 6), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 7), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 8), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 9), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 10), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 100), Matchers.contains("5", "4", "3"));

        assertThat(descendingList(indexed, 4, 4), Matchers.empty());
        assertThat(descendingList(indexed, 4, 5), Matchers.contains("4"));
        assertThat(descendingList(indexed, 4, 6), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 7), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 8), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 9), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 10), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 100), Matchers.contains("5", "4"));

        assertThat(descendingList(indexed, 5, 5), Matchers.empty());
        assertThat(descendingList(indexed, 5, 6), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 7), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 8), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 9), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 10), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 100), Matchers.contains("5"));

        assertThat(descendingList(indexed, 6, 6), Matchers.empty());
        assertThat(descendingList(indexed, 6, 7), Matchers.empty());
        assertThat(descendingList(indexed, 6, 8), Matchers.empty());
        assertThat(descendingList(indexed, 6, 9), Matchers.empty());
        assertThat(descendingList(indexed, 6, 10), Matchers.empty());
        assertThat(descendingList(indexed, 6, 100), Matchers.empty());

        assertThat(descendingList(indexed, 7, 7), Matchers.empty());
        assertThat(descendingList(indexed, 7, 8), Matchers.empty());
        assertThat(descendingList(indexed, 7, 9), Matchers.empty());
        assertThat(descendingList(indexed, 7, 10), Matchers.empty());
        assertThat(descendingList(indexed, 7, 100), Matchers.empty());

        assertThat(descendingList(indexed, 8, 8), Matchers.empty());
        assertThat(descendingList(indexed, 8, 9), Matchers.empty());
        assertThat(descendingList(indexed, 8, 10), Matchers.empty());
        assertThat(descendingList(indexed, 8, 100), Matchers.empty());

        assertThat(descendingList(indexed, 9, 9), Matchers.empty());
        assertThat(descendingList(indexed, 9, 10), Matchers.empty());
        assertThat(descendingList(indexed, 9, 100), Matchers.empty());

        assertThat(descendingList(indexed, 10, 10), Matchers.empty());
        assertThat(descendingList(indexed, 10, 100), Matchers.empty());
    }

    @Test
    public void sparseAscendingRange() {
        IndexedMap<String> indexed = putAll(new IndexedMap<String>(), 0, 1, 2, 3, 4, 5);
        indexed.put(10000, "");

        assertThat(ascendingList(indexed, 0, 0), Matchers.empty());
        assertThat(ascendingList(indexed, 1, 1), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 6), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 10, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 10, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(ascendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(ascendingList(indexed, 5, 6), Matchers.contains("5"));

        assertThat(ascendingList(indexed, 0, 2), Matchers.contains("0", "1"));
        assertThat(ascendingList(indexed, 1, 3), Matchers.contains("1", "2"));

        assertThat(ascendingList(indexed, 0, 0), Matchers.empty());
        assertThat(ascendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(ascendingList(indexed, 0, 2), Matchers.contains("0", "1"));
        assertThat(ascendingList(indexed, 0, 3), Matchers.contains("0", "1", "2"));
        assertThat(ascendingList(indexed, 0, 4), Matchers.contains("0", "1", "2", "3"));
        assertThat(ascendingList(indexed, 0, 5), Matchers.contains("0", "1", "2", "3", "4"));
        assertThat(ascendingList(indexed, 0, 6), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 7), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 8), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 9), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 10), Matchers.contains("0", "1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 0, 100), Matchers.contains("0", "1", "2", "3", "4", "5"));

        assertThat(ascendingList(indexed, 1, 1), Matchers.empty());
        assertThat(ascendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(ascendingList(indexed, 1, 3), Matchers.contains("1", "2"));
        assertThat(ascendingList(indexed, 1, 4), Matchers.contains("1", "2", "3"));
        assertThat(ascendingList(indexed, 1, 5), Matchers.contains("1", "2", "3", "4"));
        assertThat(ascendingList(indexed, 1, 6), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 7), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 8), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 9), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 10), Matchers.contains("1", "2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 1, 100), Matchers.contains("1", "2", "3", "4", "5"));

        assertThat(ascendingList(indexed, 2, 2), Matchers.empty());
        assertThat(ascendingList(indexed, 2, 3), Matchers.contains("2"));
        assertThat(ascendingList(indexed, 2, 4), Matchers.contains("2", "3"));
        assertThat(ascendingList(indexed, 2, 5), Matchers.contains("2", "3", "4"));
        assertThat(ascendingList(indexed, 2, 6), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 7), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 8), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 9), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 10), Matchers.contains("2", "3", "4", "5"));
        assertThat(ascendingList(indexed, 2, 100), Matchers.contains("2", "3", "4", "5"));

        assertThat(ascendingList(indexed, 3, 3), Matchers.empty());
        assertThat(ascendingList(indexed, 3, 4), Matchers.contains("3"));
        assertThat(ascendingList(indexed, 3, 5), Matchers.contains("3", "4"));
        assertThat(ascendingList(indexed, 3, 6), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 7), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 8), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 9), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 10), Matchers.contains("3", "4", "5"));
        assertThat(ascendingList(indexed, 3, 100), Matchers.contains("3", "4", "5"));

        assertThat(ascendingList(indexed, 4, 4), Matchers.empty());
        assertThat(ascendingList(indexed, 4, 5), Matchers.contains("4"));
        assertThat(ascendingList(indexed, 4, 6), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 7), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 8), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 9), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 10), Matchers.contains("4", "5"));
        assertThat(ascendingList(indexed, 4, 100), Matchers.contains("4", "5"));

        assertThat(ascendingList(indexed, 5, 5), Matchers.empty());
        assertThat(ascendingList(indexed, 5, 6), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 7), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 8), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 9), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 10), Matchers.contains("5"));
        assertThat(ascendingList(indexed, 5, 100), Matchers.contains("5"));

        assertThat(ascendingList(indexed, 6, 6), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 6, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 7, 7), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 7, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 8, 8), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 8, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 9, 9), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 9, 100), Matchers.empty());

        assertThat(ascendingList(indexed, 10, 10), Matchers.empty());
        assertThat(ascendingList(indexed, 10, 100), Matchers.empty());
    }

    @Test
    public void sparseDescendingRange() {
        IndexedMap<String> indexed = putAll(new IndexedMap<String>(), 0, 1, 2, 3, 4, 5);
        indexed.put(10000, "");

        assertThat(descendingList(indexed, 0, 0), Matchers.empty());
        assertThat(descendingList(indexed, 1, 1), Matchers.empty());
        assertThat(descendingList(indexed, 6, 6), Matchers.empty());
        assertThat(descendingList(indexed, 6, 7), Matchers.empty());
        assertThat(descendingList(indexed, 7, 7), Matchers.empty());
        assertThat(descendingList(indexed, 7, 8), Matchers.empty());
        assertThat(descendingList(indexed, 8, 8), Matchers.empty());
        assertThat(descendingList(indexed, 8, 9), Matchers.empty());
        assertThat(descendingList(indexed, 9, 9), Matchers.empty());
        assertThat(descendingList(indexed, 9, 10), Matchers.empty());
        assertThat(descendingList(indexed, 10, 10), Matchers.empty());
        assertThat(descendingList(indexed, 10, 100), Matchers.empty());

        assertThat(descendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(descendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(descendingList(indexed, 5, 6), Matchers.contains("5"));

        assertThat(descendingList(indexed, 0, 2), Matchers.contains("1", "0"));
        assertThat(descendingList(indexed, 1, 3), Matchers.contains("2", "1"));

        assertThat(descendingList(indexed, 0, 0), Matchers.empty());
        assertThat(descendingList(indexed, 0, 1), Matchers.contains("0"));
        assertThat(descendingList(indexed, 0, 2), Matchers.contains("1", "0"));
        assertThat(descendingList(indexed, 0, 3), Matchers.contains("2", "1", "0"));
        assertThat(descendingList(indexed, 0, 4), Matchers.contains("3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 5), Matchers.contains("4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 6), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 7), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 8), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 9), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 10), Matchers.contains("5", "4", "3", "2", "1", "0"));
        assertThat(descendingList(indexed, 0, 100), Matchers.contains("5", "4", "3", "2", "1", "0"));

        assertThat(descendingList(indexed, 1, 1), Matchers.empty());
        assertThat(descendingList(indexed, 1, 2), Matchers.contains("1"));
        assertThat(descendingList(indexed, 1, 3), Matchers.contains("2", "1"));
        assertThat(descendingList(indexed, 1, 4), Matchers.contains("3", "2", "1"));
        assertThat(descendingList(indexed, 1, 5), Matchers.contains("4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 6), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 7), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 8), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 9), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 10), Matchers.contains("5", "4", "3", "2", "1"));
        assertThat(descendingList(indexed, 1, 100), Matchers.contains("5", "4", "3", "2", "1"));

        assertThat(descendingList(indexed, 2, 2), Matchers.empty());
        assertThat(descendingList(indexed, 2, 3), Matchers.contains("2"));
        assertThat(descendingList(indexed, 2, 4), Matchers.contains("3", "2"));
        assertThat(descendingList(indexed, 2, 5), Matchers.contains("4", "3", "2"));
        assertThat(descendingList(indexed, 2, 6), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 7), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 8), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 9), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 10), Matchers.contains("5", "4", "3", "2"));
        assertThat(descendingList(indexed, 2, 100), Matchers.contains("5", "4", "3", "2"));

        assertThat(descendingList(indexed, 3, 3), Matchers.empty());
        assertThat(descendingList(indexed, 3, 4), Matchers.contains("3"));
        assertThat(descendingList(indexed, 3, 5), Matchers.contains("4", "3"));
        assertThat(descendingList(indexed, 3, 6), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 7), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 8), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 9), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 10), Matchers.contains("5", "4", "3"));
        assertThat(descendingList(indexed, 3, 100), Matchers.contains("5", "4", "3"));

        assertThat(descendingList(indexed, 4, 4), Matchers.empty());
        assertThat(descendingList(indexed, 4, 5), Matchers.contains("4"));
        assertThat(descendingList(indexed, 4, 6), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 7), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 8), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 9), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 10), Matchers.contains("5", "4"));
        assertThat(descendingList(indexed, 4, 100), Matchers.contains("5", "4"));

        assertThat(descendingList(indexed, 5, 5), Matchers.empty());
        assertThat(descendingList(indexed, 5, 6), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 7), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 8), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 9), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 10), Matchers.contains("5"));
        assertThat(descendingList(indexed, 5, 100), Matchers.contains("5"));

        assertThat(descendingList(indexed, 6, 6), Matchers.empty());
        assertThat(descendingList(indexed, 6, 7), Matchers.empty());
        assertThat(descendingList(indexed, 6, 8), Matchers.empty());
        assertThat(descendingList(indexed, 6, 9), Matchers.empty());
        assertThat(descendingList(indexed, 6, 10), Matchers.empty());
        assertThat(descendingList(indexed, 6, 100), Matchers.empty());

        assertThat(descendingList(indexed, 7, 7), Matchers.empty());
        assertThat(descendingList(indexed, 7, 8), Matchers.empty());
        assertThat(descendingList(indexed, 7, 9), Matchers.empty());
        assertThat(descendingList(indexed, 7, 10), Matchers.empty());
        assertThat(descendingList(indexed, 7, 100), Matchers.empty());

        assertThat(descendingList(indexed, 8, 8), Matchers.empty());
        assertThat(descendingList(indexed, 8, 9), Matchers.empty());
        assertThat(descendingList(indexed, 8, 10), Matchers.empty());
        assertThat(descendingList(indexed, 8, 100), Matchers.empty());

        assertThat(descendingList(indexed, 9, 9), Matchers.empty());
        assertThat(descendingList(indexed, 9, 10), Matchers.empty());
        assertThat(descendingList(indexed, 9, 100), Matchers.empty());

        assertThat(descendingList(indexed, 10, 10), Matchers.empty());
        assertThat(descendingList(indexed, 10, 100), Matchers.empty());
    }
}
