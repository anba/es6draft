/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8test;

import static com.github.anba.es6draft.repl.V8ShellGlobalObject.newGlobal;
import static com.github.anba.es6draft.util.TestInfo.filterTests;
import static com.github.anba.es6draft.util.TestInfo.toObjectArray;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import com.github.anba.es6draft.runtime.ExecutionContext;
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
public class IntlTest {

    /**
     * Returns a {@link Path} which points to the test directory 'v8.test.intl'
     */
    private static Path testDir() {
        String testPath = System.getenv("V8_INTL");
        return (testPath != null ? Paths.get(testPath) : null);
    }

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'V8_INTL'", testdir, notNullValue());
        assumeTrue("directy 'V8_INTL' does not exist", Files.exists(testdir));
        List<TestInfo> tests = filterTests(loadTests(testdir, testdir), "/intl.list");
        return toObjectArray(tests);
    }

    private static Set<CompatibilityOption> options = CompatibilityOption.WebCompatibility();
    private static ScriptCache scriptCache = new ScriptCache(Parser.Option.from(options));
    private static Script legacyJS;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Parameter(0)
    public TestInfo test;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            legacyJS = ShellGlobalObject.compileScript(scriptCache, "v8legacy.js");
        }
    };

    @Test
    public void runTest() throws Throwable {
        // filter disabled tests
        assumeTrue(test.enable);

        // TODO: collect multiple failures
        V8TestConsole console = new V8TestConsole();
        V8ShellGlobalObject global = newGlobal(console, testDir(), test.script, scriptCache,
                options);

        // load legacy.js file
        global.eval(legacyJS);

        // load and execute assert.js file
        global.include(Paths.get("assert.js"));

        // load and execute utils.js file
        global.include(Paths.get("utils.js"));

        // load and execute date-format/utils.js file
        global.include(Paths.get("date-format/utils.js"));

        // evaluate actual test-script
        Path js = testDir().resolve(test.script);
        try {
            global.eval(test.script, js);
        } catch (ParserException | CompilationException e) {
            // count towards the overall failure count
            console.getFailures().add(new AssertionError(e.getMessage(), e));
        } catch (ScriptException e) {
            // count towards the overall failure count
            ExecutionContext cx = global.getRealm().defaultContext();
            console.getFailures().add(new AssertionError(e.getMessage(cx), e));
        } catch (StackOverflowError e) {
            // count towards the overall failure count
            console.getFailures().add(new AssertionError(e.getMessage(), e));
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // fail if any test returns with errors
        MultipleFailureException.assertEmpty(console.getFailures());
    }

    private static final Set<String> excludeFiles = new HashSet<>(asList("assert.js", "utils.js"));
    private static final Set<String> excludeDirs = new HashSet<>(asList("break-iterator"));

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

    private static TestInfo createTestInfo(Path script, BufferedReader reader) throws IOException {
        // no special flags present in v8-intl tests
        return new TestInfo(script);
    }
}
