/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

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
        private final Class<?> owner;
        private final String name;

        Method(Class<?> owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        /**
         * Returns the class that declares this method.
         * 
         * @return the declaring class
         */
        public Class<?> getDeclaringClass() {
            return owner;
        }

        /**
         * Returns the method name.
         * 
         * @return the method name
         */
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return String.format("class=%s, method=%s", owner, name);
        }

        /**
         * Returns the disassembled bytecode.
         * 
         * @return the disassembled bytecode
         */
        public String disassemble() {
            byte[] bytes;
            try {
                // classBytes is an internal field created in Compiler#defineAndLoad
                Field classBytes = getDeclaringClass().getDeclaredField("classBytes");
                classBytes.setAccessible(true);
                bytes = (byte[]) classBytes.get(null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            return Code.methodToByteCode(bytes, getName());
        }
    }

    // called from generated code
    public void addMethod(Class<?> owner, String name) {
        methods.add(new Method(owner, name));
    }

    /**
     * Returns the list of generated methods.
     * 
     * @return the list of generated methods
     */
    public List<Method> getMethods() {
        return methods;
    }
}
