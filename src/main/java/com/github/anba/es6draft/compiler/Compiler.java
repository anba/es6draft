/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

import com.github.anba.es6draft.ast.AsyncFunctionDefinition;
import com.github.anba.es6draft.ast.AsyncGeneratorDefinition;
import com.github.anba.es6draft.ast.FunctionDefinition;
import com.github.anba.es6draft.ast.FunctionNode;
import com.github.anba.es6draft.ast.GeneratorDefinition;
import com.github.anba.es6draft.ast.Module;
import com.github.anba.es6draft.ast.Program;
import com.github.anba.es6draft.ast.Script;
import com.github.anba.es6draft.ast.scope.Scope;
import com.github.anba.es6draft.ast.scope.ScriptScope;
import com.github.anba.es6draft.compiler.analyzer.CodeSize;
import com.github.anba.es6draft.compiler.assembler.ClassSignature;
import com.github.anba.es6draft.compiler.assembler.Code;
import com.github.anba.es6draft.compiler.assembler.Code.ClassCode;
import com.github.anba.es6draft.compiler.assembler.SourceInfo;
import com.github.anba.es6draft.compiler.assembler.Type;
import com.github.anba.es6draft.compiler.completion.CompletionValueVisitor;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.SourceTextModuleRecord;

/**
 *
 */
public final class Compiler {
    public enum Option {
        DebugInfo, PrintCode, PrintFullCode, IterationCatchStackOverflow, NoCompletion, NoByteCodeSizeValidation,
        NoTailCall, NoInterpreter, SourceMap
    }

    private final RuntimeContext context;

    public Compiler(RuntimeContext context) {
        this.context = context;
    }

    private static final boolean MEASURE_COMPILE_TIME = false;
    private static final boolean MEASURE_LOAD_TIME = false;
    private static final boolean COUNT_CLASSES = false;
    private static final boolean COLLECT_STATISTICS = MEASURE_COMPILE_TIME | MEASURE_LOAD_TIME | COUNT_CLASSES;

    private static final Statistics STATISTICS;

    static {
        if (COLLECT_STATISTICS) {
            STATISTICS = new Statistics();
            Runtime.getRuntime().addShutdownHook(new Thread(STATISTICS::print));
        } else {
            STATISTICS = null;
        }
    }

    private static final class Statistics {
        final LongAdder compileTime = new LongAdder();
        final LongAdder compileTimeScript = new LongAdder();
        final LongAdder compileTimeModule = new LongAdder();
        final LongAdder compileTimeFunction = new LongAdder();

        final LongAdder loadTime = new LongAdder();
        final LongAdder loadTimeScript = new LongAdder();
        final LongAdder loadTimeModule = new LongAdder();
        final LongAdder loadTimeFunction = new LongAdder();

        final LongAdder defaultClasses = new LongAdder();
        final LongAdder anonymousClasses = new LongAdder();

        void print() {
            if (MEASURE_COMPILE_TIME) {
                System.out.printf("Compile-time: %d ms%n", TimeUnit.NANOSECONDS.toMillis(compileTime.longValue()));
                System.out.printf("Compile-time (script): %d ms%n",
                        TimeUnit.NANOSECONDS.toMillis(compileTimeScript.longValue()));
                System.out.printf("Compile-time (module): %d ms%n",
                        TimeUnit.NANOSECONDS.toMillis(compileTimeModule.longValue()));
                System.out.printf("Compile-time (function): %d ms%n",
                        TimeUnit.NANOSECONDS.toMillis(compileTimeFunction.longValue()));
            }
            if (MEASURE_LOAD_TIME) {
                System.out.printf("Load-time: %d ms%n", TimeUnit.NANOSECONDS.toMillis(loadTime.longValue()));
                System.out.printf("Load-time (script): %d ms%n",
                        TimeUnit.NANOSECONDS.toMillis(loadTimeScript.longValue()));
                System.out.printf("Load-time (module): %d ms%n",
                        TimeUnit.NANOSECONDS.toMillis(loadTimeModule.longValue()));
                System.out.printf("Load-time (function): %d ms%n",
                        TimeUnit.NANOSECONDS.toMillis(loadTimeFunction.longValue()));
            }
            if (COUNT_CLASSES) {
                System.out.printf("Default classes: %d%n", defaultClasses.longValue());
                System.out.printf("Anonymous classes: %d%n", anonymousClasses.longValue());
            }
        }
    }

    /**
     * Compiles a script node to Java bytecode.
     * 
     * @param script
     *            the script node
     * @param className
     *            the class name
     * @return the compiled script
     * @throws CompilationException
     *             if the script node could not be compiled
     */
    public CompiledScript compile(Script script, String className) throws CompilationException {
        if (!isEnabled(Compiler.Option.NoByteCodeSizeValidation)) {
            CodeSize.analyze(script);
        }
        if (!isEnabled(Compiler.Option.NoCompletion)) {
            CompletionValueVisitor.performCompletion(script);
        }

        long startCompile = 0;
        if (MEASURE_COMPILE_TIME) {
            startCompile = System.nanoTime();
        }

        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, ClassSignature.NONE, Types.CompiledScript,
                Collections.<Type> emptyList(), NodeSourceInfo.create(script, isEnabled(Option.SourceMap)));
        CodeGenerator codegen = new CodeGenerator(context, code, script);
        try {
            codegen.compile(script);
        } catch (RuntimeException e) {
            throw handleAsmError(e);
        }

        if (MEASURE_COMPILE_TIME) {
            long time = System.nanoTime() - startCompile;
            STATISTICS.compileTime.add(time);
            STATISTICS.compileTimeScript.add(time);
        }

        long startLoad = 0;
        if (MEASURE_LOAD_TIME) {
            startLoad = System.nanoTime();
        }
        CompiledScript compiledScript;
        try {
            if (useAnonymousLoader(code)) {
                compiledScript = defineAndLoad(script, code, AnonymousCodeLoader.SCRIPT);
            } else {
                compiledScript = defineAndLoad(script, code, className);
            }
        } catch (RuntimeException e) {
            throw handleAsmError(e);
        }
        if (MEASURE_LOAD_TIME) {
            long time = System.nanoTime() - startLoad;
            STATISTICS.loadTime.add(time);
            STATISTICS.loadTimeScript.add(time);
        }
        return compiledScript;
    }

    /**
     * Compiles a module node to Java bytecode.
     * 
     * @param module
     *            the module node
     * @param moduleRecord
     *            the module record
     * @param className
     *            the class name
     * @return the compiled module
     * @throws CompilationException
     *             if the module node could not be compiled
     */
    public CompiledModule compile(Module module, SourceTextModuleRecord moduleRecord, String className)
            throws CompilationException {
        if (!isEnabled(Compiler.Option.NoByteCodeSizeValidation)) {
            CodeSize.analyze(module);
        }

        long startCompile = 0;
        if (MEASURE_COMPILE_TIME) {
            startCompile = System.nanoTime();
        }

        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, ClassSignature.NONE, Types.CompiledModule,
                Collections.<Type> emptyList(), NodeSourceInfo.create(module, isEnabled(Option.SourceMap)));
        CodeGenerator codegen = new CodeGenerator(context, code, module);
        try {
            codegen.compile(module, moduleRecord);
        } catch (RuntimeException e) {
            throw handleAsmError(e);
        }

        if (MEASURE_COMPILE_TIME) {
            long time = System.nanoTime() - startCompile;
            STATISTICS.compileTime.add(time);
            STATISTICS.compileTimeModule.add(time);
        }

        long startLoad = 0;
        if (MEASURE_LOAD_TIME) {
            startLoad = System.nanoTime();
        }
        CompiledModule compiledModule;
        try {
            if (useAnonymousLoader(code)) {
                compiledModule = defineAndLoad(module, code, AnonymousCodeLoader.MODULE);
            } else {
                compiledModule = defineAndLoad(module, code, className);
            }
        } catch (RuntimeException e) {
            throw handleAsmError(e);
        }
        if (MEASURE_LOAD_TIME) {
            long time = System.nanoTime() - startLoad;
            STATISTICS.loadTime.add(time);
            STATISTICS.loadTimeModule.add(time);
        }
        return compiledModule;
    }

    /**
     * Compiles a function node to Java bytecode.
     * 
     * @param function
     *            the function node
     * @param className
     *            the class name
     * @return the compiled function
     * @throws CompilationException
     *             if the function node could not be compiled
     */
    public CompiledFunction compile(FunctionDefinition function, String className) throws CompilationException {
        return compile(function, className, CodeGenerator::compileFunction);
    }

    /**
     * Compiles a generator function node to Java bytecode.
     * 
     * @param generator
     *            the generator function node
     * @param className
     *            the class name
     * @return the compiled generator function
     * @throws CompilationException
     *             if the generator function node could not be compiled
     */
    public CompiledFunction compile(GeneratorDefinition generator, String className) throws CompilationException {
        return compile(generator, className, CodeGenerator::compileFunction);
    }

    /**
     * Compiles a async function node to Java bytecode.
     * 
     * @param asyncFunction
     *            the async function node
     * @param className
     *            the class name
     * @return the compiled async function
     * @throws CompilationException
     *             if the async function node could not be compiled
     */
    public CompiledFunction compile(AsyncFunctionDefinition asyncFunction, String className)
            throws CompilationException {
        return compile(asyncFunction, className, CodeGenerator::compileFunction);
    }

    /**
     * Compiles a async generator node to Java bytecode.
     * 
     * @param asyncGenerator
     *            the async generator node
     * @param className
     *            the class name
     * @return the compiled async generator
     * @throws CompilationException
     *             if the async generator node could not be compiled
     */
    public CompiledFunction compile(AsyncGeneratorDefinition asyncGenerator, String className)
            throws CompilationException {
        return compile(asyncGenerator, className, CodeGenerator::compileFunction);
    }

    private <FUNCTION extends FunctionNode> CompiledFunction compile(FUNCTION function, String className,
            BiConsumer<CodeGenerator, FUNCTION> compiler) {
        Script script = functionScript(function);
        if (!isEnabled(Compiler.Option.NoByteCodeSizeValidation)) {
            CodeSize.analyze(function);
        }

        long startCompile = 0;
        if (MEASURE_COMPILE_TIME) {
            startCompile = System.nanoTime();
        }

        Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, ClassSignature.NONE, Types.CompiledFunction,
                Collections.<Type> emptyList(), NodeSourceInfo.create(function, isEnabled(Option.SourceMap)));
        CodeGenerator codegen = new CodeGenerator(context, code, script);
        try {
            compiler.accept(codegen, function);
        } catch (RuntimeException e) {
            throw handleAsmError(e);
        }

        if (MEASURE_COMPILE_TIME) {
            long time = System.nanoTime() - startCompile;
            STATISTICS.compileTime.add(time);
            STATISTICS.compileTimeFunction.add(time);
        }

        long startLoad = 0;
        if (MEASURE_LOAD_TIME) {
            startLoad = System.nanoTime();
        }
        CompiledFunction compiledFunction;
        try {
            if (useAnonymousLoader(code)) {
                compiledFunction = defineAndLoad(script, code, AnonymousCodeLoader.FUNCTION);
            } else {
                compiledFunction = defineAndLoad(script, code, className);
            }
        } catch (RuntimeException e) {
            throw handleAsmError(e);
        }
        if (MEASURE_LOAD_TIME) {
            long time = System.nanoTime() - startLoad;
            STATISTICS.loadTime.add(time);
            STATISTICS.loadTimeFunction.add(time);
        }
        return compiledFunction;
    }

    private static Script functionScript(FunctionNode function) {
        Scope enclosingScope = function.getScope().getEnclosingScope();
        assert enclosingScope instanceof ScriptScope;
        return ((ScriptScope) enclosingScope).getNode();
    }

    private static RuntimeException handleAsmError(RuntimeException e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0 && stackTrace[0].getClassName().startsWith("org.objectweb.asm.")) {
            throw new CompilationException(e.getMessage(), e);
        }
        throw e;
    }

    private boolean isEnabled(Compiler.Option option) {
        return context.isEnabled(option);
    }

    private static final boolean ANONYMOUS_LOADER = false;

    private static boolean useAnonymousLoader(Code code) {
        if (ANONYMOUS_LOADER) {
            return code.getClasses().size() == 1;
        }
        return false;
    }

    private <T> T defineAndLoad(Program program, Code code, AnonymousCodeLoader loader) {
        assert code.getClasses().size() == 1;
        Source source = program.getSource();
        ClassCode classCode = code.getClasses().get(0);
        boolean printCode = isEnabled(Option.PrintCode);
        boolean printSimple = printCode && !isEnabled(Option.PrintFullCode);
        boolean debugInfo = isEnabled(Option.DebugInfo);
        String className = Type.className(classCode.className);
        if (debugInfo) {
            addClassBytesField(classCode);
        }
        byte[] bytes = classCode.toByteArray();
        if (printCode) {
            System.out.println(Code.toByteCode(bytes, printSimple));
        }
        // System.out.printf("define class '%s'%n", className);
        Class<?> c = loader.defineClass(className, bytes);
        if (debugInfo) {
            initializeClassBytes(c, bytes);
        }
        if (COUNT_CLASSES) {
            STATISTICS.anonymousClasses.increment();
        }

        try {
            @SuppressWarnings("unchecked")
            T instance = (T) c.getDeclaredConstructor(Source.class).newInstance(source);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T defineAndLoad(Program program, Code code, String mainClassName) {
        Source source = program.getSource();
        boolean printCode = isEnabled(Option.PrintCode);
        boolean printSimple = printCode && !isEnabled(Option.PrintFullCode);
        boolean debugInfo = isEnabled(Option.DebugInfo);
        CodeLoader loader = new CodeLoader();
        for (ClassCode classCode : code.getClasses()) {
            String className = Type.className(classCode.className);
            if (debugInfo) {
                addClassBytesField(classCode);
            }
            byte[] bytes = classCode.toByteArray();
            if (printCode) {
                System.out.println(Code.toByteCode(bytes, printSimple));
            }
            // System.out.printf("define class '%s'%n", className);
            Class<?> c = loader.defineClass(className, bytes);
            if (debugInfo) {
                initializeClassBytes(c, bytes);
            }
        }
        if (COUNT_CLASSES) {
            STATISTICS.defaultClasses.increment();
        }

        try {
            Class<?> c = loader.loadClass(Type.className(mainClassName));
            @SuppressWarnings("unchecked")
            T instance = (T) c.getDeclaredConstructor(Source.class).newInstance(source);
            return instance;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof StackOverflowError) {
                throw (StackOverflowError) cause;
            }
            throw new RuntimeException(e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addClassBytesField(ClassCode classCode) {
        classCode.addField(Modifier.PRIVATE | Modifier.STATIC, "classBytes", Type.of(byte[].class), null);
    }

    private static void initializeClassBytes(Class<?> c, byte[] bytes) {
        try {
            Field classBytes = c.getDeclaredField("classBytes");
            classBytes.setAccessible(true);
            classBytes.set(null, bytes);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CodeLoader extends ClassLoader {
        CodeLoader() {
            this(ClassLoader.getSystemClassLoader());
        }

        CodeLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> defineClass(String className, byte[] bytes) {
            return defineClass(className, bytes, 0, bytes.length);
        }
    }

    private static final class AnonymousCodeLoader {
        private static final sun.misc.Unsafe UNSAFE = initializeUnsafe();
        static final AnonymousCodeLoader SCRIPT = new AnonymousCodeLoader(Types.CompiledScript);
        static final AnonymousCodeLoader MODULE = new AnonymousCodeLoader(Types.CompiledModule);
        static final AnonymousCodeLoader FUNCTION = new AnonymousCodeLoader(Types.CompiledFunction);

        private final Class<?> hostClass;

        AnonymousCodeLoader(Type superClass) {
            hostClass = createHostClass(superClass);
        }

        private static sun.misc.Unsafe initializeUnsafe() {
            try {
                return AccessController.doPrivileged((PrivilegedExceptionAction<sun.misc.Unsafe>) () -> {
                    Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                    f.setAccessible(true);
                    return (sun.misc.Unsafe) f.get(null);
                });
            } catch (PrivilegedActionException e) {
                throw new ExceptionInInitializerError(e.getException());
            }
        }

        private static Class<?> createHostClass(Type superClass) {
            String className = "#AnonHost";
            SourceInfo sourceInfo = new SourceInfo() {
                @Override
                public String getFileName() {
                    return "AnonHost";
                }

                @Override
                public String getSourceMap() {
                    return null;
                }
            };
            Code code = new Code(Modifier.PUBLIC | Modifier.FINAL, className, ClassSignature.NONE, superClass,
                    Collections.<Type> emptyList(), sourceInfo);
            ClassCode classCode = code.getClasses().get(0);
            CodeLoader loader = new CodeLoader();
            loader.defineClass(className, classCode.toByteArray());
            try {
                return loader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        Class<?> defineClass(String className, byte[] bytes) {
            return UNSAFE.defineAnonymousClass(hostClass, bytes, null);
        }
    }
}
