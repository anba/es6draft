/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.Arrays;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.github.anba.es6draft.compiler.InstructionVisitor.VariablesView;

/**
 * Basic stack type information tracking, complex control instructions are not supported
 */
final class StackMethodVisitor extends MethodVisitor {
    private static final int MIN_STACK_SIZE = 8;
    private static final Type OBJECT_TYPE = Types.Object;
    private VariablesView variables;
    private Type[] stack;
    private int sp;

    private static final class LabelInfo {
        final Type[] stack;
        boolean resolved;

        LabelInfo(Type[] stack, boolean resolved) {
            this.resolved = resolved;
            this.stack = stack;
        }
    }

    public StackMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM4, mv);
    }

    /**
     * Returns the underlying method visitor
     */
    public MethodVisitor getMethodVisitor() {
        return mv;
    }

    /**
     * Provide access to variables type information
     */
    public void setVariables(VariablesView variables) {
        this.variables = variables;
    }

    /**
     * Start exception handler for {@code exception}
     */
    public void catchHandler(Type exception) {
        if (stack == null) {
            newStack();
        }
        sp = 0;
        push(exception);
    }

    /**
     * Return a copy of the current stack types
     */
    public Type[] getStack() {
        return Arrays.copyOf(stack, sp);
    }

    /**
     * Replace the stack type information with the supplied types
     */
    public void setStack(Type[] stack) {
        assert stack != null;
        int newSize = Math.max(MIN_STACK_SIZE, Integer.highestOneBit(stack.length) << 1);
        setStack(Arrays.copyOf(stack, newSize), stack.length);
    }

    /**
     * Replace the stack type information with the information from {@code label}
     */
    public void setStack(Label label) {
        LabelInfo info = getInfo(label);
        assert info != null : "label.info is null";
        assert stack == null : "current stack is not undefined";
        setStack(info.stack);
    }

    private void setStack(Type[] stack, int sp) {
        this.stack = stack;
        this.sp = sp;
    }

    private void increaseStack(int newSize) {
        stack = Arrays.copyOf(stack, newSize);
    }

    private void newStack() {
        assert sp == 0 : getStackString();
        setStack(new Type[MIN_STACK_SIZE], 0);
    }

    private void discardStack() {
        assert stack != null;
        setStack(null, 0);
    }

    private String getStackString() {
        if (stack == null) {
            return "<null>";
        }
        return Arrays.toString(getStack());
    }

    private LabelInfo getInfo(Label label) {
        return (LabelInfo) label.info;
    }

    private void setInfo(Label label, boolean resolved) {
        label.info = new LabelInfo(getStack(), resolved);
    }

    private static Type type(int sort) {
        switch (sort) {
        case Type.VOID:
            return Type.VOID_TYPE;
        case Type.BOOLEAN:
            return Type.BOOLEAN_TYPE;
        case Type.CHAR:
            return Type.CHAR_TYPE;
        case Type.BYTE:
            return Type.BYTE_TYPE;
        case Type.SHORT:
            return Type.SHORT_TYPE;
        case Type.INT:
            return Type.INT_TYPE;
        case Type.FLOAT:
            return Type.FLOAT_TYPE;
        case Type.LONG:
            return Type.LONG_TYPE;
        case Type.DOUBLE:
            return Type.DOUBLE_TYPE;
        case Type.ARRAY:
            return Types.Object_;
        case Type.OBJECT:
            return Types.Object;
        case Type.METHOD:
        default:
            return null;
        }
    }

    private static void assertEqualTypes(Type[] stack, int sp, Type[] labelStack, int labelsp) {
        assert sp == labelsp : String.format("%d != %d (%s, %s)", sp, labelsp,
                Arrays.toString(stack), Arrays.toString(labelStack));
        for (int i = 0; i < sp; ++i) {
            Type t0 = stack[i], t1 = labelStack[i];
            assert t0.equals(t1) : String.format("%s != %s", t0, t1);
        }
    }

    private void jump(Label... labels) {
        for (Label label : labels) {
            jump(label);
        }
    }

    private void jump(Label label) {
        LabelInfo info = getInfo(label);
        if (info == null) {
            // label not yet visited => forward jump
            setInfo(label, false);
        } else if (info.resolved) {
            // label already visited
            assertEqualTypes(stack, sp, info.stack, info.stack.length);
        } else {
            // update label stack state
            alignStack(info.stack, info.stack.length, stack, sp);
        }
    }

    private void label(Label label) {
        LabelInfo info = getInfo(label);
        if (info == null) {
            // new backward label
            if (stack == null) {
                // label after discard stack (goto, return, throw), create new stack
                newStack();
            }
            setInfo(label, true);
        } else if (stack == null) {
            // label after discard stack (goto, return, throw), retrieve stack from label
            setStack(info.stack);
        } else {
            assert !info.resolved;
            info.resolved = true;
            // update stack state
            alignStack(stack, sp, info.stack, info.stack.length);
        }
    }

    private void alignStack(Type[] stack, int sp, Type[] labelStack, int labelsp) {
        assert sp == labelsp : String.format("%d != %d (%s, %s)", sp, labelsp,
                Arrays.toString(stack), Arrays.toString(labelStack));
        for (int i = 0; i < sp; ++i) {
            Type t0 = stack[i], t1 = labelStack[i];
            if (!t0.equals(t1)) {
                assert compatibleTypes(t0, t1);
                if (t0.getSort() <= Type.INT) {
                    stack[i] = type(Math.max(Math.max(t0.getSort(), t1.getSort()), Type.INT));
                } else if (t0.getSort() <= Type.DOUBLE) {
                    stack[i] = Type.DOUBLE_TYPE;
                } else {
                    stack[i] = commonType(t0, t1);
                }
            }
        }
    }

    private static boolean compatibleTypes(Type left, Type right) {
        return left == right || left.getSize() == right.getSize()
                && left.getSort() < Type.ARRAY == right.getSort() < Type.ARRAY;
    }

    private static Type commonType(Type left, Type right) {
        // hard coded type relationships to avoid dynamic class loading...
        Type commonType = OBJECT_TYPE;
        if (isCharSequence(left)) {
            if (isCharSequence(right)) {
                commonType = Types.CharSequence;
            }
        } else if (isNumber(left)) {
            if (isNumber(right)) {
                commonType = Types.Number;
            }
        } else if (isFunctionObject(left)) {
            if (isFunctionObject(right)) {
                commonType = Types.FunctionObject;
            }
        } else if (isScriptObject(left)) {
            if (isScriptObject(right)) {
                commonType = Types.ScriptObject;
            }
        }
        return commonType;
    }

    private static boolean isCharSequence(Type type) {
        return Types.String.equals(type) || Types.CharSequence.equals(type);
    }

    private static boolean isNumber(Type type) {
        return Types.Integer.equals(type) || Types.Long.equals(type) || Types.Double.equals(type);
    }

    private static boolean isFunctionObject(Type type) {
        return Types.OrdinaryFunction.equals(type) || Types.OrdinaryGenerator.equals(type)
                || Types.FunctionObject.equals(type);
    }

    private static boolean isScriptObject(Type type) {
        return Types.ScriptObject.equals(type) || Types.OrdinaryObject.equals(type)
                || Types.ExoticArray.equals(type) || Types.ExoticArguments.equals(type)
                || Types.RegExpObject.equals(type) || Types.GeneratorObject.equals(type);
    }

    private Type peek() {
        assert sp > 0 : getStackString();
        return stack[sp - 1];
    }

    private Type pop(int size) {
        assert sp > 0 : getStackString();
        Type t = stack[--sp];
        assert t.getSize() == size : String.format("%d[%s] != %d, %s", t.getSize(), t, size,
                getStackString());
        return t;
    }

    private void pop(Type type) {
        assert sp > 0 : getStackString();
        Type t = stack[--sp];
        assert compatibleTypes(t, type) : String.format("%s != %s, %s", t, type, getStackString());
    }

    private void push(Type type) {
        if (sp == stack.length) {
            increaseStack(sp << 1);
        }
        stack[sp++] = type;
    }

    private void load(int var, Type type) {
        Type t = variables.getVariable(var);
        assert compatibleTypes(type, t);
        push(t);
    }

    private void store(int var, Type type) {
        Type t = variables.getVariable(var);
        assert compatibleTypes(type, t);
        pop(type);
    }

    private void array() {
        pop(OBJECT_TYPE);
    }

    private void anewarray(Type type) {
        pop(Type.INT_TYPE);
        push(Type.getType("[" + type.getDescriptor()));
    }

    private void aload(Type type) {
        pop(Type.INT_TYPE);
        array();
        push(type);
    }

    private void astore(Type type) {
        pop(type);
        pop(Type.INT_TYPE);
        array();
    }

    private void cast(Type from, Type to) {
        pop(from);
        push(to);
    }

    private void constant(Type type) {
        push(type);
    }

    private void neg(Type type) {
        pop(type);
        push(type);
    }

    private void arithmetic(Type type) {
        pop(type);
        pop(type);
        push(type);
    }

    private void shift(Type type) {
        pop(Type.INT_TYPE);
        pop(type);
        push(type);
    }

    private void areturn(Type type) {
        if (type.getSort() != Type.VOID) {
            pop(type);
        }
        assert sp == 0 : String.format("sp=%d, stack=%s", sp, getStackString());
        discardStack();
    }

    private void athrow() {
        pop(OBJECT_TYPE);
        discardStack();
    }

    private void invoke(int opcode, String desc) {
        Type ret = Type.getReturnType(desc);
        Type[] arguments = Type.getArgumentTypes(desc);
        for (int i = arguments.length - 1; i >= 0; --i) {
            pop(arguments[i]);
        }
        switch (opcode) {
        case Opcodes.INVOKEVIRTUAL:
        case Opcodes.INVOKESPECIAL:
        case Opcodes.INVOKEINTERFACE:
            pop(OBJECT_TYPE);
        }
        if (ret.getSort() != Type.VOID) {
            push(ret);
        }
    }

    @Override
    public void visitCode() {
        super.visitCode();
        newStack();
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        assert sp == 0;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);

        switch (opcode) {
        case Opcodes.GETSTATIC:
            push(Type.getType(desc));
            break;
        case Opcodes.PUTSTATIC:
            pop(Type.getType(desc));
            break;
        case Opcodes.GETFIELD:
            pop(Type.getObjectType(name));
            push(Type.getType(desc));
            break;
        case Opcodes.PUTFIELD:
            pop(Type.getType(desc));
            pop(Type.getObjectType(name));
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);

        switch (opcode) {
        case Opcodes.NOP:
            break;

        case Opcodes.ACONST_NULL:
            constant(OBJECT_TYPE);
            break;
        case Opcodes.ICONST_M1:
        case Opcodes.ICONST_0:
        case Opcodes.ICONST_1:
        case Opcodes.ICONST_2:
        case Opcodes.ICONST_3:
        case Opcodes.ICONST_4:
        case Opcodes.ICONST_5:
            constant(Type.INT_TYPE);
            break;
        case Opcodes.LCONST_0:
        case Opcodes.LCONST_1:
            constant(Type.LONG_TYPE);
            break;
        case Opcodes.FCONST_0:
        case Opcodes.FCONST_1:
        case Opcodes.FCONST_2:
            constant(Type.FLOAT_TYPE);
            break;
        case Opcodes.DCONST_0:
        case Opcodes.DCONST_1:
            constant(Type.DOUBLE_TYPE);
            break;

        case Opcodes.IALOAD:
            aload(Type.INT_TYPE);
            break;
        case Opcodes.LALOAD:
            aload(Type.LONG_TYPE);
            break;
        case Opcodes.FALOAD:
            aload(Type.FLOAT_TYPE);
            break;
        case Opcodes.DALOAD:
            aload(Type.DOUBLE_TYPE);
            break;
        case Opcodes.AALOAD:
            aload(OBJECT_TYPE);
            break;
        case Opcodes.BALOAD:
            aload(Type.BYTE_TYPE);
            break;
        case Opcodes.CALOAD:
            aload(Type.CHAR_TYPE);
            break;
        case Opcodes.SALOAD:
            aload(Type.SHORT_TYPE);
            break;

        case Opcodes.IASTORE:
            astore(Type.INT_TYPE);
            break;
        case Opcodes.LASTORE:
            astore(Type.LONG_TYPE);
            break;
        case Opcodes.FASTORE:
            astore(Type.FLOAT_TYPE);
            break;
        case Opcodes.DASTORE:
            astore(Type.DOUBLE_TYPE);
            break;
        case Opcodes.AASTORE:
            astore(OBJECT_TYPE);
            break;
        case Opcodes.BASTORE:
            astore(Type.BYTE_TYPE);
            break;
        case Opcodes.CASTORE:
            astore(Type.CHAR_TYPE);
            break;
        case Opcodes.SASTORE:
            astore(Type.SHORT_TYPE);
            break;

        case Opcodes.POP:
            pop(1);
            break;
        case Opcodes.POP2:
            if (peek().getSize() == 2) {
                pop(2);
            } else {
                pop(1);
                pop(1);
            }
            break;
        case Opcodes.DUP: {
            Type t0 = pop(1);
            push(t0);
            push(t0);
            break;
        }
        case Opcodes.DUP_X1: {
            Type t0 = pop(1);
            Type t1 = pop(1);
            push(t0);
            push(t1);
            push(t0);
            break;
        }
        case Opcodes.DUP_X2: {
            Type t0 = pop(1);
            if (peek().getSize() == 2) {
                Type t1 = pop(2);
                push(t0);
                push(t1);
                push(t0);
            } else {
                Type t1 = pop(1);
                Type t2 = pop(1);
                push(t0);
                push(t2);
                push(t1);
                push(t0);
            }
            break;
        }
        case Opcodes.DUP2: {
            if (peek().getSize() == 2) {
                Type t0 = pop(2);
                push(t0);
                push(t0);
            } else {
                Type t0 = pop(1);
                Type t1 = pop(1);
                push(t1);
                push(t0);
                push(t1);
                push(t0);
            }
            break;
        }
        case Opcodes.DUP2_X1: {
            if (peek().getSize() == 2) {
                Type t0 = pop(2);
                Type t2 = pop(1);
                push(t0);
                push(t2);
                push(t0);
            } else {
                Type t0 = pop(1);
                Type t1 = pop(1);
                Type t2 = pop(1);
                push(t1);
                push(t0);
                push(t2);
                push(t1);
                push(t0);
            }
            break;
        }
        case Opcodes.DUP2_X2: {
            if (peek().getSize() == 2) {
                Type t0 = pop(2);
                if (peek().getSize() == 2) {
                    Type t2 = pop(2);
                    push(t0);
                    push(t2);
                    push(t0);
                } else {
                    Type t2 = pop(1);
                    Type t3 = pop(1);
                    push(t0);
                    push(t3);
                    push(t2);
                    push(t0);
                }
            } else {
                Type t0 = pop(1);
                Type t1 = pop(1);
                if (peek().getSize() == 2) {
                    Type t2 = pop(2);
                    push(t1);
                    push(t0);
                    push(t2);
                    push(t1);
                    push(t0);
                } else {
                    Type t2 = pop(1);
                    Type t3 = pop(1);
                    push(t1);
                    push(t0);
                    push(t3);
                    push(t2);
                    push(t1);
                    push(t0);
                }
            }
            break;
        }
        case Opcodes.SWAP: {
            Type t0 = pop(1);
            Type t1 = pop(1);
            push(t0);
            push(t1);
            break;
        }

        case Opcodes.IADD:
        case Opcodes.ISUB:
        case Opcodes.IMUL:
        case Opcodes.IDIV:
        case Opcodes.IREM:
        case Opcodes.IAND:
        case Opcodes.IOR:
        case Opcodes.IXOR:
            arithmetic(Type.INT_TYPE);
            break;
        case Opcodes.LADD:
        case Opcodes.LSUB:
        case Opcodes.LMUL:
        case Opcodes.LDIV:
        case Opcodes.LREM:
        case Opcodes.LAND:
        case Opcodes.LOR:
        case Opcodes.LXOR:
            arithmetic(Type.LONG_TYPE);
            break;
        case Opcodes.FADD:
        case Opcodes.FSUB:
        case Opcodes.FMUL:
        case Opcodes.FDIV:
        case Opcodes.FREM:
            arithmetic(Type.FLOAT_TYPE);
            break;
        case Opcodes.DADD:
        case Opcodes.DSUB:
        case Opcodes.DMUL:
        case Opcodes.DDIV:
        case Opcodes.DREM:
            arithmetic(Type.DOUBLE_TYPE);
            break;

        case Opcodes.INEG:
            neg(Type.INT_TYPE);
            break;
        case Opcodes.LNEG:
            neg(Type.LONG_TYPE);
            break;
        case Opcodes.FNEG:
            neg(Type.FLOAT_TYPE);
            break;
        case Opcodes.DNEG:
            neg(Type.DOUBLE_TYPE);
            break;

        case Opcodes.ISHL:
        case Opcodes.ISHR:
        case Opcodes.IUSHR:
            shift(Type.INT_TYPE);
            break;
        case Opcodes.LSHL:
        case Opcodes.LSHR:
        case Opcodes.LUSHR:
            shift(Type.LONG_TYPE);
            break;

        case Opcodes.I2L:
            cast(Type.INT_TYPE, Type.LONG_TYPE);
            break;
        case Opcodes.I2F:
            cast(Type.INT_TYPE, Type.FLOAT_TYPE);
            break;
        case Opcodes.I2D:
            cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
            break;
        case Opcodes.L2I:
            cast(Type.LONG_TYPE, Type.INT_TYPE);
            break;
        case Opcodes.L2F:
            cast(Type.LONG_TYPE, Type.FLOAT_TYPE);
            break;
        case Opcodes.L2D:
            cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
            break;
        case Opcodes.F2I:
            cast(Type.FLOAT_TYPE, Type.INT_TYPE);
            break;
        case Opcodes.F2L:
            cast(Type.FLOAT_TYPE, Type.LONG_TYPE);
            break;
        case Opcodes.F2D:
            cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
            break;
        case Opcodes.D2I:
            cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
            break;
        case Opcodes.D2L:
            cast(Type.DOUBLE_TYPE, Type.LONG_TYPE);
            break;
        case Opcodes.D2F:
            cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
            break;
        case Opcodes.I2B:
            cast(Type.INT_TYPE, Type.BYTE_TYPE);
            break;
        case Opcodes.I2C:
            cast(Type.INT_TYPE, Type.CHAR_TYPE);
            break;
        case Opcodes.I2S:
            cast(Type.INT_TYPE, Type.SHORT_TYPE);
            break;

        case Opcodes.LCMP:
            pop(Type.LONG_TYPE);
            pop(Type.LONG_TYPE);
            push(Type.INT_TYPE);
            break;
        case Opcodes.FCMPL:
        case Opcodes.FCMPG:
            pop(Type.FLOAT_TYPE);
            pop(Type.FLOAT_TYPE);
            push(Type.INT_TYPE);
            break;
        case Opcodes.DCMPL:
        case Opcodes.DCMPG:
            pop(Type.DOUBLE_TYPE);
            pop(Type.DOUBLE_TYPE);
            push(Type.INT_TYPE);
            break;

        case Opcodes.IRETURN:
            areturn(Type.INT_TYPE);
            break;
        case Opcodes.LRETURN:
            areturn(Type.LONG_TYPE);
            break;
        case Opcodes.FRETURN:
            areturn(Type.FLOAT_TYPE);
            break;
        case Opcodes.DRETURN:
            areturn(Type.DOUBLE_TYPE);
            break;
        case Opcodes.ARETURN:
            areturn(OBJECT_TYPE);
            break;
        case Opcodes.RETURN:
            areturn(Type.VOID_TYPE);
            break;

        case Opcodes.ARRAYLENGTH:
            array();
            push(Type.INT_TYPE);
            break;

        case Opcodes.ATHROW:
            athrow();
            break;

        case Opcodes.MONITORENTER:
        case Opcodes.MONITOREXIT:
            pop(OBJECT_TYPE);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);

        switch (opcode) {
        case Opcodes.BIPUSH:
            push(Type.BYTE_TYPE);
            break;
        case Opcodes.SIPUSH:
            push(Type.SHORT_TYPE);
            break;
        case Opcodes.NEWARRAY:
            switch (operand) {
            case Opcodes.T_BOOLEAN:
                anewarray(Type.BOOLEAN_TYPE);
                break;
            case Opcodes.T_CHAR:
                anewarray(Type.CHAR_TYPE);
                break;
            case Opcodes.T_BYTE:
                anewarray(Type.BYTE_TYPE);
                break;
            case Opcodes.T_SHORT:
                anewarray(Type.SHORT_TYPE);
                break;
            case Opcodes.T_INT:
                anewarray(Type.INT_TYPE);
                break;
            case Opcodes.T_FLOAT:
                anewarray(Type.FLOAT_TYPE);
                break;
            case Opcodes.T_LONG:
                anewarray(Type.LONG_TYPE);
                break;
            case Opcodes.T_DOUBLE:
                anewarray(Type.DOUBLE_TYPE);
                break;
            default:
                throw new IllegalArgumentException();
            }
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);

        invoke(Opcodes.INVOKEDYNAMIC, desc);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);

        switch (opcode) {
        case Opcodes.IFEQ:
        case Opcodes.IFNE:
        case Opcodes.IFLT:
        case Opcodes.IFGE:
        case Opcodes.IFGT:
        case Opcodes.IFLE:
            pop(Type.INT_TYPE);
            break;
        case Opcodes.IF_ICMPEQ:
        case Opcodes.IF_ICMPNE:
        case Opcodes.IF_ICMPLT:
        case Opcodes.IF_ICMPGE:
        case Opcodes.IF_ICMPGT:
        case Opcodes.IF_ICMPLE:
            pop(Type.INT_TYPE);
            pop(Type.INT_TYPE);
            break;
        case Opcodes.IF_ACMPEQ:
        case Opcodes.IF_ACMPNE:
            pop(OBJECT_TYPE);
            pop(OBJECT_TYPE);
            break;
        case Opcodes.IFNULL:
        case Opcodes.IFNONNULL:
            pop(OBJECT_TYPE);
            break;
        case Opcodes.GOTO:
            break;
        case Opcodes.JSR:
        default:
            throw new IllegalArgumentException();
        }

        jump(label);

        if (opcode == Opcodes.GOTO) {
            discardStack();
        }
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);

        label(label);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);

        if (cst instanceof Integer) {
            push(Type.INT_TYPE);
        } else if (cst instanceof Byte) {
            push(Type.BYTE_TYPE);
        } else if (cst instanceof Character) {
            push(Type.CHAR_TYPE);
        } else if (cst instanceof Short) {
            push(Type.SHORT_TYPE);
        } else if (cst instanceof Boolean) {
            push(Type.BOOLEAN_TYPE);
        } else if (cst instanceof Float) {
            push(Type.FLOAT_TYPE);
        } else if (cst instanceof Long) {
            push(Type.LONG_TYPE);
        } else if (cst instanceof Double) {
            push(Type.DOUBLE_TYPE);
        } else if (cst instanceof String) {
            push(Types.String);
        } else if (cst instanceof Type) {
            int sort = ((Type) cst).getSort();
            if (sort == Type.OBJECT || sort == Type.ARRAY) {
                push(Types.Class);
            } else if (sort == Type.METHOD) {
                push(Types.MethodType);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (cst instanceof Handle) {
            push(Types.MethodHandle);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);

        pop(Type.INT_TYPE);
        jump(dflt);
        jump(labels);
        discardStack();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        super.visitMethodInsn(opcode, owner, name, desc);

        invoke(opcode, desc);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);

        for (int i = dims; i > 0; --i) {
            pop(Type.INT_TYPE);
        }
        push(Type.getType(desc));
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);

        pop(Type.INT_TYPE);
        jump(dflt);
        jump(labels);
        discardStack();
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);

        switch (opcode) {
        case Opcodes.NEW:
            push(Type.getObjectType(type));
            break;
        case Opcodes.ANEWARRAY:
            anewarray(Type.getObjectType(type));
            break;
        case Opcodes.CHECKCAST:
            pop(OBJECT_TYPE);
            push(Type.getObjectType(type));
            break;
        case Opcodes.INSTANCEOF:
            pop(OBJECT_TYPE);
            push(Type.INT_TYPE);
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);

        switch (opcode) {
        case Opcodes.ILOAD:
            load(var, Type.INT_TYPE);
            break;
        case Opcodes.LLOAD:
            load(var, Type.LONG_TYPE);
            break;
        case Opcodes.FLOAD:
            load(var, Type.FLOAT_TYPE);
            break;
        case Opcodes.DLOAD:
            load(var, Type.DOUBLE_TYPE);
            break;
        case Opcodes.ALOAD:
            load(var, OBJECT_TYPE);
            break;

        case Opcodes.ISTORE:
            store(var, Type.INT_TYPE);
            break;
        case Opcodes.LSTORE:
            store(var, Type.LONG_TYPE);
            break;
        case Opcodes.FSTORE:
            store(var, Type.FLOAT_TYPE);
            break;
        case Opcodes.DSTORE:
            store(var, Type.DOUBLE_TYPE);
            break;
        case Opcodes.ASTORE:
            store(var, OBJECT_TYPE);
            break;

        case Opcodes.RET:
        default:
            throw new IllegalArgumentException();
        }
    }
}
