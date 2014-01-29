/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.HashMap;
import java.util.Map.Entry;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.compiler.Code.ClassCode;

/**
 * {@link MethodVisitor} implementation which delegates constant loads to a {@link ConstantPool}
 * instance
 */
class ConstantPoolMethodVisitor extends MethodVisitor {
    private final InlineConstantPool constantPool;

    public ConstantPoolMethodVisitor(MethodVisitor mv, InlineConstantPool constantPool) {
        super(Opcodes.ASM4, mv);
        this.constantPool = constantPool;
    }

    @Override
    public void visitLdcInsn(Object cst) {
        if (cst instanceof Integer) {
            constantPool.iconst(mv, (Integer) cst);
        } else if (cst instanceof Byte) {
            constantPool.iconst(mv, ((Byte) cst).intValue());
        } else if (cst instanceof Character) {
            constantPool.iconst(mv, (int) ((Character) cst).charValue());
        } else if (cst instanceof Short) {
            constantPool.iconst(mv, ((Short) cst).intValue());
        } else if (cst instanceof Boolean) {
            constantPool.iconst(mv, ((Boolean) cst).booleanValue() ? 1 : 0);
        } else if (cst instanceof Float) {
            constantPool.fconst(mv, (Float) cst);
        } else if (cst instanceof Long) {
            constantPool.lconst(mv, (Long) cst);
        } else if (cst instanceof Double) {
            constantPool.dconst(mv, (Double) cst);
        } else if (cst instanceof String) {
            constantPool.aconst(mv, (String) cst);
        } else if (cst instanceof Type) {
            super.visitLdcInsn(cst);
        } else if (cst instanceof Handle) {
            super.visitLdcInsn(cst);
        } else {
            throw new IllegalArgumentException();
        }
    }

    abstract static class ConstantPool {
        private final HashMap<Object, Integer> constantPool = new HashMap<>(64);
        private ConstantPool next;
        private int integers = 0;
        private int longs = 0;
        private int floats = 0;
        private int doubles = 0;
        private int strings = 0;

        protected final Code code;
        protected final int limit;

        protected ConstantPool(Code code, int limit) {
            assert 0 <= limit && limit <= Short.MAX_VALUE;
            this.code = code;
            this.limit = limit;
        }

        /**
         * Returns this pool's integer constants
         */
        protected final Integer[] getIntegers() {
            Integer[] constants = new Integer[integers];
            for (Entry<Object, Integer> entry : constantPool.entrySet()) {
                if (entry.getKey() instanceof Integer) {
                    constants[entry.getValue()] = (Integer) entry.getKey();
                }
            }
            return constants;
        }

        /**
         * Returns this pool's long constants
         */
        protected final Long[] getLongs() {
            Long[] constants = new Long[longs];
            for (Entry<Object, Integer> entry : constantPool.entrySet()) {
                if (entry.getKey() instanceof Long) {
                    constants[entry.getValue()] = (Long) entry.getKey();
                }
            }
            return constants;
        }

        /**
         * Returns this pool's float constants
         */
        protected final Float[] getFloats() {
            Float[] constants = new Float[floats];
            for (Entry<Object, Integer> entry : constantPool.entrySet()) {
                if (entry.getKey() instanceof Float) {
                    constants[entry.getValue()] = (Float) entry.getKey();
                }
            }
            return constants;
        }

        /**
         * Returns this pool's double constants
         */
        protected final Double[] getDoubles() {
            Double[] constants = new Double[doubles];
            for (Entry<Object, Integer> entry : constantPool.entrySet()) {
                if (entry.getKey() instanceof Double) {
                    constants[entry.getValue()] = (Double) entry.getKey();
                }
            }
            return constants;
        }

        /**
         * Returns this pool's string constants
         */
        protected final String[] getStrings() {
            String[] constants = new String[strings];
            for (Entry<Object, Integer> entry : constantPool.entrySet()) {
                if (entry.getKey() instanceof String) {
                    constants[entry.getValue()] = (String) entry.getKey();
                }
            }
            return constants;
        }

        private boolean isConstantPoolFull() {
            return constantPool.size() >= limit;
        }

        private ConstantPool getNext() {
            if (next == null) {
                next = newConstantPool();
            }
            return next;
        }

        public final void iconst(MethodVisitor mv, Integer cst) {
            Integer index = constantPool.get(cst);
            if (index == null) {
                if (isConstantPoolFull()) {
                    getNext().iconst(mv, cst);
                    return;
                }
                index = Integer.valueOf(integers++);
                constantPool.put(cst, index);
            }
            iconst(mv, cst, index);
        }

        public final void lconst(MethodVisitor mv, Long cst) {
            Integer index = constantPool.get(cst);
            if (index == null) {
                if (isConstantPoolFull()) {
                    getNext().lconst(mv, cst);
                    return;
                }
                index = Integer.valueOf(longs++);
                constantPool.put(cst, index);
            }
            lconst(mv, cst, index);
        }

        public final void fconst(MethodVisitor mv, Float cst) {
            Integer index = constantPool.get(cst);
            if (index == null) {
                if (isConstantPoolFull()) {
                    getNext().fconst(mv, cst);
                    return;
                }
                index = Integer.valueOf(floats++);
                constantPool.put(cst, index);
            }
            fconst(mv, cst, index);
        }

        public final void dconst(MethodVisitor mv, Double cst) {
            Integer index = constantPool.get(cst);
            if (index == null) {
                if (isConstantPoolFull()) {
                    getNext().dconst(mv, cst);
                    return;
                }
                index = Integer.valueOf(doubles++);
                constantPool.put(cst, index);
            }
            dconst(mv, cst, index);
        }

        public final void aconst(MethodVisitor mv, String cst) {
            String key = cst;
            Integer index = constantPool.get(key);
            if (index == null) {
                if (isConstantPoolFull()) {
                    getNext().aconst(mv, cst);
                    return;
                }
                index = Integer.valueOf(strings++);
                constantPool.put(key, index);
            }
            aconst(mv, cst, index);
        }

        /**
         * Close this constant pool
         */
        protected abstract void close();

        /**
         * Create a new constant pool when this pool's limit has been exceeded
         */
        protected abstract ConstantPool newConstantPool();

        /**
         * Load the indexed integer constant {@code cst} for the given method
         */
        protected abstract void iconst(MethodVisitor mv, Integer cst, int index);

        /**
         * Load the indexed long constant {@code cst} for the given method
         */
        protected abstract void lconst(MethodVisitor mv, Long cst, int index);

        /**
         * Load the indexed float constant {@code cst} for the given method
         */
        protected abstract void fconst(MethodVisitor mv, Float cst, int index);

        /**
         * Load the indexed double constant {@code cst} for the given method
         */
        protected abstract void dconst(MethodVisitor mv, Double cst, int index);

        /**
         * Load the indexed string constant {@code cst} for the given method
         */
        protected abstract void aconst(MethodVisitor mv, String cst, int index);
    }

    static final class InlineConstantPool extends ConstantPool {
        private static final int INLINE_CONSTANTS_LIMIT = 0x2000;

        InlineConstantPool(Code code) {
            super(code, INLINE_CONSTANTS_LIMIT);
        }

        @Override
        protected void close() {
            // empty
        }

        @Override
        protected ConstantPool newConstantPool() {
            return code.getExternConstantPool();
        }

        @Override
        protected void iconst(MethodVisitor mv, Integer cst, int index) {
            mv.visitLdcInsn(cst);
        }

        @Override
        protected void lconst(MethodVisitor mv, Long cst, int index) {
            mv.visitLdcInsn(cst);
        }

        @Override
        protected void fconst(MethodVisitor mv, Float cst, int index) {
            mv.visitLdcInsn(cst);
        }

        @Override
        protected void dconst(MethodVisitor mv, Double cst, int index) {
            mv.visitLdcInsn(cst);
        }

        @Override
        protected void aconst(MethodVisitor mv, String cst, int index) {
            mv.visitLdcInsn(cst);
        }
    }

    static final class ExternConstantPool extends ConstantPool {
        private static final int EXTERN_CONSTANTS_LIMIT = 0x6000;
        private static final int METHOD_LIMIT = 0x400;
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
            assert !closed : "constant pool closed";
            closed = true;
            ClassWriter cw = classCode.classWriter;
            generate(cw, "getInt", "(I)I", Type.INT_TYPE, 0, getIntegers());
            generate(cw, "getLong", "(I)J", Type.LONG_TYPE, 0L, getLongs());
            generate(cw, "getFloat", "(I)F", Type.FLOAT_TYPE, 0f, getFloats());
            generate(cw, "getDouble", "(I)D", Type.DOUBLE_TYPE, 0d, getDoubles());
            generate(cw, "getString", "(I)Ljava/lang/String;", Types.String, "", getStrings());
        }

        private <T> void generate(ClassWriter cw, String name, String desc, Type type,
                T defaultValue, T[] values) {
            if (values.length <= METHOD_LIMIT) {
                generate(cw, name, desc, type, defaultValue, values, 0, values.length);
            } else {
                // split into multiple methods
                for (int i = 0, index = 0; i < values.length; i += METHOD_LIMIT, ++index) {
                    int from = i, to = Math.min(from + METHOD_LIMIT, values.length);
                    generate(cw, name + "_" + index, desc, type, defaultValue, values, from, to);
                }
                // generate entry method
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name,
                        desc, null, null);
                mv.visitCode();
                for (int i = 0, index = 0; i < values.length; i += METHOD_LIMIT, ++index) {
                    int from = i, to = Math.min(from + METHOD_LIMIT, values.length);
                    Label label = new Label();
                    mv.visitVarInsn(Opcodes.ILOAD, 0);
                    mv.visitIntInsn(Opcodes.SIPUSH, to);
                    mv.visitJumpInsn(Opcodes.IF_ICMPGE, label);
                    mv.visitVarInsn(Opcodes.ILOAD, 0);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, classCode.className, name + "_"
                            + index, desc);
                    mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
                    mv.visitLabel(label);
                }
                mv.visitLdcInsn(defaultValue);
                mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }

        private <T> void generate(ClassWriter cw, String name, String desc, Type type,
                T defaultValue, T[] values, int from, int to) {
            int length = to - from;
            if (length == 0) {
                return;
            }
            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, desc,
                    null, null);
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
                mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
            }
            mv.visitLabel(dflt);
            mv.visitLdcInsn(defaultValue);
            mv.visitInsn(type.getOpcode(Opcodes.IRETURN));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private void load(MethodVisitor mv, Object cst, int index, String name, String desc) {
            assert 0 <= index && index <= EXTERN_CONSTANTS_LIMIT;
            if (index <= 5) {
                mv.visitInsn(Opcodes.ICONST_0 + index);
            } else if (index <= Byte.MAX_VALUE) {
                mv.visitIntInsn(Opcodes.BIPUSH, index);
            } else if (index <= Short.MAX_VALUE) {
                mv.visitIntInsn(Opcodes.SIPUSH, index);
            } else {
                throw new IllegalArgumentException();
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, classCode.className, name, desc);
        }

        @Override
        protected void iconst(MethodVisitor mv, Integer cst, int index) {
            load(mv, cst, index, "getInt", "(I)I");
        }

        @Override
        protected void lconst(MethodVisitor mv, Long cst, int index) {
            load(mv, cst, index, "getLong", "(I)J");
        }

        @Override
        protected void fconst(MethodVisitor mv, Float cst, int index) {
            load(mv, cst, index, "getFloat", "(I)F");
        }

        @Override
        protected void dconst(MethodVisitor mv, Double cst, int index) {
            load(mv, cst, index, "getDouble", "(I)D");
        }

        @Override
        protected void aconst(MethodVisitor mv, String cst, int index) {
            load(mv, cst, index, "getString", "(I)Ljava/lang/String;");
        }
    }
}
