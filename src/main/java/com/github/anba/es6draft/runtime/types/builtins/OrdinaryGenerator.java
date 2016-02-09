/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorStart;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.FunctionInitialize;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public final class OrdinaryGenerator extends FunctionObject implements Constructor {
    /**
     * Constructs a new Generator Function object.
     * 
     * @param realm
     *            the realm object
     */
    protected OrdinaryGenerator(Realm realm) {
        super(realm);
    }

    @Override
    protected OrdinaryGenerator allocateNew() {
        return FunctionAllocate(getRealm().defaultContext(), getPrototype(), isStrict(), getFunctionKind());
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public GeneratorObject call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return (GeneratorObject) getCallMethod().invokeExact(this, callerContext, thisValue, args);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.2.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public GeneratorObject tailCall(ExecutionContext callerContext, Object thisValue, Object... args) throws Throwable {
        return (GeneratorObject) getTailCallMethod().invokeExact(this, callerContext, thisValue, args);
    }

    /**
     * 9.2.2 [[Construct]] ( argumentsList, newTarget)
     */
    @Override
    public GeneratorObject construct(ExecutionContext callerContext, Constructor newTarget, Object... argumentsList) {
        try {
            return (GeneratorObject) getConstructMethod().invokeExact(this, callerContext, newTarget, argumentsList);
        } catch (Throwable e) {
            throw FunctionObject.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.14 Runtime Semantics: EvaluateBody
     * 
     * <pre>
     * GeneratorBody : FunctionBody
     * </pre>
     * 
     * @param cx
     *            the execution context
     * @param functionObject
     *            the generator function object
     * @return the generator result value
     */
    public static GeneratorObject EvaluateBody(ExecutionContext cx, OrdinaryGenerator functionObject) {
        /* steps 1-2 */
        GeneratorObject gen = OrdinaryCreateFromConstructor(cx, functionObject, Intrinsics.GeneratorPrototype,
                GeneratorObject::new);
        /* step 3 */
        GeneratorStart(cx, gen, functionObject.getCode());
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
     * @return the new generator function object
     */
    public static OrdinaryGenerator FunctionAllocate(ExecutionContext cx, ScriptObject functionPrototype,
            boolean strict, FunctionKind kind) {
        assert kind != FunctionKind.ClassConstructor;
        Realm realm = cx.getRealm();
        /* steps 1-5 (implicit) */
        /* steps 6-9 */
        OrdinaryGenerator f = new OrdinaryGenerator(realm);
        /* steps 10-14 */
        f.allocate(realm, functionPrototype, strict, kind, ConstructorKind.Derived);
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
     * @return the new generator function object
     */
    public static OrdinaryGenerator GeneratorFunctionCreate(ExecutionContext cx, FunctionKind kind,
            RuntimeInfo.Function function, LexicalEnvironment<?> scope) {
        assert function.isGenerator() && kind != FunctionKind.ClassConstructor;
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.Generator);
        /* step 2 */
        OrdinaryGenerator f = FunctionAllocate(cx, functionPrototype, function.isStrict(), kind);
        /* step 3 */
        FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
        return f;
    }
}
