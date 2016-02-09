/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.matchers.ErrorMessageMatcher.hasErrorMessage;
import static com.github.anba.es6draft.util.matchers.PatternMatcher.matchesPattern;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestWatcher;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;
import com.github.anba.es6draft.util.SystemConsole;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * The standard test262 test suite
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "test262.test.web", file = "resource:/test-configuration.properties")
public final class Test262Web {
    private static final Configuration configuration = loadConfiguration(Test262Web.class);
    private static final DefaultMode unmarkedDefault = DefaultMode.forName(configuration.getString("unmarked_default"));
    private static final Set<String> includeFeatures = stringSet(configuration.getList("include.features"));
    private static final Set<String> excludeFeatures = stringSet(configuration.getList("exclude.features"));

    public static final Set<String> stringSet(List<?> xs) {
        Predicate<String> nonEmpty = ((Predicate<String>) String::isEmpty).negate();
        return xs.stream().filter(Objects::nonNull).map(Object::toString).filter(nonEmpty).collect(Collectors.toSet());
    }

    @Parameters(name = "{0}")
    public static List<Test262Info> suiteValues() throws IOException {
        return Resources.loadTests(configuration, Test262Info::new);
    }

    @ClassRule
    public static TestGlobals<Test262GlobalObject, Test262Info> globals = new TestGlobals<Test262GlobalObject, Test262Info>(
            configuration, Test262GlobalObject::new) {
        static final boolean USE_SHARED_EXECUTOR = false;
        final ExecutorService shared = USE_SHARED_EXECUTOR ? createDefaultSharedExecutor() : null;

        @Override
        public void release(Test262GlobalObject global) {
            if (!USE_SHARED_EXECUTOR) {
                super.release(global);
            } else if (global != null) {
                // Worker executors are not shared, explicit shutdown required.
                global.getRuntimeContext().getWorkerExecutor().shutdown();
            }
        }

        @Override
        protected ExecutorService getExecutor() {
            return shared;
        }
    };

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            isStrictTest = description.getAnnotation(Strict.class) != null;
        }
    };
    private boolean isStrictTest = false;

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public Test262Info test;

    private Test262GlobalObject global;
    private Test262Async async;
    private String sourceCode;
    private int preambleLines;

    private boolean isValidTestConfiguration() {
        return test.hasMode(isStrictTest, unmarkedDefault) && test.hasFeature(includeFeatures, excludeFeatures);
    }

    @Before
    public void setUp() throws Throwable {
        // Filter disabled tests
        assumeTrue("Test disabled", test.isEnabled());

        String fileContent = test.readFile();
        if (!isValidTestConfiguration()) {
            return;
        }

        final String preamble;
        if (test.isRaw() || test.isModule()) {
            preamble = "";
            preambleLines = 0;
        } else if (isStrictTest) {
            preamble = "\"use strict\";\nvar strict_mode = true;\n";
            preambleLines = 2;
        } else {
            preamble = "//\"use strict\";\nvar strict_mode = false;\n";
            preambleLines = 2;
        }
        sourceCode = Strings.concat(preamble, fileContent);

        global = globals.newGlobal(new SystemConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());

        if (!test.isNegative()) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            expected.expect(
                    Matchers.either(StandardErrorHandler.defaultMatcher()).or(ScriptExceptionHandler.defaultMatcher()));
            String errorType = test.getErrorType();
            if (errorType != null) {
                expected.expect(hasErrorMessage(global.getRealm().defaultContext(),
                        matchesPattern(errorType, Pattern.CASE_INSENSITIVE)));
            }
        }

        // Load test includes
        for (String name : test.getIncludes()) {
            global.include(name);
        }

        if (test.isAsync()) {
            async = global.createGlobalProperties(new Test262Async(), Test262Async.class);
        }
    }

    @After
    public void tearDown() {
        globals.release(global);
    }

    @Test
    public void runTest() throws Throwable {
        if (!isValidTestConfiguration()) {
            return;
        }

        // Evaluate actual test-script
        if (test.isModule()) {
            global.evalModule(test.toModuleName(), sourceCode, 1 - preambleLines);
        } else {
            global.eval(test.toFile(), sourceCode, 1 - preambleLines);
        }

        // Wait for pending tasks to finish
        if (test.isAsync()) {
            assertFalse(async.isDone());
            global.getRealm().getWorld().runEventLoop();
            assertTrue(async.isDone());
        } else {
            global.getRealm().getWorld().runEventLoop();
        }
    }

    @Test
    @Strict
    public void runTestStrict() throws Throwable {
        if (!isValidTestConfiguration()) {
            return;
        }

        // Evaluate actual test-script
        if (test.isModule()) {
            global.evalModule(test.toModuleName(), sourceCode, 1 - preambleLines);
        } else {
            global.eval(test.toFile(), sourceCode, 1 - preambleLines);
        }

        // Wait for pending tasks to finish
        if (test.isAsync()) {
            assertFalse(async.isDone());
            global.getRealm().getWorld().runEventLoop();
            assertTrue(async.isDone());
        } else {
            global.getRealm().getWorld().runEventLoop();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public @interface Strict {
    }
}
