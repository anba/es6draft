/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.github.anba.es6draft.runtime.objects.atomics.SharedByteBuffer;

/**
 * <h1>Shared Memory and Atomics</h1><br>
 * <h2>The Atomics Object</h2>
 * <ul>
 * <li>Runtime semantics
 * </ul>
 */
public final class Futex {
    private static final class FutexWaiter {
        private static final ScheduledThreadPoolExecutor INSTANCE;
        static {
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, new ThreadFactoryImpl());
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            executor.setRemoveOnCancelPolicy(true);
            INSTANCE = executor;
        }

        static ScheduledFuture<?> wait(Runnable command, long timeout, TimeUnit timeUnit) {
            return INSTANCE.schedule(command, timeout, timeUnit);
        }

        private static final class ThreadFactoryImpl implements ThreadFactory {
            private final ThreadGroup group;

            private ThreadFactoryImpl() {
                SecurityManager sec = System.getSecurityManager();
                group = sec != null ? sec.getThreadGroup() : Thread.currentThread().getThreadGroup();
            }

            @Override
            public Thread newThread(Runnable r) {
                Thread newThread = new Thread(group, r, "futex-waiter");
                if (!newThread.isDaemon()) {
                    newThread.setDaemon(true);
                }
                if (newThread.getPriority() != Thread.NORM_PRIORITY) {
                    newThread.setPriority(Thread.NORM_PRIORITY);
                }
                return newThread;
            }
        }
    }

    private static final class Entry {
        private final Runnable onWake;
        private final SharedByteBuffer buffer;
        private final int index;

        Entry(Condition condition, SharedByteBuffer buffer, int index) {
            this.onWake = () -> condition.signal();
            this.buffer = buffer;
            this.index = index;
        }

        Entry(Function<Entry, Runnable> schedule, SharedByteBuffer buffer, int index) {
            this.onWake = schedule.apply(this);
            this.buffer = buffer;
            this.index = index;
        }

        void wake() {
            onWake.run();
        }

        boolean matches(SharedByteBuffer buffer, int index) {
            return this.buffer.sameData(buffer) && this.index == index;
        }
    }

    // FIXME: spec issue - define fairness property for 'futex critical section'?
    private final ReentrantLock lock = new ReentrantLock(true);
    private final LinkedList<Futex.Entry> queue = new LinkedList<>();

    /**
     * Result enumeration for {@link Futex#wait(SharedByteBuffer, int, int, long, TimeUnit)}.
     */
    public enum State {
        OK, NotEqual, Timedout;
    }

    private Futex.State enqueueAndWait(SharedByteBuffer buffer, int index, long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        assert lock.isHeldByCurrentThread();

        Condition condition = lock.newCondition();
        Futex.Entry entry = new Entry(condition, buffer, index);
        queue.add(entry);
        if (condition.await(timeout, timeUnit)) {
            return State.OK;
        }
        queue.remove(entry);
        return State.Timedout;
    }

    private void enqueueAndWait(SharedByteBuffer buffer, int index, long timeout, TimeUnit timeUnit,
            BiConsumer<Futex.State, ? super Throwable> action) throws InterruptedException {
        assert lock.isHeldByCurrentThread();

        queue.add(new Entry(entry -> {
            ScheduledFuture<?> sf = FutexWaiter.wait(() -> {
                lock.lock();
                try {
                    queue.remove(entry);
                } finally {
                    lock.unlock();
                }
                action.accept(State.Timedout, null);
            }, timeout, timeUnit);

            return () -> {
                sf.cancel(false);
                action.accept(State.OK, null);
            };
        }, buffer, index));
    }

    /**
     * Causes the current agent to wait until it is waken or the timeout elapses.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the expected byte buffer value
     * @param timeout
     *            the timeout in milli-seconds
     * @param timeUnit
     *            the time unit of the timeout parameter
     * @return the result value
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public Futex.State wait(SharedByteBuffer buffer, int index, int value, long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        lock.lock();
        try {
            int w = UnsafeHolder.getIntVolatile(buffer.get(), index);
            if (w != value) {
                return State.NotEqual;
            }
            return enqueueAndWait(buffer, index, timeout, timeUnit);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Causes the current agent to wait until it is waken or the timeout elapses.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the expected byte buffer value
     * @param timeout
     *            the timeout in milli-seconds
     * @param timeUnit
     *            the time unit of the timeout parameter
     * @return the result value
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public Futex.State wait(SharedByteBuffer buffer, int index, long value, long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        lock.lock();
        try {
            long w = UnsafeHolder.getLongVolatile(buffer.get(), index);
            if (w != value) {
                return State.NotEqual;
            }
            return enqueueAndWait(buffer, index, timeout, timeUnit);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Causes the current agent to wait asynchronously until it is waken or the timeout elapses.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the expected byte buffer value
     * @param timeout
     *            the timeout in milli-seconds
     * @param timeUnit
     *            the time unit of the timeout parameter
     * @param action
     * @return
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public Futex.State waitAsync(SharedByteBuffer buffer, int index, int value, long timeout, TimeUnit timeUnit,
            BiConsumer<Futex.State, ? super Throwable> action) throws InterruptedException {
        lock.lock();
        try {
            int w = UnsafeHolder.getIntVolatile(buffer.get(), index);
            if (w != value) {
                return State.NotEqual;
            }
            enqueueAndWait(buffer, index, timeout, timeUnit, action);
            return State.OK;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Causes the current agent to wait asynchronously until it is waken or the timeout elapses.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param value
     *            the expected byte buffer value
     * @param timeout
     *            the timeout in milli-seconds
     * @param timeUnit
     *            the time unit of the timeout parameter
     * @param action
     * @return
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public Futex.State waitAsync(SharedByteBuffer buffer, int index, long value, long timeout, TimeUnit timeUnit,
            BiConsumer<Futex.State, ? super Throwable> action) throws InterruptedException {
        lock.lock();
        try {
            long w = UnsafeHolder.getLongVolatile(buffer.get(), index);
            if (w != value) {
                return State.NotEqual;
            }
            enqueueAndWait(buffer, index, timeout, timeUnit, action);
            return State.OK;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wakes up a number of currently waiting agents.
     * 
     * @param buffer
     *            the byte buffer
     * @param index
     *            the byte buffer index
     * @param count
     *            the maximum number of agents to wake
     * @return the actual number of agents awoken
     */
    public int wake(SharedByteBuffer buffer, int index, int count) {
        lock.lock();
        try {
            int n = 0;
            for (Iterator<Futex.Entry> it = queue.iterator(); count > 0 && it.hasNext();) {
                Futex.Entry entry = it.next();
                if (entry.matches(buffer, index)) {
                    it.remove();
                    entry.wake();
                    count -= 1;
                    n += 1;
                }
            }
            return n;
        } finally {
            lock.unlock();
        }
    }
}
