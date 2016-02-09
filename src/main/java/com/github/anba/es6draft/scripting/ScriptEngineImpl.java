/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.ExecutionContext.newScriptingExecutionContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Objects;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.modules.loader.FileModuleLoader;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Concrete implementation of the {@link AbstractScriptEngine} abstract class.
 */
final class ScriptEngineImpl extends AbstractScriptEngine implements ScriptEngine, Compilable, Invocable {
    private final ScriptEngineFactoryImpl factory;
    // Scripting sources have an extra scope object before the global environment record, the
    // ScriptContext object. To ensure this extra scope is properly handled, we use the
    // 'scripting' parser-option when evaluating the source code.
    private final ScriptLoader scriptingLoader;
    private final World world;

    ScriptEngineImpl(ScriptEngineFactoryImpl factory) {
        this.factory = factory;

        /* @formatter:off */
        RuntimeContext context = new RuntimeContext.Builder()
                                                   .setBaseDirectory(Paths.get("").toAbsolutePath())
                                                   .setGlobalAllocator(ScriptingGlobalObject.newGlobalObjectAllocator())
                                                   .setReader(this.context.getReader())
                                                   .setWriter(printWriter(this.context.getWriter()))
                                                   .setErrorWriter(printWriter(this.context.getErrorWriter()))
                                                   .setOptions(CompatibilityOption.WebCompatibility())
                                                   .setParserOptions(EnumSet.noneOf(Parser.Option.class))
                                                   .setCompilerOptions(EnumSet.noneOf(Compiler.Option.class))
                                                   .build();
        RuntimeContext scriptingContext = new RuntimeContext.Builder(context)
                                                            .setParserOptions(EnumSet.of(Parser.Option.Scripting))
                                                            .build();
        /* @formatter:on */

        ScriptLoader scriptLoader = new ScriptLoader(context);
        ModuleLoader moduleLoader = new FileModuleLoader(context, scriptLoader);
        this.world = new World(context, moduleLoader, scriptLoader);
        this.scriptingLoader = new ScriptLoader(scriptingContext);
        this.context.setBindings(createBindings(), ScriptContext.ENGINE_SCOPE);
    }

    private Realm newScriptingRealm() {
        try {
            return world.newInitializedRealm();
        } catch (ParserException | CompilationException | IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Bindings createBindings() {
        return new GlobalBindings(newScriptingRealm());
    }

    @Override
    public Object eval(String script, ScriptContext context) throws javax.script.ScriptException {
        return eval(script(script, context), context);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws javax.script.ScriptException {
        return eval(script(reader, context), context);
    }

    @Override
    public CompiledScript compile(String script) throws javax.script.ScriptException {
        return new CompiledScriptImpl(this, script(script, context));
    }

    @Override
    public CompiledScript compile(Reader reader) throws javax.script.ScriptException {
        return new CompiledScriptImpl(this, script(reader, context));
    }

    @Override
    public Object invokeFunction(String name, Object... args)
            throws javax.script.ScriptException, NoSuchMethodException {
        return invoke(null, Objects.requireNonNull(name), args);
    }

    @Override
    public Object invokeMethod(Object thisValue, String name, Object... args)
            throws javax.script.ScriptException, NoSuchMethodException {
        if (!(thisValue instanceof ScriptObject)) {
            throw new IllegalArgumentException();
        }
        return invoke((ScriptObject) thisValue, Objects.requireNonNull(name), args);
    }

    @Override
    public <T> T getInterface(Class<T> clazz) {
        return getInterface((ScriptObject) null, clazz);
    }

    @Override
    public <T> T getInterface(Object thisValue, Class<T> clazz) {
        if (!(thisValue instanceof ScriptObject)) {
            throw new IllegalArgumentException();
        }
        return getInterface((ScriptObject) thisValue, clazz);
    }

    private Source createSource(ScriptContext context) {
        String sourceName = Objects.toString(context.getAttribute(FILENAME), "<eval>");
        return new Source(sourceName, 1);
    }

    private Script script(String sourceCode, ScriptContext context) throws javax.script.ScriptException {
        Source source = createSource(context);
        try {
            return scriptingLoader.script(source, sourceCode);
        } catch (ParserException e) {
            throw new javax.script.ScriptException(e.getMessage(), e.getFile(), e.getLine(), e.getColumn());
        } catch (CompilationException e) {
            throw new javax.script.ScriptException(e);
        }
    }

    private Script script(Reader reader, ScriptContext context) throws javax.script.ScriptException {
        Source source = createSource(context);
        try {
            return scriptingLoader.script(source, reader);
        } catch (ParserException e) {
            throw new javax.script.ScriptException(e.getMessage(), e.getFile(), e.getLine(), e.getColumn());
        } catch (CompilationException | IOException e) {
            throw new javax.script.ScriptException(e);
        }
    }

    Object eval(Script script, ScriptContext context) throws javax.script.ScriptException {
        Realm realm = getEvalRealm(context);
        RuntimeContext runtimeContext = realm.getWorld().getContext();
        Reader reader = runtimeContext.getReader();
        Writer writer = runtimeContext.getWriter();
        Writer errorWriter = runtimeContext.getErrorWriter();
        runtimeContext.setReader(context.getReader());
        runtimeContext.setWriter(printWriter(context.getWriter()));
        runtimeContext.setErrorWriter(printWriter(context.getErrorWriter()));
        try {
            // Prepare a new execution context before calling the generated code.
            ExecutionContext evalCxt = newScriptingExecutionContext(realm, script, new LexicalEnvironment<>(
                    realm.getGlobalEnv(), new ScriptContextEnvironmentRecord(realm.defaultContext(), context)));
            Object result = script.evaluate(evalCxt);
            realm.getWorld().runEventLoop();
            return TypeConverter.toJava(result);
        } catch (ScriptException e) {
            throw new javax.script.ScriptException(e);
        } finally {
            runtimeContext.setReader(reader);
            runtimeContext.setWriter(printWriter(writer));
            runtimeContext.setErrorWriter(printWriter(errorWriter));
        }
    }

    private Object invoke(ScriptObject thisValue, String name, Object... args)
            throws javax.script.ScriptException, NoSuchMethodException {
        Realm realm = getEvalRealm(context);
        RuntimeContext runtimeContext = realm.getWorld().getContext();
        Reader reader = runtimeContext.getReader();
        Writer writer = runtimeContext.getWriter();
        Writer errorWriter = runtimeContext.getErrorWriter();
        runtimeContext.setReader(context.getReader());
        runtimeContext.setWriter(printWriter(context.getWriter()));
        runtimeContext.setErrorWriter(printWriter(context.getErrorWriter()));
        try {
            Object[] arguments = TypeConverter.fromJava(args);
            if (thisValue == null) {
                thisValue = realm.getGlobalThis();
            }
            ExecutionContext cx = realm.defaultContext();
            Object func = thisValue.get(cx, name, thisValue);
            if (!IsCallable(func)) {
                throw new NoSuchMethodException(name);
            }
            Object result = ((Callable) func).call(cx, thisValue, arguments);
            realm.getWorld().runEventLoop();
            return TypeConverter.toJava(result);
        } catch (ScriptException e) {
            throw new javax.script.ScriptException(e);
        } finally {
            runtimeContext.setReader(reader);
            runtimeContext.setWriter(printWriter(writer));
            runtimeContext.setErrorWriter(printWriter(errorWriter));
        }
    }

    private <T> T getInterface(final ScriptObject thisValue, Class<T> clazz) {
        if (clazz == null || !clazz.isInterface()) {
            throw new IllegalArgumentException();
        }
        Object instance = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object[] arguments = args != null ? args : new Object[] {};
                        return ScriptEngineImpl.this.invoke(thisValue, method.getName(), arguments);
                    }
                });
        return clazz.cast(instance);
    }

    private Realm getEvalRealm(ScriptContext context) {
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        if (bindings instanceof GlobalBindings) {
            // Return realm from engine scope bindings if compatible, i.e. from the same world instance.
            Realm realm = ((GlobalBindings) bindings).getRealm();
            if (realm.getWorld() == world) {
                return realm;
            }
        }
        // Otherwise create a new realm.
        return newScriptingRealm();
    }

    private static PrintWriter printWriter(Writer writer) {
        if (writer instanceof PrintWriter) {
            return (PrintWriter) writer;
        }
        return new PrintWriter(writer, true);
    }
}
