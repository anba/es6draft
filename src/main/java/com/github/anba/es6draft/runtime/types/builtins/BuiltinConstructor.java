/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.types.builtins;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.Constructor;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public abstract class BuiltinConstructor extends BuiltinFunction implements Constructor {
    private MethodHandle constructMethod;

    /**
     * Constructs a new built-in constructor function.
     * 
     * @param realm
     *            the realm object
     * @param name
     *            the function name
     * @param arity
     *            the function arity
     */
    protected BuiltinConstructor(Realm realm, String name, int arity) {
        super(realm, name, arity);
    }

    /**
     * Returns `(? extends BuiltinConstructor, ExecutionContext, Constructor, Object[]) {@literal ->} ScriptObject`
     * method-handle.
     * 
     * @return the call method handle
     */
    public MethodHandle getConstructMethod() {
        if (constructMethod == null) {
            try {
                Method method = getClass().getDeclaredMethod("construct", ExecutionContext.class, Constructor.class,
                        Object[].class);
                constructMethod = lookup().unreflect(method);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return constructMethod;
    }
}
