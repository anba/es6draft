/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.TimeZone;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.TaskSource;
import com.github.anba.es6draft.runtime.internal.UnhandledRejectionException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.2 Code Realms
 * <li>8.4 Tasks and Task Queues
 * </ul>
 */
public final class World<GLOBAL extends GlobalObject> {
    private final ObjectAllocator<GLOBAL> allocator;
    private final ModuleLoader moduleLoader;
    private final ScriptLoader scriptLoader;
    private final Locale locale;
    private final TimeZone timeZone;
    private final Messages messages;
    private final GlobalSymbolRegistry symbolRegistry = new GlobalSymbolRegistry();

    // TODO: move to custom class
    private final ArrayDeque<Task> scriptTasks = new ArrayDeque<>();
    private final ArrayDeque<Task> promiseTasks = new ArrayDeque<>();
    private final ArrayDeque<Object> unhandledRejections = new ArrayDeque<>();

    private static final TaskSource EMPTY_TASK_SOURCE = new TaskSource() {
        @Override
        public Task nextTask() {
            return null;
        }

        @Override
        public Task awaitTask() {
            throw new IllegalStateException();
        }
    };

    private static final ObjectAllocator<GlobalObject> DEFAULT_GLOBAL_OBJECT = new ObjectAllocator<GlobalObject>() {
        @Override
        public GlobalObject newInstance(Realm realm) {
            return new GlobalObject(realm);
        }
    };

    /**
     * Returns an {@link ObjectAllocator} which creates standard {@link GlobalObject} instances.
     * 
     * @return the default global object allocator
     */
    public static ObjectAllocator<GlobalObject> getDefaultGlobalObjectAllocator() {
        return DEFAULT_GLOBAL_OBJECT;
    }

    /**
     * Creates a new {@link World} object.
     * 
     * @param allocator
     *            the global object allocator
     * @param moduleLoader
     *            the module loader
     * @param scriptLoader
     *            the script loader
     */
    public World(ObjectAllocator<GLOBAL> allocator, ModuleLoader moduleLoader,
            ScriptLoader scriptLoader) {
        this(allocator, moduleLoader, scriptLoader, Locale.getDefault(), TimeZone.getDefault());
    }

    /**
     * Creates a new {@link World} object.
     * 
     * @param allocator
     *            the global object allocator
     * @param moduleLoader
     *            the module loader
     * @param scriptLoader
     *            the script loader
     * @param locale
     *            the default locale
     * @param timeZone
     *            the default timezone
     */
    public World(ObjectAllocator<GLOBAL> allocator, ModuleLoader moduleLoader,
            ScriptLoader scriptLoader, Locale locale, TimeZone timeZone) {
        this.allocator = allocator;
        this.moduleLoader = moduleLoader;
        this.scriptLoader = scriptLoader;
        this.locale = locale;
        this.timeZone = timeZone;
        this.messages = Messages.create(locale);
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
     * Checks whether there are any pending tasks.
     * 
     * @return {@code true} if there are any pending tasks
     */
    public boolean hasPendingTasks() {
        return !(scriptTasks.isEmpty() && promiseTasks.isEmpty());
    }

    /**
     * 8.4.1 EnqueueTask ( queueName, task, arguments) Abstract Operation
     * <p>
     * Enqueues {@code task} to the queue of pending script-tasks.
     * 
     * @param task
     *            the new script task
     */
    public void enqueueScriptTask(Task task) {
        scriptTasks.offer(task);
    }

    /**
     * 8.4.1 EnqueueTask ( queueName, task, arguments) Abstract Operation
     * <p>
     * Enqueues {@code task} to the queue of pending promise-tasks.
     * 
     * @param task
     *            the new promise task
     */
    public void enqueuePromiseTask(Task task) {
        promiseTasks.offer(task);
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
     * Executes the queue of pending tasks.
     * 
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public void runEventLoop() throws InterruptedException {
        runEventLoop(EMPTY_TASK_SOURCE);
    }

    /**
     * Executes the queue of pending tasks.
     * 
     * @param taskSource
     *            the task source
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    public void runEventLoop(TaskSource taskSource) throws InterruptedException {
        ArrayDeque<Task> scriptTasks = this.scriptTasks, promiseTasks = this.promiseTasks;
        ArrayDeque<Object> unhandledRejections = this.unhandledRejections;
        for (;;) {
            while (!(scriptTasks.isEmpty() && promiseTasks.isEmpty())) {
                executeTasks(scriptTasks);
                executeTasks(promiseTasks);
            }
            if (!unhandledRejections.isEmpty()) {
                throw new UnhandledRejectionException(unhandledRejections.poll());
            }
            Task task = taskSource.nextTask();
            if (task == null) {
                break;
            }
            enqueueScriptTask(task);
        }
    }

    /**
     * Executes the queue of pending tasks.
     * 
     * @param tasks
     *            the tasks to be executed
     */
    private void executeTasks(ArrayDeque<Task> tasks) {
        // Execute all pending tasks until the queue is empty
        for (Task task; (task = tasks.poll()) != null;) {
            task.execute();
        }
    }

    /**
     * Returns this world's locale.
     * 
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns this world's timezone.
     * 
     * @return the timezone
     */
    public TimeZone getTimeZone() {
        return timeZone;
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
     * Returns the global object allocator for this instance.
     * 
     * @return the global object allocator
     */
    public ObjectAllocator<GLOBAL> getAllocator() {
        return allocator;
    }

    /**
     * Tests whether the requested compatibility option is enabled for this instance.
     * 
     * @param option
     *            the compatibility option
     * @return {@code true} if the compatibility option is enabled
     */
    public boolean isEnabled(CompatibilityOption option) {
        return scriptLoader.getOptions().contains(option);
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
     * Creates a new {@link Realm} object and returns its {@link GlobalObject}.
     * 
     * @return the new global object
     */
    public GLOBAL newGlobal() {
        Realm realm = Realm.newRealm(this);
        @SuppressWarnings("unchecked")
        GLOBAL global = (GLOBAL) realm.getGlobalObject();
        return global;
    }

    /**
     * Creates a new, initialized {@link Realm} object and returns its {@link GlobalObject}.
     * 
     * @return the new global object
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public GLOBAL newInitializedGlobal() throws ParserException, CompilationException, IOException,
            URISyntaxException {
        GLOBAL global = newGlobal();
        global.initializeFirstRealmGlobal();
        return global;
    }

    /**
     * Creates a new global object.
     * 
     * @param realm
     *            the realm instance
     * @return the new global object
     */
    /*package*/GLOBAL newGlobal(Realm realm) {
        return getAllocator().newInstance(realm);
    }
}
