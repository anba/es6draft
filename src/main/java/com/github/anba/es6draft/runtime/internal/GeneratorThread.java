/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Thread} subclass used for Generators and Generator Comprehensions.
 */
public final class GeneratorThread extends Thread {
    private static final int MAX_STACK_DEPTH = 1000;
    private final Thread parent;
    private final int stackDepth;

    private GeneratorThread(Thread parent, int stackDepth, Runnable target, String name) {
        super(target, name);
        this.parent = parent;
        this.stackDepth = stackDepth;
    }

    /**
     * Returns the calling thread which instantiated this thread.
     * 
     * @return the parent thread
     */
    public Thread getParent() {
        return parent;
    }

    /**
     * Creates a new {@link ThreadFactory} to create {@link GeneratorThread} objects.
     * 
     * @return the new thread factory instance
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
            int depth = stackDepthOrThrow(parent);
            String name = "generator-thread-" + threads.incrementAndGet();
            Thread newThread = new GeneratorThread(parent, depth, r, name);
            if (newThread.isDaemon()) {
                newThread.setDaemon(false);
            }
            if (newThread.getPriority() != Thread.NORM_PRIORITY) {
                newThread.setPriority(Thread.NORM_PRIORITY);
            }
            return newThread;
        }

        static int stackDepthOrThrow(Thread parent) {
            if (parent instanceof GeneratorThread) {
                int parentDepth = ((GeneratorThread) parent).stackDepth;
                if (parentDepth >= MAX_STACK_DEPTH) {
                    throw new StackOverflowError();
                }
                return parentDepth + 1;
            } else {
                return 1;
            }
        }
    }
}
