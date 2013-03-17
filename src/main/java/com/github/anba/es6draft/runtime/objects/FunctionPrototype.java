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
import static com.github.anba.es6draft.runtime.types.BuiltinBrand.hasBuiltinBrand;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction.BoundFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.3 Function Objects</h2>
 * <ul>
 * <li>15.3.4 Properties of the Function Prototype Object
 * <li>15.3.5 Properties of Function Instances
 * </ul>
 */
public class FunctionPrototype extends OrdinaryObject implements ScriptObject, Callable,
        Initialisable {
    public FunctionPrototype(Realm realm) {
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
        return "function FunctionPrototype() { /* native code */ }";
    }

    /**
     * [[Call]]
     */
    @Override
    public Object call(Object thisValue, Object... args) {
        return UNDEFINED;
    }

    /**
     * 15.3.4 Properties of the Function Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = false))
        public static final int length = 0;

        /**
         * 15.3.4.1 Function.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Function;

        /**
         * 15.3.4.2 Function.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(Realm realm, Object thisValue) {
            if (!(thisValue instanceof Callable)) {
                throw throwTypeError(realm, Messages.Key.IncompatibleObject);
            }
            return ((Callable) thisValue).toSource();
        }

        /**
         * 15.3.4.3 Function.prototype.apply (thisArg, argArray)
         */
        @Function(name = "apply", arity = 2)
        public static Object apply(Realm realm, Object thisValue, Object thisArg, Object argArray) {
            Object func = thisValue;
            if (!IsCallable(func)) {
                throw throwTypeError(realm, Messages.Key.IncompatibleObject);
            }
            if (Type.isUndefinedOrNull(argArray)) {
                return ((Callable) func).call(thisArg);
            }
            if (!Type.isObject(argArray)) {
                throw throwTypeError(realm, Messages.Key.NotObjectType);
            }
            ScriptObject argarray = Type.objectValue(argArray);
            Object len = Get(argarray, "length");
            long n = ToUint32(realm, len);
            assert n < 256;// TODO: actual limit?!
            Object[] argList = new Object[(int) n];
            for (int index = 0; index < n; ++index) {
                String indexName = ToString(index);
                Object nextArg = Get(argarray, indexName);
                argList[index] = nextArg;
            }
            return ((Callable) func).call(thisArg, argList);
        }

        /**
         * 15.3.4.4 Function.prototype.call (thisArg [ , arg1 [ , arg2, ... ] ] )
         */
        @Function(name = "call", arity = 1)
        public static Object call(Realm realm, Object thisValue, Object thisArg, Object... args) {
            Object func = thisValue;
            if (!IsCallable(func)) {
                throw throwTypeError(realm, Messages.Key.IncompatibleObject);
            }
            return ((Callable) func).call(thisArg, args);
        }

        /**
         * 15.3.4.5 Function.prototype.bind (thisArg [, arg1 [, arg2, ...]])
         */
        @Function(name = "bind", arity = 1)
        public static Object bind(Realm realm, Object thisValue, Object thisArg, Object... args) {
            Object target = thisValue;
            if (!IsCallable(target)) {
                throw throwTypeError(realm, Messages.Key.IncompatibleObject);
            }
            ExoticBoundFunction f = BoundFunctionCreate(realm, (Callable) target, thisArg, args);
            int l;
            if (hasBuiltinBrand(target, BuiltinBrand.BuiltinFunction)) {
                Object targetLen = Get((ScriptObject) target, "length");
                l = (int) Math.max(0, ToInteger(realm, targetLen) - args.length);
            } else {
                l = 0;
            }
            f.defineOwnProperty("length", new PropertyDescriptor(l, false, false, false));
            AddRestrictedFunctionProperties(realm, f);
            return f;
        }

        /**
         * 15.3.4.6 Function.prototype[ @@create ] ( )
         */
        @Function(name = "@@create", arity = 0, symbol = BuiltinSymbol.create)
        public static Object create(Realm realm, Object thisValue) {
            return OrdinaryCreateFromConstructor(realm, thisValue, Intrinsics.ObjectPrototype);
        }

        /**
         * 15.3.4.7 Function.prototype[@@hasInstance] (V)
         */
        @Function(name = "@@hasInstance", arity = 1, symbol = BuiltinSymbol.hasInstance)
        public static Object hasInstance(Realm realm, Object thisValue, Object v) {
            Object f = thisValue;
            return OrdinaryHasInstance(realm, f, v);
        }
    }
}
