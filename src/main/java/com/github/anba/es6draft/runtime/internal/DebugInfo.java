/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceMethodVisitor;

import com.github.anba.es6draft.compiler.assembler.SimpleTypeTextifier;

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

            Printer p = new SimpleTypeTextifier();
            ClassReader cr = new ClassReader(bytes);
            cr.accept(new FilterMethodVisitor(p, getName()), ClassReader.EXPAND_FRAMES);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            p.print(pw);
            pw.flush();
            return sw.toString();
        }

        private static final class FilterMethodVisitor extends ClassVisitor {
            private final Printer printer;
            private final String methodName;

            public FilterMethodVisitor(Printer p, String methodName) {
                super(Opcodes.ASM5);
                this.printer = p;
                this.methodName = methodName;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                    String signature, String[] exceptions) {
                if (methodName.equals(name)) {
                    Printer p = printer.visitMethod(access, name, desc, signature, exceptions);
                    return new TraceMethodVisitor(null, p);
                }
                return null;
            }
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
