/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import static com.github.anba.es6draft.repl.V8ShellGlobalObject.newGlobal;
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
import com.github.anba.es6draft.repl.ShellGlobalObject;
import com.github.anba.es6draft.repl.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.UncheckedIOException;

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
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'TRACEUR_TEST'", testdir, notNullValue());
        assumeTrue("directy 'TRACEUR_TEST' does not exist", Files.exists(testdir));
        Path searchdir = testdir.resolve("feature");
        List<TestInfo> tests = filterTests(loadTests(searchdir, testdir), "/traceur.list");
        return toObjectArray(tests);
    }

    private static Set<CompatibilityOption> options = CompatibilityOption.WebCompatibility();
    private static ScriptCache scriptCache = new ScriptCache(Parser.Option.from(options));
    private static Script legacyJS, chaiJS;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Parameter(0)
    public TestInfo test;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            legacyJS = ShellGlobalObject.compileScript(scriptCache, "v8legacy.js");
            chaiJS = ShellGlobalObject.compileScript(scriptCache, "chai.js");
        }
    };

    @Test
    public void runTest() throws Throwable {
        // filter disabled tests
        assumeTrue(test.enable);

        // TODO: collect multiple failures
        List<Throwable> failures = new ArrayList<Throwable>();
        TraceurConsole console = new TraceurConsole();
        V8ShellGlobalObject global = newGlobal(console, testDir(), test.script, scriptCache,
                options);

        // load legacy.js file
        global.eval(legacyJS);
        global.eval(chaiJS);

        // load and execute test-utils.js file
        global.include(Paths.get("test-utils.js"));

        // evaluate actual test-script
        Path js = testDir().resolve(test.script);
        try {
            global.eval(test.script, js);
        } catch (ParserException | CompilationException e) {
            // count towards the overall failure count
            String message = e.getMessage();
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

    private static final Set<String> excludeFiles = new HashSet<>();
    private static final Set<String> excludeDirs = new HashSet<>(asList("Await", "Cascade",
            "Collection", "LegacyModules", "Modules", "PrivateNames", "PrivateNameSyntax",
            "PropertyMethodAssignment", "PropertyOptionalComma", "Types"));

    private static List<TestInfo> loadTests(Path searchdir, final Path basedir) throws IOException {
        BiFunction<Path, BufferedReader, TestInfo> create = new BiFunction<Path, BufferedReader, TestInfo>() {
            @Override
            public TestInfo apply(Path script, BufferedReader reader) {
                try {
                    return createTestInfo(script, reader);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };

        return TestInfo.loadTests(searchdir, basedir, excludeDirs, excludeFiles, create);
    }

    private static final Pattern FlagsPattern = Pattern.compile("\\s*//\\s*(.*)\\s*");

    private static TestInfo createTestInfo(Path script, BufferedReader reader) throws IOException {
        TestInfo test = new TestInfo(script);
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
        return test;
    }
}
