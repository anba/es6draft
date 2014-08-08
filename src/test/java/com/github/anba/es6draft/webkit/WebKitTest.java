/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import static com.github.anba.es6draft.repl.global.V8ShellGlobalObject.newGlobalObjectAllocator;
import static com.github.anba.es6draft.runtime.internal.Properties.createProperties;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTestsAsArray;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.model.MultipleFailureException;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.V8ShellGlobalObject;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 *
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "webkit.test", file = "resource:/test-configuration.properties")
public class WebKitTest {
    private static final Configuration configuration = loadConfiguration(WebKitTest.class);

    @Parameters(name = "{0}")
    public static Iterable<Object[]> suiteValues() throws IOException {
        return loadTestsAsArray(configuration, new BiFunction<Path, Path, TestInfo>() {
            @Override
            public TestInfo apply(Path basedir, Path file) {
                return new WebKitTestInfo(basedir, file);
            }
        });
    }

    @ClassRule
    public static TestGlobals<V8ShellGlobalObject, TestInfo> globals = new TestGlobals<V8ShellGlobalObject, TestInfo>(
            configuration) {
        @Override
        protected ObjectAllocator<V8ShellGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return newGlobalObjectAllocator(console, test.getBaseDir(), test.getScript(),
                    scriptCache);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public WebKitTestInfo test;

    private static class WebKitTestInfo extends TestInfo {
        final boolean expect;

        public WebKitTestInfo(Path basedir, Path script) {
            super(basedir, script);
            // negative tests end with "-n"
            this.expect = !script.getFileName().toString().endsWith("-n.js");
        }
    }

    private V8ShellGlobalObject global;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // Filter disabled tests
        assumeTrue(test.isEnabled());

        global = globals.newGlobal(new WebKitTestConsole(collector), test);
        ExecutionContext cx = global.getRealm().defaultContext();
        exceptionHandler.setExecutionContext(cx);

        ScriptObject globalThis = global.getRealm().getGlobalThis();
        createProperties(cx, globalThis, new WebKitNatives(), WebKitNatives.class);
        globalThis.set(cx, "window", globalThis, globalThis);

        if (test.expect) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            expected.expect(Matchers.either(StandardErrorHandler.defaultMatcher())
                    .or(ScriptExceptionHandler.defaultMatcher())
                    .or(Matchers.instanceOf(MultipleFailureException.class)));
        }
    }

    @After
    public void tearDown() {
        if (global != null) {
            global.getScriptLoader().getExecutor().shutdown();
        }
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        // - load and execute pre and post before resp. after test-script
        global.include(Paths.get("resources/standalone-pre.js"));
        global.eval(test.getScript(), test.toFile());
        global.include(Paths.get("resources/standalone-post.js"));

        // Wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop();
    }

    public static final class WebKitNatives {
        @Properties.Function(name = "neverInlineFunction", arity = 0)
        public void neverInlineFunction() {
        }

        @Properties.Function(name = "numberOfDFGCompiles", arity = 0)
        public double numberOfDFGCompiles() {
            return Double.NaN;
        }
    }
}
