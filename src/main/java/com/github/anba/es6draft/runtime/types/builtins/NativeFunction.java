/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.lang.invoke.MethodHandle;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Function;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.7 Built-in Function Objects
 * </ul>
 */
public class NativeFunction extends BuiltinFunction {
    // (Object, Object[]) -> Object
    private final MethodHandle mh;

    public NativeFunction(Realm realm, String name, int arity, MethodHandle mh) {
        super(realm);
        this.mh = mh;
        setPrototype(realm.getIntrinsic(Intrinsics.FunctionPrototype));
        defineOwnProperty("name", new PropertyDescriptor(name, false, false, false));
        defineOwnProperty("length", new PropertyDescriptor(arity, false, false, false));
        AddRestrictedFunctionProperties(realm, this);
    }

    /**
     * [[Call]]
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        try {
            return mh.invokeExact(thisValue, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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

    private static boolean isStrictFunction(Object v) {
        return v instanceof Function && ((Function) v).isStrict();
    }
}
