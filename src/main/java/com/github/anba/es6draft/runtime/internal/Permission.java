/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

/**
 * Extension: Frozen Realms
 */
public enum Permission {
    /**
     * Permission to call {@code Math.random()}.
     */
    RandomNumber,

    /**
     * Permission to call {@code Date.now()}.
     */
    CurrentTime,

    ;
}
