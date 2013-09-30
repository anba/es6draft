/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.isStrictFunction;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 ECMAScript Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.2 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>9.2.7 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinFunction extends OrdinaryObject implements Callable {
    /** [[Realm]] */
    private final Realm realm;

    public BuiltinFunction(Realm realm) {
        super(realm);
        this.realm = realm;
    }

    /**
     * [[Realm]]
     */
    protected final Realm realm() {
        return realm;
    }

    /**
     * Returns the callee execution context
     */
    protected final ExecutionContext calleeContext() {
        return realm.defaultContext();
    }

    /**
     * Creates the default function properties, i.e. 'name' and 'length', initialises the
     * [[Prototype]] to the <code>%FunctionPrototype%</code> object and calls
     * {@link OrdinaryFunction#AddRestrictedFunctionProperties(ExecutionContext, ScriptObject)}
     */
    protected final void setupDefaultFunctionProperties(String name, int arity) {
        ExecutionContext cx = realm.defaultContext();
        setPrototype(realm.getIntrinsic(Intrinsics.FunctionPrototype));
        defineOwnProperty(cx, "name", new PropertyDescriptor(name, false, false, false));
        defineOwnProperty(cx, "length", new PropertyDescriptor(arity, false, false, true));
        AddRestrictedFunctionProperties(cx, this);
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
     * 9.1.15.3 [[Get]] (P, Receiver)
     */
    @Override
    public Object get(ExecutionContext cx, String propertyKey, Object receiver) {
        /* steps 1-2 */
        Object v = super.get(cx, propertyKey, receiver);
        /* step 3 */
        if ("caller".equals(propertyKey) && isStrictFunction(v)) {
            // TODO: spec bug? [[Get]] override necessary, cf. AddRestrictedFunctionProperties
            // (Bug 1223)
            return NULL;
        }
        /* step 4 */
        return v;
    }

    /**
     * 9.1.15.4 [[GetOwnProperty]] (P)
     */
    @Override
    public Property getOwnProperty(ExecutionContext cx, String propertyKey) {
        /* steps 1-2 */
        Property v = super.getOwnProperty(cx, propertyKey);
        /* step 3 */
        if (v != null && v.isDataDescriptor()) {
            // TODO: spec bug? [[GetOwnProperty]] override necessary, cf.
            // AddRestrictedFunctionProperties (Bug 1223)
            if ("caller".equals(propertyKey) && isStrictFunction(v.getValue())) {
                PropertyDescriptor desc = v.toPropertyDescriptor();
                desc.setValue(NULL);
                v = desc.toProperty();
            }
        }
        /* step 4 */
        return v;
    }
}
