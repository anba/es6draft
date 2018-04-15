/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.matchers.ErrorMessageMatcher.hasErrorMessage;
import static com.github.anba.es6draft.util.matchers.PatternMatcher.matchesPattern;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
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

import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestRealms;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * The standard test262 test suite (strict)
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "test262.test.strict", file = "resource:/test-configuration.properties")
public final class Test262Strict {
    private static final Configuration configuration = loadConfiguration(Test262Strict.class);
    private static final DefaultMode unmarkedDefault = DefaultMode.forName(configuration.getString("unmarked_default"));
    private static final Set<String> includeFeatures = Resources.set(configuration, "include.features",
            Collections.emptySet());
    private static final Set<String> excludeFeatures = Resources.set(configuration, "exclude.features",
            Collections.emptySet());
    private static final List<Path> harnesses = Resources.list(configuration, "harness", Collections.emptyList())
            .stream().map(Paths::get).map(Path::toAbsolutePath).collect(Collectors.toList());

    @Parameters(name = "{0}")
    public static List<Test262Info> suiteValues() throws IOException {
        return Resources.loadTests(configuration, Test262Info::new);
    }

    @ClassRule
    public static TestRealms<Test262Info> realms = new TestRealms<>(configuration, Test262RealmData::new);

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

    @Rule
    public Test262TestRealm realm = new Test262TestRealm(realms);

    private boolean isValidTestConfiguration() {
        return test.hasMode(isStrictTest, unmarkedDefault) && test.hasFeature(includeFeatures, excludeFeatures);
    }

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        if (!realm.initialize(test, Test262Info::readFile, this::isValidTestConfiguration)) {
            return;
        }
        exceptionHandler.setExecutionContext(realm.get().defaultContext());

        // Load test includes
        for (String name : test.getIncludes()) {
            realm.include(harnesses, name);
        }

        if (!test.isNegative()) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            switch (test.getErrorPhase()) {
            case Parse:
            case Resolution:
                expected.expect(StandardErrorHandler.defaultMatcher());
                exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
                break;
            case Runtime:
                expected.expect(ScriptExceptionHandler.defaultMatcher());
                errorHandler.match(StandardErrorHandler.defaultMatcher());
                break;
            default:
                throw new AssertionError();
            }
            expected.expect(hasErrorMessage(realm.get().defaultContext(), matchesPattern(test.getErrorType())));
        }
    }

    @Test
    public void runTest() throws Throwable {
        realm.execute(test);
    }

    @Test
    @Strict
    public void runTestStrict() throws Throwable {
        realm.executeStrict(test);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD })
    public @interface Strict {
    }
}
