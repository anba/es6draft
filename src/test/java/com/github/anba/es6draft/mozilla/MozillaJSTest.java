/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
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

import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;
import com.github.anba.es6draft.runtime.internal.Strings;
import com.github.anba.es6draft.util.NullConsole;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;
import com.github.anba.es6draft.util.TestAssertions;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestRealm;
import com.github.anba.es6draft.util.TestRealms;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.IgnoreExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StopExecutionHandler;

/**
 * Test suite for the Mozilla js-tests.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "mozilla.test.jstests", file = "resource:/test-configuration.properties")
public final class MozillaJSTest {
    private static final Configuration configuration = loadConfiguration(MozillaJSTest.class);
    private static final Set<String> ignoreFlags = Resources.set(configuration, "flags.ignore", Collections.emptySet());
    private static final Set<String> disableFlags = Resources.set(configuration, "flags.disable",
            Collections.emptySet());
    private static final boolean warnUnknownFlag = configuration.getBoolean("flags.warn", false);

    @Parameters(name = "{0}")
    public static List<MozTest> suiteValues() throws IOException {
        return loadTests(configuration, MozTest::new, MozillaJSTest::configureTest);
    }

    @BeforeClass
    public static void setUpClass() {
        MozTestRealmData.testLoadInitializationScript();
    }

    @ClassRule
    public static TestRealms<TestInfo> realms = new TestRealms<TestInfo>(configuration, MozTestRealmData::new) {
        @Override
        protected Supplier<MozContextData> getRuntimeData() {
            return MozContextData::new;
        }
    };

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

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
    public MozTest test;

    private static final class MozTest extends TestInfo {
        List<Entry<Condition, String>> conditions = new ArrayList<>();
        boolean negative = false;
        boolean module = false;

        public MozTest(Path basedir, Path script) {
            super(basedir, script);
        }

        @Override
        public boolean isModule() {
            return module;
        }

        void addCondition(Condition c, String s) {
            conditions.add(new SimpleEntry<>(c, s));
        }
    }

    private enum Condition {
        FailsIf, SkipIf, RandomIf
    }

    @Rule
    public TestRealm<TestInfo> realm = new TestRealm<>(realms);

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        realm.initialize(new NullConsole(), test);
        realm.get().createGlobalProperties(new Print(), Print.class);
        exceptionHandler.setExecutionContext(realm.get().defaultContext());

        // Apply scripted conditions
        scriptConditions();

        // Filter disabled tests (may have changed after applying scripted conditions)
        assumeTrue("Test disabled", test.isEnabled());

        if (test.negative) {
            expected.expect(
                    Matchers.either(StandardErrorHandler.defaultMatcher()).or(ScriptExceptionHandler.defaultMatcher())
                            .or(Matchers.instanceOf(MultipleFailureException.class)));
        } else {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        }
    }

    @Test
    public void runTest() throws Throwable {
        // Load and execute shell.js files
        for (Path shell : shellJS(test)) {
            ScriptLoading.include(realm.get(), shell);
        }

        realm.execute(test);
    }

    /**
     * Returns an {@link Iterable} of 'shell.js'-{@link Path}s
     */
    private static Iterable<Path> shellJS(MozTest test) {
        // Add 'shell.js' files from each directory
        List<Path> files = new ArrayList<>();
        Path testDir = test.getBaseDir();
        Path dir = Paths.get("");
        for (Iterator<Path> iterator = test.getScript().iterator(); iterator
                .hasNext(); dir = dir.resolve(iterator.next())) {
            Path f = testDir.resolve(dir.resolve("shell.js"));
            if (Files.isRegularFile(f)) {
                files.add(f);
            }
        }
        return files;
    }

    private void scriptConditions() {
        for (Entry<Condition, String> entry : test.conditions) {
            if (!evaluateCondition(entry)) {
                continue;
            }
            switch (entry.getKey()) {
            case FailsIf:
                test.negative = true;
                break;
            case RandomIf:
            case SkipIf:
                test.setEnabled(false);
                break;
            default:
                throw new IllegalStateException();
            }
        }
    }

    private boolean evaluateCondition(Entry<Condition, String> entry) {
        String code = condition(entry.getValue());
        return ToBoolean(ScriptLoading.eval(realm.get(), "@evaluate", code));
    }

    private static String condition(String c) {
        StringBuilder sb = new StringBuilder();
        sb.append("!!(function(){\n");
        sb.append("var xulRuntime = {OS: 'Linux', XPCOMABI: 'x86_64-gcc3', shell: true};\n");
        sb.append("var browserIsRemote = false;\n");
        sb.append("var isDebugBuild = false;\n");
        sb.append("var Android = false;\n");
        sb.append("var release_or_beta = false;\n");
        sb.append("return (").append(c).append(");");
        sb.append("})();");

        return sb.toString();
    }

    public final class Print {
        @Properties.Function(name = "print", arity = 1)
        public void print(String... messages) {
            String message = Strings.concatWith(' ', messages);
            if (message.startsWith(" FAILED! ")) {
                // Collect all failures instead of calling fail() directly.
                collector.addError(TestAssertions.newAssertionError(message));
            }
        }
    }

    private static final Pattern testInfoPattern = Pattern.compile("//\\s*\\|(.+?)\\|\\s*(.*)");

    private static void configureTest(MozTest test, Stream<String> lines) {
        // Negative tests end with "-n"
        if (test.getScript().getFileName().toString().endsWith("-n.js")) {
            test.negative = true;
        }
        String line = lines.findFirst().orElseThrow(IllegalArgumentException::new);
        Matcher m = testInfoPattern.matcher(line);
        if (!m.matches()) {
            // Ignore if pattern invalid or not present
            return;
        }
        if (!"reftest".equals(m.group(1))) {
            System.err.printf("invalid tag '%s' in line: %s\n", m.group(1), line);
            return;
        }
        String content = m.group(2);
        for (String p : split(content)) {
            if (ignoreFlags.contains(p)) {
                // Ignored flags
            } else if (disableFlags.contains(p)) {
                test.setEnabled(false);
            } else if (p.equals("module")) {
                test.module = true;
            } else if (p.startsWith("error:")) {
                test.negative = true;
            } else if (p.equals("fails")) {
                test.negative = true;
            } else if (p.startsWith("fails-if")) {
                test.addCondition(Condition.FailsIf, p.substring("fails-if".length()));
            } else if (p.startsWith("skip-if")) {
                test.addCondition(Condition.SkipIf, p.substring("skip-if".length()));
            } else if (p.startsWith("random-if")) {
                test.addCondition(Condition.RandomIf, p.substring("random-if".length()));
            } else if (p.startsWith("asserts-if")) {
                // Ignore for now...
            } else if (p.startsWith("require-or")) {
                // Ignore for now...
            } else if (warnUnknownFlag) {
                System.err.printf("unknown option '%s' in line: %s [%s]%n", p, content, test.getScript());
            }
        }
    }

    private static String[] split(String line) {
        final String comment = "--";
        final String ws = "[ \t\n\r\f\013]+";
        // Remove comment if any
        int k = line.indexOf(comment);
        if (k != -1) {
            line = line.substring(0, k);
        }
        // Split at whitespace
        return line.trim().split(ws);
    }
}
