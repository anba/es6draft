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
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.Symbol;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.14 Map Objects</h2>
 * <ul>
 * <li>15.14.1 Abstract Operations For Map Objects
 * <li>15.14.2 The Map Constructor Called as a Function
 * <li>15.14.3 The Map Constructor
 * <li>15.14.4 Properties of the Map Constructor
 * </ul>
 */
public class MapConstructor extends OrdinaryObject implements Scriptable, Callable, Constructor,
        Initialisable {
    public MapConstructor(Realm realm) {
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

    /**
     * 15.14.1.1 MapInitialisation
     */
    public static Scriptable MapInitialisation(Realm realm, Scriptable obj, Object iterable) {
        if (!Type.isObject(obj)) {
            throw throwTypeError(realm, Messages.Key.NotObjectType);
        }
        if (!(obj instanceof MapObject)) {
            throw throwTypeError(realm, Messages.Key.IncompatibleObject);
        }
        if (!obj.isExtensible()) {
            throw throwTypeError(realm, Messages.Key.NotExtensible);
        }
        if (!Type.isUndefined(iterable)) {
            Scriptable _iterable = ToObject(realm, iterable);
            Symbol iterator = BuiltinSymbol.iterator.get();
            Object itr = Invoke(realm, _iterable, iterator);
            Object adder = Get(obj, "set");
            if (!IsCallable(adder)) {
                throw throwTypeError(realm, Messages.Key.NotCallable);
            }
            for (;;) {
                Object next;
                try {
                    next = Invoke(realm, itr, "next");
                } catch (ScriptException e) {
                    if (IteratorComplete(realm, e)) {
                        return obj;
                    }
                    throw e;
                }
                Scriptable entry = ToObject(realm, next);
                Object k = Get(entry, "0");
                Object v = Get(entry, "1");
                ((Callable) adder).call(obj, k, v);
            }
        } else {
            return obj;
        }
    }

    @Override
    public String toSource() {
        return "function Map() { /* native code */ }";
    }

    /**
     * 15.14.2.1 Map (iterable = undefined )
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        Scriptable map = ToObject(realm(), thisValue);
        Object iterable = args.length > 0 ? args[0] : UNDEFINED;
        MapInitialisation(realm(), map, iterable);
        return map;
    }

    /**
     * 15.14.3.1 new Map (iterable = undefined )
     */
    @Override
    public Object construct(Object... args) {
        return OrdinaryConstruct(realm(), this, args);
    }

    /**
     * 15.14.4 Properties of the Map Constructor
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.FunctionPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 0;

        /**
         * 15.14.4.1 Map.prototype
         */
        @Value(name = "prototype", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final Intrinsics prototype = Intrinsics.MapPrototype;

        /**
         * 15.14.4.2 @@create ( )
         */
        @Function(name = "@@create", symbol = BuiltinSymbol.create, arity = 0)
        public static Object create(Realm realm, Object thisValue) {
            Object f = thisValue;
            Scriptable obj = OrdinaryCreateFromConstructor(realm, f, Intrinsics.MapPrototype);
            // obj.[[MapData]] = {}; (implicit)
            return obj;
        }
    }
}
