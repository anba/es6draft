/**
 * Copyright (c) 2012-2015 André Bargull
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
    // (ExecutionContext, Object, Object[]) -> Object
    private final MethodHandle mh;

    private final Class<?> id;

    /**
     * Constructs a new native function.
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
    public NativeFunction(Realm realm, String name, int arity, MethodHandle mh) {
        this(realm, name, arity, null, mh);
    }

    /**
     * Constructs a new native function.
     * 
     * @param realm
     *            the realm object
     * @param name
     *            the function name
     * @param arity
     *            the function arity
     * @param id
     *            the native function identifier
     * @param mh
     *            the method handle to the function code
     */
    public NativeFunction(Realm realm, String name, int arity, Class<?> id, MethodHandle mh) {
        super(realm, name, arity);
        this.mh = mh;
        this.id = id;
        createDefaultFunctionProperties();
    }

    private NativeFunction(NativeFunction original) {
        super(original.getRealm(), original.getName(), original.getArity());
        this.mh = original.mh;
        this.id = original.id;
    }

    @Override
    public NativeFunction clone() {
        return new NativeFunction(this);
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
     * Returns the optional identifier for this native function.
     * 
     * @return the native function id or {@code null} if not available
     */
    public Class<?> getId() {
        return id;
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return mh.invokeExact(callerContext, thisValue, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
