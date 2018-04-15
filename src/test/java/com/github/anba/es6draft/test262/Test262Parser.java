/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.test262;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.runtime.RealmData;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.internal.Source;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;
import com.github.anba.es6draft.util.SystemConsole;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestRealms;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * The standard test262-parser test suite
 */
@Ignore
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "test262-parser", file = "resource:/test-configuration.properties")
public final class Test262Parser {
    private static final Configuration configuration = loadConfiguration(Test262Parser.class);

    @Parameters(name = "{0}")
    public static List<Test262ParserInfo> suiteValues() throws IOException {
        return Resources.loadTests(configuration, Test262ParserInfo::new);
    }

    @ClassRule
    public static TestRealms<Test262ParserInfo> realms = new TestRealms<>(configuration, RealmData::new);

    @Rule
    public StandardErrorHandler errorHandler = StandardErrorHandler.none();

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Parameter(0)
    public Test262ParserInfo test;

    @Before
    public void setUp() throws Throwable {
        // Filter disabled tests
        assumeTrue("Test disabled", test.isEnabled());

        if (!test.isNegative()) {
            errorHandler.match(StandardErrorHandler.defaultMatcher());
        } else {
            expected.expect(StandardErrorHandler.defaultMatcher());
        }
    }

    @Test
    public void runTest() throws Throwable {
        RuntimeContext context = realms.newContext(new SystemConsole(), test);
        ScriptLoader loader = new ScriptLoader(context);
        Source source = new Source(test.toFile(), test.getScript().toString(), 1);
        String sourceCode = new String(Files.readAllBytes(test.toFile()), StandardCharsets.UTF_8);

        if (test.isModule()) {
            loader.parseModule(source, sourceCode);
        } else {
            loader.parseScript(source, sourceCode);
        }
    }

    private static final class Test262ParserInfo extends TestInfo {
        public Test262ParserInfo(Path basedir, Path file) {
            super(basedir, file);
        }

        public boolean isNegative() {
            return !getScript().getParent().getFileName().toString().startsWith("pass");
        }

        @Override
        public boolean isModule() {
            return getScript().getFileName().toString().endsWith(".module.js");
        }
    }
}
