/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8test;

import static com.github.anba.es6draft.repl.global.V8ShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Functional.Function;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestShellGlobals;

/**
 *
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "v8.test.mjsunit", file = "resource:test-configuration.properties")
public class MiniJSUnitTest {
    private static final Configuration configuration = loadConfiguration(MiniJSUnitTest.class);

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
    public static TestShellGlobals<V8ShellGlobalObject> globals = new TestShellGlobals<V8ShellGlobalObject>(
            configuration) {
        @Override
        protected ObjectAllocator<V8ShellGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return newGlobalObjectAllocator(console, test.basedir, test.script, scriptCache);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    private V8ShellGlobalObject global;

    @Before
    public void setUp() throws IOException {
        // filter disabled tests
        assumeTrue(test.enable);

        global = globals.newGlobal(new V8TestConsole(collector), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
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
        global.eval(test.script, test.toFile());
    }

    private static class TestInfos implements BiFunction<Path, Iterator<String>, TestInfo> {
        private static final Pattern FlagsPattern = Pattern.compile("\\s*//\\s*Flags:\\s*(.*)\\s*");
        private final Path basedir;

        public TestInfos(Path basedir) {
            this.basedir = basedir;
        }

        @Override
        public TestInfo apply(Path file, Iterator<String> lines) {
            TestInfo test = new TestInfo(basedir, file);
            Pattern p = FlagsPattern;
            while (lines.hasNext()) {
                String line = lines.next();
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    String[] flags = m.group(1).split("\\s+");
                    for (int i = 0, len = flags.length; i < len; ++i) {
                        String flag = flags[i].replace('_', '-');
                        if (flag.startsWith("--expose-debug-as")) {
                            if (flag.equals("--expose-debug-as")) {
                                // two arguments form, consume next argument as well
                                ++i;
                            }
                            // don't run debug-mode tests
                            test.enable = false;
                        } else if (flag.startsWith("--expose-natives-as")) {
                            // don't run tests with natives or lazy compilation
                            if (flag.equals("--expose-natives-as")) {
                                // two arguments form, consume next argument as well
                                ++i;
                            }
                            test.enable = false;
                        } else if (flag.equals("--expose-externalize-string")) {
                            // don't run tests with natives or lazy compilation
                            test.enable = false;
                        } else if (flag.equals("--allow-natives-syntax")) {
                            // don't run tests with natives or lazy compilation
                            test.enable = false;
                        } else if (flag.equals("--lazy")) {
                            // don't run tests with natives or lazy compilation
                            test.enable = false;
                        } else if (flag.equals("--expose-trigger-failure")) {
                            // don't run tests with trigger-failure
                            test.enable = false;
                        } else {
                            // ignore other flags
                        }
                    }
                }
            }
            return test;
        }
    }
}
