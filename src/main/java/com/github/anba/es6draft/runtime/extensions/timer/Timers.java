/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.extensions.timer;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToFlatString;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.internal.TaskSource;
import com.github.anba.es6draft.runtime.types.Callable;

/**
 * Simple <code>Timers</code> implementation.
 * 
 * @see <a
 *      href="http://www.whatwg.org/specs/web-apps/current-work/multipage/webappapis.html#timers">Web
 *      application APIs - Timers</a>
 */
public final class Timers implements TaskSource {
    private static final int MAX_TIMEOUT_NESTING = 5;
    private static final int TIMER_CLAMP_TIMEOUT = 0;
    private static final int TIMER_CLAMP_INTERVAL = 4;
    private static final int MAX_TIMEOUT = Integer.MAX_VALUE;
    private final AtomicInteger timerIds = new AtomicInteger();
    private final DelayQueue<TimerTask> queue = new DelayQueue<>();
    private final ConcurrentHashMap<Integer, TimerTask> activeTimers = new ConcurrentHashMap<>(16, 0.75f, 2);
    private int nestingLevel = 0;

    private abstract class TimerTask implements Task, Delayed {
        private final int timerId;
        private final long delay;
        private final boolean interval;
        private boolean cancelled = false;
        private long time;

        protected TimerTask(long delay, boolean interval) {
            this.timerId = timerIds.incrementAndGet();
            this.delay = delay;
            this.interval = interval;
            this.time = nextStart();
        }

        private long nextStart() {
            return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(delay);
        }

        int getTimerId() {
            return timerId;
        }

        boolean isInterval() {
            return interval;
        }

        void cancel() {
            if (!cancelled) {
                cancelled = true;
                queue.remove(this);
            }
        }

        @Override
        public int compareTo(Delayed o) {
            if (o == this) {
                return 0;
            }
            if (o instanceof TimerTask) {
                TimerTask x = (TimerTask) o;
                long delta = time - x.time;
                return delta < 0 ? -1 : delta > 0 ? 1 : timerId < x.timerId ? -1 : 1;
            }
            long delta = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
            return delta == 0 ? 0 : delta < 0 ? -1 : 1;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public final void execute() {
            if (!cancelled) {
                long nextStart = interval ? nextStart() : 0;
                try {
                    nestingLevel++;
                    executeInner();
                } finally {
                    nestingLevel--;
                    if (!cancelled && interval) {
                        time = nextStart;
                        queue.offer(this);
                    }
                }
            }
        }

        protected abstract void executeInner();
    }

    private final class CallableTimerTask extends TimerTask {
        private final ExecutionContext cx;
        private final Callable f;
        private final Object[] args;

        CallableTimerTask(long delay, boolean interval, ExecutionContext cx, Callable f, Object... args) {
            super(delay, interval);
            this.cx = cx;
            this.f = f;
            this.args = args;
        }

        @Override
        protected void executeInner() {
            f.call(cx, cx.getRealm().getGlobalThis(), args);
        }
    }

    private final class ScriptedTimerTask extends TimerTask {
        private final ExecutionContext cx;
        private final String sourceCode;

        ScriptedTimerTask(long delay, boolean interval, ExecutionContext cx, String sourceCode) {
            super(delay, interval);
            this.cx = cx;
            this.sourceCode = sourceCode;
        }

        @Override
        protected void executeInner() {
            Source source = new Source(cx.getRealm().sourceInfo(cx), "<Timer>", 1);
            Script script;
            try {
                script = cx.getRealm().getScriptLoader().script(source, sourceCode);
            } catch (ParserException | CompilationException e) {
                throw e.toScriptException(cx);
            }
            script.evaluate(cx);
        }
    }

    private TimerTask scheduleTimer(long delay, boolean interval, ExecutionContext cx, Object f, Object... args) {
        TimerTask task;
        if (IsCallable(f)) {
            task = new CallableTimerTask(delay, interval, cx, (Callable) f, args);
        } else {
            task = new ScriptedTimerTask(delay, interval, cx, ToFlatString(cx, f));
        }
        activeTimers.put(task.getTimerId(), task);
        queue.offer(task);
        return task;
    }

    private void cancelTimer(int timerId) {
        TimerTask task = activeTimers.remove(timerId);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public Task nextTask() throws InterruptedException {
        if (queue.isEmpty()) {
            return null;
        }
        return awaitTask();
    }

    @Override
    public Task awaitTask() throws InterruptedException {
        TimerTask task = queue.take();
        if (!task.isInterval()) {
            activeTimers.remove(task.getTimerId());
        }
        return task;
    }

    @Function(name = "setTimeout", arity = 2)
    public int setTimeout(ExecutionContext cx, Object f, double timeout, Object... args) {
        int delay = (int) Math.min(Math.max(timeout, TIMER_CLAMP_TIMEOUT), MAX_TIMEOUT);
        if (nestingLevel > MAX_TIMEOUT_NESTING) {
            delay = Math.max(delay, TIMER_CLAMP_INTERVAL);
        }
        TimerTask task = scheduleTimer(delay, false, cx, f, args);
        return task.getTimerId();
    }

    @Function(name = "setInterval", arity = 2)
    public int setInterval(ExecutionContext cx, Object f, double timeout, Object... args) {
        int delay = (int) Math.min(Math.max(timeout, TIMER_CLAMP_INTERVAL), MAX_TIMEOUT);
        TimerTask task = scheduleTimer(delay, true, cx, f, args);
        return task.getTimerId();
    }

    @Function(name = "clearTimeout", arity = 1)
    public void clearTimeout(ExecutionContext cx, int handle) {
        cancelTimer(handle);
    }

    @Function(name = "clearInterval", arity = 1)
    public void clearInterval(ExecutionContext cx, int handle) {
        cancelTimer(handle);
    }
}
