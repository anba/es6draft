/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * Interface for {@link Microtask} objects
 */
public interface Microtask {
    /**
     * Execute the action for this micro-task
     */
    void execute(ExecutionContext cx);
}
