/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async;

import static com.github.anba.es6draft.runtime.AbstractOperations.*;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorResume;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorStart;
import static com.github.anba.es6draft.runtime.objects.iteration.GeneratorAbstractOperations.GeneratorThrow;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorObject;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryAsyncFunction;

/**
 * Extension: Async Function Definitions
 */
public final class AsyncAbstractOperations {
    private AsyncAbstractOperations() {
    }

    /**
     * Spawn Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param functionObject
     *            the async function object
     * @return the new promise object
     */
    public static PromiseObject Spawn(ExecutionContext cx, OrdinaryAsyncFunction functionObject) {
        GeneratorObject generator = ObjectCreate(cx,
                cx.getIntrinsic(Intrinsics.GeneratorPrototype), GeneratorObjectAllocator.INSTANCE);
        GeneratorStart(cx, generator, functionObject.getCode());
        Callable executor = new SpawnExecutor(cx.getRealm(), generator);
        return PromiseNew(cx, executor);
    }

    private static final class GeneratorObjectAllocator implements ObjectAllocator<GeneratorObject> {
        static final ObjectAllocator<GeneratorObject> INSTANCE = new GeneratorObjectAllocator();

        @Override
        public GeneratorObject newInstance(Realm realm) {
            return new GeneratorObject(realm);
        }
    }

    /**
     * Spawn Executor Functions
     */
    private static final class SpawnExecutor extends BuiltinFunction {
        private final GeneratorObject generator;

        public SpawnExecutor(Realm realm, GeneratorObject generator) {
            this(realm, generator, null);
            createDefaultFunctionProperties(ANONYMOUS, 2);
        }

        private SpawnExecutor(Realm realm, GeneratorObject generator, Void ignore) {
            super(realm, ANONYMOUS);
            this.generator = generator;
        }

        @Override
        public SpawnExecutor clone() {
            return new SpawnExecutor(getRealm(), generator, null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object resolve = getArgument(args, 0);
            Object reject = getArgument(args, 1);
            assert IsCallable(resolve) : "resolve not callable";
            assert IsCallable(reject) : "reject not callable";
            InitialStep(calleeContext, generator, (Callable) resolve, (Callable) reject);
            return UNDEFINED;
        }
    }

    /**
     * AsyncState Records
     */
    private static final class AsyncState {
        final GeneratorObject generator;
        final Callable resolve, reject;
        final CallStep resolvedAction, rejectedAction;

        public AsyncState(ExecutionContext cx, GeneratorObject generator, Callable resolve,
                Callable reject) {
            this.generator = generator;
            this.resolve = resolve;
            this.reject = reject;
            this.resolvedAction = new CallStep(cx.getRealm(), this, StepAction.Next);
            this.rejectedAction = new CallStep(cx.getRealm(), this, StepAction.Throw);
        }
    }

    private enum StepAction {
        Next, Throw
    }

    /**
     * InitialStep Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param generator
     *            the generator object
     * @param resolve
     *            the resolve callback
     * @param reject
     *            the reject callback
     */
    private static void InitialStep(ExecutionContext cx, GeneratorObject generator,
            Callable resolve, Callable reject) {
        AsyncState asyncState = new AsyncState(cx, generator, (Callable) resolve, (Callable) reject);
        Step(cx, asyncState, StepAction.Next, UNDEFINED);
    }

    /**
     * Step Abstract Operation
     * 
     * @param cx
     *            the execution context
     * @param asyncState
     *            the asynchronous state
     * @param action
     *            the step action to be performed
     * @param value
     *            the value argument
     */
    private static void Step(ExecutionContext cx, AsyncState asyncState, StepAction action,
            Object value) {
        GeneratorObject generator = asyncState.generator;
        ScriptObject next;
        try {
            if (action == StepAction.Next) {
                next = GeneratorResume(cx, generator, value);
            } else {
                next = GeneratorThrow(cx, generator, value);
            }
        } catch (ScriptException e) {
            asyncState.reject.call(cx, UNDEFINED, e.getValue());
            return;
        }
        if (IteratorComplete(cx, next)) {
            asyncState.resolve.call(cx, UNDEFINED, IteratorValue(cx, next));
            return;
        }
        PromiseObject p = PromiseOf(cx, IteratorValue(cx, next));
        PromiseThen(cx, p, asyncState.resolvedAction, asyncState.rejectedAction);
    }

    /**
     * Call Step Functions
     */
    private static final class CallStep extends BuiltinFunction {
        private final AsyncState asyncState;
        private final StepAction action;

        public CallStep(Realm realm, AsyncState asyncState, StepAction action) {
            this(realm, asyncState, action, null);
            createDefaultFunctionProperties(ANONYMOUS, 1);
        }

        private CallStep(Realm realm, AsyncState asyncState, StepAction action, Void ignore) {
            super(realm, ANONYMOUS);
            this.asyncState = asyncState;
            this.action = action;
        }

        @Override
        public CallStep clone() {
            return new CallStep(getRealm(), asyncState, action, null);
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = getArgument(args, 0);
            Step(calleeContext, asyncState, action, value);
            return UNDEFINED;
        }
    }
}
