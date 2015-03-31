/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompoundList;
import com.github.anba.es6draft.runtime.internal.StringPropertyKeyList;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
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
    private final CharSequence stringData;

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
        super(realm);
        // StringCreate - step 4
        this.stringData = stringData;
        // StringCreate - step 9
        setPrototype(prototype);
        // StringCreate - steps 11-13
        infallibleDefineOwnProperty("length",
                new Property(stringData.length(), false, false, false));
    }

    /**
     * [[StringData]]
     * 
     * @return the string data
     */
    public CharSequence getStringData() {
        return stringData;
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
        return propertyKey < getStringData().length() || super.hasOwnProperty(cx, propertyKey);
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
        /* step 4 */
        return StringGetIndexProperty(this, propertyKey);
    }

    /**
     * 9.4.3.1.1 StringGetIndexProperty (S, P)
     * 
     * @param s
     *            the string object
     * @param propertyKey
     *            the property key
     * @return the property descriptor or {@code null}
     */
    private static Property StringGetIndexProperty(StringObject s, long propertyKey) {
        /* steps 1-6 (not applicable) */
        assert propertyKey >= 0;
        /* step 7 */
        CharSequence str = s.getStringData();
        /* step 8 */
        int len = str.length();
        /* step 9 */
        if (len <= propertyKey) {
            return null;
        }
        int index = (int) propertyKey;
        /* step 10 */
        String resultStr = String.valueOf(str.charAt(index));
        /* step 11 */
        return new Property(resultStr, false, true, false);
    }

    /**
     * 9.4.3.2 [[HasProperty]](P)
     */
    @Override
    protected boolean has(ExecutionContext cx, long propertyKey) {
        /* steps 1-3 */
        return propertyKey < getStringData().length() || super.has(cx, propertyKey);
    }

    /**
     * 9.4.3.3 [[OwnPropertyKeys]] ()
     */
    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        /* steps 1-8 */
        return new CompoundList<>(new StringPropertyKeyList(getStringData().length()),
                super.getOwnPropertyKeys(cx));
    }

    @Override
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        return new CompoundList<>(new StringPropertyKeyList(getStringData().length()),
                super.getEnumerableKeys(cx));
    }

    @Override
    protected Enumerability isEnumerableOwnProperty(String key) {
        int index = Strings.toStringIndex(key);
        if (0 <= index && index < getStringData().length()) {
            return Enumerability.Enumerable;
        }
        return super.isEnumerableOwnProperty(key);
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
        /* steps 1-14 */
        return new StringObject(cx.getRealm(), stringData,
                cx.getIntrinsic(Intrinsics.StringPrototype));
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
    public static StringObject StringCreate(ExecutionContext cx, CharSequence stringData,
            ScriptObject prototype) {
        /* steps 1-2 (not applicable) */
        /* steps 3-14 */
        return new StringObject(cx.getRealm(), stringData, prototype);
    }
}
