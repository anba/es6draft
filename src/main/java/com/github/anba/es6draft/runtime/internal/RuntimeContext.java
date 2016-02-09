/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.RuntimeWorkerThreadFactory.createThreadPoolExecutor;

import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;

import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.objects.GlobalObject;

/**
 * The new runtime options and configuration class.
 */
public final class RuntimeContext {
    private final ObjectAllocator<? extends GlobalObject> globalAllocator;
    private final Locale locale;
    private final TimeZone timeZone;
    private final Path baseDirectory; // TODO: or/and URI?

    // TODO: Replace with Console interface?
    private Reader reader;
    private PrintWriter writer;
    private PrintWriter errorWriter;

    private final ScriptCache scriptCache;
    private final ExecutorService executor;
    private final boolean shutdownExecutorOnFinalization;

    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;

    RuntimeContext(ObjectAllocator<? extends GlobalObject> globalAllocator, Locale locale, TimeZone timeZone,
            Path baseDirectory, Reader reader, PrintWriter writer, PrintWriter errorWriter, ScriptCache scriptCache,
            ExecutorService executor, EnumSet<CompatibilityOption> options, EnumSet<Parser.Option> parserOptions,
            EnumSet<Compiler.Option> compilerOptions) {
        this.globalAllocator = globalAllocator;
        this.locale = locale;
        this.timeZone = timeZone;
        this.baseDirectory = baseDirectory;
        this.reader = reader;
        this.writer = writer;
        this.errorWriter = errorWriter;
        this.scriptCache = scriptCache;
        this.executor = executor != null ? executor : createThreadPoolExecutor();
        this.shutdownExecutorOnFinalization = executor == null;
        this.options = EnumSet.copyOf(options);
        this.parserOptions = EnumSet.copyOf(parserOptions);
        this.compilerOptions = EnumSet.copyOf(compilerOptions);
    }

    @Override
    protected void finalize() throws Throwable {
        if (shutdownExecutorOnFinalization)
            executor.shutdown();
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
     * Returns the optional reader for this instance.
     * 
     * @return the reader or {@code null}
     */
    public Reader getReader() {
        return reader;
    }

    /**
     * Sets the reader of this instance
     * 
     * @param reader
     *            the new reader
     */
    public void setReader(Reader reader) {
        this.reader = Objects.requireNonNull(reader);
    }

    /**
     * Returns the optional writer for this instance.
     * 
     * @return the writer or {@code null}
     */
    public PrintWriter getWriter() {
        return writer;
    }

    /**
     * Sets the writer of this instance
     * 
     * @param writer
     *            the new writer
     */
    public void setWriter(PrintWriter writer) {
        this.writer = Objects.requireNonNull(writer);
    }

    /**
     * Returns the optional error writer for this instance.
     * 
     * @return the error writer or {@code null}
     */
    public PrintWriter getErrorWriter() {
        return errorWriter;
    }

    /**
     * Sets the error writer of this instance
     * 
     * @param errorWriter
     *            the new error writer
     */
    public void setErrorWriter(PrintWriter errorWriter) {
        this.errorWriter = Objects.requireNonNull(errorWriter);
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
    static ObjectAllocator<GlobalObject> getDefaultGlobalObjectAllocator() {
        return DEFAULT_GLOBAL_OBJECT;
    }

    /**
     * Builder class to create new runtime contexts.
     */
    public static final class Builder {
        private ObjectAllocator<? extends GlobalObject> allocator;
        private Locale locale;
        private TimeZone timeZone;
        private Path baseDirectory;
        private Reader reader;
        private PrintWriter writer;
        private PrintWriter errorWriter;
        private ScriptCache scriptCache;
        private ExecutorService executor;
        private final EnumSet<CompatibilityOption> options = EnumSet.noneOf(CompatibilityOption.class);
        private final EnumSet<Parser.Option> parserOptions = EnumSet.noneOf(Parser.Option.class);
        private final EnumSet<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);

        public Builder() {
            allocator = getDefaultGlobalObjectAllocator();
            locale = Locale.getDefault();
            timeZone = TimeZone.getDefault();
            baseDirectory = Paths.get("");
            scriptCache = new ScriptCache();
        }

        public Builder(RuntimeContext context) {
            allocator = context.globalAllocator;
            locale = context.locale;
            timeZone = context.timeZone;
            baseDirectory = context.baseDirectory;
            reader = context.reader;
            writer = context.writer;
            errorWriter = context.errorWriter;
            scriptCache = context.scriptCache;
            executor = context.executor;
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
            return new RuntimeContext(allocator, locale, timeZone, baseDirectory, reader, writer, errorWriter,
                    scriptCache, executor, options, parserOptions, compilerOptions);
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
         * Sets the reader.
         * 
         * @param reader
         *            the reader
         * @return this builder
         */
        public Builder setReader(Reader reader) {
            this.reader = Objects.requireNonNull(reader);
            return this;
        }

        /**
         * Sets the writer.
         * 
         * @param writer
         *            the writer
         * @return this builder
         */
        public Builder setWriter(PrintWriter writer) {
            this.writer = Objects.requireNonNull(writer);
            return this;
        }

        /**
         * Sets the erro writer.
         * 
         * @param errorWriter
         *            the error writer
         * @return this builder
         */
        public Builder setErrorWriter(PrintWriter errorWriter) {
            this.errorWriter = Objects.requireNonNull(errorWriter);
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
