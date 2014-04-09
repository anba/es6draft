/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptExecutionContext;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.CompiledFunction;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.interpreter.InterpretedScript;
import com.github.anba.es6draft.interpreter.Interpreter;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;

/**
 * <h1>15 ECMAScript Language: Modules and Scripts</h1>
 * <ul>
 * <li>15.2 Script
 * </ul>
 */
public final class ScriptLoader {
    private ScriptLoader() {
    }

    /**
     * [15.2.7 Runtime Semantics: Script Evaluation]
     * 
     * @param script
     *            the script object
     * @param realm
     *            the realm instance
     * @param deletableBindings
     *            the deletableBindings flag
     * @return the script evaluation result
     */
    public static Object ScriptEvaluation(Script script, Realm realm, boolean deletableBindings) {
        /* steps 1-2 */
        RuntimeInfo.ScriptBody scriptBody = script.getScriptBody();
        if (scriptBody == null)
            return null;
        /* step 3 */
        LexicalEnvironment<GlobalEnvironmentRecord> globalEnv = realm.getGlobalEnv();
        /* steps 4-5 */
        scriptBody.globalDeclarationInstantiation(realm.defaultContext(), globalEnv, globalEnv,
                deletableBindings);
        /* steps 6-9 */
        ExecutionContext progCxt = newScriptExecutionContext(realm, script);
        ExecutionContext oldScriptContext = realm.getScriptContext();
        try {
            realm.setScriptContext(progCxt);
            /* steps 10-14 */
            Object result = script.evaluate(progCxt);
            /* step 15 */
            return result;
        } finally {
            realm.setScriptContext(oldScriptContext);
        }
    }

    /**
     * Returns an executable {@link Script} object for given
     * {@link com.github.anba.es6draft.ast.Script} AST-node. This may either be an
     * {@link InterpretedScript} or {@link CompiledScript} instance.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @param options
     *            the compiler options
     * @return the script object
     */
    public static Script load(com.github.anba.es6draft.ast.Script parsedScript, String className,
            EnumSet<Compiler.Option> options) throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(parsedScript, className, options);
        }
        return script;
    }

    /**
     * Returns an executable {@link Script} object for given
     * {@link com.github.anba.es6draft.ast.Script} AST-node. This may either be an
     * {@link InterpretedScript} or {@link CompiledScript} instance.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @param executor
     *            the executor for parallel compilation
     * @param options
     *            the compiler options
     * @return the script object
     */
    public static Script load(com.github.anba.es6draft.ast.Script parsedScript, String className,
            ExecutorService executor, EnumSet<Compiler.Option> options) throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(parsedScript, className, executor, options);
        }
        return script;
    }

    /**
     * Compiles the given {@link com.github.anba.es6draft.ast.Script} to an executable
     * {@link Script} object.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @param options
     *            the compiler options
     * @return the script object
     */
    public static CompiledScript compile(com.github.anba.es6draft.ast.Script parsedScript,
            String className, EnumSet<Compiler.Option> options) throws CompilationException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Compiler compiler = new Compiler(executor, options);
            return compiler.compile(parsedScript, className);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Compiles the given {@link com.github.anba.es6draft.ast.Script} to an executable
     * {@link Script} object.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @param executor
     *            the executor for parallel compilation
     * @param options
     *            the compiler options
     * @return the script object
     */
    public static CompiledScript compile(com.github.anba.es6draft.ast.Script parsedScript,
            String className, ExecutorService executor, EnumSet<Compiler.Option> options)
            throws CompilationException {
        return tryCompile(parsedScript, className, executor, options);
    }

    /**
     * Returns an executable {@link Script} object for given
     * {@link com.github.anba.es6draft.ast.Script} AST-node. This may either be an
     * {@link InterpretedScript} or {@link CompiledScript} instance.
     * 
     * @param realm
     *            the realm instance
     * @param parsedScript
     *            the script node
     * @return the script object
     */
    public static Script load(Realm realm, com.github.anba.es6draft.ast.Script parsedScript)
            throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            String className = realm.nextEvalName();
            script = compile(realm, parsedScript, className);
        }
        return script;
    }

    /**
     * Returns an executable {@link Script} object for given
     * {@link com.github.anba.es6draft.ast.Script} AST-node. This may either be an
     * {@link InterpretedScript} or {@link CompiledScript} instance.
     * 
     * @param realm
     *            the realm instance
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @return the script object
     */
    public static Script load(Realm realm, com.github.anba.es6draft.ast.Script parsedScript,
            String className) throws CompilationException {
        Script script = Interpreter.script(parsedScript);
        if (script == null) {
            script = compile(realm, parsedScript, className);
        }
        return script;
    }

    /**
     * Compiles the given {@link com.github.anba.es6draft.ast.Script} to an executable
     * {@link Script} object.
     * 
     * @param realm
     *            the realm instance
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @return the script object
     */
    public static CompiledScript compile(Realm realm,
            com.github.anba.es6draft.ast.Script parsedScript, String className)
            throws CompilationException {
        ExecutorService executor = realm.getExecutor();
        return tryCompile(parsedScript, className, executor, realm.getCompilerOptions());
    }

    /**
     * Compiles the given {@link FunctionDefinition} to a
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.Function} object.
     * 
     * @param realm
     *            the realm instance
     * @param function
     *            the function node
     * @return the compiled function runtime information
     */
    public static RuntimeInfo.Function compile(Realm realm, FunctionDefinition function)
            throws CompilationException {
        ExecutorService executor = realm.getExecutor();
        String className = realm.nextFunctionName();
        return tryCompile(function, className, executor, realm.getCompilerOptions()).getFunction();
    }

    /**
     * Compiles the given {@link GeneratorDefinition} to a
     * {@link com.github.anba.es6draft.runtime.internal.RuntimeInfo.Function} object.
     * 
     * @param realm
     *            the realm instance
     * @param generator
     *            the generator node
     * @return the compiled function runtime information
     */
    public static RuntimeInfo.Function compile(Realm realm, GeneratorDefinition generator)
            throws CompilationException {
        ExecutorService executor = realm.getExecutor();
        String className = realm.nextFunctionName();
        return tryCompile(generator, className, executor, realm.getCompilerOptions()).getFunction();
    }

    /**
     * Try to compile the script with the given executor, unless it has been shutdown, in that case
     * create a new executor for compilation.
     * 
     * @param parsedScript
     *            the script node
     * @param className
     *            the class name
     * @param executor
     *            the executor for parallel compilation
     * @param options
     *            the compiler options
     * @return the compile script
     */
    private static CompiledScript tryCompile(com.github.anba.es6draft.ast.Script parsedScript,
            String className, ExecutorService executor, EnumSet<Compiler.Option> options) {
        if (executor.isShutdown()) {
            return compile(parsedScript, className, options);
        }
        Compiler compiler = new Compiler(executor, options);
        return compiler.compile(parsedScript, className);
    }

    /**
     * Try to compile the function with the given executor, unless it has been shutdown, in that
     * case create a new executor for compilation.
     * 
     * @param function
     *            the function node
     * @param className
     *            the class name
     * @param executor
     *            the executor for parallel compilation
     * @param options
     *            the compiler options
     * @return the compile function
     */
    private static CompiledFunction tryCompile(FunctionDefinition function, String className,
            ExecutorService executor, EnumSet<Compiler.Option> options) {
        if (executor.isShutdown()) {
            return compile(function, className, options);
        }
        Compiler compiler = new Compiler(executor, options);
        return compiler.compile(function, className);
    }

    private static CompiledFunction compile(FunctionDefinition function, String className,
            EnumSet<Compiler.Option> options) throws CompilationException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Compiler compiler = new Compiler(executor, options);
            return compiler.compile(function, className);
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Try to compile the generator with the given executor, unless it has been shutdown, in that
     * case create a new executor for compilation.
     * 
     * @param generator
     *            the generator node
     * @param className
     *            the class name
     * @param executor
     *            the executor for parallel compilation
     * @param options
     *            the compiler options
     * @return the compile function
     */
    private static CompiledFunction tryCompile(GeneratorDefinition generator, String className,
            ExecutorService executor, EnumSet<Compiler.Option> options) {
        if (executor.isShutdown()) {
            return compile(generator, className, options);
        }
        Compiler compiler = new Compiler(executor, options);
        return compiler.compile(generator, className);
    }

    private static CompiledFunction compile(GeneratorDefinition generator, String className,
            EnumSet<Compiler.Option> options) throws CompilationException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Compiler compiler = new Compiler(executor, options);
            return compiler.compile(generator, className);
        } finally {
            executor.shutdown();
        }
    }
}
