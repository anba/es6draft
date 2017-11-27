/**
 * Copyright (c) André Bargull
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
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

import com.github.anba.es6draft.runtime.extensions.timer.Timers;
import com.github.anba.es6draft.runtime.internal.UnhandledRejectionException;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.SystemConsole;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestRealm;
import com.github.anba.es6draft.util.TestRealms;
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
    public static TestRealms<TestInfo> realms = new TestRealms<>(configuration, PromiseTestRealmData::new);

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

    @Rule
    public TestRealm<TestInfo> realm = new TestRealm<>(realms);

    private Timers timers;

    @SuppressWarnings("serial")
    public static final class MissingRejectionException extends RuntimeException {
    }

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        realm.initialize(new SystemConsole(), test);
        exceptionHandler.setExecutionContext(realm.get().defaultContext());
        timers = realm.get().createGlobalProperties(new Timers(), Timers.class);

        if (test.negative) {
            expected.expect(Matchers.either(Matchers.instanceOf(UnhandledRejectionException.class))
                    .or(Matchers.instanceOf(MissingRejectionException.class)));
        } else {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        }
    }

    @Test
    public void runTest() throws Throwable {
        realm.execute(test, timers);

        // FIXME: Missing unhandled rejection exceptions need to be soft instead of hard failures.
        if (test.negative) {
            throw new MissingRejectionException();
        }
    }
}
