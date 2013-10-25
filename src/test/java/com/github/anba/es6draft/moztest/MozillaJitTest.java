/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.repl.MozShellGlobalObject.newGlobal;
import static com.github.anba.es6draft.util.TestInfo.filterTests;
import static com.github.anba.es6draft.util.TestInfo.toObjectArray;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.MultipleFailureException;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.MozShellGlobalObject;
import com.github.anba.es6draft.repl.ShellGlobalObject;
import com.github.anba.es6draft.repl.StopExecutionException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.UncheckedIOException;

/**
 *
 */
@RunWith(Parallelized.class)
public class MozillaJitTest {

    /**
     * Returns a {@link Path} which points to the test directory 'mozilla.js.tests'
     */
    private static Path testDir() {
        String testPath = System.getenv("MOZ_JITTESTS");
        return (testPath != null ? Paths.get(testPath) : null);
    }

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> mozillaSuiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'MOZ_JITTESTS'", testdir, notNullValue());
        assumeTrue("directy 'MOZ_JITTESTS' does not exist", Files.exists(testdir));
        Path searchdir = testdir.resolve("tests");
        List<MozTest> tests = filterTests(loadTests(searchdir, testdir), "/jittests.list");
        return toObjectArray(tests);
    }

    private static class MozTest extends TestInfo {
        String error = null;

        public MozTest(Path script) {
            super(script);
        }
    }

    private static Set<CompatibilityOption> options = CompatibilityOption.MozCompatibility();
    private static ScriptCache scriptCache = new ScriptCache(Parser.Option.from(options));
    private static Script legacyMozilla;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Parameter(0)
    public MozTest moztest;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            legacyMozilla = ShellGlobalObject.compileScript(scriptCache, "mozlegacy.js");
        }
    };

    @Test
    public void runTest() throws Throwable {
        // filter disabled tests
        assumeTrue(moztest.enable);

        MozTestConsole console = new MozTestConsole();
        MozShellGlobalObject global = newGlobal(console, testDir(), moztest.script, Paths.get(""),
                scriptCache, options);

        // load legacy.js file
        global.eval(legacyMozilla);

        // load and execute prolog.js files
        global.include(Paths.get("lib/prolog.js"));

        // set required global variables
        ExecutionContext cx = global.getRealm().defaultContext();
        global.set(cx, "libdir", "lib/", global);
        global.set(cx, "environment", OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype),
                global);

        // evaluate actual test-script
        Path js = testDir().resolve(moztest.script);
        try {
            global.eval(moztest.script, js);
            if (moztest.error != null) {
                fail("Expected exception: " + moztest.error);
            }
        } catch (ParserException | CompilationException e) {
            // count towards the overall failure count
            String message = e.getMessage();
            if (moztest.error == null || !message.contains(moztest.error)) {
                console.getFailures().add(new AssertionError(message, e));
            }
        } catch (ScriptException e) {
            // count towards the overall failure count
            String message = e.getMessage(cx);
            if (moztest.error == null || !message.contains(moztest.error)) {
                console.getFailures().add(new AssertionError(message, e));
            }
        } catch (StackOverflowError e) {
            // count towards the overall failure count
            String message = "InternalError: too much recursion";
            if (moztest.error == null || !message.contains(moztest.error)) {
                console.getFailures().add(new AssertionError(message, e));
            }
        } catch (StopExecutionException e) {
            // ignore
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // fail if any test returns with errors
        List<Throwable> failures = new ArrayList<Throwable>();
        failures.addAll(console.getFailures());
        if (moztest.expect) {
            MultipleFailureException.assertEmpty(failures);
        } else {
            assertFalse("Expected test to throw error", failures.isEmpty());
        }
    }

    // Any file who's basename matches something in this set is ignored
    private static final Set<String> excludeFiles = new HashSet<>();
    private static final Set<String> excludeDirs = new HashSet<>(asList("asm.js", "baseline",
            "debug", "gc", "ion", "jaeger", "modules", "parallel", "parallelarray", "truthiness",
            "v8-v5"));

    private static List<MozTest> loadTests(Path searchdir, Path basedir) throws IOException {
        BiFunction<Path, BufferedReader, MozTest> create = new BiFunction<Path, BufferedReader, MozTest>() {
            @Override
            public MozTest apply(Path script, BufferedReader reader) {
                try {
                    return createTestInfo(script, reader);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };

        return TestInfo.loadTests(searchdir, basedir, excludeDirs, excludeFiles, create);
    }

    private static final Pattern testInfoPattern = Pattern.compile("//\\s*\\|(.+?)\\|\\s*(.*)");

    private static MozTest createTestInfo(Path script, BufferedReader reader) throws IOException {
        MozTest test = new MozTest(script);
        String line = reader.readLine();
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
