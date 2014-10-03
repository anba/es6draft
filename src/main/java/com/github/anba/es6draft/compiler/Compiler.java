/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeAnalysis;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeException;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.compiler.assembler.Code.ClassCode;
import com.github.anba.es6draft.compiler.assembler.SimpleTypeTextifier;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 *
 */
public final class Compiler {
    public enum Option {
        Debug, FullDebug, DebugInfo, NoResume, NoTailCall, SourceMap, VerifyStack
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
        // prepend '#' to mark generated classes, cf. ErrorPrototype
        String clazzName = "#" + className;

        String superClassName = Types.CompiledScript.getInternalName();
        Code code = new Code(clazzName, superClassName, sourceName(script), sourceMap(script));

        // generate code
        CodeGenerator codegen = new CodeGenerator(code, executor, script, compilerOptions);
        codegen.compile(script);

        // finalize
        return defineAndLoad(code, clazzName);
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
        // prepend '#' to mark generated classes, cf. ErrorPrototype
        String clazzName = "#" + className;
        String superClassName = Types.CompiledFunction.getInternalName();
        Code code = new Code(clazzName, superClassName, "<Function>", null);

        // generate code
        CodeGenerator codegen = new CodeGenerator(code, executor, script(function), compilerOptions);
        codegen.compileFunction(function);

        // finalize
        return defineAndLoad(code, clazzName);
    }

    private <T> T defineAndLoad(Code code, String clazzName) {
        boolean debug = compilerOptions.contains(Option.Debug);
        boolean debugInfo = compilerOptions.contains(Option.DebugInfo);
        CodeLoader loader = new CodeLoader();
        for (ClassCode classCode : code.getClasses()) {
            if (debugInfo) {
                classCode.addField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "classBytes",
                        Type.getDescriptor(byte[].class), null);
            }
            byte[] bytes = classCode.toByteArray();
            if (debug) {
                debug(bytes);
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

    private static Script script(FunctionNode function) {
        Scope enclosingScope = function.getScope().getEnclosingScope();
        if (enclosingScope instanceof ScriptScope) {
            return ((ScriptScope) enclosingScope).getNode();
        }
        // Create a dummy script instance
        return new Script(0, 0, new Source("<unknown>", 0), null,
                Collections.<StatementListItem> emptyList(),
                EnumSet.noneOf(CompatibilityOption.class), EnumSet.noneOf(Parser.Option.class),
                false);
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

    private void debug(byte[] b) {
        ClassReader cr = new ClassReader(b);
        Printer p;
        if (compilerOptions.contains(Option.FullDebug)) {
            p = new Textifier();
        } else {
            p = new SimpleTypeTextifier();
        }
        cr.accept(new TraceClassVisitor(null, p, null), ClassReader.EXPAND_FRAMES);
        PrintWriter pw = new PrintWriter(System.out);
        p.print(pw);
        pw.flush();
    }

    private static String sourceName(Script script) {
        return script.getSource().getName();
    }

    private String sourceMap(Script script) {
        if (!compilerOptions.contains(Option.SourceMap)) {
            return null;
        }
        Path sourceFile = script.getSource().getFile();
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
            smap.format("%s%n", sourceName(script));
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
            smap.format("%d#1,%d:%d%n", script.getBeginLine(), script.getEndLine(),
                    script.getBeginLine());
            // EndSection
            smap.format("*E%n");

            return smap.toString();
        }
    }
}
