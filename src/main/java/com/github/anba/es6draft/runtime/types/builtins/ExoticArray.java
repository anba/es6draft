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
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.2 Array Exotic Objects
 * </ul>
 */
public class ExoticArray extends OrdinaryObject {
    /** [[ArrayInitialisationState]] */
    private boolean arrayInitialisationState = false;

    public ExoticArray(Realm realm) {
        super(realm);
    }

    /** [[ArrayInitialisationState]] */
    public boolean getArrayInitialisationState() {
        return arrayInitialisationState;
    }

    /** [[ArrayInitialisationState]] */
    public void setArrayInitialisationState(boolean arrayInitialisationState) {
        assert arrayInitialisationState : "cannot de-initialise an array";
        assert !this.arrayInitialisationState : "array already initialised";
        this.arrayInitialisationState = arrayInitialisationState;
    }

    // FIXME: spec bug (per introductory paragraph [[Set]] is overridden!)

    /**
     * 8.4.2.1 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(ExecutionContext cx, String propertyKey,
            PropertyDescriptor desc) {
        if ("length".equals(propertyKey)) {
            return ArraySetLength(cx, this, desc);
        } else if (isArrayIndex(propertyKey)) {
            Property oldLenDesc = getOwnProperty(cx, "length");
            assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
            long oldLen = ToUint32(cx, oldLenDesc.getValue());
            long index = ToUint32(cx, propertyKey);
            if (index >= oldLen && !oldLenDesc.isWritable()) {
                return false;
            }
            boolean succeeded = ordinaryDefineOwnProperty(propertyKey, desc);
            if (!succeeded) {
                return false;
            }
            if (index >= oldLen) {
                PropertyDescriptor lenDesc = oldLenDesc.toPropertyDescriptor();
                lenDesc.setValue(index + 1);
                ordinaryDefineOwnProperty("length", lenDesc);
            }
            return true;
        }
        return ordinaryDefineOwnProperty(propertyKey, desc);
    }

    /**
     * 8.4.2.2 ArrayCreate Abstract Operation
     */
    public static ExoticArray ArrayCreate(ExecutionContext cx, long length) {
        return ArrayCreate(cx, length, cx.getIntrinsic(Intrinsics.ArrayPrototype));
    }

    /**
     * 8.4.2.2 ArrayCreate Abstract Operation
     */
    public static ExoticArray ArrayCreate(ExecutionContext cx, long length, ScriptObject proto) {
        assert length <= 4294967295L && proto != null;
        /* step 2-4, 6 (implicit) */
        ExoticArray array = new ExoticArray(cx.getRealm());
        /* step 5 */
        array.setPrototype(cx, proto);
        if (length >= 0) {
            array.arrayInitialisationState = true;
        } else {
            // negative values represent 'undefined'
            array.arrayInitialisationState = false;
            length = 0;
        }
        /* step 8 */
        array.ordinaryDefineOwnProperty("length",
                new PropertyDescriptor(length, true, false, false));
        /* step 9 */
        return array;
    }

    /**
     * 8.4.2.3 ArraySetLength Abstract Operation
     */
    public static boolean ArraySetLength(ExecutionContext cx, ExoticArray array,
            PropertyDescriptor desc) {
        if (!desc.hasValue()) {
            return array.ordinaryDefineOwnProperty("length", desc);
        }
        PropertyDescriptor newLenDesc = new PropertyDescriptor(desc);
        long newLen = ToUint32(cx, desc.getValue());
        if (newLen != ToNumber(cx, desc.getValue())) {
            throw throwRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        newLenDesc.setValue(newLen);
        Property oldLenDesc = array.getOwnProperty(cx, "length");
        assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
        long oldLen = ToUint32(cx, oldLenDesc.getValue());
        if (newLen >= oldLen) {
            return array.ordinaryDefineOwnProperty("length", newLenDesc);
        }
        if (!oldLenDesc.isWritable()) {
            return false;
        }
        boolean newWritable;
        if (!newLenDesc.hasWritable() || newLenDesc.isWritable()) {
            newWritable = true;
        } else {
            newWritable = false;
            newLenDesc.setWritable(true);
        }
        boolean succeeded = array.ordinaryDefineOwnProperty("length", newLenDesc);
        if (!succeeded) {
            return false;
        }
        if ((oldLen - newLen) > 1000) {
            oldLen = SparseArraySetLength(cx, array, newLen);
            if (oldLen >= 0) {
                newLenDesc.setValue(oldLen + 1);
                if (!newWritable) {
                    newLenDesc.setWritable(false);
                }
                array.ordinaryDefineOwnProperty("length", newLenDesc);
                return false;
            }
        } else {
            while (newLen < oldLen) {
                oldLen -= 1;
                boolean deleteSucceeded = array.delete(cx, ToString(oldLen));
                if (!deleteSucceeded) {
                    newLenDesc.setValue(oldLen + 1);
                    if (!newWritable) {
                        newLenDesc.setWritable(false);
                    }
                    array.ordinaryDefineOwnProperty("length", newLenDesc);
                    return false;
                }
            }
        }
        if (!newWritable) {
            PropertyDescriptor nonWritable = new PropertyDescriptor();
            nonWritable.setWritable(false);
            array.ordinaryDefineOwnProperty("length", nonWritable);
        }
        return true;
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

    /**
     * 8.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     */
    public static boolean isArrayIndex(String p) {
        return toArrayIndex(p) >= 0;
    }

    /**
     * 8.4.2 Array Exotic Objects
     * <p>
     * Introductory paragraph
     */
    public static long toArrayIndex(String p) {
        return Strings.toArrayIndex(p);
    }
}
