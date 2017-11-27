/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorStart;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public final class OrdinaryGenerator extends FunctionObject {
    /**
     * Constructs a new Generator Function object.
     * 
     * @param realm
     *            the realm object
     */
    public OrdinaryGenerator(Realm realm) {
        super(realm);
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
     * 14.4 Generator Function Definitions
     * <p>
     * 14.4.11 Runtime Semantics: EvaluateBody
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
        /* step 1 */
        GeneratorObject gen = OrdinaryCreateFromConstructor(cx, functionObject, Intrinsics.GeneratorPrototype,
                GeneratorObject::new);
        /* step 2 */
        GeneratorStart(cx, gen, functionObject.getCode());
        /* step 3 */
        return gen;
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
        boolean strict = function.isStrict();
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.Generator);
        /* step 2 */
        OrdinaryGenerator f = FunctionAllocate(cx, OrdinaryGenerator::new, functionPrototype, strict, kind);
        /* step 3 */
        return FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
    }
}
