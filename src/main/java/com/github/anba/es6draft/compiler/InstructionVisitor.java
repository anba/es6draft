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
        private static final Type INVALID = Type.getType("invalid");
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
            assert variables.get(var);
            Type type = types[var];
            assert type != null && type != INVALID : type;
            variables.clear(var);
        }
    }

    static final class Variable<T> {
        private final String name;
        private final Type type;
        private final int var;
        private boolean live = true;

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

    private static final int MAX_STRING_SIZE = 16384;

    private final String methodName;
    private final Type methodDescriptor;
    private final Variables variables = new Variables();
    private final ClassValue<Type> typeCache = new ClassValue<Type>() {
        @Override
        protected Type computeValue(Class<?> c) {
            return Type.getType(c);
        }
    };

    private Type getType(Class<?> c) {
        return typeCache.get(c);
    }

    protected InstructionVisitor(MethodVisitor mv, String methodName, Type methodDescriptor) {
        super(Opcodes.ASM4, mv);
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        initParams(methodDescriptor);
    }

    public String getMethodName() {
        return methodName;
    }

    public Type getMethodDescriptor() {
        return methodDescriptor;
    }

    private void initParams(Type methodType) {
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (int i = 0, var = 0, len = argumentTypes.length; i < len; ++i) {
            reserveFixedSlot(var, argumentTypes[i]);
            var += argumentTypes[i].getSize();
        }
    }

    private void reserveFixedSlot(int var, Type type) {
        variables.reserveFixedSlot(var, type);
    }

    protected final <T> Variable<T> reserveFixedSlot(int var, Class<T> clazz) {
        Type type = getType(clazz);
        reserveFixedSlot(var, type);
        return new Variable<>("(fixed-slot)", type, var);
    }

    private static int parameterSlot(int index, Type[] argumentTypes) {
        int slot = 0;
        for (int i = 0; i < index; ++i) {
            slot += argumentTypes[i].getSize();
        }
        return slot;
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

    public <T> Variable<T> newVariable(String name, Class<T> clazz) {
        Type type = getType(clazz);
        return new Variable<>(name, type, variables.newVariable(type));
    }

    public void freeVariable(Variable<?> var) {
        assert var.live && !(var.live = false);
        variables.freeVariable(var.var);
    }

    @Override
    @Deprecated
    public void load(int var, Type type) {
        super.load(var, type);
    }

    public void load(Variable<?> var) {
        super.load(var.var, var.type);
    }

    @Override
    @Deprecated
    public void store(int var, Type type) {
        super.store(var, type);
    }

    public void store(Variable<?> var) {
        super.store(var.var, var.type);
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
     * [] → value
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
     * array → array
     */
    public void astore(int index, String element) {
        dup();
        iconst(index);
        aconst(element);
        astore(Types.String);
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
