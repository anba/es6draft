/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.runtime.AbstractOperations.IsCallable;
import static com.github.anba.es6draft.runtime.ExecutionContext.newEvalExecutionContext;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.LexicalEnvironment;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.ScriptObject;

/**
 *
 */
final class ScriptEngineImpl extends AbstractScriptEngine implements ScriptEngine, Compilable,
        Invocable {
    private final ScriptEngineFactoryImpl factory;
    private final ScriptCache scriptCache;
    private final World<ScriptingGlobalObject> world;

    ScriptEngineImpl(ScriptEngineFactoryImpl factory) {
        this.factory = factory;

        Set<CompatibilityOption> compatibilityOptions = CompatibilityOption.WebCompatibility();
        Set<Parser.Option> parserOptions = EnumSet.of(Parser.Option.EvalScript);
        Set<Compiler.Option> compilerOptions = EnumSet.noneOf(Compiler.Option.class);
        ScriptCache scriptCache = new ScriptCache(compatibilityOptions, parserOptions);
        this.scriptCache = scriptCache;

        ObjectAllocator<ScriptingGlobalObject> allocator = ScriptingGlobalObject
                .newGlobalObjectAllocator();
        this.world = new World<>(allocator, compatibilityOptions, compilerOptions);
        context.setBindings(createBindings(), ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public GlobalBindings createBindings() {
        ScriptingGlobalObject global = world.newGlobal();
        global.initialise(global);
        return new GlobalBindings(global);
    }

    @Override
    public Object eval(String script, ScriptContext context) throws javax.script.ScriptException {
        return eval(new StringReader(script), context);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws javax.script.ScriptException {
        return eval(script(reader, context), context);
    }

    @Override
    public CompiledScript compile(String script) throws javax.script.ScriptException {
        return compile(new StringReader(script));
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

    Script script(Reader reader, ScriptContext context) throws javax.script.ScriptException {
        String sourceName = Objects.toString(context.getAttribute(FILENAME), "<eval>");
        int sourceLine = 1;
        try {
            return scriptCache.script(sourceName, sourceLine, reader);
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
            LexicalEnvironment<ScriptContextEnvironmentRecord> lexEnv = new LexicalEnvironment<>(
                    realm.getGlobalEnv(), new ScriptContextEnvironmentRecord(cx, context));
            script.getScriptBody().evalDeclarationInstantiation(cx, lexEnv, lexEnv, true);
            ExecutionContext evalCxt = newEvalExecutionContext(cx, lexEnv, lexEnv);
            Object result = script.evaluate(evalCxt);
            return TypeConverter.toJava(result);
        } catch (ScriptException e) {
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
            return TypeConverter.toJava(result);
        } catch (ScriptException e) {
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
        Bindings bindings = context.getBindings(ScriptContext.ENGINE_SCOPE);
        if (bindings instanceof GlobalBindings) {
            GlobalBindings globalBindings = (GlobalBindings) bindings;
            Realm realm = globalBindings.getGlobalObject().getRealm();
            if (realm.getWorld() == world) {
                return realm;
            }
        }
        return createBindings().getGlobalObject().getRealm();
    }
}
