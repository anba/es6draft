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
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Constructor;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * <h1>9 Ordinary and Exotic Objects Behaviours</h1>
 * <ul>
 * <li>9.3 Built-in Function Objects
 * </ul>
 */
public final class NativeConstructor extends BuiltinConstructor {
    // (ExecutionContext, Object, Object[]) -> Object
    private final MethodHandle callMethod;
    // (ExecutionContext, Constructor, Object[]) -> Object
    private final MethodHandle constructMethod;

    /**
     * Constructs a new native constructor function.
     * 
     * @param realm
     *            the realm object
     * @param name
     *            the function name
     * @param arity
     *            the function arity
     * @param callMethod
     *            the method handle to the function call code
     * @param constructMethod
     *            the method handle to the function construct code
     */
    public NativeConstructor(Realm realm, String name, int arity, MethodHandle callMethod,
            MethodHandle constructMethod) {
        super(realm, name, arity);
        this.callMethod = callMethod;
        this.constructMethod = constructMethod;
        createDefaultFunctionProperties();
    }

    private NativeConstructor(NativeConstructor original) {
        super(original.getRealm(), original.getName(), original.getArity());
        this.callMethod = original.callMethod;
        this.constructMethod = original.constructMethod;
    }

    @Override
    public NativeConstructor clone() {
        return new NativeConstructor(this);
    }

    /**
     * Returns `(Object, Object[]) {@literal ->} Object` method-handle.
     * 
     * @return the call method handle
     */
    public MethodHandle getCallMethod() {
        return callMethod;
    }

    /**
     * 9.3.1 [[Call]] (thisArgument, argumentsList)
     */
    @Override
    public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
        try {
            return callMethod.invokeExact(callerContext, thisValue, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 9.3.2 [[Construct]] (argumentsList, newTarget)
     */
    @Override
    public ScriptObject construct(ExecutionContext callerContext, Constructor newTarget,
            Object... args) {
        Object result;
        try {
            result = constructMethod.invokeExact(callerContext, newTarget, args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        if (Type.isObject(result)) {
            return Type.objectValue(result);
        }
        throw Errors.newTypeError(callerContext, Messages.Key.NotObjectType);
    }
}
