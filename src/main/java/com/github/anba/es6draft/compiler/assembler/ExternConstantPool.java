/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.compiler.assembler.Code.ClassCode;

/**
 * 
 */
final class ExternConstantPool extends ConstantPool {
    private static final int EXTERN_CONSTANTS_LIMIT = 0x6000;
    private static final int METHOD_LIMIT = 0x400;

    private static final class TypeSpec<T> {
        final String methodName;
        final String methodDescriptor;
        final Type type;
        final T defaultValue;

        private TypeSpec(String methodName, Type type, T defaultValue) {
            this.methodName = methodName;
            this.methodDescriptor = Type.getMethodDescriptor(type, Type.INT_TYPE);
            this.type = type;
            this.defaultValue = defaultValue;
        }

        static final TypeSpec<Integer> INT = new TypeSpec<>("getInt", Type.INT_TYPE, 0);
        static final TypeSpec<Long> LONG = new TypeSpec<>("getLong", Type.LONG_TYPE, 0L);
        static final TypeSpec<Float> FLOAT = new TypeSpec<>("getFloat", Type.FLOAT_TYPE, 0f);
        static final TypeSpec<Double> DOUBLE = new TypeSpec<>("getDouble", Type.DOUBLE_TYPE, 0d);
        static final TypeSpec<String> STRING = new TypeSpec<>("getString", Types.String, "");
    }

    private final ClassCode classCode;
    private boolean closed = false;

    ExternConstantPool(Code code) {
        super(code, EXTERN_CONSTANTS_LIMIT);
        classCode = code.newClass(this);
    }

    @Override
    protected ConstantPool newConstantPool() {
        return new ExternConstantPool(code);
    }

    @Override
    protected void close() {
        assert !closed && (closed = true) : "constant pool closed";
        ClassWriter cw = classCode.classWriter;
        generate(cw, TypeSpec.INT, getIntegers());
        generate(cw, TypeSpec.LONG, getLongs());
        generate(cw, TypeSpec.FLOAT, getFloats());
        generate(cw, TypeSpec.DOUBLE, getDoubles());
        generate(cw, TypeSpec.STRING, getStrings());
    }

    private <T> void generate(ClassWriter cw, TypeSpec<T> spec, T[] values) {
        if (values.length <= METHOD_LIMIT) {
            generate(cw, spec, "", values, 0, values.length);
        } else {
            generateSplit(cw, spec, values);
        }
    }

    private <T> void generateSplit(ClassWriter cw, TypeSpec<T> spec, T[] values) {
        // split into multiple methods
        for (int i = 0, index = 0; i < values.length; i += METHOD_LIMIT, ++index) {
            int from = i, to = Math.min(from + METHOD_LIMIT, values.length);
            generate(cw, spec, "_" + index, values, from, to);
        }
        // generate entry method
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, spec.methodName,
                spec.methodDescriptor, null, null);
        mv.visitCode();
        for (int i = 0, index = 0; i < values.length; i += METHOD_LIMIT, ++index) {
            int from = i, to = Math.min(from + METHOD_LIMIT, values.length);
            Label label = new Label();
            mv.visitVarInsn(Opcodes.ILOAD, 0);
            mv.visitIntInsn(Opcodes.SIPUSH, to);
            mv.visitJumpInsn(Opcodes.IF_ICMPGE, label);
            mv.visitVarInsn(Opcodes.ILOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, classCode.className, spec.methodName + "_"
                    + index, spec.methodDescriptor, false);
            mv.visitInsn(spec.type.getOpcode(Opcodes.IRETURN));
            mv.visitLabel(label);
        }
        mv.visitLdcInsn(spec.defaultValue);
        mv.visitInsn(spec.type.getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private <T> void generate(ClassWriter cw, TypeSpec<T> spec, String suffix, T[] values,
            int from, int to) {
        int length = to - from;
        if (length == 0) {
            return;
        }
        String methodName = spec.methodName + suffix;
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, methodName,
                spec.methodDescriptor, null, null);
        Label dflt = new Label();
        Label[] labels = new Label[length];
        for (int index = 0; index < length; ++index) {
            labels[index] = new Label();
        }
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ILOAD, 0);
        mv.visitTableSwitchInsn(from, to - 1, dflt, labels);
        for (int index = 0; index < length; ++index) {
            mv.visitLabel(labels[index]);
            mv.visitLdcInsn(values[from + index]);
            mv.visitInsn(spec.type.getOpcode(Opcodes.IRETURN));
        }
        mv.visitLabel(dflt);
        mv.visitLdcInsn(spec.defaultValue);
        mv.visitInsn(spec.type.getOpcode(Opcodes.IRETURN));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void load(InstructionAssembler assembler, Object cst, int index, TypeSpec<?> spec) {
        assert 0 <= index && index <= EXTERN_CONSTANTS_LIMIT;
        assembler.iconst(index);
        assembler.invokestatic(classCode.className, spec.methodName, spec.methodDescriptor, false);
    }

    @Override
    protected void iconst(InstructionAssembler assembler, Integer cst, int index) {
        load(assembler, cst, index, TypeSpec.INT);
    }

    @Override
    protected void lconst(InstructionAssembler assembler, Long cst, int index) {
        load(assembler, cst, index, TypeSpec.LONG);
    }

    @Override
    protected void fconst(InstructionAssembler assembler, Float cst, int index) {
        load(assembler, cst, index, TypeSpec.FLOAT);
    }

    @Override
    protected void dconst(InstructionAssembler assembler, Double cst, int index) {
        load(assembler, cst, index, TypeSpec.DOUBLE);
    }

    @Override
    protected void aconst(InstructionAssembler assembler, String cst, int index) {
        load(assembler, cst, index, TypeSpec.STRING);
    }
}
