/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * Bytecode-based continuation implementation. 
 */
public final class CodeContinuation<VALUE> implements Continuation<VALUE> {
    private final Continuation.Handler<VALUE> handler;
    private ResumptionPoint resumptionPoint;

    public CodeContinuation(Continuation.Handler<VALUE> handler) {
        this.handler = handler;
    }

    @Override
    public VALUE start(ExecutionContext cx) {
        assert resumptionPoint == null;
        return execute(cx, null);
    }

    @Override
    public VALUE resume(ExecutionContext cx, Object value) {
        return execute(cx, prepareResume(value));
    }

    @Override
    public VALUE _return(ExecutionContext cx, Object value) {
        return execute(cx, prepareResume(new ReturnValue(value)));
    }

    @Override
    public VALUE _throw(ExecutionContext cx, ScriptException exception) {
        return execute(cx, prepareResume(exception));
    }

    @Override
    public Object suspend(VALUE value) {
        // Context switch is implemented in generated code.
        return null;
    }

    private ResumptionPoint prepareResume(Object value) {
        assert value != null;
        ResumptionPoint rp = resumptionPoint;
        resumptionPoint = null;
        rp.setResumeValue(value);
        return rp;
    }

    private VALUE execute(ExecutionContext cx, ResumptionPoint resumptionPoint) {
        Object result;
        try {
            result = handler.evaluate(resumptionPoint);
        } catch (ScriptException e) {
            handler.close();
            return handler.returnWith(cx, e);
        } catch (Throwable e) {
            handler.close();
            throw CodeContinuation.<RuntimeException> rethrow(e);
        }
        if (result instanceof ResumptionPoint) {
            ResumptionPoint rp = (ResumptionPoint) result;
            this.resumptionPoint = rp;
            return handler.suspendWith(cx, rp.getSuspendValue());
        }
        handler.close();
        return handler.returnWith(cx, result);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }
}
