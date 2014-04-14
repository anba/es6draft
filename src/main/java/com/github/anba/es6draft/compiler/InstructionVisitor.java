/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.github.anba.es6draft.compiler.Code.MethodCode;
import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;

/**
 *
 */
class InstructionVisitor extends InstructionAdapter {
    static interface VariablesView {
        /**
         * Returns the type information for the requested local variable.
         * 
         * @param slot
         *            the variable index slot
         * @return the variable type
         */
        Type getVariable(int slot);

        /**
         * Returns the next local variable slot after {@code slot}.
         * 
         * @param slot
         *            the variable index slot
         * @return the next active slot
         */
        int getNextActiveSlot(int slot);

        /**
         * Returns the number of variables.
         * 
         * @return the total variable count
         */
        int getVariableCount();
    }

    private static final class Variables implements VariablesView {
        private static final Type RESERVED = Type.getType("reserved");
        private static final int INITIAL_SIZE = 8;
        private final AtomicInteger id = new AtomicInteger();
        private final BitSet variables = new BitSet();
        private final BitSet active = new BitSet();
        private Type[] types = new Type[INITIAL_SIZE];
        private VariableScope varScope = null;

        private static Type[] grow(Type[] types) {
            int newLength = types.length + (types.length >>> 1);
            return Arrays.copyOf(types, newLength, Type[].class);
        }

        private void assign(int slot, Type type) {
            if (type.getSize() == 1) {
                types[slot] = type;
                variables.set(slot);
            } else {
                types[slot] = type;
                types[slot + 1] = RESERVED;
                variables.set(slot, slot + 2);
            }
        }

        @Override
        public Type getVariable(int slot) {
            assert 0 <= slot && slot <= types.length : String.format("slot=%d not in [%d, %d]",
                    slot, 0, types.length);
            assert variables.get(slot) && types[slot] != null && types[slot] != RESERVED : String
                    .format("slot=%d, used=%b, type=%s", slot, variables.get(slot), types[slot]);
            return types[slot];
        }

        @Override
        public int getNextActiveSlot(int slot) {
            assert 0 <= slot && slot <= types.length : String.format("slot=%d not in [%d, %d]",
                    slot, 0, types.length);
            assert variables.get(slot) && active.get(slot) && types[slot] != null
                    && types[slot] != RESERVED : String.format(
                    "slot=%d, used=%b, active=%b, type=%s", slot, variables.get(slot),
                    active.get(slot), types[slot]);
            return active.nextSetBit(slot + types[slot].getSize());
        }

        @Override
        public int getVariableCount() {
            int count = 0;
            for (int i = 0, len = types.length; i < len; ++i) {
                count += types[i] != null && types[i] != RESERVED ? 1 : 0;
            }
            return count;
        }

        VariablesView view() {
            Variables clone = new Variables();
            clone.variables.or(variables);
            clone.active.or(active);
            clone.types = Arrays.copyOf(types, types.length);
            return clone;
        }

        VariableScope enter() {
            return varScope = new VariableScope(varScope, variables.length());
        }

        VariableScope exit() {
            VariableScope scope = varScope;
            varScope = scope.parent;
            // clear stored variable type info
            for (Variable<?> v : scope) {
                v.alive = false;
            }
            Arrays.fill(types, scope.firstSlot, types.length, null);
            variables.clear(scope.firstSlot, variables.size());
            active.clear(scope.firstSlot, active.size());
            return scope;
        }

        void close() {
            assert varScope == null : "unclosed variable scopes";
        }

        void activate(int slot) {
            active.set(slot);
        }

        boolean isActive(int slot) {
            return active.get(slot);
        }

        /**
         * Adds a new entry to the variable map.
         * 
         * @param <T>
         *            the variable class type
         * @param name
         *            the variable name
         * @param type
         *            the variable type
         * @param slot
         *            the variable slot
         * @return the new variable object
         */
        <T> Variable<T> addVariable(String name, Type type, int slot) {
            assert variables.get(slot) && types[slot].equals(type);
            return varScope.add(name, type, slot);
        }

        /**
         * Sets the variable information for a fixed slot.
         * 
         * @param type
         *            the variable type
         * @param slot
         *            the variable slot
         */
        void reserveSlot(Type type, int slot) {
            assert slot >= 0;
            if (slot + type.getSize() > types.length) {
                types = grow(types);
            }
            assert types[slot] == null && (type.getSize() == 1 || types[slot + 1] == null);
            assign(slot, type);
            activate(slot);
        }

        /**
         * Sets the variable information for a fixed slot and adds an entry to the variable map.
         * 
         * @param <T>
         *            the variable class type
         * @param name
         *            the variable name
         * @param type
         *            the variable type
         * @param slot
         *            the variable slot
         * @return the variable object
         */
        <T> Variable<T> reserveSlot(String name, Type type, int slot) {
            reserveSlot(type, slot);
            return varScope.add(name, type, slot);
        }

        /**
         * Creates a new variable and adds it to the variable map.
         * 
         * @param <T>
         *            the variable class type
         * @param name
         *            the variable name
         * @param type
         *            the variable type
         * @return the new variable object
         */
        <T> Variable<T> newVariable(String name, Type type) {
            int slot = newVariable(type);
            if (slot < 0) {
                // reusing an existing slot
                return varScope.add("<shared>", type, slot);
            }
            if (name == null) {
                // null-name signals scratch variable, don't create variable map entry
                return varScope.add("<scratch>", type, -slot);
            }
            String uniqueName = name + "$" + id.incrementAndGet();
            return varScope.add(uniqueName, type, slot);
        }

        /**
         * Marks the given variable as no longer being used, only applicable for scratch variables.
         * 
         * @param variable
         *            the variable to be freed
         */
        void freeVariable(Variable<?> variable) {
            int slot = variable.getSlot();
            assert variable.alive && !(variable.alive = false);
            assert "<scratch>".equals(variable.getName()) || "<shared>".equals(variable.getName());
            assert variables.get(slot);
            assert types[slot].equals(variable.getType());
            variables.clear(slot);
            active.clear(slot);
        }

        private int newVariable(Type type) {
            for (int slot = varScope.firstSlot, size = type.getSize();;) {
                slot = variables.nextClearBit(slot);
                if (slot + size > types.length) {
                    types = grow(types);
                }
                Type old = types[slot];
                if (old == null) {
                    if (size == 1 || types[slot + 1] == null) {
                        assign(slot, type);
                        return slot;
                    }
                } else if (old.equals(type)) {
                    assert size == 1 || types[slot + 1] == RESERVED;
                    assign(slot, type);
                    return -slot;
                }
                // try next index
                slot += 1;
            }
        }
    }

    private static final class VariableScope implements Iterable<Variable<?>> {
        final VariableScope parent;
        final int firstSlot;
        final ArrayDeque<Variable<?>> variables = new ArrayDeque<>(6);
        final Label start = new Label(), end = new Label();

        VariableScope(VariableScope parent, int firstSlot) {
            this.parent = parent;
            this.firstSlot = firstSlot;
        }

        <T> Variable<T> add(String name, Type type, int slot) {
            Variable<T> variable = new Variable<>(name, type, slot);
            variables.add(variable);
            return variable;
        }

        @Override
        public Iterator<Variable<?>> iterator() {
            if (variables.isEmpty()) {
                return Collections.emptyIterator();
            }
            return variables.iterator();
        }
    }

    static final class Variable<T> {
        private final String name;
        private final Type type;
        private final int slot;
        private boolean alive = true;

        private Variable(String name, Type type, int slot) {
            this.name = name;
            this.type = type;
            this.slot = slot;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public int getSlot() {
            return slot < 0 ? -slot : slot;
        }

        public boolean isAlive() {
            return alive;
        }

        @SuppressWarnings("unchecked")
        public <U> Variable<U> uncheckedCast() {
            return (Variable<U>) this;
        }
    }

    enum MethodType {
        Interface, Virtual, Special, Static, VirtualInterface, SpecialInterface, StaticInterface;

        final int toTag() {
            switch (this) {
            case Interface:
                return Opcodes.H_INVOKEINTERFACE;
            case Special:
            case SpecialInterface:
                return Opcodes.H_INVOKESPECIAL;
            case Static:
            case StaticInterface:
                return Opcodes.H_INVOKESTATIC;
            case Virtual:
            case VirtualInterface:
                return Opcodes.H_INVOKEVIRTUAL;
            default:
                throw new IllegalStateException();
            }
        }
    }

    static final class MethodDesc {
        final MethodType type;
        final String owner;
        final String name;
        final String desc;

        private MethodDesc(MethodType type, String owner, String name, String desc) {
            this.type = type;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        static MethodDesc create(MethodType type, Type owner, String name, Type desc) {
            return new MethodDesc(type, owner.getInternalName(), name, desc.getDescriptor());
        }

        static MethodDesc create(MethodType type, String owner, String name, String desc) {
            return new MethodDesc(type, owner, name, desc);
        }
    }

    enum FieldType {
        Instance, Static
    }

    static final class FieldDesc {
        final FieldType type;
        final String owner;
        final String name;
        final String desc;

        private FieldDesc(FieldType type, String owner, String name, String desc) {
            this.type = type;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        static FieldDesc create(FieldType type, Type owner, String name, Type desc) {
            return new FieldDesc(type, owner.getInternalName(), name, desc.getDescriptor());
        }
    }

    private static final class Methods {
        // class: StringBuilder
        static final MethodDesc StringBuilder_append_String = MethodDesc.create(MethodType.Virtual,
                Types.StringBuilder, "append",
                Type.getMethodType(Types.StringBuilder, Types.String));

        static final MethodDesc StringBuilder_init_int = MethodDesc.create(MethodType.Special,
                Types.StringBuilder, "<init>", Type.getMethodType(Type.VOID_TYPE, Type.INT_TYPE));

        static final MethodDesc StringBuilder_toString = MethodDesc.create(MethodType.Virtual,
                Types.StringBuilder, "toString", Type.getMethodType(Types.String));
    }

    enum MethodAllocation {
        Class, Instance;

        static MethodAllocation from(int access) {
            return (access & Opcodes.ACC_STATIC) != 0 ? Class : Instance;
        }
    }

    private static final int MAX_STRING_SIZE = 16384;

    private final String methodName;
    private final Type methodDescriptor;
    private final MethodAllocation methodAllocation;
    private final MethodVisitor methodVisitor;
    private final StackMethodVisitor stack;
    private final Variables variables = new Variables();
    private final ClassValue<Type> typeCache = new ClassValue<Type>() {
        @Override
        protected Type computeValue(Class<?> c) {
            return Type.getType(c);
        }
    };
    private int lastLineNumber = -1;

    private Type getType(Class<?> c) {
        return typeCache.get(c);
    }

    protected InstructionVisitor(MethodCode method) {
        super(Opcodes.ASM5, method.methodVisitor);
        this.methodName = method.methodName;
        this.methodDescriptor = Type.getMethodType(method.methodDescriptor);
        this.methodAllocation = MethodAllocation.from(method.access);
        if (method.methodVisitor instanceof StackMethodVisitor) {
            this.stack = (StackMethodVisitor) method.methodVisitor;
            this.methodVisitor = stack.getMethodVisitor();
            stack.setVariables(variables);
        } else {
            this.stack = null;
            this.methodVisitor = method.methodVisitor;
        }
    }

    public final MethodVisitor getMethodVisitor() {
        return methodVisitor;
    }

    public final String getMethodName() {
        return methodName;
    }

    public final Type getMethodDescriptor() {
        return methodDescriptor;
    }

    public final MethodAllocation getMethodAllocation() {
        return methodAllocation;
    }

    public final boolean hasStack() {
        return stack != null;
    }

    public final Type[] getStack() {
        return stack.getStack();
    }

    protected final void restoreStack(Type[] stack) {
        this.stack.setStack(stack);
    }

    protected final void restoreVariables(VariablesView variables) {
        assert variables instanceof Variables;
        Variables locals = (Variables) variables;
        this.variables.variables.clear();
        this.variables.variables.or(locals.variables);
        this.variables.active.clear();
        this.variables.active.or(locals.active);
        this.variables.types = Arrays.copyOf(locals.types, locals.types.length);
    }

    public final VariablesView getVariables() {
        return variables.view();
    }

    private void initializeParameters() {
        int slot = 0;
        if (methodAllocation == MethodAllocation.Instance) {
            variables.reserveSlot("<this>", Types.Object, slot);
            slot += Types.Object.getSize();
        }
        for (Type argument : methodDescriptor.getArgumentTypes()) {
            variables.reserveSlot(argument, slot);
            slot += argument.getSize();
        }
    }

    private int parameterSlot(int index, Type[] argumentTypes) {
        int slot = methodAllocation == MethodAllocation.Instance ? 1 : 0;
        for (int i = 0; i < index; ++i) {
            slot += argumentTypes[i].getSize();
        }
        return slot;
    }

    protected void setParameterName(String name, int index, Type type) {
        Type[] argTypes = methodDescriptor.getArgumentTypes();
        if (!argTypes[index].equals(type)) {
            throw new IllegalArgumentException();
        }
        variables.addVariable(name, argTypes[index], parameterSlot(index, argTypes));
    }

    public boolean hasParameter(int index, Class<?> clazz) {
        Type[] argTypes = methodDescriptor.getArgumentTypes();
        return index < argTypes.length && argTypes[index].equals(getType(clazz));
    }

    public <T> Variable<T> getParameter(int index, Class<T> clazz) {
        Type[] argTypes = methodDescriptor.getArgumentTypes();
        if (!argTypes[index].equals(getType(clazz))) {
            throw new IllegalArgumentException();
        }
        return new Variable<>("(parameter)", argTypes[index], parameterSlot(index, argTypes));
    }

    public <T> void loadParameter(int index, Class<T> clazz) {
        load(getParameter(index, clazz));
    }

    public void enterVariableScope() {
        VariableScope scope = variables.enter();
        getMethodVisitor().visitLabel(scope.start);
    }

    public void exitVariableScope() {
        VariableScope scope = variables.exit();
        getMethodVisitor().visitLabel(scope.end);
        for (Variable<?> variable : scope) {
            visitLocalVariable(variable, scope.start, scope.end);
        }
    }

    private void visitLocalVariable(Variable<?> variable, Label start, Label end) {
        if (variable.slot >= 0) {
            visitLocalVariable(variable.getName(), variable.getType().getDescriptor(), null, start,
                    end, variable.getSlot());
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

    @Override
    public final void load(int var, Type type) {
        assert variables.isActive(var) : "variable is not initialized";
        super.load(var, type);
    }

    public void load(Variable<?> variable) {
        assert variable.isAlive() : "variable out of scope";
        load(variable.getSlot(), variable.getType());
    }

    @Override
    public final void store(int var, Type type) {
        variables.activate(var);
        super.store(var, type);
    }

    public void store(Variable<?> variable) {
        assert variable.isAlive() : "variable out of scope";
        store(variable.getSlot(), variable.getType());
    }

    public void begin() {
        visitCode();
        enterVariableScope();
        initializeParameters();
    }

    public void end() {
        exitVariableScope();
        variables.close();
        visitMaxs(0, 0);
        visitEnd();
    }

    public void lineInfo(int line) {
        if (lastLineNumber == line) {
            // omit duplicate line info entries
            return;
        }
        lastLineNumber = line;
        Label label = new Label();
        getMethodVisitor().visitLabel(label);
        visitLineNumber(line, label);
    }

    protected final int getLastLineNumber() {
        return lastLineNumber;
    }

    public void mark(LocationLabel label) {
        getMethodVisitor().visitLabel(label);
    }

    /**
     * Emit a <code>goto</code> instruction and update the stack state.
     * 
     * @param target
     *            the target instruction to jump to
     */
    public void goToAndSetStack(Label target) {
        goToAndSetStack(target, target);
    }

    /**
     * Emit a <code>goto</code> instruction and update the stack state.
     * 
     * @param target
     *            the target instruction to jump to
     * @param stackLabel
     *            the label which holds the stack information to use
     */
    public void goToAndSetStack(Label target, Label stackLabel) {
        goTo(target);
        if (stack != null) {
            stack.setStack(stackLabel);
        }
    }

    /**
     * value → not(value)
     */
    public void not() {
        iconst(1);
        xor(Type.INT_TYPE);
    }

    /**
     * value → bitnot(value)
     */
    public void bitnot() {
        iconst(-1);
        xor(Type.INT_TYPE);
    }

    /**
     * &#x2205; → value
     * 
     * @param b
     *            the constant boolean to load on the stack
     */
    public void iconst(boolean b) {
        iconst(b ? 1 : 0);
    }

    /**
     * &#x2205; → value
     * 
     * @param cst
     *            the constant string to load on the stack
     */
    public void aconst(String cst) {
        if (cst == null || cst.length() <= MAX_STRING_SIZE) {
            super.aconst(cst);
        } else {
            // string literal too large, split to avoid bytecode error
            anew(Types.StringBuilder);
            dup();
            iconst(cst.length());
            invoke(Methods.StringBuilder_init_int);

            for (int i = 0, len = cst.length(); i < len; i += MAX_STRING_SIZE) {
                String chunk = cst.substring(i, Math.min(i + MAX_STRING_SIZE, len));
                super.aconst(chunk);
                invoke(Methods.StringBuilder_append_String);
            }

            invoke(Methods.StringBuilder_toString);
        }
    }

    /**
     * value → &#x2205;
     */
    public void areturn() {
        areturn(methodDescriptor.getReturnType());
    }

    /**
     * &#x2205; → this
     */
    public void loadThis() {
        assert methodAllocation == MethodAllocation.Instance;
        load(0, Types.Object);
    }

    /**
     * &#x2205; → array
     * 
     * @param length
     *            the array length
     * @param type
     *            the array type
     */
    public void newarray(int length, Type type) {
        iconst(length);
        newarray(type);
    }

    /**
     * array → array
     * 
     * @param index
     *            the array index
     * @param element
     *            the element to store
     * @param type
     *            the array type
     */
    public void astore(int index, Object element, Type type) {
        dup();
        iconst(index);
        aconst(element);
        astore(type);
    }

    /**
     * array → array
     * 
     * @param index
     *            the array index
     * @param element
     *            the string element to store
     */
    public void astore(int index, String element) {
        dup();
        iconst(index);
        aconst(element);
        astore(Types.String);
    }

    /**
     * array → value.
     * 
     * @param index
     *            the array index
     * @param type
     *            the array type
     */
    public void aload(int index, Type type) {
        iconst(index);
        aload(type);
    }

    /**
     * {value3}, {value2, value1} → {value2, value1}, {value3}
     */
    public void swap1_2() {
        dup2X1();
        pop2();
    }

    /**
     * {value3, value2}, {value1} → {value1}, {value3, value2}
     */
    public void swap2_1() {
        dupX2();
        pop();
    }

    /**
     * {value4, value3}, {value2, value1} → {value2, value1}, {value4, value3}
     */
    public void swap2() {
        dup2X2();
        pop2();
    }

    /**
     * value → value, value
     * 
     * @param type
     *            the topmost stack value
     */
    public void dup(ValType type) {
        if (type.size() == 1) {
            dup();
        } else {
            assert type.size() == 2;
            dup2();
        }
    }

    /**
     * value → []
     * 
     * @param type
     *            the topmost stack value
     */
    public void pop(ValType type) {
        if (type.size() == 1) {
            pop();
        } else {
            assert type.size() == 2;
            pop2();
        }
    }

    /**
     * lvalue, rvalue → rvalue, lvalue, rvalue
     * 
     * @param ltype
     *            the second topmost stack value
     * @param rtype
     *            the topmost stack value
     */
    public void dupX(ValType ltype, ValType rtype) {
        int lsize = ltype.size(), rsize = rtype.size();
        if (lsize == 1 && rsize == 1) {
            dupX1();
        } else if (lsize == 1 && rsize == 2) {
            dup2X1();
        } else if (lsize == 2 && rsize == 1) {
            dupX2();
        } else if (lsize == 2 && rsize == 2) {
            dup2X2();
        } else {
            assert false : "invalid type size";
        }
    }

    /**
     * lvalue, rvalue → rvalue, lvalue
     * 
     * @param ltype
     *            the second topmost stack value
     * @param rtype
     *            the topmost stack value
     */
    public void swap(ValType ltype, ValType rtype) {
        int lsize = ltype.size(), rsize = rtype.size();
        if (lsize == 1 && rsize == 1) {
            swap();
        } else if (lsize == 1 && rsize == 2) {
            swap1_2();
        } else if (lsize == 2 && rsize == 1) {
            swap2_1();
        } else if (lsize == 2 && rsize == 2) {
            swap2();
        } else {
            assert false : "invalid type size";
        }
    }

    /**
     * &#x2205; → value
     * 
     * @param field
     *            the field descriptor
     */
    public void get(FieldDesc field) {
        switch (field.type) {
        case Instance:
            getfield(field.owner, field.name, field.desc);
            break;
        case Static:
            getstatic(field.owner, field.name, field.desc);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * value → &#x2205;
     * 
     * @param field
     *            the field descriptor
     */
    public void put(FieldDesc field) {
        switch (field.type) {
        case Instance:
            putfield(field.owner, field.name, field.desc);
            break;
        case Static:
            putstatic(field.owner, field.name, field.desc);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * parameters → value
     * 
     * @param method
     *            the method descriptor for the invocation
     */
    public void invoke(MethodDesc method) {
        switch (method.type) {
        case Interface:
            invokeinterface(method.owner, method.name, method.desc);
            break;
        case Special:
            invokespecial(method.owner, method.name, method.desc, false);
            break;
        case SpecialInterface:
            invokespecial(method.owner, method.name, method.desc, true);
            break;
        case Static:
            invokestatic(method.owner, method.name, method.desc, false);
            break;
        case StaticInterface:
            invokestatic(method.owner, method.name, method.desc, true);
            break;
        case Virtual:
            invokevirtual(method.owner, method.name, method.desc, false);
            break;
        case VirtualInterface:
            invokevirtual(method.owner, method.name, method.desc, true);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    /**
     * &#x2205; → handle
     * 
     * @param method
     *            the method descriptor for the handle
     */
    public void handle(MethodDesc method) {
        hconst(new Handle(method.type.toTag(), method.owner, method.name, method.desc));
    }

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
     * @see MethodVisitor#visitTryCatchBlock(Label, Label, Label, String)
     */
    public void tryCatch(LocationLabel start, LocationLabel end, LocationLabel handler, Type type) {
        visitTryCatchBlock(start, end, handler, type.getInternalName());
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
     * @see MethodVisitor#visitTryCatchBlock(Label, Label, Label, String)
     */
    public void tryFinally(LocationLabel start, LocationLabel end, LocationLabel handler) {
        visitTryCatchBlock(start, end, handler, null);
    }

    /**
     * Marks the start of a catch-handler.
     * 
     * @param handler
     *            the catch handler label
     * @param exception
     *            the exception type
     * @see #tryCatch(LocationLabel, LocationLabel, LocationLabel, Type)
     */
    public void catchHandler(LocationLabel handler, Type exception) {
        if (stack != null) {
            stack.catchHandler(exception);
        }
        mark(handler);
    }

    /**
     * Marks the start of a finally-handler.
     * 
     * @param handler
     *            the finally handler label
     * @see #tryFinally(LocationLabel, LocationLabel, LocationLabel)
     */
    public void finallyHandler(LocationLabel handler) {
        catchHandler(handler, Types.Throwable);
    }

    /**
     * value → boxed
     * 
     * @param type
     *            the value type
     */
    public void toBoxed(ValType type) {
        toBoxed(type.toType());
    }

    /**
     * value → boxed
     * 
     * @param type
     *            the type kind
     */
    public void toBoxed(Type type) {
        switch (type.getSort()) {
        case Type.VOID:
            return;
        case Type.BOOLEAN:
            invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            return;
        case Type.CHAR:
            invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            return;
        case Type.BYTE:
            invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            return;
        case Type.SHORT:
            invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            return;
        case Type.INT:
            invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            return;
        case Type.FLOAT:
            invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            return;
        case Type.LONG:
            invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            return;
        case Type.DOUBLE:
            invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            return;
        case Type.ARRAY:
        case Type.OBJECT:
        case Type.METHOD:
            return;
        }
    }

    /**
     * boxed → value
     * 
     * @param type
     *            the type kind
     */
    public void toUnboxed(Type type) {
        switch (type.getSort()) {
        case Type.VOID:
            return;
        case Type.BOOLEAN:
            invokevirtual("java/lang/Boolean", "booleanValue", "()Z", false);
            return;
        case Type.CHAR:
            invokevirtual("java/lang/Character", "charValue", "()C", false);
            return;
        case Type.BYTE:
            invokevirtual("java/lang/Byte", "byteValue", "()B", false);
            return;
        case Type.SHORT:
            invokevirtual("java/lang/Short", "shortValue", "()S", false);
            return;
        case Type.INT:
            invokevirtual("java/lang/Integer", "intValue", "()I", false);
            return;
        case Type.FLOAT:
            invokevirtual("java/lang/Float", "floatValue", "()F", false);
            return;
        case Type.LONG:
            invokevirtual("java/lang/Long", "longValue", "()J", false);
            return;
        case Type.DOUBLE:
            invokevirtual("java/lang/Double", "doubleValue", "()D", false);
            return;
        case Type.ARRAY:
        case Type.OBJECT:
        case Type.METHOD:
            return;
        }
    }

    /**
     * Returns the wrapper for {@code type}, or {@code type} if it does not represent a primitive.
     * 
     * @param type
     *            the type kind
     * @return the type's wrapper
     */
    public Type getWrapper(Type type) {
        switch (type.getSort()) {
        case Type.VOID:
            return Types.Void;
        case Type.BOOLEAN:
            return Types.Boolean;
        case Type.CHAR:
            return Types.Character;
        case Type.BYTE:
            return Types.Byte;
        case Type.SHORT:
            return Types.Short;
        case Type.INT:
            return Types.Integer;
        case Type.FLOAT:
            return Types.Float;
        case Type.LONG:
            return Types.Long;
        case Type.DOUBLE:
            return Types.Double;
        case Type.ARRAY:
        case Type.OBJECT:
        case Type.METHOD:
        default:
            return type;
        }
    }
}
