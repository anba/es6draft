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
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.2 ECMAScript Function Objects
 * </ul>
 */
public final class OrdinaryFunction extends FunctionObject {
    /**
     * Constructs a new Function object.
     * 
     * @param realm
     *            the realm object
     */
    public OrdinaryFunction(Realm realm) {
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
     * 9.2.5 FunctionCreate (kind, ParameterList, Body, Scope, Strict, prototype)
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
    public static OrdinaryFunction FunctionCreate(ExecutionContext cx, FunctionKind kind, RuntimeInfo.Function function,
            LexicalEnvironment<?> scope) {
        assert !function.isGenerator() && !function.isAsync();
        boolean strict = function.isStrict();
        /* step 1 */
        ScriptObject functionPrototype = cx.getIntrinsic(Intrinsics.FunctionPrototype);
        /* steps 2-3 (not applicable) */
        /* step 4 */
        OrdinaryFunction f = FunctionAllocate(cx, OrdinaryFunction::new, functionPrototype, strict, kind);
        /* step 5 */
        return FunctionInitialize(f, kind, function, scope, cx.getCurrentExecutable());
    }
}
