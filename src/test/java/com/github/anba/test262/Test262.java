/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.test262;

import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.test262.environment.Environment;
import com.github.anba.test262.environment.Environments;
import com.github.anba.test262.environment.Environments.EnvironmentProvider;
import com.github.anba.test262.environment.GlobalObject;
import com.github.anba.test262.util.ExceptionHandler;
import com.github.anba.test262.util.LabelledParameterized;
import com.github.anba.test262.util.LazyInit;
import com.github.anba.test262.util.Test262AssertionError;
import com.github.anba.test262.util.Test262Info;

/**
 * The new test262 style
 * 
 */
@RunWith(LabelledParameterized.class)
@LabelledParameterized.Parallelized
public final class Test262 extends BaseTest262 {
    private static final String TEST_SUITE = "test.suite.test262";

    private static final LazyInit<Configuration> configuration = newConfiguration();
    private static EnvironmentProvider<? extends GlobalObject> provider;
    private Environment<? extends GlobalObject> environment;

    public Test262(String sourceName, String path) {
        super(configuration.get(), TEST_SUITE, sourceName, path);
    }

    // @Rule
    // public Timeout timeout = new Timeout(4000);

    @Rule
    public ExpectedException expected = ExpectedException.none().handleAssertionErrors();

    @Rule
    public ExceptionHandler handler = new ExceptionHandler() {
        @Override
        protected void handle(Throwable t) {
            throw new AssertionError(t.getMessage(), t);
        }
    };

    @Test
    public void test() throws Throwable {
        Test262Info info = info();
        if (info.isOnlyStrict() || info.isNoStrict()) {
            assumeTrue(isStrictSupported());
        }

        Matcher<Object> m = anyInstanceOf(environment.exceptions());
        if (info.isNegative()) {
            m = either(m).or(instanceOf(Test262AssertionError.class));
            expected.expect(m);
            String errorType = info.getErrorType();
            if (errorType != null) {
                expected.expect(hasErrorType(errorType, environment.matcher(errorType)));
            }
        } else {
            handler.match(m);
        }

        execute(environment);
    }

    @Before
    public void setUp() throws IOException {
        Environment<? extends GlobalObject> env = provider.environment(TEST_SUITE, getSourceName(),
                info());
        env.global().include("sta.js");
        env.global().setUp();
        environment = env;
    }

    @BeforeClass
    public static void setUpClass() {
        provider = Environments.get(configuration.get());
    }

    @Parameters
    public static List<Object[]> files() throws IOException {
        List<Object[]> testCases = collectTestCases(configuration.get().subset(TEST_SUITE));
        if (testCases.isEmpty()) {
            System.err.println("no testcases selected!");
        }
        return testCases.subList(0, testCases.size());
    }
}
