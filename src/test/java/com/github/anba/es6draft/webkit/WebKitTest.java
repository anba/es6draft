/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import org.junit.runners.model.MultipleFailureException;

import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.util.NullConsole;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.TestAssertions;
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
@TestConfiguration(name = "webkit.test", file = "resource:/test-configuration.properties")
public final class WebKitTest {
    private static final Configuration configuration = loadConfiguration(WebKitTest.class);

    @Parameters(name = "{0}")
    public static List<WebKitTestInfo> suiteValues() throws IOException {
        return loadTests(configuration, WebKitTestInfo::new);
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
    public ErrorCollector collector = new ErrorCollector();

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public WebKitTestInfo test;

    private static class WebKitTestInfo extends TestInfo {
        final boolean negative;

        public WebKitTestInfo(Path basedir, Path script) {
            super(basedir, script);
            // negative tests end with "-n"
            this.negative = script.getFileName().toString().endsWith("-n.js");
        }
    }

    private WebKitTestGlobalObject global;

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        global = globals.newGlobal(new NullConsole(), test);
        global.createGlobalProperties(new Print(), Print.class);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());

        if (test.negative) {
            expected.expect(
                    Matchers.either(StandardErrorHandler.defaultMatcher()).or(ScriptExceptionHandler.defaultMatcher())
                            .or(Matchers.instanceOf(MultipleFailureException.class)));
        } else {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        }
    }

    @After
    public void tearDown() {
        globals.release(global);
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        // - load and execute pre and post before resp. after test-script
        global.include(Paths.get("resources/standalone-pre.js"));
        global.eval(test.getScript(), test.toFile());
        global.include(Paths.get("resources/standalone-post.js"));

        // Wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop();
    }

    public final class Print {
        @Function(name = "print", arity = 1)
        public void print(String... messages) {
            String message = Strings.concatWith(' ', messages);
            if (message.startsWith("FAIL ")) {
                // Collect all failures instead of calling fail() directly.
                collector.addError(TestAssertions.newAssertionError(message));
            }
        }
    }
}
