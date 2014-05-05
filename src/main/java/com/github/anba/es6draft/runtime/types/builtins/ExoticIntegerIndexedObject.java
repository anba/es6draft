/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.CanonicalNumericString;
import static com.github.anba.es6draft.runtime.AbstractOperations.IsInteger;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
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
public abstract class ExoticIntegerIndexedObject extends OrdinaryObject {
    public ExoticIntegerIndexedObject(Realm realm) {
        super(realm);
    }

    private static boolean isCanonicalNumeric(double numericIndex) {
        // FIXME: spec issue https://bugs.ecmascript.org/show_bug.cgi?id=2049
        return numericIndex != 0 || Double.compare(numericIndex, -0d) != 0;
    }

    /** [[HasOwnProperty]] (P) */
    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        double numericIndex = CanonicalNumericString(propertyKey);
        if (isCanonicalNumeric(numericIndex)) {
            return elementHas(cx, numericIndex);
        }
        /* step 4 */
        return super.hasOwnProperty(cx, propertyKey);
    }

    /** 9.4.5.1 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        double numericIndex = CanonicalNumericString(propertyKey);
        if (isCanonicalNumeric(numericIndex)) {
            Object value = elementGet(cx, numericIndex);
            if (Type.isUndefined(value)) {
                return null;
            }
            return new Property(value, true, true, false);
        }
        /* step 4 */
        return ordinaryGetOwnProperty(propertyKey);
    }

    /** 9.4.5.2 [[DefineOwnProperty]] (P, Desc) */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        double numericIndex = CanonicalNumericString(propertyKey);
        if (isCanonicalNumeric(numericIndex)) {
            /* step 3.c.i */
            if (!IsInteger(numericIndex)) {
                return false;
            }
            /* step 3.c.ii */
            double intIndex = numericIndex;
            /* step 3.c.iii */
            if (intIndex < 0) {
                return false;
            }
            /* step 3.c.iv */
            long length = getLength();
            /* step 3.c.v */
            if (intIndex >= length) {
                return false;
            }
            /* step 3.c.vi */
            if (desc.isAccessorDescriptor()) {
                return false;
            }
            /* step 3.c.vii */
            if (desc.hasConfigurable() && desc.isConfigurable()) {
                return false;
            }
            /* step 3.c.viii */
            if (desc.hasEnumerable() && !desc.isEnumerable()) {
                return false;
            }
            /* step 3.c.ix */
            if (desc.hasWritable() && !desc.isWritable()) {
                return false;
            }
            /* step 3.c.x */
            if (desc.hasValue()) {
                Object value = desc.getValue();
                elementSet(cx, intIndex, value);
            }
            /* step 3.c.xi */
            return true;
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.4.5.3 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (this == receiver) { // SameValue(this, receiver)
            double numericIndex = CanonicalNumericString(propertyKey);
            if (isCanonicalNumeric(numericIndex)) {
                return elementGet(cx, numericIndex);
            }
        }
        /* step 3 */
        return super.get(cx, propertyKey, receiver);
    }

    /** 9.4.5.4 [[Set]] (P, V, Receiver) */
    @Override
    public boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (this == receiver) { // SameValue(this, receiver)
            double numericIndex = CanonicalNumericString(propertyKey);
            if (isCanonicalNumeric(numericIndex)) {
                return elementSet(cx, numericIndex, value);
            }
        }
        /* step 3 */
        return super.set(cx, propertyKey, value, receiver);
    }

    /** 9.4.5.5 [[Enumerate]] () */
    @Override
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        // FIXME: spec incomplete
        List<String> keys = super.getEnumerableKeys(cx);
        addIntegerIndices(cx, keys);
        return keys;
    }

    /** 9.4.5.6 [[OwnPropertyKeys]] () */
    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        // FIXME: spec incomplete
        List<Object> keys = super.getOwnPropertyKeys(cx);
        addIntegerIndices(cx, keys);
        return keys;
    }

    @Override
    protected boolean isEnumerableOwnProperty(String propertyKey) {
        double numericIndex = CanonicalNumericString(propertyKey);
        if (isCanonicalNumeric(numericIndex)) {
            long length = getLength();
            return 0 <= numericIndex && numericIndex < length;
        }
        return super.isEnumerableOwnProperty(propertyKey);
    }

    /**
     * Appends the integer indices to {@code keys} collection.
     * 
     * @param cx
     *            the execution context
     * @param keys
     *            the property keys
     */
    private void addIntegerIndices(ExecutionContext cx, List<? super String> keys) {
        for (long i = 0, length = getLength(cx); i < length; ++i) {
            keys.add(Long.toString(i));
        }
    }

    /**
     * 9.4.5.7 IntegerIndexedObjectCreate Abstract Operation
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
    protected abstract long getLength();

    /**
     * Not in spec
     *
     * @param cx
     *            the execution context
     * @return the length property or a TypeError if not initialized
     */
    protected abstract long getLength(ExecutionContext cx);

    /**
     * Not in spec
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the integer index
     * @return {@code true} if the element is present
     */
    protected abstract boolean elementHas(ExecutionContext cx, double index);

    /**
     * 9.4.5.8 IntegerIndexedElementGet (O, index) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the integer index
     * @return the element value
     */
    protected abstract Object elementGet(ExecutionContext cx, double index);

    /**
     * 9.4.5.9 IntegerIndexedElementSet (O, index, value) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the integer index
     * @param value
     *            the new element value
     * @return {@code true} on success
     */
    protected abstract boolean elementSet(ExecutionContext cx, double index, Object value);
}
