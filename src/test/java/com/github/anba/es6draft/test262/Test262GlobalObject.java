/**
 * Copyright (c) 2012-2014 André Bargull
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
import com.github.anba.es6draft.Scripts;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Attributes;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.types.Callable;

/**
 * Global object for test262 tests, includes all necessary global function definitions.
 */
public class Test262GlobalObject extends ShellGlobalObject {
    private final Test262Info test;

    public Test262GlobalObject(Realm realm, ShellConsole console, Test262Info test,
            ScriptCache scriptCache) {
        super(realm, console, test.getBaseDir(), test.getScript(), scriptCache);
        this.test = test;
    }

    /**
     * Returns an object to allocate new instances of this class
     */
    public static ObjectAllocator<Test262GlobalObject> newGlobalObjectAllocator(
            final ShellConsole console, final Test262Info test, final ScriptCache scriptCache) {
        return new ObjectAllocator<Test262GlobalObject>() {
            @Override
            public Test262GlobalObject newInstance(Realm realm) {
                return new Test262GlobalObject(realm, console, test, scriptCache);
            }
        };
    }

    /**
     * Parses, compiles and executes the javascript file.
     * 
     * @param fileName
     *            the file name for the script file
     * @param source
     *            the source code
     * @param sourceLine
     *            the source line offset
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    void eval(Path fileName, String source, int sourceLine) throws ParserException,
            CompilationException {
        Script script = getScriptLoader().script(fileName.toString(), sourceLine, source);
        Scripts.ScriptEvaluation(script, getRealm(), false);
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
    @Function(name = "$ERROR", arity = 1, attributes = @Attributes(writable = true,
            enumerable = true, configurable = false))
    public void error(String message) {
        failure(message);
    }

    /**
     * {@code $FAIL} function for canonical test262 tests
     */
    @Function(name = "$FAIL", arity = 1, attributes = @Attributes(writable = true,
            enumerable = true, configurable = false))
    public void fail(String message) {
        failure(message);
    }

    /**
     * {@code $PRINT} function for canonical test262 tests
     */
    @Function(name = "$PRINT", arity = 1, attributes = @Attributes(writable = true,
            enumerable = true, configurable = false))
    public void print(String message) {
        console.print(message);
    }

    /**
     * {@code $INCLUDE} function for canonical test262 tests
     */
    @Function(name = "$INCLUDE", arity = 1, attributes = @Attributes(writable = true,
            enumerable = true, configurable = false))
    public void include(String file) throws IOException {
        // resolve the input file against the library path
        super.include(Paths.get("harness", file));
    }

    /**
     * {@code runTestCase} function for canonical test262 tests
     */
    @Function(name = "runTestCase", arity = 1, attributes = @Attributes(writable = true,
            enumerable = true, configurable = false))
    public void runTestCase(ExecutionContext cx, Callable testcase) {
        Object value = testcase.call(cx, UNDEFINED);
        if (!ToBoolean(value)) {
            failure(test.getDescription());
        }
    }
}
