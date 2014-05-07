/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;

import java.util.Iterator;
import java.util.Map;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.2 Array Exotic Objects
 * </ul>
 */
public final class ExoticArray extends OrdinaryObject {
    /** [[ArrayInitializationState]] */
    private boolean initialized = false;

    public ExoticArray(Realm realm) {
        super(realm);
    }

    /**
     * [[ArrayInitializationState]]
     *
     * @return {@code true} if the array was successfully initialized
     */
    public boolean initialize() {
        if (initialized) {
            return false;
        }
        initialized = true;
        return true;
    }

    /**
     * 9.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     * 
     * @param p
     *            the property key
     * @return {@code true} if the property key is a valid array index
     */
    public static boolean isArrayIndex(int p) {
        return p >= 0;
    }

    /**
     * 9.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     * 
     * @param p
     *            the property key
     * @return {@code true} if the property key is a valid array index
     */
    public static boolean isArrayIndex(long p) {
        return p >= 0 && p < 0xFFFF_FFFFL;
    }

    /**
     * 9.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     * 
     * @param p
     *            the property key
     * @return {@code true} if the property key is a valid array index
     */
    public static boolean isArrayIndex(String p) {
        return toArrayIndex(p) >= 0;
    }

    /**
     * 9.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     * 
     * @param p
     *            the property key
     * @return the array index or {@code -1}
     */
    public static long toArrayIndex(String p) {
        return Strings.toArrayIndex(p);
    }

    /**
     * 9.4.2.1 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    protected boolean defineProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (isArrayIndex(propertyKey)) {
            /* steps 3.a-3.d */
            Property oldLenDesc = getOwnProperty(cx, "length");
            assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
            long oldLen = ToUint32(cx, oldLenDesc.getValue());
            long index = propertyKey;
            /* steps 3.e */
            if (index >= oldLen && !oldLenDesc.isWritable()) {
                return false;
            }
            /* steps 3.f-3.h */
            boolean succeeded = ordinaryDefineOwnProperty(cx, propertyKey, desc);
            if (!succeeded) {
                return false;
            }
            /* step 3.i */
            if (index >= oldLen) {
                PropertyDescriptor lenDesc = oldLenDesc.toPropertyDescriptor();
                lenDesc.setValue(index + 1);
                ordinaryDefineOwnProperty(cx, "length", lenDesc);
            }
            /* step 3.j */
            return true;
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.4.2.1 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    protected boolean defineProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* steps 1, 3 (not applicable) */
        /* step 2 */
        if ("length".equals(propertyKey)) {
            return ArraySetLength(cx, this, desc);
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.4.2.2 ArrayCreate(length) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param length
     *            the array length
     * @return the new array object
     */
    public static ExoticArray ArrayCreate(ExecutionContext cx, long length) {
        assert length >= 0;
        return ArrayCreate(cx, length, cx.getIntrinsic(Intrinsics.ArrayPrototype));
    }

    /**
     * 9.4.2.2 ArrayCreate(length) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @return the new array object
     */
    public static ExoticArray ArrayCreate(ExecutionContext cx, ScriptObject proto) {
        assert proto != null;
        /* step 1 (not applicable) */
        /* steps 2-4, 6 (implicit) */
        ExoticArray array = new ExoticArray(cx.getRealm());
        /* step 5 */
        array.setPrototype(proto);
        /* step 7 (not applicable) */
        /* step 8 */
        long length = 0;
        /* step 9 (not applicable) */
        /* step 10 */
        array.ordinaryDefineOwnProperty(cx, "length", new PropertyDescriptor(length, true, false,
                false));
        /* step 11 */
        return array;
    }

    /**
     * 9.4.2.2 ArrayCreate(length) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param length
     *            the array length
     * @param proto
     *            the prototype object
     * @return the new array object
     */
    public static ExoticArray ArrayCreate(ExecutionContext cx, long length, ScriptObject proto) {
        assert proto != null && length >= 0;
        /* step 1 (not applicable) */
        /* step 9 (moved) */
        if (length > 0xFFFF_FFFFL) {
            // enfore array index invariant
            throw newRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* steps 2-4, 6 (implicit) */
        ExoticArray array = new ExoticArray(cx.getRealm());
        /* step 5 */
        array.setPrototype(proto);
        /* step 7 */
        array.initialized = true;
        /* step 8 (not applicable) */
        /* step 9 (see above) */
        /* step 10 */
        array.ordinaryDefineOwnProperty(cx, "length", new PropertyDescriptor(length, true, false,
                false));
        /* step 11 */
        return array;
    }

    /**
     * Helper method to create dense arrays
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param values
     *            the element values
     * @return the new array object
     */
    public static ExoticArray DenseArrayCreate(ExecutionContext cx, Object[] values) {
        ExoticArray array = ArrayCreate(cx, values.length);
        IndexedMap<Property> indexed = array.indexedProperties();
        for (int i = 0, len = values.length; i < len; ++i) {
            indexed.put(i, new Property(values[i], true, true, true));
        }
        return array;
    }

    /**
     * Helper method to create sparse arrays
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param values
     *            the element values
     * @return the new array object
     */
    public static ExoticArray SparseArrayCreate(ExecutionContext cx, Object[] values) {
        ExoticArray array = ArrayCreate(cx, values.length);
        IndexedMap<Property> indexed = array.indexedProperties();
        for (int i = 0, len = values.length; i < len; ++i) {
            if (values[i] != null) {
                indexed.put(i, new Property(values[i], true, true, true));
            }
        }
        return array;
    }

    /**
     * 9.4.2.3 ArraySetLength(A, Desc) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param array
     *            the array object
     * @param desc
     *            the property descriptor
     * @return {@code true} on success
     */
    public static boolean ArraySetLength(ExecutionContext cx, ExoticArray array,
            PropertyDescriptor desc) {
        /* step 1 */
        if (!desc.hasValue()) {
            return array.ordinaryDefineOwnProperty(cx, "length", desc);
        }
        /* step 2 */
        PropertyDescriptor newLenDesc = desc.clone();
        /* step 3 */
        long newLen = ToUint32(cx, desc.getValue());
        /* step 4 */
        if (newLen != ToNumber(cx, desc.getValue())) {
            throw newRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* step 5 */
        newLenDesc.setValue(newLen);
        /* step 6 */
        Property oldLenDesc = array.getProperty(cx, "length");
        assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
        /* step 7 */
        long oldLen = ToUint32(cx, oldLenDesc.getValue());
        /* step 8 */
        if (newLen >= oldLen) {
            return array.ordinaryDefineOwnProperty(cx, "length", newLenDesc);
        }
        /* step 9 */
        if (!oldLenDesc.isWritable()) {
            return false;
        }
        /* steps 10-11 */
        boolean newWritable;
        if (!newLenDesc.hasWritable() || newLenDesc.isWritable()) {
            newWritable = true;
        } else {
            newWritable = false;
            newLenDesc.setWritable(true);
        }
        /* steps 12-13 */
        boolean succeeded = array.ordinaryDefineOwnProperty(cx, "length", newLenDesc);
        /* step 14 */
        if (!succeeded) {
            return false;
        }
        /* step 15 */
        IndexedMap<Property> indexed = array.indexedProperties();
        if (indexed.isSparse()) {
            oldLen = SparseArraySetLength(indexed, newLen, oldLen);
        } else {
            oldLen = DenseArraySetLength(indexed, newLen, oldLen);
        }
        // Need to call updateLength() manually.
        indexed.updateLength();
        /* step 15.d */
        if (oldLen >= 0) {
            newLenDesc.setValue(oldLen + 1);
            if (!newWritable) {
                newLenDesc.setWritable(false);
            }
            array.ordinaryDefineOwnProperty(cx, "length", newLenDesc);
            return false;
        }
        /* step 16 */
        if (!newWritable) {
            PropertyDescriptor nonWritable = new PropertyDescriptor();
            nonWritable.setWritable(false);
            array.ordinaryDefineOwnProperty(cx, "length", nonWritable);
        }
        /* step 17 */
        return true;
    }

    private static long DenseArraySetLength(IndexedMap<Property> indexed, long newLen, long oldLen) {
        assert newLen < oldLen;
        for (long index = oldLen; --index >= newLen;) {
            Property prop = indexed.get(index);
            if (prop != null && !prop.isConfigurable()) {
                return index;
            }
            indexed.removeUnchecked(index);
        }
        return -1;
    }

    private static long SparseArraySetLength(IndexedMap<Property> indexed, long newLen, long oldLen) {
        assert newLen < oldLen;
        Iterator<Map.Entry<Long, Property>> iter = indexed.descendingIterator(newLen, oldLen);
        while (iter.hasNext()) {
            Map.Entry<Long, Property> entry = iter.next();
            long index = entry.getKey();
            Property prop = entry.getValue();
            if (!prop.isConfigurable()) {
                return index;
            }
            // Cannot call remove[Unchecked]() directly b/c of ConcurrentModificationException.
            // indexed.remove(index);
            iter.remove();
        }
        return -1;
    }
}
