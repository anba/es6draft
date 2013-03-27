/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;

import java.util.Collection;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.3 String Exotic Objects
 * </ul>
 */
public class ExoticString extends OrdinaryObject {
    /** [[StringData]] */
    private CharSequence stringData = null;

    public ExoticString(Realm realm) {
        super(realm);
    }

    private CharSequence getStringDataOrEmpty() {
        // FIXME: spec bug (undefined [[StringData]] not handled in spec)
        return (stringData != null ? stringData : "");
    }

    /**
     * [[StringData]]
     */
    public CharSequence getStringData() {
        return stringData;
    }

    /**
     * [[StringData]]
     */
    public void setStringData(CharSequence stringData) {
        assert this.stringData == null;
        this.stringData = stringData;
    }

    public static int toStringIndex(String p) {
        final long limit = Integer.MAX_VALUE;
        int length = p.length();
        if (length < 1 || length > 10) {
            // empty string or definitely greater than "2147483647"
            // "2147483647".length == 10
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
        return acc <= limit ? (int) acc : -1;
    }

    /**
     * 8.4.3.1 [[HasOwnProperty]] (P)
     */
    @Override
    public boolean hasOwnProperty(Realm realm, String propertyKey) {
        boolean has = super.hasOwnProperty(realm, propertyKey);
        if (has) {
            return true;
        }
        int index = toStringIndex(propertyKey);
        if (index < 0) {
            return false;
        }
        CharSequence str = getStringDataOrEmpty();
        int len = str.length();
        if (len <= index) {
            return false;
        }
        return true;
    }

    /**
     * 8.4.3.2 [[GetOwnProperty]] ( P )
     */
    @Override
    public Property getOwnProperty(Realm realm, String propertyKey) {
        Property desc = ordinaryGetOwnProperty(propertyKey);
        if (desc != null) {
            return desc;
        }
        int index = toStringIndex(propertyKey);
        if (index < 0) {
            return null;
        }
        CharSequence str = getStringDataOrEmpty();
        int len = str.length();
        if (len <= index) {
            return null;
        }
        CharSequence resultStr = str.subSequence((int) index, (int) index + 1);
        return new PropertyDescriptor(resultStr, false, true, false).toProperty();
    }

    /**
     * 8.4.3.3 [[DefineOwnProperty]] (P, Desc)
     */
    @Override
    public boolean defineOwnProperty(Realm realm, String propertyKey, PropertyDescriptor desc) {
        Property current = getOwnProperty(realm, propertyKey);
        boolean extensible = isExtensible();
        return ValidateAndApplyPropertyDescriptor(this, propertyKey, extensible, desc, current);
    }

    /**
     * 8.4.3.4 [[Enumerate]] ()
     */
    @Override
    protected Collection<String> enumerateKeys() {
        Collection<String> keys = super.enumerateKeys();
        addStringIndices(keys);
        return keys;
    }

    /**
     * 8.4.3.5 [[OwnPropertyKeys]] ()
     */
    @Override
    protected Collection<Object> enumerateOwnKeys() {
        Collection<Object> keys = super.enumerateOwnKeys();
        addStringIndices(keys);
        return keys;
    }

    @Override
    protected boolean isEnumerableOwnProperty(String key) {
        int index = toStringIndex(key);
        if (index >= 0 && index < getStringDataOrEmpty().length()) {
            return true;
        }
        return super.isEnumerableOwnProperty(key);
    }

    /**
     * 8.4.6.6 StringCreate Abstract Operation
     */
    public static ExoticString StringCreate(Realm realm, ScriptObject prototype) {
        // step 1, 2-6, 9 (implicit)
        ExoticString obj = new ExoticString(realm);
        // step 8
        obj.setPrototype(realm, prototype);
        return obj;
    }

    /**
     * Custom helper function
     */
    public static ExoticString StringCreate(Realm realm, CharSequence stringData) {
        ExoticString obj = StringCreate(realm, realm.getIntrinsic(Intrinsics.StringPrototype));
        DefinePropertyOrThrow(realm, obj, "length", new PropertyDescriptor(stringData.length(),
                false, false, false));
        obj.setStringData(stringData);
        return obj;
    }

    /**
     * Append string indices to {@code keys} collection
     */
    private void addStringIndices(Collection<? super String> keys) {
        // No longer possible to simply add string indices, cf.
        // ---
        // s = new (class extends String{constructor(){}})()
        // Object.defineProperty(s, "0", {value:'hello'})
        // String.call(s, "world")
        // ---
        // SpiderMonkey appends string indices, whereas JSC/V8 prepends the indices
        for (int i = 0, length = getStringDataOrEmpty().length(); i < length; ++i) {
            String s = Integer.toString(i);
            if (ordinaryGetOwnProperty(s) == null) {
                keys.add(s);
            }
        }
    }

}
