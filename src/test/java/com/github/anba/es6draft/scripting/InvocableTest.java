/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.util.matchers.IsInstanceOfWith.instanceOfWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.concurrent.Callable;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Before;
import org.junit.Test;

/**
 * JSR-223 Scripting API tests
 */
public final class InvocableTest {
    private ScriptEngineManager manager;
    private ScriptEngine engine;
    private Invocable invocable;

    @Before
    public void setUp() {
        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("es6draft");
        assertThat(engine, notNullValue());
        assertThat(engine, instanceOf(Invocable.class));
        invocable = (Invocable) engine;
    }

    @Test
    public void invokeFunction() throws NoSuchMethodException, ScriptException {
        engine.eval("function test1() { return 'Centaurus' }");
        engine.eval("function test2(v) { return v + ' Major' }");

        Object result1 = invocable.invokeFunction("test1");
        assertThat(result1, instanceOfWith(String.class, is("Centaurus")));

        Object result2 = invocable.invokeFunction("test1", "...");
        assertThat(result2, instanceOfWith(String.class, is("Centaurus")));

        Object result3 = invocable.invokeFunction("test2");
        assertThat(result3, instanceOfWith(String.class, is("undefined Major")));

        Object result4 = invocable.invokeFunction("test2", "Canis");
        assertThat(result4, instanceOfWith(String.class, is("Canis Major")));
    }

    @Test(expected = NullPointerException.class)
    public void invokeFunctionNullName() throws NoSuchMethodException, ScriptException {
        invocable.invokeFunction(null);
    }

    @Test(expected = NoSuchMethodException.class)
    public void invokeNonExistentFunction() throws NoSuchMethodException, ScriptException {
        invocable.invokeFunction("doesNotExist");
    }

    @Test(expected = NoSuchMethodException.class)
    public void invokeNonFunction() throws NoSuchMethodException, ScriptException {
        invocable.invokeFunction("NaN");
    }

    @Test
    public void invokeMethod() throws NoSuchMethodException, ScriptException {
        Object obj = engine.eval("({ test1() { return 'Cygnus' }, test2(v) { return v + ' Minor' } })");

        Object result1 = invocable.invokeMethod(obj, "test1");
        assertThat(result1, instanceOfWith(String.class, is("Cygnus")));

        Object result2 = invocable.invokeMethod(obj, "test1");
        assertThat(result2, instanceOfWith(String.class, is("Cygnus")));

        Object result3 = invocable.invokeMethod(obj, "test2");
        assertThat(result3, instanceOfWith(String.class, is("undefined Minor")));

        Object result4 = invocable.invokeMethod(obj, "test2", "Canis");
        assertThat(result4, instanceOfWith(String.class, is("Canis Minor")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invokeMethodNullObject() throws NoSuchMethodException, ScriptException {
        invocable.invokeMethod(null, "");
    }

    @Test(expected = NullPointerException.class)
    public void invokeMethodNullName() throws NoSuchMethodException, ScriptException {
        Object obj = engine.eval("({})");
        invocable.invokeMethod(obj, null);
    }

    @Test(expected = NoSuchMethodException.class)
    public void invokeNonExistentMethod() throws NoSuchMethodException, ScriptException {
        Object obj = engine.eval("({})");
        invocable.invokeMethod(obj, "doesNotExist");
    }

    @Test(expected = NoSuchMethodException.class)
    public void invokeNonMethod() throws NoSuchMethodException, ScriptException {
        Object obj = engine.eval("({notFunction: 0})");
        invocable.invokeMethod(obj, "notFunction");
    }

    @Test
    public void getInterfaceCallable() throws ScriptException, Exception {
        engine.eval("function call() { return 'vvv' }");

        Callable<?> callable = invocable.getInterface(Callable.class);
        Object result1 = callable.call();
        assertThat(result1, instanceOfWith(String.class, is("vvv")));
    }

    @Test
    public void getInterfaceObjectCallable() throws ScriptException, Exception {
        Object fn = engine.eval("function fn() { return 'Sagittarius' } fn");

        Callable<?> callable = invocable.getInterface(fn, Callable.class);
        Object result1 = callable.call();
        assertThat(result1, instanceOfWith(String.class, is("Sagittarius")));
    }

    @Test(expected = ScriptException.class)
    public void getInterfaceThrowsError() throws ScriptException, Exception {
        Object fn = engine.eval("function fn() { throw new Error } fn");

        Callable<?> callable = invocable.getInterface(fn, Callable.class);
        callable.call();
    }

    @Test(expected = ScriptException.class)
    public void getInterfaceThrowsPrimitive() throws ScriptException, Exception {
        Object fn = engine.eval("function fn() { throw 123 } fn");

        Callable<?> callable = invocable.getInterface(fn, Callable.class);
        callable.call();
    }
}
