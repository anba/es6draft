/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import static com.github.anba.es6draft.runtime.AbstractOperations.CreateDataProperty;
import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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

import com.github.anba.es6draft.runtime.internal.RuntimeContext;
import com.github.anba.es6draft.runtime.internal.ScriptLoader;
import com.github.anba.es6draft.runtime.modules.ModuleRecord;
import com.github.anba.es6draft.runtime.types.ScriptObject;
import com.github.anba.es6draft.util.NullConsole;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
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
@TestConfiguration(name = "webkit.modules.test", file = "resource:/test-configuration.properties")
public final class ModulesTest {
    private static final Configuration configuration = loadConfiguration(ModulesTest.class);

    @Parameters(name = "{0}")
    public static List<ModuleTestInfo> suiteValues() throws IOException {
        return loadTests(configuration, ModuleTestInfo::new);
    }

    @BeforeClass
    public static void setUpClass() {
        WebKitTestRealmData.testLoadInitializationScript();
    }

    @ClassRule
    public static TestRealms<TestInfo> realms = new TestRealms<TestInfo>(configuration, WebKitTestRealmData::new) {
        @Override
        protected BiFunction<RuntimeContext, ScriptLoader, TestModuleLoader<?>> getModuleLoader() {
            return WebKitFileModuleLoader::new;
        }

        @Override
        protected BiConsumer<ScriptObject, ModuleRecord> getImportMeta() {
            return (meta, module) -> {
                CreateDataProperty(module.getRealm().defaultContext(), meta, "filename",
                        fileName(module.getSourceCodeId().toUri()));
            };
        }

        private String fileName(URI uri) {
            String path = uri.getPath();
            int n = path.lastIndexOf('/');
            return n >= 0 ? path.substring(n + 1) : "";
        }
    };

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public ModuleTestInfo test;

    private static class ModuleTestInfo extends TestInfo {
        ModuleTestInfo(Path basedir, Path file) {
            super(basedir, file);
        }

        @Override
        public boolean isModule() {
            return true;
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
}
