/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static com.github.anba.es6draft.util.Functional.intoCollection;
import static com.github.anba.es6draft.util.Functional.toStrings;
import static com.github.anba.es6draft.util.TestInfo.toObjectArray;
import static java.util.Collections.emptyList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.text.StrLookup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.anba.es6draft.util.Functional.BiFunction;
import com.github.anba.es6draft.util.Functional.Function;

/**
 * Resource and configuration loading utility
 */
public final class Resources {
    private Resources() {
    }

    /**
     * {@link ConfigurationInterpolator} which reports an error for missing variables
     */
    private static final ConfigurationInterpolator MISSING_VAR = new ConfigurationInterpolator() {
        private final StrLookup errorLookup = new StrLookup() {
            @Override
            public String lookup(String key) {
                String msg = String.format("Variable '%s' is not set", key);
                throw new NoSuchElementException(msg);
            }
        };

        @Override
        public String lookup(String var) {
            return errorLookup.lookup(var);
        }

        @Override
        protected StrLookup fetchLookupForPrefix(String prefix) {
            return errorLookup;
        }

        @Override
        protected StrLookup fetchNoPrefixLookup() {
            return errorLookup;
        }
    };

    /**
     * Loads the configuration file
     */
    public static Configuration loadConfiguration(Class<?> clazz) {
        TestConfiguration config = clazz.getAnnotation(TestConfiguration.class);
        String file = config.file();
        String name = config.name();
        try {
            PropertiesConfiguration configuration = new PropertiesConfiguration();
            // entries are mandatory unless an explicit default value was given
            configuration.setThrowExceptionOnMissing(true);
            configuration.getInterpolator().setParentInterpolator(MISSING_VAR);
            configuration.load(resource(file), "UTF-8");
            return configuration.subset(name);
        } catch (ConfigurationException | IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            throw e;
        }
    }

    /**
     * Loads the named resource through {@link Class#getResourceAsStream(String)} if the uri is
     * prepended with "resource:", otherwise loads the resource with
     * {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}
     */
    public static InputStream resource(String uri) throws IOException {
        final String RESOURCE = "resource:";
        if (uri.startsWith(RESOURCE)) {
            String name = "/" + uri.substring(RESOURCE.length());
            InputStream res = Resources.class.getResourceAsStream(name);
            if (res == null) {
                throw new IOException("resource not found: " + name);
            }
            return res;
        } else {
            return Files.newInputStream(Paths.get(uri));
        }
    }

    /**
     * Returns the test suite's base path
     */
    private static Path getTestSuitePath(Configuration configuration) {
        String testSuite;
        try {
            testSuite = configuration.getString("");
        } catch (NoSuchElementException e) {
            System.err.println(e.getMessage());
            return null;
        }
        return Paths.get(testSuite);
    }

    /**
     * Load the test files based on the supplied {@link Configuration}
     */
    public static Iterable<TestInfo[]> loadTests(Configuration config) throws IOException {
        return loadTests(config, null);
    }

    /**
     * Load the test files based on the supplied {@link Configuration}
     */
    public static Iterable<TestInfo[]> loadTests(Configuration config,
            Function<Path, BiFunction<Path, Iterator<String>, TestInfo>> fn) throws IOException {
        Path basedir = getTestSuitePath(config);
        if (basedir == null) {
            return emptyList();
        }
        Path searchdir = basedir.resolve(config.getString("searchdir", ""));
        Iterable<Object> excludeDirs = config.getList("exclude.dirs", emptyList());
        Iterable<Object> excludeFiles = config.getList("exclude.files", emptyList());
        Set<String> excludeDirSet = intoCollection(toStrings(excludeDirs), new HashSet<String>());
        Set<String> excludeFilesSet = intoCollection(toStrings(excludeFiles), new HashSet<String>());

        List<TestInfo> tests;
        if (fn == null) {
            tests = TestInfo.loadTests(searchdir, basedir, excludeDirSet, excludeFilesSet);
        } else {
            tests = TestInfo
                    .loadTests(searchdir, excludeDirSet, excludeFilesSet, fn.apply(basedir));
        }

        if (config.containsKey("exclude.list")) {
            InputStream exclusionList = Resources.resource(config.getString("exclude.list"));
            tests = filterTests(tests, exclusionList);
        }

        return toObjectArray(tests);
    }

    /**
     * Load the test files based on the supplied {@link Configuration}
     */
    public static Iterable<TestInfo[]> loadXMLTests(Configuration config,
            BiFunction<Path, Path, TestInfo> fn) throws IOException {
        Path basedir = getTestSuitePath(config);
        if (basedir == null) {
            return emptyList();
        }
        Set<String> excludeDirs = Collections.emptySet();
        Set<String> excludeFiles = Collections.emptySet();
        List<TestInfo> tests = TestInfo.loadTests(basedir, basedir, excludeDirs, excludeFiles, fn);

        Pattern excludePattern = Pattern.compile(config.getString("exclude_re", ""));
        Set<String> excludes = readExcludeXMLs(config.getList("exclude", emptyList()));
        Set<String> includes = readExcludeXMLs(config.getList("include", emptyList()));
        boolean onlyExcluded = config.getBoolean("only_excluded", false);

        return toObjectArray(filterTests(tests, excludePattern, excludes, includes, onlyExcluded));
    }

    /**
     * Filter the initially collected test cases
     */
    private static <T extends TestInfo> List<T> filterTests(List<T> tests, InputStream resource)
            throws IOException {
        // list->map
        Map<Path, TestInfo> map = new LinkedHashMap<>();
        for (TestInfo test : tests) {
            map.put(test.script, test);
        }
        // disable tests
        List<TestInfo> disabledTests = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                TestInfo t = map.get(Paths.get(line));
                if (t == null) {
                    System.err.printf("detected stale entry '%s'\n", line);
                    continue;
                }
                disabledTests.add(t);
                t.enable = false;
            }
        }
        System.out.printf("disabled %d tests of %d in total%n", disabledTests.size(), tests.size());
        return tests;
    }

    /**
     * Filter the initially collected test cases
     */
    private static <T extends TestInfo> List<T> filterTests(List<T> tests, Pattern excludePattern,
            Set<String> excludes, Set<String> includes, boolean onlyExcluded) {
        Pattern pattern = Pattern.compile("(.+?)(?:\\.([^.]*)$|$)");
        for (TestInfo test : tests) {
            String rel = test.script.toString();
            if (excludePattern.matcher(rel).matches()) {
                test.enable = false;
                continue;
            }
            String filename = test.script.getFileName().toString();
            Matcher matcher = pattern.matcher(filename);
            if (!matcher.matches()) {
                assert false : "regexp failure";
            }
            String testname = matcher.group(1);
            if (excludes.contains(testname) ^ onlyExcluded) {
                test.enable = false;
                continue;
            }
            if (!includes.isEmpty() && !includes.contains(testname)) {
                test.enable = false;
                continue;
            }
        }
        return tests;
    }

    /**
     * Reads all exlusion xml-files from the configuration
     */
    private static Set<String> readExcludeXMLs(List<?> values) throws IOException {
        Set<String> exclude = new HashSet<>();
        for (String s : Functional.toStrings(values)) {
            try (InputStream res = Resources.resource(s)) {
                exclude.addAll(readExcludeXML(res));
            }
        }
        return exclude;
    }

    /**
     * Load the exclusion xml-list for invalid test cases from {@link InputStream}
     */
    private static Set<String> readExcludeXML(InputStream is) throws IOException {
        Set<String> exclude = new HashSet<>();
        Reader reader = new InputStreamReader(new BOMInputStream(is), StandardCharsets.UTF_8);
        NodeList ns = xml(reader).getDocumentElement().getElementsByTagName("test");
        for (int i = 0, len = ns.getLength(); i < len; ++i) {
            exclude.add(((Element) ns.item(i)).getAttribute("id"));
        }
        return exclude;
    }

    /**
     * Reads the xml-structure from {@link Reader} and returns the corresponding {@link Document}
     */
    private static Document xml(Reader xml) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // turn off any validation or namespace features
        factory.setNamespaceAware(false);
        factory.setValidating(false);
        List<String> features = Arrays.asList("http://xml.org/sax/features/namespaces",
                "http://xml.org/sax/features/validation",
                "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                "http://apache.org/xml/features/nonvalidating/load-external-dtd");
        for (String feature : features) {
            try {
                factory.setFeature(feature, false);
            } catch (ParserConfigurationException e) {
                // ignore invalid feature names
            }
        }

        try {
            return factory.newDocumentBuilder().parse(new InputSource(xml));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }
}
