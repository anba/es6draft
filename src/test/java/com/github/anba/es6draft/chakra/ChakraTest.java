/**
 * Copyright (c) 2012-2016 André Bargull
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
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.junit.After;
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
import com.github.anba.es6draft.util.TestGlobals;
import com.github.anba.es6draft.util.TestInfo;
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

    @Parameters(name = "{0}")
    public static List<ChakraTestInfo> suiteValues() throws IOException {
        return loadTests(configuration, createTestFunction());
    }

    @BeforeClass
    public static void setUpClass() throws IOException {
        ChakraTestGlobalObject.testLoadInitializationScript();
    }

    @ClassRule
    public static TestGlobals<ChakraTestGlobalObject, TestInfo> globals = new TestGlobals<>(configuration,
            ChakraTestGlobalObject::new);

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

    private ChakraTestGlobalObject global;

    @Before
    public void setUp() throws Throwable {
        assumeTrue("Test disabled", test.isEnabled());

        global = globals.newGlobal(new NullConsole(), test);
        exceptionHandler.setExecutionContext(global.getRealm().defaultContext());

        if (test.baseline != null && !test.baseline.isEmpty()) {
            global.createGlobalProperties(new MessageProducer(), MessageProducer.class);
            test.expected = lineIterator(test.toFile().resolveSibling(test.baseline));
        }
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
        if (Files.isRegularFile(settingsFile)) {
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
                        setting.disabled = flags.contains("-verbose") || flags.contains("-dump:")
                                || flags.contains("-trace:") || flags.contains("-testtrace:")
                                || flags.contains("-testTrace:");
                    }
                    settingMap.putIfAbsent(files, setting);
                }
                return settingMap;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return Collections.emptyMap();
    }

    private static Iterator<String> lineIterator(Path p) throws IOException {
        try (BufferedReader reader = bomReader(Files.newInputStream(p))) {
            ArrayList<String> list = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null)
                    break;
                list.add(line);
            }
            return list.iterator();
        }
    }

    private static BufferedReader bomReader(InputStream is) throws IOException {
        BOMInputStream bis = new BOMInputStream(is);
        Charset cs = charsetFor(bis, StandardCharsets.UTF_8);
        return new BufferedReader(new InputStreamReader(bis, cs));
    }

    private static Charset charsetFor(BOMInputStream bis, Charset defaultCharset) throws IOException {
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
        return defaultCharset;
    }
}
