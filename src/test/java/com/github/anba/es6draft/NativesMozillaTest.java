/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.internal.ScriptLoading;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.SystemConsole;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestRealm;
import com.github.anba.es6draft.util.TestRealms;
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
    public static TestRealms<TestInfo> realms = new TestRealms<TestInfo>(configuration, NativeTestRealmData::new) {
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

    @Rule
    public TestRealm<TestInfo> realm = new TestRealm<>(realms);

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        realm.initialize(new SystemConsole(), test);
        exceptionHandler.setExecutionContext(realm.get().defaultContext());
    }

    @Test
    public void runTest() throws Throwable {
        realm.execute(test);
    }

    public static final class NativeTestRealmData extends RealmData {
        NativeTestRealmData(Realm realm) {
            super(realm);
        }

        @Override
        public void initializeScripted() throws IOException {
            ScriptLoading.evalNative(getRealm(), "arraybuffer.js");
            ScriptLoading.evalNative(getRealm(), "console-stub.js");
            ScriptLoading.evalNative(getRealm(), "global.js");
            ScriptLoading.evalNative(getRealm(), "source.js");
            ScriptLoading.evalNative(getRealm(), "statics.js");
        }
    }
}
