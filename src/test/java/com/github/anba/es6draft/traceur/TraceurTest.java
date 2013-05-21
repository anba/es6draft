/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import static com.github.anba.es6draft.repl.V8ShellGlobalObject.newGlobal;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.MultipleFailureException;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.util.Parallelized;

/**
 *
 */
@RunWith(Parallelized.class)
public class TraceurTest {

    /**
     * Returns a {@link Path} which points to the test directory 'traceur.test'
     */
    private static Path testDir() {
        String testPath = System.getenv("TRACEUR_TEST");
        return (testPath != null ? Paths.get(testPath) : null);
    }

    @Parameters(name = "{0}")
    public static Iterable<Object[]> suiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'TRACEUR_TEST'", testdir, notNullValue());
        assumeTrue("directy 'TRACEUR_TEST' does not exist", Files.exists(testdir));
        Path searchdir = testdir.resolve("feature");
        List<TestInfo> tests = filterTests(loadTests(searchdir, testdir), "/traceur.list");
        return toObjectArray(tests);
    }

    private static ScriptCache scriptCache = new ScriptCache();
    private static Script legacyJS, chaiJS;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Parameter(0)
    public TestInfo test;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            legacyJS = V8ShellGlobalObject.compileLegacy(scriptCache);
            chaiJS = V8ShellGlobalObject.compileScript(scriptCache, "chai.js");
        }
    };

    @Test
    public void runTest() throws Throwable {
        TestInfo test = this.test;
        // filter disabled tests
        assumeTrue(test.enable);

        // TODO: collect multiple failures
        List<Throwable> failures = new ArrayList<Throwable>();
        TraceurConsole console = new TraceurConsole();
        V8ShellGlobalObject global = newGlobal(console, testDir(), test.script, scriptCache);

        // load legacy.js file
        global.eval(legacyJS);
        global.eval(chaiJS);

        // load and execute mjsunit.js file
        global.include(Paths.get("test-utils.js"));

        // evaluate actual test-script
        Path js = testDir().resolve(test.script);
        try {
            global.eval(test.script, js);
        } catch (ParserException e) {
            // count towards the overall failure count
            String message = String.format("%s: %s", e.getExceptionType(), e.getMessage());
            failures.add(new AssertionError(message, e));
        } catch (ScriptException e) {
            // count towards the overall failure count
            String message = e.getMessage(global.getRealm().defaultContext());
            failures.add(new AssertionError(message, e));
        } catch (IOException e) {
            fail(e.getMessage());
        }

        if (test.expect) {
            // fail if any test returns with errors
            MultipleFailureException.assertEmpty(failures);
        } else {
            assertFalse("Expected test to throw error", failures.isEmpty());
        }
    }

    static class TestInfo {
        Path script;
        boolean enable = true;
        boolean expect = true;

        @Override
        public String toString() {
            return script.toString();
        }
    }

    private static final Set<String> excludedSet = new HashSet<>(asList(""));
    private static final Set<String> excludeDirs = new HashSet<>(asList("Await", "Cascade",
            "Collection", "Modules", "PrivateNames", "PrivateNameSyntax",
            "PropertyMethodAssignment", "PropertyOptionalComma", "Types"));

    private static List<TestInfo> loadTests(Path searchdir, final Path basedir) throws IOException {
        final List<TestInfo> tests = new ArrayList<>();
        Files.walkFileTree(searchdir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (excludeDirs.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (attrs.isRegularFile() && attrs.size() != 0L) {
                    String name = file.getFileName().toString();
                    if (!excludedSet.contains(name) && name.endsWith(".js")) {
                        TestInfo test = new TestInfo();
                        test.script = basedir.relativize(file);
                        try (BufferedReader reader = Files.newBufferedReader(file,
                                StandardCharsets.UTF_8)) {
                            applyFlagsInfo(test, reader);
                        }
                        tests.add(test);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return tests;
    }

    private static final Pattern FlagsPattern = Pattern.compile("\\s*//\\s*(.*)\\s*");

    private static void applyFlagsInfo(TestInfo test, BufferedReader reader) throws IOException {
        Pattern p = FlagsPattern;
        for (String line; (line = reader.readLine()) != null;) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                String s = m.group(1);
                if ("Should not compile.".equals(s)) {
                    test.expect = false;
                } else if ("Only in browser.".equals(s) || s.startsWith("Skip.")) {
                    test.enable = false;
                } else if (s.startsWith("Error:")) {
                    // ignore
                } else if (s.startsWith("Options:")) {
                    // ignore
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private static List<TestInfo> filterTests(List<TestInfo> tests, String filename)
            throws IOException {
        // list->map
        Map<Path, TestInfo> map = new LinkedHashMap<>();
        for (TestInfo test : tests) {
            map.put(test.script, test);
        }
        // disable tests
        List<TestInfo> disabledTests = new ArrayList<>();
        InputStream res = TraceurTest.class.getResourceAsStream(filename);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                TestInfo t = map.get(Paths.get(line));
                if (t == null) {
                    System.err.printf("detected stale entry '%s'\n", line);
                    continue;
                }
                disabledTests.add(t);
                t.enable = false;
            }
        }
        System.out.printf("disabled %d tests of %d in total%n", disabledTests.size(), tests.size());
        return tests;
    }

    /**
     * {@link Parameterized} expects a list of {@code Object[]}
     */
    private static Iterable<Object[]> toObjectArray(Iterable<?> iterable) {
        List<Object[]> list = new ArrayList<Object[]>();
        for (Object o : iterable) {
            list.add(new Object[] { o });
        }
        return list;
    }
}
