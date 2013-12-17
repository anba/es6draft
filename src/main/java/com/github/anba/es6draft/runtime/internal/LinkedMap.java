/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple double-linked map implementation without fail-fast iterator
 */
public class LinkedMap<KEY, VALUE> {
    @SuppressWarnings("serial")
    private static final class Entry<KEY, VALUE> extends AbstractMap.SimpleEntry<KEY, VALUE> {
        private Entry<KEY, VALUE> prev, next;
        private boolean removed = false;

        Entry(KEY key, VALUE value) {
            super(key, value);
        }
    }

    public interface MapBuilder {
        <K, V> Map<K, V> create();
    }

    public static final MapBuilder HashMapBuilder = new MapBuilder() {
        @Override
        public <K, V> Map<K, V> create() {
            return new HashMap<K, V>();
        }
    };

    private final Map<KEY, Entry<KEY, VALUE>> map;
    private final Entry<KEY, VALUE> head;

    public LinkedMap() {
        this(HashMapBuilder);
    }

    public LinkedMap(MapBuilder builder) {
        map = builder.create();
        head = new Entry<KEY, VALUE>(null, null);
        head.prev = head;
        head.next = head;
    }

    private void insert(KEY hashKey, KEY key, VALUE value) {
        Entry<KEY, VALUE> entry = new Entry<>(key, value);
        map.put(hashKey, entry);
        entry.prev = head.prev;
        entry.next = head;
        head.prev.next = entry;
        head.prev = entry;
    }

    private void remove(Entry<KEY, VALUE> entry) {
        entry.removed = true;
        entry.prev.next = entry.next;
        entry.next.prev = entry.prev;
    }

    protected KEY hashKey(KEY key) {
        return key;
    }

    public int size() {
        return map.size();
    }

    public void clear() {
        map.clear();
        for (Entry<KEY, VALUE> e = head.next; e != head; e = e.next) {
            e.removed = true;
        }
        head.next = head;
        head.prev = head;
    }

    // java.util.Map compatibility
    public boolean remove(KEY key) {
        return delete(key);
    }

    // java.util.Map compatibility
    public boolean containsKey(KEY key) {
        return has(key);
    }

    // java.util.Map compatibility
    public void put(KEY key, VALUE value) {
        set(key, value);
    }

    public boolean delete(KEY key) {
        KEY hashKey = hashKey(key);
        Entry<KEY, VALUE> entry = map.remove(hashKey);
        if (entry != null) {
            remove(entry);
            return true;
        }
        return false;
    }

    public VALUE get(KEY key) {
        KEY hashKey = hashKey(key);
        Entry<KEY, VALUE> entry = map.get(hashKey);
        if (entry != null) {
            return entry.getValue();
        } else {
            return null;
        }
    }

    public boolean has(KEY key) {
        KEY hashKey = hashKey(key);
        return map.containsKey(hashKey);
    }

    public void set(KEY key, VALUE value) {
        KEY hashKey = hashKey(key);
        Entry<KEY, VALUE> entry = map.get(hashKey);
        if (entry != null) {
            entry.setValue(value);
        } else {
            insert(hashKey, key, value);
        }
    }

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
                return (find() != head);
            }

            @Override
            public Entry<KEY, VALUE> next() {
                Entry<KEY, VALUE> next = find();
                if (next == head) {
                    return null;
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
