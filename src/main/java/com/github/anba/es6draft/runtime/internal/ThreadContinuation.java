/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * Thread-based continuation implementation.
 */
public final class ThreadContinuation<VALUE> implements Continuation<VALUE> {
    private static final Object COMPLETED = new Object();
    private final Continuation.Handler<VALUE> handler;
    private Future<Object> future;
    private SynchronousQueue<Object> out;
    private SynchronousQueue<Object> in;

    public ThreadContinuation(Continuation.Handler<VALUE> handler) {
        this.handler = handler;
    }

    @Override
    public VALUE start(ExecutionContext cx) {
        prepareStart();
        return execute(cx);
    }

    @Override
    public VALUE resume(ExecutionContext cx, Object value) {
        prepareResume(value);
        return execute(cx);
    }

    @Override
    public VALUE _return(ExecutionContext cx, Object value) {
        prepareResume(new ReturnValue(value));
        return execute(cx);
    }

    @Override
    public VALUE _throw(ExecutionContext cx, ScriptException exception) {
        prepareResume(exception);
        return execute(cx);
    }

    @Override
    public Object suspend(VALUE value) throws ReturnValue {
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    this.out.put(value);
                    Object resumptionValue = this.in.take();
                    if (resumptionValue instanceof ScriptException) {
                        throw (ScriptException) resumptionValue;
                    }
                    if (resumptionValue instanceof ReturnValue) {
                        throw (ReturnValue) resumptionValue;
                    }
                    return resumptionValue;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void prepareStart() {
        in = new SynchronousQueue<>();
        out = new SynchronousQueue<>();
        future = GeneratorThread.submit(new Callable<Object>() {
            @Override
            public Object call() throws InterruptedException {
                Object result;
                try {
                    result = handler.evaluate(null);
                } catch (ScriptException e) {
                    result = e;
                } catch (ReturnValue e) {
                    result = e.getValue();
                } catch (Throwable e) {
                    out.put(COMPLETED);
                    throw ThreadContinuation.<RuntimeException> rethrow(e);
                }
                out.put(COMPLETED);
                return result;
            }
        });
    }

    private void prepareResume(Object value) {
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    in.put(value);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private VALUE execute(ExecutionContext cx) {
        Future<Object> f = future;
        if (!f.isDone()) {
            boolean interrupted = false;
            Object result;
            try {
                for (;;) {
                    try {
                        result = out.take();
                        break;
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            if (result != COMPLETED) {
                return handler.suspendWith(cx, result);
            }
        }
        handler.close();

        Object result;
        try {
            result = f.get();
        } catch (ExecutionException e) {
            throw ThreadContinuation.<RuntimeException> rethrow(e.getCause());
        } catch (InterruptedException e) {
            // The future already completed its task, throwing InterruptedException shouldn't be possible.
            throw new AssertionError(e);
        }
        if (result instanceof ScriptException) {
            return handler.returnWith(cx, (ScriptException) result);
        }
        return handler.returnWith(cx, result);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }
}
