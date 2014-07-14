/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import static com.github.anba.es6draft.runtime.AbstractOperations.Construct;

import java.lang.invoke.MethodHandle;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public final class NativeConstructor extends BuiltinConstructor {
    // (Object, Object[]) -> Object
    private final MethodHandle mh;

    public NativeConstructor(Realm realm, String name, int arity, MethodHandle mh) {
        this(realm, name, mh);
        createDefaultFunctionProperties(name, arity);
    }

    private NativeConstructor(Realm realm, String name, MethodHandle mh) {
        super(realm, name);
        this.mh = mh;
    }

    /**
     * Returns `(Object, Object[]) {@literal ->} Object` method-handle.
     * 
     * @return the call method handle
     */
    public MethodHandle getCallMethod() {
        return mh;
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

    @Override
    public NativeConstructor clone() {
        return new NativeConstructor(getRealm(), getName(), mh);
    }

    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }
}
