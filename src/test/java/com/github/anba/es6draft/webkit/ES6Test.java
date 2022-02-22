/**
 * Copyright (c) André Bargull
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
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestRealm;
import com.github.anba.es6draft.util.TestRealms;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 *
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "webkit.es6.test", file = "resource:/test-configuration.properties")
public final class ES6Test {
    private static final Configuration configuration = loadConfiguration(ES6Test.class);

    @Parameters(name = "{0}")
    public static List<TestInfo> suiteValues() throws IOException {
        return loadTests(configuration);
    }

    @BeforeClass
    public static void setUpClass() {
        WebKitTestRealmData.testLoadInitializationScript();
    }

    @ClassRule
    public static TestRealms<TestInfo> realms = new TestRealms<>(configuration, WebKitTestRealmData::new);

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    @Rule
    public TestRealm<TestInfo> realm = new TestRealm<>(realms);

    private Timers timers;

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        realm.initialize(new NullConsole(), test);
        timers = realm.get().createGlobalProperties(new Timers(), Timers.class);
        realm.get().createGlobalProperties(new DrainMicrotasks(), DrainMicrotasks.class);
        exceptionHandler.setExecutionContext(realm.get().defaultContext());
    }

    @Test
    public void runTest() throws Throwable {
        realm.execute(test, timers);
    }

    public final class DrainMicrotasks {
        @Function(name = "drainMicrotasks", arity = 0)
        public void print() throws InterruptedException {
            realm.get().getWorld().runEventLoop(timers);
        }
    }
}