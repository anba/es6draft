/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.repl.MozShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.util.TestInfo.toObjectArray;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.MozShellGlobalObject;
import com.github.anba.es6draft.repl.ShellGlobalObject;
import com.github.anba.es6draft.repl.StopExecutionException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestInfo;

/**
 * Test suite for the Reflect API tests.
 */
@RunWith(Parallelized.class)
public class ReflectTest {

    /**
     * Returns a {@link Path} which points to the test directory
     */
    private static Path testDir() {
        return Paths.get("src/test/scripts/reflect");
    }

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> mozillaSuiteValues() throws IOException {
        Path testdir = testDir();
        assumeTrue("directy does not exist", Files.exists(testdir));
        List<TestInfo> tests = loadTests(testdir, testdir);
        return toObjectArray(tests);
    }

    private static Set<CompatibilityOption> options = CompatibilityOption.MozCompatibility();
    private static ScriptCache scriptCache = new ScriptCache(options);
    private static Script legacyMozilla;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Parameter(0)
    public TestInfo test;

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
        assumeTrue(test.enable);

        MozTestConsole console = new MozTestConsole();
        World<MozShellGlobalObject> world = new World<>(newGlobalObjectAllocator(console,
                testDir(), test.script, scriptCache), options);
        MozShellGlobalObject global = world.newGlobal();
        ExecutionContext cx = global.getRealm().defaultContext();

        // simplistic reportCompare
        global.set(cx, "reportCompare", global.get(cx, "assertEq", global), global);

        // load legacy.js file
        global.eval(legacyMozilla);

        // load and execute shell.js file
        global.include(Paths.get("shell.js"));

        // evaluate actual test-script
        Path js = testDir().resolve(test.script);
        try {
            global.eval(test.script, js);
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
        if (test.expect) {
            MultipleFailureException.assertEmpty(failures);
        } else {
            assertFalse("Expected test to throw error", failures.isEmpty());
        }
    }

    // Any file who's basename matches something in this set is ignored
    private static final Set<String> excludeFiles = new HashSet<>(asList("shell.js"));
    private static final Set<String> excludeDirs = new HashSet<>();

    private static List<TestInfo> loadTests(Path searchdir, Path basedir) throws IOException {
        return TestInfo.loadTests(searchdir, basedir, excludeDirs, excludeFiles);
    }
}
