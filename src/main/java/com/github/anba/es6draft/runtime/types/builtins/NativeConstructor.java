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
import com.github.anba.es6draft.runtime.types.Creatable;
import com.github.anba.es6draft.runtime.types.CreateAction;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public final class NativeConstructor extends BuiltinConstructor implements Creatable<ScriptObject> {
    /** [[CreateAction]] */
    private final CreateAction<?> createAction;
    // (ExecutionContext, Object, Object[]) -> Object
    private final MethodHandle mh;

    /**
     * Constructs a new native constructor function.
     * 
     * @param realm
     *            the realm object
     * @param name
     *            the function name
     * @param arity
     *            the function arity
     * @param createAction
     *            the create action operation
     * @param mh
     *            the method handle to the function code
     */
    public NativeConstructor(Realm realm, String name, int arity, CreateAction<?> createAction,
            MethodHandle mh) {
        this(realm, name, createAction, mh);
        createDefaultFunctionProperties(name, arity);
    }

    private NativeConstructor(Realm realm, String name, CreateAction<?> createAction,
            MethodHandle mh) {
        super(realm, name);
        this.createAction = createAction;
        this.mh = mh;
    }

    @Override
    public NativeConstructor clone() {
        return new NativeConstructor(getRealm(), getName(), createAction, mh);
    }

    @Override
    public CreateAction<?> createAction() {
        return createAction;
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
            return mh.invokeExact(callerContext, thisValue, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ScriptObject construct(ExecutionContext callerContext, Object... args) {
        return Construct(callerContext, this, args);
    }
}
