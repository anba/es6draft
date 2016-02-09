/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public final class NativeTailCallFunction extends BuiltinFunction {
    // (ExecutionContext, ExecutionContext, Object, Object[]) -> Object
    private final MethodHandle mh;

    // (ExecutionContext, ExecutionContext, Object, Object[]) -> Object
    private final MethodHandle tmh;

    /**
     * Constructs a new native tail-calling function.
     * 
     * @param realm
     *            the realm object
     * @param name
     *            the function name
     * @param arity
     *            the function arity
     * @param mh
     *            the method handle to the function code
     */
    public NativeTailCallFunction(Realm realm, String name, int arity, MethodHandle mh) {
        super(realm, name, arity);
        this.mh = tailCallAdapter(mh);
        this.tmh = mh;
        createDefaultFunctionProperties();
    }

    private NativeTailCallFunction(NativeTailCallFunction original) {
        super(original.getRealm(), original.getName(), original.getArity());
        this.mh = original.mh;
        this.tmh = original.tmh;
    }

    @Override
    public NativeTailCallFunction clone() {
        return new NativeTailCallFunction(this);
    }

    @Override
    public MethodHandle getCallMethod() {
        MethodHandle mh = MethodHandles.insertArguments(this.mh, 0, getRealm().defaultContext());
        return MethodHandles.dropArguments(mh, 0, NativeTailCallFunction.class);
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return mh.invokeExact(getRealm().defaultContext(), callerContext, thisValue, args);
        } catch (Throwable e) {
            throw NativeTailCallFunction.<RuntimeException> rethrow(e);
        }
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args) throws Throwable {
        return tmh.invokeExact(getRealm().defaultContext(), callerContext, thisValue, args);
    }

    private static MethodHandle tailCallAdapter(MethodHandle mh) {
        MethodHandle result = TailCallInvocation.getTailCallHandler();
        result = MethodHandles.dropArguments(result, 2, Object.class, Object[].class);
        result = MethodHandles.dropArguments(result, 1, ExecutionContext.class);
        result = MethodHandles.foldArguments(result, mh);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }
}
