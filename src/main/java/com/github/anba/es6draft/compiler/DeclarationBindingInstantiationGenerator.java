/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;

/**
 * <h1>10 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>10.5 Declaration Binding Instantiation
 * </ul>
 */
class DeclarationBindingInstantiationGenerator {
    private static class Methods {
        // class: EnvironmentRecord
        static final MethodDesc EnvironmentRecord_hasBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "hasBinding",
                Type.getMethodType(Type.BOOLEAN_TYPE, Types.String));

        static final MethodDesc EnvironmentRecord_createMutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodDesc EnvironmentRecord_createImmutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "createImmutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String));

        static final MethodDesc EnvironmentRecord_initialiseBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initialiseBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        static final MethodDesc EnvironmentRecord_setMutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "setMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object, Type.BOOLEAN_TYPE));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_InstantiateFunctionObject = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstantiateFunctionObject", Type
                        .getMethodType(Types.OrdinaryFunction, Types.ExecutionContext,
                                Types.LexicalEnvironment, Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_InstantiateGeneratorObject = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstantiateGeneratorObject", Type
                        .getMethodType(Types.OrdinaryGenerator, Types.ExecutionContext,
                                Types.LexicalEnvironment, Types.RuntimeInfo$Function));
    }

    protected final CodeGenerator codegen;

    protected DeclarationBindingInstantiationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    protected void hasBinding(int envRec, String name, InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        mv.aconst(name);
        mv.invoke(Methods.EnvironmentRecord_hasBinding);
    }

    protected void createMutableBinding(int envRec, String name, boolean deletable,
            InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        mv.aconst(name);
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    protected void createMutableBinding(int envRec, String name, int deletable,
            InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        mv.aconst(name);
        mv.load(deletable, Type.BOOLEAN_TYPE);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    protected void createImmutableBinding(int envRec, String name, InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        mv.aconst(name);
        mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);
    }

    protected void initialiseBinding(int envRec, String name, InstructionVisitor mv) {
        // stack: [obj] -> []
        mv.load(envRec, Types.EnvironmentRecord);
        mv.swap();
        mv.aconst(name);
        mv.swap();
        mv.invoke(Methods.EnvironmentRecord_initialiseBinding);
    }

    protected void setMutableBinding(int envRec, String name, boolean strict, InstructionVisitor mv) {
        // stack: [obj] -> []
        mv.load(envRec, Types.EnvironmentRecord);
        mv.swap();
        mv.aconst(name);
        mv.swap();
        mv.iconst(strict);
        mv.invoke(Methods.EnvironmentRecord_setMutableBinding);
    }

    protected void InstantiateFunctionObject(int context, int env, FunctionDeclaration f,
            InstructionVisitor mv) {
        codegen.compile(f);

        mv.load(context, Types.ExecutionContext);
        mv.load(env, Types.LexicalEnvironment);
        mv.invokestatic(codegen.getClassName(), codegen.methodName(f) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));

        mv.invoke(Methods.ScriptRuntime_InstantiateFunctionObject);
    }

    protected void InstantiateGeneratorObject(int context, int env, GeneratorDeclaration f,
            InstructionVisitor mv) {
        codegen.compile(f);

        mv.load(context, Types.ExecutionContext);
        mv.load(env, Types.LexicalEnvironment);
        mv.invokestatic(codegen.getClassName(), codegen.methodName(f) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));

        mv.invoke(Methods.ScriptRuntime_InstantiateGeneratorObject);
    }

    protected static String BoundName(Declaration d) {
        if (d instanceof FunctionDeclaration) {
            return ((FunctionDeclaration) d).getIdentifier().getName();
        } else {
            assert d instanceof GeneratorDeclaration;
            return ((GeneratorDeclaration) d).getIdentifier().getName();
        }
    }

    protected static <T> Iterable<T> reverse(final List<T> list) {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    ListIterator<T> iter = list.listIterator(list.size());

                    @Override
                    public boolean hasNext() {
                        return iter.hasPrevious();
                    }

                    @Override
                    public T next() {
                        return iter.previous();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
