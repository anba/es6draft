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
     * @param prototype
     *            the prototype object
     * @param stringData
     *            the string data
     */
    public StringObject(Realm realm, ScriptObject prototype, CharSequence stringData) {
        super(realm);
        this.stringData = stringData;
        setPrototype(prototype);
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
        return super.hasOwnProperty(cx, propertyKey) || propertyKey < getStringData().length();
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
        // FIXME: spec bug - don't traverse proto chain for own indexed properties! (bug 3618)
        boolean hasOrdinary = ordinaryHasOwnProperty(propertyKey);
        if (hasOrdinary) {
            return true;
        }
        return propertyKey < getStringData().length();
    }

    /**
     * 9.4.3.3 [[Enumerate]] ()
     */
    @Override
    protected List<String> getEnumerableKeys(ExecutionContext cx) {
        /* steps 1-7 */
        return new CompoundList<>(new StringPropertyKeyList(getStringData().length()),
                super.getEnumerableKeys(cx));
    }

    /**
     * 9.4.3.4 [[OwnPropertyKeys]] ()
     */
    @Override
    protected List<Object> getOwnPropertyKeys(ExecutionContext cx) {
        /* steps 1-8 */
        return new CompoundList<>(new StringPropertyKeyList(getStringData().length()),
                super.getOwnPropertyKeys(cx));
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
     * 9.4.3.6 StringCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param stringData
     *            the string value
     * @return the new string object
     */
    public static StringObject StringCreate(ExecutionContext cx, CharSequence stringData) {
        return StringCreate(cx, cx.getIntrinsic(Intrinsics.StringPrototype), stringData);
    }

    /**
     * 9.4.3.6 StringCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param prototype
     *            the prototype object
     * @param stringData
     *            the string value
     * @return the new string object
     */
    public static StringObject StringCreate(ExecutionContext cx, ScriptObject prototype,
            CharSequence stringData) {
        /* step 1 (not applicable) */
        /* steps 2-9 */
        return new StringObject(cx.getRealm(), prototype, stringData);
    }
}
