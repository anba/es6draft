/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.github.anba.es6draft.runtime.internal.LinkedMap;

/**
 * 
 */
public class LinkedMapTest {

    @Test
    public void test() {
        LinkedMap<String, String> map = new LinkedMap<>();

        assertEquals(0, map.size());
        map.set("key1", "value1");
        assertEquals(1, map.size());
        map.set("key2", "value2");
        assertEquals(2, map.size());
        map.set("key2", "value2-new");
        assertEquals(2, map.size());

        assertTrue(map.has("key1"));
        assertTrue(map.has("key2"));
        assertFalse(map.has("key3"));

        map.delete("key2");
        assertEquals(1, map.size());
        assertTrue(map.has("key1"));
        assertFalse(map.has("key2"));
        assertFalse(map.has("key3"));

        map.delete("key2");
        assertEquals(1, map.size());
        assertTrue(map.has("key1"));
        assertFalse(map.has("key2"));
        assertFalse(map.has("key3"));
    }

    private <K, V> void assertEntryEquals(K key, V value, Entry<K, V> entry) {
        assertEquals(key, entry.getKey());
        assertEquals(value, entry.getValue());
    }

    @Test
    public void testIterator1() {
        LinkedMap<String, String> map = new LinkedMap<>();
        map.set("key1", "value1");
        map.set("key2", "value2");

        Iterator<Entry<String, String>> itr = map.iterator();

        assertTrue(itr.hasNext());
        assertTrue(itr.hasNext());
        assertTrue(itr.hasNext());
        assertEntryEquals("key1", "value1", itr.next());

        assertTrue(itr.hasNext());
        assertEntryEquals("key2", "value2", itr.next());

        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testIterator2() {
        LinkedMap<String, String> map = new LinkedMap<>();

        Iterator<Entry<String, String>> itr = map.iterator();

        map.set("key1", "value1");
        assertTrue(itr.hasNext());
        assertEntryEquals("key1", "value1", itr.next());

        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testIterator3() {
        LinkedMap<String, String> map = new LinkedMap<>();

        Iterator<Entry<String, String>> itr = map.iterator();

        // iterator finished concept not available
        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }

        map.set("key1", "value1");
        assertTrue(itr.hasNext());
        assertEntryEquals("key1", "value1", itr.next());

        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testIterator4() {
        LinkedMap<String, String> map = new LinkedMap<>();

        Iterator<Entry<String, String>> itr = map.iterator();

        map.set("key1", "value1");
        map.set("key2", "value2");

        assertTrue(itr.hasNext());
        assertEntryEquals("key1", "value1", itr.next());

        assertTrue(itr.hasNext());
        assertEntryEquals("key2", "value2", itr.next());

        assertFalse(itr.hasNext());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testIterator5() {
        LinkedMap<String, String> map = new LinkedMap<>();

        Iterator<Entry<String, String>> itr = map.iterator();

        map.set("key1", "value1");
        map.set("key2", "value2");
        map.set("key3", "value3");
        map.set("key4", "value4");

        assertTrue(itr.hasNext());
        assertEntryEquals("key1", "value1", itr.next());

        assertTrue(itr.hasNext());

        map.delete("key1");

        assertTrue(itr.hasNext());
        assertEntryEquals("key2", "value2", itr.next());
        assertEntryEquals("key3", "value3", itr.next());
        assertEntryEquals("key4", "value4", itr.next());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testIterator6() {
        LinkedMap<String, String> map = new LinkedMap<>();

        Iterator<Entry<String, String>> itr = map.iterator();

        map.set("key1", "value1");
        map.set("key2", "value2");
        map.set("key3", "value3");
        map.set("key4", "value4");

        assertTrue(itr.hasNext());
        assertEntryEquals("key1", "value1", itr.next());

        assertTrue(itr.hasNext());

        map.delete("key1");
        map.delete("key2");

        assertTrue(itr.hasNext());
        assertEntryEquals("key3", "value3", itr.next());
        assertEntryEquals("key4", "value4", itr.next());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testIterator7() {
        LinkedMap<String, String> map = new LinkedMap<>();

        Iterator<Entry<String, String>> itr = map.iterator();

        map.set("key1", "value1");
        map.set("key2", "value2");
        map.set("key3", "value3");
        map.set("key4", "value4");

        assertTrue(itr.hasNext());
        assertEntryEquals("key1", "value1", itr.next());

        assertTrue(itr.hasNext());

        map.delete("key1");
        map.delete("key3");

        assertTrue(itr.hasNext());
        assertEntryEquals("key2", "value2", itr.next());
        assertEntryEquals("key4", "value4", itr.next());
        try {
            itr.next();
            fail();
        } catch (NoSuchElementException e) {
        }
    }
}
