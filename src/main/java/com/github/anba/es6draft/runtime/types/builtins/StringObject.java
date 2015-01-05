/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.4.3 String Exotic Objects
 * </ul>
 */
public final class StringObject extends OrdinaryObject {
    /** [[StringData]] */
    private CharSequence stringData = null;

    /**
     * Constructs a new String object.
     * 
     * @param realm
     *            the realm object
     */
    public StringObject(Realm realm) {
        super(realm);
    }

    /**
     * Returns the [[StringData]] internal data property. If the property is undefined, the empty
     * string is returned.
     * 
     * @return the string data or the empty string
     */
    private CharSequence getStringDataOrEmpty() {
        return stringData != null ? stringData : "";
    }

    /**
     * [[StringData]]
     * 
     * @return the string data
     */
    public CharSequence getStringData() {
        return stringData;
    }

    /**
     * [[StringData]]
     * 
     * @param stringData
     *            the new string value
     */
    public void setStringData(CharSequence stringData) {
        assert this.stringData == null;
        this.stringData = stringData;
    }

    @Override
    public boolean hasSpecialIndexedProperties() {
        return true;
    }

    /**
     * [[HasOwnProperty]] (P)
     */
    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, long propertyKey) {
        boolean has = super.hasOwnProperty(cx, propertyKey);
        if (has) {
            return true;
        }
        return propertyKey < getStringDataOrEmpty().length();
    }

    /**
     * 9.4.3.1 [[GetOwnProperty]] ( P )
     */
    @Override
    protected Property getProperty(ExecutionContext cx, long propertyKey) {
        /* step 1 (not applicable) */
        /* step 2 */
        Property desc = ordinaryGetOwnProperty(propertyKey);
        /* step 3 */
        if (desc != null) {
            return desc;
        }
        /* steps 4-9 (not applicable) */
        /* step 10 */
        CharSequence str = getStringDataOrEmpty();
        /* step 11 */
        int len = str.length();
        /* step 12 */
        if (len <= propertyKey) {
            return null;
        }
        int index = (int) propertyKey;
        /* step 13 */
        String resultStr = String.valueOf(str.charAt(index));
        /* step 14 */
        return new Property(resultStr, false, true, false);
    }

    /**
     * 9.4.3.2 [[Enumerate]] ()
     */
    @Override
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        /* step 1 */
        ArrayList<String> keys = new ArrayList<>();
        /* steps 2-4 */
        addStringIndices(keys);
        /* steps 5-6 */
        if (!indexedProperties().isEmpty()) {
            keys.addAll(indexedProperties().keys(getStringDataOrEmpty().length(),
                    0x1F_FFFF_FFFF_FFFFL));
        }
        if (!properties().isEmpty()) {
            keys.addAll(properties().keySet());
        }
        /* step 7 */
        return keys;
    }

    /**
     * 9.4.3.3 [[OwnPropertyKeys]] ()
     */
    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        /* step 1 */
        ArrayList<Object> ownKeys = new ArrayList<>();
        /* steps 2-4 */
        addStringIndices(ownKeys);
        /* step 5 */
        if (!indexedProperties().isEmpty()) {
            ownKeys.addAll(indexedProperties().keys(getStringDataOrEmpty().length(),
                    0x1F_FFFF_FFFF_FFFFL));
        }
        /* step 6 */
        if (!properties().isEmpty()) {
            ownKeys.addAll(properties().keySet());
        }
        /* step 7 */
        if (!symbolProperties().isEmpty()) {
            ownKeys.addAll(symbolProperties().keySet());
        }
        /* step 8 */
        return ownKeys;
    }

    @Override
    protected Enumerability isEnumerableOwnProperty(String key) {
        int index = Strings.toStringIndex(key);
        if (0 <= index && index < getStringDataOrEmpty().length() && !ordinaryHasOwnProperty(index)) {
            return Enumerability.Enumerable;
        }
        return super.isEnumerableOwnProperty(key);
    }

    /**
     * 9.4.3.5 StringCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param prototype
     *            the prototype object
     * @return the new string object
     */
    public static StringObject StringCreate(ExecutionContext cx, ScriptObject prototype) {
        /* steps 1-5, 7 (implicit) */
        StringObject obj = new StringObject(cx.getRealm());
        /* step 6 */
        obj.setPrototype(prototype);
        /* step 8 */
        return obj;
    }

    /**
     * Custom helper function.
     * 
     * @param cx
     *            the execution context
     * @param stringData
     *            the string value
     * @return the new string object
     */
    public static StringObject StringCreate(ExecutionContext cx, CharSequence stringData) {
        StringObject obj = StringCreate(cx, cx.getIntrinsic(Intrinsics.StringPrototype));
        DefinePropertyOrThrow(cx, obj, "length", new PropertyDescriptor(stringData.length(), false,
                false, false));
        obj.setStringData(stringData);
        return obj;
    }

    /**
     * Append string indices to {@code keys} collection.
     * 
     * @param keys
     *            the property keys
     */
    private void addStringIndices(ArrayList<? super String> keys) {
        for (int i = 0, length = getStringDataOrEmpty().length(); i < length; ++i) {
            keys.add(Integer.toString(i));
        }
    }
}
