/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsConstructor;
import static com.github.anba.es6draft.runtime.internal.Errors.newRangeError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.objects.FunctionPrototype;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1><br>
 * <h2>9.4 Built-in Exotic Object Internal Methods and Slots</h2>
 * <ul>
 * <li>9.4.1 Bound Function Exotic Objects
 * </ul>
 */
public class BoundFunctionObject extends OrdinaryObject implements Callable {
    /** [[BoundTargetFunction]] */
    private Callable boundTargetFunction;
    private Callable flattenedTargetFunction;

    /** [[BoundThis]] */
    private Object boundThis;

    /** [[BoundArguments]] */
    private Object[] boundArguments;

    private static final class ConstructorBoundFunctionObject extends BoundFunctionObject implements Constructor {
        /**
         * Constructs a new Bound Function object.
         * 
         * @param realm
         *            the realm object
         * @param prototype
         *            the prototype object
         */
        ConstructorBoundFunctionObject(Realm realm, ScriptObject prototype) {
            super(realm, prototype);
        }

        /**
         * 9.4.1.2 [[Construct]] (argumentsList, newTarget)
         */
        @Override
        public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... argumentsList) {
            /* step 1 */
            Callable target = getFlattenedTargetFunction();
            /* step 2 */
            assert IsConstructor(target);
            /* step 3 */
            Object[] boundArgs = getBoundArguments();
            /* step 4 */
            Object[] args = concatArguments(callerContext, boundArgs, argumentsList);
            /* step 5 */
            if (this == newTarget) {
                newTarget = (Constructor) target;
            } else if (isFlattened()) {
                newTarget = newTargetFromBound(newTarget);
            }
            /* step 6 */
            return ((Constructor) target).construct(callerContext, newTarget, args);
        }

        private Constructor newTargetFromBound(Constructor newTarget) {
            for (Callable target = getBoundTargetFunction(); target instanceof ConstructorBoundFunctionObject;) {
                ConstructorBoundFunctionObject bound = (ConstructorBoundFunctionObject) target;
                if (bound == newTarget) {
                    return (Constructor) getFlattenedTargetFunction();
                }
                target = bound.getBoundTargetFunction();
            }
            return newTarget;
        }
    }

    /**
     * Constructs a new Bound Function object.
     * 
     * @param realm
     *            the realm object
     * @param prototype
     *            the prototype object
     */
    private BoundFunctionObject(Realm realm, ScriptObject prototype) {
        super(realm, prototype);
    }

    /**
     * [[BoundTargetFunction]]
     * 
     * @return the bound target function
     */
    public final Callable getBoundTargetFunction() {
        return boundTargetFunction;
    }

    /**
     * [[BoundThis]]
     * 
     * @return the bound this-value
     */
    public final Object getBoundThis() {
        return boundThis;
    }

    /**
     * [[BoundArguments]]
     * 
     * @return the bound function arguments
     */
    public final Object[] getBoundArguments() {
        return boundArguments;
    }

    protected final Callable getFlattenedTargetFunction() {
        return flattenedTargetFunction;
    }

    protected final boolean isFlattened() {
        return boundTargetFunction != flattenedTargetFunction;
    }

    @Override
    public final String toSource(ExecutionContext cx) {
        return FunctionSource.nativeCode("BoundFunction");
    }

    /**
     * 9.4.1.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public final Object call(ExecutionContext callerContext, Object thisValue, Object... argumentsList) {
        /* step 1 */
        Callable target = getFlattenedTargetFunction();
        /* step 2 */
        Object boundThis = getBoundThis();
        /* step 3 */
        Object[] boundArgs = getBoundArguments();
        /* step 4 */
        Object[] args = concatArguments(callerContext, boundArgs, argumentsList);
        /* step 5 */
        return target.call(callerContext, boundThis, args);
    }

    /**
     * 9.4.1.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public final Object tailCall(ExecutionContext callerContext, Object thisValue, Object... argumentsList)
            throws Throwable {
        /* step 1 */
        Callable target = getFlattenedTargetFunction();
        /* step 2 */
        Object boundThis = getBoundThis();
        /* step 3 */
        Object[] boundArgs = getBoundArguments();
        /* step 4 */
        Object[] args = concatArguments(callerContext, boundArgs, argumentsList);
        /* step 5 */
        return target.tailCall(callerContext, boundThis, args);
    }

    @Override
    public final Realm getRealm(ExecutionContext cx) {
        /* 7.3.22 GetFunctionRealm ( obj ) */
        return getFlattenedTargetFunction().getRealm(cx);
    }

    /**
     * 9.4.1.3 BoundFunctionCreate (targetFunction, boundThis, boundArgs)
     * 
     * @param cx
     *            the execution context
     * @param targetFunction
     *            the target function
     * @param boundThis
     *            the bound this-value
     * @param boundArgs
     *            the bound function arguments
     * @return the new bound function object
     */
    public static BoundFunctionObject BoundFunctionCreate(ExecutionContext cx, Callable targetFunction,
            Object boundThis, Object... boundArgs) {
        /* step 1 (not applicable) */
        /* step 2 */
        ScriptObject proto = targetFunction.getPrototypeOf(cx);
        /* steps 3-8 */
        BoundFunctionObject obj;
        if (IsConstructor(targetFunction)) {
            obj = new ConstructorBoundFunctionObject(cx.getRealm(), proto);
        } else {
            obj = new BoundFunctionObject(cx.getRealm(), proto);
        }
        // Flatten chain of bound function objects.
        if (targetFunction instanceof BoundFunctionObject) {
            BoundFunctionObject target = (BoundFunctionObject) targetFunction;
            /* step 9 */
            obj.boundTargetFunction = target;
            obj.flattenedTargetFunction = target.flattenedTargetFunction;
            /* step 10 */
            obj.boundThis = target.boundThis;
            /* step 11 */
            obj.boundArguments = concatArguments(cx, target.boundArguments, intern(boundArgs));
        } else {
            /* step 9 */
            obj.boundTargetFunction = targetFunction;
            obj.flattenedTargetFunction = targetFunction;
            /* step 10 */
            obj.boundThis = boundThis;
            /* step 11 */
            obj.boundArguments = intern(boundArgs);
        }
        /* step 12 */
        return obj;
    }

    private static Object[] concatArguments(ExecutionContext cx, Object[] boundArgs, Object[] argumentsList) {
        int argsLen = boundArgs.length + argumentsList.length;
        if (argsLen > FunctionPrototype.getMaxArguments()) {
            throw newRangeError(cx, Messages.Key.FunctionTooManyArguments);
        }
        if (boundArgs.length == 0) {
            return argumentsList;
        }
        if (argumentsList.length == 0) {
            return boundArgs;
        }
        Object[] args = new Object[argsLen];
        System.arraycopy(boundArgs, 0, args, 0, boundArgs.length);
        System.arraycopy(argumentsList, 0, args, boundArgs.length, argumentsList.length);
        return args;
    }

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private static Object[] intern(Object[] arguments) {
        return arguments.length > 0 ? arguments : EMPTY_ARRAY;
    }
}
