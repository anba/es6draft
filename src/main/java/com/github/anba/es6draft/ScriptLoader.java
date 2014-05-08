/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledFunction;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.interpreter.Interpreter;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.FunctionConstructor;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorFunctionConstructor;

/** 
 * 
 */
public final class ScriptLoader {
    private static final int THREAD_POOL_SIZE = 2;
    private static final long THREAD_POOL_TTL = 5 * 60;

    private final ExecutorService executor;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;
    private final AtomicInteger scriptCounter = new AtomicInteger(0);
    private final AtomicInteger evalCounter = new AtomicInteger(0);
    private final AtomicInteger functionCounter = new AtomicInteger(0);

    /**
     * Next class name for eval scripts.
     * 
     * @return the next class name for eval scripts
     * @see Eval
     */
    private String nextEvalName() {
        return "Eval_" + evalCounter.incrementAndGet();
    }

    /**
     * Next class name for functions.
     * 
     * @return the next class name for functions
     * @see FunctionConstructor
     * @see GeneratorFunctionConstructor
     */
    private String nextFunctionName() {
        return "Function_" + functionCounter.incrementAndGet();
    }

    /**
     * Next class name for scripts.
     * 
     * @return the next class name for functions
     * @see FunctionConstructor
     * @see GeneratorFunctionConstructor
     */
    private String nextScriptName() {
        return "Script_" + scriptCounter.incrementAndGet();
    }

    public ScriptLoader(Set<CompatibilityOption> options) {
        this(createThreadPoolExecutor(), options, EnumSet.noneOf(Parser.Option.class), EnumSet
                .noneOf(Compiler.Option.class));
    }

    public ScriptLoader(Set<CompatibilityOption> options, Set<Parser.Option> parserOptions,
            Set<Compiler.Option> compilerOptions) {
        this(createThreadPoolExecutor(), options, parserOptions, compilerOptions);
    }

    public ScriptLoader(ExecutorService executor, Set<CompatibilityOption> options,
            Set<Parser.Option> parserOptions, Set<Compiler.Option> compilerOptions) {
        this.executor = executor;
        this.options = EnumSet.copyOf(options);
        this.parserOptions = EnumSet.copyOf(parserOptions);
        this.compilerOptions = EnumSet.copyOf(compilerOptions);
    }

    @Override
    protected void finalize() throws Throwable {
        executor.shutdown();
        super.finalize();
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
     * Parses the javascript script source.
     * 
     * @param sourceName
     *            the script source name
     * @param sourceLine
     *            the script start line
     * @param source
     *            the source
     * @return the parsed script node
     * @throws ParserException
     *             if the source contains any syntax errors
     */
    public com.github.anba.es6draft.ast.Script parseScript(String sourceName, int sourceLine,
            String source) throws ParserException {
        Parser parser = new Parser(sourceName, sourceLine, options, parserOptions);
        return parser.parseScript(source);
    }

    /**
     * Parses the javascript script source.
     * 
     * @param sourceFile
     *            the script source file
     * @param sourceName
     *            the script source name
     * @param sourceLine
     *            the script start line
     * @param source
     *            the source
     * @return the parsed script node
     * @throws ParserException
     *             if the source contains any syntax errors
     */
    public com.github.anba.es6draft.ast.Script parseScript(Path sourceFile, String sourceName,
            int sourceLine, String source) throws ParserException {
        Parser parser = new Parser(sourceFile, sourceName, sourceLine, options, parserOptions);
        return parser.parseScript(source);
    }

    /**
     * Parses the javascript module source.
     * 
     * @param sourceName
     *            the script source name
     * @param sourceLine
     *            the script start line
     * @param source
     *            the source
     * @return the parsed script node
     * @throws ParserException
     *             if the source contains any syntax errors
     */
    public com.github.anba.es6draft.ast.Module parseModule(String sourceName, int sourceLine,
            String source) throws ParserException {
        Parser parser = new Parser(sourceName, sourceLine, options, parserOptions);
        return parser.parseModule(source);
    }

    /**
     * Parses the javascript module source.
     * 
     * @param sourceFile
     *            the script source file
     * @param sourceName
     *            the script source name
     * @param sourceLine
     *            the script start line
     * @param source
     *            the source
     * @return the parsed script node
     * @throws ParserException
     *             if the source contains any syntax errors
     */
    public com.github.anba.es6draft.ast.Module parseModule(Path sourceFile, String sourceName,
            int sourceLine, String source) throws ParserException {
        Parser parser = new Parser(sourceFile, sourceName, sourceLine, options, parserOptions);
        return parser.parseModule(source);
    }

    /**
     * Parses and compiles the javascript eval-script.
     * 
     * @param sourceName
     *            the source name
     * @param sourceLine
     *            the source start line number
     * @param source
     *            the source string
     * @param evalOptions
     *            the eval parser options
     * @return the compiled script
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script evalScript(String sourceName, int sourceLine, String source,
            EnumSet<Parser.Option> evalOptions) throws ParserException, CompilationException {
        Parser parser = new Parser(sourceName, sourceLine, options, evalOptions);
        com.github.anba.es6draft.ast.Script parsedScript = parser.parseScript(source);
        if (parsedScript.getStatements().isEmpty()) {
            return null;
        }
        return load(parsedScript, nextEvalName());
    }

    /**
     * Parses and compiles the javascript function.
     * 
     * @param sourceName
     *            the source name
     * @param sourceLine
     *            the source start line number
     * @param formals
     *            the formal parameters
     * @param bodyText
     *            the function body
     * @return the compiled function
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public CompiledFunction function(String sourceName, int sourceLine, String formals,
            String bodyText) throws ParserException, CompilationException {
        Parser parser = new Parser(sourceName, sourceLine, options, parserOptions);
        FunctionDefinition functionDef = parser.parseFunction(formals, bodyText);
        return compile(functionDef, nextFunctionName());
    }

    /**
     * Parses and compiles the javascript generator function.
     * 
     * @param sourceName
     *            the source name
     * @param sourceLine
     *            the source start line number
     * @param formals
     *            the formal parameters
     * @param bodyText
     *            the generator function body
     * @return the compiled generator function
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public CompiledFunction generator(String sourceName, int sourceLine, String formals,
            String bodyText) throws ParserException, CompilationException {
        Parser parser = new Parser(sourceName, sourceLine, options, parserOptions);
        GeneratorDefinition generatorDef = parser.parseGenerator(formals, bodyText);
        return compile(generatorDef, nextFunctionName());
    }

    /**
     * Returns a new {@link Reader} for the {@code stream} parameter.
     * 
     * @param stream
     *            the input stream
     * @return the buffered input stream reader
     */
    private Reader newReader(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param sourceName
     *            the source name
     * @param sourceLine
     *            the source start line number
     * @param file
     *            the script file URL
     * @return the compiled script
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script script(String sourceName, int sourceLine, URL file) throws IOException,
            ParserException, CompilationException {
        return script(sourceName, sourceLine, newReader(file.openStream()));
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param sourceName
     *            the source name
     * @param sourceLine
     *            the source start line number
     * @param stream
     *            the source
     * @return the compiled script
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script script(String sourceName, int sourceLine, InputStream stream) throws IOException,
            ParserException, CompilationException {
        return script(sourceName, sourceLine, newReader(stream));
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param sourceName
     *            the source name
     * @param sourceLine
     *            the source start line number
     * @param reader
     *            the source
     * @return the compiled script
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script script(String sourceName, int sourceLine, Reader reader) throws IOException,
            ParserException, CompilationException {
        try (Reader r = reader) {
            String source = readFully(reader);
            com.github.anba.es6draft.ast.Script parsedScript = parseScript(sourceName, sourceLine,
                    source);
            return load(parsedScript, nextScriptName());
        }
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param sourceName
     *            the source name
     * @param sourceLine
     *            the source start line number
     * @param file
     *            the script file path
     * @return the compiled script
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script script(String sourceName, int sourceLine, Path file) throws IOException,
            ParserException, CompilationException {
        if (!file.isAbsolute()) {
            throw new IllegalArgumentException(String.format("'%s' is not an absolute path", file));
        }
        try (Reader r = newReader(Files.newInputStream(file))) {
            String source = readFully(r);
            com.github.anba.es6draft.ast.Script parsedScript = parseScript(file, sourceName,
                    sourceLine, source);
            return load(parsedScript, nextScriptName());
        }
    }

    /**
     * Returns an executable {@link Script} object for the
     * {@link com.github.anba.es6draft.ast.Script Script} AST-node.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @return the script object
     */
    public Script load(com.github.anba.es6draft.ast.Script parsedScript, String className)
            throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(parsedScript, className);
        }
        return script;
    }

    /**
     * Compiles the {@link com.github.anba.es6draft.ast.Script Script} AST-node to an executable
     * {@link Script} object.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @return the script object
     */
    public CompiledScript compile(com.github.anba.es6draft.ast.Script parsedScript, String className)
            throws CompilationException {
        return tryCompile(parsedScript, className, executor, compilerOptions);
    }

    /**
     * Compiles the {@link FunctionDefinition} AST-node to a
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.Function RuntimeInfo.Function}
     * object.
     * 
     * @param function
     *            the function node
     * @param className
     *            the class name
     * @return the compiled function
     */
    public CompiledFunction compile(FunctionDefinition function, String className)
            throws CompilationException {
        return tryCompile(function, className, executor, compilerOptions);
    }

    /**
     * Compiles the {@link GeneratorDefinition} AST-node to a
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.Function RuntimeInfo.Function}
     * object.
     * 
     * @param generator
     *            the generator node
     * @param className
     *            the class name
     * @return the compiled generator function
     */
    public CompiledFunction compile(GeneratorDefinition generator, String className)
            throws CompilationException {
        return tryCompile(generator, className, executor, compilerOptions);
    }

    private static CompiledScript tryCompile(com.github.anba.es6draft.ast.Script parsedScript,
            String className, ExecutorService executor, EnumSet<Compiler.Option> options) {
        if (executor.isShutdown()) {
            return compileWithNew(parsedScript, className, options);
        }
        return compileWith(parsedScript, className, executor, options);
    }

    private static CompiledFunction tryCompile(FunctionDefinition function, String className,
            ExecutorService executor, EnumSet<Compiler.Option> options) {
        if (executor.isShutdown()) {
            return compileWithNew(function, className, options);
        }
        return compileWith(function, className, executor, options);
    }

    private static CompiledFunction tryCompile(GeneratorDefinition generator, String className,
            ExecutorService executor, EnumSet<Compiler.Option> options) {
        if (executor.isShutdown()) {
            return compileWithNew(generator, className, options);
        }
        return compileWith(generator, className, executor, options);
    }

    private static CompiledScript compileWithNew(com.github.anba.es6draft.ast.Script parsedScript,
            String className, EnumSet<Compiler.Option> options) throws CompilationException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            return compileWith(parsedScript, className, executor, options);
        } finally {
            executor.shutdown();
        }
    }

    private static CompiledFunction compileWithNew(FunctionDefinition function, String className,
            EnumSet<Compiler.Option> options) throws CompilationException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            return compileWith(function, className, executor, options);
        } finally {
            executor.shutdown();
        }
    }

    private static CompiledFunction compileWithNew(GeneratorDefinition generator, String className,
            EnumSet<Compiler.Option> options) throws CompilationException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            return compileWith(generator, className, executor, options);
        } finally {
            executor.shutdown();
        }
    }

    private static CompiledScript compileWith(com.github.anba.es6draft.ast.Script parsedScript,
            String className, ExecutorService executor, EnumSet<Compiler.Option> options) {
        Compiler compiler = new Compiler(executor, options);
        return compiler.compile(parsedScript, className);
    }

    private static CompiledFunction compileWith(FunctionDefinition function, String className,
            ExecutorService executor, EnumSet<Compiler.Option> options) throws CompilationException {
        Compiler compiler = new Compiler(executor, options);
        return compiler.compile(function, className);
    }

    private static CompiledFunction compileWith(GeneratorDefinition generator, String className,
            ExecutorService executor, EnumSet<Compiler.Option> options) {
        Compiler compiler = new Compiler(executor, options);
        return compiler.compile(generator, className);
    }

    private static String readFully(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(4096);
        char cbuf[] = new char[4096];
        for (int len; (len = reader.read(cbuf)) != -1;) {
            sb.append(cbuf, 0, len);
        }
        return sb.toString();
    }

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
}
