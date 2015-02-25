/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.CanonicalNumericIndexString;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompoundList;
import com.github.anba.es6draft.runtime.internal.IndexedPropertyKeyList;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
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
     */
    public IntegerIndexedObject(Realm realm) {
        super(realm);
    }

    private static boolean isCanonicalNumericIndex(long numericIndex) {
        return numericIndex >= 0;
    }

    @Override
    public boolean hasSpecialIndexedProperties() {
        return true;
    }

    /** [[HasOwnProperty]] (P) */
    @Override
    protected final boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        return elementHas(cx, propertyKey);
    }

    /** [[HasOwnProperty]] (P) */
    @Override
    protected final boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        if (isCanonicalNumericIndex(numericIndex)) {
            return elementHas(cx, numericIndex);
        }
        /* step 4 */
        return super.hasOwnProperty(cx, propertyKey);
    }

    /** 9.4.5.1 [[GetOwnProperty]] (P) */
    @Override
    protected final Property getProperty(ExecutionContext cx, long propertyKey) {
        Object value = elementGet(cx, propertyKey);
        if (Type.isUndefined(value)) {
            return null;
        }
        return new Property(value, true, true, false);
    }

    /** 9.4.5.1 [[GetOwnProperty]] (P) */
    @Override
    protected final Property getProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
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
    protected boolean has(ExecutionContext cx, long propertyKey) {
        return elementHas(cx, propertyKey);
    }

    /** 9.4.5.2 [[HasProperty]](P) */
    @Override
    protected boolean has(ExecutionContext cx, String propertyKey) {
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        if (isCanonicalNumericIndex(numericIndex)) {
            return elementHas(cx, numericIndex);
        }
        return ordinaryHasProperty(cx, propertyKey);
    }

    /** 9.4.5.3 [[DefineOwnProperty]] (P, Desc) */
    @Override
    protected final boolean defineProperty(ExecutionContext cx, long propertyKey,
            PropertyDescriptor desc) {
        /* steps 3.c.i-3.c.vi */
        if (propertyKey >= getLength()) {
            return false;
        }
        /* step 3.c.vii */
        if (desc.isAccessorDescriptor()) {
            return false;
        }
        /* step 3.c.viii */
        if (desc.hasConfigurable() && desc.isConfigurable()) {
            return false;
        }
        /* step 3.c.ix */
        if (desc.hasEnumerable() && !desc.isEnumerable()) {
            return false;
        }
        /* step 3.c.x */
        if (desc.hasWritable() && !desc.isWritable()) {
            return false;
        }
        /* step 3.c.xi */
        if (desc.hasValue()) {
            Object value = desc.getValue();
            elementSet(cx, propertyKey, value);
        }
        /* step 3.c.xii */
        return true;
    }

    /** 9.4.5.3 [[DefineOwnProperty]] (P, Desc) */
    @Override
    protected final boolean defineProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        if (isCanonicalNumericIndex(numericIndex)) {
            return defineProperty(cx, numericIndex, desc);
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.4.5.4 [[Get]] (P, Receiver) */
    @Override
    protected final Object getValue(ExecutionContext cx, long propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (this == receiver) { // SameValue(this, receiver)
            return elementGet(cx, propertyKey);
        }
        /* step 3 */
        return super.getValue(cx, propertyKey, receiver);
    }

    /** 9.4.5.4 [[Get]] (P, Receiver) */
    @Override
    protected final Object getValue(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (this == receiver) { // SameValue(this, receiver)
            long numericIndex = CanonicalNumericIndexString(propertyKey);
            if (isCanonicalNumericIndex(numericIndex)) {
                return elementGet(cx, numericIndex);
            }
        }
        /* step 3 */
        return super.getValue(cx, propertyKey, receiver);
    }

    /** 9.4.5.5 [[Set]] (P, V, Receiver) */
    @Override
    protected final boolean setValue(ExecutionContext cx, long propertyKey, Object value,
            Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (this == receiver) { // SameValue(this, receiver)
            return elementSet(cx, propertyKey, value);
        }
        /* step 3 */
        return super.setValue(cx, propertyKey, value, receiver);
    }

    /** 9.4.5.5 [[Set]] (P, V, Receiver) */
    @Override
    protected final boolean setValue(ExecutionContext cx, String propertyKey, Object value,
            Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (this == receiver) { // SameValue(this, receiver)
            long numericIndex = CanonicalNumericIndexString(propertyKey);
            if (isCanonicalNumericIndex(numericIndex)) {
                return elementSet(cx, numericIndex, value);
            }
        }
        /* step 3 */
        return super.setValue(cx, propertyKey, value, receiver);
    }

    /** 9.4.5.6 [[Enumerate]] () */
    @Override
    protected final List<String> getEnumerableKeys(ExecutionContext cx) {
        /* steps 1-7 */
        return new CompoundList<>(new IndexedPropertyKeyList(getLength()),
                super.getEnumerableKeys(cx));
    }

    /** 9.4.5.7 [[OwnPropertyKeys]] () */
    @Override
    protected final List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        /* steps 1-7 */
        return new CompoundList<>(new IndexedPropertyKeyList(getLength()),
                super.getOwnPropertyKeys(cx));
    }

    @Override
    protected final Enumerability isEnumerableOwnProperty(String propertyKey) {
        long numericIndex = CanonicalNumericIndexString(propertyKey);
        if (isCanonicalNumericIndex(numericIndex)) {
            return Enumerability.isEnumerable(numericIndex < getLength());
        }
        return super.isEnumerableOwnProperty(propertyKey);
    }

    /**
     * 9.4.5.8 IntegerIndexedObjectCreate (prototype, internalSlotsList)
     * 
     * @param cx
     *            the execution context
     * @param prototype
     *            the prototype object
     * @return the new integer indexed object
     */
    public static ScriptObject IntegerIndexedObjectCreate(ExecutionContext cx,
            ScriptObject prototype) {
        // the operation is not supported in this implementation
        throw new UnsupportedOperationException();
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
     * 9.4.5.9 IntegerIndexedElementGet (O, index)
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the integer index
     * @return the element value
     */
    protected abstract Object elementGet(ExecutionContext cx, long index);

    /**
     * 9.4.5.10 IntegerIndexedElementSet (O, index, value)
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
