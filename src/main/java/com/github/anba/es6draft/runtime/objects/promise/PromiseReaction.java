/**
 * Copyright (c) 2012-2015 André Bargull
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

    private final Type type;

    /**
     * Promise reaction handler type
     */
    public enum Type {
        /** Identity function reaction handler */
        Identity,

        /** Thrower function reaction handler */
        Thrower,

        /** User-defined reaction handler */
        Function
    }

    /**
     * Constructs a new Promise Reaction record.
     * 
     * @param capabilities
     *            the promise capabilities
     * @param handler
     *            the reaction handle
     * @param type
     *            the reaction type
     */
    public PromiseReaction(PromiseCapability<?> capabilities, Callable handler, Type type) {
        assert type == Type.Function ^ handler == null;
        this.capabilities = capabilities;
        this.handler = handler;
        this.type = type;
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

    /**
     * Returns the promise reaction's type.
     * 
     * @return the promise reaction type
     */
    public Type getType() {
        return type;
    }
}
