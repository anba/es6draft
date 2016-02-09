/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.objects.async.AsyncAbstractOperations.AsyncFunctionStart;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Extension: Async Function Definitions
 */
public final class OrdinaryAsyncFunction extends FunctionObject {
    /**
     * Constructs a new Async Function object.
     * 
     * @param realm
     *            the realm object
     */
    protected OrdinaryAsyncFunction(Realm realm) {
        super(realm);
    }

    @Override
    protected OrdinaryAsyncFunction allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(), getFunctionKind());
    }

    /**
     * 9.2.1 [[Call]] ( thisArgument, argumentsList)
     */
    @Override
    public PromiseObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return (PromiseObject) getCallMethod().invokeExact(this, callerContext, thisValue, args);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.1 [[Call]] ( thisArgument, argumentsList)
     */
    @Override
    public PromiseObject tailCall(ExecutionContext callerContext, Object thisValue, Object... args) throws Throwable {
        return (PromiseObject) getTailCallMethod().invokeExact(this, callerContext, thisValue, args);
    }

    /**
     * [Called from generated code]
     * 
     * @param cx
     *            the execution context
     * @param functionObject
     *            the async function object
     * @return the function promise result value
     */
    public static PromiseObject EvaluateBody(ExecutionContext cx, OrdinaryAsyncFunction functionObject) {
        /* step 1 */
        PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
        /* steps 2-3 */
        AsyncFunctionStart(cx, promiseCapability, functionObject.getCode());
        /* step 4 */
        return promiseCapability.getPromise();
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.3 FunctionAllocate (functionPrototype, strict [,functionKind] )
     * 
     * @param cx
     *            the execution context
     * @param functionPrototype
     *            the function prototype
     * @param strict
     *            the strict mode flag
     * @param kind
     *            the function kind
     * @return the new async function object
     */
    public static OrdinaryAsyncFunction FunctionAllocate(ExecutionContext cx, ScriptObject functionPrototype,
            boolean strict, FunctionKind kind) {
        assert kind != FunctionKind.ClassConstructor;
        Realm realm = cx.getRealm();
        /* steps 1-5 (implicit) */
        /* steps 6-9 */
        OrdinaryAsyncFunction f = new OrdinaryAsyncFunction(realm);
        /* steps 10-14 */
        f.allocate(realm, functionPrototype, strict, kind, ConstructorKind.Base);
        /* step 15 */
        return f;
    }

    /**
     * AsyncFunctionCreate (kind, ParameterList, Body, Scope, Strict)
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @return the new async function object
     */
    public static OrdinaryAsyncFunction AsyncFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope) {
        assert function.isAsync() && kind != FunctionKind.ClassConstructor;
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.AsyncFunctionPrototype);
        /* step 2 */
        OrdinaryAsyncFunction f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
        return f;
    }
}
