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

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
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
 * <h2>23.1 Map Objects</h2>
 * <ul>
 * <li>23.1.1 The Map Constructor
 * <li>23.1.2 Properties of the Map Constructor
 * </ul>
 */
public final class MapConstructor extends BuiltinConstructor implements Initializable {
    /**
     * Constructs a new Map constructor function.
     * 
     * @param realm
     *            the realm object
     */
    public MapConstructor(Realm realm) {
        super(realm, "Map");
    }

    @Override
    public void initialize(ExecutionContext cx) {
        addRestrictedFunctionProperties(cx);
        createProperties(cx, this, Properties.class);
    }

    @Override
    public MapConstructor clone() {
        return new MapConstructor(getRealm());
    }

    /**
     * 23.1.1.1 Map ([ iterable ])
     */
    @Override
    public MapObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = calleeContext();
        Object iterable = argument(args, 0);

        /* steps 1-4 */
        if (!Type.isObject(thisValue)) {
            throw newTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        if (!(thisValue instanceof MapObject)) {
            throw newTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        MapObject map = (MapObject) thisValue;
        if (map.isInitialized()) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }

        /* steps 5-7 */
        ScriptObject iter;
        Callable adder = null;
        if (Type.isUndefinedOrNull(iterable)) {
            iter = null;
        } else {
            Object _adder = Get(calleeContext, map, "set");
            if (!IsCallable(_adder)) {
                throw newTypeError(calleeContext, Messages.Key.PropertyNotCallable, "set");
            }
            adder = (Callable) _adder;
            iter = GetIterator(calleeContext, iterable);
        }

        /* step 8 */
        if (map.isInitialized()) {
            throw newTypeError(calleeContext, Messages.Key.InitializedObject);
        }

        /* steps 9-10 */
        map.initialize();

        /* step 11 */
        if (iter == null) {
            return map;
        }
        /* step 12 */
        for (;;) {
            ScriptObject next = IteratorStep(calleeContext, iter);
            if (next == null) {
                return map;
            }
            Object nextItem = IteratorValue(calleeContext, next);
            if (!Type.isObject(nextItem)) {
                throw newTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            ScriptObject entry = Type.objectValue(nextItem);
            Object k = Get(calleeContext, entry, 0);
            Object v = Get(calleeContext, entry, 1);
            adder.call(calleeContext, map, k, v);
        }
    }

    /**
     * 23.1.1.2 new Map (...argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 23.1.2 Properties of the Map Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 1;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "Map";

        /**
         * 23.1.2.1 Map.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.MapPrototype;

        /**
         * 23.1.2.2 Map[ @@create ] ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the new uninitialized map object
         */
        @Function(name = "[Symbol.create]", symbol = BuiltinSymbol.create, arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.MapPrototype,
                    MapObjectAllocator.INSTANCE);
        }
    }

    private static final class MapObjectAllocator implements ObjectAllocator<MapObject> {
        static final ObjectAllocator<MapObject> INSTANCE = new MapObjectAllocator();

        @Override
        public MapObject newInstance(Realm realm) {
            return new MapObject(realm);
        }
    }
}
