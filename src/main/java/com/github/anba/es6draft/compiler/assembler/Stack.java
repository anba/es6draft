/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.Arrays;

/**
 * Basic stack type information tracking, complex control instructions are not supported.
 */
public class Stack {
    private static final int MIN_STACK_SIZE = 8;
    private static final Type OBJECT_TYPE = Types.Object;
    private static final Type STRING_TYPE = Types.String;
    private static final Type[] EMPTY_STACK = new Type[0];

    private final Variables variables;
    private Type[] stack;
    private int sp;

    public Stack(Variables variables) {
        this.variables = variables;
        newStack();
    }

    /**
     * Return a copy of the current stack.
     * 
     * @return the current stack
     */
    public final Type[] getStack() {
        if (sp == 0) {
            return EMPTY_STACK;
        }
        return Arrays.copyOf(stack, sp);
    }

    /**
     * Return the current stack pointer.
     * 
     * @return the current stack pointer
     */
    public final int getStackPointer() {
        return sp;
    }

    /**
     * Start exception handler for {@code exception}.
     * 
     * @param exception
     *            the exception type for the catch handler
     */
    void catchHandler(Type exception) {
        if (stack == null) {
            newStack();
        }
        sp = 0;
        push(exception);
    }

    /**
     * Replace the stack type information with the supplied types.
     * 
     * @param stack
     *            the new stack
     */
    void setStack(Type[] stack) {
        assert stack != null;
        int newSize = Math.max(MIN_STACK_SIZE, Integer.highestOneBit(stack.length) << 1);
        setStack(Arrays.copyOf(stack, newSize), stack.length);
    }

    Type[] getStackDirect() {
        return stack;
    }

    void setStack(Type[] stack, int sp) {
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
        // assert stack != null;
        setStack(null, 0);
    }

    private String getStackString() {
        if (stack == null) {
            return "<null>";
        }
        // Replace [] with {}
        StringBuilder sb = new StringBuilder(Arrays.toString(getStack()));
        sb.setCharAt(0, '{');
        sb.setCharAt(sb.length() - 1, '}');
        return sb.toString();
    }

    private static boolean assertEqualTypes(Type[] stack, int sp, Type[] labelStack, int labelsp) {
        assert sp == labelsp : String.format("%d != %d (%s, %s)", sp, labelsp, Arrays.toString(stack),
                Arrays.toString(labelStack));
        for (int i = 0; i < sp; ++i) {
            Type t0 = stack[i], t1 = labelStack[i];
            assert t0.equals(t1) : String.format("%s != %s", t0, t1);
        }
        return true;
    }

    private void alignStack(Type[] stack, int sp, Type[] labelStack, int labelsp) {
        assert sp == labelsp : String.format("%d != %d (%s, %s)", sp, labelsp, Arrays.toString(stack),
                Arrays.toString(labelStack));
        for (int i = 0; i < sp; ++i) {
            Type t0 = stack[i], t1 = labelStack[i];
            if (!t0.equals(t1)) {
                assert compatibleTypes(t0, t1);
                if (t0.getSort() <= Type.Sort.INT) {
                    stack[i] = Type.of(Math.max(Math.max(t0.getSort(), t1.getSort()), Type.Sort.INT));
                } else if (t0.getSort() <= Type.Sort.DOUBLE) {
                    stack[i] = Type.DOUBLE_TYPE;
                } else {
                    stack[i] = intersectionType(t0, t1);
                }
            }
        }
    }

    private static boolean compatibleTypes(Type left, Type right) {
        return left == right || left.getSize() == right.getSize()
                && left.getSort() < Type.Sort.ARRAY == right.getSort() < Type.Sort.ARRAY;
    }

    protected Type intersectionType(Type left, Type right) {
        return OBJECT_TYPE;
    }

    /* stack operations */

    protected Type peek() {
        assert sp > 0 : getStackString();
        return stack[sp - 1];
    }

    protected Type peek(int n) {
        assert n < 0;
        assert sp + n >= 0 : getStackString();
        return stack[sp + n];
    }

    protected Type pop(int size) {
        assert sp > 0 : getStackString();
        Type t = stack[--sp];
        assert t.getSize() == size : String.format("%d[%s] != %d, %s", t.getSize(), t, size, getStackString());
        return t;
    }

    protected void pop(Type type) {
        assert sp > 0 : getStackString();
        Type t = stack[--sp];
        assert compatibleTypes(t, type) : String.format("%s != %s, %s", t, type, getStackString());
    }

    protected void push(Type type) {
        if (sp == stack.length) {
            increaseStack(sp << 1);
        }
        stack[sp++] = type;
    }

    /* */

    protected void jmp(Jump jump) {
        Type[] labelStack = jump.stack();
        if (labelStack == null) {
            // label not yet visited => forward jump
            jump.setStack(getStack());
        } else if (jump.isResolved()) {
            // label already resolved
            assert assertEqualTypes(stack, sp, labelStack, labelStack.length);
        } else {
            // update label stack state
            alignStack(labelStack, labelStack.length, stack, sp);
        }
    }

    protected void label(Jump jump) {
        Type[] labelStack = jump.stack();
        if (labelStack == null) {
            // new backward label
            if (stack == null) {
                // label after discard stack (goto, return, throw), create new stack (or assert in
                // debug mode)
                assert false : "newStack after discard stack";
                newStack();
            }
            jump.setStack(getStack());
        } else if (stack == null) {
            // label after discard stack (goto, return, throw), retrieve stack from label
            setStack(labelStack);
        } else {
            // update stack state
            alignStack(stack, sp, labelStack, labelStack.length);
        }
    }

    /* */

    public final void mark(Jump jump) {
        label(jump);
    }

    /* constant value instructions */

    public final void anull() {
        push(OBJECT_TYPE);
    }

    public final void aconst() {
        push(STRING_TYPE);
    }

    public final void iconst() {
        push(Type.INT_TYPE);
    }

    public final void lconst() {
        push(Type.LONG_TYPE);
    }

    public final void fconst() {
        push(Type.FLOAT_TYPE);
    }

    public final void dconst() {
        push(Type.DOUBLE_TYPE);
    }

    public final void bipush() {
        push(Type.BYTE_TYPE);
    }

    public final void sipush() {
        push(Type.SHORT_TYPE);
    }

    public final void hconst() {
        push(Types.MethodHandle);
    }

    public final void tconst(Type type) {
        push(Types.Class);
    }

    public final void tconst(MethodTypeDescriptor type) {
        push(Types.MethodType);
    }

    public final void ldc(Object cst) {
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
        } else {
            throw new IllegalArgumentException();
        }
    }

    /* local load instructions */

    private void load(int var, Type type) {
        Type t = variables.getVariable(var);
        assert compatibleTypes(type, t);
        push(t);
    }

    public final void iload(int var) {
        load(var, Type.INT_TYPE);
    }

    public final void lload(int var) {
        load(var, Type.LONG_TYPE);
    }

    public final void fload(int var) {
        load(var, Type.FLOAT_TYPE);
    }

    public final void dload(int var) {
        load(var, Type.DOUBLE_TYPE);
    }

    public final void aload(int var) {
        load(var, OBJECT_TYPE);
    }

    /* array load instructions */

    private void aload(Type type) {
        pop(Type.INT_TYPE);
        pop(OBJECT_TYPE);
        push(type);
    }

    public final void iaload() {
        aload(Type.INT_TYPE);
    }

    public final void laload() {
        aload(Type.LONG_TYPE);
    }

    public final void faload() {
        aload(Type.FLOAT_TYPE);
    }

    public final void daload() {
        aload(Type.DOUBLE_TYPE);
    }

    public final void aaload() {
        aload(OBJECT_TYPE);
    }

    public final void baload() {
        aload(Type.BYTE_TYPE);
    }

    public final void caload() {
        aload(Type.CHAR_TYPE);
    }

    public final void saload() {
        aload(Type.SHORT_TYPE);
    }

    /* local store instructions */

    private void store(int var, Type type) {
        Type t = variables.getVariable(var);
        assert compatibleTypes(type, t);
        pop(type);
    }

    public final void istore(int var) {
        store(var, Type.INT_TYPE);
    }

    public final void lstore(int var) {
        store(var, Type.LONG_TYPE);
    }

    public final void fstore(int var) {
        store(var, Type.FLOAT_TYPE);
    }

    public final void dstore(int var) {
        store(var, Type.DOUBLE_TYPE);
    }

    public final void astore(int var) {
        store(var, OBJECT_TYPE);
    }

    /* array store instructions */

    private void astore(Type type) {
        pop(type);
        pop(Type.INT_TYPE);
        pop(OBJECT_TYPE);
    }

    public final void iastore() {
        astore(Type.INT_TYPE);
    }

    public final void lastore() {
        astore(Type.LONG_TYPE);
    }

    public final void fastore() {
        astore(Type.FLOAT_TYPE);
    }

    public final void dastore() {
        astore(Type.DOUBLE_TYPE);
    }

    public final void aastore() {
        astore(OBJECT_TYPE);
    }

    public final void bastore() {
        astore(Type.BYTE_TYPE);
    }

    public final void castore() {
        astore(Type.CHAR_TYPE);
    }

    public final void sastore() {
        astore(Type.SHORT_TYPE);
    }

    /* stack instructions */

    /**
     * {@code pop} bytecode instruction.
     */
    public final void pop() {
        pop(1);
    }

    /**
     * {@code pop2} bytecode instruction.
     */
    public final void pop2() {
        if (peek().getSize() == 2) {
            pop(2);
        } else {
            pop(1);
            pop(1);
        }
    }

    /**
     * {@code dup} bytecode instruction.
     */
    public final void dup() {
        Type t0 = pop(1);
        push(t0);
        push(t0);
    }

    /**
     * {@code dupX1} bytecode instruction.
     */
    public final void dupX1() {
        Type t0 = pop(1);
        Type t1 = pop(1);
        push(t0);
        push(t1);
        push(t0);
    }

    /**
     * {@code dupX2} bytecode instruction.
     */
    public final void dupX2() {
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
    }

    /**
     * {@code dup2} bytecode instruction.
     */
    public final void dup2() {
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
    }

    /**
     * {@code dup2X1} bytecode instruction.
     */
    public final void dup2X1() {
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
    }

    /**
     * {@code dup2X2} bytecode instruction.
     */
    public final void dup2X2() {
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
    }

    /**
     * {@code swap} bytecode instruction.
     */
    public final void swap() {
        Type t0 = pop(1);
        Type t1 = pop(1);
        push(t0);
        push(t1);
    }

    /* math instructions */

    private void arithmetic(Type type) {
        pop(type);
        pop(type);
        push(type);
    }

    private void neg(Type type) {
        pop(type);
        push(type);
    }

    private void shift(Type type) {
        pop(Type.INT_TYPE);
        pop(type);
        push(type);
    }

    public final void iadd() {
        arithmetic(Type.INT_TYPE);
    }

    public final void ladd() {
        arithmetic(Type.LONG_TYPE);
    }

    public final void fadd() {
        arithmetic(Type.FLOAT_TYPE);
    }

    public final void dadd() {
        arithmetic(Type.DOUBLE_TYPE);
    }

    public final void isub() {
        arithmetic(Type.INT_TYPE);
    }

    public final void lsub() {
        arithmetic(Type.LONG_TYPE);
    }

    public final void fsub() {
        arithmetic(Type.FLOAT_TYPE);
    }

    public final void dsub() {
        arithmetic(Type.DOUBLE_TYPE);
    }

    public final void imul() {
        arithmetic(Type.INT_TYPE);
    }

    public final void lmul() {
        arithmetic(Type.LONG_TYPE);
    }

    public final void fmul() {
        arithmetic(Type.FLOAT_TYPE);
    }

    public final void dmul() {
        arithmetic(Type.DOUBLE_TYPE);
    }

    public final void idiv() {
        arithmetic(Type.INT_TYPE);
    }

    public final void ldiv() {
        arithmetic(Type.LONG_TYPE);
    }

    public final void fdiv() {
        arithmetic(Type.FLOAT_TYPE);
    }

    public final void ddiv() {
        arithmetic(Type.DOUBLE_TYPE);
    }

    public final void irem() {
        arithmetic(Type.INT_TYPE);
    }

    public final void lrem() {
        arithmetic(Type.LONG_TYPE);
    }

    public final void frem() {
        arithmetic(Type.FLOAT_TYPE);
    }

    public final void drem() {
        arithmetic(Type.DOUBLE_TYPE);
    }

    public final void ineg() {
        neg(Type.INT_TYPE);
    }

    public final void lneg() {
        neg(Type.LONG_TYPE);
    }

    public final void fneg() {
        neg(Type.FLOAT_TYPE);
    }

    public final void dneg() {
        neg(Type.DOUBLE_TYPE);
    }

    public final void ishl() {
        shift(Type.INT_TYPE);
    }

    public final void lshl() {
        shift(Type.LONG_TYPE);
    }

    public final void ishr() {
        shift(Type.INT_TYPE);
    }

    public final void lshr() {
        shift(Type.LONG_TYPE);
    }

    public final void iushr() {
        shift(Type.INT_TYPE);
    }

    public final void lushr() {
        shift(Type.LONG_TYPE);
    }

    public final void iand() {
        arithmetic(Type.INT_TYPE);
    }

    public final void land() {
        arithmetic(Type.LONG_TYPE);
    }

    public final void ior() {
        arithmetic(Type.INT_TYPE);
    }

    public final void lor() {
        arithmetic(Type.LONG_TYPE);
    }

    public final void ixor() {
        arithmetic(Type.INT_TYPE);
    }

    public final void lxor() {
        arithmetic(Type.LONG_TYPE);
    }

    /* primitive type cast instructions */

    private void cast(Type from, Type to) {
        pop(from);
        push(to);
    }

    /**
     * {@code i2l} bytecode instruction.
     */
    public final void i2l() {
        cast(Type.INT_TYPE, Type.LONG_TYPE);
    }

    /**
     * {@code i2f} bytecode instruction.
     */
    public final void i2f() {
        cast(Type.INT_TYPE, Type.FLOAT_TYPE);
    }

    /**
     * {@code i2d} bytecode instruction.
     */
    public final void i2d() {
        cast(Type.INT_TYPE, Type.DOUBLE_TYPE);
    }

    /**
     * {@code l2i} bytecode instruction.
     */
    public final void l2i() {
        cast(Type.LONG_TYPE, Type.INT_TYPE);
    }

    /**
     * {@code l2f} bytecode instruction.
     */
    public final void l2f() {
        cast(Type.LONG_TYPE, Type.FLOAT_TYPE);
    }

    /**
     * {@code l2d} bytecode instruction.
     */
    public final void l2d() {
        cast(Type.LONG_TYPE, Type.DOUBLE_TYPE);
    }

    /**
     * {@code f2i} bytecode instruction.
     */
    public final void f2i() {
        cast(Type.FLOAT_TYPE, Type.INT_TYPE);
    }

    /**
     * {@code f2l} bytecode instruction.
     */
    public final void f2l() {
        cast(Type.FLOAT_TYPE, Type.LONG_TYPE);
    }

    /**
     * {@code f2d} bytecode instruction.
     */
    public final void f2d() {
        cast(Type.FLOAT_TYPE, Type.DOUBLE_TYPE);
    }

    /**
     * {@code d2i} bytecode instruction.
     */
    public final void d2i() {
        cast(Type.DOUBLE_TYPE, Type.INT_TYPE);
    }

    /**
     * {@code d2l} bytecode instruction.
     */
    public final void d2l() {
        cast(Type.DOUBLE_TYPE, Type.LONG_TYPE);
    }

    /**
     * {@code d2f} bytecode instruction.
     */
    public final void d2f() {
        cast(Type.DOUBLE_TYPE, Type.FLOAT_TYPE);
    }

    /**
     * {@code i2b} bytecode instruction.
     */
    public final void i2b() {
        cast(Type.INT_TYPE, Type.BYTE_TYPE);
    }

    /**
     * {@code i2c} bytecode instruction.
     */
    public final void i2c() {
        cast(Type.INT_TYPE, Type.CHAR_TYPE);
    }

    /**
     * {@code i2s} bytecode instruction.
     */
    public final void i2s() {
        cast(Type.INT_TYPE, Type.SHORT_TYPE);
    }

    /* compare instructions */

    /**
     * {@code lcmp} bytecode instruction.
     */
    public final void lcmp() {
        pop(Type.LONG_TYPE);
        pop(Type.LONG_TYPE);
        push(Type.INT_TYPE);
    }

    /**
     * {@code fcmpl} bytecode instruction.
     */
    public final void fcmpl() {
        pop(Type.FLOAT_TYPE);
        pop(Type.FLOAT_TYPE);
        push(Type.INT_TYPE);
    }

    /**
     * {@code fcmpg} bytecode instruction.
     */
    public final void fcmpg() {
        pop(Type.FLOAT_TYPE);
        pop(Type.FLOAT_TYPE);
        push(Type.INT_TYPE);
    }

    /**
     * {@code dcmpl} bytecode instruction.
     */
    public final void dcmpl() {
        pop(Type.DOUBLE_TYPE);
        pop(Type.DOUBLE_TYPE);
        push(Type.INT_TYPE);
    }

    /**
     * {@code dcmpg} bytecode instruction.
     */
    public final void dcmpg() {
        pop(Type.DOUBLE_TYPE);
        pop(Type.DOUBLE_TYPE);
        push(Type.INT_TYPE);
    }

    /* jump instructions */

    /**
     * {@code ifeq} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifeq(Jump jump) {
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ifne} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifne(Jump jump) {
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code iflt} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void iflt(Jump jump) {
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ifge} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifge(Jump jump) {
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ifgt} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifgt(Jump jump) {
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ifle} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifle(Jump jump) {
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ificmpeq} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ificmpeq(Jump jump) {
        pop(Type.INT_TYPE);
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ificmpne} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ificmpne(Jump jump) {
        pop(Type.INT_TYPE);
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ificmplt} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ificmplt(Jump jump) {
        pop(Type.INT_TYPE);
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ificmpge} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ificmpge(Jump jump) {
        pop(Type.INT_TYPE);
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ificmpgt} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ificmpgt(Jump jump) {
        pop(Type.INT_TYPE);
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ificmple} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ificmple(Jump jump) {
        pop(Type.INT_TYPE);
        pop(Type.INT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ifacmpeq} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifacmpeq(Jump jump) {
        pop(OBJECT_TYPE);
        pop(OBJECT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ifacmpne} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifacmpne(Jump jump) {
        pop(OBJECT_TYPE);
        pop(OBJECT_TYPE);
        jmp(jump);
    }

    /**
     * {@code goto} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void goTo(Jump jump) {
        jmp(jump);
        discardStack();
    }

    /**
     * {@code ifnull} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifnull(Jump jump) {
        pop(OBJECT_TYPE);
        jmp(jump);
    }

    /**
     * {@code ifnonnull} bytecode instruction.
     * 
     * @param jump
     *            jump target
     */
    public final void ifnonnull(Jump jump) {
        pop(OBJECT_TYPE);
        jmp(jump);
    }

    /* switch instructions */

    private void jmp(Jump... jumps) {
        for (Jump jump : jumps) {
            jmp(jump);
        }
    }

    /**
     * {@code tableswitch} bytecode instruction.
     * 
     * @param min
     *            minimum entry key
     * @param max
     *            maximum entry key
     * @param dflt
     *            default case label
     * @param jumps
     *            case labels
     */
    public final void tableswitch(int min, int max, Jump dflt, Jump[] jumps) {
        pop(Type.INT_TYPE);
        jmp(dflt);
        jmp(jumps);
        discardStack();
    }

    /**
     * {@code lookupswitch} bytecode instruction.
     * 
     * @param dflt
     *            default case label
     * @param keys
     *            case entry keys
     * @param jumps
     *            case labels
     */
    public final void lookupswitch(Jump dflt, int[] keys, Jump[] jumps) {
        pop(Type.INT_TYPE);
        jmp(dflt);
        jmp(jumps);
        discardStack();
    }

    /* return instructions */

    private void areturn(Type type) {
        if (type.getSort() != Type.Sort.VOID) {
            pop(type);
        }
        assert sp == 0 : String.format("sp=%d, stack=%s", sp, getStackString());
        discardStack();
    }

    /**
     * {@code ireturn} bytecode instruction.
     */
    public final void ireturn() {
        areturn(Type.INT_TYPE);
    }

    /**
     * {@code lreturn} bytecode instruction.
     */
    public final void lreturn() {
        areturn(Type.LONG_TYPE);
    }

    /**
     * {@code freturn} bytecode instruction.
     */
    public final void freturn() {
        areturn(Type.FLOAT_TYPE);
    }

    /**
     * {@code dreturn} bytecode instruction.
     */
    public final void dreturn() {
        areturn(Type.DOUBLE_TYPE);
    }

    /**
     * {@code areturn} bytecode instruction.
     */
    public final void areturn() {
        areturn(OBJECT_TYPE);
    }

    /**
     * {@code return} bytecode instruction.
     */
    public final void voidreturn() {
        areturn(Type.VOID_TYPE);
    }

    /* field instructions */

    /**
     * {@code getstatic} bytecode instruction.
     * 
     * @param desc
     *            the field descriptor
     */
    public final void getstatic(Type desc) {
        push(desc);
    }

    /**
     * {@code putstatic} bytecode instruction.
     * 
     * @param desc
     *            the field descriptor
     */
    public final void putstatic(Type desc) {
        pop(desc);
    }

    /**
     * {@code getfield} bytecode instruction.
     * 
     * @param owner
     *            field owner
     * @param desc
     *            the field descriptor
     */
    public final void getfield(Type owner, Type desc) {
        pop(owner);
        push(desc);
    }

    /**
     * {@code putfield} bytecode instruction.
     * 
     * @param owner
     *            field owner
     * @param desc
     *            the field descriptor
     */
    public final void putfield(Type owner, Type desc) {
        pop(desc);
        pop(owner);
    }

    /* invoke instructions */

    private void invoke(MethodTypeDescriptor desc, boolean consumeThis) {
        for (int i = desc.parameterCount(); i > 0;) {
            pop(desc.parameterType(--i));
        }
        if (consumeThis) {
            pop(OBJECT_TYPE);
        }
        Type ret = desc.returnType();
        if (ret.getSort() != Type.Sort.VOID) {
            push(ret);
        }
    }

    /**
     * {@code invokevirtual} bytecode instruction.
     * 
     * @param desc
     *            the method descriptor
     */
    public final void invokevirtual(MethodTypeDescriptor desc) {
        invoke(desc, true);
    }

    /**
     * {@code invokespecial} bytecode instruction.
     * 
     * @param desc
     *            the method descriptor
     */
    public final void invokespecial(MethodTypeDescriptor desc) {
        invoke(desc, true);
    }

    /**
     * {@code invokestatic} bytecode instruction.
     * 
     * @param desc
     *            the method descriptor
     */
    public final void invokestatic(MethodTypeDescriptor desc) {
        invoke(desc, false);
    }

    /**
     * {@code invokeinterface} bytecode instruction.
     * 
     * @param desc
     *            the method descriptor
     */
    public final void invokeinterface(MethodTypeDescriptor desc) {
        invoke(desc, true);
    }

    /**
     * {@code invokedynamic} bytecode instruction.
     * 
     * @param desc
     *            the method descriptor
     */
    public final void invokedynamic(MethodTypeDescriptor desc) {
        invoke(desc, false);
    }

    /* other instructions */

    /**
     * {@code new} bytecode instruction.
     * 
     * @param type
     *            the object type
     */
    public final void anew(Type type) {
        push(type);
    }

    /**
     * {@code newarray} or {@code anewarray} bytecode instruction.
     * 
     * @param type
     *            the array component type
     */
    public final void newarray(Type type) {
        pop(Type.INT_TYPE);
        push(type.asArray());
    }

    /**
     * {@code arraylength} bytecode instruction.
     */
    public final void arraylength() {
        pop(OBJECT_TYPE);
        push(Type.INT_TYPE);
    }

    /**
     * {@code athrow} bytecode instruction.
     */
    public final void athrow() {
        pop(OBJECT_TYPE);
        discardStack();
    }

    /**
     * {@code checkcast} bytecode instruction.
     * 
     * @param type
     *            the object type
     */
    public final void checkcast(Type type) {
        pop(OBJECT_TYPE);
        push(type);
    }

    /**
     * {@code instanceOf} bytecode instruction.
     * 
     * @param type
     *            the object type
     */
    public final void instanceOf(Type type) {
        pop(OBJECT_TYPE);
        push(Type.INT_TYPE);
    }

    /**
     * {@code monitorenter} bytecode instruction.
     */
    public final void monitorenter() {
        pop(OBJECT_TYPE);
    }

    /**
     * {@code monitorexit} bytecode instruction.
     */
    public final void monitorexit() {
        pop(OBJECT_TYPE);
    }

    /**
     * {@code multianewarray} bytecode instruction.
     * 
     * @param type
     *            the array type
     * @param dims
     *            the array dimensions
     */
    public final void multianewarray(Type type, int dims) {
        for (int i = dims; i > 0; --i) {
            pop(Type.INT_TYPE);
        }
        push(type);
    }
}
