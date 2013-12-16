/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToNumber;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToString;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToUint32;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;

import java.util.Arrays;
import java.util.Collection;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
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
public class ExoticArray extends OrdinaryObject {
    /** [[ArrayInitialisationState]] */
    private boolean initialisationState = false;

    public ExoticArray(Realm realm) {
        super(realm);
    }

    /** [[ArrayInitialisationState]] */
    public boolean getInitialisationState() {
        return initialisationState;
    }

    /** [[ArrayInitialisationState]] */
    public void setInitialisationState(boolean initialisationState) {
        assert initialisationState : "cannot de-initialise an array";
        assert !this.initialisationState : "array already initialised";
        this.initialisationState = initialisationState;
    }

    /**
     * 9.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     */
    public static boolean isArrayIndex(String p) {
        return toArrayIndex(p) >= 0;
    }

    /**
     * 9.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     */
    public static long toArrayIndex(String p) {
        return Strings.toArrayIndex(p);
    }

    /**
     * 9.4.2.1 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        /* step 1 (not applicable) */
        /* steps 2-3 */
        if ("length".equals(propertyKey)) {
            /* step 2 */
            return ArraySetLength(cx, this, desc);
        } else if (isArrayIndex(propertyKey)) {
            /* step 3 */
            /* steps 3.a-3.d */
            Property oldLenDesc = getOwnProperty(cx, "length");
            assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
            long oldLen = ToUint32(cx, oldLenDesc.getValue());
            long index = ToUint32(cx, propertyKey);
            /* steps 3.e */
            if (index >= oldLen && !oldLenDesc.isWritable()) {
                return false;
            }
            /* steps 3.f-3.h */
            boolean succeeded = ordinaryDefineOwnProperty(propertyKey, desc);
            if (!succeeded) {
                return false;
            }
            /* step 3.i */
            if (index >= oldLen) {
                PropertyDescriptor lenDesc = oldLenDesc.toPropertyDescriptor();
                lenDesc.setValue(index + 1);
                ordinaryDefineOwnProperty("length", lenDesc);
            }
            /* step 3.j */
            return true;
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(propertyKey, desc);
    }

    /**
     * 9.4.2.2 ArrayCreate(length) Abstract Operation
     */
    public static ExoticArray ArrayCreate(ExecutionContext cx, long length) {
        assert length >= 0;
        return ArrayCreate(cx, length, cx.getIntrinsic(Intrinsics.ArrayPrototype));
    }

    /**
     * 9.4.2.2 ArrayCreate(length) Abstract Operation
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
        array.initialisationState = false;
        long length = 0;
        /* step 9 (not applicable) */
        /* step 10 */
        array.ordinaryDefineOwnProperty("length",
                new PropertyDescriptor(length, true, false, false));
        /* step 11 */
        return array;
    }

    /**
     * 9.4.2.2 ArrayCreate(length) Abstract Operation
     */
    public static ExoticArray ArrayCreate(ExecutionContext cx, long length, ScriptObject proto) {
        assert proto != null && length >= 0;
        /* step 1 (not applicable) */
        /* step 9 (moved) */
        if (length > 0xFFFF_FFFFL) {
            // enfore array index invariant
            throw throwRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* steps 2-4, 6 (implicit) */
        ExoticArray array = new ExoticArray(cx.getRealm());
        /* step 5 */
        array.setPrototype(proto);
        /* step 7 */
        array.initialisationState = true;
        /* step 8 (not applicable) */
        /* step 9 (see above) */
        /* step 10 */
        array.ordinaryDefineOwnProperty("length",
                new PropertyDescriptor(length, true, false, false));
        /* step 11 */
        return array;
    }

    /**
     * Helper method to create dense arrays
     * <p>
     * [Called from generated code]
     */
    public static ExoticArray DenseArrayCreate(ExecutionContext cx, Object[] values) {
        ExoticArray array = ArrayCreate(cx, values.length);
        for (int i = 0, len = values.length; i < len; ++i) {
            array.addProperty(Integer.toString(i), new Property(values[i], true, true, true));
        }
        return array;
    }

    /**
     * Helper method to create sparse arrays
     * <p>
     * [Called from generated code]
     */
    public static ExoticArray SparseArrayCreate(ExecutionContext cx, Object[] values) {
        ExoticArray array = ArrayCreate(cx, values.length);
        for (int i = 0, len = values.length; i < len; ++i) {
            if (values[i] != null) {
                array.addProperty(Integer.toString(i), new Property(values[i], true, true, true));
            }
        }
        return array;
    }

    /**
     * 9.4.2.3 ArraySetLength(A, Desc) Abstract Operation
     */
    public static boolean ArraySetLength(ExecutionContext cx, ExoticArray array,
            PropertyDescriptor desc) {
        /* step 1 */
        if (!desc.hasValue()) {
            return array.ordinaryDefineOwnProperty("length", desc);
        }
        /* step 2 */
        PropertyDescriptor newLenDesc = desc.clone();
        /* step 3 */
        long newLen = ToUint32(cx, desc.getValue());
        /* step 4 */
        if (newLen != ToNumber(cx, desc.getValue())) {
            throw throwRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* step 5 */
        newLenDesc.setValue(newLen);
        /* step 6 */
        Property oldLenDesc = array.getOwnProperty(cx, "length");
        assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
        /* step 7 */
        long oldLen = ToUint32(cx, oldLenDesc.getValue());
        /* step 8 */
        if (newLen >= oldLen) {
            return array.ordinaryDefineOwnProperty("length", newLenDesc);
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
        boolean succeeded = array.ordinaryDefineOwnProperty("length", newLenDesc);
        /* step 14 */
        if (!succeeded) {
            return false;
        }
        /* step 15 */
        if ((oldLen - newLen) > 1000) {
            oldLen = SparseArraySetLength(cx, array, newLen);
        } else {
            oldLen = DenseArraySetLength(cx, array, oldLen, newLen);
        }
        /* step 15.d */
        if (oldLen >= 0) {
            newLenDesc.setValue(oldLen + 1);
            if (!newWritable) {
                newLenDesc.setWritable(false);
            }
            array.ordinaryDefineOwnProperty("length", newLenDesc);
            return false;
        }
        /* step 16 */
        if (!newWritable) {
            PropertyDescriptor nonWritable = new PropertyDescriptor();
            nonWritable.setWritable(false);
            array.ordinaryDefineOwnProperty("length", nonWritable);
        }
        /* step 17 */
        return true;
    }

    private static long DenseArraySetLength(ExecutionContext cx, ExoticArray array, long oldLen,
            long newLen) {
        while (newLen < oldLen) {
            oldLen -= 1;
            boolean deleteSucceeded = array.delete(cx, ToString(oldLen));
            if (!deleteSucceeded) {
                return oldLen;
            }
        }
        return -1;
    }

    private static long SparseArraySetLength(ExecutionContext cx, ExoticArray array, long newLen) {
        long[] indices = array.indices(newLen);
        for (int i = indices.length - 1; i >= 0; --i) {
            long oldLen = indices[i];
            boolean deleteSucceeded = array.delete(cx, ToString(oldLen));
            if (!deleteSucceeded) {
                return oldLen;
            }
        }
        return -1;
    }

    private long[] indices(long minIndex) {
        Collection<String> keys = enumerateKeys();
        long[] indices = new long[keys.size()];
        int i = 0;
        for (String key : keys) {
            long index = toArrayIndex(key);
            if (index >= minIndex) {
                indices[i++] = index;
            }
        }
        indices = Arrays.copyOf(indices, i);
        Arrays.sort(indices);
        return indices;
    }
}
