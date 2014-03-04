/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static com.github.anba.es6draft.util.IsInstanceOfWith.instanceOfWith;
import static com.github.anba.es6draft.util.IsNumberCloseTo.numberCloseTo;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * JSR-223 Scripting API tests
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScriptEngineScopeTest {
    private ScriptEngineManager manager;
    private ScriptEngine engine;

    @Before
    public void setUp() {
        manager = new ScriptEngineManager();
        engine = manager.getEngineByName("es6draft");
        assertThat(engine, notNullValue());
    }

    @Test
    public void implicitDefaultBindingsAndEval() throws ScriptException {
        engine.put("nullValue", null);
        engine.put("intValue", 1);
        engine.put("doubleValue", 1.5);
        engine.put("booleanValue", true);
        engine.put("stringValue", "Cepheus");

        Object nullValue = engine.eval("nullValue");
        Object intValue = engine.eval("intValue");
        Object doubleValue = engine.eval("doubleValue");
        Object stringValue = engine.eval("stringValue");
        Object booleanValue = engine.eval("booleanValue");

        assertThat(nullValue, nullValue());
        assertThat(intValue, instanceOfWith(Number.class, is(numberCloseTo(1))));
        assertThat(doubleValue, instanceOfWith(Number.class, is(numberCloseTo(1.5))));
        assertThat(stringValue, instanceOfWith(String.class, is("Cepheus")));
        assertThat(booleanValue, instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
    }

    @Test
    public void explicitDefaultBindingsAndEval() throws ScriptException {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        assertThat(bindings, notNullValue());
        assertThat(bindings.entrySet(), is(empty()));

        bindings.put("nullValue", null);
        bindings.put("intValue", 1);
        bindings.put("doubleValue", 1.5);
        bindings.put("booleanValue", true);
        bindings.put("stringValue", "Cepheus");

        Object nullValue = engine.eval("nullValue", bindings);
        Object intValue = engine.eval("intValue", bindings);
        Object doubleValue = engine.eval("doubleValue", bindings);
        Object stringValue = engine.eval("stringValue", bindings);
        Object booleanValue = engine.eval("booleanValue", bindings);

        assertThat(nullValue, nullValue());
        assertThat(intValue, instanceOfWith(Number.class, is(numberCloseTo(1))));
        assertThat(doubleValue, instanceOfWith(Number.class, is(numberCloseTo(1.5))));
        assertThat(stringValue, instanceOfWith(String.class, is("Cepheus")));
        assertThat(booleanValue, instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
    }

    @Test
    public void createBindingsAndEval() throws ScriptException {
        Bindings bindings = engine.createBindings();
        assertThat(bindings, notNullValue());
        assertThat(bindings.entrySet(), is(empty()));

        bindings.put("nullValue", null);
        bindings.put("intValue", 1);
        bindings.put("doubleValue", 1.5);
        bindings.put("booleanValue", true);
        bindings.put("stringValue", "Cepheus");

        Object nullValue = engine.eval("nullValue", bindings);
        Object intValue = engine.eval("intValue", bindings);
        Object doubleValue = engine.eval("doubleValue", bindings);
        Object stringValue = engine.eval("stringValue", bindings);
        Object booleanValue = engine.eval("booleanValue", bindings);

        assertThat(nullValue, nullValue());
        assertThat(intValue, instanceOfWith(Number.class, is(numberCloseTo(1))));
        assertThat(doubleValue, instanceOfWith(Number.class, is(numberCloseTo(1.5))));
        assertThat(stringValue, instanceOfWith(String.class, is("Cepheus")));
        assertThat(booleanValue, instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
    }

    @Test
    public void simpleBindingsAndEval() throws ScriptException {
        Bindings bindings = new SimpleBindings();
        assertThat(bindings, notNullValue());
        assertThat(bindings.entrySet(), is(empty()));

        bindings.put("nullValue", null);
        bindings.put("intValue", 1);
        bindings.put("doubleValue", 1.5);
        bindings.put("booleanValue", true);
        bindings.put("stringValue", "Cepheus");

        Object nullValue = engine.eval("nullValue", bindings);
        Object intValue = engine.eval("intValue", bindings);
        Object doubleValue = engine.eval("doubleValue", bindings);
        Object stringValue = engine.eval("stringValue", bindings);
        Object booleanValue = engine.eval("booleanValue", bindings);

        assertThat(nullValue, nullValue());
        assertThat(intValue, instanceOfWith(Number.class, is(numberCloseTo(1))));
        assertThat(doubleValue, instanceOfWith(Number.class, is(numberCloseTo(1.5))));
        assertThat(stringValue, instanceOfWith(String.class, is("Cepheus")));
        assertThat(booleanValue, instanceOfWith(Boolean.class, sameInstance(Boolean.TRUE)));
    }

    @Test
    public void implicitDefaultBindingsRetrieval() throws ScriptException {
        engine.eval("var a = 0; b = 1");

        assertThat(engine.get("a"), instanceOfWith(Number.class, is(numberCloseTo(0))));
        assertThat(engine.get("b"), instanceOfWith(Number.class, is(numberCloseTo(1))));
    }

    @Test
    public void explicitDefaultBindingsRetrieval() throws ScriptException {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        engine.eval("var a = 0; b = 1", bindings);

        assertThat(bindings.get("a"), instanceOfWith(Number.class, is(numberCloseTo(0))));
        assertThat(bindings.get("b"), instanceOfWith(Number.class, is(numberCloseTo(1))));
    }

    @Test
    public void createBindingsRetrieval() throws ScriptException {
        Bindings bindings = engine.createBindings();
        engine.eval("var a = 0; b = 1", bindings);

        assertThat(bindings.get("a"), instanceOfWith(Number.class, is(numberCloseTo(0))));
        assertThat(bindings.get("b"), instanceOfWith(Number.class, is(numberCloseTo(1))));
    }

    @Test
    public void simpleBindingsRetrieval() throws ScriptException {
        Bindings bindings = new SimpleBindings();
        engine.eval("var a = 0; b = 3", bindings);

        assertThat(bindings.get("a"), instanceOfWith(Number.class, is(numberCloseTo(0))));
        assertThat(bindings.get("b"), nullValue());
    }

    @Test
    public void accessToBuiltins() {
        Object engineObject = engine.get("Object");
        Object bindingsObject = engine.getBindings(ScriptContext.ENGINE_SCOPE).get("Object");

        assertThat(engineObject, notNullValue());
        assertThat(engineObject, sameInstance(bindingsObject));
    }

    @Test
    public void accessToBuiltinsNonSharedContext() throws ScriptException {
        Object defaultObject = engine.eval("Object");
        Object contextObject = engine.eval("Object", new SimpleScriptContext());

        assertThat(defaultObject, notNullValue());
        assertThat(contextObject, notNullValue());
        assertThat(defaultObject, Matchers.not(sameInstance(contextObject)));
    }

    @Test
    public void accessToBuiltinsNonSharedBindings() throws ScriptException {
        Object defaultObject = engine.eval("Object");
        Object bindingsObject = engine.eval("Object", engine.createBindings());

        assertThat(defaultObject, notNullValue());
        assertThat(bindingsObject, notNullValue());
        assertThat(defaultObject, Matchers.not(sameInstance(bindingsObject)));
    }

    @Test
    public void accessToBuiltinsNonSharedSimpleBindings() throws ScriptException {
        Object defaultObject = engine.eval("Object");
        Object bindingsObject = engine.eval("Object", new SimpleBindings());

        assertThat(defaultObject, notNullValue());
        assertThat(bindingsObject, notNullValue());
        assertThat(defaultObject, Matchers.not(sameInstance(bindingsObject)));
    }

    @Test
    public void isolatedContextsEvalDefault() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        engine.eval("var value = 'Sirius'");

        assertThat(engine.eval("value"), instanceOfWith(String.class, is("Sirius")));
        assertThat(engine.eval("typeof value", context),
                instanceOfWith(String.class, is("undefined")));
    }

    @Test
    public void isolatedContextsEvalContext() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        engine.eval("var value = 'Vega'", context);

        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Vega")));
        assertThat(engine.eval("typeof value"), instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", new SimpleScriptContext()),
                instanceOfWith(String.class, is("undefined")));
    }

    @Test
    public void isolatedContextsEvalContextWithDefaults() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        engine.eval("var value = 'Arcturus'", context);

        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Arcturus")));
        assertThat(engine.eval("typeof value"), instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", new SimpleScriptContext()),
                instanceOfWith(String.class, is("undefined")));
    }

    @Test
    public void globalScopeAccess() throws ScriptException {
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        globalScope.put("globalVar", "Aldebaran");

        assertThat(engine.eval("globalVar"), instanceOfWith(String.class, is("Aldebaran")));
    }

    @Test
    public void globalScopeAccessNewEmptyContext() throws ScriptException {
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        globalScope.put("globalVar", "Gacrux");

        assertThat(engine.eval("typeof globalVar", new SimpleScriptContext()),
                instanceOfWith(String.class, is("undefined")));
    }

    @Test
    public void globalScopeAccessNewContextWithGlobal() throws ScriptException {
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        globalScope.put("globalVar", "Achernar");

        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        context.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);
        assertThat(engine.eval("globalVar", context), instanceOfWith(String.class, is("Achernar")));
    }

    @Test
    public void globalScopeAccessNewContextWithGlobalSimpleBindings() throws ScriptException {
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        globalScope.put("globalVar", "Sigma Sagittarii");

        ScriptContext context = new SimpleScriptContext();
        context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        context.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);
        assertThat(engine.eval("globalVar", context),
                instanceOfWith(String.class, is("Sigma Sagittarii")));
    }

    @Test
    public void globalScopeAccessWithOverride() throws ScriptException {
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        globalScope.put("globalVar", "Bellatrix");

        engine.getBindings(ScriptContext.ENGINE_SCOPE).put("globalVar", "Eta Centauri");
        assertThat(engine.eval("globalVar"), instanceOfWith(String.class, is("Eta Centauri")));

        engine.getBindings(ScriptContext.ENGINE_SCOPE).remove("globalVar");
        assertThat(engine.eval("globalVar"), instanceOfWith(String.class, is("Bellatrix")));
    }

    @Test
    public void globalScopeAccessWithOverrideNewContext() throws ScriptException {
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        globalScope.put("globalVar", "Canopus");

        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        context.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

        context.getBindings(ScriptContext.ENGINE_SCOPE).put("globalVar", "Acrux");
        assertThat(engine.eval("globalVar", context), instanceOfWith(String.class, is("Acrux")));

        engine.getBindings(ScriptContext.ENGINE_SCOPE).remove("globalVar");
        assertThat(engine.eval("globalVar"), instanceOfWith(String.class, is("Canopus")));
    }

    @Test
    public void globalScopeAccessWithOverrideNewContextSimpleBindings() throws ScriptException {
        Bindings globalScope = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        globalScope.put("globalVar", "Becrux");

        ScriptContext context = new SimpleScriptContext();
        context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        context.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);

        context.getBindings(ScriptContext.ENGINE_SCOPE).put("globalVar", "Antares");
        assertThat(engine.eval("globalVar", context), instanceOfWith(String.class, is("Antares")));

        engine.getBindings(ScriptContext.ENGINE_SCOPE).remove("globalVar");
        assertThat(engine.eval("globalVar"), instanceOfWith(String.class, is("Becrux")));
    }

    @Test
    public void isolatedBindings() throws ScriptException {
        Bindings binding = engine.createBindings();
        engine.eval("var value = 'Betelgeuse'", binding);

        assertThat(engine.eval("value", binding), instanceOfWith(String.class, is("Betelgeuse")));
        assertThat(engine.eval("typeof value"), instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", engine.createBindings()),
                instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", new SimpleBindings()),
                instanceOfWith(String.class, is("undefined")));
    }

    @Test
    public void isolatedBindingsAndSimpleBindings() throws ScriptException {
        Bindings binding = new SimpleBindings();
        engine.eval("var value = 'Alnitak'", binding);

        assertThat(engine.eval("value", binding), instanceOfWith(String.class, is("Alnitak")));
        assertThat(engine.eval("typeof value"), instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", engine.createBindings()),
                instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", new SimpleBindings()),
                instanceOfWith(String.class, is("undefined")));
    }

    @Test
    public void isolatedBindingsWithContext() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        Bindings binding = engine.createBindings();
        context.setBindings(binding, ScriptContext.ENGINE_SCOPE);
        engine.eval("var value = 'Alnilam'", context);

        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Alnilam")));

        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        assertThat(engine.eval("typeof value", context),
                instanceOfWith(String.class, is("undefined")));

        context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        assertThat(engine.eval("typeof value", context),
                instanceOfWith(String.class, is("undefined")));

        context.setBindings(binding, ScriptContext.ENGINE_SCOPE);
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Alnilam")));
    }

    @Test
    public void isolatedBindingsWithContextAndSimpleBindings() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        Bindings binding = new SimpleBindings();
        context.setBindings(binding, ScriptContext.ENGINE_SCOPE);
        engine.eval("var value = 'Zeta Scorpii'", context);

        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Zeta Scorpii")));

        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        assertThat(engine.eval("typeof value", context),
                instanceOfWith(String.class, is("undefined")));

        context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        assertThat(engine.eval("typeof value", context),
                instanceOfWith(String.class, is("undefined")));

        context.setBindings(binding, ScriptContext.ENGINE_SCOPE);
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Zeta Scorpii")));
    }

    @Test
    public void isolatedContextsNoVarEvalDefault() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        engine.eval("value = 'Rigel'");

        assertThat(engine.eval("value"), instanceOfWith(String.class, is("Rigel")));
        assertThat(engine.eval("typeof value", context),
                instanceOfWith(String.class, is("undefined")));

        assertThat(engine.getContext().getAttribute("value"),
                instanceOfWith(String.class, is("Rigel")));
        assertThat(context.getAttribute("value"), nullValue());
    }

    @Test
    public void isolatedContextsNoVarEvalContext() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        engine.eval("value = 'Deneb'", context);

        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Deneb")));
        assertThat(engine.eval("typeof value"), instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", new SimpleScriptContext()),
                instanceOfWith(String.class, is("undefined")));

        assertThat(engine.getContext().getAttribute("value"), nullValue());
        assertThat(context.getAttribute("value"), instanceOfWith(String.class, is("Deneb")));
    }

    @Test
    public void isolatedContextsNoVarEvalContextWithDefaults() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        engine.eval("value = 'Polaris'", context);

        assertThat(engine.eval("typeof value", context),
                instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value"), instanceOfWith(String.class, is("undefined")));
        assertThat(engine.eval("typeof value", new SimpleScriptContext()),
                instanceOfWith(String.class, is("undefined")));

        assertThat(engine.getContext().getAttribute("value"), nullValue());
        assertThat(context.getAttribute("value"), nullValue());
    }

    @Test
    public void bindingValueWithThis() throws ScriptException {
        engine.put("value", "Gienah");

        assertThat(engine.eval("this.value"), instanceOfWith(String.class, is("Gienah")));
    }

    @Test
    public void bindingValueWithThisExplicitContext() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Markab", ScriptContext.ENGINE_SCOPE);

        assertThat(engine.eval("this.value", context), instanceOfWith(String.class, is("Markab")));
    }

    @Test
    public void bindingValueWithThisExplicitContextSimpleBindings() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setAttribute("value", "Markeb", ScriptContext.ENGINE_SCOPE);

        assertThat(engine.eval("this.value", context), nullValue());
    }

    @Test
    public void bindingValueWithThisExplicitBinding() throws ScriptException {
        Bindings binding = engine.createBindings();
        binding.put("value", "Alderamin");

        assertThat(engine.eval("this.value", binding),
                instanceOfWith(String.class, is("Alderamin")));
    }

    @Test
    public void bindingValueWithThisExplicitSimpleBinding() throws ScriptException {
        Bindings binding = new SimpleBindings();
        binding.put("value", "Sabik");

        assertThat(engine.eval("this.value", binding), nullValue());
    }

    @Test
    public void scopeInteractionDefaultContextGlobalAssignment() throws ScriptException {
        ScriptContext context = engine.getContext();
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionDefaultContextPropertyAssignment() throws ScriptException {
        ScriptContext context = engine.getContext();
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("this.value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionDefaultContextVarAssignment() throws ScriptException {
        ScriptContext context = engine.getContext();
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("var value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionNewContextGlobalAssignment() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE),
                ScriptContext.GLOBAL_SCOPE);
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionNewContextPropertyAssignment() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE),
                ScriptContext.GLOBAL_SCOPE);
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("this.value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionNewContextVarAssignment() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE),
                ScriptContext.GLOBAL_SCOPE);
        context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("var value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionNewContextSimpleBindingsGlobalAssignment() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE),
                ScriptContext.GLOBAL_SCOPE);
        context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), nullValue());
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionNewContextSimpleBindingsPropertyAssignment() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE),
                ScriptContext.GLOBAL_SCOPE);
        context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("this.value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), nullValue());
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Phecda")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Phecda")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }

    @Test
    public void scopeInteractionNewContextSimpleBindingsVarAssignment() throws ScriptException {
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(engine.getBindings(ScriptContext.GLOBAL_SCOPE),
                ScriptContext.GLOBAL_SCOPE);
        context.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Phecda", ScriptContext.ENGINE_SCOPE);
        context.setAttribute("value", "Scheat", ScriptContext.GLOBAL_SCOPE);

        engine.eval("var value = 'Aludra'", context);

        assertThat(engine.eval("this.value", context), nullValue());
        assertThat(engine.eval("value", context), instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.ENGINE_SCOPE),
                instanceOfWith(String.class, is("Aludra")));
        assertThat(context.getAttribute("value", ScriptContext.GLOBAL_SCOPE),
                instanceOfWith(String.class, is("Scheat")));
    }
}
