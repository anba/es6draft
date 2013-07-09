/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.Arrays;
import java.util.BitSet;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import com.github.anba.es6draft.compiler.DefaultCodeGenerator.ValType;

/**
 *
 */
class InstructionVisitor extends InstructionAdapter {
    private static class Variables {
        private static final Type INVALID = Type.getType("Invalid");
        private static final int INITIAL_SIZE = 8;
        private final BitSet variables = new BitSet();
        private Type[] types = new Type[INITIAL_SIZE];

        void reserveFixedSlot(int var, Type type) {
            if (type.getSize() == 1) {
                assert var < INITIAL_SIZE;
                assert types[var] == null || types[var].equals(type);
                types[var] = type;
                variables.set(var);
            } else {
                assert var + 1 < INITIAL_SIZE;
                assert types[var] == null || types[var].equals(type);
                assert types[var + 1] == null || types[var + 1] == INVALID;
                types[var] = type;
                types[var + 1] = INVALID;
                variables.set(var, var + 2);
            }
        }

        int newVariable(Type type) {
            int size = type.getSize();
            for (int var = 0;;) {
                var = variables.nextClearBit(var);
                if (var + size > types.length) {
                    int newLength = types.length + (types.length >>> 1);
                    types = Arrays.copyOf(types, newLength, Type[].class);
                }
                Type old = types[var];
                if (old == null || old.equals(type)) {
                    if (type.getSize() != 2) {
                        types[var] = type;
                        variables.set(var);
                        return var;
                    } else {
                        Type next = types[var + 1];
                        if (next == null || next == INVALID) {
                            types[var] = type;
                            types[var + 1] = INVALID;
                            variables.set(var, var + 2);
                            return var;
                        }
                    }
                }
                // try next index
                var += 1;
            }
        }

        void freeVariable(int var) {
            Type type = types[var];
            assert type != null && type != INVALID;
            variables.clear(var);
        }
    }

    static class Variable<T> {
        private final String name;
        private final Type type;
        private final int var;

        private Variable(String name, Type type, int var) {
            this.name = name;
            this.type = type;
            this.var = var;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }
    }

    enum MethodType {
        Interface, Virtual, Special, Static
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

    final String methodName;
    final Type methodDescriptor;
    final Variables variables = new Variables();
    final ClassValue<Type> typeCache = new ClassValue<Type>() {
        @Override
        protected Type computeValue(Class<?> type) {
            return Type.getType(type);
        }
    };

    protected InstructionVisitor(MethodVisitor mv, String methodName, Type methodDescriptor) {
        super(Opcodes.ASM4, mv);
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        initParams(methodDescriptor);
    }

    private void initParams(Type methodType) {
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (int i = 0, len = argumentTypes.length; i < len; ++i) {
            // FIXME: var-size!
            reserveFixedSlot(i, argumentTypes[i]);
        }
    }

    protected final void reserveFixedSlot(int var, Type type) {
        variables.reserveFixedSlot(var, type);
    }

    private static int parameterSlot(int index, Type[] argumentTypes) {
        int slot = 0;
        for (int i = 0; i < index; ++i) {
            slot += argumentTypes[i].getSize();
        }
        return slot;
    }

    public Variable<?> getParameter(int index) {
        Type[] argTypes = methodDescriptor.getArgumentTypes();
        return new Variable<>("(parameter)", argTypes[index], parameterSlot(index, argTypes));
    }

    public <T> Variable<T> getParameter(int index, Class<T> clazz) {
        Type[] argTypes = methodDescriptor.getArgumentTypes();
        if (!argTypes[index].equals(typeCache.get(clazz))) {
            throw new IllegalArgumentException();
        }
        return new Variable<>("(parameter)", argTypes[index], parameterSlot(index, argTypes));
    }

    public Variable<?> newVariable(String name, Type type) {
        return new Variable<>(name, type, variables.newVariable(type));
    }

    public <T> Variable<T> newVariable(String name, Class<T> clazz) {
        Type type = typeCache.get(clazz);
        return new Variable<>(name, type, variables.newVariable(type));
    }

    public void freeVariable(Variable<?> var) {
        variables.freeVariable(var.var);
    }

    public void load(Variable<?> var) {
        load(var.var, var.type);
    }

    public void store(Variable<?> var) {
        store(var.var, var.type);
    }

    public void begin() {
        visitCode();
    }

    public void end() {
        visitMaxs(0, 0);
        visitEnd();
    }

    public void lineInfo(int line) {
        Label start = new Label();
        mv.visitLabel(start);
        mv.visitLineNumber(line, start);
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
     * [] → value
     */
    public void iconst(boolean b) {
        iconst(b ? 1 : 0);
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
        load(0, Types.Object);
    }

    /**
     * &#x2205; → array
     */
    public void newarray(int length, Type type) {
        iconst(length);
        newarray(type);
    }

    /**
     * array → array
     */
    public void astore(int index, Object element, Type type) {
        dup();
        iconst(index);
        aconst(element);
        astore(type);
    }

    /**
     * array → value
     */
    public void aload(int index, Type type) {
        iconst(index);
        aload(type);
    }

    /**
     * value → value, value
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
     */
    public void swap(ValType ltype, ValType rtype) {
        int lsize = ltype.size(), rsize = rtype.size();
        if (lsize == 1 && rsize == 1) {
            swap();
        } else if (lsize == 1 && rsize == 2) {
            dup2X1();
            pop2();
        } else if (lsize == 2 && rsize == 1) {
            dupX2();
            pop();
        } else if (lsize == 2 && rsize == 2) {
            dup2X2();
            pop2();
        } else {
            assert false : "invalid type size";
        }
    }

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

    public void invoke(MethodDesc method) {
        switch (method.type) {
        case Interface:
            invokeinterface(method.owner, method.name, method.desc);
            break;
        case Special:
            invokespecial(method.owner, method.name, method.desc);
            break;
        case Static:
            invokestatic(method.owner, method.name, method.desc);
            break;
        case Virtual:
            invokevirtual(method.owner, method.name, method.desc);
            break;
        default:
            throw new IllegalStateException();
        }
    }

    public void invokeStaticMH(String className, String name, String desc) {
        hconst(new Handle(Opcodes.H_INVOKESTATIC, className, name, desc));
    }

    public void toBoxed(ValType type) {
        toBoxed(type.toType());
    }

    public void toBoxed(Type type) {
        switch (type.getSort()) {
        case Type.VOID:
            return;
        case Type.BOOLEAN:
            invokestatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
            return;
        case Type.CHAR:
            invokestatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
            return;
        case Type.BYTE:
            invokestatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
            return;
        case Type.SHORT:
            invokestatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
            return;
        case Type.INT:
            invokestatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            return;
        case Type.FLOAT:
            invokestatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
            return;
        case Type.LONG:
            invokestatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
            return;
        case Type.DOUBLE:
            invokestatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
            return;
        case Type.ARRAY:
        case Type.OBJECT:
        case Type.METHOD:
            return;
        }
    }
}
