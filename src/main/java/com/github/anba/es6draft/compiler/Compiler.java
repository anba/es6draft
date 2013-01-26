/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.parser.ParserException;

/**
 *
 */
public class Compiler {
    private final boolean debug;

    public Compiler(boolean debug) {
        this.debug = debug;
    }

    public byte[] compile(Script script, String className) throws ParserException {
        final int flags = ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
        String superClassName = Types.CompiledScript.getInternalName();
        String[] interfaces = null;

        // set-up
        ClassWriter cw = new ClassWriter(flags);
        cw.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null,
                superClassName, interfaces);
        cw.visitSource(script.getSourceFile(), null);

        // add default constructor
        defaultConstructor(cw, className);

        // generate actual code
        CodeGenerator codegen = new CodeGenerator(cw, className);
        script.accept(codegen, null);
        codegen.close();

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
        cr.accept(new TraceClassVisitor(null, new Textifier(), new PrintWriter(System.out)),
                ClassReader.SKIP_DEBUG);
    }

    private static void defaultConstructor(ClassWriter cw, String className) {
        MethodDesc constructor = Methods.CompiledScript_Constructor;
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "script_rti",
                Type.getMethodType(Types.RuntimeInfo$ScriptBody).getDescriptor());
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, constructor.owner, constructor.name,
                constructor.desc);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
