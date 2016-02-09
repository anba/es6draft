/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import static com.github.anba.es6draft.semantics.StaticSemantics.IsStrict;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.github.anba.es6draft.ast.AsyncFunctionDeclaration;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
import com.github.anba.es6draft.ast.LegacyGeneratorDeclaration;
import com.github.anba.es6draft.ast.Node;
import com.github.anba.es6draft.ast.VariableDeclaration;
import com.github.anba.es6draft.ast.scope.Name;
import com.github.anba.es6draft.compiler.assembler.Jump;
import com.github.anba.es6draft.compiler.assembler.MethodName;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.assembler.Variable;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.internal.ScriptRuntime;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * Base class for Binding Instantiation generators
 */
class DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: ExecutionContext
        static final MethodName ExecutionContext_getLexicalEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "getLexicalEnvironment",
                Type.methodType(Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getVariableEnvironment = MethodName.findVirtual(
                Types.ExecutionContext, "getVariableEnvironment",
                Type.methodType(Types.LexicalEnvironment));

        // class: GlobalEnvironmentRecord
        static final MethodName GlobalEnvironmentRecord_createGlobalVarBinding = MethodName
                .findVirtual(Types.GlobalEnvironmentRecord, "createGlobalVarBinding",
                        Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName GlobalEnvironmentRecord_createGlobalFunctionBinding = MethodName
                .findVirtual(Types.GlobalEnvironmentRecord, "createGlobalFunctionBinding", Type
                        .methodType(Type.VOID_TYPE, Types.String, Types.Object, Type.BOOLEAN_TYPE));

        static final MethodName GlobalEnvironmentRecord_canDeclareGlobalFunction = MethodName.findVirtual(
                Types.GlobalEnvironmentRecord, "canDeclareGlobalFunction",
                Type.methodType(Type.BOOLEAN_TYPE, Types.String));

        static final MethodName GlobalEnvironmentRecord_hasLexicalDeclaration = MethodName.findVirtual(
                Types.GlobalEnvironmentRecord, "hasLexicalDeclaration",
                Type.methodType(Type.BOOLEAN_TYPE, Types.String));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getEnvRec = MethodName.findVirtual(
                Types.LexicalEnvironment, "getEnvRec", Type.methodType(Types.EnvironmentRecord));

        // class: ScriptRuntime
        static final MethodName ScriptRuntime_canDeclareGlobalFunctionOrThrow = MethodName
                .findStatic(Types.ScriptRuntime, "canDeclareGlobalFunctionOrThrow", Type
                        .methodType(Type.VOID_TYPE, Types.ExecutionContext,
                                Types.GlobalEnvironmentRecord, Types.String));

        static final MethodName ScriptRuntime_canDeclareGlobalVarOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "canDeclareGlobalVarOrThrow", Type.methodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.GlobalEnvironmentRecord, Types.String));

        static final MethodName ScriptRuntime_canDeclareLexicalScopedOrThrow = MethodName
                .findStatic(Types.ScriptRuntime, "canDeclareLexicalScopedOrThrow", Type.methodType(
                        Type.VOID_TYPE, Types.ExecutionContext, Types.GlobalEnvironmentRecord,
                        Types.String));

        static final MethodName ScriptRuntime_canDeclareVarScopedOrThrow = MethodName.findStatic(
                Types.ScriptRuntime, "canDeclareVarScopedOrThrow", Type.methodType(Type.VOID_TYPE,
                        Types.ExecutionContext, Types.GlobalEnvironmentRecord, Types.String));

        static final MethodName ScriptRuntime_canDeclareVarBinding = MethodName.findStatic(Types.ScriptRuntime,
                "canDeclareVarBinding", Type.methodType(Type.BOOLEAN_TYPE, Types.LexicalEnvironment,
                        Types.LexicalEnvironment, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName ScriptRuntime_setLegacyBlockFunction = MethodName.findStatic(Types.ScriptRuntime,
                "setLegacyBlockFunction", Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Type.INT_TYPE));

        static final MethodName ScriptRuntime_InstantiateAsyncFunctionObject = MethodName
                .findStatic(Types.ScriptRuntime, "InstantiateAsyncFunctionObject", Type.methodType(
                        Types.OrdinaryAsyncFunction, Types.LexicalEnvironment,
                        Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodName ScriptRuntime_InstantiateFunctionObject = MethodName.findStatic(
                Types.ScriptRuntime, "InstantiateFunctionObject", Type.methodType(
                        Types.OrdinaryConstructorFunction, Types.LexicalEnvironment,
                        Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodName ScriptRuntime_InstantiateLegacyFunctionObject = MethodName.findStatic(
                Types.ScriptRuntime, "InstantiateLegacyFunctionObject", Type.methodType(Types.LegacyConstructorFunction,
                        Types.LexicalEnvironment, Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodName ScriptRuntime_InstantiateConstructorGeneratorObject = MethodName.findStatic(
                Types.ScriptRuntime, "InstantiateConstructorGeneratorObject", Type.methodType(
                        Types.OrdinaryConstructorGenerator, Types.LexicalEnvironment, Types.ExecutionContext,
                        Types.RuntimeInfo$Function));

        static final MethodName ScriptRuntime_InstantiateGeneratorObject = MethodName.findStatic(Types.ScriptRuntime,
                "InstantiateGeneratorObject", Type.methodType(Types.OrdinaryGenerator, Types.LexicalEnvironment,
                        Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodName ScriptRuntime_InstantiateLegacyGeneratorObject = MethodName
                .findStatic(Types.ScriptRuntime, "InstantiateLegacyGeneratorObject", Type
                        .methodType(Types.OrdinaryConstructorGenerator, Types.LexicalEnvironment,
                                Types.ExecutionContext, Types.RuntimeInfo$Function));
    }

    protected final CodeGenerator codegen;

    protected DeclarationBindingInstantiationGenerator(CodeGenerator codegen) {
        this.codegen = codegen;
    }

    /**
     * Emit function call for: {@link ExecutionContext#getLexicalEnvironment()}
     * 
     * @param context
     *            the variable which holds the execution context
     * @param env
     *            the variable which holds the lexical environment
     * @param mv
     *            the instruction visitor
     */
    protected final void getLexicalEnvironment(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(context);
        mv.invoke(Methods.ExecutionContext_getLexicalEnvironment);
        mv.store(env);
    }

    /**
     * Emit function call for: {@link ExecutionContext#getVariableEnvironment()}
     * 
     * @param context
     *            the variable which holds the execution context
     * @param env
     *            the variable which holds the lexical environment
     * @param mv
     *            the instruction visitor
     */
    protected final void getVariableEnvironment(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, InstructionVisitor mv) {
        mv.load(context);
        mv.invoke(Methods.ExecutionContext_getVariableEnvironment);
        mv.store(env);
    }

    /**
     * Emit function call for: {@link LexicalEnvironment#getEnvRec()}
     * 
     * @param env
     *            the variable which holds the lexical environment
     * @param envRec
     *            the variable which holds the environment record
     * @param mv
     *            the instruction visitor
     */
    protected final <R extends EnvironmentRecord, R2 extends R, R3 extends R2> void getEnvironmentRecord(
            Variable<? extends LexicalEnvironment<? extends R2>> env, Variable<R3> envRec,
            InstructionVisitor mv) {
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
        if (envRec.getType() != Types.EnvironmentRecord) {
            mv.checkcast(envRec.getType());
        }
        mv.store(envRec);
    }

    /**
     * Emit: {@code ScriptRuntime.canDeclareLexicalScopedOrThrow(cx, envRec, name)}
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the declaration node
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected final void canDeclareLexicalScopedOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, Node node, Name name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_canDeclareLexicalScopedOrThrow);
    }

    /**
     * Emit: {@code ScriptRuntime.canDeclareVarScopedOrThrow(cx, envRec, name)}
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the declaration node
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected final void canDeclareVarScopedOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, Node node, Name name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_canDeclareVarScopedOrThrow);
    }

    /**
     * Emit: {@code ScriptRuntime.canDeclareGlobalFunctionOrThrow(cx, envRec, name)}
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the function node
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected final void canDeclareGlobalFunctionOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, HoistableDeclaration node, Name name,
            InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_canDeclareGlobalFunctionOrThrow);
    }

    /**
     * Emit: {@code ScriptRuntime.canDeclareGlobalVarOrThrow(cx, envRec, name)}
     * 
     * @param context
     *            the variable which holds the execution context
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the variable declaration node
     * @param name
     *            the binding name
     * @param mv
     *            the instruction visitor
     */
    protected final void canDeclareGlobalVarOrThrow(Variable<ExecutionContext> context,
            Variable<GlobalEnvironmentRecord> envRec, VariableDeclaration node, Name name,
            InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.lineInfo(node);
        mv.invoke(Methods.ScriptRuntime_canDeclareGlobalVarOrThrow);
    }

    /**
     * Emit: {@code envRec.createGlobalVarBinding(name, deletableBindings)}
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the variable declaration node
     * @param name
     *            the binding name
     * @param deletableBindings
     *            the variable which holds the deletable flag
     * @param mv
     *            the instruction visitor
     */
    protected final void createGlobalVarBinding(Variable<GlobalEnvironmentRecord> envRec,
            VariableDeclaration node, Name name, boolean deletableBindings, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.iconst(deletableBindings);
        mv.lineInfo(node);
        mv.invoke(Methods.GlobalEnvironmentRecord_createGlobalVarBinding);
    }

    /**
     * Emit: {@code envRec.createGlobalFunctionBinding(name, functionObject, deletableBindings)}
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the function node
     * @param name
     *            the binding name
     * @param fo
     *            the function object
     * @param deletableBindings
     *            the variable which holds the deletable flag
     * @param mv
     *            the instruction visitor
     */
    protected final void createGlobalFunctionBinding(Variable<GlobalEnvironmentRecord> envRec,
            HoistableDeclaration node, Name name, Variable<FunctionObject> fo,
            boolean deletableBindings, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.load(fo);
        mv.iconst(deletableBindings);
        mv.lineInfo(node);
        mv.invoke(Methods.GlobalEnvironmentRecord_createGlobalFunctionBinding);
    }

    /**
     * Emit: {@code envRec.createGlobalFunctionBinding(name, undefined, deletableBindings)}
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the function node
     * @param name
     *            the binding name
     * @param deletableBindings
     *            the variable which holds the deletable flag
     * @param mv
     *            the instruction visitor
     */
    protected final void createGlobalFunctionBinding(Variable<GlobalEnvironmentRecord> envRec,
            HoistableDeclaration node, Name name, boolean deletableBindings, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.loadUndefined();
        mv.iconst(deletableBindings);
        mv.lineInfo(node);
        mv.invoke(Methods.GlobalEnvironmentRecord_createGlobalFunctionBinding);
    }

    /**
     * Emit: {@code !envRec.hasLexicalDeclaration(name) && envRec.canDeclareGlobalFunction}
     * 
     * @param envRec
     *            the variable which holds the environment record
     * @param node
     *            the function node
     * @param name
     *            the binding name
     * @param next
     *            the next instruction
     * @param mv
     *            the instruction visitor
     */
    protected final void canDeclareGlobalFunction(Variable<GlobalEnvironmentRecord> envRec,
            HoistableDeclaration node, Name name, Jump next, InstructionVisitor mv) {
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.invoke(Methods.GlobalEnvironmentRecord_hasLexicalDeclaration);
        mv.ifne(next);
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.lineInfo(node);
        mv.invoke(Methods.GlobalEnvironmentRecord_canDeclareGlobalFunction);
        mv.ifeq(next);
    }

    /**
     * Emit: {@code ScriptRuntime.canDeclareVarBinding(varEnv, lexEnv, name)}
     * 
     * @param <R>
     *            the environment record type
     * @param varEnv
     *            the variable environment
     * @param lexEnv
     *            the lexical environment
     * @param name
     *            the binding name
     * @param catchVar
     *            the {@code catchVar} flag
     * @param next
     *            the next instruction
     * @param mv
     *            the instruction visitor
     */
    protected final <R extends EnvironmentRecord> void canDeclareVarBinding(Variable<LexicalEnvironment<R>> varEnv,
            Variable<LexicalEnvironment<DeclarativeEnvironmentRecord>> lexEnv, Name name, boolean catchVar, Jump next,
            InstructionVisitor mv) {
        mv.load(varEnv);
        mv.load(lexEnv);
        mv.aconst(name.getIdentifier());
        mv.iconst(catchVar);
        mv.invoke(Methods.ScriptRuntime_canDeclareVarBinding);
        mv.ifeq(next);
    }

    /**
     * Emit: {@code ScriptRuntime.setScriptBlockFunction(cx, functionId)}
     * 
     * @param context
     *            the variable which holds the execution context
     * @param function
     *            the function declaration
     * @param mv
     *            the instruction visitor
     */
    protected final void setLegacyBlockFunction(Variable<ExecutionContext> context, FunctionDeclaration function,
            InstructionVisitor mv) {
        mv.load(context);
        mv.iconst(function.getLegacyBlockScopeId());
        mv.invoke(Methods.ScriptRuntime_setLegacyBlockFunction);
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
    protected final void InstantiateFunctionObject(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, Declaration f, InstructionVisitor mv) {
        if (f instanceof FunctionDeclaration) {
            if (isLegacy((FunctionDeclaration) f)) {
                InstantiateLegacyFunctionObject(context, env, (FunctionDeclaration) f, mv);
            } else {
                InstantiateFunctionObject(context, env, (FunctionDeclaration) f, mv);
            }
        } else if (f instanceof GeneratorDeclaration) {
            InstantiateGeneratorObject(context, env, (GeneratorDeclaration) f, mv);
        } else {
            InstantiateAsyncFunctionObject(context, env, (AsyncFunctionDeclaration) f, mv);
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
            Variable<? extends LexicalEnvironment<?>> env, AsyncFunctionDeclaration f,
            InstructionVisitor mv) {
        MethodName method = codegen.compile(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
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
            Variable<? extends LexicalEnvironment<?>> env, FunctionDeclaration f,
            InstructionVisitor mv) {
        MethodName method = codegen.compile(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        mv.invoke(Methods.ScriptRuntime_InstantiateFunctionObject);
    }

    /**
     * Emit function call for:
     * {@link ScriptRuntime#InstantiateLegacyFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
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
    private void InstantiateLegacyFunctionObject(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, FunctionDeclaration f, InstructionVisitor mv) {
        MethodName method = codegen.compile(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        mv.invoke(Methods.ScriptRuntime_InstantiateLegacyFunctionObject);
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
            Variable<? extends LexicalEnvironment<?>> env, GeneratorDeclaration f,
            InstructionVisitor mv) {
        MethodName method = codegen.compile(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        if (f instanceof LegacyGeneratorDeclaration) {
            mv.invoke(Methods.ScriptRuntime_InstantiateLegacyGeneratorObject);
        } else if (f.isConstructor()) {
            mv.invoke(Methods.ScriptRuntime_InstantiateConstructorGeneratorObject);
        } else {
            mv.invoke(Methods.ScriptRuntime_InstantiateGeneratorObject);
        }
    }

    private boolean isLegacy(FunctionDeclaration node) {
        if (IsStrict(node)) {
            return false;
        }
        return codegen.isEnabled(CompatibilityOption.FunctionArguments)
                || codegen.isEnabled(CompatibilityOption.FunctionCaller);
    }

    protected static final <T> Iterable<T> reverse(List<T> list) {
        return () -> new Iterator<T>() {
            final ListIterator<T> iter = list.listIterator(list.size());

            @Override
            public boolean hasNext() {
                return iter.hasPrevious();
            }

            @Override
            public T next() {
                return iter.previous();
            }
        };
    }
}
