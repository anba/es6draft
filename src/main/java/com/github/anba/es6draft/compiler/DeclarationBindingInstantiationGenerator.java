/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.objectweb.asm.Type;

import com.github.anba.es6draft.ast.AsyncFunctionDeclaration;
import com.github.anba.es6draft.ast.BindingIdentifier;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.LegacyGeneratorDeclaration;
import com.github.anba.es6draft.ast.StatementListItem;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.CodeGenerator.FunctionName;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodDesc;
import com.github.anba.es6draft.compiler.InstructionVisitor.MethodType;
import com.github.anba.es6draft.compiler.InstructionVisitor.Variable;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;

/**
 * Base class for Binding Instantiation generators
 */
abstract class DeclarationBindingInstantiationGenerator {
    private static final class Methods {
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

        static final MethodDesc EnvironmentRecord_initializeBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "initializeBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object));

        static final MethodDesc EnvironmentRecord_setMutableBinding = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "setMutableBinding",
                Type.getMethodType(Type.VOID_TYPE, Types.String, Types.Object, Type.BOOLEAN_TYPE));

        static final MethodDesc EnvironmentRecord_getBindingValue = MethodDesc.create(
                MethodType.Interface, Types.EnvironmentRecord, "getBindingValue",
                Type.getMethodType(Types.Object, Types.String, Type.BOOLEAN_TYPE));

        // class: LexicalEnvironment
        static final MethodDesc LexicalEnvironment_getEnvRec = MethodDesc.create(
                MethodType.Virtual, Types.LexicalEnvironment, "getEnvRec",
                Type.getMethodType(Types.EnvironmentRecord));

        // class: ScriptRuntime
        static final MethodDesc ScriptRuntime_InstantiateAsyncFunctionObject = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstantiateAsyncFunctionObject", Type
                        .getMethodType(Types.OrdinaryAsyncFunction, Types.LexicalEnvironment,
                                Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_InstantiateFunctionObject = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstantiateFunctionObject", Type
                        .getMethodType(Types.OrdinaryFunction, Types.LexicalEnvironment,
                                Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_InstantiateGeneratorObject = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstantiateGeneratorObject", Type
                        .getMethodType(Types.OrdinaryGenerator, Types.LexicalEnvironment,
                                Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodDesc ScriptRuntime_InstantiateLegacyGeneratorObject = MethodDesc.create(
                MethodType.Static, Types.ScriptRuntime, "InstantiateLegacyGeneratorObject", Type
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
     * stack: [] {@literal ->} [boolean]
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected void hasBinding(Variable<? extends EnvironmentRecord> envRec, Name name,
            InstructionVisitor mv) {
        mv.load(envRec);
        hasBinding(name, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#hasBinding(String)}
     * <p>
     * stack: [envRec] {@literal ->} [boolean]
     * 
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected void hasBinding(Name name, InstructionVisitor mv) {
        mv.aconst(name.getIdentifier());
        mv.invoke(Methods.EnvironmentRecord_hasBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createMutableBinding(String, boolean)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param deletable
     *            the deletable flag
     * @param mv
     *            the instruction visitor
     */
    protected void createMutableBinding(Variable<? extends EnvironmentRecord> envRec, Name name,
            boolean deletable, InstructionVisitor mv) {
        mv.load(envRec);
        createMutableBinding(name, deletable, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createMutableBinding(String, boolean)}
     * <p>
     * stack: [envRec] {@literal ->} []
     * 
     * @param name
     *            the binding name
     * @param deletable
     *            the deletable flag
     * @param mv
     *            the instruction visitor
     */
    protected void createMutableBinding(Name name, boolean deletable, InstructionVisitor mv) {
        mv.aconst(name.getIdentifier());
        mv.iconst(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createMutableBinding(String, boolean)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param deletable
     *            the variable which holds the deletable flag
     * @param mv
     *            the instruction visitor
     */
    protected void createMutableBinding(Variable<? extends EnvironmentRecord> envRec, Name name,
            Variable<Boolean> deletable, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.load(deletable);
        mv.invoke(Methods.EnvironmentRecord_createMutableBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createImmutableBinding(String)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected void createImmutableBinding(Variable<? extends EnvironmentRecord> envRec, Name name,
            InstructionVisitor mv) {
        mv.load(envRec);
        createImmutableBinding(name, mv);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#createImmutableBinding(String)}
     * <p>
     * stack: [envRec] {@literal ->} []
     * 
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected void createImmutableBinding(Name name, InstructionVisitor mv) {
        mv.aconst(name.getIdentifier());
        mv.invoke(Methods.EnvironmentRecord_createImmutableBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#initializeBinding(String, Object)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param value
     *            the variable which holds the binding value
     * @param mv
     *            the instruction visitor
     */
    protected void initializeBinding(Variable<? extends EnvironmentRecord> envRec, Name name,
            Variable<?> value, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.load(value);
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#initializeBinding(String, Object)}
     * <p>
     * stack: [envRec, name, obj] {@literal ->} []
     * 
     * @param mv
     *            the instruction visitor
     */
    protected void initializeBinding(InstructionVisitor mv) {
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    /**
     * Emit fused function call for: {@link EnvironmentRecord#getBindingValue(String, boolean)} and
     * {@link EnvironmentRecord#initializeBinding(String, Object)}
     * <p>
     * stack: [] {@literal ->} []
     * 
     * @param targetEnvRec
     *            the variable which holds the target environment record
     * @param sourceEnvRec
     *            the variable which holds the source environment record
     * @param name
     *            the binding name
     * @param strict
     *            the strict-mode flag
     * @param mv
     *            the instruction visitor
     */
    protected void initializeBindingFrom(Variable<? extends EnvironmentRecord> targetEnvRec,
            Variable<? extends EnvironmentRecord> sourceEnvRec, Name name, boolean strict,
            InstructionVisitor mv) {
        mv.load(targetEnvRec);
        mv.aconst(name.getIdentifier());
        {
            mv.load(sourceEnvRec);
            mv.aconst(name.getIdentifier());
            mv.iconst(strict);
            mv.invoke(Methods.EnvironmentRecord_getBindingValue);
        }
        mv.invoke(Methods.EnvironmentRecord_initializeBinding);
    }

    /**
     * Emit function call for: {@link EnvironmentRecord#setMutableBinding(String, Object, boolean)}
     * <p>
     * stack: [obj] {@literal ->} []
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param name
     *            the binding name
     * @param strict
     *            the strict-mode flag
     * @param mv
     *            the instruction visitor
     */
    protected void setMutableBinding(Variable<? extends EnvironmentRecord> envRec, Name name,
            Variable<?> value, boolean strict, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.load(value);
        mv.iconst(strict);
        mv.invoke(Methods.EnvironmentRecord_setMutableBinding);
    }

    /**
     * Emit function call for: {@link LexicalEnvironment#getEnvRec()}
     * <p>
     * stack: [] {@literal ->} [envRec]
     * 
     * @param env
     *            the variable which holds the lexical environment
     * @param mv
     *            the instruction visitor
     */
    protected void getEnvironmentRecord(Variable<LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
    }

    /**
     * Emit function call for: {@link LexicalEnvironment#getEnvRec()}
     * <p>
     * stack: [env] {@literal ->} [envRec]
     * 
     * @param mv
     *            the instruction visitor
     */
    protected void getEnvironmentRecord(InstructionVisitor mv) {
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
    }

    /**
     * Emit runtime call to initialize the function object.
     * <p>
     * stack: [] {@literal ->} [fo]
     * 
     * @param context
     *            the variable which holds the execution context
     * @param env
     *            the variable which holds the lexical environment
     * @param f
     *            the function declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    protected void InstantiateFunctionObject(Variable<ExecutionContext> context,
            Variable<LexicalEnvironment<?>> env, Declaration f, InstructionVisitor mv) {
        if (f instanceof FunctionDeclaration) {
            InstantiateFunctionObject(context, env, (FunctionDeclaration) f, mv);
        } else if (f instanceof GeneratorDeclaration) {
            InstantiateGeneratorObject(context, env, (GeneratorDeclaration) f, mv);
        } else {
            InstantiateAsyncFunctionObject(context, env, (AsyncFunctionDeclaration) f, mv);
        }
    }

    /**
     * Emit runtime call to initialize the function object.
     * <p>
     * stack: [env, cx] {@literal ->} [fo]
     * 
     * @param f
     *            the function declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    protected void InstantiateFunctionObject(Declaration f, InstructionVisitor mv) {
        if (f instanceof FunctionDeclaration) {
            InstantiateFunctionObject((FunctionDeclaration) f, mv);
        } else if (f instanceof GeneratorDeclaration) {
            InstantiateGeneratorObject((GeneratorDeclaration) f, mv);
        } else {
            InstantiateAsyncFunctionObject((AsyncFunctionDeclaration) f, mv);
        }
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateAsyncFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [] {@literal ->} [fo]
     * 
     * @param context
     *            the variable which holds the execution context
     * @param env
     *            the variable which holds the lexical environment
     * @param f
     *            the function declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    private void InstantiateAsyncFunctionObject(Variable<ExecutionContext> context,
            Variable<LexicalEnvironment<?>> env, AsyncFunctionDeclaration f, InstructionVisitor mv) {
        mv.load(env);
        mv.load(context);

        InstantiateAsyncFunctionObject(f, mv);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateAsyncFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [env, cx] {@literal ->} [fo]
     * 
     * @param f
     *            the function declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    private void InstantiateAsyncFunctionObject(AsyncFunctionDeclaration f, InstructionVisitor mv) {
        codegen.compile(f);

        mv.invoke(codegen.methodDesc(f, FunctionName.RTI));
        mv.invoke(Methods.ScriptRuntime_InstantiateAsyncFunctionObject);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [] {@literal ->} [fo]
     * 
     * @param context
     *            the variable which holds the execution context
     * @param env
     *            the variable which holds the lexical environment
     * @param f
     *            the function declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    private void InstantiateFunctionObject(Variable<ExecutionContext> context,
            Variable<LexicalEnvironment<?>> env, FunctionDeclaration f, InstructionVisitor mv) {
        mv.load(env);
        mv.load(context);

        InstantiateFunctionObject(f, mv);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [env, cx] {@literal ->} [fo]
     * 
     * @param f
     *            the function declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    private void InstantiateFunctionObject(FunctionDeclaration f, InstructionVisitor mv) {
        codegen.compile(f);

        mv.invoke(codegen.methodDesc(f, FunctionName.RTI));
        mv.invoke(Methods.ScriptRuntime_InstantiateFunctionObject);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateGeneratorObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [] {@literal ->} [fo]
     * 
     * @param context
     *            the variable which holds the execution context
     * @param env
     *            the variable which holds the lexical environment
     * @param f
     *            the generator declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    private void InstantiateGeneratorObject(Variable<ExecutionContext> context,
            Variable<LexicalEnvironment<?>> env, GeneratorDeclaration f, InstructionVisitor mv) {
        mv.load(env);
        mv.load(context);

        InstantiateGeneratorObject(f, mv);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateGeneratorObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
     * <p>
     * stack: [env, cx] {@literal ->} [fo]
     * 
     * @param f
     *            the generator declaration to instantiate
     * @param mv
     *            the instruction visitor
     */
    private void InstantiateGeneratorObject(GeneratorDeclaration f, InstructionVisitor mv) {
        codegen.compile(f);

        mv.invoke(codegen.methodDesc(f, FunctionName.RTI));
        if (!(f instanceof LegacyGeneratorDeclaration)) {
            mv.invoke(Methods.ScriptRuntime_InstantiateGeneratorObject);
        } else {
            mv.invoke(Methods.ScriptRuntime_InstantiateLegacyGeneratorObject);
        }
    }

    /**
     * Returns the bound name of the declaration {@code d}, which must be either a function,
     * generator or async function declaration. The bound name of function declaration node is its
     * function name.
     * 
     * @param d
     *            the function declaration node
     * @return the bound name of the function declaration
     */
    protected static Name BoundName(Declaration d) {
        return getFunctionName(d).getName();
    }

    /**
     * Returns the function name of the declaration {@code d}, which must be either a function,
     * generator or async function declaration.
     * 
     * @param d
     *            the function declaration node
     * @return the function name of the function declaration
     */
    protected static BindingIdentifier getFunctionName(Declaration d) {
        if (d instanceof FunctionDeclaration) {
            return ((FunctionDeclaration) d).getIdentifier();
        } else if (d instanceof GeneratorDeclaration) {
            return ((GeneratorDeclaration) d).getIdentifier();
        } else {
            assert d instanceof AsyncFunctionDeclaration;
            return ((AsyncFunctionDeclaration) d).getIdentifier();
        }
    }

    /**
     * Returns {@code true} if {@code d} is either a function, generator or async function
     * declaration.
     * 
     * @param item
     *            the statement list node
     * @return {@code true} if the declaration is a function declaration
     */
    protected static boolean isFunctionDeclaration(StatementListItem item) {
        return item instanceof FunctionDeclaration || item instanceof GeneratorDeclaration
                || item instanceof AsyncFunctionDeclaration;
    }

    /**
     * Returns {@code true} if {@code d} is either a function, generator or async function
     * declaration.
     * 
     * @param d
     *            the declaration node
     * @return {@code true} if the declaration is a function declaration
     */
    protected static boolean isFunctionDeclaration(Declaration d) {
        return d instanceof FunctionDeclaration || d instanceof GeneratorDeclaration
                || d instanceof AsyncFunctionDeclaration;
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
