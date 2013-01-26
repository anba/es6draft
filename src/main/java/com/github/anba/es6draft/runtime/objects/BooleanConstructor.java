/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.6 Boolean Objects</h2>
 * <ul>
 * <li>15.6.1 The Boolean Constructor Called as a Function
 * <li>15.6.2 The Boolean Constructor
 * <li>15.6.3 Properties of the Boolean Constructor
 * </ul>
 */
public class BooleanConstructor extends OrdinaryObject implements Scriptable, Callable,
        Constructor, Initialisable {
    public BooleanConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    /**
     * [[BuiltinBrand]]
     */
    @Override
    public BuiltinBrand getBuiltinBrand() {
        return BuiltinBrand.BuiltinFunction;
    }

    @Override
    public String toSource() {
        return "function Boolean() { /* native code */ }";
    }

    /**
     * 15.6.1.1 Boolean (value)
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Object value = args.length > 0 ? args[0] : UNDEFINED;
        return ToBoolean(value);
    }

    /**
     * 15.6.2.1 new Boolean (value)
     */
    @Override
    public Object construct(Object... args) {
        Object value = args.length > 0 ? args[0] : UNDEFINED;
        boolean booleanData = ToBoolean(value);
        BooleanObject obj = new BooleanObject(realm(), booleanData);
        obj.setPrototype(realm().getIntrinsic(Intrinsics.BooleanPrototype));
        return obj;
    }

    /**
     * 15.6.3 Properties of the Boolean Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 1;

        /**
         * 15.6.3.1 Boolean.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.BooleanPrototype;
    }
}
