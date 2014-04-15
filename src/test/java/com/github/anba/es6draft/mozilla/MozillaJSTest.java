/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import static com.github.anba.es6draft.mozilla.MozTestGlobalObject.newTestGlobalObjectAllocator;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.MultipleFailureException;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.util.ExceptionHandlers.IgnoreExceptionHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StopExecutionHandler;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Functional.Function;
import com.github.anba.es6draft.util.*;

/**
 * Test suite for the Mozilla js-tests.
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "mozilla.test.jstests", file = "resource:/test-configuration.properties")
public class MozillaJSTest {
    private static final Configuration configuration = loadConfiguration(MozillaJSTest.class);

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
    public static TestGlobals<MozTestGlobalObject, TestInfo> globals = new TestGlobals<MozTestGlobalObject, TestInfo>(
            configuration) {
        @Override
        protected ObjectAllocator<MozTestGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return newTestGlobalObjectAllocator(console, test.getBaseDir(), test.getScript(),
                    scriptCache);
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
    public IgnoreExceptionHandler ignoreHandler = IgnoreExceptionHandler.none();

    @Rule
    public StopExecutionHandler stopHandler = new StopExecutionHandler();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public MozTest moztest;

    private static class MozTest extends TestInfo {
        List<Entry<Condition, String>> conditions = new ArrayList<>();
        boolean random = false;
        boolean expect = true;

        public MozTest(Path basedir, Path script) {
            super(basedir, script);
        }

        void addCondition(Condition c, String s) {
            conditions.add(new SimpleEntry<>(c, s));
        }
    }

    private enum Condition {
        FailsIf, SkipIf, RandomIf
    }

    private MozTestGlobalObject global;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // filter disabled tests
        assumeTrue(moztest.isEnabled());

        global = globals.newGlobal(new MozTestConsole(collector), moztest);
        ExecutionContext cx = global.getRealm().defaultContext();
        exceptionHandler.setExecutionContext(cx);

        // apply scripted conditions
        scriptConditions(cx, global);

        // filter disabled tests (may have changed after applying scripted conditions)
        assumeTrue(moztest.isEnabled());

        if (moztest.random) {
            // results from random tests are simply ignored...
            ignoreHandler.match(IgnoreExceptionHandler.defaultMatcher());
        } else if (moztest.expect) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            expected.expect(Matchers.either(StandardErrorHandler.defaultMatcher())
                    .or(ScriptExceptionHandler.defaultMatcher())
                    .or(Matchers.instanceOf(MultipleFailureException.class)));
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
        // load and execute shell.js files
        for (Path shell : shellJS(moztest)) {
            global.include(shell);
        }

        // evaluate actual test-script
        global.eval(moztest.getScript(), moztest.toFile());
    }

    /**
     * Returns an {@link Iterable} of 'shell.js'-{@link Path}s
     */
    private static Iterable<Path> shellJS(MozTest test) {
        // add 'shell.js' files from each directory
        List<Path> files = new ArrayList<>();
        Path testDir = test.getBaseDir();
        Path dir = Paths.get("");
        for (Iterator<Path> iterator = test.getScript().iterator(); iterator.hasNext(); dir = dir
                .resolve(iterator.next())) {
            Path f = testDir.resolve(dir.resolve("shell.js"));
            if (Files.exists(f)) {
                files.add(f);
            }
        }
        return files;
    }

    private void scriptConditions(ExecutionContext cx, MozTestGlobalObject global) {
        for (Entry<Condition, String> entry : moztest.conditions) {
            String code = condition(entry.getValue());
            boolean value = ToBoolean(global.evaluate(cx, code, Undefined.UNDEFINED));
            if (!value) {
                continue;
            }
            switch (entry.getKey()) {
            case FailsIf:
                moztest.expect = false;
                break;
            case RandomIf:
                moztest.random = true;
                break;
            case SkipIf:
                moztest.setEnabled(false);
                break;
            default:
                throw new IllegalStateException();
            }
        }
    }

    private static String condition(String c) {
        StringBuilder sb = new StringBuilder();
        sb.append("!!(function(){\n");
        sb.append("var xulRuntime = {OS: 'Linux', XPCOMABI: 'x86_64-gcc3', shell: true};\n");
        sb.append("var browserIsRemote = false;\n");
        sb.append("var isDebugBuild = false;\n");
        sb.append("var Android = false;\n");
        sb.append("return (").append(c).append(");");
        sb.append("})();");

        return sb.toString();
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
            // negative tests end with "-n"
            if (file.getFileName().toString().endsWith("-n.js")) {
                test.expect = false;
            }
            String line = lines.next();
            Matcher m = testInfoPattern.matcher(line);
            if (!m.matches()) {
                // ignore if pattern invalid or not present
                return test;
            }
            if (!"reftest".equals(m.group(1))) {
                System.err.printf("invalid tag '%s' in line: %s\n", m.group(1), line);
                return test;
            }
            String content = m.group(2);
            for (String p : split(content)) {
                if (p.equals("fails")) {
                    test.expect = false;
                } else if (p.equals("skip")) {
                    test.setEnabled(false);
                } else if (p.equals("random")) {
                    test.random = true;
                } else if (p.equals("slow")) {
                    // don't run slow tests
                    test.setEnabled(false);
                } else if (p.equals("silentfail")) {
                    // ignore for now...
                } else if (p.startsWith("fails-if")) {
                    test.addCondition(Condition.FailsIf, p.substring("fails-if".length()));
                } else if (p.startsWith("skip-if")) {
                    test.addCondition(Condition.SkipIf, p.substring("skip-if".length()));
                } else if (p.startsWith("random-if")) {
                    test.addCondition(Condition.RandomIf, p.substring("random-if".length()));
                } else if (p.startsWith("asserts-if")) {
                    // ignore for now...
                } else if (p.startsWith("require-or")) {
                    // ignore for now...
                } else {
                    System.err.printf("invalid manifest line: %s\n", p);
                }
            }
            return test;
        }

        private static String[] split(String line) {
            final String comment = "--";
            final String ws = "[ \t\n\r\f\013]+";
            // remove comment if any
            int k = line.indexOf(comment);
            if (k != -1) {
                line = line.substring(0, k);
            }
            // split at whitespace
            return line.trim().split(ws);
        }
    }
}
