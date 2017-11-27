/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.v8;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;

import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.types.ScriptObject;
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

/**
 *
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "v8.test.mjsunit", file = "resource:/test-configuration.properties")
public final class MiniJSUnitTest {
    private static final Configuration configuration = loadConfiguration(MiniJSUnitTest.class);
    private static final Set<String> ignoreFlags = Resources.set(configuration, "flags.ignore", Collections.emptySet());
    private static final Set<String> disableFlags = Resources.set(configuration, "flags.disable",
            Collections.emptySet());
    private static final boolean warnUnknownFlag = configuration.getBoolean("flags.warn", false);

    @Parameters(name = "{0}")
    public static List<V8TestInfo> suiteValues() throws IOException {
        return loadTests(configuration, V8TestInfo::new, MiniJSUnitTest::configureTest);
    }

    @BeforeClass
    public static void setUpClass() {
        V8TestRealmData.testLoadInitializationScript();
    }

    @ClassRule
    public static TestRealms<TestInfo> realms = new TestRealms<TestInfo>(configuration, V8TestRealmData::new) {
        @Override
        protected EnumSet<Parser.Option> getParserOptions() {
            return EnumSet.of(Parser.Option.NativeCall);
        }

        @Override
        protected BiFunction<String, MethodType, MethodHandle> getNativeCallResolver() {
            return NativeFunctions::resolve;
        }

        @Override
        protected BiFunction<RuntimeContext, ScriptLoader, TestModuleLoader<?>> getModuleLoader() {
            return V8FileModuleLoader::new;
        }

        @Override
        protected BiConsumer<ScriptObject, ModuleRecord> getImportMeta() {
            return (meta, module) -> {
                CreateDataProperty(module.getRealm().defaultContext(), meta, "url",
                        module.getSourceCodeId().toUri().toString());
            };
        }
    };

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public V8TestInfo test;

    private static final class V8TestInfo extends TestInfo {
        private boolean module = false;

        public V8TestInfo(Path basedir, Path file) {
            super(basedir, file);
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
    }

    @Test
    public void runTest() throws Throwable {
        realm.execute(test);
    }

    private static final Pattern FlagsPattern = Pattern.compile("\\s*(?://\\s*)+Flags:\\s*(.*)\\s*");

    private static void configureTest(V8TestInfo test, Stream<String> lines) {
        lines.forEach(line -> {
            Matcher m = FlagsPattern.matcher(line);
            if (m.matches()) {
                String[] flags = m.group(1).split("\\s+");
                for (int i = 0, len = flags.length; i < len; ++i) {
                    if (!flags[i].startsWith("--")) {
                        continue;
                    }
                    String flag = flags[i].replace('_', '-');
                    int sep = flag.indexOf('=');
                    String name;
                    if (sep != -1) {
                        name = flag.substring(0, sep).trim();
                    } else {
                        name = flag;
                    }
                    if (ignoreFlags.contains(name)) {
                        // Ignored flags
                    } else if (disableFlags.contains(name)) {
                        test.setEnabled(false);
                    } else if ("--allow-natives-syntax".equals(name)) {
                        if (!checkNativeSyntax(test.toFile())) {
                            // don't run tests with native syntax
                            test.setEnabled(false);
                        }
                    } else if (warnUnknownFlag) {
                        System.err.printf("unknown option '%s' in line: %s [%s]%n", flag, line, test.getScript());
                    }
                }
            } else if (line.equals("// MODULE")) {
                test.module = true;
            }
        });
    }

    private static boolean checkNativeSyntax(Path p) {
        try {
            String content = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
            if (content.indexOf('%') == -1) {
                return true;
            }
            Pattern pattern = Pattern.compile("%([A-Za-z_]+)\\(");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String name = matcher.group(1);
                if (!NativeFunctions.isSupported(name)) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
