/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.RuntimeWorkerThreadFactory.createThreadPoolExecutor;

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
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledFunction;
import com.github.anba.es6draft.compiler.CompiledModule;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.interpreter.Interpreter;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.FunctionConstructor;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorFunctionConstructor;

/** 
 * 
 */
public final class ScriptLoader {
    private final boolean shutdownExecutorOnFinalization;
    private final ExecutorService executor;
    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;
    private final AtomicInteger scriptCounter = new AtomicInteger(0);
    private final AtomicInteger moduleCounter = new AtomicInteger(0);
    private final AtomicInteger evalCounter = new AtomicInteger(0);
    private final AtomicInteger functionCounter = new AtomicInteger(0);

    /**
     * Next class name for eval scripts.
     * 
     * @return the next class name for eval scripts
     * @see Eval
     */
    private String nextEvalName() {
        return "#Eval_" + evalCounter.incrementAndGet();
    }

    /**
     * Next class name for functions.
     * 
     * @return the next class name for functions
     * @see FunctionConstructor
     * @see GeneratorFunctionConstructor
     */
    private String nextFunctionName() {
        return "#Function_" + functionCounter.incrementAndGet();
    }

    /**
     * Next class name for modules.
     * 
     * @return the next class name for modules
     */
    private String nextModuleName() {
        return "#Module_" + moduleCounter.incrementAndGet();
    }

    /**
     * Next class name for scripts.
     * 
     * @return the next class name for scripts
     */
    private String nextScriptName() {
        return "#Script_" + scriptCounter.incrementAndGet();
    }

    public ScriptLoader(Set<CompatibilityOption> options, Set<Parser.Option> parserOptions,
            Set<Compiler.Option> compilerOptions) {
        this(null, options, parserOptions, compilerOptions);
    }

    public ScriptLoader(ExecutorService executor, Set<CompatibilityOption> options,
            Set<Parser.Option> parserOptions, Set<Compiler.Option> compilerOptions) {
        this.shutdownExecutorOnFinalization = executor == null;
        this.executor = executor != null ? executor : createThreadPoolExecutor();
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

    /**
     * Parses the javascript script source.
     * 
     * @param source
     *            the script source descriptor
     * @param sourceCode
     *            the source code
     * @return the parsed script node
     * @throws ParserException
     *             if the source contains any syntax errors
     */
    public com.github.anba.es6draft.ast.Script parseScript(Source source, String sourceCode)
            throws ParserException {
        Parser parser = new Parser(source, options, parserOptions);
        return parser.parseScript(sourceCode);
    }

    /**
     * Parses the javascript module source.
     * 
     * @param source
     *            the script source descriptor
     * @param sourceCode
     *            the source code
     * @return the parsed script node
     * @throws ParserException
     *             if the source contains any syntax errors
     */
    public com.github.anba.es6draft.ast.Module parseModule(Source source, String sourceCode)
            throws ParserException {
        Parser parser = new Parser(source, options, parserOptions);
        return parser.parseModule(sourceCode);
    }

    /**
     * Parses and compiles the javascript eval-script.
     * 
     * @param source
     *            the script source descriptor
     * @param sourceCode
     *            the source code
     * @param evalOptions
     *            the eval parser options
     * @return the compiled script
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script evalScript(Source source, String sourceCode, EnumSet<Parser.Option> evalOptions)
            throws ParserException, CompilationException {
        Parser parser = new Parser(source, options, evalOptions);
        com.github.anba.es6draft.ast.Script parsedScript = parser.parseScript(sourceCode);
        if (parsedScript.getStatements().isEmpty()) {
            return null;
        }
        return load(parsedScript, nextEvalName());
    }

    /**
     * Parses and compiles the javascript function.
     * 
     * @param source
     *            the script source descriptor
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
    public CompiledFunction function(Source source, String formals, String bodyText)
            throws ParserException, CompilationException {
        Parser parser = new Parser(source, options, parserOptions);
        FunctionDefinition functionDef = parser.parseFunction(formals, bodyText);
        return compile(functionDef, nextFunctionName());
    }

    /**
     * Parses and compiles the javascript generator function.
     * 
     * @param source
     *            the script source descriptor
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
    public CompiledFunction generator(Source source, String formals, String bodyText)
            throws ParserException, CompilationException {
        Parser parser = new Parser(source, options, parserOptions);
        GeneratorDefinition generatorDef = parser.parseGenerator(formals, bodyText);
        return compile(generatorDef, nextFunctionName());
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param source
     *            the script source descriptor
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
    public Script script(Source source, URL file) throws IOException, ParserException,
            CompilationException {
        return script(source, newReader(file.openStream()));
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param source
     *            the script source descriptor
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
    public Script script(Source source, InputStream stream) throws IOException, ParserException,
            CompilationException {
        return script(source, newReader(stream));
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param source
     *            the script source descriptor
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
    public Script script(Source source, Reader reader) throws IOException, ParserException,
            CompilationException {
        return script(source, readFully(reader));
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param source
     *            the script source descriptor
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
    public Script script(Source source, Path file) throws IOException, ParserException,
            CompilationException {
        if (!file.isAbsolute()) {
            throw new IllegalArgumentException(String.format("'%s' is not an absolute path", file));
        }
        return script(source, readFully(file));
    }

    /**
     * Parses and compiles the javascript file.
     * 
     * @param source
     *            the script source descriptor
     * @param sourceCode
     *            the source code
     * @return the compiled script
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script script(Source source, String sourceCode) throws ParserException,
            CompilationException {
        com.github.anba.es6draft.ast.Script parsedScript = parseScript(source, sourceCode);
        return load(parsedScript, nextScriptName());
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
     * Returns an executable {@link Module} object for the
     * {@link com.github.anba.es6draft.ast.Module Module} AST-node.
     * 
     * @param parsedModule
     *            the module node
     * @param moduleRecord
     *            the module record
     * @return the module object
     */
    public Module load(com.github.anba.es6draft.ast.Module parsedModule, ModuleRecord moduleRecord)
            throws CompilationException {
        return compile(parsedModule, moduleRecord, nextModuleName());
    }

    /**
     * Returns an executable {@link Module} object for the
     * {@link com.github.anba.es6draft.ast.Module Module} AST-node.
     * 
     * @param parsedModule
     *            the module node
     * @param moduleRecord
     *            the module record
     * @param className
     *            the class name
     * @return the module object
     */
    public Module load(com.github.anba.es6draft.ast.Module parsedModule, ModuleRecord moduleRecord,
            String className) throws CompilationException {
        return compile(parsedModule, moduleRecord, className);
    }

    /**
     * Compiles the {@link com.github.anba.es6draft.ast.Script Script} AST-node to an executable
     * {@link CompiledScript} object.
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
     * Compiles the {@link com.github.anba.es6draft.ast.Module Module} AST-node to an executable
     * {@link CompiledModule} object.
     * 
     * @param parsedModule
     *            the module node
     * @param moduleRecord
     *            the module record
     * @param className
     *            the class name
     * @return the module object
     */
    public CompiledModule compile(com.github.anba.es6draft.ast.Module parsedModule,
            ModuleRecord moduleRecord, String className) throws CompilationException {
        return tryCompile(parsedModule, moduleRecord, className, executor, compilerOptions);
    }

    /**
     * Compiles the {@link FunctionDefinition} AST-node to a {@link CompiledFunction} object.
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
     * Compiles the {@link GeneratorDefinition} AST-node to a {@link CompiledFunction} object.
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

    private static CompiledModule tryCompile(com.github.anba.es6draft.ast.Module parsedModule,
            ModuleRecord moduleRecord, String className, ExecutorService executor,
            EnumSet<Compiler.Option> options) {
        if (executor.isShutdown()) {
            return compileWithNew(parsedModule, moduleRecord, className, options);
        }
        return compileWith(parsedModule, moduleRecord, className, executor, options);
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

    private static CompiledModule compileWithNew(com.github.anba.es6draft.ast.Module parsedModule,
            ModuleRecord moduleRecord, String className, EnumSet<Compiler.Option> options)
            throws CompilationException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            return compileWith(parsedModule, moduleRecord, className, executor, options);
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

    private static CompiledModule compileWith(com.github.anba.es6draft.ast.Module parsedModule,
            ModuleRecord moduleRecord, String className, ExecutorService executor,
            EnumSet<Compiler.Option> options) {
        Compiler compiler = new Compiler(executor, options);
        return compiler.compile(parsedModule, moduleRecord, className);
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

    private static Reader newReader(InputStream stream) {
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private static String readFully(Reader reader) throws IOException {
        try {
            StringBuilder sb = new StringBuilder(4096);
            char cbuf[] = new char[4096];
            for (int len; (len = reader.read(cbuf)) != -1;) {
                sb.append(cbuf, 0, len);
            }
            return sb.toString();
        } finally {
            reader.close();
        }
    }

    private static String readFully(Path p) throws IOException {
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
}
