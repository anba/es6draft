/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.util.ErrorMessageMatcher.hasErrorMessage;
import static com.github.anba.es6draft.util.PatternMatcher.matchesPattern;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.util.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.Resources;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;

/**
 * The standard test262 test suite
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "test.suite.test262", file = "resource:test262.properties")
public final class Test262 {
    private static final Configuration configuration = loadConfiguration(Test262.class);

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        return Resources.loadXMLTests(configuration, new BiFunction<Path, Path, TestInfo>() {
            @Override
            public TestInfo apply(Path basedir, Path file) {
                return new Test262Info(basedir, file);
            }
        });
    }

    @ClassRule
    public static Test262Globals globals = new Test262Globals(configuration);

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public ExpectedException expected = ExpectedException.none().handleAssertionErrors();

    @Parameter(0)
    public Test262Info test;

    private Test262GlobalObject global;

    @Before
    public void setUp() throws IOException {
        // filter disabled tests
        assumeTrue(test.enable);

        global = globals.newGlobal(test);
        ExecutionContext cx = global.getRealm().defaultContext();
        exceptionHandler.setExecutionContext(cx);

        if (!test.isNegative()) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            expected.expect(Matchers.either(StandardErrorHandler.defaultMatcher())
                    .or(ScriptExceptionHandler.defaultMatcher())
                    .or(instanceOf(Test262AssertionError.class)));
            String errorType = test.getErrorType();
            if (errorType != null) {
                expected.expect(hasErrorMessage(cx,
                        matchesPattern(errorType, Pattern.CASE_INSENSITIVE)));
            }
        }
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
        global.eval(test.toFile());
    }
}
