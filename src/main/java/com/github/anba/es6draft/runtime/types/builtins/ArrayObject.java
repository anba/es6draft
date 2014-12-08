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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.IndexedMap;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.ArrayIteratorPrototype;
import com.github.anba.es6draft.runtime.objects.ArrayPrototype;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
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
public final class ArrayObject extends OrdinaryObject {
    /** [[ArrayInitializationState]] */
    private boolean initialized = false;
    private boolean hasIndexedAccessors = false;
    private boolean lengthWritable = true;
    private long length = 0;

    /**
     * Constructs a new Array object.
     * 
     * @param realm
     *            the realm object
     */
    public ArrayObject(Realm realm) {
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
     * Returns the array's length.
     * 
     * @return the array's length
     */
    public long getLength() {
        return length;
    }

    /**
     * Sets the array's length.
     * 
     * @param length
     *            the new array length
     */
    public void setLengthUnchecked(long length) {
        assert this.length <= length && lengthWritable;
        this.length = length;
    }

    /**
     * Returns {@code true} if the array is dense and has no indexed accessors.
     * 
     * @return {@code true} if the array is dense
     */
    public boolean isDenseArray() {
        IndexedMap<Property> ix = indexedProperties();
        return !hasIndexedAccessors && ix.getLength() == length && !ix.isSparse() && !ix.hasHoles();
    }

    /**
     * Returns the array's indexed property values. Only applicable for dense arrays.
     * 
     * @return the array's indexed values
     */
    public Object[] toArray() {
        assert isDenseArray();
        IndexedMap<Property> indexed = indexedProperties();
        long length = this.length;
        assert 0 <= length && length <= Integer.MAX_VALUE : "length=" + length;
        int len = (int) length;
        Object[] values = new Object[len];
        for (int i = 0; i < len; ++i) {
            values[i] = indexed.get(i).getValue();
        }
        return values;
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
    private static boolean isArrayIndex(long p) {
        return 0 <= p && p < 0xFFFF_FFFFL;
    }

    private boolean isCompatibleLengthProperty(PropertyDescriptor desc, long newLength) {
        if (desc.isEmpty()) {
            return true;
        }
        if (desc.isAccessorDescriptor() || desc.isConfigurable() || desc.isEnumerable()) {
            return false;
        }
        if (desc.isGenericDescriptor()) {
            return true;
        }
        assert desc.isDataDescriptor();
        if (!lengthWritable && (desc.isWritable() || (desc.hasValue() && newLength != length))) {
            return false;
        }
        return true;
    }

    private boolean defineLength(PropertyDescriptor desc, long newLength) {
        assert desc.hasValue() ? newLength >= 0 : newLength < 0;
        boolean succeeded = isCompatibleLengthProperty(desc, newLength);
        if (succeeded) {
            if (newLength >= 0) {
                length = newLength;
            }
            if (desc.hasWritable()) {
                lengthWritable = desc.isWritable();
            }
        }
        return succeeded;
    }

    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        if ("length".equals(propertyKey)) {
            return true;
        }
        return super.hasOwnProperty(cx, propertyKey);
    }

    @Override
    protected Property getProperty(ExecutionContext cx, String propertyKey) {
        if ("length".equals(propertyKey)) {
            return new Property(length, lengthWritable, false, false);
        }
        return super.getProperty(cx, propertyKey);
    }

    @Override
    protected boolean deleteProperty(ExecutionContext cx, String propertyKey) {
        if ("length".equals(propertyKey)) {
            return false;
        }
        return super.deleteProperty(cx, propertyKey);
    }

    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        /* step 1 */
        ArrayList<Object> ownKeys = new ArrayList<>();
        /* step 2 */
        if (!indexedProperties().isEmpty()) {
            ownKeys.addAll(indexedProperties().keys());
        }
        /* step 3 */
        ownKeys.add("length");
        if (!properties().isEmpty()) {
            ownKeys.addAll(properties().keySet());
        }
        /* step 4 */
        if (!symbolProperties().isEmpty()) {
            ownKeys.addAll(symbolProperties().keySet());
        }
        /* step 5 */
        return ownKeys;
    }

    /**
     * 9.4.2.1 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    protected boolean defineProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (isArrayIndex(propertyKey)) {
            /* steps 3.a-3.c */
            long oldLen = this.length;
            /* steps 3.d-3.e */
            long index = propertyKey;
            /* steps 3.f */
            if (index >= oldLen && !lengthWritable) {
                return false;
            }
            /* steps 3.g-3.h */
            boolean succeeded = ordinaryDefineOwnProperty(cx, propertyKey, desc);
            /* step 3.i */
            if (!succeeded) {
                return false;
            }
            /* step 3.j */
            if (index >= oldLen) {
                this.length = index + 1;
            }
            hasIndexedAccessors |= desc.isAccessorDescriptor();
            /* step 3.k */
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
     * 9.4.2.2 ArrayCreate(length, proto) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @return the new array object
     */
    public static ArrayObject ArrayCreate(ExecutionContext cx, ScriptObject proto) {
        assert proto != null;
        /* steps 1-3 (not applicable) */
        /* steps 4-6, 8 (implicit) */
        ArrayObject array = new ArrayObject(cx.getRealm());
        /* step 7 */
        array.setPrototype(proto);
        /* step 9 (not applicable) */
        /* step 10 */
        long length = 0;
        /* step 11 (not applicable) */
        /* step 12 */
        array.length = length;
        /* step 13 */
        return array;
    }

    /**
     * 9.4.2.2 ArrayCreate(length, proto) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param length
     *            the array length
     * @return the new array object
     */
    public static ArrayObject ArrayCreate(ExecutionContext cx, long length) {
        return ArrayCreate(cx, length, cx.getIntrinsic(Intrinsics.ArrayPrototype));
    }

    /**
     * 9.4.2.2 ArrayCreate(length, proto) Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param length
     *            the array length
     * @param proto
     *            the prototype object
     * @return the new array object
     */
    public static ArrayObject ArrayCreate(ExecutionContext cx, long length, ScriptObject proto) {
        assert proto != null;
        /* steps 1-2 */
        assert length >= 0;
        /* step 3 (not applicable) */
        /* step 11 (moved) */
        if (length > 0xFFFF_FFFFL) {
            // enforce array index invariant
            throw newRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* steps 4-6, 8 (implicit) */
        ArrayObject array = new ArrayObject(cx.getRealm());
        /* step 7 */
        array.setPrototype(proto);
        /* step 9 */
        array.initialized = true;
        /* step 10 (not applicable) */
        /* step 11 (see above) */
        /* step 12 */
        array.length = length;
        /* step 13 */
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
    public static ArrayObject DenseArrayCreate(ExecutionContext cx, Object[] values) {
        ArrayObject array = ArrayCreate(cx, values.length);
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
    public static ArrayObject SparseArrayCreate(ExecutionContext cx, Object[] values) {
        ArrayObject array = ArrayCreate(cx, values.length);
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
    public static boolean ArraySetLength(ExecutionContext cx, ArrayObject array,
            PropertyDescriptor desc) {
        /* step 1 */
        if (!desc.hasValue()) {
            return array.defineLength(desc, -1);
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
        /* steps 6-8 (not applicable) */
        /* step 9 */
        long oldLen = array.length;
        /* step 10 */
        if (newLen >= oldLen) {
            return array.defineLength(newLenDesc, newLen);
        }
        /* step 11 */
        if (!array.lengthWritable) {
            return false;
        }
        /* steps 12-13 */
        boolean newWritable;
        if (!newLenDesc.hasWritable() || newLenDesc.isWritable()) {
            newWritable = true;
        } else {
            newWritable = false;
            newLenDesc.setWritable(true);
        }
        /* steps 14-15 */
        boolean succeeded = array.defineLength(newLenDesc, newLen);
        /* step 16 */
        if (!succeeded) {
            return false;
        }
        /* step 17 */
        oldLen = ArraySetLength(array, newLen, oldLen);
        /* step 17.d */
        if (oldLen >= 0) {
            array.length = oldLen + 1;
            if (!newWritable) {
                array.lengthWritable = false;
            }
            return false;
        }
        /* step 18 */
        if (!newWritable) {
            array.lengthWritable = false;
        }
        /* step 19 */
        return true;
    }

    private static long ArraySetLength(ArrayObject array, long newLen, long oldLen) {
        IndexedMap<Property> indexed = array.indexedProperties();
        long lastIndex;
        if (indexed.isSparse()) {
            lastIndex = SparseArraySetLength(indexed, newLen, oldLen);
        } else {
            lastIndex = DenseArraySetLength(indexed, newLen, oldLen);
        }
        // Need to call updateLength() manually.
        indexed.updateLength();
        return lastIndex;
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

    /**
     * Checks if an object uses the built-in array iterator.
     * 
     * @param object
     *            the object to validate
     * @return {@code true} if {@code object} uses the built-in array iterator
     */
    /*package*/static boolean hasBuiltinArrayIterator(OrdinaryObject object) {
        // Test 1: Is object[Symbol.iterator] == %ArrayPrototype%.values?
        Property iteratorProp = object.ordinaryGetOwnProperty(BuiltinSymbol.iterator.get());
        if (iteratorProp == null || !ArrayPrototype.isBuiltinValues(iteratorProp.getValue())) {
            return false;
        }
        // Test 2: Is %ArrayIteratorPrototype%.next the built-in next method?
        OrdinaryObject arrayIterProto = ((NativeFunction) iteratorProp.getValue()).getRealm()
                .getIntrinsic(Intrinsics.ArrayIteratorPrototype);
        Property iterNextProp = arrayIterProto.ordinaryGetOwnProperty("next");
        if (iterNextProp == null || !ArrayIteratorPrototype.isBuiltinNext(iterNextProp.getValue())) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the array is applicable for fast spread operations.
     * 
     * @return {@code true} if the array is applicable for fast spread
     */
    public boolean isSpreadable() {
        if (!isDenseArray()) {
            return false;
        }
        if (ordinaryHasOwnProperty(BuiltinSymbol.iterator.get())) {
            return false;
        }
        if (!(getPrototype() instanceof ArrayPrototype)) {
            return false;
        }
        return hasBuiltinArrayIterator((ArrayPrototype) getPrototype());
    }

    /**
     * Inserts the given array object into this array.
     * 
     * @param spreadArray
     *            the array to spread
     * @param index
     *            the start index
     */
    // TODO: rename?
    public void spread(ArrayObject spreadArray, int index) {
        assert isExtensible() && lengthWritable && index >= this.length;
        assert spreadArray.isSpreadable();
        IndexedMap<Property> spreadIndexed = spreadArray.indexedProperties();
        IndexedMap<Property> indexed = indexedProperties();
        long length = spreadArray.length;
        assert 0 <= length && length <= Integer.MAX_VALUE : "length=" + length;
        assert index + length <= Integer.MAX_VALUE;
        int len = (int) length;
        for (int i = 0, j = index; i < len; ++i, ++j) {
            Object value = spreadIndexed.get(i).getValue();
            indexed.put(j, new Property(value, true, true, true));
        }
        this.length = index + len;
    }

    /**
     * Inserts the given arguments object into this array.
     * 
     * @param spreadArguments
     *            the array to spread
     * @param index
     *            the start index
     */
    // TODO: rename?
    public void spread(ArgumentsObject spreadArguments, int index) {
        assert isExtensible() && lengthWritable && index >= this.length;
        assert spreadArguments.isSpreadable();
        IndexedMap<Property> spreadIndexed = spreadArguments.indexedProperties();
        ParameterMap parameterMap = spreadArguments.getParameterMap();
        IndexedMap<Property> indexed = indexedProperties();
        long length = spreadArguments.getLength();
        assert 0 <= length && length <= Integer.MAX_VALUE : "length=" + length;
        assert index + length <= Integer.MAX_VALUE;
        int len = (int) length;
        for (int i = 0, j = index; i < len; ++i, ++j) {
            Object value;
            if (parameterMap != null && parameterMap.hasOwnProperty(i, false)) {
                value = parameterMap.get(i);
            } else {
                value = spreadIndexed.get(i).getValue();
            }
            indexed.put(j, new Property(value, true, true, true));
        }
        this.length = index + len;
    }
}
