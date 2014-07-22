/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.lang.invoke.MethodHandle;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.GlobalEnvironmentRecord;
import com.github.anba.es6draft.runtime.LexicalEnvironment;

/**
 * Classes for function and script code bootstrapping.
 */
public final class RuntimeInfo {
    private RuntimeInfo() {
    }

    /**
     * Returns a new {@link Function} object.
     * 
     * @param functionName
     *            the function name
     * @param functionFlags
     *            the function flags
     * @param expectedArgumentCount
     *            the number of expected arguments
     * @param source
     *            the encoded source string
     * @param bodySourceStart
     *            the body source start index
     * @param handle
     *            the method handle
     * @param callMethod
     *            the call method handle
     * @return the new function object
     */
    public static Function newFunction(String functionName, int functionFlags,
            int expectedArgumentCount, String source, int bodySourceStart, MethodHandle handle,
            MethodHandle callMethod) {
        return new CompiledFunction(functionName, functionFlags, expectedArgumentCount, source,
                bodySourceStart, handle, callMethod);
    }

    /**
     * Returns a new {@link ScriptBody} object.
     * 
     * @param sourceFile
     *            the source file location
     * @param isStrict
     *            the strict mode flag
     * @param initialization
     *            the initialization method handle
     * @param evalinitialization
     *            the eval-initialization method handle
     * @param handle
     *            the code method handle
     * @return the new script object
     */
    public static ScriptBody newScriptBody(String sourceFile, boolean isStrict,
            MethodHandle initialization, MethodHandle evalinitialization, MethodHandle handle) {
        return new CompiledScriptBody(sourceFile, isStrict, initialization, evalinitialization,
                handle);
    }

    /**
     * Compiled script body information
     */
    public interface ScriptBody {
        /**
         * Returns the source file location.
         * 
         * @return the source file location
         */
        String sourceFile();

        /**
         * Returns {@code true} if the script uses strict mode semantics.
         * 
         * @return {@code true} if the script is strict
         */
        boolean isStrict();

        /**
         * Performs 15.1.8 Runtime Semantics: GlobalDeclarationInstantiation.
         * 
         * @param cx
         *            the execution context
         * @param globalEnv
         *            the global environment
         * @param lexicalEnv
         *            the current lexical environment
         * @param deletableBindings
         *            {@code true} if new bindings are deletable
         */
        void globalDeclarationInstantiation(ExecutionContext cx,
                LexicalEnvironment<GlobalEnvironmentRecord> globalEnv,
                LexicalEnvironment<?> lexicalEnv, boolean deletableBindings);

        /**
         * Performs 18.2.1.2 Eval Declaration Instantiation.
         * 
         * @param cx
         *            the execution context
         * @param variableEnv
         *            the current variable environment
         * @param lexicalEnv
         *            the current lexical environment
         * @param deletableBindings
         *            {@code true} if new bindings are deletable
         */
        void evalDeclarationInstantiation(ExecutionContext cx, LexicalEnvironment<?> variableEnv,
                LexicalEnvironment<?> lexicalEnv, boolean deletableBindings);

        /**
         * Performs 15.1.7 Runtime Semantics: Script Evaluation.
         * 
         * @param cx
         *            the execution context
         * @return the evaluation result
         */
        Object evaluate(ExecutionContext cx);
    }

    private static final class CompiledScriptBody implements ScriptBody {
        private final String sourceFile;
        private final boolean isStrict;
        private final MethodHandle initialization;
        private final MethodHandle evalinitialization;
        private final MethodHandle handle;

        CompiledScriptBody(String sourceFile, boolean isStrict, MethodHandle initialization,
                MethodHandle evalinitialization, MethodHandle handle) {
            this.sourceFile = sourceFile;
            this.isStrict = isStrict;
            this.initialization = initialization;
            this.evalinitialization = evalinitialization;
            this.handle = handle;
        }

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
                LexicalEnvironment<GlobalEnvironmentRecord> globalEnv,
                LexicalEnvironment<?> lexicalEnv, boolean deletableBindings) {
            try {
                initialization.invokeExact(cx, globalEnv, lexicalEnv, deletableBindings);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void evalDeclarationInstantiation(ExecutionContext cx,
                LexicalEnvironment<?> variableEnv, LexicalEnvironment<?> lexicalEnv,
                boolean deletableBindings) {
            try {
                evalinitialization.invokeExact(cx, variableEnv, lexicalEnv, deletableBindings);
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
    }

    /**
     * Function flags enumeration.
     */
    public enum FunctionFlags {
        /**
         * Flag for strict-mode functions.
         */
        Strict(0x0001),

        /**
         * Flag for implicit strict functions.
         */
        ImplicitStrict(0x0002),

        /**
         * Flag for generator functions.
         */
        Generator(0x0004),

        /**
         * Flag for async functions.
         */
        Async(0x0008),

        /**
         * Flag for arrow functions.
         */
        Arrow(0x0010),

        /**
         * Flag for declarative functions.
         */
        Declaration(0x0020),

        /**
         * Flag for expression functions.
         */
        Expression(0x0040),

        /**
         * Flag for functions with concise, braceless body.
         */
        ConciseBody(0x0080),

        /**
         * Flag for method definitions.
         */
        Method(0x0100),

        /**
         * Flag for static method definitions.
         */
        Static(0x0200),

        /**
         * Flag for legacy generator functions.
         */
        LegacyGenerator(0x0400),

        /**
         * Flag for legacy functions.
         */
        Legacy(0x0800),

        /**
         * Flag for functions which create a named scope binding.
         */
        ScopedName(0x1000),

        /**
         * Flag for functions with super-binding.
         */
        Super(0x2000),

        /**
         * Flag to select resume generator implementation.
         */
        ResumeGenerator(0x4000),

        /**
         * Flag for tail-call functions.
         */
        TailCall(0x8000),

        ;

        private final int value;

        private FunctionFlags(int value) {
            this.value = value;
        }

        /**
         * Returns the function flag bitmask.
         * 
         * @return the function flag bitmask
         */
        public int getValue() {
            return value;
        }

        /**
         * Returns {@code true} if this function flag is set in <var>bitmask</var>.
         * 
         * @param bitmask
         *            the bitmask
         * @return {@code true} if the function flag is set
         */
        public boolean isSet(int bitmask) {
            return (value & bitmask) != 0;
        }
    }

    /**
     * Compiled function information
     */
    public interface Function {
        String functionName();

        /**
         * Returns {@code true} for strict mode functions.
         * 
         * @return {@code true} if a strict mode function
         */
        boolean isStrict();

        /**
         * Returns {@code true} for functions containing a <code>super</code> expression.
         * 
         * @return {@code true} if <code>super</code> expression is present
         */
        boolean hasSuperReference();

        /**
         * Returns {@code true} if the function name is scoped.
         * 
         * @return {@code true} if the function name is scoped
         */
        boolean hasScopedName();

        /**
         * Returns {@code true} for generator function.
         * 
         * @return {@code true} if the function is a generator function
         */
        boolean isGenerator();

        /**
         * Returns {@code true} for async function.
         * 
         * @return {@code true} if the function is an async function
         */
        boolean isAsync();

        /**
         * Returns {@code true} for functions containing a tail-call.
         * 
         * @return {@code true} if tail-call is present
         */
        boolean hasTailCall();

        /**
         * Returns {@code true} for legacy mode function.
         * 
         * @return {@code true} if the function has legacy properties
         */
        boolean isLegacy();

        /**
         * Returns {@code true} if resume generators are requested for this function.
         * 
         * @return {@code true} if resume generators are requested
         */
        boolean isResumeGenerator();

        /**
         * Returns the function flags bitmask.
         * 
         * @return the function flags bitmask
         * @see FunctionFlags
         */
        int functionFlags();

        /**
         * Returns the number of expected arguments of this function.
         * 
         * @return the number of expected arguments
         */
        int expectedArgumentCount();

        /**
         * Returns the compressed source string.
         * 
         * @return the compressed source string
         */
        String source();

        /**
         * Returns the start index of the function body in the decompressed source string.
         * 
         * @return the start index of the function body
         */
        int bodySourceStart();

        /**
         * (? extends FunctionObject, ExecutionContext, Object, Object[]) {@literal ->} Object.
         * 
         * @return the method handle for normal calls
         */
        MethodHandle callMethod();

        /**
         * (ExecutionContext, ...?) {@literal ->} Object
         * 
         * @return the method handle for tail calls
         */
        MethodHandle handle();
    }

    private static final class CompiledFunction implements Function {
        private final String functionName;
        private final int functionFlags;
        private final int expectedArgumentCount;
        private final String source;
        private final int bodySourceStart;
        private final MethodHandle handle;
        private final MethodHandle callMethod;

        CompiledFunction(String functionName, int functionFlags, int expectedArgumentCount,
                String source, int bodySourceStart, MethodHandle handle, MethodHandle callMethod) {
            this.functionName = functionName;
            this.functionFlags = functionFlags;
            this.expectedArgumentCount = expectedArgumentCount;
            this.source = source;
            this.bodySourceStart = bodySourceStart;
            this.handle = handle;
            this.callMethod = callMethod;
        }

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
        public boolean isAsync() {
            return FunctionFlags.Async.isSet(functionFlags);
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
        public boolean isResumeGenerator() {
            return FunctionFlags.ResumeGenerator.isSet(functionFlags);
        }

        @Override
        public int functionFlags() {
            return functionFlags;
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
        public int bodySourceStart() {
            return bodySourceStart;
        }

        @Override
        public MethodHandle handle() {
            return handle;
        }

        @Override
        public MethodHandle callMethod() {
            return callMethod;
        }
    }
}
