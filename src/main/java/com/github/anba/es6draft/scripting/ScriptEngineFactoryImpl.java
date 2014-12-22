/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Concrete implementation of the {@link ScriptEngineFactory} interface.
 */
public final class ScriptEngineFactoryImpl implements ScriptEngineFactory {
    @Override
    public String getEngineName() {
        return "es6draft";
    }

    @Override
    public String getEngineVersion() {
        return getResourceInfo("/version", "<unknown version>");
    }

    @Override
    public List<String> getExtensions() {
        // http://www.ietf.org/rfc/rfc4329.txt
        return unmodifiableList(asList("js", "es"));
    }

    @Override
    public List<String> getMimeTypes() {
        // http://www.ietf.org/rfc/rfc4329.txt
        return unmodifiableList(asList("application/javascript", "text/javascript",
                "application/ecmascript", "text/ecmascript"));
    }

    @Override
    public List<String> getNames() {
        return unmodifiableList(asList("es6draft", "ES6Draft", "JavaScript", "javascript",
                "ECMAScript", "ecmascript"));
    }

    @Override
    public String getLanguageName() {
        return "ECMAScript";
    }

    @Override
    public String getLanguageVersion() {
        return "ECMA-262 6th Edition / Draft December 6, 2014";
    }

    @Override
    public Object getParameter(String key) {
        switch (key) {
        case ScriptEngine.ENGINE:
            return getEngineName();
        case ScriptEngine.ENGINE_VERSION:
            return getEngineVersion();
        case ScriptEngine.NAME:
            return getNames().get(0);
        case ScriptEngine.LANGUAGE:
            return getLanguageName();
        case ScriptEngine.LANGUAGE_VERSION:
            return getLanguageVersion();
        case "THREADING":
            return null;
        default:
            return null;
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder methodCall = new StringBuilder();
        methodCall.append(obj).append('.').append(m).append('(');
        for (int i = 0, len = args.length; i < len; ++i) {
            if (i != 0) {
                methodCall.append(", ");
            }
            methodCall.append(args[i]);
        }
        return methodCall.append(')').toString();

    }

    @Override
    public String getOutputStatement(String toDisplay) {
        String escapedDisplay = toDisplay.replaceAll("([\"\\\\])", "\\\\$1");
        return "print(\"" + escapedDisplay + "\")";
    }

    @Override
    public String getProgram(String... statements) {
        StringBuilder program = new StringBuilder();
        for (String statement : statements) {
            program.append(statement).append(";\n");
        }
        return program.toString();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new ScriptEngineImpl(this);
    }

    private static String getResourceInfo(String resourceName, String defaultValue) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ScriptEngineFactoryImpl.class.getResourceAsStream(resourceName),
                StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            return defaultValue;
        }
    }
}
