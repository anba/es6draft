/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static com.github.anba.es6draft.util.matchers.ErrorMessageMatcher.hasErrorMessage;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ResolutionException;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;
import com.github.anba.es6draft.util.NullConsole;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestModuleLoader;
import com.github.anba.es6draft.util.TestRealm;
import com.github.anba.es6draft.util.TestRealms;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StopExecutionHandler;

/**
 * Test suite for the Mozilla jit-tests.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "mozilla.test.jittests", file = "resource:/test-configuration.properties")
public final class MozillaJitTest {
    private static final Configuration configuration = loadConfiguration(MozillaJitTest.class);
    private static final Set<String> ignoreFlags = Resources.set(configuration, "flags.ignore", Collections.emptySet());
    private static final Set<String> disableFlags = Resources.set(configuration, "flags.disable",
            Collections.emptySet());
    private static final boolean warnUnknownFlag = configuration.getBoolean("flags.warn", false);

    @Parameters(name = "{0}")
    public static List<MozTest> suiteValues() throws IOException {
        return loadTests(configuration, MozTest::new, MozillaJitTest::configureTest);
    }

    @BeforeClass
    public static void setUpClass() {
        MozTestRealmData.testLoadInitializationScript();
    }

    @ClassRule
    public static TestRealms<TestInfo> realms = new TestRealms<TestInfo>(configuration, MozTestRealmData::new) {
        @Override
        protected BiFunction<RuntimeContext, ScriptLoader, TestModuleLoader<?>> getModuleLoader() {
            return MozJitFileModuleLoader::new;
        }

        @Override
        protected Supplier<MozContextData> getRuntimeData() {
            return MozContextData::new;
        }
    };

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ScriptExceptionHandler exceptionHandler = ScriptExceptionHandler.none();

    @Rule
    public StopExecutionHandler stopHandler = new StopExecutionHandler();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public MozTest test;

    private static final class MozTest extends TestInfo {
        String error = null;
        boolean module = false;

        public MozTest(Path basedir, Path script) {
            super(basedir, script);
        }

        @Override
        public boolean isModule() {
            return module;
        }
    }

    @Rule
    public TestRealm<TestInfo> realm = new TestRealm<>(realms);

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        realm.initialize(new NullConsole(), test);
        exceptionHandler.setExecutionContext(realm.get().defaultContext());
        realm.get().createGlobalProperties(new TestEnvironment(), TestEnvironment.class);

        if (test.error == null) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
            exceptionHandler.match(ScriptExceptionHandler.defaultMatcher());
        } else {
            expected.expect(
                    Matchers.either(StandardErrorHandler.defaultMatcher()).or(ScriptExceptionHandler.defaultMatcher()));
            expected.expect(hasErrorMessage(realm.get().defaultContext(), containsString(test.error)));
        }
    }

    @Test
    public void runTest() throws Throwable {
        try {
            realm.execute(test);
        } catch (ResolutionException e) {
            throw e.toScriptException(realm.get().defaultContext());
        }
    }

    public static final class TestEnvironment {
        @Properties.Value(name = "libdir")
        public String libdir() {
            return "lib/";
        }

        @Properties.Value(name = "environment")
        public ScriptObject environment(ExecutionContext cx) {
            return OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
        }

        @Properties.Value(name = "os")
        public ScriptObject os(ExecutionContext cx) {
            OrdinaryObject os = OrdinaryObject.ObjectCreate(cx, Intrinsics.ObjectPrototype);
            Properties.createProperties(cx, os, new OS(), OS.class);
            return os;
        }
    }

    public static final class OS {
        @Properties.Function(name = "getenv", arity = 1)
        public String getenv(String name) {
            return System.getenv().get(name);
        }
    }

    private static final Pattern testInfoPattern = Pattern.compile("//\\s*\\|(.+?)\\|\\s*(.*)");

    private static void configureTest(MozTest test, Stream<String> lines) {
        String line = lines.findFirst().orElseThrow(IllegalArgumentException::new);
        Matcher m = testInfoPattern.matcher(line);
        if (!m.matches()) {
            // Ignore if pattern invalid or not present
            return;
        }
        if (!"jit-test".equals(m.group(1))) {
            System.err.printf("invalid tag '%s' in line: %s\n", m.group(1), line);
            return;
        }
        String content = m.group(2);
        for (String p : content.split(";")) {
            int sep = Math.max(p.indexOf(':'), Math.max(p.indexOf('='), -1));
            String name, value;
            if (sep != -1) {
                name = p.substring(0, sep).trim();
                value = p.substring(sep + 1).trim();
            } else {
                name = p.trim();
                value = null;
            }
            if (name.isEmpty() || ignoreFlags.contains(name)) {
                // Ignored flags
            } else if (disableFlags.contains(name)) {
                test.setEnabled(false);
            } else if ("error".equals(name) && value != null) {
                test.error = value;
            } else if ("module".equals(name)) {
                test.module = true;
            } else if (warnUnknownFlag) {
                System.err.printf("unknown option '%s' in line: %s [%s]%n", name, content, test.getScript());
            }
        }
    }
}
