/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.GeneratorThread.newGeneratorThreadFactory;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateItrResultObject;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.19 The "std:iteration" Module</h2><br>
 * <h3>15.19.4 Generator Objects</h3>
 * <ul>
 * <li>15.19.4.2 Properties of Generator Instances
 * </ul>
 */
public class GeneratorObject extends OrdinaryObject {
    /**
     * [[GeneratorState]]
     */
    private enum GeneratorState {
        SuspendedStart, SuspendedYield, Executing, Completed
    }

    /** [[GeneratorState]] */
    private GeneratorState state;

    /** [[Code]] */
    private RuntimeInfo.Code code;

    /** [[GeneratorContext]] */
    private ExecutionContext context;

    // internal
    private static final Object COMPLETED = new Object();
    private Future<Object> future;
    private SynchronousQueue<Object> out;
    private SynchronousQueue<Object> in;

    public GeneratorObject(Realm realm) {
        super(realm);
        this.in = new SynchronousQueue<>();
        this.out = new SynchronousQueue<>();
    }

    /**
     * @see IterationAbstractOperations#GeneratorStart(ExecutionContext, GeneratorObject,
     *      RuntimeInfo.Code)
     */
    void start(ExecutionContext cx, RuntimeInfo.Code code) {
        // FIXME: spec bug - state must be <undefined>
        if (state != null) {
            // generator object already initialised
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        this.context = cx;
        this.code = code;
        this.state = GeneratorState.SuspendedStart;
        this.context.setCurrentGenerator(this);
    }

    /**
     * @see IterationAbstractOperations#GeneratorResume(ExecutionContext, Object, Object)
     */
    Object resume(ExecutionContext cx, Object value) {
        GeneratorState state = this.state;
        if (state == null) {
            // uninitialised generator object
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        switch (state) {
        case Executing:
            throw throwTypeError(cx, Messages.Key.GeneratorExecuting);
        case Completed:
            throw throwTypeError(cx, Messages.Key.GeneratorClosed);
        case SuspendedStart:
            if (value != UNDEFINED) {
                throw throwTypeError(cx, Messages.Key.GeneratorNewbornSend);
            }
            start0();
            return execute0(cx);
        case SuspendedYield:
        default:
            resume0(value);
            return execute0(cx);
        }
    }

    /**
     * @see GeneratorPrototype.Properties#_throw(ExecutionContext, Object, Object)
     */
    Object _throw(ExecutionContext cx, Object value) {
        GeneratorState state = this.state;
        if (state == null) {
            // uninitialised generator object
            throw throwTypeError(cx, Messages.Key.IncompatibleObject);
        }
        switch (state) {
        case Executing:
            throw throwTypeError(cx, Messages.Key.GeneratorExecuting);
        case Completed:
            throw throwTypeError(cx, Messages.Key.GeneratorClosed);
        case SuspendedStart:
            this.state = GeneratorState.Completed;
            throw ScriptRuntime._throw(value);
        case SuspendedYield:
        default:
            resume0(new ScriptException(value));
            return execute0(cx);
        }
    }

    /**
     * @see IterationAbstractOperations#GeneratorYield(ExecutionContext, ScriptObject)
     */
    Object yield(Object value) {
        assert state == GeneratorState.Executing : "yield from generator in state: " + state;
        try {
            this.out.put(value);
            Object resumptionValue = this.in.take();
            if (resumptionValue instanceof ScriptException) {
                throw (ScriptException) resumptionValue;
            }
            return resumptionValue;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void start0() {
        this.state = GeneratorState.Executing;
        ExecutorService executor = Executors.newSingleThreadExecutor(newGeneratorThreadFactory());
        future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object result;
                try {
                    result = code.evaluate(context);
                } catch (ScriptException e) {
                    result = e;
                } catch (Throwable t) {
                    out.put(COMPLETED);
                    throw t;
                }
                out.put(COMPLETED);
                return result;
            }
        });
        executor.shutdown();
    }

    private void resume0(Object value) {
        this.state = GeneratorState.Executing;
        try {
            in.put(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Object execute0(ExecutionContext cx) {
        if (!future.isDone()) {
            Object result;
            try {
                result = out.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (result != COMPLETED) {
                state = GeneratorState.SuspendedYield;
                return result;
            }
        }

        state = GeneratorState.Completed;
        try {
            Object result = future.get();
            if (result instanceof ScriptException) {
                throw (ScriptException) result;
            }
            return CreateItrResultObject(cx, result, true);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
