/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.util.matchers.IsInstanceOfWith.instanceOfWith;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.Before;
import org.junit.Test;

/**
 * JSR-223 Scripting API tests
 */
public final class TypeConversionTest {
    private ScriptEngineManager manager;
    private ScriptEngine engine;

    @Before
    public void setUp() {
        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("es6draft");
        assertThat(engine, notNullValue());
    }

    @Test
    public void testNumber() throws ScriptException {
        Object intValue = engine.eval("var intValue = 33; intValue");
        Object doubleValue = engine.eval("var doubleValue = 33.5; doubleValue");

        assertThat(intValue, instanceOf(Number.class));
        assertThat(doubleValue, instanceOf(Number.class));
    }

    @Test
    public void testString() throws ScriptException {
        Object simpleStr = engine.eval("var simpleStr = 'simple'; simpleStr");
        Object concatStr = engine.eval("var concatStr = simpleStr + simpleStr; concatStr");
        Object concat2Str = engine.eval("var concat2Str = concatStr + concatStr; concat2Str");

        assertThat(simpleStr, instanceOf(String.class));
        assertThat(concatStr, instanceOf(String.class));
        assertThat(concat2Str, instanceOf(String.class));
    }

    @Test
    public void testUndefined() throws ScriptException {
        Object undef = engine.eval("undefined");
        assertThat(undef, nullValue());

        Object undef2 = engine.eval("void 0");
        assertThat(undef2, nullValue());
    }

    @Test
    public void testNull() throws ScriptException {
        Object nul = engine.eval("null");
        assertThat(engine.eval("typeof null"), instanceOfWith(String.class, is("object")));
        assertThat(nul, nullValue());

        engine.put("nullValue", null);
        assertThat(engine.eval("typeof nullValue"), instanceOfWith(String.class, is("object")));
        assertThat(engine.eval("nullValue === null"),
                instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
        assertThat(engine.eval("nullValue"), nullValue());
    }

    public static class JavaObject {
    }

    @Test
    public void testUnsupported() throws ScriptException {
        // Unsupported Java classes end up as `null` in default bindings
        Object javaObject = new JavaObject();
        engine.put("javaObject", javaObject);

        assertThat(engine.get("javaObject"), nullValue());
        assertThat(engine.eval("javaObject"), nullValue());

        assertThat(engine.eval("typeof javaObject"), instanceOfWith(String.class, is("object")));
        assertThat(engine.eval("javaObject == null"),
                instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
        assertThat(engine.eval("javaObject === void 0"),
                instanceOfWith(Boolean.class, sameInstance(Boolean.FALSE)));
        assertThat(engine.eval("javaObject === null"),
                instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
    }

    @Test
    public void testUnsupportedWithBindings() throws ScriptException {
        // Unsupported Java classes end up as `null` in default bindings
        Bindings bindings = engine.createBindings();
        Object javaObject = new JavaObject();
        bindings.put("javaObject", javaObject);

        assertThat(bindings.get("javaObject"), nullValue());
        assertThat(engine.eval("javaObject", bindings), nullValue());

        assertThat(engine.eval("typeof javaObject", bindings),
                instanceOfWith(String.class, is("object")));
        assertThat(engine.eval("javaObject == null", bindings),
                instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
        assertThat(engine.eval("javaObject === void 0", bindings),
                instanceOfWith(Boolean.class, sameInstance(Boolean.FALSE)));
        assertThat(engine.eval("javaObject === null", bindings),
                instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
    }

    @Test
    public void testUnsupportedWithSimpleBindings() throws ScriptException {
        // Unsupported Java classes end up as `null` in simple bindings
        Bindings bindings = new SimpleBindings();
        Object javaObject = new JavaObject();
        bindings.put("javaObject", javaObject);

        assertThat(bindings.get("javaObject"), sameInstance(javaObject));
        assertThat(engine.eval("javaObject", bindings), nullValue());

        assertThat(engine.eval("typeof javaObject", bindings),
                instanceOfWith(String.class, is("object")));
        assertThat(engine.eval("javaObject == null", bindings),
                instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
        assertThat(engine.eval("javaObject === void 0", bindings),
                instanceOfWith(Boolean.class, sameInstance(Boolean.FALSE)));
        assertThat(engine.eval("javaObject === null", bindings),
                instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
    }
}
