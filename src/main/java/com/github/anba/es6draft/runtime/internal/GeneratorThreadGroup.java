/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.concurrent.ThreadFactory;

/**
 * 
 * 
 */
public final class GeneratorThreadGroup extends ThreadGroup {
    private GeneratorThreadGroup(ThreadGroup parent, String name) {
        super(parent, name);
    }

    public static ThreadFactory newGeneratorThreadFactory() {
        return new ThreadGroupFactory();
    }

    private static final class ThreadGroupFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            // place each thread into a new group to be able to preserve stacktraces
            ThreadGroup parent = Thread.currentThread().getThreadGroup();
            ThreadGroup newGroup = new GeneratorThreadGroup(parent, "generator-group");
            Thread newThread = new Thread(newGroup, r, "generator-thread");
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
