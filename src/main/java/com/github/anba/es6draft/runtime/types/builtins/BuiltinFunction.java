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

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinFunction extends OrdinaryObject implements Callable {
    protected static final String ANONYMOUS = "";

    /** [[Realm]] */
    private final Realm realm;

    private final String name;

    public BuiltinFunction(Realm realm, String name) {
        super(realm);
        this.realm = realm;
        this.name = name;
    }

    public BuiltinFunction(Realm realm, String name, int arity) {
        super(realm);
        this.realm = realm;
        this.name = name;
        createDefaultFunctionProperties(name, arity);
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
    private final void createDefaultFunctionProperties(String name, int arity) {
        ExecutionContext cx = realm.defaultContext();
        setPrototype(realm.getIntrinsic(Intrinsics.FunctionPrototype));
        if (!name.isEmpty()) {
            // anonymous functions do not have an own "name" property, cf. 19.2.4.1
            defineOwnProperty(cx, "name", new PropertyDescriptor(name, false, false, true));
        }
        defineOwnProperty(cx, "length", new PropertyDescriptor(arity, false, false, true));
        // 9.3.2 CreateBuiltinFunction Abstract Operation, step 2
        AddRestrictedFunctionProperties(cx, this);
    }

    @Override
    public String toSource() {
        return String.format("function %s() { /* native code */ }", name);
    }

    /**
     * [[Realm]]
     */
    public final Realm getRealm() {
        return realm;
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return call(callerContext, thisValue, args);
    }

    /**
     * 9.2.3 [[Get]] (P, Receiver)
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
     * 9.2.4 [[GetOwnProperty]] (P)
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
