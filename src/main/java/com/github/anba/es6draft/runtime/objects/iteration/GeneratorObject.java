/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.internal.GeneratorThread.newGeneratorThreadFactory;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.lang.invoke.MethodHandle;
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
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.3 Generator Objects</h2>
 * <ul>
 * <li>25.3.2 Properties of Generator Instances
 * </ul>
 */
public class GeneratorObject extends OrdinaryObject {
    /**
     * [[GeneratorState]]
     */
    public enum GeneratorState {
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

    /** [[GeneratorState]] */
    public GeneratorState getState() {
        return state;
    }

    /**
     * @see GeneratorAbstractOperations#GeneratorStart(ExecutionContext, GeneratorObject,
     *      RuntimeInfo.Code)
     */
    void start(ExecutionContext cx, RuntimeInfo.Code code) {
        assert state == null;
        this.context = cx;
        this.code = code;
        this.state = GeneratorState.SuspendedStart;
        this.context.setCurrentGenerator(this);
    }

    /**
     * @see GeneratorAbstractOperations#GeneratorResume(ExecutionContext, Object, Object)
     */
    Object resume(ExecutionContext cx, Object value) {
        GeneratorState state = this.state;
        if (state == null) {
            // uninitialised generator object
            throw throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        switch (state) {
        case Executing:
            throw throwTypeError(cx, Messages.Key.GeneratorExecuting);
        case Completed:
            return CreateIterResultObject(cx, UNDEFINED, true);
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
            throw throwTypeError(cx, Messages.Key.UninitialisedObject);
        }
        switch (state) {
        case Executing:
            throw throwTypeError(cx, Messages.Key.GeneratorExecuting);
        case Completed:
            throw ScriptException.create(value);
        case SuspendedStart:
            this.state = GeneratorState.Completed;
            throw ScriptException.create(value);
        case SuspendedYield:
        default:
            resume0(ScriptException.create(value));
            return execute0(cx);
        }
    }

    /**
     * @see GeneratorAbstractOperations#GeneratorYield(ExecutionContext, ScriptObject)
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
                    result = evaluate(context, code.handle());
                } catch (ScriptException | StackOverflowError e) {
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

    private static Object evaluate(ExecutionContext cx, MethodHandle handle) {
        try {
            return handle.invokeExact(cx);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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
        Object result;
        try {
            result = future.get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (result instanceof ScriptException) {
            throw (ScriptException) result;
        }
        if (result instanceof StackOverflowError) {
            throw (StackOverflowError) result;
        }
        return CreateIterResultObject(cx, result, true);
    }
}
