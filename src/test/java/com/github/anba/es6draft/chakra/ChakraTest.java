/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.chakra;

import static com.github.anba.es6draft.util.Resources.loadConfiguration;
import static com.github.anba.es6draft.util.Resources.loadTests;
import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Parameterized.UseParametersRunnerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.util.NullConsole;
import com.github.anba.es6draft.util.Parallelized;
import com.github.anba.es6draft.util.ParameterizedRunnerFactory;
import com.github.anba.es6draft.util.Resources;
import com.github.anba.es6draft.util.TestConfiguration;
import com.github.anba.es6draft.util.TestInfo;
import com.github.anba.es6draft.util.TestRealm;
import com.github.anba.es6draft.util.TestRealms;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.ScriptExceptionHandler;
import com.github.anba.es6draft.util.rules.ExceptionHandlers.StandardErrorHandler;

/**
 * Test suite for the ChakraCore tests.
 */
@RunWith(Parallelized.class)
@UseParametersRunnerFactory(ParameterizedRunnerFactory.class)
@TestConfiguration(name = "chakra.test", file = "resource:/test-configuration.properties")
public final class ChakraTest {
    private static final Configuration configuration = loadConfiguration(ChakraTest.class);
    private static final Set<String> ignoreFlags = Resources.set(configuration, "flags.ignore", Collections.emptySet());
    private static final Set<String> disableFlags = Resources.set(configuration, "flags.disable",
            Collections.emptySet());
    private static final boolean warnUnknownFlag = configuration.getBoolean("flags.warn", false);

    @Parameters(name = "{0}")
    public static List<ChakraTestInfo> suiteValues() throws IOException {
        return loadTests(configuration, createTestFunction());
    }

    @BeforeClass
    public static void setUpClass() {
        ChakraTestRealmData.testLoadInitializationScript();
    }

    @ClassRule
    public static TestRealms<TestInfo> realms = new TestRealms<>(configuration, ChakraTestRealmData::new);

    @Rule
    public Timeout maxTime = new Timeout(120, TimeUnit.SECONDS);

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Rule
    public StandardErrorHandler errorHandler = new StandardErrorHandler();

    @Rule
    public ScriptExceptionHandler exceptionHandler = new ScriptExceptionHandler();

    @Parameter(0)
    public ChakraTestInfo test;

    private static final class ChakraTestInfo extends TestInfo {
        String baseline;
        Iterator<String> expected = Collections.emptyIterator();

        public ChakraTestInfo(Path basedir, Path file) {
            super(basedir, file);
        }
    }

    @Rule
    public TestRealm<TestInfo> realm = new TestRealm<>(realms);

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        realm.initialize(new NullConsole(), test);
        exceptionHandler.setExecutionContext(realm.get().defaultContext());

        if (test.baseline != null && !test.baseline.isEmpty()) {
            realm.get().createGlobalProperties(new MessageProducer(), MessageProducer.class);
            test.expected = lineIterator(test.toFile().resolveSibling(test.baseline));
        }
    }

    @Test
    public void runTest() throws Throwable {
        realm.execute(test);
    }

    public final class MessageProducer {
        @Function(name = "$MESSAGE", arity = 0)
        public String message() {
            if (test.expected.hasNext()) {
                return test.expected.next();
            }
            return null;
        }
    }

    private static final class TestSetting {
        String baseline;
        boolean disabled;
    }

    private static BiFunction<Path, Path, ChakraTestInfo> createTestFunction() {
        HashMap<Path, Map<String, TestSetting>> settingsMapping = new HashMap<>();

        return (basedir, file) -> {
            ChakraTestInfo testInfo = new ChakraTestInfo(basedir, file);
            Path dir = basedir.resolve(file).getParent();
            Map<String, TestSetting> map = settingsMapping.computeIfAbsent(dir, ChakraTest::readSettings);
            TestSetting setting = map.get(file.getFileName().toString());
            if (setting != null) {
                testInfo.baseline = setting.baseline;
                testInfo.setEnabled(!setting.disabled);
            }
            return testInfo;
        };
    }

    private static Map<String, TestSetting> readSettings(Path dir) {
        Path settingsFile = dir.resolve("rlexe.xml");
        if (!Files.isRegularFile(settingsFile)) {
            return Collections.emptyMap();
        }
        try (Reader reader = bomReader(Files.newInputStream(settingsFile))) {
            Document doc = Resources.xml(reader);
            NodeList elements = doc.getElementsByTagName("default");
            HashMap<String, TestSetting> settingMap = new HashMap<>();
            for (int i = 0, length = elements.getLength(); i < length; ++i) {
                Element element = (Element) elements.item(i);
                String files = element.getElementsByTagName("files").item(0).getTextContent();
                TestSetting setting = new TestSetting();
                NodeList baseline = element.getElementsByTagName("baseline");
                if (baseline.getLength() > 0) {
                    setting.baseline = baseline.item(0).getTextContent();
                }
                NodeList compileFlags = element.getElementsByTagName("compile-flags");
                if (compileFlags.getLength() > 0) {
                    String flags = compileFlags.item(0).getTextContent();
                    for (String flag : flags.split("\\s+")) {
                        if (!flag.startsWith("-")) {
                            continue;
                        }
                        int sep = flag.indexOf(':');
                        String name;
                        if (sep != -1) {
                            name = flag.substring(0, sep).trim();
                        } else {
                            name = flag;
                        }
                        if (ignoreFlags.contains(name)) {
                            // Ignored flags
                        } else if (disableFlags.contains(name)) {
                            setting.disabled = true;
                        } else if (warnUnknownFlag) {
                            System.err.printf("unknown option '%s': %s%n", flag, settingsFile);
                        }
                    }
                }
                settingMap.putIfAbsent(files, setting);
            }
            return settingMap;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Iterator<String> lineIterator(Path p) throws IOException {
        try (BufferedReader reader = bomReader(Files.newInputStream(p))) {
            return reader.lines().collect(Collectors.toCollection(ArrayList::new)).iterator();
        }
    }

    private static BufferedReader bomReader(InputStream is) throws IOException {
        BOMInputStream bis = new BOMInputStream(is);
        return new BufferedReader(new InputStreamReader(bis, charsetFor(bis)));
    }

    private static Charset charsetFor(BOMInputStream bis) throws IOException {
        ByteOrderMark bom = bis.getBOM();
        if (ByteOrderMark.UTF_8.equals(bom)) {
            return StandardCharsets.UTF_8;
        }
        if (ByteOrderMark.UTF_16LE.equals(bom)) {
            return StandardCharsets.UTF_16LE;
        }
        if (ByteOrderMark.UTF_16BE.equals(bom)) {
            return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8;
    }
}
