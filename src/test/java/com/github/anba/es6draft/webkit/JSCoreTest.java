/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.runtime.extensions.timer.Timers;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.util.NullConsole;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 *
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "webkit.jsc.test", file = "resource:/test-configuration.properties")
public final class JSCoreTest {
    private static final Configuration configuration = loadConfiguration(JSCoreTest.class);

    @Parameters(name = "{0}")
    public static List<TestInfo> suiteValues() throws IOException {
        return loadTests(configuration, TestInfo::new);
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        WebKitTestGlobalObject.testLoadInitializationScript();
    }

    @ClassRule
    public static TestGlobals<WebKitTestGlobalObject, TestInfo> globals = new TestGlobals<>(configuration,
            WebKitTestGlobalObject::new);

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    private WebKitTestGlobalObject global;
    private Timers timers;

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        global = globals.newGlobal(new NullConsole(), test);
        timers = global.createGlobalProperties(new Timers(), Timers.class);
        global.createGlobalProperties(new DrainMicrotasks(), DrainMicrotasks.class);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
    }

    @After
    public void tearDown() {
        globals.release(global);
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // Wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop(timers);
    }

    public final class DrainMicrotasks {
        @Function(name = "drainMicrotasks", arity = 0)
        public void print() throws InterruptedException {
            global.getRealm().getWorld().runEventLoop(timers);
        }
    }
}
