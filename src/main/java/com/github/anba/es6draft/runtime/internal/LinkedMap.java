/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Simple double-linked map implementation without fail-fast iterator.
 */
public class LinkedMap<KEY, VALUE> implements Iterable<Map.Entry<KEY, VALUE>> {
    @SuppressWarnings("serial")
    private static final class Entry<KEY, VALUE> extends SimpleEntry<KEY, VALUE> {
        private Entry<KEY, VALUE> prev, next;
        private boolean removed = false;

        Entry(KEY key, VALUE value) {
            super(key, value);
        }
    }

    private final HashMap<KEY, Entry<KEY, VALUE>> map;
    private final Entry<KEY, VALUE> head;

    /**
     * Construct a new empty map.
     */
    public LinkedMap() {
        map = new HashMap<>();
        head = new Entry<>(null, null);
        head.prev = head;
        head.next = head;
    }

    private void insert(KEY hashKey, VALUE value) {
        Entry<KEY, VALUE> entry = new Entry<>(hashKey, value);
        map.put(hashKey, entry);
        entry.prev = head.prev;
        entry.next = head;
        head.prev.next = entry;
        head.prev = entry;
    }

    /**
     * Returns the hash-key for <var>key</var>.
     * 
     * @param key
     *            the key
     * @return the hash-key
     */
    protected KEY hashKey(KEY key) {
        return key;
    }

    /**
     * Returns the number of mappings.
     * 
     * @return the number of mappings
     */
    public int size() {
        return map.size();
    }

    /**
     * Removes all mappings.
     */
    public void clear() {
        map.clear();
        for (Entry<KEY, VALUE> e = head.next; e != head; e = e.next) {
            e.removed = true;
        }
        head.next = head;
        head.prev = head;
    }

    /**
     * Deletes the mapping for <var>key</var>.
     * 
     * @param key
     *            the key
     * @return {@code true} if <var>key</var> was mapped to a value
     */
    public boolean delete(KEY key) {
        KEY hashKey = hashKey(key);
        Entry<KEY, VALUE> entry = map.remove(hashKey);
        if (entry != null) {
            entry.removed = true;
            entry.prev.next = entry.next;
            entry.next.prev = entry.prev;
            return true;
        }
        return false;
    }

    /**
     * Returns the mapped value for <var>key</var> or {@code null} if no mapping was found.
     * 
     * @param key
     *            the key
     * @return the mapped value or {@code null}
     */
    public VALUE get(KEY key) {
        KEY hashKey = hashKey(key);
        Entry<KEY, VALUE> entry = map.get(hashKey);
        if (entry != null) {
            return entry.getValue();
        }
        return null;
    }

    /**
     * Returns {@code true} if a mapping for <var>key</var> is present.
     * 
     * @param key
     *            the key
     * @return {@code true} if <var>key</var> is mapped to a value
     */
    public boolean has(KEY key) {
        KEY hashKey = hashKey(key);
        return map.containsKey(hashKey);
    }

    /**
     * Inserts or updates the mapping <var>key</var> &rarr; <var>value</var>.
     * 
     * @param key
     *            the key
     * @param value
     *            the mapped value
     */
    public void set(KEY key, VALUE value) {
        KEY hashKey = hashKey(key);
        Entry<KEY, VALUE> entry = map.get(hashKey);
        if (entry != null) {
            entry.setValue(value);
        } else {
            insert(hashKey, value);
        }
    }

    /**
     * Inserts or updates the mappings from <var>map</var>. (Bulk operation)
     * 
     * @param map
     *            the source map
     * @see #set(Object, Object)
     */
    public void setAll(LinkedMap<KEY, VALUE> map) {
        if (head == head.next) {
            // empty map
            for (Entry<KEY, VALUE> e = map.head; e.next != map.head; e = e.next) {
                KEY hashKey = hashKey(e.getKey());
                insert(hashKey, e.getValue());
            }
        } else {
            for (Entry<KEY, VALUE> e = map.head; e.next != map.head; e = e.next) {
                set(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * Returns a new {@link Iterator} over this map.
     * 
     * @return an iterator over this map
     */
    @Override
    public Iterator<Map.Entry<KEY, VALUE>> iterator() {
        return new Iterator<Map.Entry<KEY, VALUE>>() {
            private Entry<KEY, VALUE> cursor = head;

            private Entry<KEY, VALUE> find() {
                Entry<KEY, VALUE> entry = cursor;
                while (entry.removed) {
                    entry = entry.prev;
                }
                return entry.next;
            }

            @Override
            public boolean hasNext() {
                return find() != head;
            }

            @Override
            public Entry<KEY, VALUE> next() {
                Entry<KEY, VALUE> next = find();
                if (next == head) {
                    throw new NoSuchElementException();
                }
                cursor = next;
                return next;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
