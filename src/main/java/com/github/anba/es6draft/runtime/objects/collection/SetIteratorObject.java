/**
 * Copyright (c) 2012-2016 André Bargull
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
 * 23.2.5.3 Properties of Set Iterator Instances
 */
public final class SetIteratorObject extends OrdinaryObject {
    /** [[IteratedSet]] / [[SetNextIndex]] */
    Iterator<Entry<Object, Void>> iterator;

    /** [[SetIterationKind]] */
    final SetIterationKind iterationKind;

    SetIteratorObject(Realm realm, SetObject set, SetIterationKind kind, ScriptObject prototype) {
        this(realm, set.getSetData().iterator(), kind, prototype);
    }

    SetIteratorObject(Realm realm, Iterator<Entry<Object, Void>> iterator, SetIterationKind kind,
            ScriptObject prototype) {
        super(realm);
        this.iterator = iterator;
        this.iterationKind = kind;
        setPrototype(prototype);
    }

    /**
     * 23.2.5.3 Properties of Set Iterator Instances
     */
    public enum SetIterationKind {
        Key, Value, KeyValue
    }

    /**
     * [[IteratedSet]] / [[SetNextIndex]]
     * 
     * @return the internal iterator
     */
    public Iterator<Entry<Object, Void>> getIterator() {
        return iterator;
    }

    /**
     * [[IteratedSet]] / [[SetNextIndex]]
     * 
     * @param iterator
     *            the internal iterator
     */
    public void setIterator(Iterator<Entry<Object, Void>> iterator) {
        this.iterator = iterator;
    }

    /**
     * [[SetIterationKind]]
     * 
     * @return the iteration kind
     */
    public SetIterationKind getIterationKind() {
        return iterationKind;
    }
}
