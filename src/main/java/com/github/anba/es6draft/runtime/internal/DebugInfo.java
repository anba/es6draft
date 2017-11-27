/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.compiler.assembler.Code;

/**
 *
 */
public final class DebugInfo {
    private final ArrayList<Method> methods = new ArrayList<>();

    /**
     *
     */
    public static final class Method {
        private final MethodHandle handle;
        private MethodHandleInfo info;

        Method(MethodHandle handle) {
            this.handle = handle;
        }

        private MethodHandleInfo info() {
            if (info == null) {
                info = MethodHandles.publicLookup().revealDirect(handle);
            }
            return info;
        }

        /**
         * Returns the class that declares this method.
         * 
         * @return the declaring class
         */
        public Class<?> getDeclaringClass() {
            return info().getDeclaringClass();
        }

        /**
         * Returns the method name.
         * 
         * @return the method name
         */
        public String getName() {
            return info().getName();
        }

        @Override
        public String toString() {
            return String.format("class=%s, method=%s", getDeclaringClass(), getName());
        }

        /**
         * Returns the disassembled bytecode.
         * 
         * @return the disassembled bytecode
         */
        public String disassemble() {
            byte[] bytes;
            try {
                bytes = classBytes(getDeclaringClass());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return Code.methodToByteCode(bytes, getName(), handle.type());
        }
    }

    // called from generated code
    public void addMethod(MethodHandle handle) {
        methods.add(new Method(handle));
    }

    /**
     * Returns the list of generated methods.
     * 
     * @return the list of generated methods
     */
    public List<Method> getMethods() {
        return methods;
    }

    /**
     * Returns the compiled class bytes from {@code declaringClass}.
     * 
     * @param declaringClass
     *            the declaring class
     * @return the class bytes
     * @throws ReflectiveOperationException
     *             if the class bytes could not be retrieved
     */
    public static byte[] classBytes(Class<?> declaringClass) throws ReflectiveOperationException {
        // classBytes is an internal field created in Compiler#defineAndLoad
        Field classBytes = declaringClass.getDeclaredField("classBytes");
        classBytes.setAccessible(true);
        return (byte[]) classBytes.get(null);
    }
}
