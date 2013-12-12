/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.runtime.types.Undefined.UNDEFINED;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.objects.GlobalObject;
import com.github.anba.es6draft.runtime.types.Callable;

/**
 * Global object for test262 tests, includes all necessary global function definitions.
 */
public class Test262GlobalObject extends GlobalObject {
    private final Realm realm;
    private final Path libpath;
    private final ScriptCache scriptCache;
    private final Test262Info test;

    public Test262GlobalObject(Realm realm, Path libpath, ScriptCache scriptCache, Test262Info test) {
        super(realm);
        this.realm = realm;
        this.libpath = libpath;
        this.scriptCache = scriptCache;
        this.test = test;
    }

    /**
     * Parses, compiles and executes the javascript file
     */
    public void eval(Path file) throws IOException {
        Script script = scriptCache.script(file.getFileName().toString(), 1, file);
        ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    /**
     * Process test failure with message
     */
    private void failure(String message) {
        String msg = String.format("%s [file: %s]", message, test);
        throw new Test262AssertionError(msg);
    }

    /**
     * {@code $ERROR} function for canonical test262 tests
     */
    @Function(name = "$ERROR", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void error(String message) {
        failure(message);
    }

    /**
     * {@code $FAIL} function for canonical test262 tests
     */
    @Function(name = "$FAIL", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void fail(String message) {
        failure(message);
    }

    /**
     * {@code $PRINT} function for canonical test262 tests
     */
    @Function(name = "$PRINT", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void print(String message) {
        // System.out.println(message);
    }

    /**
     * {@code $INCLUDE} function for canonical test262 tests
     */
    @Function(name = "$INCLUDE", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void include(String file) throws IOException {
        // resolve the input file against the library path
        Path path = libpath.resolve(Paths.get(file));
        Script script = scriptCache.get(path);
        ScriptLoader.ScriptEvaluation(script, realm, false);
    }

    /**
     * {@code runTestCase} function for canonical test262 tests
     */
    @Function(name = "runTestCase", arity = 1, attributes = @Attributes(writable = false,
            enumerable = true, configurable = false))
    public void runTestCase(ExecutionContext cx, Callable testcase) {
        Object value = testcase.call(cx, UNDEFINED);
        if (!ToBoolean(value)) {
            failure(test.getDescription());
        }
    }
}
