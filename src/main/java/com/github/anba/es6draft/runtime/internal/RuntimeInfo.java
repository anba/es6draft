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

/**
 * Classes for bootstrapping of function and script code
 */
public final class RuntimeInfo {
    private RuntimeInfo() {
    }

    public static Function newFunction(final String functionName, final int functionFlags,
            final int expectedArgumentCount, final String source, final MethodHandle handle,
            final MethodHandle callMethod) {
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
            public boolean hasTailCall() {
                return FunctionFlags.TailCall.isSet(functionFlags);
            }

            @Override
            public boolean isLegacy() {
                return FunctionFlags.Legacy.isSet(functionFlags);
            }

            @Override
            public boolean hasSyntheticMethods() {
                return FunctionFlags.SyntheticMethods.isSet(functionFlags);
            }

            @Override
            public int expectedArgumentCount() {
                return expectedArgumentCount;
            }

            @Override
            public String source() {
                return source;
            }

            @Override
            public MethodHandle handle() {
                return handle;
            }

            @Override
            public MethodHandle callMethod() {
                return callMethod;
            }
        };
    }

    public static ScriptBody newScriptBody(final String sourceFile, final boolean isStrict,
            final MethodHandle initialisation, final MethodHandle evalinitialisation,
            final MethodHandle handle) {
        return new ScriptBody() {
            @Override
            public String sourceFile() {
                return sourceFile;
            }

            @Override
            public boolean isStrict() {
                return isStrict;
            }

            @Override
            public void globalDeclarationInstantiation(ExecutionContext cx,
                    LexicalEnvironment globalEnv, LexicalEnvironment lexicalEnv,
                    boolean deletableBindings) {
                try {
                    initialisation.invokeExact(cx, globalEnv, lexicalEnv, deletableBindings);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void evalDeclarationInstantiation(ExecutionContext cx,
                    LexicalEnvironment variableEnv, LexicalEnvironment lexicalEnv,
                    boolean deletableBindings) {
                try {
                    evalinitialisation.invokeExact(cx, variableEnv, lexicalEnv, deletableBindings);
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
        String sourceFile();

        boolean isStrict();

        void globalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment globalEnv,
                LexicalEnvironment lexicalEnv, boolean deletableBindings);

        void evalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment variableEnv,
                LexicalEnvironment lexicalEnv, boolean deletableBindings);

        Object evaluate(ExecutionContext cx);
    }

    public enum FunctionFlags {
        /**
         * Flag for strict-mode functions
         */
        Strict(0b0000_0001),

        /**
         * Flag for functions with super-binding
         */
        Super(0b0000_0010),

        /**
         * Flag for functions which have their name in scope
         */
        ScopedName(0b0000_0100),

        /**
         * Flag for generator functions
         */
        Generator(0b0000_1000),

        /**
         * Flag for tail-call functions
         */
        TailCall(0b0001_0000),

        /**
         * Flag for legacy functions
         */
        Legacy(0b0010_0000),

        /**
         * Flag for functions with synthetic sub-methods
         */
        SyntheticMethods(0b0100_0000);

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
    public interface Function {
        String functionName();

        boolean isStrict();

        boolean hasSuperReference();

        boolean hasScopedName();

        boolean isGenerator();

        boolean hasTailCall();

        boolean isLegacy();

        boolean hasSyntheticMethods();

        int expectedArgumentCount();

        String source();

        /**
         * (? extends FunctionObject, ExecutionContext, Object, Object[]) -> Object
         */
        MethodHandle callMethod();

        /**
         * (ExecutionContext, ...?) -> Object
         */
        MethodHandle handle();
    }
}
