/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import static com.github.anba.es6draft.repl.global.V8ShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;

/**
 *
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "v8.test.webkit", file = "resource:/test-configuration.properties")
public class WebkitTest {
    private static final Configuration configuration = loadConfiguration(WebkitTest.class);

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        return loadTests(configuration);
    }

    @ClassRule
    public static TestGlobals<V8ShellGlobalObject, TestInfo> globals = new TestGlobals<V8ShellGlobalObject, TestInfo>(
            configuration) {
        @Override
        protected ObjectAllocator<V8ShellGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return newGlobalObjectAllocator(console, test.getBaseDir(), test.getScript(),
                    scriptCache);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    private V8ShellGlobalObject global;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // filter disabled tests
        assumeTrue(test.isEnabled());

        global = globals.newGlobal(new V8TestConsole(collector), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());

    }

    @After
    public void tearDown() {
        if (global != null) {
            global.getRealm().getExecutor().shutdown();
        }
    }

    @Test
    public void runTest() throws Throwable {
        // evaluate actual test-script
        // - load and execute pre and post before resp. after test-script
        global.include(Paths.get("resources/standalone-pre.js"));
        global.eval(test.getScript(), test.toFile());
        global.include(Paths.get("resources/standalone-post.js"));
    }
}
