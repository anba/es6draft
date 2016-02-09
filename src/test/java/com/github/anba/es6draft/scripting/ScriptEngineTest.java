/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.util.matchers.IsInstanceOfWith.instanceOfWith;
import static com.github.anba.es6draft.util.matchers.IsNumberCloseTo.numberCloseTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.StringReader;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

/**
 * JSR-223 Scripting API tests
 */
public final class ScriptEngineTest {
    private ScriptEngineManager manager;
    private ScriptEngine engine;

    @Before
    public void setUp() {
        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("es6draft");
        assertThat(engine, notNullValue());
    }

    @Test
    public void contextAndBindings() {
        ScriptContext context = engine.getContext();
        assertThat(context, notNullValue());
        assertThat(context, sameInstance(engine.getContext()));

        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        assertThat(globalScope, notNullValue());
        assertThat(globalScope, sameInstance(engine.getBindings(ScriptContext.GLOBAL_SCOPE)));
        assertThat(globalScope, sameInstance(context.getBindings(ScriptContext.GLOBAL_SCOPE)));
        assertThat(globalScope, sameInstance(manager.getBindings()));

        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        assertThat(engineScope, notNullValue());
        assertThat(engineScope, sameInstance(engine.getBindings(ScriptContext.ENGINE_SCOPE)));
        assertThat(engineScope, sameInstance(context.getBindings(ScriptContext.ENGINE_SCOPE)));
        assertThat(engineScope, not(sameInstance(manager.getBindings())));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidBinding() {
        engine.getBindings(ScriptContext.ENGINE_SCOPE + 1);
    }

    @Test
    public void getAndPut() throws ScriptException {
        final String key1 = "key1", key2 = "key2";
        final String value1 = "value1", value2 = "value2", value3 = "value3";
        Bindings engineScope = engine.getBindings(ScriptContext.ENGINE_SCOPE);

        // key-value is initially null
        assertThat(engine.get(key1), nullValue());
        assertThat(engineScope.get(key1), nullValue());

        // can set from engine key-value and retrieve values from engine and bindings
        engine.put(key1, value1);
        assertThat(engine.get(key1), instanceOfWith(String.class, is(value1)));
        assertThat(engineScope.get(key1), instanceOfWith(String.class, is(value1)));

        // can override from engine key-value and retrieve new values from engine and bindings
        engine.put(key1, value2);
        assertThat(engine.get(key1), instanceOfWith(String.class, is(value2)));
        assertThat(engineScope.get(key1), instanceOfWith(String.class, is(value2)));

        // can override key-value from bindings and retrieve new values from engine and bindings
        Object oldValueA = engineScope.put(key1, value3);
        assertThat(oldValueA, instanceOfWith(String.class, is(value2)));
        assertThat(engine.get(key1), instanceOfWith(String.class, is(value3)));
        assertThat(engineScope.get(key1), instanceOfWith(String.class, is(value3)));

        // can set key-value from bindings and retrieve values from engine and bindings
        Object oldValueB = engineScope.put(key2, value1);
        assertThat(oldValueB, nullValue());
        assertThat(engine.get(key2), instanceOfWith(String.class, is(value1)));
        assertThat(engineScope.get(key2), instanceOfWith(String.class, is(value1)));

        // can override key-value from bindings and retrieve new values from engine and bindings
        Object oldValueC = engineScope.put(key2, value2);
        assertThat(oldValueC, instanceOfWith(String.class, is(value1)));
        assertThat(engine.get(key2), instanceOfWith(String.class, is(value2)));
        assertThat(engineScope.get(key2), instanceOfWith(String.class, is(value2)));

        // can override key-value from engine and retrieve new values from engine and bindings
        engine.put(key2, value3);
        assertThat(engine.get(key2), instanceOfWith(String.class, is(value3)));
        assertThat(engineScope.get(key2), instanceOfWith(String.class, is(value3)));
    }

    @Test(expected = NullPointerException.class)
    public void getNullKey() {
        engine.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getEmptyKey() {
        engine.get("");
    }

    @Test(expected = NullPointerException.class)
    public void putNullKey() {
        engine.put(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void putEmptyKey() {
        engine.put("", 0);
    }

    @Test
    public void evalString() throws ScriptException {
        Object value1 = engine.eval("1 + 2");
        assertThat(value1, instanceOfWith(Number.class, is(numberCloseTo(3))));

        Object value2 = engine.eval("function f() { return 5 * 2 } f()");
        assertThat(value2, instanceOfWith(Number.class, is(numberCloseTo(10))));
    }

    @Test
    public void evalReader() throws ScriptException {
        Object value1 = engine.eval(new StringReader("4 + 2"));
        assertThat(value1, instanceOfWith(Number.class, is(numberCloseTo(6))));

        Object value2 = engine.eval(new StringReader("function f() { return 10 * 3.5 } f()"));
        assertThat(value2, instanceOfWith(Number.class, is(numberCloseTo(35))));
    }

    @Test
    public void evalDeclarations() throws ScriptException {
        engine.eval("function f() { return 'vvv' }");
        Object value = engine.eval("f()");
        assertThat(value, instanceOfWith(String.class, is("vvv")));
    }

    @Test
    public void evalExplicitContext() throws ScriptException {
        ScriptContext context = engine.getContext();
        assertThat(context, notNullValue());

        Object value = engine.eval("function f() { return 'vvv' } f()", context);
        assertThat(value, instanceOfWith(String.class, is("vvv")));
    }

    @Test
    public void evalExplicitBindings() throws ScriptException {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        assertThat(bindings, notNullValue());

        Object value = engine.eval("function f() { return 'vvv' } f()", bindings);
        assertThat(value, instanceOfWith(String.class, is("vvv")));
    }

    @Test(expected = ScriptException.class)
    public void evalMissingDefinition() throws ScriptException {
        engine.eval("f()");
    }

    @Test(expected = ScriptException.class)
    public void evalSyntaxError() throws ScriptException {
        engine.eval("invalid[syntax");
    }

    @Test
    public void processJobQueue() throws ScriptException {
        engine.put("log", "");
        Object value = engine.eval("Promise.resolve().then(() => log += 'b'); log = 'a'; log;");
        assertThat(value, instanceOfWith(String.class, is("a")));
        assertThat(engine.get("log"), instanceOfWith(String.class, is("ab")));
    }

    @Test
    public void processJobQueueWithInvoke() throws ScriptException, NoSuchMethodException {
        engine.put("log", "");
        engine.eval("function F() { Promise.resolve().then(() => log += 'b'); log = 'a'; return log; }");
        Object value = ((Invocable) engine).invokeFunction("F");
        assertThat(value, instanceOfWith(String.class, is("a")));
        assertThat(engine.get("log"), instanceOfWith(String.class, is("ab")));
    }
}
