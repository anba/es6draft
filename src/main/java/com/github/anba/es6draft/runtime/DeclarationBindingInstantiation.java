/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime;

import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwSyntaxError;
import static com.github.anba.es6draft.runtime.internal.ScriptRuntime.throwTypeError;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.CompleteMappedArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.CompleteStrictArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.ExoticArguments.InstantiateArgumentsObject;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.InstantiateFunctionObject;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryGenerator.InstantiateGeneratorObject;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.github.anba.es6draft.runtime.internal.RuntimeInfo;
import com.github.anba.es6draft.runtime.types.Function;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;

/**
 * <h1>10 Executable Code and Execution Contexts</h1><br>
 * <h2>10.5 Declaration Binding Instantiation</h2>
 * <ul>
 * <li>10.5.1 Global Declaration Instantiation
 * <li>10.5.2 Module Declaration Instantiation (TODO)
 * <li>10.5.3 Function Declaration Instantiation
 * <li>10.5.4 Block Declaration Instantiation (in generated code)
 * <li>10.5.5 Eval Declaration Instantiation
 * </ul>
 */
public final class DeclarationBindingInstantiation {
    private DeclarationBindingInstantiation() {
    }

    /**
     * [10.5.1 Global Declaration Instantiation]
     */
    public static void GlobalDeclarationInstantiation(Realm realm, LexicalEnvironment globalEnv,
            RuntimeInfo.ScriptBody script, boolean deletableBindings) {
        LexicalEnvironment env = globalEnv;
        GlobalEnvironmentRecord envRec = (GlobalEnvironmentRecord) env.getEnvRec();

        /* [10.5.1] step 1 */
        @SuppressWarnings("unused")
        boolean strict = script.isStrict();
        /* [10.5.1] step 2 */
        String[] lexNames = script.lexicallyDeclaredNames();
        /* [10.5.1] step 3 */
        String[] varNames = script.varDeclaredNames();
        /* [10.5.1] step 4 */
        for (String name : lexNames) {
            if (envRec.hasVarDeclaration(name)) {
                throw throwSyntaxError(realm, String.format("re-declaration of var '%s'", name));
            }
            if (envRec.hasLexicalDeclaration(name)) {
                throw throwSyntaxError(realm, String.format("re-declaration of var '%s'", name));
            }
        }
        /* [10.5.1] step 5 */
        for (String name : varNames) {
            if (envRec.hasLexicalDeclaration(name)) {
                throw throwSyntaxError(realm, String.format("re-declaration of var '%s'", name));
            }
        }
        /* [10.5.1] step 6 */
        RuntimeInfo.Declaration[] varDeclarations = script.varScopedDeclarations();
        /* [10.5.1] step 7 */
        List<RuntimeInfo.Declaration> functionsToInitialize = new ArrayList<>();
        /* [10.5.1] step 8 */
        Set<String> declaredFunctionNames = new HashSet<>();
        /* [10.5.1] step 9 */
        for (RuntimeInfo.Declaration d : reverse(varDeclarations)) {
            if (d.isFunctionDeclaration()) {
                String fn = d.boundNames()[0];
                if (!declaredFunctionNames.contains(fn)) {
                    boolean fnDefinable = envRec.canDeclareGlobalFunction(fn);
                    if (!fnDefinable) {
                        throw throwTypeError(realm,
                                String.format("cannot declare function '%s'", fn));
                    }
                    declaredFunctionNames.add(fn);
                    functionsToInitialize.add(d);
                }
            }
        }
        /* [10.5.1] step 10 */
        Set<String> declaredVarNames = new HashSet<>();
        /* [10.5.1] step 11 */
        for (RuntimeInfo.Declaration d : varDeclarations) {
            if (d.isVariableStatement()) {
                for (String vn : d.boundNames()) {
                    if (!declaredFunctionNames.contains(vn)) {
                        boolean vnDefinable = envRec.canDeclareGlobalVar(vn);
                        if (!vnDefinable) {
                            throw throwTypeError(realm,
                                    String.format("cannot declare var '%s'", vn));
                        }
                        if (!declaredVarNames.contains(vn)) {
                            declaredVarNames.add(vn);
                        }
                    }
                }
            }
        }
        /* [10.5.1] step 12-13 */
        for (RuntimeInfo.Declaration f : functionsToInitialize) {
            String fn = f.boundNames()[0];
            RuntimeInfo.Function fd = script.getFunction(f);
            Function fo;
            if (fd.isGenerator()) {
                fo = InstantiateGeneratorObject(realm, env, fd);
            } else {
                fo = InstantiateFunctionObject(realm, env, fd);
            }
            envRec.createGlobalFunctionBinding(fn, fo, deletableBindings);
        }
        /* [10.5.1] step 14 */
        for (String vn : declaredVarNames) {
            envRec.createGlobalVarBinding(vn, deletableBindings);
        }
        /* [10.5.1] step 15 */
        RuntimeInfo.Declaration[] lexDeclarations = script.lexicallyScopedDeclarations();
        /* [10.5.1] step 16 */
        for (RuntimeInfo.Declaration d : lexDeclarations) {
            for (String dn : d.boundNames()) {
                if (d.isConstDeclaration()) {
                    envRec.createImmutableBinding(dn);
                } else {
                    envRec.createMutableBinding(dn, false);
                }
            }
        }
        /* [10.5.1] step 17 */
        return;
    }

    /**
     * [10.5.5 Eval Declaration Instantiation]
     */
    public static void EvalDeclarationInstantiation(Realm realm, LexicalEnvironment lexEnv,
            LexicalEnvironment varEnv, RuntimeInfo.ScriptBody script, boolean deletableBindings) {
        // FIXME: spec incomplete (using modified ES5.1 algorithm for now...)

        // begin-modification
        for (String name : script.lexicallyDeclaredNames()) {
            if (lexEnv.getEnvRec().hasBinding(name)) {
                throw throwSyntaxError(realm, String.format("re-declaration of var '%s'", name));
            }
        }
        // end-modification

        LexicalEnvironment env = varEnv;
        /* step 1 */
        EnvironmentRecord envRec = env.getEnvRec();
        /* step 2 (not applicable) */
        /* step 3 */
        boolean strict = script.isStrict();
        /* step 4 (not applicable) */
        /* step 5 */
        RuntimeInfo.Declaration[] varDeclarations = script.varScopedDeclarations();
        for (RuntimeInfo.Declaration d : varDeclarations) {
            if (d.isFunctionDeclaration()) {
                String fn = d.boundNames()[0];
                RuntimeInfo.Function fd = script.getFunction(d);
                Function fo;
                if (fd.isGenerator()) {
                    fo = InstantiateGeneratorObject(realm, env, fd);
                } else {
                    fo = InstantiateFunctionObject(realm, env, fd);
                }
                boolean funcAlreadyDeclared = envRec.hasBinding(fn);
                if (!funcAlreadyDeclared) {
                    envRec.createMutableBinding(fn, deletableBindings);
                } else {
                    // omitted
                }
                envRec.setMutableBinding(fn, fo, strict);
            }
        }
        /* step 6-7 (not applicable) */
        /* step 8 */
        for (RuntimeInfo.Declaration d : varDeclarations) {
            if (d.isVariableStatement()) {
                for (String dn : d.boundNames()) {
                    boolean varAlreadyDeclared = envRec.hasBinding(dn);
                    if (!varAlreadyDeclared) {
                        envRec.createMutableBinding(dn, deletableBindings);
                        envRec.setMutableBinding(dn, UNDEFINED, strict);
                    }
                }
            }
        }

        // begin-modification
        RuntimeInfo.Declaration[] lexDeclarations = script.lexicallyScopedDeclarations();
        for (RuntimeInfo.Declaration d : lexDeclarations) {
            for (String dn : d.boundNames()) {
                if (d.isConstDeclaration()) {
                    lexEnv.getEnvRec().createImmutableBinding(dn);
                } else {
                    lexEnv.getEnvRec().createMutableBinding(dn, false);
                }
            }
        }
        // end-modification
    }

    /**
     * [10.5.3 Function Declaration Instantiation]
     */
    public static void FunctionDeclarationInstantiation(ExecutionContext cx, Function func,
            Object... args) {
        assert cx.getVariableEnvironment() == cx.getLexicalEnvironment();
        Realm realm = cx.getRealm();
        LexicalEnvironment env = cx.getVariableEnvironment();
        EnvironmentRecord envRec = env.getEnvRec();
        /* [10.5.3] step 1 */
        RuntimeInfo.Code code = func.getCode();
        /* [10.5.3] step 2 */
        boolean strict = func.isStrict();
        /* [10.5.3] step 3 */
        RuntimeInfo.FormalParameterList formals = func.getParameterList();
        /* [10.5.3] step 4 */
        String[] parameterNames = formals.boundNames();
        /* [10.5.3] step 5 */
        RuntimeInfo.Declaration[] varDeclarations = code.varScopedDeclarations();
        /* [10.5.3] step 6 */
        List<RuntimeInfo.Declaration> functionsToInitialize = new ArrayList<>();
        /* [10.5.3] step 7 */
        boolean argumentsObjectNotNeeded = false;
        /* [10.5.3] step 8 */
        for (RuntimeInfo.Declaration d : reverse(varDeclarations)) {
            if (d.isFunctionDeclaration()) {
                String fn = d.boundNames()[0];
                if ("arguments".equals(fn)) {
                    argumentsObjectNotNeeded = true;
                }
                boolean alreadyDeclared = envRec.hasBinding(fn);
                if (!alreadyDeclared) {
                    // FIXME: not in spec -> changed from mutable to immutable binding
                    // envRec.createMutableBinding(fn, false);
                    envRec.createImmutableBinding(fn);
                    functionsToInitialize.add(d);
                }
            }
        }
        /* [10.5.3] step 9 */
        for (String paramName : parameterNames) {
            boolean alreadyDeclared = envRec.hasBinding(paramName);
            if (!alreadyDeclared) {
                if ("arguments".equals(paramName)) {
                    argumentsObjectNotNeeded = true;
                }
                envRec.createMutableBinding(paramName, false);
                envRec.initializeBinding(paramName, UNDEFINED);
            }
        }
        /* [10.5.3] step 10-11 */
        if (!argumentsObjectNotNeeded) {
            if (strict) {
                envRec.createImmutableBinding("arguments");
            } else {
                envRec.createMutableBinding("arguments", false);
            }
        }
        /* [10.5.3] step 12 */
        String[] varNames = code.varDeclaredNames();
        /* [10.5.3] step 13 */
        for (String varName : varNames) {
            boolean alreadyDeclared = envRec.hasBinding(varName);
            if (!alreadyDeclared) {
                envRec.createMutableBinding(varName, false);
            }
        }
        /* [10.5.3] step 14 */
        RuntimeInfo.Declaration[] lexDeclarations = code.lexicalDeclarations();
        /* [10.5.3] step 15 */
        for (RuntimeInfo.Declaration d : lexDeclarations) {
            for (String dn : d.boundNames()) {
                if (d.isConstDeclaration()) {
                    envRec.createImmutableBinding(dn);
                } else {
                    envRec.createMutableBinding(dn, false);
                }
            }
        }
        /* [10.5.3] step 16 */
        for (RuntimeInfo.Declaration f : functionsToInitialize) {
            String fn = f.boundNames()[0];
            RuntimeInfo.Function fd = code.getFunction(f);
            Function fo;
            if (fd.isGenerator()) {
                fo = InstantiateGeneratorObject(realm, env, fd);
            } else {
                fo = InstantiateFunctionObject(realm, env, fd);
            }
            // FIXME: not in spec -> changed from mutable to immutable binding
            // envRec.setMutableBinding(fn, fo, false);
            envRec.initializeBinding(fn, fo);
        }
        /* [10.5.3] step 17-19 */
        ExoticArguments ao = InstantiateArgumentsObject(realm, args);
        /* [10.5.3] step 20-21 */
        BindingInitialisation(formals, cx, ao, env);
        /* [10.5.3] step 22 */
        if (!argumentsObjectNotNeeded) {
            if (strict) {
                CompleteStrictArgumentsObject(realm, ao);
            } else {
                CompleteMappedArgumentsObject(realm, ao, func, formals, env);
            }
            envRec.initializeBinding("arguments", ao);
        }
        /* [10.5.3] step 23 */
        return;
    }

    private static void BindingInitialisation(RuntimeInfo.FormalParameterList formals,
            ExecutionContext cx, Scriptable ao, LexicalEnvironment env) {
        formals.bindingInitialisation(cx, ao, env);
    }

    private static <T> Iterable<T> reverse(T[] array) {
        return reverse(asList(array));
    }

    private static <T> Iterable<T> reverse(final List<T> list) {
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

    // /**
    // * [10.5.3 Function Declaration Instantiation]
    // */
    // public static void FunctionDeclarationInstantiation(ExecutionContext cx,
    // Function func,
    // Object... args) {
    // assert cx.varEnv == cx.lexEnv;
    // Realm realm = cx.realm;
    // LexicalEnvironment env = cx.varEnv;
    // assert env.getEnvRec() instanceof DeclarativeEnvironmentRecord;
    // DeclarativeEnvironmentRecord envRec = (DeclarativeEnvironmentRecord)
    // env.getEnvRec();
    // /* [10.5.3] step 1 */
    // RuntimeInfo.Code code = func.getCode();
    // /* [10.5.3] step 2 */
    // boolean strict = func.isStrict();
    // /* [10.5.3] step 3 */
    // RuntimeInfo.FormalParameterList formals = func.getParameterList();
    // /* [10.5.3] step 4 */
    // String[] parameterNames = formals.boundNames();
    // /* [10.5.3] step 5 */
    // for (String argName : parameterNames) {
    // boolean alreadyDeclared = envRec.hasBinding(argName);
    // if (!alreadyDeclared) {
    // envRec.createMutableBinding(argName, false);
    // if (!strict) {
    // envRec.initializeBinding(argName, Undefined.instance);
    // }
    // }
    // }
    // /* [10.5.3] step 6 */
    // RuntimeInfo.Declaration[] declarations = code.lexicalDeclarations();
    // if (strict) {
    // /* [10.5.3] step 7,9-10 */
    // Scriptable ao = CreateStrictArgumentsObject(cx, args);
    // BindingInitialisation(formals, cx, ao, env);
    // } else {
    // /* [10.5.3] step 8,9-10 */
    // String[] names = formals.boundNames();
    // Scriptable ao = CreateMappedArgumentsObject(cx, func, names, env, args);
    // BindingInitialisation(formals, cx, ao, null);
    // }
    // /* [10.5.3] step 11 */
    // for (RuntimeInfo.Declaration d : declarations) {
    // for (String dn : d.boundNames()) {
    // boolean alreadyDeclared = envRec.hasBinding(dn);
    // if (!alreadyDeclared) {
    // if (d.isConstDeclaration()) {
    // envRec.createImmutableBinding(dn);
    // } else {
    // envRec.createMutableBinding(dn, false);
    // }
    // }
    // }
    // }
    // /* [10.5.3] step 12-13 */
    // boolean argumentsAlreayDeclared = envRec.hasBinding("arguments");
    // /* [10.5.3] step 14 */
    // if (!argumentsAlreayDeclared) {
    // if (strict) {
    // envRec.createImmutableBinding("arguments");
    // } else {
    // envRec.createMutableBinding("arguments", false);
    // }
    // }
    // /* [10.5.3] step 15 */
    // String[] varNames = code.varDeclaredNames();
    // /* [10.5.3] step 16 */
    // for (String varName : varNames) {
    // boolean alreadyDeclared = envRec.hasBinding(varName);
    // if (!alreadyDeclared) {
    // envRec.createMutableBinding(varName, false);
    // envRec.initializeBinding(varName, Undefined.instance);
    // }
    // }
    // /* [10.5.3] step 17 */
    // Set<String> initializedFunctions = new HashSet<>();
    // /* [10.5.3] step 18 */
    // for (RuntimeInfo.Declaration d : reverse(declarations)) {
    // if (!d.isFunctionDeclaration()) {
    // continue;
    // }
    // RuntimeInfo.Function fd = code.getFunction(d);
    // String fn = fd.functionName();
    // if (!initializedFunctions.contains(fn)) {
    // initializedFunctions.add(fn);
    // Function fo = InstantiateFunctionObject(realm, cx.lexEnv, fd);
    // envRec.initializeBinding(fn, fo);
    // }
    // }
    // /* [10.5.3] step 19 */
    // return;
    // }
}
