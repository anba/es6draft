/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.objects.promise;

import com.github.anba.es6draft.runtime.objects.zone.ZoneObject;
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
    /** [[Capability]] */
    private final PromiseCapability<?> capability;

    /** [[Type]] */
    private final Type type;

    /** [[Handler]] */
    private final Callable handler;

    /** [[Zone]] */
    private final ZoneObject zone;

    /**
     * Promise reaction handler type
     */
    public enum Type {
        /** Fulfill reaction handler */
        Fulfill,

        /** Reject reaction handler */
        Reject,
    }

    /**
     * Constructs a new Promise Reaction record.
     * 
     * @param capability
     *            the promise capabilities
     * @param type
     *            the reaction type
     * @param handler
     *            the reaction handle
     * @param zone
     *            the current zone
     */
    public PromiseReaction(PromiseCapability<?> capability, Type type, Callable handler, ZoneObject zone) {
        this.capability = capability;
        this.handler = handler;
        this.type = type;
        this.zone = zone;
    }

    /**
     * Returns the [[Capability]] field of this PromiseReaction record.
     * 
     * @return the promise capability
     */
    public PromiseCapability<?> getCapability() {
        return capability;
    }

    /**
     * Returns the promise reaction's type.
     * 
     * @return the promise reaction type
     */
    public Type getType() {
        return type;
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
     * Returns the [[Zone]] field of this PromiseReaction record.
     * 
     * @return the zone object
     */
    public ZoneObject getZone() {
        return zone;
    }
}
