/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.util.Collection;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Scriptable;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.3 String Exotic Objects
 * </ul>
 */
public class ExoticString extends OrdinaryObject implements Scriptable {
    /**
     * [[StringData]]
     */
    private final CharSequence stringData;

    public ExoticString(Realm realm, CharSequence stringData) {
        super(realm);
        this.stringData = stringData;

        // 8.4.3 String Exotic Objects
        // 15.5.5 Properties of String Instances
        // 15.5.5.1 length
        defineOwnProperty("length",
                new PropertyDescriptor(stringData.length(), false, false, false));
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinStringWrapper;
    }

    /**
     * [[StringData]]
     */
    public CharSequence getStringData() {
        return stringData;
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
    public boolean hasOwnProperty(String propertyKey) {
        boolean has = super.hasOwnProperty(propertyKey);
        if (has) {
            return true;
        }
        int index = toStringIndex(propertyKey);
        if (index < 0) {
            return false;
        }
        CharSequence str = stringData;
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
    public Property getOwnProperty(String propertyKey) {
        Property desc = ordinaryGetOwnProperty(propertyKey);
        if (desc != null) {
            return desc;
        }
        int index = toStringIndex(propertyKey);
        if (index < 0) {
            return null;
        }
        CharSequence str = stringData;
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
    public boolean defineOwnProperty(String propertyKey, PropertyDescriptor desc) {
        Property current = getOwnProperty(propertyKey);
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
        if (index >= 0 && index < stringData.length()) {
            return true;
        }
        return super.isEnumerableOwnProperty(key);
    }

    /**
     * Append string indices to {@code keys} collection
     */
    private void addStringIndices(Collection<? super String> keys) {
        // SpiderMonkey appends string indices, whereas JSC/V8 prepends the indices
        for (int i = 0, length = stringData.length(); i < length; ++i) {
            keys.add(Integer.toString(i));
        }
    }
}
