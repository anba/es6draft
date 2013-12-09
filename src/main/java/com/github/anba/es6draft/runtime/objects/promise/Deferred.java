/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>Promise Objects</h1><br>
 * <h2>Record Types for Promise Objects</h2>
 * <ul>
 * <li>The Deferred Specification Type
 * </ul>
 */
public final class Deferred {
    /** [[Promise]] */
    private final ScriptObject promise;

    /** [[Resolve]] */
    private final Callable resolve;

    /** [[Reject]] */
    private final Callable reject;

    /**
     * Creates a new Deferred record
     */
    public Deferred(ScriptObject promise, Callable resolve, Callable reject) {
        assert promise != null && resolve != null && reject != null;
        this.promise = promise;
        this.resolve = resolve;
        this.reject = reject;
    }

    /**
     * Returns the [[Promise]] field of this Deferred record
     */
    public ScriptObject getPromise() {
        return promise;
    }

    /**
     * Returns the [[Resolve]] field of this Deferred record
     */
    public Callable getResolve() {
        return resolve;
    }

    /**
     * Returns the [[Reject]] field of this Deferred record
     */
    public Callable getReject() {
        return reject;
    }
}
