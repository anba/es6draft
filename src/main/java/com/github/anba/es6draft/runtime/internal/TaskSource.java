/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.Task;

/**
 *
 */
public interface TaskSource {
    /**
     * Returns the next task or {@code null} if no tasks are available.
     * 
     * @return the next task or {@code null} if none available
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    Task nextTask() throws InterruptedException;

    /**
     * Returns the next task, waiting if necessary until a new task is available.
     * 
     * @return the next task
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    Task awaitTask() throws InterruptedException;
}
