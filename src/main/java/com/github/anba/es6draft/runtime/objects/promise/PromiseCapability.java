/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2><br>
 * <h3>25.4.1 Promise Abstract Operations</h3>
 * <ul>
 * <li>25.4.1.1 PromiseCapability Records
 * </ul>
 */
public final class PromiseCapability {
    /** [[Promise]] */
    private final ScriptObject promise;

    /** [[Resolve]] */
    private final Callable resolve;

    /** [[Reject]] */
    private final Callable reject;

    /**
     * Creates a new PromiseCapability record
     */
    public PromiseCapability(ScriptObject promise, Callable resolve, Callable reject) {
        assert promise != null && resolve != null && reject != null;
        this.promise = promise;
        this.resolve = resolve;
        this.reject = reject;
    }

    /**
     * Returns the [[Promise]] field of this PromiseCapability record
     */
    public ScriptObject getPromise() {
        return promise;
    }

    /**
     * Returns the [[Resolve]] field of this PromiseCapability record
     */
    public Callable getResolve() {
        return resolve;
    }

    /**
     * Returns the [[Reject]] field of this PromiseCapability record
     */
    public Callable getReject() {
        return reject;
    }

    /**
     * 25.4.1.1.1 IfAbruptRejectPromise (value, capability)
     */
    public static ScriptObject IfAbruptRejectPromise(ExecutionContext cx, ScriptException e,
            PromiseCapability capability) {
        /* steps 1.a-1.b */
        capability.getReject().call(cx, UNDEFINED, e.getValue());
        /* step 1.c */
        return capability.getPromise();
    }
}
