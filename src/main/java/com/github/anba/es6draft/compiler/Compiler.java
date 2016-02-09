/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;

import com.github.anba.es6draft.ast.AsyncFunctionDefinition;
import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeAnalysis;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeException;
import com.github.anba.es6draft.compiler.assembler.ClassSignature;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.compiler.assembler.Code.ClassCode;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.completion.CompletionValueVisitor;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;

/**
 *
 */
public final class Compiler {
    public enum Option {
        DebugInfo, PrintCode, PrintFullCode, IterationCatchStackOverflow, NoResume, NoCompletion,
        NoByteCodeSizeValidation, NoTailCall, SourceMap
    }

    private final ExecutorService executor;
    private final EnumSet<Option> compilerOptions;

    public Compiler(RuntimeContext context, ExecutorService executor) {
        this.executor = executor;
        this.compilerOptions = context.getCompilerOptions();
    }

    /**
     * Compiles a script node to a Java bytecode.
     * 
     * @param script
     *            the script node
     * @param className
     *            the class name
     * @return the compiled script
     * @throws CompilationException
     *             if the script node could not be compiled
     */
    public CompiledScript compile(Script script, String className) throws CompilationException {
        if (!isEnabled(Compiler.Option.NoCompletion)) {
            CompletionValueVisitor.performCompletion(script);
        }
        if (!isEnabled(Compiler.Option.NoByteCodeSizeValidation)) {
            try {
                CodeSizeAnalysis.analyze(script, executor);
            } catch (CodeSizeException e) {
                throw new CompilationException(e.getMessage());
            }
        }

        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, ClassSignature.NONE,
                Types.CompiledScript, Collections.<Type> emptyList(), NodeSourceInfo.create(script,
                        compilerOptions));
        CodeGenerator codegen = new CodeGenerator(code, script, executor, compilerOptions);
        codegen.compile(script);

        return defineAndLoad(code, className);
    }

    /**
     * Compiles a module node to a Java bytecode.
     * 
     * @param module
     *            the module node
     * @param moduleRecord
     *            the module record
     * @param className
     *            the class name
     * @return the compiled module
     * @throws CompilationException
     *             if the module node could not be compiled
     */
    public CompiledModule compile(Module module, SourceTextModuleRecord moduleRecord,
            String className) throws CompilationException {
        if (!isEnabled(Compiler.Option.NoCompletion)) {
            CompletionValueVisitor.performCompletion(module);
        }
        if (!isEnabled(Compiler.Option.NoByteCodeSizeValidation)) {
            try {
                CodeSizeAnalysis.analyze(module, executor);
            } catch (CodeSizeException e) {
                throw new CompilationException(e.getMessage());
            }
        }

        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, ClassSignature.NONE,
                Types.CompiledModule, Collections.<Type> emptyList(), NodeSourceInfo.create(module,
                        compilerOptions));
        CodeGenerator codegen = new CodeGenerator(code, module, executor, compilerOptions);
        codegen.compile(module, moduleRecord);

        return defineAndLoad(code, className);
    }

    /**
     * Compiles a function node to a Java bytecode.
     * 
     * @param function
     *            the function node
     * @param className
     *            the class name
     * @return the compiled function
     * @throws CompilationException
     *             if the function node could not be compiled
     */
    public CompiledFunction compile(FunctionDefinition function, String className)
            throws CompilationException {
        return compile((FunctionNode) function, className);
    }

    /**
     * Compiles a generator function node to a Java bytecode.
     * 
     * @param generator
     *            the generator function node
     * @param className
     *            the class name
     * @return the compiled generator function
     * @throws CompilationException
     *             if the generator function node could not be compiled
     */
    public CompiledFunction compile(GeneratorDefinition generator, String className)
            throws CompilationException {
        return compile((FunctionNode) generator, className);
    }

    /**
     * Compiles a async function node to a Java bytecode.
     * 
     * @param asyncFunction
     *            the async function node
     * @param className
     *            the class name
     * @return the compiled async function
     * @throws CompilationException
     *             if the async function node could not be compiled
     */
    public CompiledFunction compile(AsyncFunctionDefinition asyncFunction, String className)
            throws CompilationException {
        return compile((FunctionNode) asyncFunction, className);
    }

    private CompiledFunction compile(FunctionNode function, String className) {
        Script script = functionScript(function);
        if (!isEnabled(Compiler.Option.NoByteCodeSizeValidation)) {
            try {
                CodeSizeAnalysis.analyze(function, executor);
            } catch (CodeSizeException e) {
                throw new CompilationException(e.getMessage());
            }
        }

        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, ClassSignature.NONE,
                Types.CompiledFunction, Collections.<Type> emptyList(), NodeSourceInfo.create(
                        function, compilerOptions));
        CodeGenerator codegen = new CodeGenerator(code, script, executor, compilerOptions);
        codegen.compileFunction(function);

        return defineAndLoad(code, className);
    }

    private static Script functionScript(FunctionNode function) {
        Scope enclosingScope = function.getScope().getEnclosingScope();
        assert enclosingScope instanceof ScriptScope;
        return ((ScriptScope) enclosingScope).getNode();
    }

    private boolean isEnabled(Compiler.Option option) {
        return compilerOptions.contains(option);
    }

    private <T> T defineAndLoad(Code code, String clazzName) {
        boolean printCode = isEnabled(Option.PrintCode);
        boolean printSimple = printCode && !isEnabled(Option.PrintFullCode);
        boolean debugInfo = isEnabled(Option.DebugInfo);
        CodeLoader loader = new CodeLoader();
        for (ClassCode classCode : code.getClasses()) {
            String className = Type.className(classCode.className);
            if (debugInfo) {
                classCode.addField(Modifier.PRIVATE | Modifier.STATIC, "classBytes",
                        Type.of(byte[].class), null);
            }
            byte[] bytes = classCode.toByteArray();
            if (printCode) {
                System.out.println(Code.toByteCode(bytes, printSimple));
            }
            // System.out.printf("define class '%s'%n", className);
            Class<?> c = loader.defineClass(className, bytes);
            if (debugInfo) {
                try {
                    Field classBytes = c.getDeclaredField("classBytes");
                    classBytes.setAccessible(true);
                    classBytes.set(null, bytes);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            Class<?> c = loader.loadClass(Type.className(clazzName));
            @SuppressWarnings("unchecked")
            T instance = (T) c.newInstance();
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CodeLoader extends ClassLoader {
        public CodeLoader() {
            this(ClassLoader.getSystemClassLoader());
        }

        public CodeLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> defineClass(String className, byte[] bytes) {
            return defineClass(className, bytes, 0, bytes.length);
        }
    }
}
