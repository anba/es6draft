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
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.EMPTY_ARRAY;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.PrepareForTailCall;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction.BoundFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initialisable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.TailCall;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.ExoticBoundFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.2 Function Objects</h2>
 * <ul>
 * <li>19.2.3 Properties of the Function Prototype Object
 * <li>19.2.4 Properties of Function Instances
 * </ul>
 */
public class FunctionPrototype extends BuiltinFunction implements Initialisable {
    private static final int MAX_ARGUMENTS = 0x10000;

    public FunctionPrototype(Realm realm) {
        super(realm, "");
    }

    @Override
    public void initialise(ExecutionContext cx) {
        createProperties(this, cx, Properties.class);
        AddRestrictedFunctionProperties(cx, this);
    }

    /**
     * Returns the number of maximal supported arguments in {@code Function.prototype.apply}
     */
    public static final int getMaxArguments() {
        return MAX_ARGUMENTS;
    }

    /**
     * [[Call]]
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        return UNDEFINED;
    }

    /**
     * 19.2.3 Properties of the Function Prototype Object
     */
    public enum Properties {
        ;

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false,
                configurable = true))
        public static final String name = "";

        /**
         * 19.2.3.4 Function.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Function;

        /**
         * 19.2.3.5 Function.prototype.toString ( )
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            return ((Callable) thisValue).toSource();
        }

        /**
         * 19.2.3.1 Function.prototype.apply (thisArg, argArray)
         */
        @TailCall
        @Function(name = "apply", arity = 2)
        public static Object apply(ExecutionContext cx, Object thisValue, Object thisArg,
                Object argArray) {
            /* step 1 */
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable func = (Callable) thisValue;
            /* step 2 */
            if (Type.isUndefinedOrNull(argArray)) {
                return PrepareForTailCall(EMPTY_ARRAY, thisArg, func);
            }
            /* steps 3-4 */
            Object[] argList = CreateListFromArrayLike(cx, argArray);
            /* step 5 */
            return PrepareForTailCall(argList, thisArg, func);
        }

        /**
         * 19.2.3.3 Function.prototype.call (thisArg [, arg1 [, arg2, ... ]])
         */
        @TailCall
        @Function(name = "call", arity = 1)
        public static Object call(ExecutionContext cx, Object thisValue, Object thisArg,
                Object... args) {
            /* step 1 */
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable func = (Callable) thisValue;
            /* steps 2-4 */
            return PrepareForTailCall(args, thisArg, func);
        }

        /**
         * 19.2.3.2 Function.prototype.bind (thisArg [, arg1 [, arg2, ... ]])
         */
        @Function(name = "bind", arity = 1)
        public static Object bind(ExecutionContext cx, Object thisValue, Object thisArg,
                Object... args) {
            /* step 2 */
            if (!IsCallable(thisValue)) {
                throw throwTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            Callable target = (Callable) thisValue;
            /* steps 3-4 */
            ExoticBoundFunction f = BoundFunctionCreate(cx, target, thisArg, args);
            /* steps 5-6 */
            int l;
            if (target instanceof OrdinaryFunction || target instanceof OrdinaryGenerator
                    || target instanceof BuiltinFunction || target instanceof ExoticBoundFunction) {
                Object targetLen = Get(cx, target, "length");
                l = (int) Math.max(0, ToLength(cx, targetLen) - args.length);
            } else {
                l = 0;
            }
            /* step 7 */
            f.defineOwnProperty(cx, "length", new PropertyDescriptor(l, false, false, false));
            /* step 8 */
            AddRestrictedFunctionProperties(cx, f);
            /* step 9 */
            return f;
        }

        /**
         * 19.2.4.6 Function.prototype[ @@create ] ( )
         */
        @Function(name = "[Symbol.create]", arity = 0, symbol = BuiltinSymbol.create,
                attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static Object create(ExecutionContext cx, Object thisValue) {
            return OrdinaryCreateFromConstructor(cx, thisValue, Intrinsics.ObjectPrototype);
        }

        /**
         * 19.2.4.7 Function.prototype[@@hasInstance] (V)
         */
        @Function(
                name = "[Symbol.hasInstance]",
                arity = 1,
                symbol = BuiltinSymbol.hasInstance,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object hasInstance(ExecutionContext cx, Object thisValue, Object v) {
            return OrdinaryHasInstance(cx, thisValue, v);
        }
    }
}
