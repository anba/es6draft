/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.DefinePropertyOrThrow;

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
public final class ExoticString extends OrdinaryObject {
    /** [[StringData]] */
    private CharSequence stringData = null;

    public ExoticString(Realm realm) {
        super(realm);
    }

    /**
     * Returns the [[StringData]] internal data property, if the property is undefined, the empty
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

    public static int toStringIndex(String p) {
        return Strings.toIndex(p);
    }

    /**
     * [[HasOwnProperty]] (P)
     */
    @Override
    protected boolean hasOwnProperty(ExecutionContext cx, String propertyKey) {
        boolean has = super.hasOwnProperty(cx, propertyKey);
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
     * 9.4.3.1 [[GetOwnProperty]] ( P )
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
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
        return new Property(resultStr, false, true, false);
    }

    /**
     * 9.4.3.2 [[Enumerate]] ()
     */
    @Override
    protected List<String> enumerateKeys(ExecutionContext cx) {
        List<String> keys = super.enumerateKeys(cx);
        addStringIndices(keys);
        return keys;
    }

    /**
     * 9.4.3.3 [[OwnPropertyKeys]] ()
     */
    @Override
    protected List<Object> enumerateOwnKeys(ExecutionContext cx) {
        List<Object> keys = super.enumerateOwnKeys(cx);
        addStringIndices(keys);
        return keys;
    }

    @Override
    protected boolean isEnumerableOwnProperty(ExecutionContext cx, String key) {
        int index = toStringIndex(key);
        if (index >= 0 && index < getStringDataOrEmpty().length() && !super.hasOwnProperty(cx, key)) {
            return true;
        }
        return super.isEnumerableOwnProperty(cx, key);
    }

    /**
     * 9.4.3.5 StringCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param prototype
     *            the prototype object
     * @return the new string exotic object
     */
    public static ExoticString StringCreate(ExecutionContext cx, ScriptObject prototype) {
        // steps 1-7, 9 (implicit)
        ExoticString obj = new ExoticString(cx.getRealm());
        // step 8
        obj.setPrototype(prototype);
        return obj;
    }

    /**
     * Custom helper function.
     * 
     * @param cx
     *            the execution context
     * @param stringData
     *            the string value
     * @return the new string exotic object
     */
    public static ExoticString StringCreate(ExecutionContext cx, CharSequence stringData) {
        ExoticString obj = StringCreate(cx, cx.getIntrinsic(Intrinsics.StringPrototype));
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
    private void addStringIndices(List<? super String> keys) {
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
