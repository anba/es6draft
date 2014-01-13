/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.repl.global.MozShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.util.ErrorMessageMatcher.hasErrorMessage;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.MozShellGlobalObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.util.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StopExecutionHandler;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Functional.Function;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestShellGlobals;

/**
 * Test suite for the Mozilla jit-tests.
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "mozilla.test.jittests", file = "resource:test-configuration.properties")
public class MozillaJitTest {
    private static final Configuration configuration = loadConfiguration(MozillaJitTest.class);

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        return loadTests(configuration,
                new Function<Path, BiFunction<Path, Iterator<String>, TestInfo>>() {
                    @Override
                    public TestInfos apply(Path basedir) {
                        return new TestInfos(basedir);
                    }
                });
    }

    @ClassRule
    public static TestShellGlobals<MozShellGlobalObject> globals = new TestShellGlobals<MozShellGlobalObject>(
            configuration) {
        @Override
        protected ObjectAllocator<MozShellGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return newGlobalObjectAllocator(console, test.basedir, test.script, scriptCache);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public ErrorCollector collector = new ErrorCollector();

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

    private static class MozTest extends TestInfo {
        String error = null;

        public MozTest(Path basedir, Path script) {
            super(basedir, script);
        }
    }

    @Test
    public void runTest() throws Throwable {
        // filter disabled tests
        assumeTrue(moztest.enable);

        MozShellGlobalObject global = globals.newGlobal(new MozTestConsole(collector), moztest);
        ExecutionContext cx = global.getRealm().defaultContext();
        exceptionHandler.setExecutionContext(cx);

        if (moztest.error == null) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            expected.expect(Matchers.either(StandardErrorHandler.defaultMatcher()).or(
                    ScriptExceptionHandler.defaultMatcher()));
            expected.expect(hasErrorMessage(cx, containsString(moztest.error)));
        }

        // set required global variables
        global.set(cx, "libdir", "lib/", global);
        global.set(cx, "environment", OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype),
                global);

        // evaluate actual test-script
        global.eval(moztest.script, moztest.toFile());
    }

    private static class TestInfos implements BiFunction<Path, Iterator<String>, TestInfo> {
        private static final Pattern testInfoPattern = Pattern.compile("//\\s*\\|(.+?)\\|\\s*(.*)");
        private final Path basedir;

        public TestInfos(Path basedir) {
            this.basedir = basedir;
        }

        @Override
        public TestInfo apply(Path file, Iterator<String> lines) {
            MozTest test = new MozTest(basedir, file);
            String line = lines.next();
            Matcher m = testInfoPattern.matcher(line);
            if (!m.matches()) {
                // ignore if pattern invalid or not present
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
                        // ignore for now...
                        break;
                    default:
                        System.err.printf("unknown option '%s' in line: %s\n", name, content);
                    }
                } else {
                    String name = p.trim();
                    switch (name) {
                    case "slow":
                        // don't run slow tests
                        test.enable = false;
                        break;
                    case "debug":
                        // don't run debug-mode tests
                        test.enable = false;
                        break;
                    case "allow-oom":
                    case "valgrind":
                    case "tz-pacific":
                    case "mjitalways":
                    case "mjit":
                    case "no-jm":
                    case "no-ion":
                    case "ion-eager":
                    case "dump-bytecode":
                        // ignore for now...
                        break;
                    case "":
                        // ignore empty string
                        break;
                    default:
                        System.err.printf("unknown option '%s' in line: %s\n", name, content);
                    }
                }
            }
            return test;
        }
    }
}
