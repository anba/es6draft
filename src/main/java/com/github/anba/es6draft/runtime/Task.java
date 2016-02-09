/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.4 Tasks and Task Queues
 * </ul>
 * <p>
 * Interface for {@link Task} objects
 */
@FunctionalInterface
public interface Task {
    // TODO: Rename to Job...

    /**
     * Executes the action for this task.
     */
    void execute();
}
