/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.repl.MozShellGlobalObject.newGlobal;
import static java.util.Arrays.asList;
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
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.util.Parallelized;

/**
 *
 */
@RunWith(Parallelized.class)
public class MozillaJitTest extends BaseMozillaTest {

    /**
     * Returns a {@link Path} which points to the test directory 'mozilla.js.tests'
     */
    private static Path testDir() {
        String testPath = System.getenv("MOZ_JITTESTS");
        return (testPath != null ? Paths.get(testPath) : null);
    }

    @Parameters(name = "{0}")
    public static Iterable<Object[]> mozillaSuiteValues() throws IOException {
        Path testdir = testDir();
        assumeThat("missing system property 'MOZ_JITTESTS'", testdir, notNullValue());
        assumeTrue("directy 'MOZ_JITTESTS' does not exist", Files.exists(testdir));
        List<MozTest> tests = new ArrayList<>();
        List<String> dirs = asList("arguments", "arrow-functions", "auto-regress", "basic",
                "closures", "collections", "for-of", "pic", "proxy", "self-hosting");
        for (String dir : dirs) {
            Path p = testdir.resolve(Paths.get("tests", dir));
            tests.addAll(loadTests(p, testdir));
        }
        tests = filterTests(tests, "/jittests.list");
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
        // don't run debug-mode tests
        assumeFalse(moztest.debug);

        MozTestConsole console = new MozTestConsole();
        MozShellGlobalObject global = newGlobal(console, testDir(), moztest.script, Paths.get(""),
                scriptCache, legacyMozilla);

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
        } catch (ParserException e) {
            // count towards the overall failure count
            String message = String.format("%s: %s", e.getExceptionType(), e.getMessage());
            if (moztest.error == null || !(message.contains(moztest.error))) {
                console.getFailures().add(new AssertionError(message, e));
            }
        } catch (ScriptException e) {
            // count towards the overall failure count
            String message = e.getMessage();
            if (moztest.error == null || !(message.contains(moztest.error))) {
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
        if (moztest.random) {
            // results from random tests are ignored...
        } else if (moztest.expect) {
            MultipleFailureException.assertEmpty(failures);
        } else {
            assertFalse("Expected test to throw error", failures.isEmpty());
        }
    }

}
