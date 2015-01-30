/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Constructor;
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
     *            the constructor object or {@code null} if called from derived constructor
     * @return the tail call trampoline object
     */
    // Called from generated code
    public abstract TailCallInvocation toConstructTailCall(ScriptObject object);

    public static TailCallInvocation newTailCallInvocation(Callable function, Object thisValue,
            Object[] argumentsList) {
        return new CallTailCallInvocation(function, thisValue, argumentsList);
    }

    public static TailCallInvocation newTailCallInvocation(Constructor constructor,
            Constructor newTarget, Object[] argumentsList) {
        return new ConstructTailCallInvocation(constructor, newTarget, argumentsList);
    }

    private static final class CallTailCallInvocation extends TailCallInvocation {
        private final Callable function;
        private final Object thisValue;
        private final Object[] argumentsList;

        CallTailCallInvocation(Callable function, Object thisValue, Object[] argumentsList) {
            this.function = function;
            this.thisValue = thisValue;
            this.argumentsList = argumentsList;
        }

        @Override
        protected Object apply(ExecutionContext callerContext) throws Throwable {
            return function.tailCall(callerContext, thisValue, argumentsList);
        }

        @Override
        public TailCallInvocation toConstructTailCall(ScriptObject object) {
            return new ConstructResultTailCallInvocation(this, object);
        }
    }

    private static final class ConstructTailCallInvocation extends TailCallInvocation {
        private final Constructor constructor;
        private final Constructor newTarget;
        private final Object[] argumentsList;

        ConstructTailCallInvocation(Constructor constructor, Constructor newTarget,
                Object[] argumentsList) {
            this.constructor = constructor;
            this.newTarget = newTarget;
            this.argumentsList = argumentsList;
        }

        @Override
        protected Object apply(ExecutionContext callerContext) throws Throwable {
            return constructor.tailConstruct(callerContext, newTarget, argumentsList);
        }

        @Override
        public TailCallInvocation toConstructTailCall(ScriptObject object) {
            return new ConstructResultTailCallInvocation(this, object);
        }
    }

    private static final class ConstructResultTailCallInvocation extends TailCallInvocation {
        private final TailCallInvocation invocation;
        private final ScriptObject object;

        private ConstructResultTailCallInvocation(TailCallInvocation invocation, ScriptObject object) {
            this.invocation = invocation;
            this.object = object;
        }

        @Override
        protected Object apply(ExecutionContext callerContext) throws Throwable {
            Object result = invocation.apply(callerContext);
            if (result instanceof TailCallInvocation) {
                return ((TailCallInvocation) result).toConstructTailCall(object);
            }
            if (!Type.isObject(result)) {
                result = object;
                if (result == null) {
                    throw Errors.newTypeError(callerContext,
                            Messages.Key.NotObjectTypeFromConstructor);
                }
            }
            return result;
        }

        @Override
        public TailCallInvocation toConstructTailCall(ScriptObject object) {
            return this;
        }
    }

    private static final MethodHandle tailCallTrampolineMH = MethodLookup.findStatic(
            MethodHandles.lookup(), "tailCallTrampoline",
            MethodType.methodType(Object.class, Object.class, ExecutionContext.class));

    private static final MethodHandle tailConstructTrampolineMH = MethodLookup.findStatic(
            MethodHandles.lookup(), "tailConstructTrampoline",
            MethodType.methodType(ScriptObject.class, Object.class, ExecutionContext.class));

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
    private static Object tailCallTrampoline(Object result, ExecutionContext callerContext)
            throws Throwable {
        // tail-call with trampoline
        while (result instanceof TailCallInvocation) {
            result = ((TailCallInvocation) result).apply(callerContext);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static ScriptObject tailConstructTrampoline(Object result,
            ExecutionContext callerContext) throws Throwable {
        // tail-call with trampoline
        while (result instanceof TailCallInvocation) {
            result = ((TailCallInvocation) result).apply(callerContext);
        }
        return (ScriptObject) result;
    }
}
