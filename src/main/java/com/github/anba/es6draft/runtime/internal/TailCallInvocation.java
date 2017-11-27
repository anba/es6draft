/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.FunctionEnvironmentRecord;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Type;

/**
 * Object representing a tail-call invocation.
 */
public abstract class TailCallInvocation {
    private TailCallInvocation() {
    }

    protected abstract Object apply(ExecutionContext callerContext) throws Throwable;

    /**
     * Converts this tail-call invocation into a construct tail-call invocation.
     * 
     * @param object
     *            the this-argument object
     * @return the tail-call trampoline object
     */
    // Called from generated code
    public abstract TailCallInvocation toConstructTailCall(ScriptObject object);

    /**
     * Converts this tail-call invocation into a construct tail-call invocation.
     * 
     * @param envRec
     *            the function environment record
     * @return the tail-call trampoline object
     */
    // Called from generated code
    public abstract TailCallInvocation toConstructTailCall(FunctionEnvironmentRecord envRec);

    /**
     * Creates a new {@link TailCallInvocation} object.
     * 
     * @param function
     *            the function object
     * @param thisValue
     *            the function this-value
     * @param arguments
     *            the arguments list
     * @return the tail-call trampoline object
     */
    public static TailCallInvocation newTailCallInvocation(Callable function, Object thisValue, Object[] arguments) {
        return new CallTailCallInvocation(function, thisValue, arguments);
    }

    private static final class CallTailCallInvocation extends TailCallInvocation {
        private final Callable function;
        private final Object thisValue;
        private final Object[] arguments;

        CallTailCallInvocation(Callable function, Object thisValue, Object[] arguments) {
            this.function = function;
            this.thisValue = thisValue;
            this.arguments = arguments;
        }

        @Override
        protected Object apply(ExecutionContext callerContext) throws Throwable {
            return function.tailCall(callerContext, thisValue, arguments);
        }

        @Override
        public TailCallInvocation toConstructTailCall(ScriptObject object) {
            return new ConstructBaseTailCallInvocation(this, object);
        }

        @Override
        public TailCallInvocation toConstructTailCall(FunctionEnvironmentRecord envRec) {
            return new ConstructDerivedTailCallInvocation(this, envRec);
        }
    }

    private static final class ConstructBaseTailCallInvocation extends TailCallInvocation {
        private final TailCallInvocation invocation;
        private final ScriptObject object;

        ConstructBaseTailCallInvocation(TailCallInvocation invocation, ScriptObject object) {
            this.invocation = invocation;
            this.object = object;
        }

        @Override
        protected Object apply(ExecutionContext callerContext) throws Throwable {
            Object result = invocation.apply(callerContext);
            if (result instanceof TailCallInvocation) {
                return ((TailCallInvocation) result).toConstructTailCall(object);
            }
            if (Type.isObject(result)) {
                return result;
            }
            return object;
        }

        @Override
        public TailCallInvocation toConstructTailCall(ScriptObject object) {
            return this;
        }

        @Override
        public TailCallInvocation toConstructTailCall(FunctionEnvironmentRecord envRec) {
            return this;
        }
    }

    private static final class ConstructDerivedTailCallInvocation extends TailCallInvocation {
        private final TailCallInvocation invocation;
        private final FunctionEnvironmentRecord envRec;

        ConstructDerivedTailCallInvocation(TailCallInvocation invocation, FunctionEnvironmentRecord envRec) {
            this.invocation = invocation;
            this.envRec = envRec;
        }

        @Override
        protected Object apply(ExecutionContext callerContext) throws Throwable {
            Object result = invocation.apply(callerContext);
            if (result instanceof TailCallInvocation) {
                return ((TailCallInvocation) result).toConstructTailCall(envRec);
            }
            if (Type.isObject(result)) {
                return result;
            }
            if (!Type.isUndefined(result)) {
                throw Errors.newTypeError(callerContext, Messages.Key.NotObjectTypeFromConstructor);
            }
            return envRec.getThisBinding(callerContext);
        }

        @Override
        public TailCallInvocation toConstructTailCall(ScriptObject object) {
            return this;
        }

        @Override
        public TailCallInvocation toConstructTailCall(FunctionEnvironmentRecord envRec) {
            return this;
        }
    }

    private static final MethodHandle tailCallTrampolineMH = MethodLookup.findStatic(MethodHandles.lookup(),
            "tailCallTrampoline", MethodType.methodType(Object.class, Object.class, ExecutionContext.class));

    private static final MethodHandle tailConstructTrampolineMH = MethodLookup.findStatic(MethodHandles.lookup(),
            "tailConstructTrampoline", MethodType.methodType(ScriptObject.class, Object.class, ExecutionContext.class));

    /**
     * (Object, ExecutionContext) {@literal ->} Object.
     * 
     * @return the tail call method handler
     */
    public static MethodHandle getTailCallHandler() {
        return tailCallTrampolineMH;
    }

    /**
     * (Object, ExecutionContext) {@literal ->} ScriptObject.
     * 
     * @return the tail call method handler
     */
    public static MethodHandle getTailConstructHandler() {
        return tailConstructTrampolineMH;
    }

    @SuppressWarnings("unused")
    private static Object tailCallTrampoline(Object result, ExecutionContext callerContext) throws Throwable {
        // tail-call with trampoline
        while (result instanceof TailCallInvocation) {
            result = ((TailCallInvocation) result).apply(callerContext);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static ScriptObject tailConstructTrampoline(Object result, ExecutionContext callerContext)
            throws Throwable {
        // tail-call with trampoline
        while (result instanceof TailCallInvocation) {
            result = ((TailCallInvocation) result).apply(callerContext);
        }
        return (ScriptObject) result;
    }
}
