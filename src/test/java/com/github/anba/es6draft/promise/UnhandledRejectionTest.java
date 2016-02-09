/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.promise;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.TestGlobalObject;
import com.github.anba.es6draft.runtime.extensions.timer.Timers;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.UnhandledRejectionException;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.SystemConsole;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * Test class for unhandled promise rejection.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "promise.test.rejection", file = "resource:/test-configuration.properties")
public final class UnhandledRejectionTest {
    private static final Configuration configuration = loadConfiguration(UnhandledRejectionTest.class);

    @Parameters(name = "{0}")
    public static List<PromiseTestInfo> suiteValues() throws IOException {
        return loadTests(configuration, PromiseTestInfo::new);
    }

    @ClassRule
    public static TestGlobals<TestGlobalObject, TestInfo> globals = new TestGlobals<TestGlobalObject, TestInfo>(
            configuration, TestGlobalObject::new) {
        @Override
        protected EnumSet<CompatibilityOption> getOptions() {
            EnumSet<CompatibilityOption> options = super.getOptions();
            options.add(CompatibilityOption.PromiseRejection);
            return options;
        }
    };

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public PromiseTestInfo test;

    private static class PromiseTestInfo extends TestInfo {
        final boolean negative;

        public PromiseTestInfo(Path basedir, Path script) {
            super(basedir, script);
            // negative tests end with "-n"
            this.negative = script.getFileName().toString().endsWith("-n.js");
        }
    }

    private TestGlobalObject global;
    private Timers timers;

    @SuppressWarnings("serial")
    public static final class MissingRejectionException extends RuntimeException {
    }

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        global = globals.newGlobal(new SystemConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
        timers = global.createGlobalProperties(new Timers(), Timers.class);

        if (test.negative) {
            expected.expect(Matchers.either(Matchers.instanceOf(UnhandledRejectionException.class))
                    .or(Matchers.instanceOf(MissingRejectionException.class)));
        } else {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        }
    }

    @After
    public void tearDown() throws InterruptedException {
        globals.release(global);
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // Wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop(timers);

        // FIXME: Missing unhandled rejection exceptions need to be soft instead of hard failures.
        if (test.negative) {
            throw new MissingRejectionException();
        }
    }
}
