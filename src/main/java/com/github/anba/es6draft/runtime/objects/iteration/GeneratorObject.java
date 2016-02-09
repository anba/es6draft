/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CodeContinuation;
import com.github.anba.es6draft.runtime.internal.Continuation;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
import com.github.anba.es6draft.runtime.internal.ReturnValue;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ThreadContinuation;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.3 Generator Objects</h2>
 * <ul>
 * <li>25.3.2 Properties of Generator Instances
 * </ul>
 */
public final class GeneratorObject extends OrdinaryObject {
    /**
     * [[GeneratorState]]
     */
    public enum GeneratorState {
        SuspendedStart, SuspendedYield, Executing, Completed
    }

    /** [[GeneratorState]] */
    private GeneratorState state;

    /** [[Code]] */
    private RuntimeInfo.Function code;

    /** [[GeneratorContext]] */
    private ExecutionContext context;

    /** [[LastYieldValue]] */
    private Object lastYieldValue;

    // internal generator implementation
    private Continuation<ScriptObject> continuation;

    /**
     * Constructs a new Generator object.
     * 
     * @param realm
     *            the realm object
     */
    public GeneratorObject(Realm realm) {
        super(realm);
    }

    /**
     * [[GeneratorState]]
     *
     * @return the generator state
     */
    public GeneratorState getState() {
        return state;
    }

    /**
     * [[LastYieldValue]]
     *
     * @return the last yield value
     */
    public Object getLastYieldValue() {
        return lastYieldValue;
    }

    /**
     * Returns {@code true} for legacy generator objects.
     * 
     * @return {@code true} if legacy generator object
     */
    public boolean isLegacyGenerator() {
        return code != null && code.is(RuntimeInfo.FunctionFlags.LegacyGenerator);
    }

    /**
     * Proceeds to the "suspendedYield" generator state.
     */
    private void suspend() {
        assert state == GeneratorState.Executing : "suspend from: " + state;
        this.state = GeneratorState.SuspendedYield;
        this.lastYieldValue = UNDEFINED;
    }

    /**
     * Proceeds to the "completed" generator state and releases internal resources.
     */
    private void close() {
        assert state == GeneratorState.Executing || state == GeneratorState.SuspendedStart : "close from: " + state;
        this.state = GeneratorState.Completed;
        this.lastYieldValue = null;
        this.context = null;
        this.code = null;
        this.continuation = null;
    }

    /**
     * Starts generator execution and sets {@link #state} to its initial value {@link GeneratorState#SuspendedStart}.
     * 
     * @param cx
     *            the execution context
     * @param code
     *            the runtime function code
     * @see GeneratorAbstractOperations#GeneratorStart(ExecutionContext, GeneratorObject, RuntimeInfo.Function)
     */
    void start(ExecutionContext cx, RuntimeInfo.Function code) {
        assert state == null;
        this.context = cx;
        this.code = code;
        this.state = GeneratorState.SuspendedStart;
        this.lastYieldValue = UNDEFINED;
        this.context.setCurrentGenerator(this);
        GeneratorHandler handler = new GeneratorHandler(this);
        if (code.is(RuntimeInfo.FunctionFlags.ResumeGenerator)) {
            this.continuation = new CodeContinuation<>(handler);
        } else {
            this.continuation = new ThreadContinuation<>(handler);
        }
    }

    /**
     * Resumes generator execution.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the resumption value
     * @return the iterator result object
     * @see GeneratorAbstractOperations#GeneratorResume(ExecutionContext, Object, Object)
     */
    ScriptObject resume(ExecutionContext cx, Object value) {
        switch (state) {
        case Executing:
            throw newTypeError(cx, Messages.Key.GeneratorExecuting);
        case Completed:
            return CreateIterResultObject(cx, UNDEFINED, true);
        case SuspendedStart:
            this.state = GeneratorState.Executing;
            this.lastYieldValue = value;
            return continuation.start(cx);
        case SuspendedYield:
            this.state = GeneratorState.Executing;
            this.lastYieldValue = value;
            return continuation.resume(cx, value);
        default:
            throw new AssertionError();
        }
    }

    /**
     * Stops generator execution with a {@code return} event.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the return value
     * @return the iterator result object
     * @see GeneratorPrototype.Properties#_return(ExecutionContext, Object, Object)
     */
    ScriptObject _return(ExecutionContext cx, Object value) {
        switch (state) {
        case Executing:
            throw newTypeError(cx, Messages.Key.GeneratorExecuting);
        case SuspendedStart:
            close();
            // fall-through
        case Completed:
            return CreateIterResultObject(cx, value, true);
        case SuspendedYield:
            this.state = GeneratorState.Executing;
            return continuation._return(cx, value);
        default:
            throw new AssertionError();
        }
    }

    /**
     * Stops generator execution with a {@code throw} event.
     * 
     * @param cx
     *            the execution context
     * @param value
     *            the exception value
     * @return the iterator result object
     * @see GeneratorPrototype.Properties#_throw(ExecutionContext, Object, Object)
     */
    ScriptObject _throw(ExecutionContext cx, Object value) {
        switch (state) {
        case Executing:
            throw newTypeError(cx, Messages.Key.GeneratorExecuting);
        case SuspendedStart:
            close();
            // fall-through
        case Completed:
            throw ScriptException.create(value);
        case SuspendedYield:
            this.state = GeneratorState.Executing;
            return continuation._throw(cx, ScriptException.create(value));
        default:
            throw new AssertionError();
        }
    }

    /**
     * Suspends the current generator execution.
     * 
     * @param value
     *            the iteration result object to yield
     * @return the yield result
     * @see GeneratorAbstractOperations#GeneratorYield(ExecutionContext, ScriptObject)
     * @throws ReturnValue
     *             to signal an abrupt Return completion
     */
    Object yield(ScriptObject value) throws ReturnValue {
        assert state == GeneratorState.Executing : "yield from: " + state;
        return continuation.suspend(value);
    }

    private static final class GeneratorHandler implements Continuation.Handler<ScriptObject> {
        private final GeneratorObject generatorObject;

        GeneratorHandler(GeneratorObject generatorObject) {
            this.generatorObject = generatorObject;
        }

        @Override
        public Object evaluate(ResumptionPoint resumptionPoint) throws Throwable {
            GeneratorObject genObj = generatorObject;
            return genObj.code.handle().invokeExact(genObj.context, resumptionPoint);
        }

        @Override
        public void close() {
            generatorObject.close();
        }

        @Override
        public ScriptObject suspendWith(ExecutionContext cx, Object value) {
            generatorObject.suspend();
            // The iteration result object was already constructed at the call site.
            return (ScriptObject) value;
        }

        @Override
        public ScriptObject returnWith(ExecutionContext cx, Object result) {
            return CreateIterResultObject(cx, result, true);
        }

        @Override
        public ScriptObject returnWith(ExecutionContext cx, ScriptException exception) {
            throw exception;
        }
    }
}
