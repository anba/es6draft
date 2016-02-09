/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.Realm.InitializeHostDefinedRealm;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.TaskSource;
import com.github.anba.es6draft.runtime.internal.UnhandledRejectionException;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;

/**
 * <h1>8 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>8.2 Code Realms
 * <li>8.4 Tasks and Task Queues
 * </ul>
 */
public final class World {
    private final RuntimeContext context;
    private final ModuleLoader moduleLoader;
    private final ScriptLoader scriptLoader;
    private final Messages messages;

    private final GlobalSymbolRegistry symbolRegistry = new GlobalSymbolRegistry();
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
    public RuntimeContext getContext() {
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
     * Checks whether there are any pending tasks.
     * 
     * @return {@code true} if there are any pending tasks
     */
    public boolean hasPendingTasks() {
        return !(scriptTasks.isEmpty() && promiseTasks.isEmpty());
    }

    /**
     * 8.4.1 EnqueueTask ( queueName, task, arguments)
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
     * 8.4.1 EnqueueTask ( queueName, task, arguments)
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
     */
    public void runEventLoop() {
        try {
            runEventLoop(EMPTY_TASK_SOURCE);
        } catch (InterruptedException e) {
            // The empty task source never throws InterruptedException.
            throw new AssertionError(e);
        }
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
     * Tests whether the requested compatibility option is enabled for this instance.
     * 
     * @param option
     *            the compatibility option
     * @return {@code true} if the compatibility option is enabled
     */
    public boolean isEnabled(CompatibilityOption option) {
        return context.getOptions().contains(option);
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
     * Creates a new {@link Realm} object.
     * 
     * @return the new realm object
     */
    public Realm newRealm() {
        return new Realm(this);
    }

    /**
     * Creates a new, initialized {@link Realm} object.
     * 
     * @return the new realm object
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             the URL is not a valid URI
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Realm newInitializedRealm() throws ParserException, CompilationException, IOException, URISyntaxException {
        Realm realm = new Realm(this);
        InitializeHostDefinedRealm(realm);
        return realm;
    }
}
