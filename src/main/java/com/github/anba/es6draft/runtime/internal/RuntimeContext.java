/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.RuntimeWorkerThreadFactory.createThreadPoolExecutor;
import static com.github.anba.es6draft.runtime.internal.RuntimeWorkerThreadFactory.createWorkerThreadPoolExecutor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * The new runtime options and configuration class.
 */
public final class RuntimeContext {
    private final ObjectAllocator<? extends GlobalObject> globalAllocator;
    private final BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> moduleLoader;
    private final Locale locale;
    private final TimeZone timeZone;
    private final Path baseDirectory; // TODO: or/and URI?

    // TODO: Replace with Console interface?
    private Console console;

    private final ScriptCache scriptCache;
    private final ExecutorService executor;
    private final boolean shutdownExecutorOnFinalization;
    private final ExecutorService workerExecutor;
    private final boolean shutdownWorkerExecutorOnFinalization;
    private final BiConsumer<ExecutionContext, Throwable> workerErrorReporter;
    private final Futex futex;

    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;

    RuntimeContext(ObjectAllocator<? extends GlobalObject> globalAllocator,
            BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> moduleLoader, Locale locale,
            TimeZone timeZone, Path baseDirectory, Console console, ScriptCache scriptCache, ExecutorService executor,
            ExecutorService workerExecutor, BiConsumer<ExecutionContext, Throwable> workerErrorReporter, Futex futex,
            EnumSet<CompatibilityOption> options, EnumSet<Parser.Option> parserOptions,
            EnumSet<Compiler.Option> compilerOptions) {
        this.globalAllocator = globalAllocator;
        this.moduleLoader = moduleLoader;
        this.locale = locale;
        this.timeZone = timeZone;
        this.baseDirectory = baseDirectory;
        this.console = console;
        this.scriptCache = scriptCache;
        this.executor = executor != null ? executor : createThreadPoolExecutor();
        this.shutdownExecutorOnFinalization = executor == null;
        this.workerExecutor = workerExecutor != null ? workerExecutor : createWorkerThreadPoolExecutor();
        this.shutdownWorkerExecutorOnFinalization = workerExecutor == null;
        this.workerErrorReporter = workerErrorReporter;
        this.futex = futex;
        this.options = EnumSet.copyOf(options);
        this.parserOptions = EnumSet.copyOf(parserOptions);
        this.compilerOptions = EnumSet.copyOf(compilerOptions);
    }

    @Override
    protected void finalize() throws Throwable {
        if (shutdownExecutorOnFinalization)
            executor.shutdown();
        if (shutdownWorkerExecutorOnFinalization)
            workerExecutor.shutdown();
        super.finalize();
    }

    /**
     * Returns the global object allocator for this instance.
     * 
     * @return the global object allocator
     */
    public ObjectAllocator<? extends GlobalObject> getGlobalAllocator() {
        return globalAllocator;
    }

    /**
     * Returns the module loader constructor for this instance.
     * 
     * @return the module loader constructor
     */
    public BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> getModuleLoader() {
        return moduleLoader;
    }

    /**
     * Returns the locale for this instance.
     * 
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Returns the time zone for this instance.
     * 
     * @return the time zone
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Returns the base directory for this instance.
     * 
     * @return the base directory
     */
    public Path getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Returns the script cache for this instance.
     * 
     * @return the script cache
     */
    public ScriptCache getScriptCache() {
        return scriptCache;
    }

    /**
     * Returns the optional console object for this instance.
     * 
     * @return the console object or {@code null}
     */
    public Console getConsole() {
        return console;
    }

    /**
     * Sets the console object for this instance
     * 
     * @param console
     *            the new console
     */
    public void setConsole(Console console) {
        this.console = Objects.requireNonNull(console);
    }

    /**
     * Returns the executor service for parallel compilation.
     * 
     * @return the executor service
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Returns the executor service for workers.
     * 
     * @return the executor service
     */
    public ExecutorService getWorkerExecutor() {
        return workerExecutor;
    }

    /**
     * Returns the worker error reporter.
     * 
     * @return the worker error reporter
     */
    public BiConsumer<ExecutionContext, Throwable> getWorkerErrorReporter() {
        return workerErrorReporter;
    }

    /**
     * Returns the futex object.
     * 
     * @return the futex object
     */
    public Futex getFutex() {
        return futex;
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
     * Returns the parser options for this instance.
     * 
     * @return the parser options
     */
    public EnumSet<Parser.Option> getParserOptions() {
        return parserOptions;
    }

    /**
     * Returns the compiler options for this instance.
     * 
     * @return the compiler options
     */
    public EnumSet<Compiler.Option> getCompilerOptions() {
        return compilerOptions;
    }

    /**
     * Builder class to create new runtime contexts.
     */
    public static final class Builder {
        private ObjectAllocator<? extends GlobalObject> allocator;
        private BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> moduleLoader;
        private Locale locale;
        private TimeZone timeZone;
        private Path baseDirectory;
        private Console console;
        private ScriptCache scriptCache;
        private ExecutorService executor;
        private ExecutorService workerExecutor;
        private BiConsumer<ExecutionContext, Throwable> workerErrorReporter;
        private Futex futex;
        private final EnumSet<CompatibilityOption> options = EnumSet.noneOf(CompatibilityOption.class);
        private final EnumSet<Parser.Option> parserOptions = EnumSet.noneOf(Parser.Option.class);
        private final EnumSet<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);

        public Builder() {
            allocator = GlobalObject::new;
            moduleLoader = FileModuleLoader::new;
            locale = Locale.getDefault();
            timeZone = TimeZone.getDefault();
            baseDirectory = Paths.get("");
            scriptCache = new ScriptCache();
            workerErrorReporter = (cx, e) -> {
                // empty
            };
            futex = new Futex();
        }

        public Builder(RuntimeContext context) {
            allocator = context.globalAllocator;
            moduleLoader = context.moduleLoader;
            locale = context.locale;
            timeZone = context.timeZone;
            baseDirectory = context.baseDirectory;
            console = context.console;
            scriptCache = context.scriptCache;
            executor = context.executor;
            workerExecutor = context.workerExecutor;
            workerErrorReporter = context.workerErrorReporter;
            futex = context.futex;
            options.addAll(context.options);
            parserOptions.addAll(context.parserOptions);
            compilerOptions.addAll(context.compilerOptions);
        }

        /**
         * Returns a new {@link RuntimeContext} using the fields set by this builder.
         * 
         * @return the new runtime context
         */
        public RuntimeContext build() {
            return new RuntimeContext(allocator, moduleLoader, locale, timeZone, baseDirectory, console, scriptCache,
                    executor, workerExecutor, workerErrorReporter, futex, options, parserOptions, compilerOptions);
        }

        /**
         * Sets the global object allocator.
         * 
         * @param allocator
         *            the global object allocator
         * @return this builder
         */
        public Builder setGlobalAllocator(ObjectAllocator<? extends GlobalObject> allocator) {
            this.allocator = Objects.requireNonNull(allocator);
            return this;
        }

        /**
         * Sets the module loader constructor.
         * 
         * @param moduleLoader
         *            the module loader constructor
         * @return this builder
         */
        public Builder setModuleLoader(BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> moduleLoader) {
            this.moduleLoader = Objects.requireNonNull(moduleLoader);
            return this;
        }

        /**
         * Sets the locale.
         * 
         * @param locale
         *            the locale
         * @return this builder
         */
        public Builder setLocale(Locale locale) {
            this.locale = Objects.requireNonNull(locale);
            return this;
        }

        /**
         * Sets the time zone.
         * 
         * @param timeZone
         *            the time zone
         * @return this builder
         */
        public Builder setTimeZone(TimeZone timeZone) {
            this.timeZone = Objects.requireNonNull(timeZone);
            return this;
        }

        /**
         * Sets the base directory
         * 
         * @param baseDirectory
         *            the base directory
         * @return this builder
         */
        public Builder setBaseDirectory(Path baseDirectory) {
            this.baseDirectory = Objects.requireNonNull(baseDirectory);
            return this;
        }

        /**
         * Sets the script cache.
         * 
         * @param scriptCache
         *            the script cache
         * @return this builder
         */
        public Builder setScriptCache(ScriptCache scriptCache) {
            this.scriptCache = Objects.requireNonNull(scriptCache);
            return this;
        }

        /**
         * Sets the console.
         * 
         * @param console
         *            the console
         * @return this builder
         */
        public Builder setConsole(Console console) {
            this.console = Objects.requireNonNull(console);
            return this;
        }

        /**
         * Sets the executor.
         * 
         * @param executor
         *            the executor
         * @return this builder
         */
        public Builder setExecutor(ExecutorService executor) {
            this.executor = executor; // null allowed
            return this;
        }

        /**
         * Sets the executor for shared workers.
         * 
         * @param executor
         *            the executor
         * @return this builder
         */
        public Builder setWorkerExecutor(ExecutorService executor) {
            this.workerExecutor = executor; // null allowed
            return this;
        }

        /**
         * Sets the worker error reporter.
         * 
         * @param workerErrorReporter
         *            the worker error reporter
         * @return this builder
         */
        public Builder setWorkerErrorReporter(BiConsumer<ExecutionContext, Throwable> workerErrorReporter) {
            this.workerErrorReporter = Objects.requireNonNull(workerErrorReporter);
            return this;
        }

        /**
         * Sets the futex object.
         * 
         * @param futex
         *            the futex object
         * @return this builder
         */
        public Builder setFutex(Futex futex) {
            this.futex = Objects.requireNonNull(futex);
            return this;
        }

        /**
         * Sets the compatibility options.
         * 
         * @param options
         *            the compatibility options
         * @return this builder
         */
        public Builder setOptions(Set<CompatibilityOption> options) {
            this.options.clear();
            this.options.addAll(options);
            return this;
        }

        /**
         * Sets the parser options.
         * 
         * @param options
         *            the parser options
         * @return this builder
         */
        public Builder setParserOptions(Set<Parser.Option> options) {
            this.parserOptions.clear();
            this.parserOptions.addAll(options);
            return this;
        }

        /**
         * Sets the compiler options.
         * 
         * @param options
         *            the compiler options
         * @return this builder
         */
        public Builder setCompilerOptions(Set<Compiler.Option> options) {
            this.compilerOptions.clear();
            this.compilerOptions.addAll(options);
            return this;
        }
    }
}
