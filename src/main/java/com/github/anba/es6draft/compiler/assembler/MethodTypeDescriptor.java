/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler.assembler;

import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Method type descriptor object.
 */
public final class MethodTypeDescriptor {
    private final String descriptor;
    private final Type returnType;
    private final Type[] parameters;

    private MethodTypeDescriptor(String descriptor) {
        assert descriptor != null;
        this.descriptor = descriptor;
        this.returnType = getReturnType(descriptor);
        this.parameters = getParameters(descriptor);
    }

    private MethodTypeDescriptor(Type returnType, Type[] parameters) {
        this.descriptor = getMethodDescriptor(returnType, parameters);
        this.returnType = returnType;
        this.parameters = parameters;
    }

    private static String getMethodDescriptor(Type returnType, Type... argumentTypes) {
        org.objectweb.asm.Type[] args = new org.objectweb.asm.Type[argumentTypes.length];
        for (int i = 0; i < argumentTypes.length; ++i) {
            args[i] = argumentTypes[i].type();
        }
        return org.objectweb.asm.Type.getMethodDescriptor(returnType.type(), args);
    }

    private static Type getReturnType(String methodDescriptor) {
        return Type.of(org.objectweb.asm.Type.getReturnType(methodDescriptor).getDescriptor());
    }

    private static Type[] getParameters(String methodDescriptor) {
        org.objectweb.asm.Type[] args = org.objectweb.asm.Type.getArgumentTypes(methodDescriptor);
        Type[] argumentTypes = new Type[args.length];
        for (int i = 0; i < args.length; ++i) {
            argumentTypes[i] = Type.of(args[i].getDescriptor());
        }
        return argumentTypes;
    }

    /*package*/org.objectweb.asm.Type type() {
        return org.objectweb.asm.Type.getMethodType(descriptor);
    }

    /**
     * Returns the method descriptor string.
     * 
     * @return the method descriptor string
     */
    public String descriptor() {
        return descriptor;
    }

    /**
     * Returns the return type.
     * 
     * @return the return type
     */
    public Type returnType() {
        return returnType;
    }

    /**
     * Returns the number of parameters.
     * 
     * @return the number of parameters
     */
    public int parameterCount() {
        return parameters.length;
    }

    /**
     * Returns the parameter type.
     * 
     * @param index
     *            the parameter index
     * @return the parameter type
     */
    public Type parameterType(int index) {
        return parameters[index];
    }

    /**
     * Returns the parameters as a list.
     * 
     * @return the parameters
     */
    public List<Type> parameterList() {
        return Collections.unmodifiableList(Arrays.asList(parameters));
    }

    /**
     * Creates a new method type descriptor.
     * 
     * @param methodType
     *            the method type
     * @return the method type descriptor
     */
    public static MethodTypeDescriptor methodType(MethodType methodType) {
        return new MethodTypeDescriptor(methodType.toMethodDescriptorString());
    }

    /**
     * Creates a new method type descriptor.
     * 
     * @param returnType
     *            the return type
     * @param parameters
     *            the parameter types
     * @return the method type descriptor
     */
    public static MethodTypeDescriptor methodType(Class<?> returnType, Class<?>... parameters) {
        return new MethodTypeDescriptor(MethodType.methodType(returnType, parameters)
                .toMethodDescriptorString());
    }

    /**
     * Creates a new method type descriptor.
     * 
     * @param returnType
     *            the return type
     * @param parameters
     *            the parameter types
     * @return the method type descriptor
     */
    public static MethodTypeDescriptor methodType(Type returnType, Type... parameters) {
        return new MethodTypeDescriptor(returnType, parameters);
    }
}
