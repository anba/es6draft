/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateIterResultObject;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;
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
import com.github.anba.es6draft.runtime.internal.ResumptionPoint;
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

    // internal generator implementation
    private Generator generator;

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
     * Proceeds to the "suspendedYield" generator state.
     */
    private void suspend() {
        assert state == GeneratorState.Executing : "suspend from: " + state;
        this.state = GeneratorState.SuspendedYield;
    }

    /**
     * Proceeds to the "completed" generator state and releases internal resources.
     */
    private void close() {
        assert state == GeneratorState.Executing || state == GeneratorState.SuspendedStart : "close from: "
                + state;
        this.context = null;
        this.code = null;
        this.state = GeneratorState.Completed;
        this.generator = null;
    }

    /**
     * @param cx
     *            the execution context
     * @param code
     *            the runtime function code
     * @see GeneratorAbstractOperations#GeneratorStart(ExecutionContext, GeneratorObject,
     *      RuntimeInfo.Function)
     */
    void start(ExecutionContext cx, RuntimeInfo.Function code) {
        assert state == null;
        this.context = cx;
        this.code = code;
        this.state = GeneratorState.SuspendedStart;
        this.context.setCurrentGenerator(this);
        this.generator = code.hasSyntheticMethods() ? new ThreadGenerator(this)
                : new ResumeGenerator(this);
    }

    /**
     * @param cx
     *            the execution context
     * @param value
     *            the resumption value
     * @return the iterator result object
     * @see GeneratorAbstractOperations#GeneratorResume(ExecutionContext, Object, Object)
     */
    ScriptObject resume(ExecutionContext cx, Object value) {
        GeneratorState state = this.state;
        if (state == null) {
            // uninitialised generator object
            throw newTypeError(cx, Messages.Key.UninitialisedObject);
        }
        switch (state) {
        case Executing:
            throw newTypeError(cx, Messages.Key.GeneratorExecuting);
        case Completed:
            return CreateIterResultObject(cx, UNDEFINED, true);
        case SuspendedStart:
            if (value != UNDEFINED) {
                throw newTypeError(cx, Messages.Key.GeneratorNewbornSend);
            }
            this.state = GeneratorState.Executing;
            return generator.start(cx);
        case SuspendedYield:
        default:
            this.state = GeneratorState.Executing;
            return generator.resume(cx, value);
        }
    }

    /**
     * @param cx
     *            the execution context
     * @param value
     *            the exception value
     * @return the iterator result object
     * @see GeneratorPrototype.Properties#_throw(ExecutionContext, Object, Object)
     */
    ScriptObject _throw(ExecutionContext cx, Object value) {
        GeneratorState state = this.state;
        if (state == null) {
            // uninitialised generator object
            throw newTypeError(cx, Messages.Key.UninitialisedObject);
        }
        switch (state) {
        case Executing:
            throw newTypeError(cx, Messages.Key.GeneratorExecuting);
        case Completed:
            throw ScriptException.create(value);
        case SuspendedStart:
            close();
            throw ScriptException.create(value);
        case SuspendedYield:
        default:
            this.state = GeneratorState.Executing;
            return generator._throw(cx, ScriptException.create(value));
        }
    }

    /**
     * @param value
     *            the iteration result object to yield
     * @return the yield result
     * @see GeneratorAbstractOperations#GeneratorYield(ExecutionContext, ScriptObject)
     */
    Object yield(ScriptObject value) {
        assert state == GeneratorState.Executing : "yield from: " + state;
        return generator.yield(value);
    }

    /**
     * Generator implementation abstraction
     */
    private interface Generator {
        /**
         * Starts generator execution.
         * 
         * @param cx
         *            the execution context
         * @return the iterator result object
         */
        ScriptObject start(ExecutionContext cx);

        /**
         * Resumes generator execution.
         * 
         * @param cx
         *            the execution context
         * @param value
         *            the resumption value
         * @return the iterator result object
         */
        ScriptObject resume(ExecutionContext cx, Object value);

        /**
         * Resumes generator execution with an exception.
         * 
         * @param cx
         *            the execution context
         * @param exception
         *            the exception object
         * @return the iterator result object
         */
        ScriptObject _throw(ExecutionContext cx, ScriptException exception);

        /**
         * Yield support method (optional)
         * 
         * @param value
         *            the iterator result object
         * @return the yield value
         */
        Object yield(ScriptObject value);
    }

    /**
     * ResumptionPoint-based generator implementation
     */
    private static final class ResumeGenerator implements Generator {
        private final GeneratorObject generatorObject;
        private ResumptionPoint resumptionPoint = null;

        public ResumeGenerator(GeneratorObject generatorObject) {
            this.generatorObject = generatorObject;
        }

        @Override
        public ScriptObject start(ExecutionContext cx) {
            assert resumptionPoint == null;
            return execute(cx);
        }

        @Override
        public ScriptObject resume(ExecutionContext cx, Object value) {
            assert resumptionPoint != null && value != null;
            resumptionPoint.getStack()[0] = value;
            return execute(cx);
        }

        @Override
        public ScriptObject _throw(ExecutionContext cx, ScriptException exception) {
            assert resumptionPoint != null && exception != null;
            resumptionPoint.getStack()[0] = exception;
            return execute(cx);
        }

        @Override
        public Object yield(ScriptObject value) {
            // implemented in generated code
            throw new IllegalStateException();
        }

        private ResumptionPoint getResumptionPoint() {
            ResumptionPoint point = resumptionPoint;
            resumptionPoint = null;
            return point;
        }

        private ScriptObject execute(ExecutionContext cx) {
            GeneratorObject genObject = generatorObject;
            Object result;
            try {
                result = evaluate(genObject.code.handle(), genObject.context, getResumptionPoint());
            } catch (Throwable t) {
                genObject.close();
                throw t;
            }
            if (result instanceof ResumptionPoint) {
                genObject.suspend();
                resumptionPoint = (ResumptionPoint) result;
                Object[] stack = resumptionPoint.getStack();
                assert stack.length != 0 && stack[0] instanceof ScriptObject;
                return (ScriptObject) stack[0];
            }
            genObject.close();
            return CreateIterResultObject(cx, result, true);
        }

        private Object evaluate(MethodHandle handle, ExecutionContext cx, ResumptionPoint point) {
            try {
                return handle.invokeExact(cx, point);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Thread-based generator implementation
     */
    private static final class ThreadGenerator implements Generator {
        private static final Object COMPLETED = new Object();
        private final GeneratorObject generatorObject;
        private Future<Object> future;
        private SynchronousQueue<Object> out;
        private SynchronousQueue<Object> in;

        public ThreadGenerator(GeneratorObject generatorObject) {
            this.generatorObject = generatorObject;
        }

        @Override
        public ScriptObject start(ExecutionContext cx) {
            start0();
            return execute0(cx);
        }

        @Override
        public ScriptObject resume(ExecutionContext cx, Object value) {
            resume0(value);
            return execute0(cx);
        }

        @Override
        public ScriptObject _throw(ExecutionContext cx, ScriptException exception) {
            resume0(exception);
            return execute0(cx);
        }

        @Override
        public Object yield(ScriptObject value) {
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
            in = new SynchronousQueue<>();
            out = new SynchronousQueue<>();
            ExecutorService executor = Executors
                    .newSingleThreadExecutor(newGeneratorThreadFactory());
            future = executor.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    Object result;
                    try {
                        result = evaluate(generatorObject.code.handle(), generatorObject.context);
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

        private Object evaluate(MethodHandle handle, ExecutionContext cx) {
            try {
                return handle.invokeExact(cx, (ResumptionPoint) null);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        private void resume0(Object value) {
            try {
                in.put(value);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private ScriptObject execute0(ExecutionContext cx) {
            if (!future.isDone()) {
                Object result;
                try {
                    result = out.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (result != COMPLETED) {
                    generatorObject.suspend();
                    return (ScriptObject) result;
                }
            }

            generatorObject.close();
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
}
