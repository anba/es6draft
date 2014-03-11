/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.collection;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

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
 * <h2>23.4 WeakSet Objects</h2>
 * <ul>
 * <li>23.4.1 The WeakSet Constructor
 * <li>23.4.2 Properties of the WeakSet Constructor
 * </ul>
 */
public final class WeakSetConstructor extends BuiltinConstructor implements Initialisable {
    public WeakSetConstructor(Realm realm) {
        super(realm, "WeakSet");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 23.4.1.1 WeakSet (iterable = undefined)
     */
    @Override
    public WeakSetObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = args.length > 0 ? args[0] : UNDEFINED;

        /* steps 1-4 */
        if (!Type.isObject(thisValue)) {
            throw newTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        if (!(thisValue instanceof WeakSetObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        WeakSetObject set = (WeakSetObject) thisValue;
        if (set.isInitialised()) {
            throw newTypeError(calleeContext, Messages.Key.InitialisedObject);
        }

        /* steps 5-7 */
        ScriptObject iter;
        Callable adder = null;
        if (Type.isUndefinedOrNull(iterable)) {
            iter = null;
        } else {
            iter = GetIterator(calleeContext, iterable);
            Object _adder = Get(calleeContext, set, "add");
            if (!IsCallable(_adder)) {
                throw newTypeError(calleeContext, Messages.Key.NotCallable);
            }
            adder = (Callable) _adder;
        }

        // FIXME: spec bug - https://bugs.ecmascript.org/show_bug.cgi?id=2397
        if (set.isInitialised()) {
            throw newTypeError(calleeContext, Messages.Key.InitialisedObject);
        }

        /* step 8 */
        set.initialise();

        /* step 9 */
        if (iter == null) {
            return set;
        }
        /* step 10 */
        for (;;) {
            ScriptObject next = IteratorStep(calleeContext, iter);
            if (next == null) {
                return set;
            }
            Object nextValue = IteratorValue(calleeContext, next);
            adder.call(calleeContext, set, nextValue);
        }
    }

    /**
     * 23.4.1.2 new WeakSet (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 23.4.2 Properties of the WeakSet Constructor
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
        public static final String name = "WeakSet";

        /**
         * 23.4.2.1 WeakSet.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.WeakSetPrototype;

        /**
         * 23.4.2.2 WeakSet[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.WeakSetPrototype,
                    WeakSetObjectAllocator.INSTANCE);
        }
    }

    private static final class WeakSetObjectAllocator implements ObjectAllocator<WeakSetObject> {
        static final ObjectAllocator<WeakSetObject> INSTANCE = new WeakSetObjectAllocator();

        @Override
        public WeakSetObject newInstance(Realm realm) {
            return new WeakSetObject(realm);
        }
    }
}
