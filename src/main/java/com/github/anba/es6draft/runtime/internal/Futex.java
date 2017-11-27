/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.github.anba.es6draft.runtime.objects.atomics.SharedByteBuffer;

/**
 * <h1>Shared Memory and Atomics</h1><br>
 * <h2>The Atomics Object</h2>
 * <ul>
 * <li>Runtime semantics
 * </ul>
 */
public final class Futex {
    private static final class Entry {
        private final Condition condition;
        private final SharedByteBuffer buffer;
        private final int index;

        Entry(Condition condition, SharedByteBuffer buffer, int index) {
            this.condition = condition;
            this.buffer = buffer;
            this.index = index;
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
     *            the optional timeout or {@code -1}
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
            Futex.Entry entry = new Entry(lock.newCondition(), buffer, index);
            queue.add(entry);
            if (entry.condition.await(timeout, timeUnit)) {
                return State.OK;
            }
            queue.remove(entry);
            return State.Timedout;
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
     *            the optional timeout or {@code -1}
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
            Futex.Entry entry = new Entry(lock.newCondition(), buffer, index);
            queue.add(entry);
            if (entry.condition.await(timeout, timeUnit)) {
                return State.OK;
            }
            queue.remove(entry);
            return State.Timedout;
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
                    entry.condition.signal();
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
