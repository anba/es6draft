/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 22.1.5.3 Properties of Array Iterator Instances
 */
public final class ArrayIteratorObject extends OrdinaryObject {
    /** [[IteratedObject]] */
    private ScriptObject iteratedObject;

    /** [[ArrayIteratorNextIndex]] */
    private long nextIndex;

    /** [[ArrayIterationKind]] */
    private final ArrayIterationKind iterationKind;

    ArrayIteratorObject(Realm realm, ScriptObject array, ArrayIterationKind iterationKind, ScriptObject prototype) {
        this(realm, array, 0, iterationKind, prototype);
    }

    ArrayIteratorObject(Realm realm, ScriptObject array, long index, ArrayIterationKind iterationKind,
            ScriptObject prototype) {
        super(realm, prototype);
        this.iteratedObject = array;
        this.nextIndex = index;
        this.iterationKind = iterationKind;
    }

    /**
     * 22.1.5.3 Properties of Array Iterator Instances
     */
    public enum ArrayIterationKind {
        Key, Value, KeyValue
    }

    /**
     * [[IteratedObject]]
     * 
     * @return the iterated object
     */
    public ScriptObject getIteratedObject() {
        return iteratedObject;
    }

    /**
     * [[IteratedObject]]
     * 
     * @param iteratedObject
     *            the iterated object
     */
    public void setIteratedObject(ScriptObject iteratedObject) {
        this.iteratedObject = iteratedObject;
    }

    /**
     * [[ArrayIteratorNextIndex]]
     * 
     * @return the next array index
     */
    public long getNextIndex() {
        return nextIndex;
    }

    /**
     * [[ArrayIteratorNextIndex]]
     * 
     * @param nextIndex
     *            the next array index
     */
    public void setNextIndex(long nextIndex) {
        this.nextIndex = nextIndex;
    }

    /**
     * [[ArrayIterationKind]]
     * 
     * @return the array iteration kind
     */
    public ArrayIterationKind getIterationKind() {
        return iterationKind;
    }

    @Override
    public String toString() {
        return String.format("%s, nextIndex=%d, iterationKind=%s", super.toString(), nextIndex, iterationKind);
    }
}
