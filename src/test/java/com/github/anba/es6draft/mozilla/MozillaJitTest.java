/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static com.github.anba.es6draft.util.matchers.ErrorMessageMatcher.hasErrorMessage;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.util.NullConsole;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StopExecutionHandler;

/**
 * Test suite for the Mozilla jit-tests.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "mozilla.test.jittests", file = "resource:/test-configuration.properties")
public final class MozillaJitTest {
    private static final Configuration configuration = loadConfiguration(MozillaJitTest.class);

    @Parameters(name = "{0}")
    public static List<MozTest> suiteValues() throws IOException {
        return loadTests(configuration, MozillaJitTest::createTest);
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        MozTestGlobalObject.testLoadInitializationScript();
    }

    @ClassRule
    public static TestGlobals<MozTestGlobalObject, MozTest> globals = new TestGlobals<>(configuration,
            MozTestGlobalObject::new);

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public StopExecutionHandler stopHandler = new StopExecutionHandler();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public MozTest moztest;

    private static final class MozTest extends TestInfo {
        String error = null;

        public MozTest(Path basedir, Path script) {
            super(basedir, script);
        }
    }

    private MozTestGlobalObject global;

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", moztest.isEnabled());

        global = globals.newGlobal(new NullConsole(), moztest);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
        global.createGlobalProperties(new TestEnvironment(), TestEnvironment.class);

        if (moztest.error == null) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            ExecutionContext cx = global.getRealm().defaultContext();
            expected.expect(
                    Matchers.either(StandardErrorHandler.defaultMatcher()).or(ScriptExceptionHandler.defaultMatcher()));
            expected.expect(hasErrorMessage(cx, containsString(moztest.error)));
        }
    }

    @After
    public void tearDown() {
        globals.release(global);
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        global.eval(moztest.getScript(), moztest.toFile());

        // Wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop();
    }

    public static final class TestEnvironment {
        @Properties.Value(name = "libdir")
        public String libdir() {
            return "lib/";
        }

        @Properties.Value(name = "environment")
        public ScriptObject environment(ExecutionContext cx) {
            return OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
        }

        @Properties.Value(name = "os")
        public ScriptObject os(ExecutionContext cx) {
            OrdinaryObject os = OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
            Properties.createProperties(cx, os, new OS(), OS.class);
            return os;
        }
    }

    public static final class OS {
        @Properties.Function(name = "getenv", arity = 1)
        public String getenv(String name) {
            return System.getenv().get(name);
        }
    }

    private static final Pattern testInfoPattern = Pattern.compile("//\\s*\\|(.+?)\\|\\s*(.*)");

    private static BiFunction<Path, Iterator<String>, MozTest> createTest(Path basedir) {
        return (file, lines) -> {
            MozTest test = new MozTest(basedir, file);
            String line = lines.next();
            Matcher m = testInfoPattern.matcher(line);
            if (!m.matches()) {
                // Ignore if pattern invalid or not present
                return test;
            }
            if (!"jit-test".equals(m.group(1))) {
                System.err.printf("invalid tag '%s' in line: %s\n", m.group(1), line);
                return test;
            }
            String content = m.group(2);
            for (String p : content.split(";")) {
                int sep = p.indexOf(':');
                if (sep != -1) {
                    String name = p.substring(0, sep).trim();
                    String value = p.substring(sep + 1).trim();
                    switch (name) {
                    case "error":
                        test.error = value;
                        break;
                    case "exitstatus":
                        // Ignore for now...
                        break;
                    default:
                        System.err.printf("unknown option '%s' in line: %s\n", name, content);
                    }
                } else {
                    String name = p.trim();
                    switch (name) {
                    case "slow":
                        // Don't run slow tests
                        test.setEnabled(false);
                        break;
                    case "debug":
                        // Don't run debug-mode tests
                        test.setEnabled(false);
                        break;
                    case "allow-oom":
                    case "allow-overrecursed":
                    case "allow-unhandlable-oom":
                    case "valgrind":
                    case "tz-pacific":
                    case "mjitalways":
                    case "mjit":
                    case "no-jm":
                    case "no-ion":
                    case "ion-eager":
                    case "dump-bytecode":
                    case "--fuzzing-safe":
                    case "--no-threads":
                    case "--no-ion":
                    case "--no-baseline":
                        // Ignore for now...
                        break;
                    case "":
                        // Ignore empty string
                        break;
                    default:
                        System.err.printf("unknown option '%s' in line: %s [%s]\n", name, content, file);
                    }
                }
            }
            return test;
        };
    }
}
