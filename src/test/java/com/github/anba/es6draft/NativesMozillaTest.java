/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;
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
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.NativeCode;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.SystemConsole;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * Test suite for the native, mozilla tests.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "natives.test.mozilla", file = "resource:/test-configuration.properties")
public final class NativesMozillaTest {
    private static final Configuration configuration = loadConfiguration(NativesMozillaTest.class);

    @Parameters(name = "{0}")
    public static List<TestInfo> suiteValues() throws IOException {
        return loadTests(configuration);
    }

    @ClassRule
    public static TestGlobals<MozNativeTestGlobalObject, TestInfo> globals = new TestGlobals<MozNativeTestGlobalObject, TestInfo>(
            configuration, MozNativeTestGlobalObject::new) {
        @Override
        protected EnumSet<Parser.Option> getParserOptions() {
            return EnumSet.of(Parser.Option.NativeCall);
        }
    };

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public TestInfo test;

    private MozNativeTestGlobalObject global;

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        global = globals.newGlobal(new SystemConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());
    }

    @After
    public void tearDown() {
        globals.release(global);
    }

    @Test
    public void runTest() throws Throwable {
        // Evaluate actual test-script
        global.eval(test.getScript(), test.toFile());

        // Wait for pending tasks to finish
        global.getRealm().getWorld().runEventLoop();
    }

    public static final class MozNativeTestGlobalObject extends ShellGlobalObject {
        MozNativeTestGlobalObject(Realm realm) {
            super(realm);
        }

        @Override
        public void initializeScripted() throws IOException, URISyntaxException, ParserException, CompilationException {
            NativeCode.load(getRealm(), "arraybuffer.js");
            NativeCode.load(getRealm(), "collection.js");
            NativeCode.load(getRealm(), "global.js");
            NativeCode.load(getRealm(), "iterator.js");
            NativeCode.load(getRealm(), "legacy-generator.js");
            NativeCode.load(getRealm(), "object.js");
            NativeCode.load(getRealm(), "proxy.js");
            NativeCode.load(getRealm(), "simd.js");
            NativeCode.load(getRealm(), "source.js");
            NativeCode.load(getRealm(), "statics.js");
            NativeCode.load(getRealm(), "string.js");
            NativeCode.load(getRealm(), "stringtrim.js");
        }
    }
}
