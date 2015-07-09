/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.io.PrintWriter;
import java.util.HashMap;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.util.TraceMethodVisitor;

import com.github.anba.es6draft.compiler.assembler.Code.MethodCode;

/**
 * 
 */
public class InstructionAssembler {
    private static final boolean VERIFY_STACK = true;
    private static final boolean EVALUATE_SIZE = false;
    private static final boolean TRACE = false;
    private static final int MAX_STRING_SIZE = 16384;

    private static final class Methods {
        // class: StringBuilder
        static final MethodName StringBuilder_append_String = MethodName.findVirtual(
                Types.StringBuilder, "append", Type.methodType(Types.StringBuilder, Types.String));

        static final MethodName StringBuilder_init_int = MethodName.findConstructor(
                Types.StringBuilder, Type.methodType(Type.VOID_TYPE, Type.INT_TYPE));

        static final MethodName StringBuilder_toString = MethodName.findVirtual(
                Types.StringBuilder, "toString", Type.methodType(Types.String));

        static final MethodName Boolean_valueOf = MethodName.findStatic(Types.Boolean, "valueOf",
                Type.methodType(Types.Boolean, Type.BOOLEAN_TYPE));

        static final MethodName Boolean_booleanValue = MethodName.findVirtual(Types.Boolean,
                "booleanValue", Type.methodType(Type.BOOLEAN_TYPE));

        static final MethodName Character_valueOf = MethodName.findStatic(Types.Character,
                "valueOf", Type.methodType(Types.Character, Type.CHAR_TYPE));

        static final MethodName Character_charValue = MethodName.findVirtual(Types.Character,
                "charValue", Type.methodType(Type.CHAR_TYPE));

        static final MethodName Byte_valueOf = MethodName.findStatic(Types.Byte, "valueOf",
                Type.methodType(Types.Byte, Type.BYTE_TYPE));

        static final MethodName Byte_byteValue = MethodName.findVirtual(Types.Byte, "byteValue",
                Type.methodType(Type.BYTE_TYPE));

        static final MethodName Short_valueOf = MethodName.findStatic(Types.Short, "valueOf",
                Type.methodType(Types.Short, Type.SHORT_TYPE));

        static final MethodName Short_shortValue = MethodName.findVirtual(Types.Short,
                "shortValue", Type.methodType(Type.SHORT_TYPE));

        static final MethodName Integer_valueOf = MethodName.findStatic(Types.Integer, "valueOf",
                Type.methodType(Types.Integer, Type.INT_TYPE));

        static final MethodName Integer_intValue = MethodName.findVirtual(Types.Integer,
                "intValue", Type.methodType(Type.INT_TYPE));

        static final MethodName Long_valueOf = MethodName.findStatic(Types.Long, "valueOf",
                Type.methodType(Types.Long, Type.LONG_TYPE));

        static final MethodName Long_longValue = MethodName.findVirtual(Types.Long, "longValue",
                Type.methodType(Type.LONG_TYPE));

        static final MethodName Float_valueOf = MethodName.findStatic(Types.Float, "valueOf",
                Type.methodType(Types.Float, Type.FLOAT_TYPE));

        static final MethodName Float_floatValue = MethodName.findVirtual(Types.Float,
                "floatValue", Type.methodType(Type.FLOAT_TYPE));

        static final MethodName Double_valueOf = MethodName.findStatic(Types.Double, "valueOf",
                Type.methodType(Types.Double, Type.DOUBLE_TYPE));

        static final MethodName Double_doubleValue = MethodName.findVirtual(Types.Double,
                "doubleValue", Type.methodType(Type.DOUBLE_TYPE));
    }

    private static final class $CodeSizeEvaluator extends CodeSizeEvaluator {
        private final MethodCode method;

        $CodeSizeEvaluator(MethodCode method, MethodVisitor mv) {
            super(Opcodes.ASM5, mv);
            this.method = method;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            System.out.printf("%s: [%d, %d]\n", method.methodName, getMinSize(), getMaxSize());
        }
    }

    private static final class $TraceMethodVisitor extends MethodVisitor {
        private final MethodCode method;

        $TraceMethodVisitor(MethodCode method, MethodVisitor mv) {
            super(Opcodes.ASM5, new TraceMethodVisitor(mv, new SimpleTypeTextifier()));
            this.method = method;
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            trace();
        }

        public void trace() {
            PrintWriter printWriter = new PrintWriter(System.out);
            printWriter.format("%nClass=%s, Method=%s [%s]%n", method.classCode.className,
                    method.methodName, method.methodDescriptor);
            ((TraceMethodVisitor) mv).p.print(printWriter);
            printWriter.format("%n");
            printWriter.flush();
        }
    }

    private final MethodCode method;
    private final MethodVisitor methodVisitor;
    private final ConstantPool constantPool;
    private final Stack stack;
    private final Variables variables = new Variables();
    private final HashMap<Class<?>, Type> typeCache = new HashMap<>();
    private int lastLineNumber = -1;

    private Type getType(Class<?> c) {
        Type type = typeCache.get(c);
        if (type == null) {
            typeCache.put(c, type = Type.of(c));
        }
        return type;
    }

    /**
     * Creates a new instruction assembler
     * 
     * @param method
     *            the method object
     */
    public InstructionAssembler(MethodCode method) {
        this.method = method;
        this.methodVisitor = decorate(method.methodVisitor);
        this.constantPool = method.classCode.constantPool;
        this.stack = createStack(variables);
    }

    /**
     * Returns the method object.
     * 
     * @return the method object
     */
    public final MethodCode getMethod() {
        return method;
    }

    /**
     * Returns the underlying {@link MethodVisitor}.
     * 
     * @return the method visitor
     */
    public final MethodVisitor getMethodVisitor() {
        return methodVisitor;
    }

    /**
     * Returns the last emitted line number.
     * 
     * @return the last line number
     */
    protected final int getLastLineNumber() {
        return lastLineNumber;
    }

    protected Stack createStack(Variables variables) {
        if (VERIFY_STACK) {
            return new Stack(variables);
        }
        return new EmptyStack(variables);
    }

    protected MethodVisitor decorate(MethodVisitor mv) {
        if (EVALUATE_SIZE) {
            mv = new $CodeSizeEvaluator(method, mv);
        }
        if (TRACE) {
            mv = new $TraceMethodVisitor(method, mv);
        }
        return mv;
    }

    /**
     * Print current bytecode.
     */
    public void trace() {
        if (methodVisitor instanceof $TraceMethodVisitor) {
            (($TraceMethodVisitor) methodVisitor).trace();
        }
    }

    /* */

    /**
     * Starts assembling the bytecode instructions.
     * <p>
     * Sub-classes need to call {@code super.begin()} at the beginning of this method.
     */
    public void begin() {
        methodVisitor.visitCode();
        enterVariableScope();
        initializeParameters();
    }

    /**
     * Stops assembling the bytecode instructions.
     * <p>
     * Sub-classes need to call {@code super.end()} at the end of this method.
     */
    public void end() {
        exitVariableScope();
        variables.close();
        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }

    /* parameters and locals */

    protected final void restoreVariables(VariablesSnapshot snapshot) {
        this.variables.restore(snapshot);
    }

    public final VariablesSnapshot getVariablesSnapshot() {
        return variables.snapshot();
    }

    private boolean isInstanceMethod() {
        return (method.access & Opcodes.ACC_STATIC) == 0;
    }

    private void initializeParameters() {
        int slot = 0;
        if (isInstanceMethod()) {
            variables.reserveSlot("<this>", Types.Object, slot);
            slot += Types.Object.getSize();
        }
        for (Type argument : method.methodDescriptor.parameterList()) {
            variables.reserveSlot(argument, slot);
            slot += argument.getSize();
        }
    }

    private int parameterSlot(int index) {
        MethodTypeDescriptor desc = method.methodDescriptor;
        int slot = isInstanceMethod() ? 1 : 0;
        for (int i = 0; i < index; ++i) {
            slot += desc.parameterType(i).getSize();
        }
        return slot;
    }

    private void checkParameterType(int index, Type type) {
        if (!method.methodDescriptor.parameterType(index).equals(type)) {
            throw new IllegalArgumentException();
        }
    }

    protected void setParameterName(String name, int index, Type type) {
        checkParameterType(index, type);
        MethodTypeDescriptor desc = method.methodDescriptor;
        variables.addVariable(name, desc.parameterType(index), parameterSlot(index));
    }

    public boolean hasParameter(int index, Class<?> clazz) {
        MethodTypeDescriptor desc = method.methodDescriptor;
        return index < desc.parameterCount() && desc.parameterType(index).equals(getType(clazz));
    }

    public <T> Variable<T> getParameter(int index, Class<T> clazz) {
        checkParameterType(index, getType(clazz));
        MethodTypeDescriptor desc = method.methodDescriptor;
        return new Variable<>("(parameter)", desc.parameterType(index), parameterSlot(index));
    }

    public <T> void loadParameter(int index, Class<T> clazz) {
        load(getParameter(index, clazz));
    }

    public void loadParameter(int index, Type type) {
        checkParameterType(index, type);
        load(index, type);
    }

    public void enterVariableScope() {
        VariableScope scope = variables.enter();
        methodVisitor.visitLabel(scope.start);
    }

    public void exitVariableScope() {
        VariableScope scope = variables.exit();
        methodVisitor.visitLabel(scope.end);
        for (Variable<?> variable : scope) {
            localVariable(variable, scope.start, scope.end);
        }
    }

    private void localVariable(Variable<?> variable, Label start, Label end) {
        if (variable.hasSlot()) {
            methodVisitor.visitLocalVariable(variable.getName(), variable.getType().descriptor(),
                    null, start, end, variable.getSlot());
        }
    }

    /**
     * Creates a new named variable.
     * 
     * @param <T>
     *            the variable class type
     * @param name
     *            the variable name
     * @param clazz
     *            the variable class
     * @return the new variable object
     */
    public <T> Variable<T> newVariable(String name, Class<T> clazz) {
        assert name != null;
        return variables.newVariable(name, getType(clazz));
    }

    /**
     * Creates a new unnamed variable.
     * 
     * @param <T>
     *            the variable class type
     * @param clazz
     *            the variable class
     * @return the new variable object
     */
    public <T> Variable<T> newScratchVariable(Class<T> clazz) {
        return variables.newVariable(null, getType(clazz));
    }

    public void freeVariable(Variable<?> variable) {
        variables.freeVariable(variable);
    }

    /* annotations */

    public void annotation(Type type, boolean visible) {
        methodVisitor.visitAnnotation(type.descriptor(), visible);
    }

    /* stack operations */

    public final boolean hasStack() {
        return !(stack instanceof EmptyStack);
    }

    public final int getStackSize() {
        return stack.getStackPointer();
    }

    public final Type[] getStack() {
        return stack.getStack();
    }

    protected final void restoreStack(Type[] stack) {
        this.stack.setStack(stack);
    }

    /**
     * Emits a <code>goto</code> instruction without discarding the current stack state.
     * 
     * @param target
     *            the target instruction to jump to
     */
    public final void nonDestructiveGoTo(Jump target) {
        Type[] st = stack.getStackDirect();
        int sp = stack.getStackPointer();
        goTo(target);
        stack.setStack(st, sp);
    }

    /* */

    public void mark(TryCatchLabel label) {
        methodVisitor.visitLabel(label.label());
        // Stack object does not need to be updated.
    }

    public void mark(Jump jump) {
        methodVisitor.visitLabel(jump.label());
        stack.mark(jump);
    }

    public void lineInfo(int line) {
        if (lastLineNumber == line) {
            // omit duplicate line info entries
            return;
        }
        lastLineNumber = line;
        Label label = new Label();
        methodVisitor.visitLabel(label);
        methodVisitor.visitLineNumber(line, label);
    }

    /* try-catch-finally instructions */

    /**
     * Defines a try-catch block for the error {@code type}.
     * 
     * @param start
     *            the start label of the try-catch block
     * @param end
     *            the end label of the try-catch block
     * @param handler
     *            the catch handler label
     * @param type
     *            the exception type
     */
    public void tryCatch(TryCatchLabel start, TryCatchLabel end, TryCatchLabel handler, Type type) {
        methodVisitor.visitTryCatchBlock(start.label(), end.label(), handler.label(),
                type.internalName());
    }

    /**
     * Defines a try-finally block.
     * 
     * @param start
     *            the start label of the try-finally block
     * @param end
     *            the end label of the try-finally block
     * @param handler
     *            the finally handler label
     */
    public void tryFinally(TryCatchLabel start, TryCatchLabel end, TryCatchLabel handler) {
        methodVisitor.visitTryCatchBlock(start.label(), end.label(), handler.label(), null);
    }

    /**
     * Marks the start of a catch-handler.
     * 
     * @param handler
     *            the catch handler label
     * @param exception
     *            the exception type
     * @see #tryCatch(TryCatchLabel, TryCatchLabel, TryCatchLabel, Type)
     */
    public void catchHandler(TryCatchLabel handler, Type exception) {
        methodVisitor.visitLabel(handler.label());
        stack.catchHandler(exception);
    }

    /**
     * Marks the start of a finally-handler.
     * 
     * @param handler
     *            the finally handler label
     * @see #tryFinally(TryCatchLabel, TryCatchLabel, TryCatchLabel)
     */
    public void finallyHandler(TryCatchLabel handler) {
        methodVisitor.visitLabel(handler.label());
        stack.catchHandler(Types.Throwable);
    }

    /* constant value instructions */

    public void anull() {
        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
        stack.anull();
    }

    public void aconst(String cst) {
        if (cst == null) {
            anull();
        } else if (cst.length() <= MAX_STRING_SIZE) {
            constantPool.aconst(this, cst);
        } else {
            // string literal too large, split to avoid bytecode error
            anew(Types.StringBuilder);
            dup();
            iconst(cst.length());
            invoke(Methods.StringBuilder_init_int);

            for (int i = 0, len = cst.length(); i < len; i += MAX_STRING_SIZE) {
                String chunk = cst.substring(i, Math.min(i + MAX_STRING_SIZE, len));
                constantPool.aconst(this, chunk);
                invoke(Methods.StringBuilder_append_String);
            }

            invoke(Methods.StringBuilder_toString);
        }
    }

    /**
     * &#x2205; → value
     * 
     * @param b
     *            the constant boolean to load on the stack
     */
    public void iconst(boolean b) {
        methodVisitor.visitInsn(b ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        stack.iconst();
    }

    public void iconst(int value) {
        if (-1 <= value && value <= 5) {
            methodVisitor.visitInsn(Opcodes.ICONST_0 + value);
            stack.iconst();
        } else if (Byte.MIN_VALUE <= value && value <= Byte.MAX_VALUE) {
            bipush(value);
        } else if (Short.MIN_VALUE <= value && value <= Short.MAX_VALUE) {
            sipush(value);
        } else {
            constantPool.iconst(this, Integer.valueOf(value));
        }
    }

    public void lconst(long value) {
        if (value == 0) {
            methodVisitor.visitInsn(Opcodes.LCONST_0);
            stack.lconst();
        } else if (value == 1) {
            methodVisitor.visitInsn(Opcodes.LCONST_1);
            stack.lconst();
        } else {
            constantPool.lconst(this, Long.valueOf(value));
        }
    }

    public void fconst(float value) {
        if (value == 0 && Float.floatToRawIntBits(value) == 0) {
            methodVisitor.visitInsn(Opcodes.FCONST_0);
            stack.fconst();
        } else if (value == 1) {
            methodVisitor.visitInsn(Opcodes.FCONST_1);
            stack.fconst();
        } else if (value == 2) {
            methodVisitor.visitInsn(Opcodes.FCONST_2);
            stack.fconst();
        } else {
            constantPool.fconst(this, Float.valueOf(value));
        }
    }

    public void dconst(double value) {
        if (value == 0 && Double.doubleToRawLongBits(value) == 0) {
            methodVisitor.visitInsn(Opcodes.DCONST_0);
            stack.dconst();
        } else if (value == 1) {
            methodVisitor.visitInsn(Opcodes.DCONST_1);
            stack.dconst();
        } else {
            constantPool.dconst(this, Double.valueOf(value));
        }
    }

    public void ldc(Object cst) {
        methodVisitor.visitLdcInsn(cst);
        stack.ldc(cst);
    }

    public void bipush(int value) {
        methodVisitor.visitIntInsn(Opcodes.BIPUSH, value);
        stack.bipush();
    }

    public void sipush(int value) {
        methodVisitor.visitIntInsn(Opcodes.SIPUSH, value);
        stack.sipush();
    }

    /**
     * &#x2205; → handle
     * 
     * @param method
     *            the method name descriptor
     */
    public void handle(MethodName method) {
        hconst(method.toHandle());
    }

    public void hconst(Handle handle) {
        methodVisitor.visitLdcInsn(handle.handle());
        stack.hconst();
    }

    public void tconst(Type type) {
        methodVisitor.visitLdcInsn(type.type());
        stack.tconst(type);
    }

    public void tconst(MethodTypeDescriptor type) {
        methodVisitor.visitLdcInsn(type.type());
        stack.tconst(type);
    }

    /* local load instructions */

    /**
     * &#x2205; → this
     */
    public final void loadThis() {
        assert isInstanceMethod();
        load(0, Types.Object);
    }

    public final void load(Value<?> value) {
        value.load(this);
    }

    public final void load(Variable<?> variable) {
        assert variable.isAlive() : "variable out of scope";
        load(variable.getSlot(), variable.getType());
    }

    private void load(int var, Type type) {
        assert var >= 0 && variables.isActive(var) : "variable is not initialized";
        switch (type.getOpcode(Opcodes.ILOAD)) {
        case Opcodes.ILOAD:
            iload(var);
            return;
        case Opcodes.LLOAD:
            lload(var);
            return;
        case Opcodes.FLOAD:
            fload(var);
            return;
        case Opcodes.DLOAD:
            dload(var);
            return;
        case Opcodes.ALOAD:
            aload(var);
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    private void iload(int var) {
        methodVisitor.visitVarInsn(Opcodes.ILOAD, var);
        stack.iload(var);
    }

    private void lload(int var) {
        methodVisitor.visitVarInsn(Opcodes.LLOAD, var);
        stack.lload(var);
    }

    private void fload(int var) {
        methodVisitor.visitVarInsn(Opcodes.FLOAD, var);
        stack.fload(var);
    }

    private void dload(int var) {
        methodVisitor.visitVarInsn(Opcodes.DLOAD, var);
        stack.dload(var);
    }

    private void aload(int var) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, var);
        stack.aload(var);
    }

    /* array load instructions */

    /**
     * array → value.
     * 
     * @param index
     *            the array index
     * @param type
     *            the array element type
     */
    public final void aload(int index, Type type) {
        assert index >= 0;
        iconst(index);
        aload(type);
    }

    public final void aload(Type type) {
        switch (type.getOpcode(Opcodes.IALOAD)) {
        case Opcodes.IALOAD:
            iaload();
            return;
        case Opcodes.LALOAD:
            laload();
            return;
        case Opcodes.FALOAD:
            faload();
            return;
        case Opcodes.DALOAD:
            daload();
            return;
        case Opcodes.AALOAD:
            aaload();
            return;
        case Opcodes.BALOAD:
            baload();
            return;
        case Opcodes.CALOAD:
            caload();
            return;
        case Opcodes.SALOAD:
            saload();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void iaload() {
        methodVisitor.visitInsn(Opcodes.IALOAD);
        stack.iaload();
    }

    public void laload() {
        methodVisitor.visitInsn(Opcodes.LALOAD);
        stack.laload();
    }

    public void faload() {
        methodVisitor.visitInsn(Opcodes.FALOAD);
        stack.faload();
    }

    public void daload() {
        methodVisitor.visitInsn(Opcodes.DALOAD);
        stack.daload();
    }

    public void aaload() {
        methodVisitor.visitInsn(Opcodes.AALOAD);
        stack.aaload();
    }

    public void baload() {
        methodVisitor.visitInsn(Opcodes.BALOAD);
        stack.baload();
    }

    public void caload() {
        methodVisitor.visitInsn(Opcodes.CALOAD);
        stack.caload();
    }

    public void saload() {
        methodVisitor.visitInsn(Opcodes.SALOAD);
        stack.saload();
    }

    /* local store instructions */

    public final void store(Variable<?> variable) {
        assert variable.isAlive() : "variable out of scope";
        store(variable.getSlot(), variable.getType());
    }

    private void store(int var, Type type) {
        assert var >= 0;
        variables.activate(var);
        switch (type.getOpcode(Opcodes.ISTORE)) {
        case Opcodes.ISTORE:
            istore(var);
            return;
        case Opcodes.LSTORE:
            lstore(var);
            return;
        case Opcodes.FSTORE:
            fstore(var);
            return;
        case Opcodes.DSTORE:
            dstore(var);
            return;
        case Opcodes.ASTORE:
            astore(var);
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    private void istore(int var) {
        methodVisitor.visitVarInsn(Opcodes.ISTORE, var);
        stack.istore(var);
    }

    private void lstore(int var) {
        methodVisitor.visitVarInsn(Opcodes.LSTORE, var);
        stack.lstore(var);
    }

    private void fstore(int var) {
        methodVisitor.visitVarInsn(Opcodes.FSTORE, var);
        stack.fstore(var);
    }

    private void dstore(int var) {
        methodVisitor.visitVarInsn(Opcodes.DSTORE, var);
        stack.dstore(var);
    }

    private void astore(int var) {
        methodVisitor.visitVarInsn(Opcodes.ASTORE, var);
        stack.astore(var);
    }

    /* array store instructions */

    /**
     * array → array
     * 
     * @param index
     *            the array index
     * @param element
     *            the string element to store
     */
    public final void astore(int index, String element) {
        dup();
        iconst(index);
        aconst(element);
        astore(Types.String);
    }

    /**
     * array → array
     * 
     * @param index
     *            the array index
     * @param element
     *            the int element to store
     */
    public final void astore(int index, int element) {
        dup();
        iconst(index);
        iconst(element);
        astore(Type.INT_TYPE);
    }

    public final void astore(Type type) {
        switch (type.getOpcode(Opcodes.IASTORE)) {
        case Opcodes.IASTORE:
            iastore();
            return;
        case Opcodes.LASTORE:
            lastore();
            return;
        case Opcodes.FASTORE:
            fastore();
            return;
        case Opcodes.DASTORE:
            dastore();
            return;
        case Opcodes.AASTORE:
            aastore();
            return;
        case Opcodes.BASTORE:
            bastore();
            return;
        case Opcodes.CASTORE:
            castore();
            return;
        case Opcodes.SASTORE:
            sastore();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void iastore() {
        methodVisitor.visitInsn(Opcodes.IASTORE);
        stack.iastore();
    }

    public void lastore() {
        methodVisitor.visitInsn(Opcodes.LASTORE);
        stack.lastore();
    }

    public void fastore() {
        methodVisitor.visitInsn(Opcodes.FASTORE);
        stack.fastore();
    }

    public void dastore() {
        methodVisitor.visitInsn(Opcodes.DASTORE);
        stack.dastore();
    }

    public void aastore() {
        methodVisitor.visitInsn(Opcodes.AASTORE);
        stack.aastore();
    }

    public void bastore() {
        methodVisitor.visitInsn(Opcodes.BASTORE);
        stack.bastore();
    }

    public void castore() {
        methodVisitor.visitInsn(Opcodes.CASTORE);
        stack.castore();
    }

    public void sastore() {
        methodVisitor.visitInsn(Opcodes.SASTORE);
        stack.sastore();
    }

    /* stack instructions */

    /**
     * value → []
     * 
     * @param type
     *            the topmost stack value
     */
    public void pop(Type type) {
        switch (type.getSize()) {
        case 0:
            return;
        case 1:
            pop();
            return;
        case 2:
            pop2();
            return;
        default:
            throw new AssertionError();
        }
    }

    public void pop() {
        methodVisitor.visitInsn(Opcodes.POP);
        stack.pop();
    }

    public void pop2() {
        methodVisitor.visitInsn(Opcodes.POP2);
        stack.pop2();
    }

    /**
     * value → value, value
     * 
     * @param type
     *            the topmost stack value
     */
    public void dup(Type type) {
        switch (type.getSize()) {
        case 1:
            dup();
            return;
        case 2:
            dup2();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void dup() {
        methodVisitor.visitInsn(Opcodes.DUP);
        stack.dup();
    }

    /**
     * lvalue, rvalue → rvalue, lvalue, rvalue
     * 
     * @param ltype
     *            the second topmost stack value
     * @param rtype
     *            the topmost stack value
     */
    public void dupX(Type ltype, Type rtype) {
        int lsize = ltype.getSize(), rsize = rtype.getSize();
        if (lsize == 1 && rsize == 1) {
            dupX1();
        } else if (lsize == 1 && rsize == 2) {
            dup2X1();
        } else if (lsize == 2 && rsize == 1) {
            dupX2();
        } else if (lsize == 2 && rsize == 2) {
            dup2X2();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void dupX1() {
        methodVisitor.visitInsn(Opcodes.DUP_X1);
        stack.dupX1();
    }

    public void dupX2() {
        methodVisitor.visitInsn(Opcodes.DUP_X2);
        stack.dupX2();
    }

    public void dup2() {
        methodVisitor.visitInsn(Opcodes.DUP2);
        stack.dup2();
    }

    public void dup2X1() {
        methodVisitor.visitInsn(Opcodes.DUP2_X1);
        stack.dup2X1();
    }

    public void dup2X2() {
        methodVisitor.visitInsn(Opcodes.DUP2_X2);
        stack.dup2X2();
    }

    /**
     * lvalue, rvalue → rvalue, lvalue
     * 
     * @param ltype
     *            the second topmost stack value
     * @param rtype
     *            the topmost stack value
     */
    public void swap(Type ltype, Type rtype) {
        int lsize = ltype.getSize(), rsize = rtype.getSize();
        if (lsize == 1 && rsize == 1) {
            swap();
        } else if (lsize == 1 && rsize == 2) {
            swap1_2();
        } else if (lsize == 2 && rsize == 1) {
            swap2_1();
        } else if (lsize == 2 && rsize == 2) {
            swap2();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void swap() {
        methodVisitor.visitInsn(Opcodes.SWAP);
        stack.swap();
    }

    /**
     * {value3}, {value2, value1} → {value2, value1}, {value3}
     */
    public final void swap1_2() {
        dup2X1();
        pop2();
    }

    /**
     * {value3, value2}, {value1} → {value1}, {value3, value2}
     */
    public final void swap2_1() {
        dupX2();
        pop();
    }

    /**
     * {value4, value3}, {value2, value1} → {value2, value1}, {value4, value3}
     */
    public final void swap2() {
        dup2X2();
        pop2();
    }

    /* math instructions */

    public final void add(Type type) {
        switch (type.getOpcode(Opcodes.IADD)) {
        case Opcodes.IADD:
            iadd();
            return;
        case Opcodes.LADD:
            ladd();
            return;
        case Opcodes.FADD:
            fadd();
            return;
        case Opcodes.DADD:
            dadd();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void iadd() {
        methodVisitor.visitInsn(Opcodes.IADD);
        stack.iadd();
    }

    public void ladd() {
        methodVisitor.visitInsn(Opcodes.LADD);
        stack.ladd();
    }

    public void fadd() {
        methodVisitor.visitInsn(Opcodes.FADD);
        stack.fadd();
    }

    public void dadd() {
        methodVisitor.visitInsn(Opcodes.DADD);
        stack.dadd();
    }

    public final void sub(Type type) {
        switch (type.getOpcode(Opcodes.ISUB)) {
        case Opcodes.ISUB:
            isub();
            return;
        case Opcodes.LSUB:
            lsub();
            return;
        case Opcodes.FSUB:
            fsub();
            return;
        case Opcodes.DSUB:
            dsub();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void isub() {
        methodVisitor.visitInsn(Opcodes.ISUB);
        stack.isub();
    }

    public void lsub() {
        methodVisitor.visitInsn(Opcodes.LSUB);
        stack.lsub();
    }

    public void fsub() {
        methodVisitor.visitInsn(Opcodes.FSUB);
        stack.fsub();
    }

    public void dsub() {
        methodVisitor.visitInsn(Opcodes.DSUB);
        stack.dsub();
    }

    public final void mul(Type type) {
        switch (type.getOpcode(Opcodes.IMUL)) {
        case Opcodes.IMUL:
            imul();
            return;
        case Opcodes.LMUL:
            lmul();
            return;
        case Opcodes.FMUL:
            fmul();
            return;
        case Opcodes.DMUL:
            dmul();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void imul() {
        methodVisitor.visitInsn(Opcodes.IMUL);
        stack.imul();
    }

    public void lmul() {
        methodVisitor.visitInsn(Opcodes.LMUL);
        stack.lmul();
    }

    public void fmul() {
        methodVisitor.visitInsn(Opcodes.FMUL);
        stack.fmul();
    }

    public void dmul() {
        methodVisitor.visitInsn(Opcodes.DMUL);
        stack.dmul();
    }

    public final void div(Type type) {
        switch (type.getOpcode(Opcodes.IDIV)) {
        case Opcodes.IDIV:
            idiv();
            return;
        case Opcodes.LDIV:
            ldiv();
            return;
        case Opcodes.FDIV:
            fdiv();
            return;
        case Opcodes.DDIV:
            ddiv();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void idiv() {
        methodVisitor.visitInsn(Opcodes.IDIV);
        stack.idiv();
    }

    public void ldiv() {
        methodVisitor.visitInsn(Opcodes.LDIV);
        stack.ldiv();
    }

    public void fdiv() {
        methodVisitor.visitInsn(Opcodes.FDIV);
        stack.fdiv();
    }

    public void ddiv() {
        methodVisitor.visitInsn(Opcodes.DDIV);
        stack.ddiv();
    }

    public final void rem(Type type) {
        switch (type.getOpcode(Opcodes.IREM)) {
        case Opcodes.IREM:
            irem();
            return;
        case Opcodes.LREM:
            lrem();
            return;
        case Opcodes.FREM:
            frem();
            return;
        case Opcodes.DREM:
            drem();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void irem() {
        methodVisitor.visitInsn(Opcodes.IREM);
        stack.irem();
    }

    public void lrem() {
        methodVisitor.visitInsn(Opcodes.LREM);
        stack.lrem();
    }

    public void frem() {
        methodVisitor.visitInsn(Opcodes.FREM);
        stack.frem();
    }

    public void drem() {
        methodVisitor.visitInsn(Opcodes.DREM);
        stack.drem();
    }

    public final void neg(Type type) {
        switch (type.getOpcode(Opcodes.INEG)) {
        case Opcodes.INEG:
            ineg();
            return;
        case Opcodes.LNEG:
            lneg();
            return;
        case Opcodes.FNEG:
            fneg();
            return;
        case Opcodes.DNEG:
            dneg();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void ineg() {
        methodVisitor.visitInsn(Opcodes.INEG);
        stack.ineg();
    }

    public void lneg() {
        methodVisitor.visitInsn(Opcodes.LNEG);
        stack.lneg();
    }

    public void fneg() {
        methodVisitor.visitInsn(Opcodes.FNEG);
        stack.fneg();
    }

    public void dneg() {
        methodVisitor.visitInsn(Opcodes.DNEG);
        stack.dneg();
    }

    public final void shl(Type type) {
        switch (type.getOpcode(Opcodes.ISHL)) {
        case Opcodes.ISHL:
            ishl();
            return;
        case Opcodes.LSHL:
            lshl();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void ishl() {
        methodVisitor.visitInsn(Opcodes.ISHL);
        stack.ishl();
    }

    public void lshl() {
        methodVisitor.visitInsn(Opcodes.LSHL);
        stack.lshl();
    }

    public final void shr(Type type) {
        switch (type.getOpcode(Opcodes.ISHR)) {
        case Opcodes.ISHR:
            ishr();
            return;
        case Opcodes.LSHR:
            lshr();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void ishr() {
        methodVisitor.visitInsn(Opcodes.ISHR);
        stack.ishr();
    }

    public void lshr() {
        methodVisitor.visitInsn(Opcodes.LSHR);
        stack.lshr();
    }

    public final void ushr(Type type) {
        switch (type.getOpcode(Opcodes.IUSHR)) {
        case Opcodes.IUSHR:
            iushr();
            return;
        case Opcodes.LUSHR:
            lushr();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void iushr() {
        methodVisitor.visitInsn(Opcodes.IUSHR);
        stack.iushr();
    }

    public void lushr() {
        methodVisitor.visitInsn(Opcodes.LUSHR);
        stack.lushr();
    }

    public final void and(Type type) {
        switch (type.getOpcode(Opcodes.IAND)) {
        case Opcodes.IAND:
            iand();
            return;
        case Opcodes.LAND:
            land();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void iand() {
        methodVisitor.visitInsn(Opcodes.IAND);
        stack.iand();
    }

    public void land() {
        methodVisitor.visitInsn(Opcodes.LAND);
        stack.land();
    }

    public final void or(Type type) {
        switch (type.getOpcode(Opcodes.IOR)) {
        case Opcodes.IOR:
            ior();
            return;
        case Opcodes.LOR:
            lor();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void ior() {
        methodVisitor.visitInsn(Opcodes.IOR);
        stack.ior();
    }

    public void lor() {
        methodVisitor.visitInsn(Opcodes.LOR);
        stack.lor();
    }

    public final void xor(Type type) {
        switch (type.getOpcode(Opcodes.IXOR)) {
        case Opcodes.IXOR:
            ixor();
            return;
        case Opcodes.LXOR:
            lxor();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void ixor() {
        methodVisitor.visitInsn(Opcodes.IXOR);
        stack.ixor();
    }

    public void lxor() {
        methodVisitor.visitInsn(Opcodes.LXOR);
        stack.lxor();
    }

    /* local increment instructions */

    public void iinc(Variable<Integer> variable, int increment) {
        assert variable.isAlive() : "variable out of scope";
        iinc(variable.getSlot(), increment);
    }

    private void iinc(int var, int increment) {
        assert var >= 0 && Short.MIN_VALUE <= increment && increment <= Short.MAX_VALUE;
        methodVisitor.visitIincInsn(var, increment);
        // no stack change
    }

    /* primitive type cast instructions */

    public void cast(Type from, Type to) {
        switch (from.getSort()) {
        case Type.Sort.BYTE:
        case Type.Sort.CHAR:
        case Type.Sort.SHORT:
        case Type.Sort.INT:
            switch (to.getSort()) {
            case Type.Sort.INT:
                // nop
                return;
            case Type.Sort.LONG:
                i2l();
                return;
            case Type.Sort.FLOAT:
                i2f();
                return;
            case Type.Sort.DOUBLE:
                i2d();
                return;
            case Type.Sort.BYTE:
                i2b();
                return;
            case Type.Sort.CHAR:
                i2c();
                return;
            case Type.Sort.SHORT:
                i2s();
                return;
            default:
                throw new IllegalArgumentException();
            }
        case Type.Sort.LONG:
            switch (to.getSort()) {
            case Type.Sort.INT:
                l2i();
                return;
            case Type.Sort.LONG:
                // nop
                return;
            case Type.Sort.FLOAT:
                l2f();
                return;
            case Type.Sort.DOUBLE:
                l2d();
                return;
            case Type.Sort.BYTE:
                l2i();
                i2b();
                return;
            case Type.Sort.CHAR:
                l2i();
                i2c();
                return;
            case Type.Sort.SHORT:
                l2i();
                i2s();
                return;
            default:
                throw new IllegalArgumentException();
            }
        case Type.Sort.FLOAT:
            switch (to.getSort()) {
            case Type.Sort.INT:
                f2i();
                return;
            case Type.Sort.LONG:
                f2l();
                return;
            case Type.Sort.FLOAT:
                // nop
                return;
            case Type.Sort.DOUBLE:
                f2d();
                return;
            case Type.Sort.BYTE:
                f2i();
                i2b();
                return;
            case Type.Sort.CHAR:
                f2i();
                i2c();
                return;
            case Type.Sort.SHORT:
                f2i();
                i2s();
                return;
            default:
                throw new IllegalArgumentException();
            }
        case Type.Sort.DOUBLE:
            switch (to.getSort()) {
            case Type.Sort.INT:
                d2i();
                return;
            case Type.Sort.LONG:
                d2l();
                return;
            case Type.Sort.FLOAT:
                d2f();
                return;
            case Type.Sort.DOUBLE:
                // nop
                return;
            case Type.Sort.BYTE:
                d2i();
                i2b();
                return;
            case Type.Sort.CHAR:
                d2i();
                i2c();
                return;
            case Type.Sort.SHORT:
                d2i();
                i2s();
                return;
            default:
                throw new IllegalArgumentException();
            }
        default:
            throw new IllegalArgumentException();
        }
    }

    public void i2l() {
        methodVisitor.visitInsn(Opcodes.I2L);
        stack.i2l();
    }

    public void i2f() {
        methodVisitor.visitInsn(Opcodes.I2F);
        stack.i2f();
    }

    public void i2d() {
        methodVisitor.visitInsn(Opcodes.I2D);
        stack.i2d();
    }

    public void l2i() {
        methodVisitor.visitInsn(Opcodes.L2I);
        stack.l2i();
    }

    public void l2f() {
        methodVisitor.visitInsn(Opcodes.L2F);
        stack.l2f();
    }

    public void l2d() {
        methodVisitor.visitInsn(Opcodes.L2D);
        stack.l2d();
    }

    public void f2i() {
        methodVisitor.visitInsn(Opcodes.F2I);
        stack.f2i();
    }

    public void f2l() {
        methodVisitor.visitInsn(Opcodes.F2L);
        stack.f2l();
    }

    public void f2d() {
        methodVisitor.visitInsn(Opcodes.F2D);
        stack.f2d();
    }

    public void d2i() {
        methodVisitor.visitInsn(Opcodes.D2I);
        stack.d2i();
    }

    public void d2l() {
        methodVisitor.visitInsn(Opcodes.D2L);
        stack.d2l();
    }

    public void d2f() {
        methodVisitor.visitInsn(Opcodes.D2F);
        stack.d2f();
    }

    public void i2b() {
        methodVisitor.visitInsn(Opcodes.I2B);
        stack.i2b();
    }

    public void i2c() {
        methodVisitor.visitInsn(Opcodes.I2C);
        stack.i2c();
    }

    public void i2s() {
        methodVisitor.visitInsn(Opcodes.I2S);
        stack.i2s();
    }

    /**
     * value → not(value)
     */
    public final void not() {
        iconst(1);
        ixor();
    }

    /**
     * value → bitnot(value)
     */
    public final void bitnot() {
        iconst(-1);
        ixor();
    }

    /* compare instructions */

    public void lcmp() {
        methodVisitor.visitInsn(Opcodes.LCMP);
        stack.lcmp();
    }

    public void fcmpl() {
        methodVisitor.visitInsn(Opcodes.FCMPL);
        stack.fcmpl();
    }

    public void fcmpg() {
        methodVisitor.visitInsn(Opcodes.FCMPG);
        stack.fcmpg();
    }

    public void dcmpl() {
        methodVisitor.visitInsn(Opcodes.DCMPL);
        stack.dcmpl();
    }

    public void dcmpg() {
        methodVisitor.visitInsn(Opcodes.DCMPG);
        stack.dcmpg();
    }

    /* jump instructions */

    public void ifeq(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFEQ, jump.target());
        stack.ifeq(jump);
    }

    public void ifne(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFNE, jump.target());
        stack.ifne(jump);
    }

    public void iflt(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFLT, jump.target());
        stack.iflt(jump);
    }

    public void ifge(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFGE, jump.target());
        stack.ifge(jump);
    }

    public void ifgt(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFGT, jump.target());
        stack.ifgt(jump);
    }

    public void ifle(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFLE, jump.target());
        stack.ifle(jump);
    }

    public void ificmpeq(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPEQ, jump.target());
        stack.ificmpeq(jump);
    }

    public void ificmpne(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPNE, jump.target());
        stack.ificmpne(jump);
    }

    public void ificmplt(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLT, jump.target());
        stack.ificmplt(jump);
    }

    public void ificmpge(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGE, jump.target());
        stack.ificmpge(jump);
    }

    public void ificmpgt(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPGT, jump.target());
        stack.ificmpgt(jump);
    }

    public void ificmple(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ICMPLE, jump.target());
        stack.ificmple(jump);
    }

    public void ifacmpeq(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPEQ, jump.target());
        stack.ifacmpeq(jump);
    }

    public void ifacmpne(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, jump.target());
        stack.ifacmpne(jump);
    }

    public void goTo(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.GOTO, jump.target());
        stack.goTo(jump);
    }

    public void ifnull(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFNULL, jump.target());
        stack.ifnull(jump);
    }

    public void ifnonnull(Jump jump) {
        methodVisitor.visitJumpInsn(Opcodes.IFNONNULL, jump.target());
        stack.ifnonnull(jump);
    }

    /* sub-routine instructions (not supported) */

    public void jsr(Jump jump) {
        throw new UnsupportedOperationException();
    }

    public void ret(int var) {
        throw new UnsupportedOperationException();
    }

    /* switch instructions */

    private static Label[] toLabels(Jump... jumps) {
        Label[] labels = new Label[jumps.length];
        for (int i = 0, length = jumps.length; i < length; ++i) {
            labels[i] = jumps[i].target();
        }
        return labels;
    }

    public void tableswitch(int min, int max, Jump dflt, Jump[] jumps) {
        methodVisitor.visitTableSwitchInsn(min, max, dflt.target(), toLabels(jumps));
        stack.tableswitch(min, max, dflt, jumps);
    }

    public void lookupswitch(Jump dflt, int[] keys, Jump[] jumps) {
        methodVisitor.visitLookupSwitchInsn(dflt.target(), keys, toLabels(jumps));
        stack.lookupswitch(dflt, keys, jumps);
    }

    /* return instructions */

    /**
     * value → &#x2205;
     */
    public void _return() {
        Type returnType = method.methodDescriptor.returnType();
        switch (returnType.getOpcode(Opcodes.IRETURN)) {
        case Opcodes.IRETURN:
            ireturn();
            return;
        case Opcodes.LRETURN:
            lreturn();
            return;
        case Opcodes.FRETURN:
            freturn();
            return;
        case Opcodes.DRETURN:
            dreturn();
            return;
        case Opcodes.ARETURN:
            areturn();
            return;
        case Opcodes.RETURN:
            voidreturn();
            return;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void ireturn() {
        methodVisitor.visitInsn(Opcodes.IRETURN);
        stack.ireturn();
    }

    public void lreturn() {
        methodVisitor.visitInsn(Opcodes.LRETURN);
        stack.lreturn();
    }

    public void freturn() {
        methodVisitor.visitInsn(Opcodes.FRETURN);
        stack.freturn();
    }

    public void dreturn() {
        methodVisitor.visitInsn(Opcodes.DRETURN);
        stack.dreturn();
    }

    public void areturn() {
        methodVisitor.visitInsn(Opcodes.ARETURN);
        stack.areturn();
    }

    public void voidreturn() {
        methodVisitor.visitInsn(Opcodes.RETURN);
        stack.voidreturn();
    }

    /* field instructions */

    /**
     * &#x2205; → value
     * 
     * @param field
     *            the field descriptor
     */
    public void get(FieldName field) {
        switch (field.allocation) {
        case Instance:
            getfield(field.owner, field.name, field.descriptor);
            break;
        case Static:
            getstatic(field.owner, field.name, field.descriptor);
            break;
        default:
            throw new AssertionError();
        }
    }

    /**
     * value → &#x2205;
     * 
     * @param field
     *            the field descriptor
     */
    public void put(FieldName field) {
        switch (field.allocation) {
        case Instance:
            putfield(field.owner, field.name, field.descriptor);
            break;
        case Static:
            putstatic(field.owner, field.name, field.descriptor);
            break;
        default:
            throw new AssertionError();
        }
    }

    public void getstatic(Type owner, String name, Type desc) {
        methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, owner.internalName(), name,
                desc.descriptor());
        stack.getstatic(desc);
    }

    public void putstatic(Type owner, String name, Type desc) {
        methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, owner.internalName(), name,
                desc.descriptor());
        stack.putstatic(desc);
    }

    public void getfield(Type owner, String name, Type desc) {
        methodVisitor.visitFieldInsn(Opcodes.GETFIELD, owner.internalName(), name,
                desc.descriptor());
        stack.getfield(owner, desc);
    }

    public void putfield(Type owner, String name, Type desc) {
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, owner.internalName(), name,
                desc.descriptor());
        stack.putfield(owner, desc);
    }

    /* invoke instructions */

    /**
     * parameters → value
     * 
     * @param method
     *            the method for the invocation
     */
    public void invoke(MethodName method) {
        switch (method.invoke) {
        case Interface:
            invokeinterface(method.owner, method.name, method.descriptor);
            break;
        case Special:
            invokespecial(method.owner, method.name, method.descriptor, false);
            break;
        case SpecialInterface:
            invokespecial(method.owner, method.name, method.descriptor, true);
            break;
        case Static:
            invokestatic(method.owner, method.name, method.descriptor, false);
            break;
        case StaticInterface:
            invokestatic(method.owner, method.name, method.descriptor, true);
            break;
        case Virtual:
            invokevirtual(method.owner, method.name, method.descriptor, false);
            break;
        case VirtualInterface:
            invokevirtual(method.owner, method.name, method.descriptor, true);
            break;
        default:
            throw new AssertionError();
        }
    }

    public void invokevirtual(Type owner, String name, MethodTypeDescriptor desc, boolean itf) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner.internalName(), name,
                desc.descriptor(), itf);
        stack.invokevirtual(desc);
    }

    public void invokespecial(Type owner, String name, MethodTypeDescriptor desc, boolean itf) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, owner.internalName(), name,
                desc.descriptor(), itf);
        stack.invokespecial(desc);
    }

    public void invokestatic(Type owner, String name, MethodTypeDescriptor desc, boolean itf) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner.internalName(), name,
                desc.descriptor(), itf);
        stack.invokestatic(desc);
    }

    public void invokeinterface(Type owner, String name, MethodTypeDescriptor desc) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, owner.internalName(), name,
                desc.descriptor(), true);
        stack.invokeinterface(desc);
    }

    /* invokedynamic instructions */

    private static final Object[] EMPTY_BSM_ARGS = new Object[] {};

    public final void invokedynamic(String name, MethodTypeDescriptor desc, Handle bsm) {
        invokedynamic(name, desc, bsm, EMPTY_BSM_ARGS);
    }

    public void invokedynamic(String name, MethodTypeDescriptor desc, Handle bsm, Object... bsmArgs) {
        methodVisitor.visitInvokeDynamicInsn(name, desc.descriptor(), bsm.handle(), bsmArgs);
        stack.invokedynamic(desc);
    }

    /* other instructions */

    /**
     * &#x2205; → object
     * 
     * @param type
     *            the type descriptor
     * @param init
     *            the init-method call descriptor
     */
    public final void anew(Type type, MethodName init) {
        anew(type);
        dup();
        invoke(init);
    }

    public void anew(Type type) {
        methodVisitor.visitTypeInsn(Opcodes.NEW, type.internalName());
        stack.anew(type);
    }

    private static final int arrayType(Type type) {
        switch (type.getSort()) {
        case Type.Sort.BOOLEAN:
            return Opcodes.T_BOOLEAN;
        case Type.Sort.CHAR:
            return Opcodes.T_CHAR;
        case Type.Sort.BYTE:
            return Opcodes.T_BYTE;
        case Type.Sort.SHORT:
            return Opcodes.T_SHORT;
        case Type.Sort.INT:
            return Opcodes.T_INT;
        case Type.Sort.FLOAT:
            return Opcodes.T_FLOAT;
        case Type.Sort.LONG:
            return Opcodes.T_LONG;
        case Type.Sort.DOUBLE:
            return Opcodes.T_DOUBLE;
        case Type.Sort.OBJECT:
        case Type.Sort.ARRAY:
        case Type.Sort.METHOD:
        case Type.Sort.VOID:
        default:
            throw new IllegalArgumentException();
        }
    }

    /**
     * &#x2205; → array
     * 
     * @param length
     *            the array length
     * @param type
     *            the array component type
     */
    public final void newarray(int length, Type type) {
        assert length >= 0;
        iconst(length);
        newarray(type);
    }

    public void newarray(Type type) {
        methodVisitor.visitIntInsn(Opcodes.NEWARRAY, arrayType(type));
        stack.newarray(type);
    }

    /**
     * &#x2205; → array
     * 
     * @param length
     *            the array length
     * @param type
     *            the array component type
     */
    public final void anewarray(int length, Type type) {
        assert length >= 0;
        iconst(length);
        anewarray(type);
    }

    public void anewarray(Type type) {
        methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, type.internalName());
        stack.newarray(type);
    }

    public void arraylength() {
        methodVisitor.visitInsn(Opcodes.ARRAYLENGTH);
        stack.arraylength();
    }

    public void athrow() {
        methodVisitor.visitInsn(Opcodes.ATHROW);
        stack.athrow();
    }

    public void checkcast(Type type) {
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, type.internalName());
        stack.checkcast(type);
    }

    public void instanceOf(Type type) {
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, type.internalName());
        stack.instanceOf(type);
    }

    public void monitorenter() {
        methodVisitor.visitInsn(Opcodes.MONITORENTER);
        stack.monitorenter();
    }

    public void monitorexit() {
        methodVisitor.visitInsn(Opcodes.MONITOREXIT);
        stack.monitorexit();
    }

    public void multianewarray(Type type, int dims) {
        // TODO: descriptor() call correct? or internalName()?
        methodVisitor.visitMultiANewArrayInsn(type.descriptor(), dims);
        stack.multianewarray(type, dims);
    }

    public void nop() {
        methodVisitor.visitInsn(Opcodes.NOP);
    }

    /* boxed wrapper helper */

    private static final MethodName[] boxMethods;
    private static final MethodName[] unboxMethods;

    static {
        MethodName[] bm = new MethodName[12];
        bm[Type.Sort.BOOLEAN] = Methods.Boolean_valueOf;
        bm[Type.Sort.CHAR] = Methods.Character_valueOf;
        bm[Type.Sort.BYTE] = Methods.Byte_valueOf;
        bm[Type.Sort.SHORT] = Methods.Short_valueOf;
        bm[Type.Sort.INT] = Methods.Integer_valueOf;
        bm[Type.Sort.FLOAT] = Methods.Float_valueOf;
        bm[Type.Sort.LONG] = Methods.Long_valueOf;
        bm[Type.Sort.DOUBLE] = Methods.Double_valueOf;
        boxMethods = bm;

        MethodName[] um = new MethodName[12];
        um[Type.Sort.BOOLEAN] = Methods.Boolean_booleanValue;
        um[Type.Sort.CHAR] = Methods.Character_charValue;
        um[Type.Sort.BYTE] = Methods.Byte_byteValue;
        um[Type.Sort.SHORT] = Methods.Short_shortValue;
        um[Type.Sort.INT] = Methods.Integer_intValue;
        um[Type.Sort.FLOAT] = Methods.Float_floatValue;
        um[Type.Sort.LONG] = Methods.Long_longValue;
        um[Type.Sort.DOUBLE] = Methods.Double_doubleValue;
        unboxMethods = um;
    }

    /**
     * value → boxed
     * 
     * @param type
     *            the type kind
     */
    public final void toBoxed(Type type) {
        MethodName call = boxMethods[type.getSort()];
        if (call != null) {
            invoke(call);
        }
    }

    /**
     * boxed → value
     * 
     * @param type
     *            the type kind
     */
    public final void toUnboxed(Type type) {
        MethodName call = unboxMethods[type.getSort()];
        if (call != null) {
            invoke(call);
        }
    }

    /**
     * Returns the wrapper for {@code type}, or {@code type} if it does not represent a primitive.
     * 
     * @param type
     *            the type kind
     * @return the type's wrapper
     */
    public static final Type wrapper(Type type) {
        return type.asWrapper();
    }
}
