/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.util.matchers.IsInstanceOfWith.instanceOfWith;
import static com.github.anba.es6draft.util.matchers.IsNumberCloseTo.numberCloseTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.StringReader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.junit.Before;
import org.junit.Test;

/**
 * JSR-223 Scripting API tests
 */
public class CompilableTest {
    private ScriptEngineManager manager;
    private ScriptEngine engine;
    private Compilable compilable;

    @Before
    public void setUp() {
        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("es6draft");
        assertThat(engine, notNullValue());
        assertThat(engine, instanceOf(Compilable.class));
        compilable = (Compilable) engine;
    }

    @Test
    public void getEngine() throws ScriptException {
        CompiledScript script = compilable.compile("numberVal - 1");

        assertThat(script, notNullValue());
        assertThat(script.getEngine(), sameInstance(engine));
    }

    @Test
    public void compileString() throws ScriptException {
        CompiledScript script = compilable.compile("numberVal * 2");

        assertThat(script, notNullValue());
        engine.put("numberVal", 10);
        assertThat(script.eval(), instanceOfWith(Number.class, is(numberCloseTo(20))));
    }

    @Test
    public void compileReader() throws ScriptException {
        CompiledScript script = compilable.compile(new StringReader("numberVal * 4"));

        assertThat(script, notNullValue());
        engine.put("numberVal", 10);
        assertThat(script.eval(), instanceOfWith(Number.class, is(numberCloseTo(40))));
    }

    @Test
    public void compileStringWithBindings() throws ScriptException {
        CompiledScript script = compilable.compile("numberVal * 2");
        Bindings bindings = engine.createBindings();
        bindings.put("numberVal", 5);

        assertThat(script.eval(bindings), instanceOfWith(Number.class, is(numberCloseTo(10))));
    }

    @Test
    public void compileStringWithSimpleBindings() throws ScriptException {
        CompiledScript script = compilable.compile("numberVal * 2");
        Bindings bindings = new SimpleBindings();
        bindings.put("numberVal", 6);

        assertThat(script.eval(bindings), instanceOfWith(Number.class, is(numberCloseTo(12))));
    }

    @Test
    public void compileStringWithContext() throws ScriptException {
        CompiledScript script = compilable.compile("numberVal * 2");
        ScriptContext context = new SimpleScriptContext();
        context.setAttribute("numberVal", 7, ScriptContext.ENGINE_SCOPE);

        assertThat(script.eval(context), instanceOfWith(Number.class, is(numberCloseTo(14))));
    }

    @Test
    public void compileStringWithContextAndBindings() throws ScriptException {
        CompiledScript script = compilable.compile("numberVal * 2");
        ScriptContext context = new SimpleScriptContext();
        Bindings bindings = engine.createBindings();
        bindings.put("numberVal", 8);
        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        assertThat(script.eval(context), instanceOfWith(Number.class, is(numberCloseTo(16))));
    }

    @Test
    public void compileStringWithContextAndSimpleBindings() throws ScriptException {
        CompiledScript script = compilable.compile("numberVal * 2");
        ScriptContext context = new SimpleScriptContext();
        Bindings bindings = new SimpleBindings();
        bindings.put("numberVal", 9);
        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        assertThat(script.eval(context), instanceOfWith(Number.class, is(numberCloseTo(18))));
    }
}
