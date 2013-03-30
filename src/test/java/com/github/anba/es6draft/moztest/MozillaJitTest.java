/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.Realm.GlobalObjectCreator;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ScriptCache;

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

    private static ScriptCache scriptCache = new ScriptCache(StandardCharsets.ISO_8859_1);
    private static Script legacyMozilla;

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(15));

    @Parameter(0)
    public MozTest moztest;

    @ClassRule
    public static ExternalResource resource = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            String sourceName = "mozlegacy.js";
            InputStream stream = MozillaJSTest.class.getResourceAsStream("/" + sourceName);
            legacyMozilla = scriptCache.script(sourceName, stream);
        }
    };

    @Test
    public void runMozillaTest() throws Throwable {
        final MozTest moztest = this.moztest;
        // filter disabled tests
        assumeTrue(moztest.enable);
        // don't run slow tests
        assumeTrue(!moztest.slow);

        Realm realm = Realm.newRealm(new GlobalObjectCreator<MozTestGlobalObject>() {
            @Override
            public MozTestGlobalObject createGlobal(Realm realm) {
                return new MozTestGlobalObject(realm, testDir(), moztest.script, scriptCache);
            }
        });

        // start initialization
        MozTestGlobalObject global = (MozTestGlobalObject) realm.getGlobalThis();
        createProperties(global, realm, MozTestGlobalObject.class);

        // load legacy mozilla
        global.evaluate(legacyMozilla);

        // load and execute prolog.js files
        global.include(Paths.get("lib/prolog.js"));

        // set required global variables
        global.set(realm, "libdir", "lib/", global);
        global.set(realm, "environment",
                OrdinaryObject.ObjectCreate(realm, Intrinsics.ObjectPrototype), global);

        // evaluate actual test-script
        Path js = testDir().resolve(moztest.script);
        try {
            global.eval(js);
            if (moztest.error != null) {
                fail("Expected exception: " + moztest.error);
            }
        } catch (ParserException e) {
            // count towards the overall failure count
            String message = String.format("%s: %s", e.getExceptionType().name(), e.getMessage());
            if (moztest.error == null || !(message.contains(moztest.error))) {
                global.getFailures().add(new AssertionError(message, e));
            }
        } catch (ScriptException e) {
            // count towards the overall failure count
            String message = e.getMessage();
            if (moztest.error == null || !(message.contains(moztest.error))) {
                global.getFailures().add(new AssertionError(message, e));
            }
        } catch (StopExecutionException e) {
            // ignore
        } catch (IOException e) {
            fail(e.getMessage());
        }

        // fail if any test returns with errors
        List<Throwable> failures = new ArrayList<Throwable>();
        failures.addAll(global.getFailures());
        if (moztest.random) {
            // results from random tests are ignored...
        } else if (moztest.expect) {
            MultipleFailureException.assertEmpty(failures);
        } else {
            assertFalse("Expected test to throw error", failures.isEmpty());
        }
    }

}
