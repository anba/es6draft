/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.throwRangeError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorComplete;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.IteratorValue;
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
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.14 Map Objects</h2>
 * <ul>
 * <li>15.14.1 The Map Constructor Called as a Function
 * <li>15.14.2 The Map Constructor
 * <li>15.14.3 Properties of the Map Constructor
 * </ul>
 */
public class MapConstructor extends BuiltinFunction implements Constructor, Initialisable {
    public MapConstructor(Realm realm) {
        super(realm);
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * 15.14.1.1 Map (iterable = undefined , comparator = undefined )
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        ExecutionContext calleeContext = realm().defaultContext();
        Object iterable = args.length > 0 ? args[0] : UNDEFINED;
        Object comparator = args.length > 1 ? args[1] : UNDEFINED;

        /* steps 1-4 */
        if (!Type.isObject(thisValue)) {
            // FIXME: spec bug ? `Map()` no longer allowed (Bug 1406)
            throw throwTypeError(calleeContext, Messages.Key.NotObjectType);
        }
        if (!(thisValue instanceof MapObject)) {
            throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }
        MapObject map = (MapObject) thisValue;
        if (map.isInitialised()) {
            throw throwTypeError(calleeContext, Messages.Key.IncompatibleObject);
        }

        /* steps 5-7 */
        Object itr, adder = null;
        if (Type.isUndefinedOrNull(iterable)) {
            itr = UNDEFINED;
        } else {
            ScriptObject _iterable = ToObject(calleeContext, iterable);
            boolean hasValues = HasProperty(calleeContext, _iterable, "entries");
            // FIXME: spec bug? should iterable[@@iterator]() === undefined be an error?
            if (hasValues) {
                itr = Invoke(calleeContext, _iterable, "entries");
            } else {
                Symbol iterator = BuiltinSymbol.iterator.get();
                itr = Invoke(calleeContext, _iterable, iterator);
            }
            adder = Get(calleeContext, map, "set");
            if (!IsCallable(adder)) {
                throw throwTypeError(calleeContext, Messages.Key.NotCallable);
            }
        }

        /* steps 8-10 */
        MapObject.Comparator _comparator = MapObject.Comparator.SameValueZero;
        if (!Type.isUndefined(comparator)) {
            if (!SameValue(comparator, "is")) {
                throw throwRangeError(calleeContext, Messages.Key.MapInvalidComparator);
            }
            _comparator = MapObject.Comparator.SameValue;
        }
        map.initialise(_comparator);

        /* steps 11-12 */
        if (Type.isUndefined(itr)) {
            return map;
        }
        for (;;) {
            Object next = Invoke(calleeContext, itr, "next");
            if (!Type.isObject(next)) {
                throw throwTypeError(calleeContext, Messages.Key.NotObjectType);
            }
            ScriptObject nextObject = Type.objectValue(next);
            boolean done = IteratorComplete(calleeContext, nextObject);
            if (done) {
                return map;
            }
            Object nextItem = IteratorValue(calleeContext, nextObject);
            ScriptObject entry = ToObject(calleeContext, nextItem);
            Object k = Get(calleeContext, entry, "0");
            Object v = Get(calleeContext, entry, "1");
            ((Callable) adder).call(calleeContext, map, k, v);
        }
    }

    /**
     * 15.14.2.1 new Map ( ... args )
     */
    @Override
    public Object construct(ExecutionContext callerContext, Object... args) {
        return OrdinaryConstruct(callerContext, this, args);
    }

    /**
     * 15.14.3 Properties of the Map Constructor
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
        public static final String name = "Map";

        /**
         * 15.14.3.1 Map.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.MapPrototype;

        /**
         * 15.14.3.2 Map[ @@create ] ( )
         */
        @Function(
                name = "@@create",
                symbol = BuiltinSymbol.create,
                arity = 0,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.MapPrototype,
                    MapObjectAllocator.INSTANCE);
        }
    }

    private static class MapObjectAllocator implements ObjectAllocator<MapObject> {
        static final ObjectAllocator<MapObject> INSTANCE = new MapObjectAllocator();

        @Override
        public MapObject newInstance(Realm realm) {
            return new MapObject(realm);
        }
    }
}
