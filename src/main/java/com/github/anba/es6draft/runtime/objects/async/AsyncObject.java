/**
 * Copyright (c) 2012-2016 André Bargull
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
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
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
    private Continuation<Void> continuation;

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
        state = AsyncState.SuspendedAwait;
    }

    /**
     * Proceeds to the "completed" async state and releases internal resources.
     */
    private void close() {
        assert state == AsyncState.Executing : "close from: " + state;
        state = AsyncState.Completed;
        context = null;
        code = null;
        continuation = null;
    }

    /**
     * Starts async function execution.
     * 
     * @param cx
     *            the execution context
     * @param functionCode
     *            the runtime function code
     */
    void start(ExecutionContext cx, RuntimeInfo.Function functionCode) {
        assert state == null;
        context = cx;
        code = functionCode;
        state = AsyncState.Executing;
        context.setCurrentAsync(this);
        continuation = new CodeContinuation<>(new AsyncHandler(this));
        continuation.start(cx);
    }

    /**
     * Resumes async function execution.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the resumption value
     */
    void resume(ExecutionContext cx, Object value) {
        switch (state) {
        case Executing:
        case Completed:
            throw new AssertionError();
        case SuspendedAwait:
            state = AsyncState.Executing;
            continuation.resume(cx, value);
            return;
        default:
            throw new AssertionError();
        }
    }

    /**
     * Stops async function execution with a {@code throw} event.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the exception value
     */
    void _throw(ExecutionContext cx, Object value) {
        switch (state) {
        case Executing:
        case Completed:
            throw new AssertionError();
        case SuspendedAwait:
            state = AsyncState.Executing;
            continuation._throw(cx, ScriptException.create(value));
            return;
        default:
            throw new AssertionError();
        }
    }

    private static final class AsyncHandler implements Continuation.Handler<Void> {
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
        public Void suspendWith(ExecutionContext cx, Object value) {
            asyncObject.suspend();
            return null;
        }

        @Override
        public Void returnWith(ExecutionContext cx, Object result) {
            // Async execution finished, run AsyncFunctionStart steps 4-9.
            asyncObject.promiseCapability.getResolve().call(cx, UNDEFINED, result);
            return null;
        }

        @Override
        public Void returnWith(ExecutionContext cx, ScriptException exception) {
            // Async execution finished, run AsyncFunctionStart steps 4-9.
            asyncObject.promiseCapability.getReject().call(cx, UNDEFINED, exception.getValue());
            return null;
        }
    }
}
