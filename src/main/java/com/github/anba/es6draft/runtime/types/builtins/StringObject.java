/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;

import java.util.Iterator;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompoundIterator;
import com.github.anba.es6draft.runtime.internal.CompoundList;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.StringPropertyKeyList;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Slots</h2>
 * <ul>
 * <li>9.4.3 String Exotic Objects
 * </ul>
 */
public class StringObject extends OrdinaryObject {
    /**
     * Maximum allowed string length.
     */
    public static final int MAX_LENGTH = 0xFFF_FFFF;

    /** [[StringData]] */
    private final CharSequence stringData;

    /**
     * Constructs a new String object.
     * 
     * @param realm
     *            the realm object
     * @param stringData
     *            the string data
     */
    protected StringObject(Realm realm, CharSequence stringData) {
        super(realm);
        this.stringData = stringData;
    }

    /**
     * Constructs a new String object.
     * 
     * @param realm
     *            the realm object
     * @param stringData
     *            the string data
     * @param prototype
     *            the prototype object
     */
    public StringObject(Realm realm, CharSequence stringData, ScriptObject prototype) {
        super(realm, prototype);
        // StringCreate - step 3
        this.stringData = stringData;
        // StringCreate - steps 9-10
        infallibleDefineOwnProperty("length", new Property(stringData.length(), false, false, false));
    }

    /**
     * [[StringData]]
     * 
     * @return the string data
     */
    public final CharSequence getStringData() {
        return stringData;
    }

    @Override
    public final String className() {
        return "String";
    }

    @Override
    public String toString() {
        return String.format("%s, stringData=%s", super.toString(), stringData);
    }

    @Override
    public final boolean hasSpecialIndexedProperties() {
        return true;
    }

    /**
     * [[HasOwnProperty]] (P)
     */
    @Override
    public final boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        return propertyKey < getStringData().length() || ordinaryHasOwnProperty(propertyKey);
    }

    /**
     * 9.4.3.1 [[GetOwnProperty]] ( P )
     */
    @Override
    public final Property getOwnProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* step 2 */
        Property desc = ordinaryGetOwnProperty(propertyKey);
        /* step 3 */
        if (desc != null) {
            return desc;
        }
        /* step 4 */
        return StringGetOwnProperty(this, propertyKey);
    }

    /**
     * 9.4.3.2 [[DefineOwnProperty]] ( P, Desc )
     */
    @Override
    public final boolean defineOwnProperty(ExecutionContext cx, long propertyKey, PropertyDescriptor desc) {
        /* step 1 (not applicable) */
        /* step 2 */
        Property stringDesc = StringGetOwnProperty(this, propertyKey);
        /* step 3 */
        if (stringDesc != null) {
            /* step 3.a */
            boolean extensible = isExtensible();
            /* step 3.b */
            return IsCompatiblePropertyDescriptor(extensible, desc, stringDesc);
        }
        /* step 4 */
        return super.defineOwnProperty(cx, propertyKey, desc);
    }

    /**
     * 9.4.3.3 [[OwnPropertyKeys]] ()
     */
    @Override
    public final List<Object> ownPropertyKeys(ExecutionContext cx) {
        /* steps 1-8 */
        return new CompoundList<>(new StringPropertyKeyList(getStringData().length()), super.ownPropertyKeys(cx));
    }

    @Override
    public final List<String> ownPropertyNames(ExecutionContext cx) {
        /* steps 1-8 */
        return new CompoundList<>(new StringPropertyKeyList(getStringData().length()), super.ownPropertyNames(cx));
    }

    @Override
    public final Iterator<String> ownEnumerablePropertyKeys(ExecutionContext cx) {
        StringPropertyKeyList stringIndices = new StringPropertyKeyList(getStringData().length());
        return new CompoundIterator<>(stringIndices.iterator(), super.ownEnumerablePropertyKeys(cx));
    }

    @Override
    public final Enumerability isEnumerableOwnProperty(ExecutionContext cx, String key) {
        int index = Strings.toStringIndex(key);
        if (0 <= index && index < getStringData().length()) {
            return Enumerability.Enumerable;
        }
        return super.isEnumerableOwnProperty(cx, key);
    }

    /**
     * 9.4.3.4 StringCreate(value, prototype)
     * 
     * @param cx
     *            the execution context
     * @param stringData
     *            the string value
     * @return the new string object
     */
    public static StringObject StringCreate(ExecutionContext cx, CharSequence stringData) {
        /* steps 1-12 */
        return new StringObject(cx.getRealm(), stringData, cx.getIntrinsic(Intrinsics.StringPrototype));
    }

    /**
     * 9.4.3.4 StringCreate(value, prototype)
     * 
     * @param cx
     *            the execution context
     * @param stringData
     *            the string value
     * @param prototype
     *            the prototype object
     * @return the new string object
     */
    public static StringObject StringCreate(ExecutionContext cx, CharSequence stringData, ScriptObject prototype) {
        /* steps 1-12 */
        return new StringObject(cx.getRealm(), stringData, prototype);
    }

    /**
     * 9.4.3.5 StringGetOwnProperty ( S, P )
     * 
     * @param s
     *            the string object
     * @param propertyKey
     *            the property key
     * @return the property descriptor or {@code null}
     */
    private Property StringGetOwnProperty(StringObject s, long propertyKey) {
        /* steps 1-7 (not applicable) */
        /* step 8 */
        CharSequence str = getStringData();
        /* step 9 */
        int len = str.length();
        /* step 10 */
        if (len <= propertyKey) {
            return null;
        }
        /* step 11 */
        String resultStr = String.valueOf(str.charAt((int) propertyKey));
        /* step 12 */
        return new Property(resultStr, false, true, false);
    }

    /**
     * Throws an error if {@code length} exceeds the maximum allowed string length.
     * 
     * @param cx
     *            the execution context
     * @param length
     *            the string length
     */
    public static void validateLength(ExecutionContext cx, int length) {
        if (length > StringObject.MAX_LENGTH) {
            throw newInternalError(cx, Messages.Key.InvalidStringSize);
        }
    }

    /**
     * Throws an error if {@code s.length()} exceeds the maximum allowed string length.
     * 
     * @param <STRING>
     *            the string type
     * @param cx
     *            the execution context
     * @param s
     *            the string
     * @return the input string
     */
    public static <STRING extends CharSequence> STRING validateLength(ExecutionContext cx, STRING s) {
        validateLength(cx, s.length());
        return s;
    }
}
