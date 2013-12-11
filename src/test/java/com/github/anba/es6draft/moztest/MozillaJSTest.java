/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.repl.MozShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.runtime.AbstractOperations.ToBoolean;
import static com.github.anba.es6draft.util.TestInfo.filterTests;
import static com.github.anba.es6draft.util.TestInfo.toObjectArray;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
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
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.MozShellGlobalObject;
import com.github.anba.es6draft.repl.ShellGlobalObject;
import com.github.anba.es6draft.repl.StopExecutionException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Undefined;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestInfo;

/**
 * Test suite for the Mozilla js-tests.
 */
@RunWith(Parallelized.class)
public class MozillaJSTest {

    /**
     * Returns a {@link Path} which points to the test directory 'MOZ_JSTESTS'
     */
    private static Path testDir() {
        String testPath = System.getenv("MOZ_JSTESTS");
        return (testPath != null ? Paths.get(testPath) : null);
    }

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> mozillaSuiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'MOZ_JSTESTS'", testdir, notNullValue());
        assumeTrue("directy 'MOZ_JSTESTS' does not exist", Files.exists(testdir));
        List<MozTest> tests = filterTests(loadTests(testdir, testdir), "/jstests.list");
        return toObjectArray(tests);
    }

    private static class MozTest extends TestInfo {
        List<Entry<Condition, String>> conditions = new ArrayList<>();
        boolean random = false;

        public MozTest(Path script) {
            super(script);
        }

        void addCondition(Condition c, String s) {
            conditions.add(new SimpleEntry<>(c, s));
        }
    }

    private enum Condition {
        FailsIf, SkipIf, RandomIf
    }

    private static Set<CompatibilityOption> options = CompatibilityOption.MozCompatibility();
    private static ScriptCache scriptCache = new ScriptCache(options);
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
        World<MozShellGlobalObject> world = new World<>(newGlobalObjectAllocator(console,
                testDir(), moztest.script, Paths.get("test402/lib"), scriptCache), options);
        MozShellGlobalObject global = world.newGlobal();
        ExecutionContext cx = global.getRealm().defaultContext();

        // apply scripted conditions
        scriptConditions(cx, global);

        // filter disabled tests (may have changed after applying scripted conditions)
        assumeTrue(moztest.enable);

        // load legacy.js file
        global.eval(legacyMozilla);

        // load and execute shell.js files
        for (Path shell : shellJS(moztest)) {
            global.include(shell);
        }

        // patch $INCLUDE
        global.set(cx, "$INCLUDE", global.get(cx, "__$INCLUDE", global), global);

        // evaluate actual test-script
        Path js = testDir().resolve(moztest.script);
        try {
            global.eval(moztest.script, js);
        } catch (ParserException | CompilationException e) {
            // count towards the overall failure count
            console.getFailures().add(new AssertionError(e.getMessage(), e));
        } catch (ScriptException e) {
            // count towards the overall failure count
            console.getFailures().add(new AssertionError(e.getMessage(cx), e));
        } catch (StackOverflowError e) {
            // count towards the overall failure count
            console.getFailures().add(new AssertionError(e.getMessage(), e));
        } catch (StopExecutionException e) {
            // ignore
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // fail if any test returns with errors
        List<Throwable> failures = new ArrayList<Throwable>();
        failures.addAll(console.getFailures());
        if (moztest.random) {
            // results from random tests are ignored...
        } else if (moztest.expect) {
            MultipleFailureException.assertEmpty(failures);
        } else {
            assertFalse("Expected test to throw error", failures.isEmpty());
        }
    }

    /**
     * Returns an {@link Iterable} of 'shell.js'-{@link Path}s
     */
    private static Iterable<Path> shellJS(MozTest test) {
        // add 'shell.js' files from each directory
        List<Path> files = new ArrayList<>();
        Path testDir = testDir();
        Path dir = Paths.get("");
        for (Iterator<Path> iterator = test.script.iterator(); iterator.hasNext(); dir = dir
                .resolve(iterator.next())) {
            Path f = testDir.resolve(dir.resolve("shell.js"));
            if (Files.exists(f)) {
                files.add(f);
            }
        }
        return files;
    }

    private void scriptConditions(ExecutionContext cx, MozShellGlobalObject global) {
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
                moztest.enable = false;
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

    // Any file who's basename matches something in this set is ignored
    private static final Set<String> excludeFiles = new HashSet<>(asList("browser.js", "shell.js",
            "jsref.js", "template.js", "user.js", "js-test-driver-begin.js",
            "js-test-driver-end.js"));
    private static final Set<String> excludeDirs = new HashSet<>(asList("supporting", "test262"));

    private static List<MozTest> loadTests(Path searchdir, Path basedir) throws IOException {
        return TestInfo.loadTests(searchdir, basedir, excludeDirs, excludeFiles, new TestInfos());
    }

    private static class TestInfos implements BiFunction<Path, Iterator<String>, MozTest> {
        private static final Pattern testInfoPattern = Pattern.compile("//\\s*\\|(.+?)\\|\\s*(.*)");

        @Override
        public MozTest apply(Path script, Iterator<String> lines) {
            MozTest test = new MozTest(script);
            // negative tests end with "-n"
            if (script.getFileName().toString().endsWith("-n.js")) {
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
                    test.enable = false;
                } else if (p.equals("random")) {
                    test.random = true;
                } else if (p.equals("slow")) {
                    // don't run slow tests
                    test.enable = false;
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
