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

import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
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
    private final ScriptLoader scriptLoader;
    private final EnumSet<CompatibilityOption> options;

    private final Locale locale = Locale.getDefault();
    private final TimeZone timezone = TimeZone.getDefault();
    private final Messages messages = Messages.create(locale);

    // TODO: move to custom class
    private final ArrayDeque<Task> scriptTasks = new ArrayDeque<>();
    private final ArrayDeque<Task> promiseTasks = new ArrayDeque<>();

    private final GlobalSymbolRegistry symbolRegistry = new GlobalSymbolRegistry();

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
     * Creates a new {@link World} object with the default settings.
     * 
     * @param allocator
     *            the global object allocator
     */
    public World(ObjectAllocator<GLOBAL> allocator) {
        this(allocator, CompatibilityOption.WebCompatibility(),
                EnumSet.noneOf(Parser.Option.class), EnumSet.noneOf(Compiler.Option.class));
    }

    /**
     * Creates a new {@link World} object.
     * 
     * @param allocator
     *            the global object allocator
     * @param options
     *            the compatibility options
     */
    public World(ObjectAllocator<GLOBAL> allocator, Set<CompatibilityOption> options) {
        this(allocator, options, EnumSet.noneOf(Parser.Option.class), EnumSet
                .noneOf(Compiler.Option.class));
    }

    /**
     * Creates a new {@link World} object.
     * 
     * @param allocator
     *            the global object allocator
     * @param options
     *            the compatibility options
     * @param compilerOptions
     *            the compiler options
     */
    public World(ObjectAllocator<GLOBAL> allocator, Set<CompatibilityOption> options,
            Set<Parser.Option> parserOptions, Set<Compiler.Option> compilerOptions) {
        this.allocator = allocator;
        this.scriptLoader = new ScriptLoader(options, parserOptions, compilerOptions);
        this.options = EnumSet.copyOf(options);
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
     * Executes the queue of pending tasks.
     */
    public void executeTasks() {
        while (hasPendingTasks()) {
            executeTasks(scriptTasks);
            executeTasks(promiseTasks);
        }
    }

    /**
     * Executes the queue of pending tasks.
     * 
     * @param tasks
     *            the tasks to be executed
     */
    private void executeTasks(ArrayDeque<Task> tasks) {
        // execute all pending tasks until the queue is empty
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
    public TimeZone getTimezone() {
        return timezone;
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
     * Returns the compatibility options for this instance.
     * 
     * @return the compatibility options
     */
    public EnumSet<CompatibilityOption> getOptions() {
        return options;
    }

    /**
     * Tests whether the requested compatibility option is enabled for this instance.
     * 
     * @param option
     *            the compatibility option
     * @return {@code true} if the compatibility option is enabled
     */
    public boolean isEnabled(CompatibilityOption option) {
        return options.contains(option);
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
}
