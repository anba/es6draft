/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 
 */
public final class Type {
    /** Internal type to represent the reserved slot of a two-byte slot type. */
    static final Type RESERVED = new Type("reserved");

    /** The {@code void} type. */
    public static final Type VOID_TYPE = new Type("V", org.objectweb.asm.Type.VOID_TYPE);
    /** The {@code boolean} type. */
    public static final Type BOOLEAN_TYPE = new Type("Z", org.objectweb.asm.Type.BOOLEAN_TYPE);
    /** The {@code char} type. */
    public static final Type CHAR_TYPE = new Type("C", org.objectweb.asm.Type.CHAR_TYPE);
    /** The {@code byte} type. */
    public static final Type BYTE_TYPE = new Type("B", org.objectweb.asm.Type.BYTE_TYPE);
    /** The {@code short} type. */
    public static final Type SHORT_TYPE = new Type("S", org.objectweb.asm.Type.SHORT_TYPE);
    /** The {@code int} type. */
    public static final Type INT_TYPE = new Type("I", org.objectweb.asm.Type.INT_TYPE);
    /** The {@code long} type. */
    public static final Type LONG_TYPE = new Type("J", org.objectweb.asm.Type.LONG_TYPE);
    /** The {@code float} type. */
    public static final Type FLOAT_TYPE = new Type("F", org.objectweb.asm.Type.FLOAT_TYPE);
    /** The {@code double} type. */
    public static final Type DOUBLE_TYPE = new Type("D", org.objectweb.asm.Type.DOUBLE_TYPE);

    private final String descriptor;
    private transient org.objectweb.asm.Type type;

    private Type(String descriptor) {
        this.descriptor = descriptor;
    }

    private Type(String descriptor, org.objectweb.asm.Type type) {
        this.descriptor = descriptor;
        this.type = type;
    }

    @Override
    public String toString() {
        return descriptor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != Type.class) {
            return false;
        }
        return descriptor.equals(((Type) obj).descriptor);
    }

    @Override
    public int hashCode() {
        return descriptor.hashCode();
    }

    /*package*/org.objectweb.asm.Type type() {
        if (type == null) {
            type = org.objectweb.asm.Type.getType(descriptor);
        }
        return type;
    }

    /**
     * Returns the type descriptor string.
     * 
     * @return the type descriptor string
     */
    /*package*/String descriptor() {
        return descriptor;
    }

    /**
     * Returns the internal name.
     * 
     * @return the internal name
     */
    /*package*/String internalName() {
        return type().getInternalName();
    }

    /**
     * Returns the size of this type.
     * 
     * @return the size
     */
    public int getSize() {
        return type().getSize();
    }

    public boolean isPrimitive() {
        return getSort() < Type.Sort.ARRAY;
    }

    /*package*/int getSort() {
        return type().getSort();
    }

    /*package*/int getOpcode(int opcode) {
        return type().getOpcode(opcode);
    }

    /*package*/Type asArray() {
        return Type.of("[" + descriptor());
    }

    /**
     * Returns the wrapper for the type, or {@code this} if it does not represent a primitive.
     * 
     * @return the type's wrapper object
     */
    /*package*/Type asWrapper() {
        switch (getSort()) {
        case Type.Sort.VOID:
            return Types.Void;
        case Type.Sort.BOOLEAN:
            return Types.Boolean;
        case Type.Sort.CHAR:
            return Types.Character;
        case Type.Sort.BYTE:
            return Types.Byte;
        case Type.Sort.SHORT:
            return Types.Short;
        case Type.Sort.INT:
            return Types.Integer;
        case Type.Sort.FLOAT:
            return Types.Float;
        case Type.Sort.LONG:
            return Types.Long;
        case Type.Sort.DOUBLE:
            return Types.Double;
        case Type.Sort.ARRAY:
        case Type.Sort.OBJECT:
        case Type.Sort.METHOD:
        default:
            return this;
        }
    }

    static final class Sort {
        private Sort() {
        }

        static final int VOID = org.objectweb.asm.Type.VOID;
        static final int BOOLEAN = org.objectweb.asm.Type.BOOLEAN;
        static final int CHAR = org.objectweb.asm.Type.CHAR;
        static final int BYTE = org.objectweb.asm.Type.BYTE;
        static final int SHORT = org.objectweb.asm.Type.SHORT;
        static final int INT = org.objectweb.asm.Type.INT;
        static final int FLOAT = org.objectweb.asm.Type.FLOAT;
        static final int LONG = org.objectweb.asm.Type.LONG;
        static final int DOUBLE = org.objectweb.asm.Type.DOUBLE;
        static final int ARRAY = org.objectweb.asm.Type.ARRAY;
        static final int OBJECT = org.objectweb.asm.Type.OBJECT;
        static final int METHOD = org.objectweb.asm.Type.METHOD;
    }

    /**
     * Create a type instance for a named object.
     * 
     * @param internalName
     *            the internal name of the object
     * @return the type object
     */
    /*package*/static Type forName(String internalName) {
        return of(org.objectweb.asm.Type.getObjectType(internalName).getDescriptor());
    }

    /**
     * Create a type instance for the given type descriptor string.
     * 
     * @param descriptor
     *            the type descriptor string
     * @return the type object
     */
    /*package*/static Type of(String descriptor) {
        return new Type(descriptor);
    }

    private static final Map<Class<?>, Type> typeCache = Collections
            .synchronizedMap(new WeakHashMap<Class<?>, Type>());

    /**
     * Create a type instance for the given class.
     * 
     * @param clazz
     *            the class instance
     * @return the type object
     */
    public static Type of(Class<?> clazz) {
        Type type = typeCache.get(clazz);
        if (type == null) {
            typeCache.put(clazz, type = new Type(org.objectweb.asm.Type.getDescriptor(clazz)));
        }
        return type;
    }

    /**
     * Create a type instance for the given sort kind.
     * 
     * @param sort
     *            the sort kind
     * @return the type object
     */
    /*package*/static Type of(int sort) {
        switch (sort) {
        case Type.Sort.VOID:
            return Type.VOID_TYPE;
        case Type.Sort.BOOLEAN:
            return Type.BOOLEAN_TYPE;
        case Type.Sort.CHAR:
            return Type.CHAR_TYPE;
        case Type.Sort.BYTE:
            return Type.BYTE_TYPE;
        case Type.Sort.SHORT:
            return Type.SHORT_TYPE;
        case Type.Sort.INT:
            return Type.INT_TYPE;
        case Type.Sort.FLOAT:
            return Type.FLOAT_TYPE;
        case Type.Sort.LONG:
            return Type.LONG_TYPE;
        case Type.Sort.DOUBLE:
            return Type.DOUBLE_TYPE;
        case Type.Sort.ARRAY:
            return Types.Object_;
        case Type.Sort.OBJECT:
            return Types.Object;
        case Type.Sort.METHOD:
        default:
            return null;
        }
    }

    /**
     * Convenience method for {@link MethodTypeDescriptor#methodType(Type, Type...)}.
     * 
     * @param returnType
     *            the return type
     * @param parameters
     *            the parameter types
     * @return the method type descriptor
     */
    public static MethodTypeDescriptor methodType(Type returnType, Type... parameters) {
        return MethodTypeDescriptor.methodType(returnType, parameters);
    }
}
