/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.lang.reflect.Modifier;

import com.github.anba.es6draft.compiler.assembler.Code.ClassCode;
import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;

/**
 * 
 */
final class ExternConstantPool extends ConstantPool {
    private static final int EXTERN_CONSTANTS_LIMIT = 0x6000;
    private static final int METHOD_LIMIT = 0x400;

    private static final class TypeSpec<T> {
        final String methodName;
        final MethodTypeDescriptor methodDescriptor;
        final T defaultValue;

        private TypeSpec(String methodName, Type type, T defaultValue) {
            this.methodName = methodName;
            this.methodDescriptor = MethodTypeDescriptor.methodType(type, Type.INT_TYPE);
            this.defaultValue = defaultValue;
        }

        String methodName(int index) {
            if (index < 0) {
                return methodName;
            }
            return methodName + "_" + index;
        }

        static final TypeSpec<Integer> INT = new TypeSpec<>("getInt", Type.INT_TYPE, 0);
        static final TypeSpec<Long> LONG = new TypeSpec<>("getLong", Type.LONG_TYPE, 0L);
        static final TypeSpec<Float> FLOAT = new TypeSpec<>("getFloat", Type.FLOAT_TYPE, 0f);
        static final TypeSpec<Double> DOUBLE = new TypeSpec<>("getDouble", Type.DOUBLE_TYPE, 0d);
        static final TypeSpec<String> STRING = new TypeSpec<>("getString", Types.String, "");
    }

    private static final class ConstantClassAssembler extends InstructionAssembler {
        public ConstantClassAssembler(MethodCode method) {
            super(method);
        }

        @Override
        protected Stack createStack(Variables variables) {
            return new EmptyStack(variables);
        }
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
        generate(TypeSpec.INT, getIntegers());
        generate(TypeSpec.LONG, getLongs());
        generate(TypeSpec.FLOAT, getFloats());
        generate(TypeSpec.DOUBLE, getDoubles());
        generate(TypeSpec.STRING, getStrings());
    }

    private <T> void generate(TypeSpec<T> spec, T[] values) {
        if (values.length <= METHOD_LIMIT) {
            generate(spec, -1, values, 0, values.length);
        } else {
            generateSplit(spec, values);
        }
    }

    private <T> void generateSplit(TypeSpec<T> spec, T[] values) {
        // split into multiple methods
        for (int i = 0, index = 0; i < values.length; i += METHOD_LIMIT, ++index) {
            int from = i, to = Math.min(from + METHOD_LIMIT, values.length);
            generate(spec, index, values, from, to);
        }
        // generate entry method
        InstructionAssembler asm = newMethod(spec.methodName, spec);
        asm.begin();
        for (int i = 0, index = 0; i < values.length; i += METHOD_LIMIT, ++index) {
            int from = i, to = Math.min(from + METHOD_LIMIT, values.length);
            Jump jump = new Jump();
            asm.loadParameter(0, Type.INT_TYPE);
            asm.sipush(to);
            asm.ificmpge(jump);
            asm.loadParameter(0, Type.INT_TYPE);
            asm.invokestatic(classCode.classType, spec.methodName(index), spec.methodDescriptor,
                    false);
            asm._return();
            asm.mark(jump);
        }
        asm.ldc(spec.defaultValue);
        asm._return();
        asm.end();
    }

    private <T> void generate(TypeSpec<T> spec, int index, T[] values, int from, int to) {
        int length = to - from;
        if (length == 0) {
            return;
        }
        Jump dflt = new Jump();
        Jump[] jumps = new Jump[length];
        for (int caseIndex = 0; caseIndex < length; ++caseIndex) {
            jumps[caseIndex] = new Jump();
        }
        InstructionAssembler asm = newMethod(spec.methodName(index), spec);
        asm.begin();
        asm.loadParameter(0, Type.INT_TYPE);
        asm.tableswitch(from, to - 1, dflt, jumps);
        for (int caseIndex = 0; caseIndex < length; ++caseIndex) {
            asm.mark(jumps[caseIndex]);
            asm.ldc(values[from + caseIndex]);
            asm._return();
        }
        asm.mark(dflt);
        asm.ldc(spec.defaultValue);
        asm._return();
        asm.end();
    }

    private <T> ConstantClassAssembler newMethod(String methodName, TypeSpec<T> spec) {
        MethodCode method = classCode.newMethod(Modifier.PUBLIC | Modifier.STATIC, methodName,
                spec.methodDescriptor, null, null);
        return new ConstantClassAssembler(method);
    }

    private void load(InstructionAssembler assembler, Object cst, int index, TypeSpec<?> spec) {
        assert 0 <= index && index <= EXTERN_CONSTANTS_LIMIT;
        assembler.iconst(index);
        assembler.invokestatic(classCode.classType, spec.methodName, spec.methodDescriptor, false);
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
