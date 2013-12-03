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
    private ScriptObject promise;

    /** [[Resolve]] */
    private Callable resolve;

    /** [[Reject]] */
    private Callable reject;

    /**
     * Creates an empty Deferred record
     */
    public Deferred() {
    }

    /**
     * Returns the [[Promise]] field of this Deferred record
     */
    public ScriptObject getPromise() {
        assert promise != null;
        return promise;
    }

    /**
     * Sets the [[Promise]] field of this Deferred record to {@code promise}
     */
    public void setPromise(ScriptObject promise) {
        assert this.promise == null && promise != null;
        assert resolve != null && reject != null;
        this.promise = promise;
    }

    /**
     * Returns the [[Resolve]] field of this Deferred record
     */
    public Callable getResolve() {
        return resolve;
    }

    /**
     * Sets the [[Resolve]] field of this Deferred record to {@code resolve}
     */
    public void setResolve(Callable resolve) {
        // assert this.resolve == null && resolve != null;
        this.resolve = resolve;
    }

    /**
     * Returns the [[Reject]] field of this Deferred record
     */
    public Callable getReject() {
        return reject;
    }

    /**
     * Sets the [[Reject]] field of this Deferred record to {@code reject}
     */
    public void setReject(Callable reject) {
        // assert this.reject == null && reject != null;
        this.reject = reject;
    }
}
