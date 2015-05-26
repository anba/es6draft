/**
 * Copyright (c) 2012-2015 André Bargull
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
     * @param source
     *            the encoded source string
     * @param bodySourceStart
     *            the body source start index
     * @param handle
     *            the method handle
     * @param callMethod
     *            the call method handle
     * @param constructMethod
     *            the construct method handle
     * @return the new function object
     */
    public static Function newFunction(Object methodInfo, String functionName, int functionFlags,
            int expectedArgumentCount, String source, int bodySourceStart, MethodHandle handle,
            MethodHandle callMethod, MethodHandle constructMethod) {
        return new CompiledFunction(methodInfo, functionName, functionFlags, expectedArgumentCount,
                source, bodySourceStart, handle, callMethod, constructMethod, null);
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
     * @param source
     *            the encoded source string
     * @param bodySourceStart
     *            the body source start index
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
            int expectedArgumentCount, String source, int bodySourceStart, MethodHandle handle,
            MethodHandle callMethod, MethodHandle constructMethod, MethodHandle debugInfo) {
        return new CompiledFunction(methodInfo, functionName, functionFlags, expectedArgumentCount,
                source, bodySourceStart, handle, callMethod, constructMethod, debugInfo);
    }

    /**
     * Returns a new {@link ModuleBody} object.
     * 
     * @param sourceName
     *            the source name
     * @param sourcePath
     *            the source path
     * @param initialization
     *            the initialization method handle
     * @param handle
     *            the code method handle
     * @return the new module object
     */
    public static ModuleBody newModuleBody(String sourceName, String sourcePath,
            MethodHandle initialization, MethodHandle handle) {
        return new CompiledModuleBody(sourceName, sourcePath, initialization, handle, null);
    }

    /**
     * Returns a new {@link ModuleBody} object.
     * 
     * @param sourceName
     *            the source name
     * @param sourcePath
     *            the source path
     * @param initialization
     *            the initialization method handle
     * @param handle
     *            the code method handle
     * @param debugInfo
     *            the debug info method handle
     * @return the new module object
     */
    public static ModuleBody newModuleBody(String sourceName, String sourcePath,
            MethodHandle initialization, MethodHandle handle, MethodHandle debugInfo) {
        return new CompiledModuleBody(sourceName, sourcePath, initialization, handle, debugInfo);
    }

    /**
     * Returns a new {@link ScriptBody} object.
     * 
     * @param sourceName
     *            the source name
     * @param sourcePath
     *            the source path
     * @param isStrict
     *            the strict mode flag
     * @param initialization
     *            the initialization method handle
     * @param handle
     *            the code method handle
     * @return the new script object
     */
    public static ScriptBody newScriptBody(String sourceName, String sourcePath, boolean isStrict,
            MethodHandle initialization, MethodHandle handle) {
        return new CompiledScriptBody(sourceName, sourcePath, isStrict, initialization, handle,
                null);
    }

    /**
     * Returns a new {@link ScriptBody} object.
     * 
     * @param sourceName
     *            the source name
     * @param sourcePath
     *            the source path
     * @param isStrict
     *            the strict mode flag
     * @param initialization
     *            the initialization method handle
     * @param handle
     *            the code method handle
     * @param debugInfo
     *            the debug info method handle
     * @return the new script object
     */
    public static ScriptBody newScriptBody(String sourceName, String sourcePath, boolean isStrict,
            MethodHandle initialization, MethodHandle handle, MethodHandle debugInfo) {
        return new CompiledScriptBody(sourceName, sourcePath, isStrict, initialization, handle,
                debugInfo);
    }

    /**
     * Compiled source object.
     */
    public interface SourceObject {
        /**
         * Returns the source information for this object.
         * 
         * @return the source object
         */
        Source toSource();

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
    public interface ScriptBody extends SourceObject {
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
         */
        void globalDeclarationInstantiation(ExecutionContext cx);

        /**
         * Performs 18.2.1.2 Eval Declaration Instantiation.
         * 
         * @param cx
         *            the execution context
         */
        void evalDeclarationInstantiation(ExecutionContext cx);

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
        private final String sourceName;
        private final String sourceFile;
        private final boolean isStrict;
        private final MethodHandle initialization;
        private final MethodHandle handle;
        private final MethodHandle debugInfo;

        CompiledScriptBody(String sourceName, String sourceFile, boolean isStrict,
                MethodHandle initialization, MethodHandle handle, MethodHandle debugInfo) {
            this.sourceName = sourceName;
            this.sourceFile = sourceFile;
            this.isStrict = isStrict;
            this.initialization = initialization;
            this.handle = handle;
            this.debugInfo = debugInfo;
        }

        @Override
        public Source toSource() {
            return new Source(sourceFile, sourceName, 1);
        }

        @Override
        public boolean isStrict() {
            return isStrict;
        }

        @Override
        public void globalDeclarationInstantiation(ExecutionContext cx) {
            try {
                initialization.invokeExact(cx);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void evalDeclarationInstantiation(ExecutionContext cx) {
            try {
                initialization.invokeExact(cx);
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

        @Override
        public DebugInfo debugInfo() {
            if (debugInfo != null) {
                try {
                    return (DebugInfo) debugInfo.invokeExact();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
    }

    /**
     * Compiled module information.
     */
    public interface ModuleBody extends SourceObject {
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
                LexicalEnvironment<ModuleEnvironmentRecord> env) throws IOException,
                ResolutionException, MalformedNameException;

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
        private final String sourceName;
        private final String sourceFile;
        private final MethodHandle initialization;
        private final MethodHandle handle;
        private final MethodHandle debugInfo;

        CompiledModuleBody(String sourceName, String sourceFile, MethodHandle initialization,
                MethodHandle handle, MethodHandle debugInfo) {
            this.sourceName = sourceName;
            this.sourceFile = sourceFile;
            this.initialization = initialization;
            this.handle = handle;
            this.debugInfo = debugInfo;
        }

        @Override
        public Source toSource() {
            return new Source(sourceFile, sourceName, 1);
        }

        @Override
        public void moduleDeclarationInstantiation(ExecutionContext cx,
                SourceTextModuleRecord module, LexicalEnvironment<ModuleEnvironmentRecord> env)
                throws IOException, ResolutionException, MalformedNameException {
            try {
                initialization.invokeExact(cx, module, env);
            } catch (RuntimeException | Error | IOException | ResolutionException
                    | MalformedNameException e) {
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

        @Override
        public DebugInfo debugInfo() {
            if (debugInfo != null) {
                try {
                    return (DebugInfo) debugInfo.invokeExact();
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
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

        /**
         * Flag for native functions.
         */
        Native(0x10000);

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
        private final int bodyStart;
        private boolean compressed = true;

        private FunctionSource(String source, int bodyStart) {
            this.source = source;
            this.bodyStart = bodyStart;
        }

        /**
         * Returns the function source string.
         * 
         * @return the function source string
         */
        public synchronized String sourceString() {
            if (compressed) {
                try {
                    source = SourceCompressor.decompress(source).call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                compressed = false;
            }
            return source;
        }

        /**
         * Returns the function parameters source string.
         * 
         * @return the function parameters source string
         */
        public String parameters() {
            return sourceString().substring(0, bodyStart);
        }

        /**
         * Returns the function body source string.
         * 
         * @return the function body source string
         */
        public String body() {
            return sourceString().substring(bodyStart);
        }
    }

    /**
     * Compiled function information
     */
    public interface Function {
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
         * @return the method handle for normal construct calls
         */
        MethodHandle constructMethod();

        /**
         * (ExecutionContext, ...?) {@literal ->} Object
         * 
         * @return the method handle for tail calls
         */
        MethodHandle handle();

        /**
         * Returns the debug information or {@code null} if not available.
         * 
         * @return the debug information
         */
        DebugInfo debugInfo();
    }

    private static final class CompiledFunction implements Function {
        private final Object methodInfo;
        private final String functionName;
        private final int functionFlags;
        private final int expectedArgumentCount;
        private final FunctionSource source;
        private final MethodHandle handle;
        private final MethodHandle callMethod;
        private final MethodHandle constructMethod;
        private final MethodHandle debugInfo;

        CompiledFunction(Object methodInfo, String functionName, int functionFlags,
                int expectedArgumentCount, String source, int bodySourceStart, MethodHandle handle,
                MethodHandle callMethod, MethodHandle constructMethod, MethodHandle debugInfo) {
            this.methodInfo = methodInfo;
            this.functionName = functionName;
            this.functionFlags = functionFlags;
            this.expectedArgumentCount = expectedArgumentCount;
            this.source = source != null ? new FunctionSource(source, bodySourceStart) : null;
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
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }
    }
}
