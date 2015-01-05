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
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.Before;
import org.junit.Test;

/**
 * JSR-223 Scripting API tests
 */
public class ScriptEngineFactoryTest {
    private static final String ENGINE_NAME = "es6draft";
    private static final String LANGUAGE_VERSION = "ECMA-262 6th Edition";
    private static final String LANGUAGE_NAME = "ECMAScript";
    private static final String COMMON_NAME = "JavaScript";

    private ScriptEngineFactory factory;

    @Before
    public void setUp() {
        ScriptEngineManager manager = new ScriptEngineManager();
        for (ScriptEngineFactory factory : manager.getEngineFactories()) {
            if (ENGINE_NAME.equals(factory.getEngineName())) {
                this.factory = factory;
                return;
            }
        }
        fail("script engine factory not found");
    }

    @Test
    public void testGetEngineName() {
        assertThat(factory.getEngineName(), is(ENGINE_NAME));
    }

    @Test
    public void testGetEngineVersion() {
        assertThat(factory.getEngineVersion(), notNullValue());
    }

    @Test
    public void testGetExtensions() {
        List<String> extensions = factory.getExtensions();
        assertThat(extensions, containsInAnyOrder("js", "es"));
    }

    @Test
    public void testGetMimeTypes() {
        List<String> mimeTypes = factory.getMimeTypes();
        assertThat(
                mimeTypes,
                containsInAnyOrder("application/javascript", "application/ecmascript",
                        "text/javascript", "text/ecmascript"));
    }

    @Test
    public void testGetNames() {
        List<String> names = factory.getNames();
        assertThat(names, hasItem(ENGINE_NAME));
        assertThat(names, hasItems(LANGUAGE_NAME, LANGUAGE_NAME.toLowerCase(Locale.ROOT)));
        assertThat(names, hasItems(COMMON_NAME, COMMON_NAME.toLowerCase(Locale.ROOT)));
    }

    @Test
    public void testGetLanguageName() {
        assertThat(factory.getLanguageName(), is(LANGUAGE_NAME));
    }

    @Test
    public void testGetLanguageVersion() {
        assertThat(factory.getLanguageVersion(), containsString(LANGUAGE_VERSION));
    }

    @Test
    public void testGetParameter() {
        // getNames()
        assertThat(factory.getParameter(ScriptEngine.NAME),
                instanceOfWith(String.class, is(ENGINE_NAME)));

        // getEngineName()
        assertThat(factory.getParameter(ScriptEngine.ENGINE),
                instanceOfWith(String.class, is(ENGINE_NAME)));

        // getEngineVersion()
        assertThat(factory.getParameter(ScriptEngine.ENGINE_VERSION),
                instanceOfWith(String.class, notNullValue()));

        // getLanguageName
        assertThat(factory.getParameter(ScriptEngine.LANGUAGE),
                instanceOfWith(String.class, is(LANGUAGE_NAME)));

        // getLanguageVersion
        assertThat(factory.getParameter(ScriptEngine.LANGUAGE_VERSION),
                instanceOfWith(String.class, hasToString(containsString(LANGUAGE_VERSION))));

        // Other parameters
        assertThat(factory.getParameter("THREADING"), nullValue());
    }

    @Test
    public void testGetMethodCallSyntax() {
        assertThat(factory.getMethodCallSyntax("obj", "m"), is("obj.m()"));
        assertThat(factory.getMethodCallSyntax("obj", "m", "arg1"), is("obj.m(arg1)"));
        assertThat(factory.getMethodCallSyntax("obj", "m", "arg1", "arg2"), is("obj.m(arg1, arg2)"));

    }

    @Test
    public void testGetOutputStatement() {
        assertThat(factory.getOutputStatement(""), is("print(\"\")"));
        assertThat(factory.getOutputStatement("test"), is("print(\"test\")"));
        assertThat(factory.getOutputStatement("\""), is("print(\"\\\"\")"));
        assertThat(factory.getOutputStatement("\"\""), is("print(\"\\\"\\\"\")"));
        assertThat(factory.getOutputStatement("\\"), is("print(\"\\\\\")"));
        assertThat(factory.getOutputStatement("\\\\"), is("print(\"\\\\\\\\\")"));
    }

    @Test
    public void testGetProgram() {
        assertThat(factory.getProgram(), is(""));
        assertThat(factory.getProgram("fn()", "while(1){}"), is("fn();\n" + "while(1){};\n"));
    }

    @Test
    public void testGetScriptEngine() {
        ScriptEngine scriptEngine = factory.getScriptEngine();
        ScriptEngine scriptEngine2 = factory.getScriptEngine();

        assertThat(scriptEngine, notNullValue());
        assertThat(scriptEngine2, notNullValue());
        assertThat(scriptEngine, not(sameInstance(scriptEngine2)));
        assertThat(scriptEngine.getFactory(), is(factory));
        assertThat(scriptEngine2.getFactory(), is(factory));
    }
}
