/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.io.PrintWriter;
import java.util.EnumSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.TraceClassVisitor;

import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.CodeGenerator.ScriptName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeAnalysis;
import com.github.anba.es6draft.compiler.analyzer.CodeSizeException;

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
        Debug
    }

    private final boolean debug;

    public Compiler(EnumSet<Option> options) {
        this.debug = options.contains(Option.Debug);
    }

    public byte[] compile(Script script, String className) {
        final int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        String superClassName = Types.CompiledScript.getInternalName();
        String[] interfaces = null;

        // set-up
        ClassWriter cw = new ClassWriter(flags);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null,
                superClassName, interfaces);
        cw.visitSource(script.getSourceFile(), null);

        try (CodeSizeAnalysis analysis = new CodeSizeAnalysis()) {
            analysis.submit(script);
        } catch (CodeSizeException e) {
            throw new CompilationException(e.getMessage());
        }

        try (CodeGenerator codegen = new CodeGenerator(cw, className)) {
            // generate code
            codegen.compile(script);

            // add default constructor
            defaultScriptConstructor(cw, className, codegen.methodName(script, ScriptName.RTI));
        }

        // finalize
        cw.visitEnd();

        byte[] bytes = cw.toByteArray();
        if (debug) {
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

        try (CodeGenerator codegen = new CodeGenerator(cw, className)) {
            // generate code
            codegen.compile(function);

            // add default constructor
            defaultFunctionConstructor(cw, className,
                    codegen.methodName(function, FunctionName.RTI));
        }

        // finalize
        cw.visitEnd();

        byte[] bytes = cw.toByteArray();
        if (debug) {
            debug(bytes);
        }

        return bytes;
    }

    private static void debug(byte[] b) {
        ClassReader cr = new ClassReader(b);
        cr.accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.SKIP_DEBUG);
    }

    private static void defaultScriptConstructor(ClassWriter cw, String className,
            String methodNameRTI) {
        String methodName = "<init>";
        Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE);

        InstructionVisitor mv = new InstructionVisitor(cw.visitMethod(Opcodes.ACC_PUBLIC,
                methodName, "()V", null, null), methodName, methodDescriptor);
        mv.begin();
        mv.loadThis();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodNameRTI,
                Type.getMethodType(Types.RuntimeInfo$ScriptBody).getDescriptor());
        mv.invoke(Methods.CompiledScript_Constructor);
        mv.areturn();
        mv.end();
    }

    private static void defaultFunctionConstructor(ClassWriter cw, String className,
            String methodNameRTI) {
        String methodName = "<init>";
        Type methodDescriptor = Type.getMethodType(Type.VOID_TYPE);

        InstructionVisitor mv = new InstructionVisitor(cw.visitMethod(Opcodes.ACC_PUBLIC,
                methodName, "()V", null, null), methodName, methodDescriptor);
        mv.begin();
        mv.loadThis();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, methodNameRTI,
                Type.getMethodType(Types.RuntimeInfo$Function).getDescriptor());
        mv.invoke(Methods.CompiledFunction_Constructor);
        mv.areturn();
        mv.end();
    }
}
