/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.repl.global.SimpleShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
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

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.SimpleShellGlobalObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 *
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "promise.test", file = "resource:/test-configuration.properties")
public class PromiseTest {
    private static final Configuration configuration = loadConfiguration(PromiseTest.class);

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        return loadTests(configuration);
    }

    @ClassRule
    public static TestGlobals<SimpleShellGlobalObject, TestInfo> globals = new TestGlobals<SimpleShellGlobalObject, TestInfo>(
            configuration) {
        @Override
        protected ObjectAllocator<SimpleShellGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return newGlobalObjectAllocator(console, test.getBaseDir(), test.getScript(),
                    scriptCache);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    private SimpleShellGlobalObject global;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // filter disabled tests
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
        // create global 'done' function for async tests
        AsyncHelper async = new AsyncHelper();
        WindowTimers timers = new WindowTimers();
        ExecutionContext cx = global.getRealm().defaultContext();
        ScriptObject globalThis = global.getRealm().getGlobalThis();
        Properties.createProperties(cx, globalThis, async, AsyncHelper.class);
        Properties.createProperties(cx, globalThis, timers, WindowTimers.class);

        // evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // wait for pending tasks to finish
        assertFalse(async.doneCalled);
        for (;;) {
            global.getRealm().getWorld().executeTasks();
            Task task = timers.nextTaskOrNull();
            if (task == null) {
                break;
            }
            global.getRealm().enqueueScriptTask(task);
        }
        assertTrue(async.doneCalled);
    }

    public static class AsyncHelper {
        boolean doneCalled = false;

        @Properties.Function(name = "done", arity = 0)
        public void done() {
            assertFalse(doneCalled);
            doneCalled = true;
        }
    }
}
