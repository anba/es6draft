/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 */
final class RuntimeWorkerThreadFactory implements ThreadFactory {
    private static final int THREAD_POOL_SIZE = 2;
    private static final long THREAD_POOL_TTL = 5 * 60;
    private static final AtomicInteger runtimeCount = new AtomicInteger(0);

    private final AtomicInteger workerCount = new AtomicInteger(0);
    private final ThreadGroup group;
    private final String namePrefix;

    private RuntimeWorkerThreadFactory() {
        SecurityManager sec = System.getSecurityManager();
        group = sec != null ? sec.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = "runtime-" + runtimeCount.incrementAndGet() + "-worker-";
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = namePrefix + workerCount.incrementAndGet();
        Thread newThread = new Thread(group, r, name);
        if (!newThread.isDaemon()) {
            newThread.setDaemon(true);
        }
        if (newThread.getPriority() != Thread.NORM_PRIORITY) {
            newThread.setPriority(Thread.NORM_PRIORITY);
        }
        return newThread;
    }

    /**
     * Returns a new {@link ThreadPoolExecutor} to create runtime worker threads.
     * 
     * @return a new {@link ThreadPoolExecutor} for runtime worker threads
     */
    static ThreadPoolExecutor createThreadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE,
                THREAD_POOL_TTL, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new RuntimeWorkerThreadFactory());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
