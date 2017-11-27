/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.CanonicalNumericIndexString;

import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompoundIterator;
import com.github.anba.es6draft.runtime.internal.CompoundList;
import com.github.anba.es6draft.runtime.internal.IndexedPropertyKeyList;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Slots</h2>
 * <ul>
 * <li>9.4.5 Integer Indexed Exotic Objects
 * </ul>
 */
public abstract class IntegerIndexedObject extends OrdinaryObject {
    /**
     * Constructs a new Integer Indexed object.
     * 
     * @param realm
     *            the realm object
     * @param prototype
     *            the prototype object
     */
    protected IntegerIndexedObject(Realm realm, ScriptObject prototype) {
        super(realm, prototype);
    }

    private static boolean isCanonicalNumericIndex(long numericIndex) {
        return numericIndex >= 0;
    }

    @Override
    public final boolean hasSpecialIndexedProperties() {
        return true;
    }

    /** [[HasOwnProperty]] (P) */
    @Override
    public final boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        return elementHas(cx, propertyKey);
    }

    /** [[HasOwnProperty]] (P) */
    @Override
    public final boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        if (isCanonicalNumericIndex(numericIndex)) {
            return elementHas(cx, numericIndex);
        }
        return ordinaryHasOwnProperty(propertyKey);
    }

    /** 9.4.5.1 [[GetOwnProperty]] (P) */
    @Override
    public final Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        /* step 3.b */
        Object value = elementGet(cx, propertyKey);
        if (Type.isUndefined(value)) {
            return null;
        }
        return new Property(value, true, true, false);
    }

    /** 9.4.5.1 [[GetOwnProperty]] (P) */
    @Override
    public final Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        /* step 3.a */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        /* step 3.b */
        if (isCanonicalNumericIndex(numericIndex)) {
            Object value = elementGet(cx, numericIndex);
            if (Type.isUndefined(value)) {
                return null;
            }
            return new Property(value, true, true, false);
        }
        /* step 4 */
        return ordinaryGetOwnProperty(propertyKey);
    }

    /** 9.4.5.2 [[HasProperty]](P) */
    @Override
    public final boolean hasProperty(ExecutionContext cx, long propertyKey) {
        /* step 3.b */
        return elementHas(cx, propertyKey);
    }

    /** 9.4.5.2 [[HasProperty]](P) */
    @Override
    public final boolean hasProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        /* step 3.a */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        /* step 3.b */
        if (isCanonicalNumericIndex(numericIndex)) {
            return elementHas(cx, numericIndex);
        }
        /* step 4 */
        return ordinaryHasProperty(cx, propertyKey);
    }

    /** 9.4.5.3 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* steps 3.b.i-vi */
        if (propertyKey >= getLength()) {
            return false;
        }
        /* step 3.b.vii */
        if (desc.isAccessorDescriptor()) {
            return false;
        }
        /* step 3.b.viii */
        if (desc.hasConfigurable() && desc.isConfigurable()) {
            return false;
        }
        /* step 3.b.ix */
        if (desc.hasEnumerable() && !desc.isEnumerable()) {
            return false;
        }
        /* step 3.b.x */
        if (desc.hasWritable() && !desc.isWritable()) {
            return false;
        }
        /* step 3.b.xi */
        if (desc.hasValue()) {
            Object value = desc.getValue();
            return elementSet(cx, propertyKey, value);
        }
        /* step 3.b.xii */
        return true;
    }

    /** 9.4.5.3 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        /* step 3.a */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        /* step 3.b */
        if (isCanonicalNumericIndex(numericIndex)) {
            return defineOwnProperty(cx, numericIndex, desc);
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.4.5.4 [[Get]] (P, Receiver) */
    @Override
    public final Object get(ExecutionContext cx, long propertyKey, Object receiver) {
        /* step 2 */
        return elementGet(cx, propertyKey);
    }

    /** 9.4.5.4 [[Get]] (P, Receiver) */
    @Override
    public final Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        /* step 2.a */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        /* step 2.b */
        if (isCanonicalNumericIndex(numericIndex)) {
            return elementGet(cx, numericIndex);
        }
        /* step 3 */
        return ordinaryGet(cx, propertyKey, receiver);
    }

    /** 9.4.5.5 [[Set]] (P, V, Receiver) */
    @Override
    public final boolean set(ExecutionContext cx, long propertyKey, Object value, Object receiver) {
        /* step 2 */
        return elementSet(cx, propertyKey, value);
    }

    /** 9.4.5.5 [[Set]] (P, V, Receiver) */
    @Override
    public final boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        /* step 2.a */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        /* step 2.b */
        if (isCanonicalNumericIndex(numericIndex)) {
            return elementSet(cx, numericIndex, value);
        }
        /* step 3 */
        return ordinarySet(cx, propertyKey, value, receiver);
    }

    /** 9.4.5.6 [[OwnPropertyKeys]] () */
    @Override
    public final List<Object> ownPropertyKeys(ExecutionContext cx) {
        /* steps 1-7 */
        IndexedPropertyKeyList indexedProperties = new IndexedPropertyKeyList(getLength());
        return new CompoundList<>(indexedProperties, super.ownPropertyKeys(cx));
    }

    @Override
    public final List<String> ownPropertyNames(ExecutionContext cx) {
        /* steps 1-7 */
        IndexedPropertyKeyList indexedProperties = new IndexedPropertyKeyList(getLength());
        return new CompoundList<>(indexedProperties, super.ownPropertyNames(cx));
    }

    /** 9.4.5.6 [[OwnPropertyKeys]] () */
    @Override
    public final Iterator<String> ownEnumerablePropertyKeys(ExecutionContext cx) {
        IndexedPropertyKeyList indexedProperties = new IndexedPropertyKeyList(getLength());
        return new CompoundIterator<>(indexedProperties.iterator(), super.ownEnumerablePropertyKeys(cx));
    }

    @Override
    public final Enumerability isEnumerableOwnProperty(ExecutionContext cx, String propertyKey) {
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        if (isCanonicalNumericIndex(numericIndex)) {
            return Enumerability.isEnumerable(elementHas(cx, numericIndex));
        }
        return super.isEnumerableOwnProperty(cx, propertyKey);
    }

    /**
     * Not in spec
     *
     * @return the length property
     */
    @Override
    public abstract long getLength();

    /**
     * Not in spec
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the integer index
     * @return {@code true} if the element is present
     */
    protected abstract boolean elementHas(ExecutionContext cx, long index);

    /**
     * 9.4.5.8 IntegerIndexedElementGet (O, index)
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the integer index
     * @return the element value
     */
    protected abstract Object elementGet(ExecutionContext cx, long index);

    /**
     * 9.4.5.9 IntegerIndexedElementSet (O, index, value)
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the integer index
     * @param value
     *            the new element value
     * @return {@code true} on success
     */
    protected abstract boolean elementSet(ExecutionContext cx, long index, Object value);
}
