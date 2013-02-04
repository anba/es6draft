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

/**
 * <h1>10 Executable Code and Execution Contexts</h1>
 * <ul>
 * <li>10.5 Declaration Binding Instantiation
 * </ul>
 */
abstract class DeclarationBindingInstantiationGenerator {
    protected final CodeGenerator codegen;

    DeclarationBindingInstantiationGenerator(CodeGenerator codegen) {
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

    protected void initializeBinding(int envRec, String name, InstructionVisitor mv) {
        // stack: [obj] -> []
        mv.load(envRec, Types.EnvironmentRecord);
        mv.swap();
        mv.aconst(name);
        mv.swap();
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
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

    protected void InstantiateFunctionObject(int realm, int env, FunctionDeclaration f,
            InstructionVisitor mv) {
        codegen.compile(f);

        mv.load(realm, Types.Realm);
        mv.load(env, Types.LexicalEnvironment);
        mv.invokestatic(codegen.getClassName(), codegen.methodName(f) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));

        mv.invoke(Methods.OrdinaryFunction_InstantiateFunctionObject);
    }

    protected void InstantiateGeneratorObject(int realm, int env, GeneratorDeclaration f,
            InstructionVisitor mv) {
        codegen.compile(f);

        mv.load(realm, Types.Realm);
        mv.load(env, Types.LexicalEnvironment);
        mv.invokestatic(codegen.getClassName(), codegen.methodName(f) + "_rti",
                Type.getMethodDescriptor(Types.RuntimeInfo$Function));

        mv.invoke(Methods.OrdinaryGenerator_InstantiateGeneratorObject);
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
