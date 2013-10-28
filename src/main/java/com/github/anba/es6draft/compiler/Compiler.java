/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.io.PrintWriter;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Locale;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.TraceClassVisitor;

import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.Scope;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodAllocation;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeAnalysis;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeException;
import com.github.anba.es6draft.parser.Parser;

/**
 *
 */
public class Compiler {
    private static class Methods {
        // class: CompiledFunction
        static final MethodDesc CompiledFunction_Constructor = MethodDesc.create(
                MethodType.Special, Types.CompiledFunction, "<init>",
                Type.getMethodType(Type.VOID_TYPE, Types.RuntimeInfo$Function));

        // class: CompiledScript
        static final MethodDesc CompiledScript_Constructor = MethodDesc.create(MethodType.Special,
                Types.CompiledScript, "<init>",
                Type.getMethodType(Type.VOID_TYPE, Types.RuntimeInfo$ScriptBody));
    }

    public enum Option {
        Debug, SourceMap
    }

    private final EnumSet<Option> options;

    public Compiler(EnumSet<Option> options) {
        this.options = EnumSet.copyOf(options);
    }

    public byte[] compile(Script script, String className) {
        final int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        String superClassName = Types.CompiledScript.getInternalName();
        String[] interfaces = null;

        // set-up
        ClassWriter cw = new ClassWriter(flags);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null,
                superClassName, interfaces);
        cw.visitSource(script.getSourceFile(), sourceMap(script));

        try (CodeSizeAnalysis analysis = new CodeSizeAnalysis()) {
            analysis.submit(script);
        } catch (CodeSizeException e) {
            throw new CompilationException(e.getMessage());
        }

        try (CodeGenerator codegen = new CodeGenerator(cw, className, script.getOptions())) {
            // generate code
            codegen.compile(script);

            // add default constructor
            defaultScriptConstructor(cw, className, codegen.methodName(script, ScriptName.RTI),
                    codegen.methodDescriptor(script, ScriptName.RTI));
        }

        // finalize
        cw.visitEnd();

        byte[] bytes = cw.toByteArray();
        if (options.contains(Option.Debug)) {
            debug(bytes);
        }

        return bytes;
    }

    public byte[] compile(FunctionNode function, String className) {
        final int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        String superClassName = Types.CompiledFunction.getInternalName();
        String[] interfaces = null;

        // set-up
        ClassWriter cw = new ClassWriter(flags);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null,
                superClassName, interfaces);
        cw.visitSource("<Function>", null);

        try (CodeSizeAnalysis analysis = new CodeSizeAnalysis()) {
            analysis.submit(function);
        } catch (CodeSizeException e) {
            throw new CompilationException(e.getMessage());
        }

        try (CodeGenerator codegen = new CodeGenerator(cw, className, optionsFrom(function))) {
            // generate code
            codegen.compile(function);

            // add default constructor
            defaultFunctionConstructor(cw, className,
                    codegen.methodName(function, FunctionName.RTI),
                    codegen.methodDescriptor(function, FunctionName.RTI));
        }

        // finalize
        cw.visitEnd();

        byte[] bytes = cw.toByteArray();
        if (options.contains(Option.Debug)) {
            debug(bytes);
        }

        return bytes;
    }

    private static EnumSet<Parser.Option> optionsFrom(FunctionNode function) {
        Scope enclosingScope = function.getScope().getEnclosingScope();
        if (enclosingScope != null && enclosingScope.getNode() instanceof Script) {
            return ((Script) enclosingScope.getNode()).getOptions();
        }
        return EnumSet.noneOf(Parser.Option.class);
    }

    private static void debug(byte[] b) {
        ClassReader cr = new ClassReader(b);
        cr.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
    }

    private String sourceMap(Script script) {
        if (!options.contains(Option.SourceMap)) {
            return null;
        }
        String sourceFile = script.getSourceFile();
        Path path;
        try {
            path = Paths.get(sourceFile);
        } catch (InvalidPathException e) {
            // return here if 'sourceFile' is not a valid path
            return null;
        }
        // line numbers are limited to uint16 in bytecode, valid line count not needed for smap
        final int maxLineCount = 0xffff;
        try (Formatter smap = new Formatter(Locale.ROOT)) {
            // Header
            // - ID
            smap.format("SMAP%n");
            // - OutputFileName
            smap.format("%s%n", script.getSourceFile());
            // - DefaultStratumId
            smap.format("Script%n");
            // Section
            // - StratumSection
            smap.format("*S Script%n");
            // - FileSection
            smap.format("*F%n");
            // -- FileInfo
            smap.format("+ 1 %s%n%s%n", path.getFileName(), path);
            // - LineSection
            smap.format("*L%n");
            // -- LineInfo
            smap.format("%d#1,%d:%d%n", script.getBeginLine(), maxLineCount, script.getBeginLine());
            // EndSection
            smap.format("*E%n");

            return smap.toString();
        }
    }

    private static void defaultScriptConstructor(ClassWriter cw, String className,
            String methodNameRTI, String methodDescriptorRTI) {
        String methodName = "<init>";
        Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE);

        InstructionVisitor mv = new InstructionVisitor(cw.visitMethod(Opcodes.ACC_PUBLIC,
                methodName, "()V", null, null), methodName, methodDescriptor,
                MethodAllocation.Instance);
        mv.begin();
        mv.loadThis();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodNameRTI, methodDescriptorRTI);
        mv.invoke(Methods.CompiledScript_Constructor);
        mv.areturn();
        mv.end();
    }

    private static void defaultFunctionConstructor(ClassWriter cw, String className,
            String methodNameRTI, String methodDescriptorRTI) {
        String methodName = "<init>";
        Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE);

        InstructionVisitor mv = new InstructionVisitor(cw.visitMethod(Opcodes.ACC_PUBLIC,
                methodName, "()V", null, null), methodName, methodDescriptor,
                MethodAllocation.Instance);
        mv.begin();
        mv.loadThis();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodNameRTI, methodDescriptorRTI);
        mv.invoke(Methods.CompiledFunction_Constructor);
        mv.areturn();
        mv.end();
    }
}
