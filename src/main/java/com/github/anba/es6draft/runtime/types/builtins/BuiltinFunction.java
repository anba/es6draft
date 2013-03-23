/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.isStrictFunction;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.7 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinFunction extends OrdinaryObject implements ScriptObject, Callable {
    public BuiltinFunction(Realm realm) {
        super(realm);
    }

    @Override
    public String toSource() {
        Property desc = ordinaryGetOwnProperty("name");
        assert desc != null && desc.isDataDescriptor() : "built-in functions have an own 'name' data property";
        Object name = desc.getValue();
        assert Type.isString(name) : "'name' is a string valued data property";
        return String.format("function %s() { /* native code */ }", Type.stringValue(name));
    }

    /**
     * 8.3.19.3 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(String propertyKey, Object receiver) {
        /* step 1-2 */
        Object v = super.get(propertyKey, receiver);
        /* step 3 */
        if ("caller".equals(propertyKey) && isStrictFunction(v)) {
            // TODO: spec bug? [[Get]] override necessary, cf. AddRestrictedFunctionProperties
            return NULL;
        }
        /* step 4 */
        return v;
    }

    /**
     * 8.3.19.4 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(String propertyKey) {
        /* step 1-2 */
        Property v = super.getOwnProperty(propertyKey);
        if (v != null && v.isDataDescriptor()) {
            // TODO: spec bug? [[GetOwnProperty]] override necessary, cf.
            // AddRestrictedFunctionProperties
            if ("caller".equals(propertyKey) && isStrictFunction(v)) {
                PropertyDescriptor desc = v.toPropertyDescriptor();
                desc.setValue(NULL);
                v = desc.toProperty();
            }
        }
        return v;
    }
}
