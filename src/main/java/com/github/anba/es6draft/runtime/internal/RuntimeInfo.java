/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.ModuleEnvironmentRecord;
import com.github.anba.es6draft.runtime.modules.MalformedNameException;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;

/**
 * Classes for function and script code bootstrapping.
 */
public final class RuntimeInfo {
    private RuntimeInfo() {
    }

    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type) {
        assert "methodInfo".equals(name);
        // Empty object as a placeholder.
        return new ConstantCallSite(MethodHandles.constant(Object.class, new Object()));
    }

    /**
     * Returns a new {@link Function} object.
     * 
     * @param methodInfo
     *            the method info object
     * @param functionName
     *            the function name
     * @param functionFlags
     *            the function flags
     * @param expectedArgumentCount
     *            the number of expected arguments
     * @param parameters
     *            the parameter names or {@code null}
     * @param source
     *            the encoded source string
     * @param handle
     *            the method handle
     * @param callMethod
     *            the call method handle
     * @param constructMethod
     *            the construct method handle
     * @return the new function object
     */
    public static Function newFunction(Object methodInfo, String functionName, int functionFlags,
            int expectedArgumentCount, String[] parameters, String source, MethodHandle handle, MethodHandle callMethod,
            MethodHandle constructMethod) {
        return new CompiledFunction(methodInfo, functionName, functionFlags, expectedArgumentCount, parameters, source,
                handle, callMethod, constructMethod, null);
    }

    /**
     * Returns a new {@link Function} object.
     * 
     * @param methodInfo
     *            the method info object
     * @param functionName
     *            the function name
     * @param functionFlags
     *            the function flags
     * @param expectedArgumentCount
     *            the number of expected arguments
     * @param parameters
     *            the parameter names or {@code null}
     * @param source
     *            the encoded source string
     * @param handle
     *            the method handle
     * @param callMethod
     *            the call method handle
     * @param constructMethod
     *            the construct method handle
     * @param debugInfo
     *            the debug info method handle
     * @return the new function object
     */
    public static Function newFunction(Object methodInfo, String functionName, int functionFlags,
            int expectedArgumentCount, String[] parameters, String source, MethodHandle handle, MethodHandle callMethod,
            MethodHandle constructMethod, MethodHandle debugInfo) {
        return new CompiledFunction(methodInfo, functionName, functionFlags, expectedArgumentCount, parameters, source,
                handle, callMethod, constructMethod, debugInfo);
    }

    /**
     * Returns a new {@link ModuleBody} object.
     * 
     * @param initialization
     *            the initialization method handle
     * @param handle
     *            the code method handle
     * @return the new module object
     */
    public static ModuleBody newModuleBody(MethodHandle initialization, MethodHandle handle) {
        return new CompiledModuleBody(initialization, handle, null);
    }

    /**
     * Returns a new {@link ModuleBody} object.
     * 
     * @param initialization
     *            the initialization method handle
     * @param handle
     *            the code method handle
     * @param debugInfo
     *            the debug info method handle
     * @return the new module object
     */
    public static ModuleBody newModuleBody(MethodHandle initialization, MethodHandle handle, MethodHandle debugInfo) {
        return new CompiledModuleBody(initialization, handle, debugInfo);
    }

    /**
     * Returns a new {@link ScriptBody} object.
     * 
     * @param evaluation
     *            the script evaluation method handle
     * @return the new script object
     */
    public static ScriptBody newScriptBody(MethodHandle evaluation) {
        return new CompiledScriptBody(evaluation, null);
    }

    /**
     * Returns a new {@link ScriptBody} object.
     * 
     * @param evaluation
     *            the script evaluation method handle
     * @param debugInfo
     *            the debug info method handle
     * @return the new script object
     */
    public static ScriptBody newScriptBody(MethodHandle evaluation, MethodHandle debugInfo) {
        return new CompiledScriptBody(evaluation, debugInfo);
    }

    /**
     * The runtime object.
     */
    public interface RuntimeObject {
        /**
         * Returns the debug information or {@code null} if not available.
         * 
         * @return the debug information
         */
        DebugInfo debugInfo();
    }

    /**
     * Compiled script body information.
     */
    public interface ScriptBody extends RuntimeObject {
        /**
         * Evaluates the script.
         * 
         * @param cx
         *            the execution context
         * @param script
         *            the script
         * @return the evaluation result
         */
        Object evaluate(ExecutionContext cx, Script script);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E rethrow(Throwable e) throws E {
        throw (E) e;
    }

    private static final class CompiledScriptBody implements ScriptBody {
        private final MethodHandle evaluation;
        private final MethodHandle debugInfo;

        CompiledScriptBody(MethodHandle evaluation, MethodHandle debugInfo) {
            this.evaluation = evaluation;
            this.debugInfo = debugInfo;
        }

        @Override
        public Object evaluate(ExecutionContext cx, Script script) {
            try {
                return evaluation.invokeExact(cx, script);
            } catch (Throwable e) {
                throw RuntimeInfo.<RuntimeException> rethrow(e);
            }
        }

        @Override
        public DebugInfo debugInfo() {
            if (debugInfo != null) {
                try {
                    return (DebugInfo) debugInfo.invokeExact();
                } catch (Throwable e) {
                    throw RuntimeInfo.<RuntimeException> rethrow(e);
                }
            }
            return null;
        }
    }

    /**
     * Compiled module information.
     */
    public interface ModuleBody extends RuntimeObject {
        /**
         * Performs 15.2.1.16.4 ModuleDeclarationInstantiation( ) Concrete Method.
         * 
         * @param cx
         *            the execution context
         * @param module
         *            the module record
         * @param env
         *            the lexical environment
         * @throws IOException
         *             if there was any I/O error
         * @throws ResolutionException
         *             if any export or import binding cannot be resolved
         * @throws MalformedNameException
         *             if any module specifier cannot be normalized
         */
        void moduleDeclarationInstantiation(ExecutionContext cx, SourceTextModuleRecord module,
                LexicalEnvironment<ModuleEnvironmentRecord> env)
                throws IOException, ResolutionException, MalformedNameException;

        /**
         * Performs 15.2.1.16.5 ModuleEvaluation() Concrete Method.
         * 
         * @param cx
         *            the execution context
         * @return the evaluation result
         */
        Object evaluate(ExecutionContext cx);
    }

    private static final class CompiledModuleBody implements ModuleBody {
        private final MethodHandle initialization;
        private final MethodHandle handle;
        private final MethodHandle debugInfo;

        CompiledModuleBody(MethodHandle initialization, MethodHandle handle, MethodHandle debugInfo) {
            this.initialization = initialization;
            this.handle = handle;
            this.debugInfo = debugInfo;
        }

        @Override
        public void moduleDeclarationInstantiation(ExecutionContext cx, SourceTextModuleRecord module,
                LexicalEnvironment<ModuleEnvironmentRecord> env)
                throws IOException, ResolutionException, MalformedNameException {
            try {
                initialization.invokeExact(cx, module, env);
            } catch (Throwable e) {
                throw RuntimeInfo.<RuntimeException> rethrow(e);
            }
        }

        @Override
        public Object evaluate(ExecutionContext cx) {
            try {
                return handle.invokeExact(cx);
            } catch (Throwable e) {
                throw RuntimeInfo.<RuntimeException> rethrow(e);
            }
        }

        @Override
        public DebugInfo debugInfo() {
            if (debugInfo != null) {
                try {
                    return (DebugInfo) debugInfo.invokeExact();
                } catch (Throwable e) {
                    throw RuntimeInfo.<RuntimeException> rethrow(e);
                }
            }
            return null;
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
         * Unused.
         */
        Unused4(0x0002),

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
         * Flag for method definitions.
         */
        Method(0x0080),

        /**
         * Flag for getter accessor methods.
         */
        Getter(0x0100),

        /**
         * Flag for setter accessor methods.
         */
        Setter(0x0200),

        /**
         * Unused.
         */
        Unused2(0x0400),

        /**
         * Flag for legacy functions.
         */
        Legacy(0x0800),

        /**
         * Flag for functions which create a named scope binding.
         */
        ScopedName(0x1000),

        /**
         * Unused.
         */
        Unused3(0x2000),

        /**
         * Unused.
         */
        Unused(0x4000),

        /**
         * Flag for tail-call functions.
         */
        TailCall(0x8000),

        /**
         * Flag for native functions.
         */
        Native(0x10000),

        /**
         * Flag for functions with direct eval calls.
         */
        Eval(0x20000),

        /**
         * Flag for tail-call functions.
         */
        TailConstruct(0x40000),

        /**
         * Flag for class constructor functions.
         */
        Class(0x80000),

        /**
         * Flag for mapped arguments.
         */
        MappedArguments(0x100000),

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
     * Compiled function source information
     */
    public static final class FunctionSource {
        private String source;
        private boolean compressed = true;

        FunctionSource(String source) {
            this.source = source;
        }

        /**
         * Returns the function source string.
         * 
         * @return the function source string
         */
        @Override
        public synchronized String toString() {
            if (compressed) {
                try {
                    source = SourceCompressor.decompress(source);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                compressed = false;
            }
            return source;
        }
    }

    /**
     * Compiled function information
     */
    public interface Function extends RuntimeObject {
        /**
         * Returns the method info object.
         * 
         * @return the method info object
         */
        Object methodInfo();

        /**
         * Returns the function's name.
         * 
         * @return the function name
         */
        String functionName();

        /**
         * Returns {@code true} for strict mode functions.
         * <p>
         * Convenience method for {@code is(FunctionFlags.Strict)}.
         * 
         * @return {@code true} if a strict mode function
         */
        boolean isStrict();

        /**
         * Returns {@code true} for generator function.
         * <p>
         * Convenience method for {@code is(FunctionFlags.Generator)}.
         * 
         * @return {@code true} if the function is a generator function
         */
        boolean isGenerator();

        /**
         * Returns {@code true} for async function.
         * <p>
         * Convenience method for {@code is(FunctionFlags.Async)}.
         * 
         * @return {@code true} if the function is an async function
         */
        boolean isAsync();

        /**
         * Returns {@code true} if the function flag is set for this function.
         * 
         * @param flag
         *            the function flag
         * @return {@code true} if the function flag is set
         */
        boolean is(FunctionFlags flag);

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
         * Returns the parameter names of this function.
         * 
         * @return the formal parameter names or {@code null}
         */
        String[] parameters();

        /**
         * Returns the compressed source string.
         * 
         * @return the compressed source string
         */
        FunctionSource source();

        /**
         * (? extends FunctionObject, ExecutionContext, Object, Object[]) {@literal ->} Object.
         * 
         * @return the method handle for normal calls
         */
        MethodHandle callMethod();

        /**
         * (? extends FunctionObject, ExecutionContext, Constructor, Object[]) {@literal ->} Object.
         * 
         * @return the method handle for construct calls
         */
        MethodHandle constructMethod();

        /**
         * (ExecutionContext, ...?) {@literal ->} Object
         * 
         * @return the method handle for the function body
         */
        MethodHandle handle();
    }

    private static final class CompiledFunction implements Function {
        private final Object methodInfo;
        private final String functionName;
        private final int functionFlags;
        private final int expectedArgumentCount;
        private final String[] parameters;
        private final FunctionSource source;
        private final MethodHandle handle;
        private final MethodHandle callMethod;
        private final MethodHandle constructMethod;
        private final MethodHandle debugInfo;

        CompiledFunction(Object methodInfo, String functionName, int functionFlags, int expectedArgumentCount,
                String[] parameters, String source, MethodHandle handle, MethodHandle callMethod,
                MethodHandle constructMethod, MethodHandle debugInfo) {
            this.methodInfo = methodInfo;
            this.functionName = functionName;
            this.functionFlags = functionFlags;
            this.expectedArgumentCount = expectedArgumentCount;
            this.parameters = parameters;
            this.source = source != null ? new FunctionSource(source) : null;
            this.handle = handle;
            this.callMethod = callMethod;
            this.constructMethod = constructMethod;
            this.debugInfo = debugInfo;
        }

        @Override
        public Object methodInfo() {
            return methodInfo;
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
        public boolean isGenerator() {
            return FunctionFlags.Generator.isSet(functionFlags);
        }

        @Override
        public boolean isAsync() {
            return FunctionFlags.Async.isSet(functionFlags);
        }

        @Override
        public boolean is(FunctionFlags flag) {
            return flag.isSet(functionFlags);
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
        public String[] parameters() {
            return parameters;
        }

        @Override
        public FunctionSource source() {
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

        @Override
        public MethodHandle constructMethod() {
            return constructMethod;
        }

        @Override
        public DebugInfo debugInfo() {
            if (debugInfo != null) {
                try {
                    return (DebugInfo) debugInfo.invokeExact();
                } catch (Throwable e) {
                    throw RuntimeInfo.<RuntimeException> rethrow(e);
                }
            }
            return null;
        }
    }
}
