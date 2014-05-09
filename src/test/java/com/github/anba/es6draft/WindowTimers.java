/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Undefined;

/**
 * Simple <code>setTimeout()</code> implementation for test cases.
 */
public final class WindowTimers {
    private AtomicInteger timerCount = new AtomicInteger(0);
    private LinkedBlockingQueue<Task> tasks = new LinkedBlockingQueue<Task>();

    private void enqueueTask(final ExecutionContext cx, final Callable f, final Object... args) {
        tasks.offer(new Task() {
            @Override
            public void execute() {
                f.call(cx, Undefined.UNDEFINED, args);
            }
        });
    }

    /**
     * Runs the event loop until all tasks have been finished.
     */
    public void runEventLoop(Realm realm) throws InterruptedException {
        for (;;) {
            realm.getWorld().executeTasks();
            Task task = nextTaskOrNull();
            if (task == null) {
                break;
            }
            realm.enqueueScriptTask(task);
        }
    }

    /**
     * Returns the next task or <code>null</code> if no tasks are waiting.
     */
    public Task nextTaskOrNull() throws InterruptedException {
        if (timerCount.get() == 0) {
            return null;
        }
        timerCount.decrementAndGet();
        return tasks.take();
    }

    @Properties.Function(name = "setTimeout", arity = 2)
    public void setTimeout(final ExecutionContext cx, final Callable f, double timeout,
            final Object... args) {
        long delay = (long) Math.min(Math.max(timeout, 0), TimeUnit.SECONDS.toMillis(10));
        timerCount.incrementAndGet();
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                enqueueTask(cx, f, args);
            }
        }, delay);
    }
}
