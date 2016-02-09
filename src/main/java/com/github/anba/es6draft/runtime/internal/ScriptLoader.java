/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Module;
import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ast.AsyncFunctionDefinition;
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
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;
import com.github.anba.es6draft.runtime.objects.Eval;
import com.github.anba.es6draft.runtime.objects.FunctionConstructor;
import com.github.anba.es6draft.runtime.objects.iteration.GeneratorFunctionConstructor;

/** 
 * 
 */
public final class ScriptLoader {
    private final RuntimeContext context;
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

    /**
     * Constructs a new script loader.
     * 
     * @param context
     *            the runtime context
     */
    public ScriptLoader(RuntimeContext context) {
        this.context = context;
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
    public com.github.anba.es6draft.ast.Script parseScript(Source source, String sourceCode) throws ParserException {
        Parser parser = new Parser(context, source);
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
    public com.github.anba.es6draft.ast.Module parseModule(Source source, String sourceCode) throws ParserException {
        Parser parser = new Parser(context, source);
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
        Parser parser = new Parser(source, context.getOptions(), evalOptions);
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
        Parser parser = new Parser(context, source);
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
        Parser parser = new Parser(context, source);
        GeneratorDefinition generatorDef = parser.parseGenerator(formals, bodyText);
        return compile(generatorDef, nextFunctionName());
    }

    /**
     * Parses and compiles the javascript async function.
     * 
     * @param source
     *            the script source descriptor
     * @param formals
     *            the formal parameters
     * @param bodyText
     *            the async function body
     * @return the compiled async function
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public CompiledFunction asyncFunction(Source source, String formals, String bodyText)
            throws ParserException, CompilationException {
        Parser parser = new Parser(context, source);
        AsyncFunctionDefinition asyncDef = parser.parseAsyncFunction(formals, bodyText);
        return compile(asyncDef, nextFunctionName());
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
    public Script script(Source source, URL file) throws IOException, ParserException, CompilationException {
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
    public Script script(Source source, InputStream stream) throws IOException, ParserException, CompilationException {
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
    public Script script(Source source, Reader reader) throws IOException, ParserException, CompilationException {
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
    public Script script(Source source, Path file) throws IOException, ParserException, CompilationException {
        if (!file.isAbsolute()) {
            throw new IllegalArgumentException(String.format("'%s' is not an absolute path", file));
        }
        // Don't interpret script files to get better stack trace information.
        com.github.anba.es6draft.ast.Script parsedScript = parseScript(source, readFully(file));
        return compile(parsedScript, nextScriptName());
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
    public Script script(Source source, String sourceCode) throws ParserException, CompilationException {
        com.github.anba.es6draft.ast.Script parsedScript = parseScript(source, sourceCode);
        return load(parsedScript, nextScriptName());
    }

    /**
     * Returns an executable {@link Script} object for the {@link com.github.anba.es6draft.ast.Script Script} AST-node.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @return the script object
     */
    public Script load(com.github.anba.es6draft.ast.Script parsedScript, String className) throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(parsedScript, className);
        }
        return script;
    }

    /**
     * Returns an executable {@link Module} object for the {@link com.github.anba.es6draft.ast.Module Module} AST-node.
     * 
     * @param parsedModule
     *            the module node
     * @param moduleRecord
     *            the module record
     * @return the module object
     */
    public Module load(com.github.anba.es6draft.ast.Module parsedModule, SourceTextModuleRecord moduleRecord)
            throws CompilationException {
        return compile(parsedModule, moduleRecord, nextModuleName());
    }

    /**
     * Returns an executable {@link Module} object for the {@link com.github.anba.es6draft.ast.Module Module} AST-node.
     * 
     * @param parsedModule
     *            the module node
     * @param moduleRecord
     *            the module record
     * @param className
     *            the class name
     * @return the module object
     */
    public Module load(com.github.anba.es6draft.ast.Module parsedModule, SourceTextModuleRecord moduleRecord,
            String className) throws CompilationException {
        return compile(parsedModule, moduleRecord, className);
    }

    /**
     * Compiles the {@link com.github.anba.es6draft.ast.Script Script} AST-node to an executable {@link CompiledScript}
     * object.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @return the script object
     */
    public CompiledScript compile(com.github.anba.es6draft.ast.Script parsedScript, String className)
            throws CompilationException {
        try (CloseableExecutor t = executor()) {
            Compiler compiler = new Compiler(context, t.executor());
            return compiler.compile(parsedScript, className);
        }
    }

    /**
     * Compiles the {@link com.github.anba.es6draft.ast.Module Module} AST-node to an executable {@link CompiledModule}
     * object.
     * 
     * @param parsedModule
     *            the module node
     * @param moduleRecord
     *            the module record
     * @param className
     *            the class name
     * @return the module object
     */
    public CompiledModule compile(com.github.anba.es6draft.ast.Module parsedModule, SourceTextModuleRecord moduleRecord,
            String className) throws CompilationException {
        try (CloseableExecutor t = executor()) {
            Compiler compiler = new Compiler(context, t.executor());
            return compiler.compile(parsedModule, moduleRecord, className);
        }
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
    public CompiledFunction compile(FunctionDefinition function, String className) throws CompilationException {
        try (CloseableExecutor t = executor()) {
            Compiler compiler = new Compiler(context, t.executor());
            return compiler.compile(function, className);
        }
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
    public CompiledFunction compile(GeneratorDefinition generator, String className) throws CompilationException {
        try (CloseableExecutor t = executor()) {
            Compiler compiler = new Compiler(context, t.executor());
            return compiler.compile(generator, className);
        }
    }

    /**
     * Compiles the {@link AsyncFunctionDefinition} AST-node to a {@link CompiledFunction} object.
     * 
     * @param asyncFunction
     *            the async function node
     * @param className
     *            the class name
     * @return the compiled async function
     */
    public CompiledFunction compile(AsyncFunctionDefinition asyncFunction, String className)
            throws CompilationException {
        try (CloseableExecutor t = executor()) {
            Compiler compiler = new Compiler(context, t.executor());
            return compiler.compile(asyncFunction, className);
        }
    }

    private CloseableExecutor executor() {
        if (context.getExecutor().isShutdown()) {
            return new TempExecutor();
        }
        return new ContextExecutor();
    }

    interface CloseableExecutor extends AutoCloseable {
        ExecutorService executor();

        @Override
        void close();
    }

    static final class TempExecutor implements CloseableExecutor {
        final ExecutorService executor = Executors.newFixedThreadPool(2);

        @Override
        public ExecutorService executor() {
            return executor;
        }

        @Override
        public void close() {
            executor.shutdown();
        }
    }

    final class ContextExecutor implements CloseableExecutor {
        @Override
        public ExecutorService executor() {
            return context.getExecutor();
        }

        @Override
        public void close() {
            // empty
        }
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
