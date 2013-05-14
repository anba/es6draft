/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.repl.MozShellGlobalObject.newGlobal;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.MozShellGlobalObject;
import com.github.anba.es6draft.repl.StopExecutionException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.util.Parallelized;

/**
 * Test suite for the Mozilla js-tests.
 */
@RunWith(Parallelized.class)
public class MozillaJSTest extends BaseMozillaTest {

    /**
     * Returns a {@link Path} which points to the test directory 'MOZ_JSTESTS'
     */
    private static Path testDir() {
        String testPath = System.getenv("MOZ_JSTESTS");
        return (testPath != null ? Paths.get(testPath) : null);
    }

    @Parameters(name = "{0}")
    public static Iterable<Object[]> mozillaSuiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'MOZ_JSTESTS'", testdir, notNullValue());
        assumeTrue("directy 'MOZ_JSTESTS' does not exist", Files.exists(testdir));
        List<MozTest> tests = filterTests(loadTests(testdir, testdir), "/jstests.list");
        return toObjectArray(tests);
    }

    private static ScriptCache scriptCache = new ScriptCache();
    private static Script legacyMozilla;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Parameter(0)
    public MozTest moztest;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            legacyMozilla = MozShellGlobalObject.compileLegacy(scriptCache);
        }
    };

    @Test
    public void runMozillaTest() throws Throwable {
        MozTest moztest = this.moztest;
        // filter disabled tests
        assumeTrue(moztest.enable);
        // don't run slow tests
        assumeFalse(moztest.slow);

        MozTestConsole console = new MozTestConsole();
        MozShellGlobalObject global = newGlobal(console, testDir(), moztest.script,
                Paths.get("test402/lib"), scriptCache, legacyMozilla);

        // load and execute shell.js files
        for (Path shell : shellJS(moztest)) {
            global.include(shell);
        }

        // patch $INCLUDE
        ExecutionContext cx = global.getRealm().defaultContext();
        global.set(cx, "$INCLUDE", global.get(cx, "__$INCLUDE", global), global);

        // evaluate actual test-script
        Path js = testDir().resolve(moztest.script);
        try {
            global.eval(moztest.script, js);
        } catch (ParserException e) {
            // count towards the overall failure count
            console.getFailures().add(new AssertionError(e.getMessage(), e));
        } catch (ScriptException e) {
            // count towards the overall failure count
            console.getFailures().add(new AssertionError(e.getMessage(cx), e));
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
}
