/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.EMPTY_ARRAY;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.PrepareForTailCall;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject.BoundFunctionClone;
import static com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject.BoundFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.CloneMethod;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.SetFunctionName;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Initializable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Properties.Prototype;
import com.github.anba.es6draft.runtime.internal.Properties.TailCall;
import com.github.anba.es6draft.runtime.internal.Properties.Value;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.Property;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.2 Function Objects</h2>
 * <ul>
 * <li>19.2.3 Properties of the Function Prototype Object
 * <li>19.2.4 Properties of Function Instances
 * </ul>
 */
public final class FunctionPrototype extends BuiltinFunction implements Initializable {
    private static final int MAX_ARGUMENTS = 0x10000;

    /**
     * Constructs a new Function prototype object.
     * 
     * @param realm
     *            the realm object
     */
    public FunctionPrototype(Realm realm) {
        super(realm, "", 0);
    }

    @Override
    public void initialize(Realm realm) {
        createProperties(realm, this, Properties.class);
    }

    @Override
    public FunctionPrototype clone() {
        return new FunctionPrototype(getRealm());
    }

    /**
     * Returns the number of maximal supported arguments in {@code Function.prototype.apply}.
     * 
     * @return the maximum number of supported arguments
     */
    public static final int getMaxArguments() {
        return MAX_ARGUMENTS;
    }

    /**
     * [[Call]]
     */
    @Override
    public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
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
         * 19.2.3.6 Function.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            if (!IsCallable(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            return ((Callable) thisValue).toSource(SourceSelector.Function);
        }

        /**
         * 19.2.3.1 Function.prototype.apply (thisArg, argArray)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param thisArg
         *            the this-argument
         * @param argArray
         *            the arguments array
         * @return the function invocation result
         */
        @TailCall
        @Function(name = "apply", arity = 2)
        public static Object apply(ExecutionContext cx, Object thisValue, Object thisArg,
                Object argArray) {
            /* step 1 */
            if (!IsCallable(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable func = (Callable) thisValue;
            /* step 2 */
            if (Type.isUndefinedOrNull(argArray)) {
                return PrepareForTailCall(func, thisArg, EMPTY_ARRAY);
            }
            /* steps 3-4 */
            Object[] argList = CreateListFromArrayLike(cx, argArray);
            /* steps 5-6 */
            return PrepareForTailCall(func, thisArg, argList);
        }

        /**
         * 19.2.3.3 Function.prototype.call (thisArg, ...args)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param thisArg
         *            the this-argument
         * @param args
         *            the arguments array
         * @return the function invocation result
         */
        @TailCall
        @Function(name = "call", arity = 1)
        public static Object call(ExecutionContext cx, Object thisValue, Object thisArg,
                Object... args) {
            /* step 1 */
            if (!IsCallable(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable func = (Callable) thisValue;
            /* steps 2-5 */
            return PrepareForTailCall(func, thisArg, args);
        }

        /**
         * 19.2.3.2 Function.prototype.bind (thisArg, ...args)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param thisArg
         *            the this-argument
         * @param args
         *            the arguments array
         * @return the bound function object
         */
        @Function(name = "bind", arity = 1)
        public static Object bind(ExecutionContext cx, Object thisValue, Object thisArg,
                Object... args) {
            /* step 2 */
            if (!IsCallable(thisValue)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            /* step 1 */
            Callable target = (Callable) thisValue;
            /* steps 3-4 */
            BoundFunctionObject f = BoundFunctionCreate(cx, target, thisArg, args);
            /* steps 5-6 */
            boolean targetHasLength = HasOwnProperty(cx, target, "length");
            /* steps 7-8 */
            double l = 0;
            if (targetHasLength) {
                Object targetLen = Get(cx, target, "length");
                if (Type.isNumber(targetLen)) {
                    double intLength = ToInteger(Type.numberValue(targetLen));
                    l = Math.max(0, intLength - args.length);
                }
            }
            /* steps 9-10 */
            f.infallibleDefineOwnProperty("length", new Property(l, false, false, true));
            /* steps 11-12 */
            Object targetName = Get(cx, target, "name");
            /* step 13 */
            if (!Type.isString(targetName)) {
                targetName = "";
            }
            /* steps 14-15 */
            SetFunctionName(f, Type.stringValue(targetName).toString(), "bound");
            /* step 16 */
            return f;
        }

        /**
         * 19.2.3.5 Function.prototype.toMethod (newHome)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param newHome
         *            the new home object
         * @return the new function object
         */
        @Function(name = "toMethod", arity = 1)
        public static Object toMethod(ExecutionContext cx, Object thisValue, Object newHome) {
            /* step 1 */
            if (!(thisValue instanceof Callable)) {
                throw newTypeError(cx, Messages.Key.IncompatibleObject);
            }
            Callable func = (Callable) thisValue;
            /* step 2 */
            if (!Type.isObject(newHome)) {
                throw newTypeError(cx, Messages.Key.NotObjectType);
            }
            ScriptObject newHomeObject = Type.objectValue(newHome);
            /* step 3 */
            if (func instanceof FunctionObject) {
                return CloneMethod(cx, (FunctionObject) func, newHomeObject);
            }
            if (func instanceof BuiltinFunction) {
                return CloneMethod(cx, (BuiltinFunction) func);
            }
            /* step 4 */
            if (func instanceof BoundFunctionObject) {
                return BoundFunctionClone(cx, (BoundFunctionObject) func);
            }
            /* steps 5-6 */
            return func.clone(cx);
        }

        /**
         * 19.2.3.7 Function.prototype[@@hasInstance] (V)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param v
         *            the value
         * @return {@code true} if the value is an instance of this function
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
