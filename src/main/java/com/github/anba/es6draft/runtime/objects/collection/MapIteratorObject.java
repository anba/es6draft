/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import java.util.Iterator;
import java.util.Map.Entry;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 23.1.5.3 Properties of Map Iterator Instances
 */
public final class MapIteratorObject extends OrdinaryObject {
    /** [[Map]] / [[MapNextIndex]] */
    private Iterator<Entry<Object, Object>> iterator;

    /** [[MapIterationKind]] */
    private final MapIterationKind iterationKind;

    MapIteratorObject(Realm realm, MapObject map, MapIterationKind kind, ScriptObject prototype) {
        this(realm, map.getMapData().iterator(), kind, prototype);
    }

    MapIteratorObject(Realm realm, Iterator<Entry<Object, Object>> iterator, MapIterationKind kind,
            ScriptObject prototype) {
        super(realm);
        this.iterator = iterator;
        this.iterationKind = kind;
        setPrototype(prototype);
    }

    /**
     * 23.1.5.3 Properties of Map Iterator Instances
     */
    public enum MapIterationKind {
        Key, Value, KeyValue
    }

    /**
     * [[Map]] / [[MapNextIndex]]
     * 
     * @return the internal iterator
     */
    public Iterator<Entry<Object, Object>> getIterator() {
        return iterator;
    }

    /**
     * [[Map]] / [[MapNextIndex]]
     * 
     * @param iterator
     *            the internal iterator
     */
    public void setIterator(Iterator<Entry<Object, Object>> iterator) {
        this.iterator = iterator;
    }

    /**
     * [[MapIterationKind]]
     * 
     * @return the iteration kind
     */
    public MapIterationKind getIterationKind() {
        return iterationKind;
    }
}
