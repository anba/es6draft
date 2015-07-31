/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CodeContinuation;
import com.github.anba.es6draft.runtime.internal.Continuation;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ThreadContinuation;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;

/**
 * <h1>Async Functions</h1>
 * 
 * The async state shares
 */
public final class AsyncObject {
    public enum AsyncState {
        SuspendedAwait, Executing, Completed
    }

    private final PromiseCapability<PromiseObject> promiseCapability;
    private RuntimeInfo.Function code;
    private ExecutionContext context;
    private AsyncState state;
    private Continuation<Object> continuation;

    AsyncObject(PromiseCapability<PromiseObject> promiseCapability) {
        this.promiseCapability = promiseCapability;
    }

    /**
     * Returns the current async state.
     *
     * @return the async state
     */
    public AsyncState getState() {
        return state;
    }

    /**
     * Proceeds to the "SuspendedAwait" async state.
     */
    private void suspend() {
        assert state == AsyncState.Executing : "suspend from: " + state;
        this.state = AsyncState.SuspendedAwait;
    }

    /**
     * Proceeds to the "completed" async state and releases internal resources.
     */
    private void close() {
        assert state == AsyncState.Executing : "close from: " + state;
        this.state = AsyncState.Completed;
        this.context = null;
        this.code = null;
        this.continuation = null;
    }

    /**
     * Starts async function execution.
     * 
     * @param cx
     *            the execution context
     * @param code
     *            the runtime function code
     * @return the result value
     */
    Object start(ExecutionContext cx, RuntimeInfo.Function code) {
        assert state == null;
        this.context = cx;
        this.code = code;
        this.state = AsyncState.Executing;
        this.context.setCurrentAsync(this);
        AsyncHandler handler = new AsyncHandler(this);
        if (code.is(RuntimeInfo.FunctionFlags.ResumeGenerator)) {
            this.continuation = new CodeContinuation<>(handler);
        } else {
            this.continuation = new ThreadContinuation<>(handler);
        }
        return continuation.start(cx);
    }

    /**
     * Resumes async function execution.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the resumption value
     * @return the result value
     */
    Object resume(ExecutionContext cx, Object value) {
        switch (state) {
        case Executing:
        case Completed:
            throw new AssertionError();
        case SuspendedAwait:
        default:
            this.state = AsyncState.Executing;
            return continuation.resume(cx, value);
        }
    }

    /**
     * Stops async function execution with a {@code throw} event.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the exception value
     * @return the result value
     */
    Object _throw(ExecutionContext cx, Object value) {
        switch (state) {
        case Executing:
        case Completed:
            throw new AssertionError();
        case SuspendedAwait:
        default:
            this.state = AsyncState.Executing;
            return continuation._throw(cx, ScriptException.create(value));
        }
    }

    /**
     * Suspends the current async function execution.
     * 
     * @param value
     *            the await value
     * @return the result value
     * @throws ReturnValue
     *             to signal an abrupt Return completion
     */
    Object await(Object value) throws ReturnValue {
        assert state == AsyncState.Executing : "await from: " + state;
        return continuation.suspend(value);
    }

    private static final class AsyncHandler implements Continuation.Handler<Object> {
        private final AsyncObject asyncObject;

        AsyncHandler(AsyncObject asyncObject) {
            this.asyncObject = asyncObject;
        }

        @Override
        public Object evaluate(ResumptionPoint resumptionPoint) throws Throwable {
            AsyncObject asyncObj = asyncObject;
            return asyncObj.code.handle().invokeExact(asyncObj.context, resumptionPoint);
        }

        @Override
        public void close() {
            asyncObject.close();
        }

        @Override
        public Object suspendWith(ExecutionContext cx, Object value) {
            asyncObject.suspend();
            return value;
        }

        @Override
        public Object returnWith(ExecutionContext cx, Object result) {
            // Async execution finished, run AsyncFunctionStart steps 4-9.
            return asyncObject.promiseCapability.getResolve().call(cx, UNDEFINED, result);
        }

        @Override
        public Object returnWith(ExecutionContext cx, ScriptException exception) {
            // Async execution finished, run AsyncFunctionStart steps 4-9.
            return asyncObject.promiseCapability.getReject().call(cx, UNDEFINED, exception.getValue());
        }
    }
}
