/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import com.github.anba.es6draft.runtime.types.Callable;

/**
 * <h1>25 Control Abstraction Objects</h1><br>
 * <h2>25.4 Promise Objects</h2><br>
 * <h3>25.4.1 Promise Abstract Operations</h3>
 * <ul>
 * <li>25.4.1.2 PromiseReaction Records
 * </ul>
 */
public final class PromiseReaction {
    /** [[Capabilities]] */
    private final PromiseCapability<?> capabilities;

    /** [[Handler]] */
    private final Callable handler;

    public PromiseReaction(PromiseCapability<?> capabilities, Callable handler) {
        this.capabilities = capabilities;
        this.handler = handler;
    }

    /**
     * Returns the [[Capabilities]] field of this PromiseReaction record.
     * 
     * @return the promise capability
     */
    public PromiseCapability<?> getCapabilities() {
        return capabilities;
    }

    /**
     * Returns the [[Handler]] field of this PromiseReaction record.
     * 
     * @return the handler function
     */
    public Callable getHandler() {
        return handler;
    }
}
