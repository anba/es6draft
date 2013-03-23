/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandle;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction;

/**
 * Classes for bootstrapping of functions and script code
 * 
 */
public final class RuntimeInfo {
    private RuntimeInfo() {
    }

    private static Object evaluateCode(ExecutionContext cx, Code code) {
        try {
            Object result = code.handle().invokeExact(cx);
            // tail-call with trampoline
            while (result instanceof Object[]) {
                // <func(Callable), thisValue, args>
                Object[] h = (Object[]) result;
                OrdinaryFunction f = (OrdinaryFunction) h[0];
                Object thisValue = h[1];
                Object[] args = (Object[]) h[2];

                // see OrdinaryFunction#call()
                /* step 1-11 */
                ExecutionContext calleeContext = ExecutionContext.newFunctionExecutionContext(f,
                        thisValue);
                /* step 12-13 */
                f.getFunction().functionDeclarationInstantiation(calleeContext, f, args);

                result = f.getCode().handle().invokeExact(calleeContext);
            }
            return result;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static Code newCode(final MethodHandle handle) {
        return new Code() {
            @Override
            public MethodHandle handle() {
                return handle;
            }

            @Override
            public Object evaluate(ExecutionContext cx) {
                return evaluateCode(cx, this);
            }
        };
    }

    public static Function newFunction(final String functionName, final boolean isGenerator,
            final boolean hasSuperReference, final boolean isStrict,
            final int expectedArgumentCount, final MethodHandle initialisation,
            final MethodHandle handle, final String source) {
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
            public int expectedArgumentCount() {
                return expectedArgumentCount;
            }

            @Override
            public void functionDeclarationInstantiation(ExecutionContext cx,
                    FunctionObject function, Object[] args) {
                try {
                    initialisation.invokeExact(cx, function, args);
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
                return evaluateCode(cx, this);
            }

            @Override
            public String source() {
                return source;
            }
        };
    }

    public static ScriptBody newScriptBody(final boolean isStrict,
            final MethodHandle initialisation, final MethodHandle evalinitialisation,
            final MethodHandle handle) {
        return new ScriptBody() {
            @Override
            public boolean isStrict() {
                return isStrict;
            }

            // TODO: create ScriptBody and EvalScriptBody interfaces

            @Override
            public void globalDeclarationInstantiation(Realm realm, LexicalEnvironment globalEnv,
                    boolean deletableBindings) {
                try {
                    initialisation.invokeExact(realm, globalEnv, deletableBindings);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void evalDeclarationInstantiation(Realm realm, LexicalEnvironment lexEnv,
                    LexicalEnvironment varEnv, boolean deletableBindings) {
                try {
                    evalinitialisation.invokeExact(realm, lexEnv, varEnv, deletableBindings);
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

        void globalDeclarationInstantiation(Realm realm, LexicalEnvironment globalEnv,
                boolean deletableBindings);

        void evalDeclarationInstantiation(Realm realm, LexicalEnvironment lexEnv,
                LexicalEnvironment varEnv, boolean deletableBindings);

        Object evaluate(ExecutionContext cx);
    }

    /**
     * Compiled function information
     */
    public static interface Function extends Code {
        String functionName();

        boolean isGenerator();

        boolean hasSuperReference();

        boolean isStrict();

        int expectedArgumentCount();

        void functionDeclarationInstantiation(ExecutionContext cx, FunctionObject function,
                Object[] args);

        String source();
    }

    /**
     * Compiled function code information
     */
    public static interface Code {
        MethodHandle handle();

        Object evaluate(ExecutionContext cx);
    }
}
