/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.iteration;

import static com.github.anba.es6draft.runtime.internal.Errors.throwTypeError;
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
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * 
 *
 */
public class GeneratorObject extends OrdinaryObject {
    /**
     * [[GeneratorState]]
     */
    private enum GeneratorState {
        Newborn, Executing, Suspended, Closed
    }

    /** [[GeneratorState]] */
    private GeneratorState state = GeneratorState.Newborn;

    /** [[Code]] */
    private RuntimeInfo.Code code;

    /** [[GeneratorContext]] */
    private ExecutionContext context;

    // internal
    private static final Object RETURN = new Object();
    private Future<Object> future;
    private SynchronousQueue<Object> out;
    private SynchronousQueue<Object> in;

    public GeneratorObject(Realm realm) {
        super(realm);
        this.in = new SynchronousQueue<>();
        this.out = new SynchronousQueue<>();
    }

    public GeneratorObject(Realm realm, RuntimeInfo.Code code, ExecutionContext context) {
        super(realm);
        this.code = code;
        this.context = context;
        this.in = new SynchronousQueue<>();
        this.out = new SynchronousQueue<>();
    }

    @SuppressWarnings("serial")
    private static class CloseGenerator extends RuntimeException {
    }

    private void start0() {
        this.state = GeneratorState.Executing;
        this.context.setCurrentGenerator(this);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object result;
                try {
                    result = code.evaluate(context);
                } catch (ScriptException | CloseGenerator e) {
                    result = e;
                } catch (Throwable t) {
                    out.put(RETURN);
                    throw t;
                }
                out.put(RETURN);
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
            if (result != RETURN) {
                state = GeneratorState.Suspended;
                return result;
            }
        }

        state = GeneratorState.Closed;
        try {
            Object result = future.get();
            if (result instanceof ScriptException) {
                throw (ScriptException) result;
            }
            if (result instanceof CloseGenerator) {
                return UNDEFINED;
            }
            // TODO: StopIteration with value
            // throw new StopIteration(result);
            return ScriptRuntime._throw(cx.getIntrinsic(Intrinsics.StopIteration));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * [[Send]]
     */
    public Object send(ExecutionContext cx, Object value) {
        GeneratorState state = this.state;
        switch (state) {
        case Executing:
            throw throwTypeError(cx, Messages.Key.GeneratorExecuting);
        case Closed:
            throw throwTypeError(cx, Messages.Key.GeneratorClosed);
        case Newborn: {
            if (value != UNDEFINED) {
                throw throwTypeError(cx, Messages.Key.GeneratorNewbornSend);
            }
            start0();
            return execute0(cx);
        }
        case Suspended:
        default: {
            resume0(value);
            return execute0(cx);
        }
        }
    }

    /**
     * [[Throw]]
     */
    public Object _throw(ExecutionContext cx, Object value) {
        GeneratorState state = this.state;
        switch (state) {
        case Executing:
            throw throwTypeError(cx, Messages.Key.GeneratorExecuting);
        case Closed:
            throw throwTypeError(cx, Messages.Key.GeneratorClosed);
        case Newborn: {
            this.state = GeneratorState.Closed;
            throw ScriptRuntime._throw(value);
        }
        case Suspended:
        default: {
            resume0(new ScriptException(value));
            return execute0(cx);
        }
        }
    }

    /**
     * [[Close]]
     */
    public Object close(ExecutionContext cx) {
        GeneratorState state = this.state;
        switch (state) {
        case Executing:
            throw throwTypeError(cx, Messages.Key.GeneratorExecuting);
        case Closed:
            return UNDEFINED;
        case Newborn: {
            this.state = GeneratorState.Closed;
            return UNDEFINED;
        }
        case Suspended:
        default: {
            resume0(new CloseGenerator());
            return execute0(cx);
        }
        }
    }

    /**
     * 13.4 Generator Definitions
     */
    public Object yield(Object value) {
        try {
            this.out.put(value);
            Object result = this.in.take();
            if (result instanceof ScriptException) {
                // [[Throw]]
                throw (ScriptException) result;
            }
            if (result instanceof CloseGenerator) {
                // [[Close]]
                throw (CloseGenerator) result;
            }
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
