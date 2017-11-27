/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public final class OrdinaryConstructorFunction extends FunctionObject implements Constructor {
    /**
     * Constructs a new Constructor Function object.
     * 
     * @param realm
     *            the realm object
     */
    public OrdinaryConstructorFunction(Realm realm) {
        super(realm);
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... argumentsList) {
        try {
            return getCallMethod().invokeExact(this, callerContext, thisValue, argumentsList);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... argumentsList) throws Throwable {
        return getTailCallMethod().invokeExact(this, callerContext, thisValue, argumentsList);
    }

    /**
     * 9.2.2 [[Construct]] (argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget, Object... argumentsList) {
        try {
            return (ScriptObject) getConstructMethod().invokeExact(this, callerContext, newTarget, argumentsList);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.5 FunctionCreate (kind, ParameterList, Body, Scope, Strict)
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @return the new function object
     */
    public static OrdinaryConstructorFunction ConstructorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope) {
        /* step 1 */
        ScriptObject prototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        /* steps 4-5 */
        return ConstructorFunctionCreate(cx, kind, ConstructorKind.Base, function, scope, prototype);
    }

    /**
     * 9.2.5 FunctionCreate (kind, ParameterList, Body, Scope, Strict, prototype)
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param constructorKind
     *            the constructor kind
     * @param scope
     *            the lexical environment
     * @param prototype
     *            the function prototype
     * @return the new function object
     */
    public static OrdinaryConstructorFunction ConstructorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            ConstructorKind constructorKind, RuntimeInfo.Function function, LexicalEnvironment<?> scope,
            ScriptObject prototype) {
        assert !function.isGenerator() && !function.isAsync();
        boolean strict = function.isStrict();
        /* steps 1-3 (not applicable) */
        /* step 4 */
        OrdinaryConstructorFunction f = FunctionAllocate(cx, OrdinaryConstructorFunction::new, prototype, strict, kind,
                constructorKind);
        /* step 5 */
        return FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
    }
}
