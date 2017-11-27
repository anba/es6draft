/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.runtime.language.CallOperations.PrepareForTailCall;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject.BoundFunctionCreate;
import static com.github.anba.es6draft.runtime.types.builtins.FunctionObject.SetFunctionName;

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
import com.github.anba.es6draft.runtime.types.Type;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BoundFunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

/**
 * <h1>19 Fundamental Objects</h1><br>
 * <h2>19.2 Function Objects</h2>
 * <ul>
 * <li>19.2.3 Properties of the Function Prototype Object
 * <li>19.2.4 Properties of Function Instances
 * </ul>
 */
public final class FunctionPrototype extends BuiltinFunction implements Initializable {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final int MAX_ARGUMENTS = 0x1ffff;

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

    /**
     * Returns the number of maximal supported arguments in {@code Function.prototype.apply}.
     * 
     * @return the maximum number of supported arguments
     */
    public static int getMaxArguments() {
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

        private static Callable thisFunctionObject(ExecutionContext cx, Object value, String method) {
            if (value instanceof Callable) {
                return (Callable) value;
            }
            throw newTypeError(cx, Messages.Key.IncompatibleThis, method, Type.of(value).toString());
        }

        @Prototype
        public static final Intrinsics __proto__ = Intrinsics.ObjectPrototype;

        @Value(name = "length", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final int length = 0;

        @Value(name = "name", attributes = @Attributes(writable = false, enumerable = false, configurable = true))
        public static final String name = "";

        /**
         * 19.2.3.4 Function.prototype.constructor
         */
        @Value(name = "constructor")
        public static final Intrinsics constructor = Intrinsics.Function;

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
        public static Object apply(ExecutionContext cx, Object thisValue, Object thisArg, Object argArray) {
            /* step 1 */
            Callable func = thisFunctionObject(cx, thisValue, "Function.prototype.apply");
            /* step 2 */
            if (Type.isUndefinedOrNull(argArray)) {
                return PrepareForTailCall(func, thisArg, EMPTY_ARRAY);
            }
            /* step 3 */
            Object[] argList = CreateListFromArrayLike(cx, argArray);
            /* steps 4-5 */
            return PrepareForTailCall(func, thisArg, argList);
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
        public static Object bind(ExecutionContext cx, Object thisValue, Object thisArg, Object... args) {
            /* steps 1-2 */
            Callable target = thisFunctionObject(cx, thisValue, "Function.prototype.bind");
            /* step 3 (not applicable) */
            /* step 4 */
            BoundFunctionObject f = BoundFunctionCreate(cx, target, thisArg, args);
            /* step 5 */
            boolean targetHasLength = HasOwnProperty(cx, target, "length");
            /* steps 6-7 */
            double l = 0;
            if (targetHasLength) {
                Object targetLen = Get(cx, target, "length");
                if (Type.isNumber(targetLen)) {
                    double intLength = ToInteger(Type.numberValue(targetLen));
                    l = Math.max(0, intLength - args.length);
                }
            }
            /* step 8 */
            f.infallibleDefineOwnProperty("length", new Property(l, false, false, true));
            /* step 9 */
            Object targetName = Get(cx, target, "name");
            /* step 10 */
            String name = Type.isString(targetName) ? Type.stringValue(targetName).toString() : "";
            StringObject.validateLength(cx, name.length() + 5);
            /* step 11 */
            SetFunctionName(f, name, "bound");
            /* step 12 */
            return f;
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
        public static Object call(ExecutionContext cx, Object thisValue, Object thisArg, Object... args) {
            /* step 1 */
            Callable func = thisFunctionObject(cx, thisValue, "Function.prototype.call");
            /* steps 2-5 */
            return PrepareForTailCall(func, thisArg, args);
        }

        /**
         * 19.2.3.5 Function.prototype.toString ( )
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @return the string representation
         */
        @Function(name = "toString", arity = 0)
        public static Object toString(ExecutionContext cx, Object thisValue) {
            /* steps 1-3 */
            return thisFunctionObject(cx, thisValue, "Function.prototype.toString").toSource(cx);
        }

        /**
         * 19.2.3.6 Function.prototype[@@hasInstance] (V)
         * 
         * @param cx
         *            the execution context
         * @param thisValue
         *            the function this-value
         * @param v
         *            the value
         * @return {@code true} if the value is an instance of this function
         */
        @Function(name = "[Symbol.hasInstance]", arity = 1, symbol = BuiltinSymbol.hasInstance,
                attributes = @Attributes(writable = false, enumerable = false, configurable = false))
        public static Object hasInstance(ExecutionContext cx, Object thisValue, Object v) {
            /* steps 1-2 */
            return OrdinaryHasInstance(cx, thisValue, v);
        }
    }
}
