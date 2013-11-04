/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.lang.invoke.MethodHandle;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.TailCallInvocation;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public class NativeTailCallFunction extends BuiltinFunction {
    // (Object, Object[]) -> Object
    private final MethodHandle mh;

    public NativeTailCallFunction(Realm realm, String name, int arity, MethodHandle mh) {
        super(realm, name, arity);
        this.mh = mh;
    }

    /**
     * [[Call]]
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            Object result = mh.invokeExact(thisValue, args);
            // tail-call with trampoline
            while (result instanceof TailCallInvocation) {
                TailCallInvocation tc = (TailCallInvocation) result;
                result = tc.getFunction().tailCall(callerContext, tc.getThisValue(),
                        tc.getArgumentsList());
            }
            return result;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * [[Call]]
     */
    @Override
    public Object tailCall(ExecutionContext callerContext, Object thisValue, Object... args)
            throws Throwable {
        return mh.invokeExact(thisValue, args);
    }
}
