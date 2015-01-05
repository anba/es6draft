/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.promise;

import static com.github.anba.es6draft.TestGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
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

import com.github.anba.es6draft.TestGlobalObject;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Task;
import com.github.anba.es6draft.runtime.extensions.timer.Timers;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.types.Callable;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParallelizedRunnerFactory;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * Test class for Promise-Unwrapping tests
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParallelizedRunnerFactory.class)
@TestConfiguration(name = "promise.test.unwrapping",
        file = "resource:/test-configuration.properties")
public final class PromiseUnwrappingTest {
    private static final Configuration configuration = loadConfiguration(PromiseUnwrappingTest.class);

    @Parameters(name = "{0}")
    public static List<TestInfo> suiteValues() throws IOException {
        return loadTests(configuration);
    }

    @ClassRule
    public static TestGlobals<TestGlobalObject, TestInfo> globals = new TestGlobals<TestGlobalObject, TestInfo>(
            configuration) {
        @Override
        protected ObjectAllocator<TestGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return newGlobalObjectAllocator(console, test, scriptCache);
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

    private TestGlobalObject global;
    private AsyncHelper async;
    private Timers timers;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // Filter disabled tests
        assumeTrue(test.isEnabled());

        global = globals.newGlobal(new PromiseTestConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
        async = global.install(new AsyncHelper(), AsyncHelper.class);
        timers = global.install(new Timers(), Timers.class);
    }

    @After
    public void tearDown() throws InterruptedException {
        if (global != null) {
            global.getScriptLoader().getExecutor().shutdown();
        }
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // Wait for pending tasks to finish
        assertFalse(async.doneCalled);
        global.getRealm().getWorld().runEventLoop(timers);
        assertTrue(async.doneCalled);
    }

    public static final class AsyncHelper {
        boolean doneCalled = false;

        @Properties.Function(name = "$async_done", arity = 0)
        public void done() {
            assertFalse(doneCalled);
            doneCalled = true;
        }

        @Properties.Function(name = "$async_enqueueTask", arity = 1)
        public void enqueueTask(final ExecutionContext cx, final Callable task) {
            cx.getRealm().enqueuePromiseTask(new Task() {
                @Override
                public void execute() {
                    task.call(cx, Undefined.UNDEFINED);
                }
            });
        }
    }
}
