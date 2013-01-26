/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.DeclarationBindingInstantiation.FunctionDeclarationInstantiation;

import java.lang.invoke.MethodHandle;
import java.util.Map;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.Scriptable;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;

/**
 * Classes for bootstrapping of functions and script code
 * 
 */
public final class RuntimeInfo {
    private RuntimeInfo() {
    }

    public static Code newCode(final String source, final Declaration[] lexicalDeclarations,
            final String[] varDeclaredNames, final Declaration[] varScopedDeclarations,
            final Map<Declaration, MethodHandle> functions, final MethodHandle handle) {
        return new Code() {
            @Override
            public String source() {
                return source;
            }

            @Override
            public Declaration[] lexicalDeclarations() {
                return lexicalDeclarations;
            }

            @Override
            public String[] varDeclaredNames() {
                return varDeclaredNames;
            }

            @Override
            public Declaration[] varScopedDeclarations() {
                return varScopedDeclarations;
            }

            @Override
            public Function getFunction(Declaration d) {
                try {
                    return (Function) functions.get(d).invokeExact();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public MethodHandle handle() {
                return handle;
            }

            @Override
            public Object evaluate(ExecutionContext cx) {
                try {
                    MethodHandle mh = handle();
                    Object result = mh.invokeExact(cx);
                    // tail-call with trampoline
                    while (result instanceof Object[]) {
                        // <func(Callable), thisValue, args>
                        Object[] h = (Object[]) result;
                        OrdinaryFunction f = (OrdinaryFunction) h[0];
                        Object thisValue = h[1];
                        Object[] args = (Object[]) h[2];

                        // see OrdinaryFunction#call()
                        /* step 1-11 */
                        ExecutionContext calleeContext = ExecutionContext
                                .newFunctionExecutionContext(f, thisValue);
                        /* step 12-13 */
                        FunctionDeclarationInstantiation(calleeContext, f, args);

                        result = f.getCode().handle().invokeExact(calleeContext);
                    }
                    return result;
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static FormalParameter newFormalParameter(final String[] boundNames,
            final boolean isBindingIdentifier) {
        return new FormalParameter() {
            @Override
            public String[] boundNames() {
                return boundNames;
            }

            @Override
            public boolean isBindingIdentifier() {
                return isBindingIdentifier;
            }
        };
    }

    public static FormalParameterList newFormalParameterList(final String[] boundNames,
            final int expectedArgumentCount, final int numberOfParameters,
            final FormalParameter[] parameters, final MethodHandle bindingInitialisation) {
        return new FormalParameterList() {
            @Override
            public String[] boundNames() {
                return boundNames;
            }

            @Override
            public int expectedArgumentCount() {
                return expectedArgumentCount;
            }

            @Override
            public int numberOfParameters() {
                return numberOfParameters;
            }

            @Override
            public FormalParameter getParameter(int index) {
                return parameters[index];
            }

            @Override
            public void bindingInitialisation(ExecutionContext cx, Scriptable ao,
                    LexicalEnvironment env) {
                try {
                    bindingInitialisation.invokeExact(cx, ao, env);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static Function newFunction(final String functionName, final boolean isGenerator,
            final boolean hasSuperReference, final boolean isStrict,
            final FormalParameterList formals, final Code code) {
        return new Function() {
            @Override
            public String functionName() {
                return functionName;
            }

            @Override
            public boolean isGenerator() {
                return isGenerator;
            }

            @Override
            public boolean hasSuperReference() {
                return hasSuperReference;
            }

            @Override
            public boolean isStrict() {
                return isStrict;
            }

            @Override
            public FormalParameterList formals() {
                return formals;
            }

            @Override
            public Code code() {
                return code;
            }
        };
    }

    public static Declaration newDeclaration(final String[] boundNames,
            final boolean isConstDeclaration, final boolean isFunctionDeclaration,
            final boolean isVariableStatement) {
        return new Declaration() {
            @Override
            public String[] boundNames() {
                return boundNames;
            }

            @Override
            public boolean isConstDeclaration() {
                return isConstDeclaration;
            }

            @Override
            public boolean isFunctionDeclaration() {
                return isFunctionDeclaration;
            }

            @Override
            public boolean isVariableStatement() {
                return isVariableStatement;
            }
        };
    }

    public static ScriptBody newScriptBody(final boolean isStrict,
            final String[] lexicallyDeclaredNames, final Declaration[] lexicallyScopedDeclarations,
            final String[] varDeclaredNames, final Declaration[] varScopedDeclarations,
            final Map<Declaration, MethodHandle> functions, final MethodHandle handle) {
        return new ScriptBody() {
            @Override
            public boolean isStrict() {
                return isStrict;
            }

            @Override
            public String[] lexicallyDeclaredNames() {
                return lexicallyDeclaredNames;
            }

            @Override
            public Declaration[] lexicallyScopedDeclarations() {
                return lexicallyScopedDeclarations;
            }

            @Override
            public String[] varDeclaredNames() {
                return varDeclaredNames;
            }

            @Override
            public Declaration[] varScopedDeclarations() {
                return varScopedDeclarations;
            }

            @Override
            public Function getFunction(Declaration d) {
                try {
                    return (Function) functions.get(d).invokeExact();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Object evaluate(ExecutionContext cx) {
                try {
                    return handle.invokeExact(cx);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Compiled script body information
     */
    public static interface ScriptBody {
        boolean isStrict();

        String[] lexicallyDeclaredNames();

        Declaration[] lexicallyScopedDeclarations();

        String[] varDeclaredNames();

        Declaration[] varScopedDeclarations();

        Function getFunction(Declaration d);

        Object evaluate(ExecutionContext cx);
    }

    /**
     * Compiled declaration information
     */
    public static interface Declaration {
        String[] boundNames();

        boolean isConstDeclaration();

        boolean isFunctionDeclaration();

        boolean isVariableStatement();
    }

    /**
     * Compiled function information
     */
    public static interface Function {
        String functionName();

        boolean isGenerator();

        boolean hasSuperReference();

        boolean isStrict();

        FormalParameterList formals();

        Code code();
    }

    /**
     * Compiled formal parameter information
     */
    public static interface FormalParameter {
        String[] boundNames();

        boolean isBindingIdentifier();
    }

    /**
     * Compiled formal parameter list information
     */
    public static interface FormalParameterList {
        String[] boundNames();

        int expectedArgumentCount();

        int numberOfParameters();

        FormalParameter getParameter(int index);

        void bindingInitialisation(ExecutionContext cx, Scriptable ao, LexicalEnvironment env);
    }

    /**
     * Compiled function code information
     */
    public static interface Code {
        String source();

        Declaration[] lexicalDeclarations();

        String[] varDeclaredNames();

        Declaration[] varScopedDeclarations();

        Function getFunction(Declaration d);

        MethodHandle handle();

        Object evaluate(ExecutionContext cx);
    }
}
