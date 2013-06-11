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
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;

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
                        .getMethodType(Types.OrdinaryFunction, Types.LexicalEnvironment,
                                Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_InstantiateGeneratorObject = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstantiateGeneratorObject", Type
                        .getMethodType(Types.OrdinaryGenerator, Types.LexicalEnvironment,
                                Types.ExecutionContext, Types.RuntimeInfo$Function));
    }

    protected final CodeGenerator codegen;

    protected DeclarationBindingInstantiationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#hasBinding(String)}
     * <p>
     * stack: [] -> [boolean]
     */
    protected void hasBinding(int envRec, String name, InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        hasBinding(name, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#hasBinding(String)}
     * <p>
     * stack: [envRec] -> [boolean]
     */
    protected void hasBinding(String name, InstructionVisitor mv) {
        mv.aconst(name);
        mv.invoke(Methods.EnvironmentRecord_hasBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createMutableBinding(String, boolean)}
     * <p>
     * stack: [] -> []
     */
    protected void createMutableBinding(int envRec, String name, boolean deletable,
            InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        createMutableBinding(name, deletable, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createMutableBinding(String, boolean)}
     * <p>
     * stack: [envRec] -> []
     */
    protected void createMutableBinding(String name, boolean deletable, InstructionVisitor mv) {
        mv.aconst(name);
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createMutableBinding(String, boolean)}
     * <p>
     * stack: [] -> []
     */
    protected void createMutableBinding(int envRec, String name, int deletable,
            InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        mv.aconst(name);
        mv.load(deletable, Type.BOOLEAN_TYPE);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createImmutableBinding(String)}
     * <p>
     * stack: [] -> []
     */
    protected void createImmutableBinding(int envRec, String name, InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        createImmutableBinding(name, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createImmutableBinding(String)}
     * <p>
     * stack: [envRec] -> []
     */
    protected void createImmutableBinding(String name, InstructionVisitor mv) {
        mv.aconst(name);
        mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#initialiseBinding(String, Object)}
     * <p>
     * stack: [obj] -> []
     */
    protected void initialiseBinding(int envRec, String name, InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        mv.swap();
        initialiseBinding(name, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#initialiseBinding(String, Object)}
     * <p>
     * stack: [envRec, obj] -> []
     */
    protected void initialiseBinding(String name, InstructionVisitor mv) {
        mv.aconst(name);
        mv.swap();
        mv.invoke(Methods.EnvironmentRecord_initialiseBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#setMutableBinding(String, Object, boolean)}
     * <p>
     * stack: [obj] -> []
     */
    protected void setMutableBinding(int envRec, String name, boolean strict, InstructionVisitor mv) {
        mv.load(envRec, Types.EnvironmentRecord);
        mv.swap();
        setMutableBinding(name, strict, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#setMutableBinding(String, Object, boolean)}
     * <p>
     * stack: [envRec, obj] -> []
     */
    protected void setMutableBinding(String name, boolean strict, InstructionVisitor mv) {
        mv.aconst(name);
        mv.swap();
        mv.iconst(strict);
        mv.invoke(Methods.EnvironmentRecord_setMutableBinding);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [] -> [fo]
     */
    protected void InstantiateFunctionObject(int context, int env, FunctionDeclaration f,
            InstructionVisitor mv) {
        mv.load(env, Types.LexicalEnvironment);
        mv.load(context, Types.ExecutionContext);

        InstantiateFunctionObject(f, mv);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [env, cx] -> [fo]
     */
    protected void InstantiateFunctionObject(FunctionDeclaration f, InstructionVisitor mv) {
        codegen.compile(f);

        mv.invokestatic(codegen.getClassName(), codegen.methodName(f, FunctionName.RTI),
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));

        mv.invoke(Methods.ScriptRuntime_InstantiateFunctionObject);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateGeneratorObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [] -> [fo]
     */
    protected void InstantiateGeneratorObject(int context, int env, GeneratorDeclaration f,
            InstructionVisitor mv) {
        mv.load(env, Types.LexicalEnvironment);
        mv.load(context, Types.ExecutionContext);

        InstantiateGeneratorObject(f, mv);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateGeneratorObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [env, cx] -> [fo]
     */
    protected void InstantiateGeneratorObject(GeneratorDeclaration f, InstructionVisitor mv) {
        codegen.compile(f);

        mv.invokestatic(codegen.getClassName(), codegen.methodName(f, FunctionName.RTI),
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
