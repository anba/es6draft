/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.github.anba.es6draft.runtime.internal.JobSource;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.Ref;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.UnhandledRejectionException;
import com.github.anba.es6draft.runtime.internal.WeakReferenceWithFinalizer;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.2 Code Realms
 * <li>8.4 Jobs and Job Queues
 * </ul>
 */
public final class World {
    private final RuntimeContext context;
    private final ModuleLoader moduleLoader;
    private final ScriptLoader scriptLoader;
    private final Messages messages;

    private final GlobalSymbolRegistry symbolRegistry = new GlobalSymbolRegistry();
    private final ArrayDeque<Job> scriptJobs = new ArrayDeque<>();
    private final ArrayDeque<Job> promiseJobs = new ArrayDeque<>();
    private final ArrayDeque<Job> finalizerJobs = new ArrayDeque<>();
    private final ConcurrentLinkedDeque<Job> asyncJobs = new ConcurrentLinkedDeque<>();
    private final ArrayDeque<Object> unhandledRejections = new ArrayDeque<>();

    private final WeakHashMap<Object, ScriptObject> reachabilityMap = new WeakHashMap<>();
    private final ReferenceQueue<ScriptObject> weakQueue = new ReferenceQueue<>();

    private ExecutionContext scriptContext;

    private static final JobSource EMPTY_JOB_SOURCE = new JobSource() {
        @Override
        public Job nextJob() {
            return null;
        }

        @Override
        public Job awaitJob() {
            throw new IllegalStateException();
        }
    };

    /**
     * Creates a new {@link World} object.
     * 
     * @param context
     *            the runtime context
     */
    public World(RuntimeContext context) {
        this.context = context;
        this.scriptLoader = new ScriptLoader(context);
        this.moduleLoader = context.getModuleLoader().apply(context, scriptLoader);
        this.messages = Messages.create(context.getLocale());
    }

    /**
     * Returns the runtime context.
     * 
     * @return the runtime context
     */
    public RuntimeContext getRuntimeContext() {
        return context;
    }

    /**
     * Returns the module loader.
     * 
     * @return the module loader
     */
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Returns the script loader.
     * 
     * @return the script loader
     */
    public ScriptLoader getScriptLoader() {
        return scriptLoader;
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending script-jobs.
     * 
     * @param job
     *            the new script job
     */
    public void enqueueScriptJob(Job job) {
        scriptJobs.offer(job);
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending promise-jobs.
     * 
     * @param job
     *            the new promise job
     */
    public void enqueuePromiseJob(Job job) {
        promiseJobs.offer(job);
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending finalizer-jobs.
     * 
     * @param job
     *            the new finalizer job
     */
    public void enqueueFinalizerJob(Job job) {
        finalizerJobs.offer(job);
    }

    /**
     * 8.4.1 EnqueueJob (queueName, job, arguments)
     * <p>
     * Enqueues {@code job} to the queue of pending async-jobs.
     * 
     * @param job
     *            the new async job
     */
    public void enqueueAsyncJob(Job job) {
        asyncJobs.offer(job);
    }

    /**
     * Enqueue a promise rejection reason to the global rejection list.
     * 
     * @param reason
     *            the promise rejection reason
     */
    public void enqueueUnhandledPromiseRejection(Object reason) {
        unhandledRejections.offer(reason);
    }

    /**
     * Executes the queue of pending jobs.
     */
    public void runEventLoop() {
        try {
            runEventLoop(EMPTY_JOB_SOURCE);
        } catch (InterruptedException e) {
            // The empty job source never throws InterruptedException.
            throw new AssertionError(e);
        }
    }

    /**
     * Executes the queue of pending jobs.
     * 
     * @param jobSource
     *            the job source
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public void runEventLoop(JobSource jobSource) throws InterruptedException {
        ArrayDeque<Job> scriptJobs = this.scriptJobs;
        ArrayDeque<Job> promiseJobs = this.promiseJobs;
        ArrayDeque<Job> finalizerJobs = this.finalizerJobs;
        ConcurrentLinkedDeque<Job> asyncJobs = this.asyncJobs;
        ArrayDeque<Object> unhandledRejections = this.unhandledRejections;
        for (;;) {
            while (!(scriptJobs.isEmpty() && promiseJobs.isEmpty() && finalizerJobs.isEmpty() && asyncJobs.isEmpty())) {
                executeJobs(scriptJobs);
                executeJobs(promiseJobs);
                executeJobs(finalizerJobs);
                executeJobs(asyncJobs);
            }
            if (!unhandledRejections.isEmpty()) {
                throw new UnhandledRejectionException(unhandledRejections.poll());
            }
            Job job = jobSource.nextJob();
            if (job == null) {
                break;
            }
            enqueueScriptJob(job);
        }
    }

    private void executeJobs(Deque<Job> jobs) {
        // Execute all pending jobs until the queue is empty
        for (Job job; (job = jobs.poll()) != null;) {
            job.execute();
            enqueueWeakFinalizers();
        }
    }

    private void enqueueWeakFinalizers() {
        // Clear any strong references.
        WeakHashMap<?, ?> strongRefs = this.reachabilityMap;
        if (!strongRefs.isEmpty()) {
            strongRefs.clear();
        }

        // Enqueue finalizer jobs.
        ReferenceQueue<ScriptObject> weakQueue = this.weakQueue;
        ArrayDeque<Job> finalizerJobs = this.finalizerJobs;
        for (Reference<? extends ScriptObject> ref; (ref = weakQueue.poll()) != null;) {
            Ref<Runnable> finalizer = ((WeakReferenceWithFinalizer) ref).getFinalizer();
            if (finalizer.get() != null) {
                finalizerJobs.add(new FinalizerJob(finalizer));
            }
        }
    }

    private static final class FinalizerJob implements Job {
        private Ref<Runnable> finalizer;

        FinalizerJob(Ref<Runnable> finalizer) {
            this.finalizer = finalizer;
        }

        @Override
        public void execute() {
            Runnable finalizer = this.finalizer.get();
            if (finalizer != null) {
                this.finalizer.clear();
                this.finalizer = null;
                finalizer.run();
            }
        }
    }

    /**
     * Returns the localised message for {@code key}.
     * 
     * @param key
     *            the message key
     * @return the localised message
     */
    public String message(Messages.Key key) {
        return messages.getMessage(key);
    }

    /**
     * Returns the localised message for {@code key}.
     * 
     * @param key
     *            the message key
     * @param args
     *            the message arguments
     * @return the localised message
     */
    public String message(Messages.Key key, String... args) {
        return messages.getMessage(key, args);
    }

    /**
     * Returns the global symbol registry.
     * 
     * @return the global symbol registry
     */
    public GlobalSymbolRegistry getSymbolRegistry() {
        return symbolRegistry;
    }

    /**
     * Creates a new weak reference
     * 
     * @param target
     *            the target object
     * @param finalizer
     *            the optional finalizer or {@code null}
     * @return the new weak reference
     */
    public WeakReference<ScriptObject> makeWeakRef(ScriptObject target, Runnable finalizer) {
        return new WeakReferenceWithFinalizer(target, finalizer, weakQueue);
    }

    /**
     * Ensure {@code target} is reachable as long as {@code key} is reachable.
     * 
     * @param key
     *            the key
     * @param target
     *            target object
     */
    public void ensureReachable(Object key, ScriptObject target) {
        reachabilityMap.put(key, target);
    }

    /**
     * Returns the current script execution context for this object.
     * 
     * @return the current script execution context
     */
    public ExecutionContext getScriptContext() {
        return scriptContext;
    }

    /**
     * Sets a new script execution context for this object.
     * 
     * @param scriptContext
     *            the new script execution context
     */
    public void setScriptContext(ExecutionContext scriptContext) {
        this.scriptContext = scriptContext;
    }
}
