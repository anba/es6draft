/**
 * Copyright (c) André Bargull
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
import com.github.anba.es6draft.ast.AsyncGeneratorDeclaration;
import com.github.anba.es6draft.ast.Declaration;
import com.github.anba.es6draft.ast.FunctionDeclaration;
import com.github.anba.es6draft.ast.GeneratorDeclaration;
import com.github.anba.es6draft.ast.HoistableDeclaration;
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
import com.github.anba.es6draft.runtime.language.FunctionOperations;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * Base class for Binding Instantiation generators
 */
class DeclarationBindingInstantiationGenerator {
    private static final class Methods {
        // class: ExecutionContext
        static final MethodName ExecutionContext_getLexicalEnvironment = MethodName.findVirtual(Types.ExecutionContext,
                "getLexicalEnvironment", Type.methodType(Types.LexicalEnvironment));

        static final MethodName ExecutionContext_getVariableEnvironment = MethodName.findVirtual(Types.ExecutionContext,
                "getVariableEnvironment", Type.methodType(Types.LexicalEnvironment));

        // class: GlobalEnvironmentRecord
        static final MethodName GlobalEnvironmentRecord_createGlobalVarBinding = MethodName.findVirtual(
                Types.GlobalEnvironmentRecord, "createGlobalVarBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName GlobalEnvironmentRecord_createGlobalFunctionBinding = MethodName.findVirtual(
                Types.GlobalEnvironmentRecord, "createGlobalFunctionBinding",
                Type.methodType(Type.VOID_TYPE, Types.String, Types.Object, Type.BOOLEAN_TYPE));

        static final MethodName GlobalEnvironmentRecord_canDeclareGlobalFunction = MethodName.findVirtual(
                Types.GlobalEnvironmentRecord, "canDeclareGlobalFunction",
                Type.methodType(Type.BOOLEAN_TYPE, Types.String));

        static final MethodName GlobalEnvironmentRecord_hasLexicalDeclaration = MethodName.findVirtual(
                Types.GlobalEnvironmentRecord, "hasLexicalDeclaration",
                Type.methodType(Type.BOOLEAN_TYPE, Types.String));

        // class: LexicalEnvironment
        static final MethodName LexicalEnvironment_getEnvRec = MethodName.findVirtual(Types.LexicalEnvironment,
                "getEnvRec", Type.methodType(Types.EnvironmentRecord));

        // class: DeclarationOperations
        static final MethodName DeclarationOperations_canDeclareGlobalFunctionOrThrow = MethodName.findStatic(
                Types.DeclarationOperations, "canDeclareGlobalFunctionOrThrow",
                Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Types.GlobalEnvironmentRecord, Types.String));

        static final MethodName DeclarationOperations_canDeclareGlobalVarOrThrow = MethodName.findStatic(
                Types.DeclarationOperations, "canDeclareGlobalVarOrThrow",
                Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Types.GlobalEnvironmentRecord, Types.String));

        static final MethodName DeclarationOperations_canDeclareLexicalScopedOrThrow = MethodName.findStatic(
                Types.DeclarationOperations, "canDeclareLexicalScopedOrThrow",
                Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Types.GlobalEnvironmentRecord, Types.String));

        static final MethodName DeclarationOperations_canDeclareVarScopedOrThrow = MethodName.findStatic(
                Types.DeclarationOperations, "canDeclareVarScopedOrThrow",
                Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Types.GlobalEnvironmentRecord, Types.String));

        static final MethodName DeclarationOperations_canDeclareVarBinding = MethodName
                .findStatic(Types.DeclarationOperations, "canDeclareVarBinding", Type.methodType(Type.BOOLEAN_TYPE,
                        Types.LexicalEnvironment, Types.LexicalEnvironment, Types.String, Type.BOOLEAN_TYPE));

        static final MethodName DeclarationOperations_setLegacyBlockFunction = MethodName.findStatic(
                Types.DeclarationOperations, "setLegacyBlockFunction",
                Type.methodType(Type.VOID_TYPE, Types.ExecutionContext, Type.INT_TYPE));

        // class: FunctionOperations
        static final MethodName FunctionOperations_InstantiateAsyncFunctionObject = MethodName.findStatic(
                Types.FunctionOperations, "InstantiateAsyncFunctionObject", Type.methodType(Types.OrdinaryAsyncFunction,
                        Types.LexicalEnvironment, Types.ExecutionContext, Types.RuntimeInfo$Function));

        static final MethodName FunctionOperations_InstantiateAsyncGeneratorObject = MethodName.findStatic(
                Types.FunctionOperations, "InstantiateAsyncGeneratorObject",
                Type.methodType(Types.OrdinaryAsyncGenerator, Types.LexicalEnvironment, Types.ExecutionContext,
                        Types.RuntimeInfo$Function));

        static final MethodName FunctionOperations_InstantiateFunctionObject = MethodName.findStatic(
                Types.FunctionOperations, "InstantiateFunctionObject",
                Type.methodType(Types.OrdinaryConstructorFunction, Types.LexicalEnvironment, Types.ExecutionContext,
                        Types.RuntimeInfo$Function));

        static final MethodName FunctionOperations_InstantiateLegacyFunctionObject = MethodName.findStatic(
                Types.FunctionOperations, "InstantiateLegacyFunctionObject",
                Type.methodType(Types.LegacyConstructorFunction, Types.LexicalEnvironment, Types.ExecutionContext,
                        Types.RuntimeInfo$Function));

        static final MethodName FunctionOperations_InstantiateGeneratorObject = MethodName.findStatic(
                Types.FunctionOperations, "InstantiateGeneratorObject", Type.methodType(Types.OrdinaryGenerator,
                        Types.LexicalEnvironment, Types.ExecutionContext, Types.RuntimeInfo$Function));
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
            Variable<? extends LexicalEnvironment<? extends R2>> env, Variable<R3> envRec, InstructionVisitor mv) {
        mv.load(env);
        mv.invoke(Methods.LexicalEnvironment_getEnvRec);
        if (envRec.getType() != Types.EnvironmentRecord) {
            mv.checkcast(envRec.getType());
        }
        mv.store(envRec);
    }

    /**
     * Emit: {@code DeclarationOperations.canDeclareLexicalScopedOrThrow(cx, envRec, name)}
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
        mv.invoke(Methods.DeclarationOperations_canDeclareLexicalScopedOrThrow);
    }

    /**
     * Emit: {@code DeclarationOperations.canDeclareVarScopedOrThrow(cx, envRec, name)}
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
        mv.invoke(Methods.DeclarationOperations_canDeclareVarScopedOrThrow);
    }

    /**
     * Emit: {@code DeclarationOperations.canDeclareGlobalFunctionOrThrow(cx, envRec, name)}
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
            Variable<GlobalEnvironmentRecord> envRec, HoistableDeclaration node, Name name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.lineInfo(node);
        mv.invoke(Methods.DeclarationOperations_canDeclareGlobalFunctionOrThrow);
    }

    /**
     * Emit: {@code DeclarationOperations.canDeclareGlobalVarOrThrow(cx, envRec, name)}
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
            Variable<GlobalEnvironmentRecord> envRec, VariableDeclaration node, Name name, InstructionVisitor mv) {
        mv.load(context);
        mv.load(envRec);
        mv.aconst(name.getIdentifier());
        mv.lineInfo(node);
        mv.invoke(Methods.DeclarationOperations_canDeclareGlobalVarOrThrow);
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
    protected final void createGlobalVarBinding(Variable<GlobalEnvironmentRecord> envRec, VariableDeclaration node,
            Name name, boolean deletableBindings, InstructionVisitor mv) {
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
            HoistableDeclaration node, Name name, Variable<FunctionObject> fo, boolean deletableBindings,
            InstructionVisitor mv) {
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
    protected final void canDeclareGlobalFunction(Variable<GlobalEnvironmentRecord> envRec, HoistableDeclaration node,
            Name name, Jump next, InstructionVisitor mv) {
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
     * Emit: {@code DeclarationOperations.canDeclareVarBinding(varEnv, lexEnv, name)}
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
        mv.invoke(Methods.DeclarationOperations_canDeclareVarBinding);
        mv.ifeq(next);
    }

    /**
     * Emit: {@code DeclarationOperations.setScriptBlockFunction(cx, functionId)}
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
        mv.invoke(Methods.DeclarationOperations_setLegacyBlockFunction);
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
        } else if (f instanceof AsyncFunctionDeclaration) {
            InstantiateAsyncFunctionObject(context, env, (AsyncFunctionDeclaration) f, mv);
        } else {
            InstantiateAsyncGeneratorObject(context, env, (AsyncGeneratorDeclaration) f, mv);
        }
    }

    /**
     * Emit function call for:
     * {@link FunctionOperations#InstantiateAsyncFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
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
            Variable<? extends LexicalEnvironment<?>> env, AsyncFunctionDeclaration f, InstructionVisitor mv) {
        MethodName method = codegen.asyncFunctionDefinition(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        mv.invoke(Methods.FunctionOperations_InstantiateAsyncFunctionObject);
    }

    /**
     * Emit function call for:
     * {@link FunctionOperations#InstantiateAsyncGeneratorObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
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
    private void InstantiateAsyncGeneratorObject(Variable<ExecutionContext> context,
            Variable<? extends LexicalEnvironment<?>> env, AsyncGeneratorDeclaration f, InstructionVisitor mv) {
        MethodName method = codegen.asyncGeneratorDefinition(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        mv.invoke(Methods.FunctionOperations_InstantiateAsyncGeneratorObject);
    }

    /**
     * Emit function call for:
     * {@link FunctionOperations#InstantiateFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
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
            Variable<? extends LexicalEnvironment<?>> env, FunctionDeclaration f, InstructionVisitor mv) {
        MethodName method = codegen.functionDefinition(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        mv.invoke(Methods.FunctionOperations_InstantiateFunctionObject);
    }

    /**
     * Emit function call for:
     * {@link FunctionOperations#InstantiateLegacyFunctionObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
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
        MethodName method = codegen.functionDefinition(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        mv.invoke(Methods.FunctionOperations_InstantiateLegacyFunctionObject);
    }

    /**
     * Emit function call for:
     * {@link FunctionOperations#InstantiateGeneratorObject(LexicalEnvironment, ExecutionContext, RuntimeInfo.Function)}
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
            Variable<? extends LexicalEnvironment<?>> env, GeneratorDeclaration f, InstructionVisitor mv) {
        MethodName method = codegen.generatorDefinition(f);

        mv.load(env);
        mv.load(context);
        mv.invoke(method);
        mv.invoke(Methods.FunctionOperations_InstantiateGeneratorObject);
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
