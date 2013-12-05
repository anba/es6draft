/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.repl.SimpleShellGlobalObject.newGlobalObjectAllocator;
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.MultipleFailureException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.SimpleShellGlobalObject;
import com.github.anba.es6draft.runtime.World;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestInfo;

/**
 *
 */
@RunWith(Parallelized.class)
public class ScriptTest {

    /**
     * Returns a {@link Path} which points to the test directory
     */
    private static Path testDir() {
        return Paths.get("src/test/scripts/suite");
    }

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        Path testdir = testDir();
        assumeTrue("directy does not exist", Files.exists(testdir));
        List<TestInfo> tests = loadTests(testdir, testdir);
        return toObjectArray(tests);
    }

    private static Set<CompatibilityOption> options = CompatibilityOption.WebCompatibility();
    private static ScriptCache scriptCache = new ScriptCache(Parser.Option.from(options));

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Parameter(0)
    public TestInfo test;

    @Test
    public void runTest() throws Throwable {
        // filter disabled tests
        assumeTrue(test.enable);

        // TODO: collect multiple failures
        List<Throwable> failures = new ArrayList<Throwable>();

        ScriptTestConsole console = new ScriptTestConsole();
        World<SimpleShellGlobalObject> world = new World<>(newGlobalObjectAllocator(console,
                testDir(), test.script, scriptCache), options);
        SimpleShellGlobalObject global = world.newGlobal();

        // load and execute assert.js file
        global.include(Paths.get("lib/assert.js"));

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

        while (world.hasTasks()) {
            try {
                world.executeTasks(global.getRealm().defaultContext());
            } catch (ScriptException e) {
                // count towards the overall failure count (?)
                String message = e.getMessage(global.getRealm().defaultContext());
                failures.add(new AssertionError(message, e));
            }
        }

        if (test.expect) {
            // fail if any test returns with errors
            MultipleFailureException.assertEmpty(failures);
        } else {
            assertFalse("Expected test to throw error", failures.isEmpty());
        }
    }

    // Any file who's basename matches something in this set is ignored
    private static final Set<String> excludeFiles = new HashSet<>();
    private static final Set<String> excludeDirs = new HashSet<>(asList("lib"));

    private static List<TestInfo> loadTests(Path searchdir, Path basedir) throws IOException {
        return TestInfo.loadTests(searchdir, basedir, excludeDirs, excludeFiles);
    }
}
