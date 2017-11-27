/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.objects.async.AsyncAbstractOperations.AsyncFunctionStart;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;

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
    public OrdinaryAsyncFunction(Realm realm) {
        super(realm);
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

    // FIXME: spec issue - move into 9.2. similar to GeneratorFunctionCreate
    /**
     * 25.5.5.1 AsyncFunctionCreate ( kind, parameters, body, Scope, Strict )
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
        boolean strict = function.isStrict();
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.AsyncFunctionPrototype);
        /* step 2 */
        OrdinaryAsyncFunction f = FunctionAllocate(cx, OrdinaryAsyncFunction::new, functionPrototype, strict, kind);
        /* step 3 */
        return FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
    }
}
