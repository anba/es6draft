/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeAnalysis;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeException;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.compiler.assembler.Code.ClassCode;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;

/**
 *
 */
public final class Compiler {
    public enum Option {
        PrintCode, PrintFullCode, DebugInfo, NoResume, NoTailCall, SourceMap
    }

    private final ExecutorService executor;
    private final EnumSet<Option> compilerOptions;

    public Compiler(ExecutorService executor, EnumSet<Option> compilerOptions) {
        this.executor = executor;
        this.compilerOptions = EnumSet.copyOf(compilerOptions);
    }

    public CompiledScript compile(Script script, String className) throws CompilationException {
        try {
            CodeSizeAnalysis analysis = new CodeSizeAnalysis(executor);
            analysis.submit(script);
        } catch (CodeSizeException e) {
            throw new CompilationException(e.getMessage());
        }

        // set-up
        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, Types.CompiledScript,
                sourceName(script), sourceMap(script));

        // generate code
        CodeGenerator codegen = new CodeGenerator(code, executor, script.getOptions(),
                script.getParserOptions(), compilerOptions);
        codegen.compile(script);

        // finalize
        return defineAndLoad(code, className);
    }

    public CompiledModule compile(Module module, ModuleRecord moduleRecord, String className)
            throws CompilationException {
        try {
            CodeSizeAnalysis analysis = new CodeSizeAnalysis(executor);
            analysis.submit(module);
        } catch (CodeSizeException e) {
            throw new CompilationException(e.getMessage());
        }

        // set-up
        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, Types.CompiledModule,
                sourceName(module), sourceMap(module));

        // generate code
        CodeGenerator codegen = new CodeGenerator(code, executor, module.getOptions(),
                module.getParserOptions(), compilerOptions);
        codegen.compile(module, moduleRecord);

        // finalize
        return defineAndLoad(code, className);
    }

    public CompiledFunction compile(FunctionDefinition function, String className)
            throws CompilationException {
        return compile((FunctionNode) function, className);
    }

    public CompiledFunction compile(GeneratorDefinition generator, String className)
            throws CompilationException {
        return compile((FunctionNode) generator, className);
    }

    private CompiledFunction compile(FunctionNode function, String className) {
        try {
            CodeSizeAnalysis analysis = new CodeSizeAnalysis(executor);
            analysis.submit(function);
        } catch (CodeSizeException e) {
            throw new CompilationException(e.getMessage());
        }

        // set-up
        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, Types.CompiledFunction,
                "<Function>", null);

        // generate code
        CodeGenerator codegen = new CodeGenerator(code, executor, compatibilityOptions(function),
                parserOptions(function), compilerOptions);
        codegen.compileFunction(function);

        // finalize
        return defineAndLoad(code, className);
    }

    private <T> T defineAndLoad(Code code, String clazzName) {
        boolean printCode = compilerOptions.contains(Option.PrintCode);
        boolean debugInfo = compilerOptions.contains(Option.DebugInfo);
        CodeLoader loader = new CodeLoader();
        for (ClassCode classCode : code.getClasses()) {
            if (debugInfo) {
                classCode.addField(Modifier.PRIVATE | Modifier.STATIC, "classBytes",
                        Type.of(byte[].class), null);
            }
            byte[] bytes = classCode.toByteArray();
            if (printCode) {
                printCode(bytes);
            }
            // System.out.printf("define class '%s'%n", classCode.className);
            loader.defineClass(classCode.className, bytes);
            if (debugInfo) {
                try {
                    Class<?> c = loader.loadClass(classCode.className);
                    Field classBytes = c.getDeclaredField("classBytes");
                    classBytes.setAccessible(true);
                    classBytes.set(null, bytes);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            Class<?> c = loader.loadClass(clazzName);
            @SuppressWarnings("unchecked")
            T instance = (T) c.newInstance();
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static EnumSet<CompatibilityOption> compatibilityOptions(FunctionNode function) {
        Scope enclosingScope = function.getScope().getEnclosingScope();
        if (enclosingScope instanceof ScriptScope) {
            return ((ScriptScope) enclosingScope).getNode().getOptions();
        }
        return EnumSet.noneOf(CompatibilityOption.class);
    }

    private static EnumSet<Parser.Option> parserOptions(FunctionNode function) {
        Scope enclosingScope = function.getScope().getEnclosingScope();
        if (enclosingScope instanceof ScriptScope) {
            return ((ScriptScope) enclosingScope).getNode().getParserOptions();
        }
        return EnumSet.noneOf(Parser.Option.class);
    }

    private static final class CodeLoader extends ClassLoader {
        public CodeLoader() {
            this(ClassLoader.getSystemClassLoader());
        }

        public CodeLoader(ClassLoader parent) {
            super(parent);
        }

        void defineClass(String className, byte[] bytes) {
            defineClass(className, bytes, 0, bytes.length);
        }
    }

    private void printCode(byte[] b) {
        String code = Code.toByteCode(b, !compilerOptions.contains(Option.PrintFullCode));
        System.out.println(code);
    }

    private static String sourceName(Script script) {
        return sourceName(script.getSource());
    }

    private static String sourceName(Module module) {
        return sourceName(module.getSource());
    }

    private static String sourceName(Source source) {
        return source.getName();
    }

    private String sourceMap(Script script) {
        return sourceMap(script, script.getSource());
    }

    private String sourceMap(Module module) {
        return sourceMap(module, module.getSource());
    }

    private String sourceMap(Node node, Source source) {
        if (!compilerOptions.contains(Option.SourceMap)) {
            return null;
        }
        Path sourceFile = source.getFile();
        if (sourceFile == null) {
            // return if 'sourceFile' is not available
            return null;
        }
        Path relativePath = Paths.get("").toAbsolutePath().relativize(sourceFile);

        try (Formatter smap = new Formatter(Locale.ROOT)) {
            // Header
            // - ID
            smap.format("SMAP%n");
            // - OutputFileName
            smap.format("%s%n", sourceName(source));
            // - DefaultStratumId
            smap.format("Script%n");
            // Section
            // - StratumSection
            smap.format("*S Script%n");
            // - FileSection
            smap.format("*F%n");
            // -- FileInfo
            smap.format("+ 1 %s%n%s%n", sourceFile.getFileName(), relativePath);
            // - LineSection
            smap.format("*L%n");
            // -- LineInfo
            smap.format("%d#1,%d:%d%n", node.getBeginLine(), node.getEndLine(), node.getBeginLine());
            // EndSection
            smap.format("*E%n");
            System.out.println(smap);

            return smap.toString();
        }
    }
}
