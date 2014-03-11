/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.SameValue;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToInteger;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;

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

    private static double toIntegerIndex(String propertyKey) {
        double intIndex = ToInteger(ToNumber(propertyKey));
        if (ToString(intIndex).equals(propertyKey)) {
            return intIndex;
        }
        return Double.NaN;
    }

    /** [[HasOwnProperty]] (P) */
    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        double intIndex = toIntegerIndex(propertyKey);
        if (!Double.isNaN(intIndex)) {
            if (intIndex < 0) {
                return false;
            }
            long length = getLength();
            if (intIndex >= length) {
                return false;
            }
            return true;
        }
        /* step 4 */
        return super.hasOwnProperty(cx, propertyKey);
    }

    /** 9.4.5.1 [[GetOwnProperty]] (P) */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        double intIndex = toIntegerIndex(propertyKey);
        if (!Double.isNaN(intIndex)) {
            Object value = elementGet(cx, intIndex);
            if (Type.isUndefined(value)) {
                return null;
            }
            boolean writable = getWritable();
            return new Property(value, writable, true, false);
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
        double intIndex = toIntegerIndex(propertyKey);
        if (!Double.isNaN(intIndex)) {
            if (intIndex < 0) {
                return false;
            }
            long length = getLength();
            if (intIndex >= length) {
                return false;
            }
            if (desc.isAccessorDescriptor()) {
                return false;
            }
            if (desc.hasConfigurable() && desc.isConfigurable()) {
                return false;
            }
            if (desc.hasEnumerable() && !desc.isEnumerable()) {
                return false;
            }
            boolean writable = getWritable();
            boolean makeReadOnly = false;
            if (desc.hasWritable()) {
                if (desc.isWritable() && !writable) {
                    return false;
                }
                if (!desc.isWritable() && writable) {
                    makeReadOnly = true;
                }
            }
            if (desc.hasValue()) {
                Object value = desc.getValue();
                if (!writable) {
                    Object oldValue = elementGet(cx, intIndex);
                    if (Type.isUndefined(value)) {
                        return false;
                    }
                    if (!SameValue(value, oldValue)) {
                        return false;
                    }
                } else {
                    elementSet(cx, intIndex, value);
                }
            }
            if (makeReadOnly) {
                setNonWritable();
            }
            return true;
        }
        /* step 4 */
        return super.defineOwnProperty(cx, propertyKey, desc);
    }

    /** 9.4.5.3 [[Get]] (P, Receiver) */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* step 1 (not applicable) */
        /* step 2 */
        if (this == receiver) { // SameValue(this, receiver)
            double intIndex = toIntegerIndex(propertyKey);
            if (!Double.isNaN(intIndex)) {
                return elementGet(cx, intIndex);
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
            double intIndex = toIntegerIndex(propertyKey);
            if (!Double.isNaN(intIndex)) {
                return elementSet(cx, intIndex, value);
            }
        }
        /* step 3 */
        return super.set(cx, propertyKey, value, receiver);
    }

    /** 9.4.5.5 [[Enumerate]] () */
    @Override
    protected List<String> enumerateKeys(ExecutionContext cx) {
        // FIXME: spec incomplete
        List<String> keys = super.enumerateKeys(cx);
        addIntegerIndices(keys);
        return keys;
    }

    /** 9.4.5.6 [[OwnPropertyKeys]] () */
    @Override
    protected List<Object> enumerateOwnKeys(ExecutionContext cx) {
        // FIXME: spec incomplete
        List<Object> keys = super.enumerateOwnKeys(cx);
        addIntegerIndices(keys);
        return keys;
    }

    @Override
    protected boolean isEnumerableOwnProperty(ExecutionContext cx, String key) {
        double intIndex = toIntegerIndex(key);
        if (!Double.isNaN(intIndex)) {
            long length = getLength();
            return 0 <= intIndex && intIndex < length;
        }
        return super.isEnumerableOwnProperty(cx, key);
    }

    /**
     * Append integer indices to {@code keys} collection
     */
    private void addIntegerIndices(List<? super String> keys) {
        for (long i = 0, length = getLength(); i < length; ++i) {
            keys.add(Long.toString(i));
        }
    }

    /**
     * 9.4.5.7 IntegerIndexedObjectCreate Abstract Operation
     */
    public static ScriptObject IntegerIndexedObjectCreate(ExecutionContext cx,
            ScriptObject prototype) {
        // the operation is not supported in this implementation
        throw new UnsupportedOperationException();
    }

    /** Not in spec */
    protected abstract boolean getWritable();

    /** Not in spec */
    protected abstract void setNonWritable();

    /** Not in spec */
    protected abstract long getLength();

    /**
     * 9.4.5.8 IntegerIndexedElementGet (O, index) Abstract Operation
     */
    protected abstract Object elementGet(ExecutionContext cx, double index);

    /**
     * 9.4.5.9 IntegerIndexedElementSet (O, index, value) Abstract Operation
     */
    protected abstract boolean elementSet(ExecutionContext cx, double index, Object value);
}
