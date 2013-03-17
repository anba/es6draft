/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.OrdinaryCreateFromConstructor;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.6 Boolean Objects</h2>
 * <ul>
 * <li>15.6.1 The Boolean Constructor Called as a Function
 * <li>15.6.2 The Boolean Constructor
 * <li>15.6.3 Properties of the Boolean Constructor
 * </ul>
 */
public class BooleanConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public BooleanConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
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
        boolean b = (args.length > 0 ? ToBoolean(args[0]) : false);
        if (thisValue instanceof BooleanObject) {
            BooleanObject obj = (BooleanObject) thisValue;
            if (!obj.isInitialised()) {
                obj.setBooleanData(b);
                return obj;
            }
        }
        return b;
    }

    /**
     * 15.6.2.1 new Boolean (value)
     */
    @Override
    public Object construct(Object... args) {
        return OrdinaryConstruct(realm(), this, args);
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

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "Boolean";

        /**
         * 15.6.3.1 Boolean.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.BooleanPrototype;

        /**
         * 15.6.3.2 Boolean[ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(Realm realm, Object thisValue) {
            return OrdinaryCreateFromConstructor(realm, thisValue, Intrinsics.BooleanPrototype,
                    BooleanObjectAllocator.INSTANCE);
        }
    }

    private static class BooleanObjectAllocator implements ObjectAllocator<BooleanObject> {
        static final ObjectAllocator<BooleanObject> INSTANCE = new BooleanObjectAllocator();

        @Override
        public BooleanObject newInstance(Realm realm) {
            return new BooleanObject(realm);
        }
    }
}
