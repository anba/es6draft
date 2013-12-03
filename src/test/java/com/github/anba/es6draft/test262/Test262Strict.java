/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.test262.Resources.loadConfiguration;
import static com.github.anba.es6draft.test262.Resources.loadTestCases;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.instanceOf;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.ExceptionHandler;
import com.github.anba.es6draft.util.LazyInit;
import com.github.anba.es6draft.util.Parallelized;

/**
 * The standard test262 test suite (strict)
 * 
 */
@RunWith(Parallelized.class)
public final class Test262Strict extends BaseTest262 {
    private static final String TEST_SUITE = "test.suite.test262-strict";

    private static Set<CompatibilityOption> options = CompatibilityOption.StrictCompatibility();
    private static final ScriptCache cache = new ScriptCache(Parser.Option.from(options));

    private static final LazyInit<Configuration> configuration = new LazyInit<Configuration>() {
        @Override
        protected Configuration initialize() {
            return loadConfiguration("resource:test262.properties").subset(TEST_SUITE);
        }
    };

    @Parameters(name = "{0}")
    public static List<Object[]> files() throws IOException {
        List<Object[]> testCases = loadTestCases(configuration.get());
        if (testCases.isEmpty()) {
            System.err.println("no testcases selected!");
        }
        return testCases;
    }

    @Parameter(0)
    public String sourceName;

    @Parameter(1)
    public String path;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

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
    public void runTest() throws Throwable {
        final Path libpath = Paths.get(configuration.get().getString("lib_path"));
        Path testfile = Paths.get(path);
        final Test262Info info = Test262Info.from(testfile);

        World world = new World(options);
        Realm realm = world.newRealm(new ObjectAllocator<Test262GlobalObject>() {
            @Override
            public Test262GlobalObject newInstance(Realm realm) {
                return new Test262GlobalObject(realm, libpath, cache, info, sourceName);
            }
        });

        // start initialization
        Test262GlobalObject global = (Test262GlobalObject) realm.getGlobalThis();
        global.include("sta.js");
        createProperties(global, realm.defaultContext(), Test262GlobalObject.class);

        Matcher<Object> m = anyInstanceOf(exceptions());
        if (info.isNegative()) {
            m = either(m).or(instanceOf(Test262AssertionError.class));
            expected.expect(m);
            String errorType = info.getErrorType();
            if (errorType != null) {
                expected.expect(hasErrorType(errorType));
            }
        } else {
            handler.match(m);
        }

        global.eval(testfile);
    }
}
