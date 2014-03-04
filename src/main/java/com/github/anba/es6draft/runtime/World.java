/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.compiler.Compiler.Option;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.Intrinsics;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.2 Code Realms
 * <li>8.4 Tasks and Task Queues
 * </ul>
 */
public final class World<GLOBAL extends GlobalObject> {
    private static final int THREAD_POOL_SIZE = 2;
    private static final long THREAD_POOL_TTL = 5 * 60;

    private final ObjectAllocator<GLOBAL> allocator;
    private final ThreadPoolExecutor executor;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Option> compilerOptions;

    private final Locale locale = Locale.getDefault();
    private final TimeZone timezone = TimeZone.getDefault();
    private final Messages messages = Messages.create(locale);

    // TODO: move to custom class
    private final ArrayDeque<Task> loadingTasks = new ArrayDeque<>();
    private final ArrayDeque<Task> promiseTasks = new ArrayDeque<>();

    private final GlobalSymbolRegistry symbolRegistry = new GlobalSymbolRegistry();

    private static final ObjectAllocator<GlobalObject> DEFAULT_GLOBAL_OBJECT = new ObjectAllocator<GlobalObject>() {
        @Override
        public GlobalObject newInstance(Realm realm) {
            return new GlobalObject(realm);
        }
    };

    /**
     * Returns an {@link ObjectAllocator} which creates standard {@link GlobalObject} instances
     */
    public static ObjectAllocator<GlobalObject> getDefaultGlobalObjectAllocator() {
        return DEFAULT_GLOBAL_OBJECT;
    }

    /**
     * Creates a new {@link World} object with the default settings
     */
    public World(ObjectAllocator<GLOBAL> allocator) {
        this(allocator, CompatibilityOption.WebCompatibility(), EnumSet
                .noneOf(Compiler.Option.class));
    }

    /**
     * Creates a new {@link World} object
     */
    public World(ObjectAllocator<GLOBAL> allocator, Set<CompatibilityOption> options) {
        this(allocator, options, EnumSet.noneOf(Compiler.Option.class));
    }

    /**
     * Creates a new {@link World} object
     */
    public World(ObjectAllocator<GLOBAL> allocator, Set<CompatibilityOption> options,
            Set<Compiler.Option> compilerOptions) {
        this.allocator = allocator;
        this.executor = createThreadPoolExecutor();
        this.options = EnumSet.copyOf(options);
        this.compilerOptions = EnumSet.copyOf(compilerOptions);
    }

    @Override
    protected void finalize() throws Throwable {
        executor.shutdown();
        super.finalize();
    }

    /**
     * Returns the shared runtime executor
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Create a new executor service
     */
    private static ThreadPoolExecutor createThreadPoolExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE,
                THREAD_POOL_TTL, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new RuntimeThreadFactory());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static final class RuntimeThreadFactory implements ThreadFactory {
        private static final AtomicInteger runtimeCount = new AtomicInteger(0);
        private final AtomicInteger workerCount = new AtomicInteger(0);
        private final ThreadGroup group;
        private final String namePrefix;

        RuntimeThreadFactory() {
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
    }

    /**
     * Checks whether there are any pending tasks
     */
    public boolean hasPendingTasks() {
        return !(loadingTasks.isEmpty() && promiseTasks.isEmpty());
    }

    /**
     * 8.4.1 EnqueueTask ( queueName, task, arguments) Abstract Operation
     * <p>
     * Enqueues {@code task} to the queue of pending loading-tasks
     */
    public void enqueueLoadingTask(Task task) {
        loadingTasks.offer(task);
    }

    /**
     * 8.4.1 EnqueueTask ( queueName, task, arguments) Abstract Operation
     * <p>
     * Enqueues {@code task} to the queue of pending promise-tasks
     */
    public void enqueuePromiseTask(Task task) {
        promiseTasks.offer(task);
    }

    /**
     * Executes the queue of pending tasks
     */
    public void executeTasks() {
        while (hasPendingTasks()) {
            executeTasks(loadingTasks);
            executeTasks(promiseTasks);
        }
    }

    /**
     * Executes the queue of pending tasks
     */
    private void executeTasks(ArrayDeque<Task> tasks) {
        // execute all pending tasks until the queue is empty
        for (Task task; (task = tasks.poll()) != null;) {
            task.execute();
        }
    }

    /**
     * Returns this world's locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns this world's timezone
     */
    public TimeZone getTimezone() {
        return timezone;
    }

    /**
     * Returns the localised message for {@code key}
     */
    public String message(Messages.Key key) {
        return messages.getMessage(key);
    }

    /**
     * Returns the localised message for {@code key}
     */
    public String message(Messages.Key key, String... args) {
        return messages.getMessage(key, args);
    }

    /**
     * Returns the global object allocator for this instance
     */
    public ObjectAllocator<GLOBAL> getAllocator() {
        return allocator;
    }

    /**
     * Returns the compatibility options for this instance
     */
    public Set<CompatibilityOption> getOptions() {
        return options;
    }

    /**
     * Tests whether the requested compatibility option is enabled for this instance
     */
    public boolean isEnabled(CompatibilityOption option) {
        return options.contains(option);
    }

    /**
     * Returns the compiler options for this instance
     */
    public EnumSet<Option> getCompilerOptions() {
        return compilerOptions;
    }

    /**
     * Returns the global symbol registry
     */
    public GlobalSymbolRegistry getSymbolRegistry() {
        return symbolRegistry;
    }

    /**
     * Creates a new {@link Realm} object and returns its {@link GlobalObject}
     */
    public GLOBAL newGlobal() {
        Realm realm = Realm.newRealm(this);
        @SuppressWarnings("unchecked")
        GLOBAL global = (GLOBAL) realm.getGlobalThis();
        // implementation defined behaviour
        global.setPrototype(realm.getIntrinsic(Intrinsics.ObjectPrototype));
        realm.defineBuiltinProperties(global);
        return global;
    }
}
