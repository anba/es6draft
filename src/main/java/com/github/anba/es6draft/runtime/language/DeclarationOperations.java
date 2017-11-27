/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import static com.github.anba.es6draft.runtime.internal.Errors.newSyntaxError;
import static com.github.anba.es6draft.runtime.internal.Errors.newTypeError;

import com.github.anba.es6draft.Executable;
import com.github.anba.es6draft.compiler.CompiledScript;
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.EnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ObjectEnvironmentRecord;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 *
 */
public final class DeclarationOperations {
    private DeclarationOperations() {
    }

    /**
     * 18.2.1.2 EvalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     */
    public static void bindingNotPresentOrThrow(ExecutionContext cx, EnvironmentRecord envRec, String name) {
        if (envRec.hasBinding(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     */
    public static void canDeclareLexicalScopedOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec,
            String name) {
        /* step 5.a */
        if (envRec.hasVarDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
        /* step 5.b */
        if (envRec.hasLexicalDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
        /* step 5.c */
        if (envRec.hasRestrictedGlobalProperty(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the binding name
     */
    public static void canDeclareVarScopedOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec, String name) {
        /* step 6.a */
        if (envRec.hasLexicalDeclaration(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param fn
     *            the function name
     */
    public static void canDeclareGlobalFunctionOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec, String fn) {
        /* steps 10.a.iv.1-2 */
        boolean fnDefinable = envRec.canDeclareGlobalFunction(fn);
        if (!fnDefinable) {
            throw newTypeError(cx, Messages.Key.InvalidDeclaration, fn);
        }
    }

    /**
     * 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param vn
     *            the variable name
     */
    public static void canDeclareGlobalVarOrThrow(ExecutionContext cx, GlobalEnvironmentRecord envRec, String vn) {
        /* steps 12.a.i.1.a-c */
        boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
        if (!vnDefinable) {
            throw newTypeError(cx, Messages.Key.InvalidDeclaration, vn);
        }
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the variable name
     */
    public static void canDeclareVarOrThrow(ExecutionContext cx, DeclarativeEnvironmentRecord envRec, String name) {
        /* steps 6.b.ii.2-3 */
        if (envRec.hasBinding(name)) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param envRec
     *            the environment record
     * @param name
     *            the variable name
     * @param catchVar
     *            {@code true} if variable redeclarations are allowed in catch clauses
     */
    public static void canDeclareVarOrThrow(ExecutionContext cx, DeclarativeEnvironmentRecord envRec, String name,
            boolean catchVar) {
        /* steps 6.b.ii.2-3 */
        if (envRec.hasBinding(name) && !(catchVar && envRec.isCatchEnvironment())) {
            throw newSyntaxError(cx, Messages.Key.VariableRedeclaration, name);
        }
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param varEnv
     *            the variable environment
     * @param lexEnv
     *            the lexical environment
     * @param name
     *            the function name
     * @param catchVar
     *            {@code true} if variable redeclarations are allowed in catch clauses
     * @return {@code true} if the name can be declared
     */
    public static boolean canDeclareVarBinding(LexicalEnvironment<?> varEnv,
            LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv, String name, boolean catchVar) {
        for (LexicalEnvironment<?> thisEnv = lexEnv; thisEnv != varEnv; thisEnv = thisEnv.getOuter()) {
            EnvironmentRecord thisEnvRec = thisEnv.getEnvRec();
            if (thisEnvRec instanceof ObjectEnvironmentRecord) {
                continue;
            }
            DeclarativeEnvironmentRecord declEnvRec = (DeclarativeEnvironmentRecord) thisEnvRec;
            if (declEnvRec.hasBinding(name) && !(catchVar && declEnvRec.isCatchEnvironment())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param functionId
     *            the function id
     */
    public static void setLegacyBlockFunction(ExecutionContext cx, int functionId) {
        Executable executable = cx.getCurrentExecutable();
        ((CompiledScript) executable).setLegacyBlockFunction(functionId);
    }

    /**
     * 18.2.1.2 Runtime Semantics: EvalDeclarationInstantiation( body, varEnv, lexEnv, strict)
     * 
     * @param cx
     *            the execution context
     * @param functionId
     *            the function id
     * @return {@code true} if the function is legacy block-level scoped
     */
    public static boolean isLegacyBlockFunction(ExecutionContext cx, int functionId) {
        Executable executable = cx.getCurrentExecutable();
        return ((CompiledScript) executable).isLegacyBlockFunction(functionId);
    }

    /**
     * Extension: Class Fields
     * 
     * @param cx
     *            the execution context
     * @param name
     *            the binding name
     */
    public static void hasDeclaredPrivateNameOrThrow(ExecutionContext cx, String name) {
        for (LexicalEnvironment<?> env = cx.getLexicalEnvironment(); env != null; env = env.getOuter()) {
            EnvironmentRecord envRec = env.getEnvRec();
            if (!(envRec instanceof DeclarativeEnvironmentRecord)) {
                continue;
            }
            DeclarativeEnvironmentRecord declEnvRev = (DeclarativeEnvironmentRecord) envRec;
            if (declEnvRev.hasBinding(name)) {
                return;
            }
        }
        throw newSyntaxError(cx, Messages.Key.UndeclaredPrivateName, name);
    }
}
