/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParallelizedRunnerFactory;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * Test suite for the native, v8 tests.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParallelizedRunnerFactory.class)
@TestConfiguration(name = "natives.test.v8", file = "resource:/test-configuration.properties")
public final class NativesV8Test {
    private static final Configuration configuration = loadConfiguration(NativesV8Test.class);

    @Parameters(name = "{0}")
    public static List<TestInfo> suiteValues() throws IOException {
        return loadTests(configuration);
    }

    @ClassRule
    public static TestGlobals<V8NativeTestGlobalObject, TestInfo> globals = new TestGlobals<V8NativeTestGlobalObject, TestInfo>(
            configuration) {
        @Override
        protected ObjectAllocator<V8NativeTestGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return V8NativeTestGlobalObject.newGlobalObjectAllocator(console, test, scriptCache);
        }

        @Override
        protected Set<CompatibilityOption> getOptions() {
            EnumSet<CompatibilityOption> options = EnumSet.copyOf(super.getOptions());
            options.add(CompatibilityOption.Comprehension);
            options.add(CompatibilityOption.Realm);
            options.add(CompatibilityOption.System);
            return options;
        }

        @Override
        protected EnumSet<Parser.Option> getParserOptions() {
            return EnumSet.of(Parser.Option.NativeCall);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    private V8NativeTestGlobalObject global;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // Filter disabled tests
        assumeTrue(test.isEnabled());

        global = globals.newGlobal(new ScriptTestConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
    }

    @After
    public void tearDown() {
        if (global != null) {
            global.getScriptLoader().getExecutor().shutdown();
        }
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // Wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop();
    }

    public static final class V8NativeTestGlobalObject extends V8ShellGlobalObject {
        protected V8NativeTestGlobalObject(Realm realm, ShellConsole console, TestInfo test,
                ScriptCache scriptCache) {
            super(realm, console, test.getBaseDir(), test.getScript(), scriptCache);
        }

        @Override
        public void initializeScripted() throws IOException, URISyntaxException, ParserException,
                CompilationException {
            // super.initializeScripted();
            includeNative(getScriptURL("compat.js"));
            includeNative(getScriptURL("cyclic.js"));
            includeNative(getScriptURL("generator.js"));
            includeNative(getScriptURL("internal-error.js"));
            includeNative(getScriptURL("proxy.js"));
            includeNative(getScriptURL("stacktrace.js"));
            includeNative(getScriptURL("typed-array.js"));
        }

        public static ObjectAllocator<V8NativeTestGlobalObject> newGlobalObjectAllocator(
                final ShellConsole console, final TestInfo test, final ScriptCache scriptCache) {
            return new ObjectAllocator<V8NativeTestGlobalObject>() {
                @Override
                public V8NativeTestGlobalObject newInstance(Realm realm) {
                    return new V8NativeTestGlobalObject(realm, console, test, scriptCache);
                }
            };
        }
    }
}
