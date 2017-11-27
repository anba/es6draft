/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.Job;

/**
 *
 */
public interface JobSource {
    /**
     * Returns the next job or {@code null} if no jobs are available.
     * 
     * @return the next job or {@code null} if none available
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    Job nextJob() throws InterruptedException;

    /**
     * Returns the next job, waiting if necessary until a new job is available.
     * 
     * @return the next job
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    Job awaitJob() throws InterruptedException;
}
