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

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.2 Array Exotic Objects
 * </ul>
 */
public class ExoticArray extends OrdinaryObject implements Scriptable {
    public ExoticArray(Realm realm) {
        super(realm);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinArray;
    }

    // [[Set]] no longer overridden in rev. 13
    // FIXME: spec bug (per introductory paragraph [[Set]] is overridden!)

    // /**
    // * 8.4.2.1 [[Set] ( P, V, Receiver)
    // */
    // @Override
    // public boolean set(String propertyKey, Object value, Object receiver) {
    // PropertyDescriptor ownDesc = ordinaryGetOwnProperty(propertyKey);
    // // FIXME : spec bug (bug 1067)
    // if (ownDesc == null) {
    // Scriptable parent = getPrototype();
    // if (parent != null) {
    // return parent.set(propertyKey, value, receiver);
    // } else {
    // if (Type.of(receiver) != Type.Object) {
    // return false;
    // }
    // return CreateOwnDataProperty(Type.objectValue(receiver), propertyKey, value);
    // }
    // }
    // if (ownDesc.isDataDescriptor()) {
    // if (!ownDesc.isWritable()) {
    // return false;
    // }
    // if (SameValue(this, receiver)) {
    // PropertyDescriptor valueDesc = new PropertyDescriptor(value);
    // if ("length".equals(propertyKey)) {
    // return ArraySetLength(this, valueDesc);
    // } else {
    // return ordinaryDefineOwnProperty(propertyKey, valueDesc);
    // }
    // } else {
    // if (Type.of(receiver) != Type.Object) {
    // return false;
    // }
    // return CreateOwnDataProperty(Type.objectValue(receiver), propertyKey, value);
    // }
    // }
    // // FIXME : spec bug (bug 1067)
    // assert ownDesc.isAccessorDescriptor();
    // Callable setter = ownDesc.getSetter();
    // if (setter == null) {
    // return false;
    // }
    // setter.call(receiver, value);
    // return true;
    // }

    /**
     * 8.4.2.1 [[DefineOwnProperty]] ( P, Desc)
     */
    @Override
    public boolean defineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        if ("length".equals(propertyKey)) {
            return ArraySetLength(this, desc);
        } else if (isArrayIndex(propertyKey)) {
            Property oldLenDesc = getOwnProperty("length");
            assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
            long oldLen = ToUint32(realm(), oldLenDesc.getValue());
            long index = ToUint32(realm(), propertyKey);
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

    // FIXME: spec bug (inconsistent section numbers, from 8.4.2.1 to 8.4.2.3)

    /**
     * 8.4.2.3 ArrayCreate Abstract Operation
     */
    public static Scriptable ArrayCreate(Realm realm, long length) {
        assert length >= 0 && length <= 4294967295L;
        /* step 1-4, 6-7 (implicit) */
        ExoticArray array = new ExoticArray(realm);
        // FIXME: spec bug (step 3-4 -> [[Set]] no longer overridden)
        /* step 5 */
        array.setPrototype(realm.getIntrinsic(Intrinsics.ArrayPrototype));
        /* step 8 */
        array.ordinaryDefineOwnProperty("length",
                new PropertyDescriptor(length, true, false, false));
        /* step 9 */
        return array;
    }

    /**
     * 8.4.2.4 ArraySetLength Abstract Operation
     */
    public static boolean ArraySetLength(ExoticArray array, PropertyDescriptor desc) {
        Realm realm = array.realm();
        // FIXME: spec bug (https://bugs.ecmascript.org/show_bug.cgi?id=1200)
        // Property oldLenDesc = array.getOwnProperty("length");
        // assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
        // long oldLen = ToUint32(realm, oldLenDesc.getValue());
        if (!desc.hasValue()) {
            return array.ordinaryDefineOwnProperty("length", desc);
        }
        PropertyDescriptor newLenDesc = new PropertyDescriptor(desc);
        long newLen = ToUint32(realm, desc.getValue());
        if (newLen != ToNumber(realm, desc.getValue())) {
            throw throwRangeError(realm, Messages.Key.InvalidArrayLength);
        }
        newLenDesc.setValue(newLen);
        Property oldLenDesc = array.getOwnProperty("length");
        assert oldLenDesc != null && !oldLenDesc.isAccessorDescriptor();
        long oldLen = ToUint32(realm, oldLenDesc.getValue());
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
            oldLen = SparseArraySetLength(array, newLen);
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
                boolean deleteSucceeded = array.delete(ToString(oldLen));
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

    private static long SparseArraySetLength(ExoticArray array, long newLen) {
        long[] indices = array.indices(newLen);
        for (int i = indices.length - 1; i >= 0; --i) {
            long oldLen = indices[i];
            boolean deleteSucceeded = array.delete(ToString(oldLen));
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
        final long limit = 4294967295L;
        // spec definition:
        // long val = ToUint32(p);
        // return val != limit && p.equals(ToString(val));

        int length = p.length();
        if (length < 1 || length > 10) {
            // empty string or definitely greater than "4294967295"
            // "4294967295".length == 10
            return -1;
        }
        if (p.charAt(0) == '0') {
            return (length == 1 ? 0 : -1);
        }
        long acc = 0L;
        for (int i = 0; i < length; ++i) {
            char c = p.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                return -1;
            }
            acc = acc * 10 + (c - '0');
        }
        return acc < limit ? acc : -1;
    }
}
