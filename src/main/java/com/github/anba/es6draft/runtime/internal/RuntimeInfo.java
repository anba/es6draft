/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryFunction.EvaluateBody;

import java.lang.invoke.MethodHandle;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.types.builtins.ExoticArguments;
import com.github.anba.es6draft.runtime.types.builtins.FunctionObject;

/**
 * Classes for bootstrapping of functions and script code
 * 
 */
public final class RuntimeInfo {
    private RuntimeInfo() {
    }

    public static Code newCode(final MethodHandle handle) {
        return new Code() {
            @Override
            public MethodHandle handle() {
                return handle;
            }

            @Override
            public Object evaluate(ExecutionContext cx) {
                return EvaluateBody(cx, this);
            }
        };
    }

    public static Function newFunction(final String functionName, final int functionFlags,
            final int expectedArgumentCount, final MethodHandle initialisation,
            final MethodHandle handle, final String source) {
        return new Function() {
            @Override
            public String functionName() {
                return functionName;
            }

            @Override
            public boolean isStrict() {
                return FunctionFlags.Strict.isSet(functionFlags);
            }

            @Override
            public boolean hasSuperReference() {
                return FunctionFlags.Super.isSet(functionFlags);
            }

            @Override
            public boolean hasScopedName() {
                return FunctionFlags.ScopedName.isSet(functionFlags);
            }

            @Override
            public boolean isGenerator() {
                return FunctionFlags.Generator.isSet(functionFlags);
            }

            @Override
            public int expectedArgumentCount() {
                return expectedArgumentCount;
            }

            @Override
            public ExoticArguments functionDeclarationInstantiation(ExecutionContext cx,
                    FunctionObject function, Object[] args) {
                try {
                    return (ExoticArguments) initialisation.invokeExact(cx, function, args);
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
                return EvaluateBody(cx, this);
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
            public void globalDeclarationInstantiation(ExecutionContext cx,
                    LexicalEnvironment globalEnv, boolean deletableBindings) {
                try {
                    initialisation.invokeExact(cx, globalEnv, deletableBindings);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void evalDeclarationInstantiation(ExecutionContext cx,
                    LexicalEnvironment lexEnv, LexicalEnvironment varEnv, boolean deletableBindings) {
                try {
                    evalinitialisation.invokeExact(cx, lexEnv, varEnv, deletableBindings);
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
    public interface ScriptBody {
        boolean isStrict();

        void globalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment globalEnv,
                boolean deletableBindings);

        void evalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment lexEnv,
                LexicalEnvironment varEnv, boolean deletableBindings);

        Object evaluate(ExecutionContext cx);
    }

    public enum FunctionFlags {
        /**
         * Flag for strict-mode functions
         */
        Strict(0b0001),

        /**
         * Flag for functions with super-binding
         */
        Super(0b0010),

        /**
         * Flag for functions which have their name in scope
         */
        ScopedName(0b0100),

        /**
         * Flag for generator functions
         */
        Generator(0b1000);

        private final int value;

        private FunctionFlags(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public boolean isSet(int bitmask) {
            return (value & bitmask) != 0;
        }
    }

    /**
     * Compiled function information
     */
    public interface Function extends Code {
        String functionName();

        boolean isStrict();

        boolean hasSuperReference();

        boolean hasScopedName();

        boolean isGenerator();

        int expectedArgumentCount();

        ExoticArguments functionDeclarationInstantiation(ExecutionContext cx,
                FunctionObject function, Object[] args);

        String source();
    }

    /**
     * Compiled function code information
     */
    public interface Code {
        MethodHandle handle();

        Object evaluate(ExecutionContext cx);
    }
}
