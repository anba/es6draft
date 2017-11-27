/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.async.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.objects.promise.PromiseAbstractOperations.PromiseBuiltinCapability;
import static com.github.anba.es6draft.runtime.objects.promise.PromisePrototype.PerformPromiseThen;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.ArrayDeque;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CodeContinuation;
import com.github.anba.es6draft.runtime.internal.Continuation;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.objects.async.Async;
import com.github.anba.es6draft.runtime.objects.promise.PromiseCapability;
import com.github.anba.es6draft.runtime.objects.promise.PromiseObject;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>Async Generator Functions</h1><br>
 * <h2>AsyncGenerator Objects</h2>
 * <ul>
 * <li>Properties of AsyncGenerator Instances
 * </ul>
 */
public final class AsyncGeneratorObject extends OrdinaryObject implements Async {
    /**
     * [[AsyncGeneratorState]]
     */
    public enum AsyncGeneratorState {
        // NB: Additional 'SuspendedAwait' state for easier debugging of async generator execution.
        SuspendedStart, SuspendedYield, SuspendedAwait, Executing, AwaitingReturn, Completed
    }

    /** [[AsyncGeneratorState]] */
    private AsyncGeneratorState state;

    /** [[Code]] */
    private RuntimeInfo.Function code;

    /** [[AsyncGeneratorContext]] */
    private ExecutionContext context;

    /** [[AsyncGeneratorQueue]] */
    private ArrayDeque<AsyncGeneratorRequest> queue;

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
    public ArrayDeque<AsyncGeneratorRequest> getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return String.format("%s, state=%s", super.toString(), state);
    }

    /**
     * Proceeds to either the "suspendedAwait" or the "suspendedYield" generator state.
     */
    private void suspend(AsyncGeneratorState suspendState) {
        assert suspendState == AsyncGeneratorState.SuspendedYield || suspendState == AsyncGeneratorState.SuspendedAwait;
        assert state == AsyncGeneratorState.Executing : "suspend from: " + state;
        this.state = suspendState;
    }

    /**
     * Proceeds to the "completed" generator state and releases internal resources.
     */
    private void close() {
        assert state == AsyncGeneratorState.AwaitingReturn || state == AsyncGeneratorState.Executing
                || state == AsyncGeneratorState.SuspendedStart : "close from: " + state;
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
        this.queue = new ArrayDeque<>();
        this.context.setCurrentAsync(this);
        this.continuation = new CodeContinuation<>(new AsyncGeneratorHandler(this));
    }

    @Override
    public void resume(ExecutionContext cx, Object value) {
        switch (state) {
        case SuspendedStart:
        case SuspendedYield:
        case Executing:
        case Completed:
            throw new AssertionError();
        case SuspendedAwait:
            state = AsyncGeneratorState.Executing;
            continuation.resume(cx, value);
            return;
        default:
            throw new AssertionError();
        }
    }

    @Override
    public void _throw(ExecutionContext cx, Object value) {
        switch (state) {
        case SuspendedStart:
        case SuspendedYield:
        case Executing:
        case Completed:
            throw new AssertionError();
        case SuspendedAwait:
            state = AsyncGeneratorState.Executing;
            continuation._throw(cx, ScriptException.create(value));
            return;
        default:
            throw new AssertionError();
        }
    }

    /**
     * AsyncGeneratorResolve ( generator, value, done )
     * 
     * @param cx
     *            the execution context
     * @param iteratorResult
     *            the iterator result object
     */
    private void resolve(ExecutionContext cx, Object value, boolean done) {
        /* steps 1-7 */
        resolveWithoutResumeNext(cx, value, done);
        /* step 8 */
        resumeNext(cx);
        /* step 9 (return) */
    }

    /**
     * AsyncGeneratorResolve ( generator, value, done )
     * 
     * @param cx
     *            the execution context
     * @param iteratorResult
     *            the iterator result object
     */
    private void resolveWithoutResumeNext(ExecutionContext cx, Object value, boolean done) {
        /* step 1 (implicit) */
        /* step 2 */
        ArrayDeque<AsyncGeneratorRequest> queue = this.queue;
        /* step 3 */
        assert !queue.isEmpty();
        /* step 4 */
        AsyncGeneratorRequest next = queue.pollFirst();
        /* step 5 */
        PromiseCapability<PromiseObject> promiseCapability = next.getCapability();
        /* step 6 */
        ScriptObject iteratorResult = CreateIterResultObject(cx, value, done);
        /* step 7 */
        promiseCapability.getResolve().call(cx, UNDEFINED, iteratorResult);
        /* steps 8-9 (not applicable) */
    }

    /**
     * AsyncGeneratorReject ( generator, exception )
     * 
     * @param cx
     *            the execution context
     * @param exception
     *            the exception
     */
    private void reject(ExecutionContext cx, ScriptException exception) {
        /* steps 1-6 */
        rejectWithoutResumeNext(cx, exception);
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
    private void rejectWithoutResumeNext(ExecutionContext cx, ScriptException exception) {
        /* step 1 (implicit) */
        /* step 2 */
        ArrayDeque<AsyncGeneratorRequest> queue = this.queue;
        /* step 3 */
        assert !queue.isEmpty();
        /* step 4 */
        AsyncGeneratorRequest next = queue.pollFirst();
        /* step 5 */
        PromiseCapability<PromiseObject> capability = next.getCapability();
        /* step 6 */
        capability.getReject().call(cx, UNDEFINED, exception.getValue());
        /* steps 7-8 (not applicable) */
    }

    /**
     * AsyncGeneratorResumeNext ( generator )
     * 
     * @param cx
     *            the execution context
     */
    private void resumeNext(ExecutionContext cx) {
        while (true) {
            /* step 1 (implicit) */
            /* steps 2-3 */
            assert !(state == AsyncGeneratorState.Executing || state == AsyncGeneratorState.SuspendedAwait);
            /* step 4 */
            if (state == AsyncGeneratorState.AwaitingReturn) {
                return;
            }
            /* step 5 */
            ArrayDeque<AsyncGeneratorRequest> queue = this.queue;
            /* step 6 */
            if (queue.isEmpty()) {
                return;
            }
            /* step 7 */
            AsyncGeneratorRequest request = queue.peekFirst();
            /* step 8 (implicit) */
            /* steps 9-21 */
            switch (request.getCompletionType()) {
            case Normal: {
                /* step 9 */
                Object completion = request.getCompletion();
                /* step 10 (not applicable) */
                switch (state) {
                case Completed:
                    /* step 11 */
                    resolveWithoutResumeNext(cx, UNDEFINED, true);
                    continue;
                case SuspendedStart:
                    /* steps 12-20 */
                    state = AsyncGeneratorState.Executing;
                    continuation.start(cx);
                    /* step 21 */
                    return;
                case SuspendedYield:
                    /* steps 12-20 */
                    state = AsyncGeneratorState.Executing;
                    continuation.resume(cx, completion);
                    /* step 21 */
                    return;
                case Executing:
                case SuspendedAwait:
                default:
                    throw new AssertionError();
                }
            }
            case Return: {
                /* step 9 */
                ReturnValue completion = (ReturnValue) request.getCompletion();
                /* steps 10-12 */
                switch (state) {
                case SuspendedStart:
                    /* step 10.a */
                    close();
                    // fall-through
                case Completed: {
                    /* step 10.b.i */
                    /* step 10.b.i.1 */
                    state = AsyncGeneratorState.AwaitingReturn;
                    /* step 10.b.i.2 */
                    PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
                    /* step 10.b.i.3 */
                    promiseCapability.getResolve().call(cx, UNDEFINED, completion.getValue());
                    /* steps 10.b.i.4-6 */
                    AsyncGeneratorResumeNextReturnProcessorFulfilledFunction onFulfilled = new AsyncGeneratorResumeNextReturnProcessorFulfilledFunction(
                            cx.getRealm(), this);
                    AsyncGeneratorResumeNextReturnProcessorRejectedFunction onRejected = new AsyncGeneratorResumeNextReturnProcessorRejectedFunction(
                            cx.getRealm(), this);
                    /* steps 10.b.i.7 */
                    PromiseCapability<PromiseObject> throwawayCapability = PromiseBuiltinCapability(cx);
                    /* steps 10.b.i.8 */
                    // TODO: Set [[PromiseIsHandled]]?
                    /* steps 10.b.i.9 */
                    PerformPromiseThen(cx, promiseCapability.getPromise(), onFulfilled, onRejected,
                            throwawayCapability);
                    /* steps 10.b.i.10 */
                    return;
                }
                case SuspendedYield:
                    /* step 11 (not applicable) */
                    /* steps 12-20 */
                    state = AsyncGeneratorState.Executing;
                    continuation._return(cx, completion.getValue());
                    /* step 21 */
                    return;
                case Executing:
                case SuspendedAwait:
                default:
                    throw new AssertionError();
                }
            }
            case Throw: {
                /* step 9 */
                ScriptException completion = (ScriptException) request.getCompletion();
                /* steps 10-12 */
                switch (state) {
                case SuspendedStart:
                    /* step 10.a */
                    close();
                    // fall-through
                case Completed:
                    /* step 10.b.ii */
                    rejectWithoutResumeNext(cx, completion);
                    continue;
                case SuspendedYield:
                    /* step 11 (not applicable) */
                    /* steps 12-20 */
                    state = AsyncGeneratorState.Executing;
                    continuation._throw(cx, completion);
                    /* step 21 */
                    return;
                case Executing:
                case SuspendedAwait:
                default:
                    throw new AssertionError();
                }
            }
            default:
                throw new AssertionError();
            }
        }
    }

    /**
     * AsyncGeneratorResumeNext Return Processor Fulfilled Functions
     */
    public static final class AsyncGeneratorResumeNextReturnProcessorFulfilledFunction extends BuiltinFunction {
        /** [[Generator]] */
        private final AsyncGeneratorObject generator;

        public AsyncGeneratorResumeNextReturnProcessorFulfilledFunction(Realm realm, AsyncGeneratorObject generator) {
            super(realm, ANONYMOUS, 1);
            this.generator = generator;
            createDefaultFunctionProperties();
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object value = argument(args, 0);
            /* step 1 */
            generator.close();
            /* step 2 */
            generator.resolve(calleeContext, value, true);
            return UNDEFINED;
        }
    }

    /**
     * AsyncGeneratorResumeNext Return Processor Rejected Functions
     */
    public static final class AsyncGeneratorResumeNextReturnProcessorRejectedFunction extends BuiltinFunction {
        /** [[Generator]] */
        private final AsyncGeneratorObject generator;

        public AsyncGeneratorResumeNextReturnProcessorRejectedFunction(Realm realm, AsyncGeneratorObject generator) {
            super(realm, ANONYMOUS, 1);
            this.generator = generator;
            createDefaultFunctionProperties();
        }

        @Override
        public Undefined call(ExecutionContext callerContext, Object thisValue, Object... args) {
            ExecutionContext calleeContext = calleeContext();
            Object reason = argument(args, 0);
            /* step 1 */
            generator.close();
            /* step 2 */
            generator.reject(calleeContext, ScriptException.create(reason));
            return UNDEFINED;
        }
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param completion
     *            the completion value
     * @return the promise object
     */
    PromiseObject enqueue(ExecutionContext cx, Object completion) {
        return enqueue(cx, AsyncGeneratorRequest.CompletionType.Normal, completion);
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param completion
     *            the completion value
     * @return the promise object
     */
    PromiseObject enqueue(ExecutionContext cx, ScriptException completion) {
        return enqueue(cx, AsyncGeneratorRequest.CompletionType.Throw, completion);
    }

    /**
     * AsyncGeneratorEnqueue ( generator, completion )
     * 
     * @param cx
     *            the execution context
     * @param completion
     *            the completion value
     * @return the promise object
     */
    PromiseObject enqueue(ExecutionContext cx, ReturnValue completion) {
        return enqueue(cx, AsyncGeneratorRequest.CompletionType.Return, completion);
    }

    private PromiseObject enqueue(ExecutionContext cx, AsyncGeneratorRequest.CompletionType type, Object completion) {
        /* steps 1, 3 (not applicable) */
        /* step 2 */
        PromiseCapability<PromiseObject> promiseCapability = PromiseBuiltinCapability(cx);
        /* step 4 */
        ArrayDeque<AsyncGeneratorRequest> queue = this.queue;
        /* step 5 */
        AsyncGeneratorRequest request = new AsyncGeneratorRequest(type, completion, promiseCapability);
        /* step 6 */
        queue.addLast(request);
        /* step 7 */
        AsyncGeneratorState state = this.state;
        /* step 8 */
        if (!(state == AsyncGeneratorState.Executing || state == AsyncGeneratorState.SuspendedAwait)) {
            resumeNext(cx);
        }
        /* step 9 */
        return promiseCapability.getPromise();
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
            if (value != null) {
                generatorObject.suspend(AsyncGeneratorState.SuspendedYield);
                generatorObject.resolve(cx, value, false);
            } else {
                generatorObject.suspend(AsyncGeneratorState.SuspendedAwait);
            }
            return null;
        }

        @Override
        public Void returnWith(ExecutionContext cx, Object result) {
            // AsyncGeneratorStart - step 5.g
            generatorObject.resolve(cx, result, true);
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
