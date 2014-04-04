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
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
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
import com.github.anba.es6draft.util.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Functional.Function;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;

/**
 *
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "webkit.test", file = "resource:/test-configuration.properties")
public class WebKitTest {
    private static final Configuration configuration = loadConfiguration(WebKitTest.class);

    @Parameters(name = "{0}")
    public static Iterable<TestInfo[]> suiteValues() throws IOException {
        return loadTests(configuration,
                new Function<Path, BiFunction<Path, Iterator<String>, TestInfo>>() {
                    @Override
                    public TestInfos apply(Path basedir) {
                        return new TestInfos(basedir);
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
        boolean expect = true;

        public WebKitTestInfo(Path basedir, Path script) {
            super(basedir, script);
        }
    }

    private V8ShellGlobalObject global;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // filter disabled tests
        assumeTrue(test.isEnabled());

        global = globals.newGlobal(new WebKitTestConsole(collector), test);
        ExecutionContext cx = global.getRealm().defaultContext();
        exceptionHandler.setExecutionContext(cx);

        createProperties(cx, global, new WebKitNatives(), WebKitNatives.class);
        global.set(cx, "window", global, global);

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
            global.getRealm().getExecutor().shutdown();
        }
    }

    @Test
    public void runTest() throws Throwable {
        // evaluate actual test-script
        // - load and execute pre and post before resp. after test-script
        global.include(Paths.get("resources/standalone-pre.js"));
        global.eval(test.getScript(), test.toFile());
        global.include(Paths.get("resources/standalone-post.js"));
    }

    public static final class WebKitNatives {
        @Properties.Function(name = "neverInlineFunction", arity = 0)
        public void neverInlineFunction() {
        }

        @Properties.Function(name = "numberOfDFGCompiles", arity = 0)
        public void numberOfDFGCompiles() {
        }
    }

    private static class TestInfos implements BiFunction<Path, Iterator<String>, TestInfo> {
        private final Path basedir;

        public TestInfos(Path basedir) {
            this.basedir = basedir;
        }

        @Override
        public TestInfo apply(Path file, Iterator<String> lines) {
            WebKitTestInfo test = new WebKitTestInfo(basedir, file);
            // negative tests end with "-n"
            if (file.getFileName().toString().endsWith("-n.js")) {
                test.expect = false;
            }
            return test;
        }
    }
}
