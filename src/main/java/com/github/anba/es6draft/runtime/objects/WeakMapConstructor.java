/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.StopIterationObject.IteratorComplete;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.15 WeakMap Objects</h2>
 * <ul>
 * <li>15.15.1 The WeakMap Constructor Called as a Function
 * <li>15.15.2 The WeakMap Constructor
 * <li>15.15.3 Properties of the WeakMap Constructor
 * </ul>
 */
public class WeakMapConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public WeakMapConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(Realm realm) {
        createProperties(this, realm, Properties.class);
        AddRestrictedFunctionProperties(realm, this);
    }

    @Override
    public String toSource() {
        return "function WeakMap() { /* native code */ }";
    }

    /**
     * 15.15.1.1 WeakMap (iterable = undefined )
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Realm realm = realm();
        Object iterable = args.length > 0 ? args[0] : UNDEFINED;

        /* steps 1-4 */
        if (!Type.isObject(thisValue)) {
            throw throwTypeError(realm, Messages.Key.NotObjectType);
        }
        if (!(thisValue instanceof WeakMapObject)) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        WeakMapObject map = (WeakMapObject) thisValue;
        if (map.isInitialised()) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }

        /* steps 5-7 */
        Object itr, adder = null;
        if (Type.isUndefinedOrNull(iterable)) {
            itr = UNDEFINED;
        } else {
            Symbol iterator = BuiltinSymbol.iterator.get();
            itr = Invoke(realm, iterable, iterator);
            adder = Get(map, "set");
            if (!IsCallable(adder)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
        }

        /* step 8 */
        map.initialise();

        /* steps 9-10 */
        if (Type.isUndefined(itr)) {
            return map;
        }
        for (;;) {
            Object next;
            try {
                next = Invoke(realm, itr, "next");
            } catch (ScriptException e) {
                if (IteratorComplete(realm, e)) {
                    return map;
                }
                throw e;
            }
            ScriptObject entry = ToObject(realm, next);
            Object k = Get(entry, "0");
            Object v = Get(entry, "1");
            ((Callable) adder).call(map, k, v);
        }
    }

    /**
     * 15.15.2.1 new WeakMap ( ... args )
     */
    @Override
    public Object construct(Object... args) {
        return OrdinaryConstruct(realm(), this, args);
    }

    /**
     * 15.15.3 Properties of the WeakMap Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final String name = "WeakMap";

        /**
         * 15.15.3.1 WeakMap.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.WeakMapPrototype;

        /**
         * 15.15.3.2 WeakMap[ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(Realm realm, Object thisValue) {
            return OrdinaryCreateFromConstructor(realm, thisValue, Intrinsics.WeakMapPrototype,
                    WeakMapObjectAllocator.INSTANCE);
        }
    }

    private static class WeakMapObjectAllocator implements ObjectAllocator<WeakMapObject> {
        static final ObjectAllocator<WeakMapObject> INSTANCE = new WeakMapObjectAllocator();

        @Override
        public WeakMapObject newInstance(Realm realm) {
            return new WeakMapObject(realm);
        }
    }
}
