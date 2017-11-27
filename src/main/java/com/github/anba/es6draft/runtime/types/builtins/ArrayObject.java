/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.binary.TypedArrayObject;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Slots</h2>
 * <ul>
 * <li>9.4.2 Array Exotic Objects
 * </ul>
 */
public class ArrayObject extends OrdinaryObject {
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
     * Constructs a new Array object.
     * 
     * @param realm
     *            the realm object
     * @param length
     *            the initial length
     * @param prototype
     *            the prototype object
     */
    protected ArrayObject(Realm realm, long length, ScriptObject prototype) {
        super(realm, prototype);
        this.length = length;
    }

    /**
     * Returns the array's length.
     * 
     * @return the array's length
     */
    @Override
    public final long getLength() {
        return length;
    }

    /**
     * Sets the array's length.
     * 
     * @param length
     *            the new array length
     */
    public final void setLengthUnchecked(long length) {
        assert this.length <= length && lengthWritable;
        this.length = length;
    }

    /**
     * Returns the own, dense element from the requested index.
     * 
     * @param propertyKey
     *            the indexed property key
     * @return the property value
     */
    public final Object getDenseElement(long propertyKey) {
        assert isDenseArray() && propertyKey < length;
        return getIndexed(propertyKey);
    }

    /**
     * Returns {@code true} if the array has indexed accessors.
     * 
     * @return {@code true} if the array has indexed accessors
     */
    @Override
    public final boolean hasIndexedAccessors() {
        return hasIndexedAccessors;
    }

    @Override
    public final String className() {
        return "Array";
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
        if (!lengthWritable && (desc.isWritable() || newLength != length)) {
            return false;
        }
        return true;
    }

    private boolean defineLength(PropertyDescriptor desc, long newLength) {
        boolean succeeded = isCompatibleLengthProperty(desc, newLength);
        if (succeeded) {
            length = newLength;
            if (desc.hasWritable()) {
                lengthWritable = desc.isWritable();
            }
        }
        return succeeded;
    }

    private boolean defineLength(long newLength) {
        assert newLength >= 0;
        boolean succeeded = (lengthWritable || newLength == length);
        if (succeeded) {
            length = newLength;
        }
        return succeeded;
    }

    @Override
    public final boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        if ("length".equals(propertyKey)) {
            return true;
        }
        return super.hasOwnProperty(cx, propertyKey);
    }

    @Override
    public final Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        if ("length".equals(propertyKey)) {
            return new Property(length, lengthWritable, false, false);
        }
        return super.getOwnProperty(cx, propertyKey);
    }

    @Override
    public final Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        if ("length".equals(propertyKey)) {
            return length;
        }
        return super.get(cx, propertyKey, receiver);
    }

    @Override
    public final boolean set(ExecutionContext cx, String propertyKey, Object value, Object receiver) {
        if (this == receiver && "length".equals(propertyKey)) {
            if (!lengthWritable) {
                return false;
            }
            return ArraySetLength(cx, this, value);
        }
        return super.set(cx, propertyKey, value, receiver);
    }

    @Override
    protected final void ownPropertyNames(List<? super String> list) {
        list.add("length");
        super.ownPropertyNames(list);
    }

    /**
     * 9.4.2.1 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        if (isArrayIndex(propertyKey)) {
            /* steps 3.a-c */
            long oldLen = this.length;
            /* step 3.d */
            long index = propertyKey;
            /* step 3.e */
            if (index >= oldLen && !lengthWritable) {
                return false;
            }
            /* step 3.f */
            boolean succeeded = ordinaryDefineOwnProperty(cx, propertyKey, desc);
            /* step 3.g */
            if (!succeeded) {
                return false;
            }
            /* step 3.h */
            if (index >= oldLen) {
                this.length = index + 1;
            }
            hasIndexedAccessors |= desc.isAccessorDescriptor();
            /* step 3.i */
            return true;
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.4.2.1 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, String propertyKey, PropertyDescriptor desc) {
        /* steps 1, 3 (not applicable) */
        /* step 2 */
        if ("length".equals(propertyKey)) {
            return ArraySetLength(cx, this, desc);
        }
        /* step 4 */
        return ordinaryDefineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.4.2.2 ArrayCreate(length, proto)
     * 
     * @param cx
     *            the execution context
     * @param length
     *            the array length
     * @return the new array object
     */
    public static ArrayObject ArrayCreate(ExecutionContext cx, int length) {
        return ArrayCreate(cx, length, cx.getIntrinsic(Intrinsics.ArrayPrototype));
    }

    /**
     * 9.4.2.2 ArrayCreate(length, proto)
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
     * 9.4.2.2 ArrayCreate(length, proto)
     * 
     * @param cx
     *            the execution context
     * @param length
     *            the array length
     * @param proto
     *            the prototype object
     * @return the new array object
     */
    public static ArrayObject ArrayCreate(ExecutionContext cx, int length, ScriptObject proto) {
        assert proto != null;
        /* steps 1-2 */
        assert length >= 0;
        /* steps 3-4 (not applicable) */
        /* steps 5-11 */
        return new ArrayObject(cx.getRealm(), length, proto);
    }

    /**
     * 9.4.2.2 ArrayCreate(length, proto)
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
        /* step 3 */
        if (length > 0xFFFF_FFFFL) {
            // enforce array index invariant
            throw newRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* step 4 (not applicable) */
        /* steps 5-11 */
        return new ArrayObject(cx.getRealm(), length, proto);
    }

    /**
     * Helper method to create dense arrays.
     * 
     * @param cx
     *            the execution context
     * @param proto
     *            the prototype object
     * @param values
     *            the element values
     * @return the new array object
     */
    public static ArrayObject DenseArrayCreate(ExecutionContext cx, ScriptObject proto, Object... values) {
        ArrayObject array = ArrayCreate(cx, values.length, proto);
        for (int i = 0, len = values.length; i < len; ++i) {
            array.setIndexed(i, values[i]);
        }
        return array;
    }

    /**
     * Helper method to create dense arrays.
     * 
     * @param cx
     *            the execution context
     * @param values
     *            the element values
     * @return the new array object
     */
    public static ArrayObject DenseArrayCreate(ExecutionContext cx, Collection<?> values) {
        ArrayObject array = ArrayCreate(cx, values.size());
        int i = 0;
        for (Object value : values) {
            array.setIndexed(i++, value);
        }
        return array;
    }

    /**
     * Helper method to create dense arrays.
     * 
     * @param cx
     *            the execution context
     * @param values
     *            the element values
     * @return the new array object
     */
    public static ArrayObject DenseArrayCreate(ExecutionContext cx, List<?> values) {
        ArrayObject array = ArrayCreate(cx, values.size());
        int i = 0;
        for (Object value : values) {
            array.setIndexed(i++, value);
        }
        return array;
    }

    /**
     * Helper method to create dense arrays.
     * 
     * @param cx
     *            the execution context
     * @param values
     *            the element values
     * @return the new array object
     */
    public static ArrayObject DenseArrayCreate(ExecutionContext cx, Stream<?> values) {
        Spliterator<?> spliterator = values.spliterator();
        ArrayObject array = ArrayCreate(cx, Math.max(spliterator.getExactSizeIfKnown(), 0));
        spliterator.forEachRemaining(value -> {
            array.setIndexed(array.getIndexedLength(), value);
        });
        array.setLengthUnchecked(array.getIndexedLength());
        return array;
    }

    /**
     * Helper method to create dense arrays.
     * <p>
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param values
     *            the element values
     * @return the new array object
     */
    public static ArrayObject DenseArrayCreate(ExecutionContext cx, Object... values) {
        ArrayObject array = ArrayCreate(cx, values.length);
        for (int i = 0, len = values.length; i < len; ++i) {
            array.setIndexed(i, values[i]);
        }
        return array;
    }

    /**
     * Helper method to create sparse arrays.
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
        for (int i = 0, len = values.length; i < len; ++i) {
            if (values[i] != null) {
                array.setIndexed(i, values[i]);
            }
        }
        return array;
    }

    /**
     * 9.4.2.3 ArraySpeciesCreate(originalArray, length)
     * 
     * @param cx
     *            the execution context
     * @param orginalArray
     *            the source array
     * @param length
     *            the array length
     * @return the new array object
     */
    public static ScriptObject ArraySpeciesCreate(ExecutionContext cx, ScriptObject orginalArray, long length) {
        /* step 1 */
        assert length >= 0;
        /* step 2 (not applicable) */
        /* steps 3-4 */
        if (!IsArray(cx, orginalArray)) {
            return ArrayCreate(cx, length);
        }
        /* step 5 */
        Object c = Get(cx, orginalArray, "constructor");
        /* step 6 */
        if (IsConstructor(c)) {
            Constructor constructor = (Constructor) c;
            /* step 6.a */
            Realm thisRealm = cx.getRealm();
            /* step 6.b */
            Realm realmC = GetFunctionRealm(cx, constructor);
            /* step 6.c */
            if (thisRealm != realmC && constructor == realmC.getIntrinsic(Intrinsics.Array)) {
                c = UNDEFINED;
            }
        }
        /* step 7 */
        if (Type.isObject(c)) {
            /* step 7.a.*/
            c = Get(cx, Type.objectValue(c), BuiltinSymbol.species.get());
            /* step 7.b */
            if (Type.isNull(c)) {
                c = UNDEFINED;
            }
        }
        /* step 8 */
        if (Type.isUndefined(c)) {
            return ArrayCreate(cx, length);
        }
        /* step 9 */
        if (!IsConstructor(c)) {
            throw newTypeError(cx, Messages.Key.NotConstructor);
        }
        /* step 10 */
        return ((Constructor) c).construct(cx, length);
    }

    /**
     * 9.4.2.4 ArraySetLength(A, Desc)
     * 
     * @param cx
     *            the execution context
     * @param array
     *            the array object
     * @param desc
     *            the property descriptor
     * @return {@code true} on success
     */
    public static boolean ArraySetLength(ExecutionContext cx, ArrayObject array, PropertyDescriptor desc) {
        /* step 1 */
        if (!desc.hasValue()) {
            return array.defineLength(desc, array.length);
        }
        /* step 2 (not applicable) */
        /* step 3 */
        long newLen = ToUint32(cx, desc.getValue());
        /* step 4 */
        double numberLen = ToNumber(cx, desc.getValue());
        /* step 5 */
        if (newLen != numberLen) {
            throw newRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* step 6 (not applicable) */
        /* steps 7-9 */
        long oldLen = array.length;
        /* step 10 */
        if (newLen >= oldLen) {
            return array.defineLength(desc, newLen);
        }
        /* step 11 */
        if (!array.lengthWritable) {
            return false;
        }
        /* steps 12-13 */
        boolean newWritable;
        if (!desc.hasWritable() || desc.isWritable()) {
            newWritable = true;
        } else {
            newWritable = false;
            desc = desc.clone();
            desc.setWritable(true);
        }
        /* step 14 */
        boolean succeeded = array.defineLength(desc, newLen);
        /* step 15 */
        if (!succeeded) {
            return false;
        }
        /* step 16 */
        long nonDeletableIndex = array.deleteRange(newLen, oldLen);
        /* step 16.c */
        if (nonDeletableIndex >= 0) {
            array.length = nonDeletableIndex + 1;
            if (!newWritable) {
                array.lengthWritable = false;
            }
            return false;
        }
        /* step 17 */
        if (!newWritable) {
            array.lengthWritable = false;
        }
        /* step 18 */
        return true;
    }

    /**
     * 9.4.2.4 ArraySetLength(A, Desc)
     * 
     * @param cx
     *            the execution context
     * @param array
     *            the array object
     * @param lenValue
     *            the new length value
     * @return {@code true} on success
     */
    private static boolean ArraySetLength(ExecutionContext cx, ArrayObject array, Object lenValue) {
        /* steps 1-2 (not applicable) */
        /* step 3 */
        long newLen = ToUint32(cx, lenValue);
        /* step 4 */
        double numberLen = ToNumber(cx, lenValue);
        /* step 5 */
        if (newLen != numberLen) {
            throw newRangeError(cx, Messages.Key.InvalidArrayLength);
        }
        /* step 6 (not applicable) */
        /* steps 7-9 */
        long oldLen = array.length;
        /* step 10 */
        if (newLen >= oldLen) {
            return array.defineLength(newLen);
        }
        /* step 11 */
        if (!array.lengthWritable) {
            return false;
        }
        /* steps 12-13 (not applicable) */
        /* step 14 */
        boolean succeeded = array.defineLength(newLen);
        /* step 15 */
        assert succeeded;
        /* step 16 */
        long nonDeletableIndex = array.deleteRange(newLen, oldLen);
        /* step 16.c */
        if (nonDeletableIndex >= 0) {
            array.length = nonDeletableIndex + 1;
            return false;
        }
        /* step 17 (not applicable) */
        /* step 18 */
        return true;
    }

    /**
     * Inserts the object into this array object.
     * 
     * @param index
     *            the destination start index
     * @param source
     *            the source object
     * @param sourceLength
     *            the source object's length
     */
    public final void insertFrom(int index, OrdinaryObject source, long sourceLength) {
        assert isExtensible() && lengthWritable && index >= this.length;
        assert source.isDenseArray(sourceLength);
        assert 0 <= sourceLength && sourceLength <= Integer.MAX_VALUE : "length=" + sourceLength;
        assert index + sourceLength <= Integer.MAX_VALUE;
        int len = (int) sourceLength;
        for (int i = 0, j = index; i < len; ++i, ++j) {
            setIndexed(j, source.getIndexed(i));
        }
        this.length = index + len;
    }

    /**
     * Inserts the object into this array object.
     * 
     * @param cx
     *            the execution context
     * @param index
     *            the destination start index
     * @param source
     *            the source object
     */
    public final void insertFrom(ExecutionContext cx, int index, TypedArrayObject source) {
        assert isExtensible() && lengthWritable && index >= this.length;
        assert !source.getBuffer().isDetached();
        long sourceLength = source.getLength();
        assert 0 <= sourceLength && sourceLength <= Integer.MAX_VALUE : "length=" + sourceLength;
        assert index + sourceLength <= Integer.MAX_VALUE;
        int len = (int) sourceLength;
        for (int i = 0, j = index; i < len; ++i, ++j) {
            setIndexed(j, source.get(cx, i, source));
        }
        this.length = index + len;
    }

    /**
     * Inserts the value into this array object.
     * 
     * @param index
     *            the array index
     * @param value
     *            the value
     */
    public final void insert(int index, Object value) {
        assert isExtensible() && lengthWritable && index >= getIndexedLength();
        setIndexed(index, value);
        if (index >= length) {
            this.length = index + 1;
        }
    }
}
