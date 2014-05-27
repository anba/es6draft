/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;
import static com.github.anba.es6draft.runtime.objects.async.AsyncAbstractOperations.Spawn;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Extension: Async Function Definitions
 */
public final class OrdinaryAsyncFunction extends FunctionObject implements Constructor {
    protected OrdinaryAsyncFunction(Realm realm) {
        super(realm);
    }

    @Override
    public final boolean isConstructor() {
        return super.isConstructor();
    }

    @Override
    protected OrdinaryAsyncFunction allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(),
                getFunctionKind());
    }

    /**
     * 9.2.1 [[Construct]] (argumentsList)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }

    /**
     * 9.2.1 [[Construct]] (argumentsList)
     */
    @Override
    public ScriptObject tailConstruct(ExecutionContext callerContext, Object... args)
            throws Throwable {
        return construct(callerContext, args);
    }

    /**
     * 9.2.4 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return getCallMethod().invokeExact(this, callerContext, thisValue, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 9.2.4 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return getTailCallMethod().invokeExact(this, callerContext, thisValue, args);
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
    public static PromiseObject EvaluateBody(ExecutionContext cx,
            OrdinaryAsyncFunction functionObject) {
        return Spawn(cx, functionObject);
    }

    /* ***************************************************************************************** */

    /**
     * 9.2.3 FunctionAllocate Abstract Operation
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
    public static OrdinaryAsyncFunction FunctionAllocate(ExecutionContext cx,
            ScriptObject functionPrototype, boolean strict, FunctionKind kind) {
        assert kind != FunctionKind.ConstructorMethod && kind != FunctionKind.Arrow;
        Realm realm = cx.getRealm();
        /* steps 1-3 (implicit) */
        /* steps 4-8 */
        OrdinaryAsyncFunction f = new OrdinaryAsyncFunction(realm);
        /* steps 9-13 */
        f.allocate(realm, functionPrototype, strict, kind, uninitializedAsyncFunctionMH);
        /* step 14 */
        return f;
    }

    /**
     * AsyncFunctionCreate Abstract Operation
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
        return AsyncFunctionCreate(cx, kind, function, scope, null);
    }

    /**
     * AsyncFunctionCreate Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @param functionPrototype
     *            the function prototype
     * @return the new async function object
     */
    public static OrdinaryAsyncFunction AsyncFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope,
            ScriptObject functionPrototype) {
        assert function.isAsync() && kind != FunctionKind.ConstructorMethod
                && kind != FunctionKind.Arrow;
        /* step 1 */
        if (functionPrototype == null) {
            functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        }
        /* step 2 */
        OrdinaryAsyncFunction f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        return FunctionInitialize(f, kind, function.isStrict(), function, scope);
    }
}
