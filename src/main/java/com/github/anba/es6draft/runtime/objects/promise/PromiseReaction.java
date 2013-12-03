/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import com.github.anba.es6draft.runtime.types.Callable;

/**
 * <h1>Promise Objects</h1><br>
 * <h2>Record Types for Promise Objects</h2>
 * <ul>
 * <li>The PromiseReaction Specification Type
 * </ul>
 */
public final class PromiseReaction {
    /** [[Deferred]] */
    private final Deferred deferred;

    /** [[Handler]] */
    private final Callable handler;

    public PromiseReaction(Deferred deferred, Callable handler) {
        this.deferred = deferred;
        this.handler = handler;
    }

    /**
     * Returns the [[Deferred]] field of this PromiseReaction record
     */
    public Deferred getDeferred() {
        return deferred;
    }

    /**
     * Returns the [[Handler]] field of this PromiseReaction record
     */
    public Callable getHandler() {
        return handler;
    }
}
