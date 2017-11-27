/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.RuntimeWorkerThreadFactory.createThreadPoolExecutor;
import static com.github.anba.es6draft.runtime.internal.RuntimeWorkerThreadFactory.createWorkerThreadPoolExecutor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * The new runtime options and configuration class.
 */
public final class RuntimeContext {
    private final Function<Realm, ? extends RealmData> realmData;
    private final BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> moduleLoader;
    private final Locale locale;
    private TimeZone timeZone;
    private final Path baseDirectory; // TODO: or/and URI?

    private Console console;

    private final ScriptCache scriptCache;
    private final ExecutorService executor;
    private final boolean shutdownExecutorOnFinalization;
    private final ExecutorService workerExecutor;
    private final boolean shutdownWorkerExecutorOnFinalization;
    private final BiConsumer<ExecutionContext, Throwable> errorReporter;
    private final BiConsumer<ExecutionContext, Throwable> workerErrorReporter;
    private final Futex futex;
    private final Consumer<ExecutionContext> debugger;
    private final BiFunction<String, MethodType, MethodHandle> nativeCallResolver;
    private final BiConsumer<ScriptObject, ModuleRecord> importMeta;

    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;

    public static class Data {
        // User-overridable data.
    }

    private final Supplier<? extends RuntimeContext.Data> runtimeData;
    private final RuntimeContext.Data contextData;

    RuntimeContext(Supplier<? extends RuntimeContext.Data> runtimeData, Function<Realm, ? extends RealmData> realmData,
            BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> moduleLoader, Locale locale,
            TimeZone timeZone, Path baseDirectory, Console console, ScriptCache scriptCache, ExecutorService executor,
            BiConsumer<ExecutionContext, Throwable> errorReporter, ExecutorService workerExecutor,
            BiConsumer<ExecutionContext, Throwable> workerErrorReporter, Futex futex,
            Consumer<ExecutionContext> debugger, BiFunction<String, MethodType, MethodHandle> nativeCallResolver,
            BiConsumer<ScriptObject, ModuleRecord> importMeta, EnumSet<CompatibilityOption> options,
            EnumSet<Parser.Option> parserOptions, EnumSet<Compiler.Option> compilerOptions) {
        this.runtimeData = runtimeData;
        this.contextData = runtimeData.get();
        this.realmData = realmData;
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
        this.errorReporter = errorReporter;
        this.workerErrorReporter = workerErrorReporter;
        this.futex = futex;
        this.debugger = debugger;
        this.nativeCallResolver = nativeCallResolver;
        this.importMeta = importMeta;
        this.options = EnumSet.copyOf(options);
        this.parserOptions = EnumSet.copyOf(parserOptions);
        this.compilerOptions = EnumSet.copyOf(compilerOptions);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        if (shutdownExecutorOnFinalization)
            executor.shutdown();
        if (shutdownWorkerExecutorOnFinalization)
            workerExecutor.shutdown();
        super.finalize();
    }

    /**
     * Returns the context data for this instance.
     * 
     * @return the context data
     */
    public RuntimeContext.Data getContextData() {
        return contextData;
    }

    /**
     * Returns the runtime context data constructor for this instance.
     * 
     * @return the runtime context data constructor
     */
    public Supplier<? extends RuntimeContext.Data> getRuntimeData() {
        return runtimeData;
    }

    /**
     * Returns the realm data constructor for this instance.
     * 
     * @return the realm data constructor
     */
    public Function<Realm, ? extends RealmData> getRealmData() {
        return realmData;
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
     * Changes the default time zone for this instance.
     * 
     * @param timeZone
     *            the new time zone
     */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
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
     * Returns the error reporter.
     * 
     * @return the error reporter
     */
    public BiConsumer<ExecutionContext, Throwable> getErrorReporter() {
        return errorReporter;
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
     * Returns the debugger callback.
     * 
     * @return the debugger callback
     */
    public Consumer<ExecutionContext> getDebugger() {
        return debugger;
    }

    /**
     * Returns the native call resolver function.
     * 
     * @return the native call resolver function
     */
    public BiFunction<String, MethodType, MethodHandle> getNativeCallResolver() {
        return nativeCallResolver;
    }

    /**
     * Returns the {@code import.meta} callback.
     * 
     * @return the {@code import.meta} callback
     */
    public BiConsumer<ScriptObject, ModuleRecord> getImportMeta() {
        return importMeta;
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
     * Returns {@code true} if the specified compatibility option is enabled.
     * <p>
     * Convenience method for {@code getOptions().contains(option)}.
     * 
     * @param option
     *            the compatibility option
     * @return {@code true} if the specified option is enabled
     */
    public boolean isEnabled(CompatibilityOption option) {
        return options.contains(option);
    }

    /**
     * Returns {@code true} if the specified parser option is enabled.
     * <p>
     * Convenience method for {@code getParserOptions().contains(option)}.
     * 
     * @param option
     *            the parser option
     * @return {@code true} if the specified option is enabled
     */
    public boolean isEnabled(Parser.Option option) {
        return parserOptions.contains(option);
    }

    /**
     * Returns {@code true} if the specified compiler option is enabled.
     * <p>
     * Convenience method for {@code getCompilerOptions().contains(option)}.
     * 
     * @param option
     *            the compiler option
     * @return {@code true} if the specified option is enabled
     */
    public boolean isEnabled(Compiler.Option option) {
        return compilerOptions.contains(option);
    }

    /**
     * Builder class to create new runtime contexts.
     */
    public static final class Builder {
        private Supplier<? extends RuntimeContext.Data> runtimeData;
        private Function<Realm, ? extends RealmData> realmData;
        private BiFunction<RuntimeContext, ScriptLoader, ? extends ModuleLoader> moduleLoader;
        private Locale locale;
        private TimeZone timeZone;
        private Path baseDirectory;
        private Console console;
        private ScriptCache scriptCache;
        private ExecutorService executor;
        private ExecutorService workerExecutor;
        private BiConsumer<ExecutionContext, Throwable> errorReporter;
        private BiConsumer<ExecutionContext, Throwable> workerErrorReporter;
        private Futex futex;
        private Consumer<ExecutionContext> debugger;
        private BiFunction<String, MethodType, MethodHandle> nativeCallResolver;
        private BiConsumer<ScriptObject, ModuleRecord> importMeta;
        private final EnumSet<CompatibilityOption> options = EnumSet.noneOf(CompatibilityOption.class);
        private final EnumSet<Parser.Option> parserOptions = EnumSet.noneOf(Parser.Option.class);
        private final EnumSet<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);

        public Builder() {
            runtimeData = RuntimeContext.Data::new;
            realmData = RealmData::new;
            moduleLoader = FileModuleLoader::new;
            locale = Locale.getDefault();
            timeZone = TimeZone.getDefault();
            baseDirectory = Paths.get("");
            scriptCache = new ScriptCache();
            errorReporter = (cx, e) -> {
                e.printStackTrace();
            };
            workerErrorReporter = (cx, e) -> {
                e.printStackTrace();
            };
            futex = new Futex();
            debugger = cx -> {
                // empty
            };
            nativeCallResolver = (name, type) -> null;
            importMeta = (meta, module) -> {
                // empty
            };
        }

        public Builder(RuntimeContext context) {
            runtimeData = context.runtimeData;
            realmData = context.realmData;
            moduleLoader = context.moduleLoader;
            locale = context.locale;
            timeZone = context.timeZone;
            baseDirectory = context.baseDirectory;
            console = context.console;
            scriptCache = context.scriptCache;
            executor = context.executor;
            workerExecutor = context.workerExecutor;
            errorReporter = context.errorReporter;
            workerErrorReporter = context.workerErrorReporter;
            futex = context.futex;
            debugger = context.debugger;
            importMeta = context.importMeta;
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
            return new RuntimeContext(runtimeData, realmData, moduleLoader, locale, timeZone, baseDirectory, console,
                    scriptCache, executor, errorReporter, workerExecutor, workerErrorReporter, futex, debugger,
                    nativeCallResolver, importMeta, options, parserOptions, compilerOptions);
        }

        /**
         * Sets the runtime context data constructor.
         * 
         * @param runtimeData
         *            the runtime context data constructor
         * @return this builder
         */
        public Builder setRuntimeData(Supplier<? extends RuntimeContext.Data> runtimeData) {
            this.runtimeData = Objects.requireNonNull(runtimeData);
            return this;
        }

        /**
         * Sets the realm data constructor.
         * 
         * @param realmData
         *            the realm data constructor
         * @return this builder
         */
        public Builder setRealmData(Function<Realm, ? extends RealmData> realmData) {
            this.realmData = Objects.requireNonNull(realmData);
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
         * Sets the error reporter.
         * 
         * @param errorReporter
         *            the error reporter
         * @return this builder
         */
        public Builder setErrorReporter(BiConsumer<ExecutionContext, Throwable> errorReporter) {
            this.errorReporter = Objects.requireNonNull(errorReporter);
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
         * Sets the debugger callback.
         * 
         * @param debugger
         *            the debugger callback
         * @return this builder
         */
        public Builder setDebugger(Consumer<ExecutionContext> debugger) {
            this.debugger = Objects.requireNonNull(debugger);
            return this;
        }

        /**
         * Sets the native call resolver function.
         * 
         * @param nativeCallResolver
         *            the native call resolver function
         * @return this builder
         */
        public Builder setNativeCallResolver(BiFunction<String, MethodType, MethodHandle> nativeCallResolver) {
            this.nativeCallResolver = Objects.requireNonNull(nativeCallResolver);
            return this;
        }

        /**
         * Sets the {@code import.meta} callback.
         * 
         * @param importMeta
         *            the {@code import.meta} callback
         * @return this builder
         */
        public Builder setImportMeta(BiConsumer<ScriptObject, ModuleRecord> importMeta) {
            this.importMeta = importMeta;
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
