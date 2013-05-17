/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
import static com.github.anba.es6draft.runtime.objects.iteration.IterationAbstractOperations.CreateItrResultObject;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.AddRestrictedFunctionProperties;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.types.BuiltinSymbol;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.builtins.BuiltinFunction;
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

        /* 15.19.4.2.1  @@iterator */
        // FIXME: spec bug - this does not make any sense....
        PropertyDescriptor desc = new PropertyDescriptor(new IteratorFunction(realm, this), false,
                false, true);
        ordinaryDefineOwnProperty(BuiltinSymbol.iterator.get(), desc);
    }

    private static class IteratorFunction extends BuiltinFunction {
        private final GeneratorObject generator;

        public IteratorFunction(Realm realm, GeneratorObject generator) {
            super(realm);
            this.generator = generator;
            ExecutionContext cx = realm.defaultContext();
            setPrototype(cx, realm.getIntrinsic(Intrinsics.FunctionPrototype));
            defineOwnProperty(cx, "name", new PropertyDescriptor("@@iterator", false, false, false));
            defineOwnProperty(cx, "length", new PropertyDescriptor(0, false, false, false));
            AddRestrictedFunctionProperties(cx, this);
        }

        /**
         * [[Call]]
         */
        @Override
        public Object call(ExecutionContext callerContext, Object thisValue, Object... args) {
            return generator;
        }
    }

    public void start(ExecutionContext cx, RuntimeInfo.Code code) {
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

    public Object resume(ExecutionContext cx, Object value) {
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

    public Object _throw(ExecutionContext cx, Object value) {
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

    public Object yield(Object value) {
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

    private static final class ThreadGroupFactory implements ThreadFactory {
        private final ThreadGroup group;

        ThreadGroupFactory() {
            this.group = Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            // place each thread into a new group to be able to preserve stacktraces
            ThreadGroup newGroup = new ThreadGroup(group, "generator-group");
            Thread newThread = new Thread(newGroup, r, "generator-thread");
            if (newThread.isDaemon()) {
                newThread.setDaemon(false);
            }
            if (newThread.getPriority() != Thread.NORM_PRIORITY) {
                newThread.setPriority(Thread.NORM_PRIORITY);
            }
            return newThread;
        }
    }

    private void start0() {
        this.state = GeneratorState.Executing;
        ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadGroupFactory());
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
