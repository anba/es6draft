/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.4 Jobs and Job Queues
 * </ul>
 * <p>
 * Interface for {@link Job} objects
 */
@FunctionalInterface
public interface Job {
    /**
     * Executes the action for this job.
     */
    void execute();
}
