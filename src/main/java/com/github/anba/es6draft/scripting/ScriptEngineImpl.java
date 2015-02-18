/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.ExecutionContext.newEvalExecutionContext;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

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
import com.github.anba.es6draft.runtime.DeclarativeEnvironmentRecord;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.FileModuleLoader;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.runtime.modules.ModuleLoader;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 * Concrete implementation of the {@link AbstractScriptEngine} abstract class.
 */
final class ScriptEngineImpl extends AbstractScriptEngine implements ScriptEngine, Compilable,
        Invocable {
    private final ScriptEngineFactoryImpl factory;
    private final ScriptLoader evalScriptLoader;
    private final World<ScriptingGlobalObject> world;

    ScriptEngineImpl(ScriptEngineFactoryImpl factory) {
        this.factory = factory;

        Set<CompatibilityOption> compatibilityOptions = CompatibilityOption.WebCompatibility();
        // Scripting sources have an extra scope object before the global environment record, the
        // ScriptContext object. To ensure this extra scope is properly handled, we compile
        // scripting sources with the EvalScript option and use eval-declaration instead of the
        // normal global declaration instantiation when evaluating the source code.
        EnumSet<Parser.Option> parserOptions = EnumSet.of(Parser.Option.EvalScript,
                Parser.Option.Scripting);
        EnumSet<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);
        ScriptLoader evalScriptLoader = new ScriptLoader(compatibilityOptions, parserOptions,
                compilerOptions);

        this.evalScriptLoader = evalScriptLoader;

        ObjectAllocator<ScriptingGlobalObject> allocator = ScriptingGlobalObject
                .newGlobalObjectAllocator();
        ScriptLoader scriptLoader = new ScriptLoader(compatibilityOptions,
                EnumSet.noneOf(Parser.Option.class), compilerOptions);
        ModuleLoader moduleLoader = new FileModuleLoader(scriptLoader, Paths.get("")
                .toAbsolutePath());
        this.world = new World<>(allocator, moduleLoader, scriptLoader);
        context.setBindings(createBindings(), ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public GlobalBindings createBindings() {
        ScriptingGlobalObject global;
        try {
            global = world.newInitializedGlobal();
        } catch (ParserException | CompilationException | IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return new GlobalBindings(global);
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
    public Object invokeFunction(String name, Object... args) throws javax.script.ScriptException,
            NoSuchMethodException {
        return invoke(null, name, args);
    }

    @Override
    public Object invokeMethod(Object thisValue, String name, Object... args)
            throws javax.script.ScriptException, NoSuchMethodException {
        if (!(thisValue instanceof ScriptObject)) {
            throw new IllegalArgumentException();
        }
        return invoke((ScriptObject) thisValue, name, args);
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

    private Script script(String sourceCode, ScriptContext context)
            throws javax.script.ScriptException {
        Source source = createSource(context);
        try {
            return evalScriptLoader.script(source, sourceCode);
        } catch (ParserException e) {
            throw new javax.script.ScriptException(e.getMessage(), e.getFile(), e.getLine(),
                    e.getColumn());
        } catch (CompilationException e) {
            throw new javax.script.ScriptException(e);
        }
    }

    private Script script(Reader reader, ScriptContext context) throws javax.script.ScriptException {
        Source source = createSource(context);
        try {
            return evalScriptLoader.script(source, reader);
        } catch (ParserException e) {
            throw new javax.script.ScriptException(e.getMessage(), e.getFile(), e.getLine(),
                    e.getColumn());
        } catch (CompilationException | IOException e) {
            throw new javax.script.ScriptException(e);
        }
    }

    Object eval(Script script, ScriptContext context) throws javax.script.ScriptException {
        try {
            Realm realm = getEvalRealm(context);
            ExecutionContext cx = realm.defaultContext();
            LexicalEnvironment<ScriptContextEnvironmentRecord> varEnv = new LexicalEnvironment<>(
                    realm.getGlobalEnv(), new ScriptContextEnvironmentRecord(cx, context));
            LexicalEnvironment<DeclarativeEnvironmentRecord> lexEnv = new LexicalEnvironment<>(
                    varEnv, new DeclarativeEnvironmentRecord(cx));
            ExecutionContext evalCxt = newEvalExecutionContext(cx, script, varEnv, lexEnv);
            script.getScriptBody().evalDeclarationInstantiation(evalCxt, varEnv, lexEnv);
            Object result = script.evaluate(evalCxt);
            realm.getWorld().runEventLoop();
            return TypeConverter.toJava(result);
        } catch (ScriptException e) {
            throw new javax.script.ScriptException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new javax.script.ScriptException(e);
        }
    }

    private Object invoke(ScriptObject thisValue, String name, Object... args)
            throws javax.script.ScriptException, NoSuchMethodException {
        if (name == null) {
            throw new NullPointerException();
        }
        Object[] arguments = TypeConverter.fromJava(args);
        Realm realm = getEvalRealm(context);
        if (thisValue == null) {
            thisValue = realm.getGlobalThis();
        }
        try {
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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new javax.script.ScriptException(e);
        }
    }

    private <T> T getInterface(final ScriptObject thisValue, Class<T> clazz) {
        if (clazz == null || !clazz.isInterface()) {
            throw new IllegalArgumentException();
        }
        Object instance = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {
                        Object[] arguments = args != null ? args : new Object[] {};
                        return ScriptEngineImpl.this.invoke(thisValue, method.getName(), arguments);
                    }
                });
        return clazz.cast(instance);
    }

    private Realm getEvalRealm(ScriptContext context) {
        return getBindings(context).getGlobalObject().getRealm();
    }

    private GlobalBindings getBindings(ScriptContext context) {
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        if (bindings instanceof GlobalBindings) {
            // Return engine scope bindings as-is if compatible, i.e. from the same world instance
            GlobalBindings globalBindings = (GlobalBindings) bindings;
            if (globalBindings.getGlobalObject().getRealm().getWorld() == world) {
                return globalBindings;
            }
        }
        // Otherwise create a fresh binding instance
        return createBindings();
    }
}
