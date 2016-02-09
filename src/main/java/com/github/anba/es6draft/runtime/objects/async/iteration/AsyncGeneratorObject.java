/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CodeContinuation;
import com.github.anba.es6draft.runtime.internal.Continuation;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Async Generator Functions</h1><br>
 * <h2>AsyncGenerator Objects</h2>
 * <ul>
 * <li>Properties of AsyncGenerator Instances
 * </ul>
 */
public final class AsyncGeneratorObject extends OrdinaryObject {
    /**
     * [[AsyncGeneratorState]]
     */
    public enum AsyncGeneratorState {
        SuspendedStart, SuspendedYield, Executing, Completed
    }

    /** [[AsyncGeneratorState]] */
    private AsyncGeneratorState state;

    /** [[Code]] */
    private RuntimeInfo.Function code;

    /** [[AsyncGeneratorContext]] */
    private ExecutionContext context;

    /** [[AsyncGeneratorQueue]] */
    private List<AsyncGeneratorRequest> queue;

    // internal generator implementation
    private Continuation<Void> continuation;

    /**
     * Constructs a new AsyncGenerator object.
     * 
     * @param realm
     *            the realm object
     */
    public AsyncGeneratorObject(Realm realm) {
        super(realm);
    }

    /**
     * [[AsyncGeneratorState]]
     *
     * @return the async generator state
     */
    public AsyncGeneratorState getState() {
        return state;
    }

    /**
     * [[AsyncGeneratorQueue]]
     * 
     * @return the list of generator requests
     */
    public List<?> getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return String.format("%s, state=%s, queue=%s", super.toString(), state, queue);
    }

    /**
     * Proceeds to the "suspendedYield" generator state.
     */
    private void suspend() {
        assert state == AsyncGeneratorState.Executing : "suspend from: " + state;
        this.state = AsyncGeneratorState.SuspendedYield;
    }

    /**
     * Proceeds to the "completed" generator state and releases internal resources.
     */
    private void close() {
        assert state == AsyncGeneratorState.Executing || state == AsyncGeneratorState.SuspendedStart : "close from: "
                + state;
        this.state = AsyncGeneratorState.Completed;
        this.context = null;
        this.code = null;
        this.continuation = null;
    }

    /**
     * Starts generator execution and sets {@link #state} to its initial value
     * {@link AsyncGeneratorState#SuspendedStart}.
     * 
     * @param cx
     *            the execution context
     * @param code
     *            the runtime function code
     * @see AsyncGeneratorAbstractOperations#AsyncGeneratorStart(ExecutionContext, AsyncGeneratorObject,
     *      com.github.anba.es6draft.runtime.internal.RuntimeInfo.Function)
     */
    void start(ExecutionContext cx, RuntimeInfo.Function code) {
        assert state == null;
        this.context = cx;
        this.code = code;
        this.state = AsyncGeneratorState.SuspendedStart;
        this.queue = new ArrayList<>();
        this.context.setCurrentAsyncGenerator(this);
        this.continuation = new CodeContinuation<>(new AsyncGeneratorHandler(this));
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
        case Completed:
            throw newTypeError(cx, Messages.Key.GeneratorExecuting);
        case SuspendedYield:
            state = AsyncGeneratorState.Executing;
            continuation.resume(cx, value);
            return;
        case SuspendedStart:
        case Executing:
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
        case Completed:
            throw newTypeError(cx, Messages.Key.GeneratorExecuting);
        case SuspendedYield:
            state = AsyncGeneratorState.Executing;
            continuation._throw(cx, ScriptException.create(value));
            return;
        case SuspendedStart:
        case Executing:
        default:
            throw new AssertionError();
        }
    }

    /**
     * AsyncGeneratorFulfill ( generator, iteratorResult )
     * 
     * @param cx
     *            the execution context
     * @param iteratorResult
     *            the iterator result object
     */
    void fulfill(ExecutionContext cx, ScriptObject iteratorResult) {
        /* step 1 (implicit) */
        /* step 2 (not applicable) */
        /* step 3 */
        List<AsyncGeneratorRequest> queue = this.queue;
        // FIXME: spec bug - queue can be empty
        if (!queue.isEmpty()) {
            /* step 4 */
            AsyncGeneratorRequest next = queue.remove(0);
            /* step 5 */
            PromiseCapability<PromiseObject> capability = next.getCapability();
            /* step 6 */
            capability.getResolve().call(cx, UNDEFINED, iteratorResult);
        }
        /* step 7 */
        resumeNext(cx);
        /* step 8 (return) */
    }

    /**
     * AsyncGeneratorReject ( generator, exception )
     * 
     * @param cx
     *            the execution context
     * @param exception
     *            the exception
     */
    void reject(ExecutionContext cx, ScriptException exception) {
        /* step 1 (implicit) */
        /* step 2 (not applicable) */
        /* step 3 */
        List<AsyncGeneratorRequest> queue = this.queue;
        /* step 4 */
        AsyncGeneratorRequest next = queue.remove(0);
        /* step 5 */
        PromiseCapability<PromiseObject> capability = next.getCapability();
        /* step 6 */
        capability.getReject().call(cx, UNDEFINED, exception.getValue());
        /* step 7 */
        resumeNext(cx);
        /* step 8 (return) */
    }

    /**
     * AsyncGeneratorResumeNext ( generator )
     * 
     * @param cx
     *            the execution context
     */
    void resumeNext(ExecutionContext cx) {
        /* step 1 (implicit) */
        /* steps 2-3 */
        assert state != AsyncGeneratorState.Executing;
        /* step 4 */
        List<AsyncGeneratorRequest> queue = this.queue;
        /* step 5 */
        if (queue.isEmpty()) {
            return;
        }
        /* step 6 */
        AsyncGeneratorRequest request = queue.get(0);
        /* step 7 (implicit) */
        /* steps 8-20 */
        // System.out.printf("request=%s, queue=%s%n", request, queue);
        switch (request.getCompletionType()) {
        case Normal: {
            /* step 8 */
            Object completion = request.getCompletion();
            /* step 9 (not applicable) */
            switch (state) {
            case Completed:
                /* step 10 */
                fulfill(cx, CreateIterResultObject(cx, UNDEFINED, true));
                return;
            case SuspendedStart:
                /* steps 11-19 */
                state = AsyncGeneratorState.Executing;
                continuation.start(cx);
                /* step 20 */
                return;
            case SuspendedYield:
                /* steps 11-19 */
                state = AsyncGeneratorState.Executing;
                continuation.resume(cx, completion);
                /* step 20 */
                return;
            case Executing:
            default:
                throw new AssertionError();
            }
        }
        case Return: {
            /* step 8 */
            ReturnValue completion = (ReturnValue) request.getCompletion();
            /* steps 9-11 */
            switch (state) {
            case SuspendedStart:
                /* step 9.a */
                state = AsyncGeneratorState.Completed;
                // fall-through
            case Completed:
                /* step 9.b.i */
                ScriptObject result = CreateIterResultObject(cx, completion.getValue(), true);
                fulfill(cx, result);
                return;
            case SuspendedYield:
                /* step 10 (not applicable) */
                /* steps 11-19 */
                state = AsyncGeneratorState.Executing;
                continuation._return(cx, completion.getValue());
                /* step 20 */
                return;
            case Executing:
            default:
                throw new AssertionError();
            }
        }
        case Throw: {
            /* step 8 */
            ScriptException completion = (ScriptException) request.getCompletion();
            /* steps 9-11 */
            switch (state) {
            case SuspendedStart:
                /* step 9.a */
                state = AsyncGeneratorState.Completed;
                // fall-through
            case Completed:
                /* step 9.b */
                reject(cx, completion);
                return;
            case SuspendedYield:
                /* step 10 (not applicable) */
                /* steps 11-19 */
                state = AsyncGeneratorState.Executing;
                continuation._throw(cx, completion);
                /* step 20 */
                return;
            case Executing:
            default:
                throw new AssertionError();
            }
        }
        default:
            throw new AssertionError();
        }
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param completionType
     *            the completion type
     * @param completion
     *            the completion value
     * @return the promise object
     */
    PromiseObject enqueue(ExecutionContext cx, AsyncGeneratorRequest.CompletionType completionType, Object completion) {
        /* steps 1-3 (not applicable) */
        /* step 4 */
        List<AsyncGeneratorRequest> queue = this.queue;
        /* step 5 */
        PromiseCapability<PromiseObject> capability = PromiseAbstractOperations.PromiseBuiltinCapability(cx);
        /* step 6 */
        AsyncGeneratorRequest request = new AsyncGeneratorRequest(completionType, completion, capability);
        /* step 7 */
        queue.add(request);
        /* step 8 */
        AsyncGeneratorState state = this.state;
        /* step 9 */
        if (state != AsyncGeneratorState.Executing) {
            resumeNext(cx);
        }
        return capability.getPromise();
    }

    private static final class AsyncGeneratorHandler implements Continuation.Handler<Void> {
        private final AsyncGeneratorObject generatorObject;

        AsyncGeneratorHandler(AsyncGeneratorObject generatorObject) {
            this.generatorObject = generatorObject;
        }

        @Override
        public Object evaluate(ResumptionPoint resumptionPoint) throws Throwable {
            AsyncGeneratorObject genObj = generatorObject;
            return genObj.code.handle().invokeExact(genObj.context, resumptionPoint);
        }

        @Override
        public void close() {
            generatorObject.close();
        }

        @Override
        public Void suspendWith(ExecutionContext cx, Object value) {
            generatorObject.suspend();
            if (value != null) {
                // The iteration result object was already constructed at the call site.
                generatorObject.fulfill(cx, (ScriptObject) value);
            }
            return null;
        }

        @Override
        public Void returnWith(ExecutionContext cx, Object result) {
            // AsyncGeneratorStart - step 5.g
            generatorObject.fulfill(cx, CreateIterResultObject(cx, result, true));
            return null;
        }

        @Override
        public Void returnWith(ExecutionContext cx, ScriptException exception) {
            // AsyncGeneratorStart - step 5.f
            generatorObject.reject(cx, exception);
            return null;
        }
    }
}
