/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.OrdinaryConstruct;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinConstructor;

/**
 * <h1>23 Keyed Collection</h1><br>
 * <h2>23.3 WeakMap Objects</h2>
 * <ul>
 * <li>23.3.1 The WeakMap Constructor
 * <li>23.3.2 Properties of the WeakMap Constructor
 * </ul>
 */
public class WeakMapConstructor extends BuiltinConstructor implements Initialisable {
    public WeakMapConstructor(Realm realm) {
        super(realm, "WeakMap");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 23.3.1.1 WeakMap (iterable = undefined)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = args.length > 0 ? args[0] : UNDEFINED;

        /* steps 1-4 */
        if (!Type.isObject(thisValue)) {
            throw throwTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        if (!(thisValue instanceof WeakMapObject)) {
            throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        WeakMapObject map = (WeakMapObject) thisValue;
        if (map.isInitialised()) {
            throw throwTypeError(calleeContext, Messages.Key.InitialisedObject);
        }

        /* steps 5-7 */
        ScriptObject iter;
        Callable adder = null;
        if (Type.isUndefinedOrNull(iterable)) {
            iter = null;
        } else {
            iter = GetIterator(calleeContext, iterable);
            Object _adder = Get(calleeContext, map, "set");
            if (!IsCallable(_adder)) {
                throw throwTypeError(calleeContext, Messages.Key.NotCallable);
            }
            adder = (Callable) _adder;
        }

        /* step 8 */
        map.initialise();

        /* step 9 */
        if (iter == null) {
            return map;
        }
        /* step 10 */
        for (;;) {
            ScriptObject next = IteratorStep(calleeContext, iter);
            if (next == null) {
                return map;
            }
            Object nextValue = IteratorValue(calleeContext, next);
            if (!Type.isObject(nextValue)) {
                throw throwTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            ScriptObject entry = Type.objectValue(nextValue);
            Object k = Get(calleeContext, entry, "0");
            Object v = Get(calleeContext, entry, "1");
            adder.call(calleeContext, map, k, v);
        }
    }

    /**
     * 23.3.1.2 new WeakMap (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 23.3.2 Properties of the WeakMap Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "WeakMap";

        /**
         * 23.3.2.1 WeakMap.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.WeakMapPrototype;

        /**
         * 23.3.2.2 WeakMap[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.WeakMapPrototype,
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
