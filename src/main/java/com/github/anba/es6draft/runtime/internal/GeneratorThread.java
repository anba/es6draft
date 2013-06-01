/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Thread} subclass used for Generators and Generator Comprehensions
 */
public class GeneratorThread extends Thread {
    private final Thread parent;

    private GeneratorThread(Thread parent, Runnable target, String name) {
        super(target, name);
        this.parent = parent;
    }

    /**
     * Returns the calling thread which instantiated this thread
     */
    public Thread getParent() {
        return parent;
    }

    /**
     * Creates a new {@link ThreadFactory} to create {@link GeneratorThread} objects
     */
    public static ThreadFactory newGeneratorThreadFactory() {
        return new GeneratorThreadFactory();
    }

    private static final class GeneratorThreadFactory implements ThreadFactory {
        private static final AtomicInteger threads = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            // save caller thread to be able to restore stacktraces
            Thread parent = Thread.currentThread();
            String name = "generator-thread-" + threads.incrementAndGet();
            Thread newThread = new GeneratorThread(parent, r, name);
            if (newThread.isDaemon()) {
                newThread.setDaemon(false);
            }
            if (newThread.getPriority() != Thread.NORM_PRIORITY) {
                newThread.setPriority(Thread.NORM_PRIORITY);
            }
            return newThread;
        }
    }
}
