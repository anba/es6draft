/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorAbstractOperations.AsyncGeneratorStart;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.async.iteration.AsyncGeneratorObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Extension: Async Generator Function Definitions
 */
public final class OrdinaryAsyncGenerator extends FunctionObject {
    /**
     * Constructs a new Async Generator Function object.
     * 
     * @param realm
     *            the realm object
     */
    protected OrdinaryAsyncGenerator(Realm realm) {
        super(realm);
    }

    @Override
    protected OrdinaryAsyncGenerator allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(), getFunctionKind());
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public AsyncGeneratorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return (AsyncGeneratorObject) getCallMethod().invokeExact(this, callerContext, thisValue, args);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public AsyncGeneratorObject tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return (AsyncGeneratorObject) getTailCallMethod().invokeExact(this, callerContext, thisValue, args);
    }

    /**
     * Runtime Semantics: EvaluateBody
     * 
     * @param cx
     *            the execution context
     * @param functionObject
     *            the async generator function object
     * @return the async generator result value
     */
    public static AsyncGeneratorObject EvaluateBody(ExecutionContext cx, OrdinaryAsyncGenerator functionObject) {
        /* steps 1-2 */
        AsyncGeneratorObject gen = OrdinaryCreateFromConstructor(cx, functionObject, Intrinsics.AsyncGeneratorPrototype,
                AsyncGeneratorObject::new);
        /* step 3 */
        AsyncGeneratorStart(cx, gen, functionObject.getCode());
        /* step 4 */
        return gen;
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
     * @return the new async enerator function object
     */
    public static OrdinaryAsyncGenerator FunctionAllocate(ExecutionContext cx, ScriptObject functionPrototype,
            boolean strict, FunctionKind kind) {
        assert kind != FunctionKind.ClassConstructor;
        Realm realm = cx.getRealm();
        /* steps 1-5 (implicit) */
        /* steps 6-9 */
        OrdinaryAsyncGenerator f = new OrdinaryAsyncGenerator(realm);
        /* steps 10-14 */
        f.allocate(realm, functionPrototype, strict, kind, ConstructorKind.Base);
        /* step 15 */
        return f;
    }

    /**
     * 9.2.6 GeneratorFunctionCreate (kind, ParameterList, Body, Scope, Strict)
     * 
     * @param cx
     *            the execution context
     * @param kind
     *            the function kind
     * @param function
     *            the function code
     * @param scope
     *            the lexical environment
     * @return the new async generator function object
     */
    public static OrdinaryAsyncGenerator AsyncGeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope) {
        assert function.isAsync() && function.isGenerator() && kind != FunctionKind.ClassConstructor;
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.AsyncGenerator);
        /* step 2 */
        OrdinaryAsyncGenerator f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
        return f;
    }
}
