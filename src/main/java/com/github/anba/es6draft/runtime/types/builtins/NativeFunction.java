/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.lang.invoke.MethodHandle;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public final class NativeFunction extends BuiltinFunction {
    // (Object, Object[]) -> Object
    private final MethodHandle mh;

    private final NativeFunctionId id;

    public enum NativeFunctionId {
        None, RegExpPrototypeExec
    }

    public NativeFunction(Realm realm, String name, int arity, MethodHandle mh) {
        super(realm, name, arity);
        this.mh = mh;
        this.id = NativeFunctionId.None;
    }

    public NativeFunction(Realm realm, String name, int arity, NativeFunctionId id, MethodHandle mh) {
        super(realm, name, arity);
        this.mh = mh;
        this.id = id;
    }

    /**
     * Returns `(Object, Object[]) -> Object` method-handle
     */
    public MethodHandle getCallMethod() {
        return mh;
    }

    /**
     * Returns the id for this native function
     */
    public final NativeFunctionId getId() {
        return id;
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return mh.invokeExact(thisValue, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
