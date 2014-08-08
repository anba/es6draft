/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTestsAsArray;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.MozShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * Test suite for the native, mozilla tests.
 */
@RunWith(Parallelized.class)
@TestConfiguration(name = "natives.test.mozilla", file = "resource:/test-configuration.properties")
public final class NativesMozillaTest {
    private static final Configuration configuration = loadConfiguration(NativesMozillaTest.class);

    @Parameters(name = "{0}")
    public static Iterable<Object[]> suiteValues() throws IOException {
        return loadTestsAsArray(configuration);
    }

    @ClassRule
    public static TestGlobals<MozNativeTestGlobalObject, TestInfo> globals = new TestGlobals<MozNativeTestGlobalObject, TestInfo>(
            configuration) {
        @Override
        protected ObjectAllocator<MozNativeTestGlobalObject> newAllocator(ShellConsole console,
                TestInfo test, ScriptCache scriptCache) {
            return MozNativeTestGlobalObject.newTestGlobalObjectAllocator(console,
                    test.getBaseDir(), test.getScript(), scriptCache);
        }

        @Override
        protected EnumSet<Parser.Option> getParserOptions() {
            return EnumSet.of(Parser.Option.NativeCall);
        }

        @Override
        protected EnumSet<Compiler.Option> getCompilerOptions() {
            return EnumSet.of(Compiler.Option.VerifyStack);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout((int) TimeUnit.SECONDS.toMillis(120));

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    private MozNativeTestGlobalObject global;

    @Before
    public void setUp() throws IOException, URISyntaxException {
        // filter disabled tests
        assumeTrue(test.isEnabled());

        global = globals.newGlobal(new ScriptTestConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
    }

    @After
    public void tearDown() {
        if (global != null) {
            global.getScriptLoader().getExecutor().shutdown();
        }
    }

    @Test
    public void runTest() throws Throwable {
        // evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop();
    }

    public static final class MozNativeTestGlobalObject extends MozShellGlobalObject {
        protected MozNativeTestGlobalObject(Realm realm, ShellConsole console, Path baseDir,
                Path script, ScriptCache scriptCache) {
            super(realm, console, baseDir, script, scriptCache);
        }

        @Override
        public void initializeScripted() throws IOException, URISyntaxException, ParserException,
                CompilationException {
            // super.initializeScripted();
            includeNative(getScriptURL("collection.js"));
            includeNative(getScriptURL("compat.js"));
            includeNative(getScriptURL("iterator.js"));
            includeNative(getScriptURL("legacy-generator.js"));
            includeNative(getScriptURL("number.js"));
            includeNative(getScriptURL("proxy.js"));
            includeNative(getScriptURL("source.js"));
            includeNative(getScriptURL("statics.js"));
            includeNative(getScriptURL("string.js"));
            includeNative(getScriptURL("typed-array.js"));
        }

        public static ObjectAllocator<MozNativeTestGlobalObject> newTestGlobalObjectAllocator(
                final ShellConsole console, final Path baseDir, final Path script,
                final ScriptCache scriptCache) {
            return new ObjectAllocator<MozNativeTestGlobalObject>() {
                @Override
                public MozNativeTestGlobalObject newInstance(Realm realm) {
                    return new MozNativeTestGlobalObject(realm, console, baseDir, script,
                            scriptCache);
                }
            };
        }
    }
}
