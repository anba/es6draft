/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;

import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.BuiltinBrand;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Types</h1><br>
 * <h2>8.4 Built-in Exotic Object Internal Methods and Data Fields</h2>
 * <ul>
 * <li>8.4.1 Bound Function Exotic Objects
 * </ul>
 */
public class ExoticBoundFunction extends OrdinaryObject implements ScriptObject, Callable,
        Constructor {
    /** [[BoundTargetFunction]] */
    private Callable boundTargetFunction;

    /** [[BoundThis]] */
    private Object boundThis;

    /** [[BoundArguments]] */
    private Object[] boundArguments;

    public ExoticBoundFunction(Realm realm) {
        super(realm);
    }

    /**
     * [[BoundTargetFunction]]
     */
    public Callable getBoundTargetFunction() {
        return boundTargetFunction;
    }

    /**
     * [[BoundThis]]
     */
    public Object getBoundThis() {
        return boundThis;
    }

    /**
     * [[BoundArguments]]
     */
    public Object[] getBoundArguments() {
        return boundArguments;
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
        return "function BoundFunction() { /* native code */ }";
    }

    /**
     * 8.4.1.1 [[Call]]
     */
    @Override
    public Object call(Object thisValue, Object... argumentsList) {
        /* step 1 */
        Object[] boundArgs = boundArguments;
        /* step 2 */
        Object boundThis = this.boundThis;
        /* step 3 */
        Callable target = boundTargetFunction;
        /* step 4 */
        Object[] args = new Object[boundArgs.length + argumentsList.length];
        System.arraycopy(boundArgs, 0, args, 0, boundArgs.length);
        System.arraycopy(argumentsList, 0, args, boundArgs.length, argumentsList.length);
        /* step 5 */
        return target.call(boundThis, args);
    }

    /**
     * 8.4.1.2 [[Construct]]
     */
    @Override
    public Object construct(Object... extraArgs) {
        /* step 1 */
        Callable target = boundTargetFunction;
        /* step 2 */
        if (!(target instanceof Constructor)) {
            throw throwTypeError(realm(), Messages.Key.NotConstructor);
        }
        /* step 3 */
        Object[] boundArgs = boundArguments;
        /* step 4 */
        Object[] args = new Object[boundArgs.length + extraArgs.length];
        System.arraycopy(boundArgs, 0, args, 0, boundArgs.length);
        System.arraycopy(extraArgs, 0, args, boundArgs.length, extraArgs.length);
        /* step 5 */
        return ((Constructor) target).construct(args);
    }

    /**
     * 8.4.1.3 BoundFunctionCreate Abstract Operation
     */
    public static ExoticBoundFunction BoundFunctionCreate(Realm realm, Callable targetFunction,
            Object boundThis, Object[] boundArgs) {
        /* step 1 */
        ScriptObject proto = realm.getIntrinsic(Intrinsics.FunctionPrototype);
        /* step 2-5 (implicit) */
        ExoticBoundFunction obj = new ExoticBoundFunction(realm);
        /* step 6 */
        obj.setPrototype(proto);
        /* step 7 (implicit) */
        /* step 8 */
        obj.boundTargetFunction = targetFunction;
        /* step 9 */
        obj.boundThis = boundThis;
        /* step 10 */
        obj.boundArguments = boundArgs;
        /* step 11 (implicit) */
        /* step 12 */
        return obj;
    }
}
