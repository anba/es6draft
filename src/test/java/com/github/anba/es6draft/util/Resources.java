/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static com.github.anba.es6draft.util.Functional.intoCollection;
import static com.github.anba.es6draft.util.Functional.toStrings;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
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
    private static final boolean REMOVE_DISABLED_TESTS = false;
    private static final boolean DISABLE_ALL_TESTS = false;
    private static final boolean RUN_DISABLED_TESTS = false;

    private Resources() {
    }

    /**
     * {@link ConfigurationInterpolator} which reports an error for missing variables.
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
     * Loads the configuration file.
     */
    public static Configuration loadConfiguration(Class<?> clazz) {
        TestConfiguration config = clazz.getAnnotation(TestConfiguration.class);
        String file = config.file();
        String name = config.name();
        try {
            PropertiesConfiguration properties = new PropertiesConfiguration();
            // entries are mandatory unless an explicit default value was given
            properties.setThrowExceptionOnMissing(true);
            properties.getInterpolator().setParentInterpolator(MISSING_VAR);
            properties.load(resource(file), "UTF-8");

            Configuration configuration = new CompositeConfiguration(Arrays.asList(
                    new SystemConfiguration(), properties));
            return configuration.subset(name);
        } catch (ConfigurationException | IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            throw e;
        }
    }

    /**
     * Loads the named resource through {@link Class#getResourceAsStream(String)} if the uri starts
     * with "resource:", otherwise loads the resource with
     * {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}.
     */
    public static InputStream resource(String uri) throws IOException {
        return resource(uri, Paths.get(""));
    }

    /**
     * Loads the named resource through {@link Class#getResourceAsStream(String)} if the uri starts
     * with "resource:", otherwise loads the resource with
     * {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}.
     */
    public static InputStream resource(String uri, Path basedir) throws IOException {
        final String RESOURCE = "resource:";
        if (uri.startsWith(RESOURCE)) {
            String name = uri.substring(RESOURCE.length());
            InputStream res = Resources.class.getResourceAsStream(name);
            if (res == null) {
                throw new IOException("resource not found: " + name);
            }
            return res;
        } else {
            return Files.newInputStream(basedir.resolve(Paths.get(uri)));
        }
    }

    /**
     * Returns the resource path if available.
     */
    public static Path resourcePath(String uri, Path basedir) {
        final String RESOURCE = "resource:";
        if (uri.startsWith(RESOURCE)) {
            return null;
        } else {
            return basedir.resolve(Paths.get(uri)).toAbsolutePath();
        }
    }

    /**
     * Returns {@code true} if the test suite is enabled.
     */
    public static boolean isEnabled(Configuration configuration) {
        return !configuration.getBoolean("skip", false);
    }

    /**
     * Returns the test suite's base path.
     */
    public static Path getTestSuitePath(Configuration configuration) {
        try {
            String testSuite = configuration.getString("");
            return Paths.get(testSuite).toAbsolutePath();
        } catch (InvalidPathException | NoSuchElementException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Load the test files based on the supplied {@link Configuration}.
     */
    public static List<TestInfo> loadTests(Configuration config) throws IOException {
        return loadTests(config, defaultCreate);
    }

    /**
     * Load the test files based on the supplied {@link Configuration}.
     */
    public static <TEST extends TestInfo> List<TEST> loadTests(Configuration config,
            BiFunction<Path, Path, TEST> fn) throws IOException {
        if (!isEnabled(config)) {
            return emptyList();
        }
        Path basedir = getTestSuitePath(config);
        if (basedir == null) {
            return emptyList();
        }
        return loadTests(config, mapper(fn, basedir), basedir);
    }

    /**
     * Load the test files based on the supplied {@link Configuration}.
     */
    public static <TEST extends TestInfo> List<TEST> loadTests(Configuration config,
            Function<Path, BiFunction<Path, Iterator<String>, TEST>> fn) throws IOException {
        if (!isEnabled(config)) {
            return emptyList();
        }
        Path basedir = getTestSuitePath(config);
        if (basedir == null) {
            return emptyList();
        }
        return loadTests(config, mapper(fn.apply(basedir)), basedir);
    }

    /**
     * Recursively searches for js-file test cases in {@code basedir} and its sub-directories.
     */
    private static <TEST extends TestInfo> List<TEST> loadTests(Configuration config,
            Function<Path, TEST> mapper, Path basedir) throws IOException {
        FilterFileVisitor<Path> ffv = new FilterFileVisitor<Path>(basedir, new FileMatcher(config));
        CollectorFileVisitor<Path, TEST> cfv = new CollectorFileVisitor<>(ffv, mapper);
        Files.walkFileTree(basedir, cfv);
        List<TEST> tests = cfv.getResult();
        filterTests(tests, basedir, config);
        if (REMOVE_DISABLED_TESTS) {
            tests = removeDisabled(tests);
        }
        return tests;
    }

    private static <TEST extends TestInfo> List<TEST> removeDisabled(List<TEST> tests) {
        ArrayList<TEST> actual = new ArrayList<>();
        for (TEST test : tests) {
            if (test.isEnabled()) {
                actual.add(test);
            }
        }
        return actual;
    }

    /**
     * Filter the initially collected test cases.
     */
    private static void filterTests(List<? extends TestInfo> tests, Path basedir,
            Configuration config) throws IOException {
        if (DISABLE_ALL_TESTS) {
            for (TestInfo test : tests) {
                test.setEnabled(false);
            }
        }
        if (config.containsKey("exclude.list")) {
            InputStream exclusionList = Resources.resource(config.getString("exclude.list"),
                    basedir);
            filterTests(tests, exclusionList, config);
        }
        if (config.containsKey("exclude.xml")) {
            Set<String> excludes = readExcludeXMLs(config.getList("exclude.xml", emptyList()),
                    basedir);
            filterTests(tests, excludes);
        }
    }

    /**
     * Filter the initially collected test cases.
     */
    private static void filterTests(List<? extends TestInfo> tests, InputStream resource,
            Configuration config) throws IOException {
        // list->map
        Map<Path, TestInfo> map = new LinkedHashMap<>();
        for (TestInfo test : tests) {
            map.put(test.getScript(), test);
        }
        // disable tests
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource,
                StandardCharsets.UTF_8))) {
            FileMatcher fileMatcher = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                TestInfo test = map.get(Paths.get(line));
                if (test == null) {
                    if (fileMatcher == null) {
                        fileMatcher = new FileMatcher(config);
                    }
                    if (matchesIncludesOrInvalidEntry(fileMatcher, line)) {
                        System.err.printf("detected stale entry '%s'\n", line);
                    }
                    continue;
                }
                test.setEnabled(RUN_DISABLED_TESTS);
            }
        }
    }

    private static boolean matchesIncludesOrInvalidEntry(FileMatcher fileMatcher, String entry) {
        Path file;
        try {
            file = Paths.get(entry);
        } catch (InvalidPathException e) {
            return true;
        }
        return fileMatcher.matches(file);
    }

    /**
     * Filter the initially collected test cases.
     */
    private static void filterTests(List<? extends TestInfo> tests, Set<String> excludes) {
        Pattern pattern = Pattern.compile("(.+?)(?:\\.([^.]*)$|$)");
        for (TestInfo test : tests) {
            String filename = test.getScript().getFileName().toString();
            Matcher matcher = pattern.matcher(filename);
            if (!matcher.matches()) {
                assert false : "regexp failure";
                continue;
            }
            String testname = matcher.group(1);
            if (excludes.contains(testname)) {
                test.setEnabled(RUN_DISABLED_TESTS);
                continue;
            }
        }
    }

    /**
     * Reads all exlusion xml-files from the configuration.
     */
    private static Set<String> readExcludeXMLs(List<?> values, Path basedir) throws IOException {
        Set<String> exclude = new HashSet<>();
        for (String s : Functional.toStrings(values)) {
            try (InputStream res = Resources.resource(s, basedir)) {
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
     * Reads the xml-structure from {@link Reader} and returns the corresponding {@link Document}.
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

    private static final BiFunction<Path, Path, TestInfo> defaultCreate = new BiFunction<Path, Path, TestInfo>() {
        @Override
        public TestInfo apply(Path basedir, Path file) {
            return new TestInfo(basedir, file);
        }
    };

    private static <T extends TestInfo> Function<Path, T> mapper(
            final BiFunction<Path, Path, T> fn, final Path basedir) {
        return new Function<Path, T>() {
            @Override
            public T apply(Path file) {
                return fn.apply(basedir, file);
            }
        };
    }

    private static <T extends TestInfo> Function<Path, T> mapper(
            final BiFunction<Path, Iterator<String>, T> fn) {
        return new Function<Path, T>() {
            @Override
            public T apply(Path file) {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    return fn.apply(file, new LineIterator(reader));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    private static final class LineIterator implements Iterator<String> {
        private final BufferedReader reader;
        private String line = null;

        LineIterator(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public boolean hasNext() {
            if (line == null) {
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return line != null;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            String line = this.line;
            this.line = null;
            return line;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class CollectorFileVisitor<PATH extends Path, T> extends
            SimpleFileVisitor<PATH> {
        private final FileVisitor<PATH> visitor;
        private Function<PATH, T> mapper;
        private ArrayList<T> result = new ArrayList<>();

        CollectorFileVisitor(FileVisitor<PATH> visitor, Function<PATH, T> mapper) {
            this.visitor = visitor;
            this.mapper = mapper;
        }

        public ArrayList<T> getResult() {
            return result;
        }

        @Override
        public FileVisitResult preVisitDirectory(PATH dir, BasicFileAttributes attrs)
                throws IOException {
            return visitor.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(PATH file, BasicFileAttributes attrs) throws IOException {
            if (visitor.visitFile(file, attrs) == FileVisitResult.CONTINUE) {
                try {
                    result.add(mapper.apply(file));
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
            }
            return FileVisitResult.CONTINUE;
        }
    }

    private static final class FilterFileVisitor<PATH extends Path> extends SimpleFileVisitor<PATH> {
        private final Path basedir;
        private final FileMatcher fileMatcher;

        FilterFileVisitor(Path basedir, FileMatcher fileMatcher) {
            this.basedir = basedir;
            this.fileMatcher = fileMatcher;
        }

        @Override
        public FileVisitResult preVisitDirectory(PATH path, BasicFileAttributes attrs)
                throws IOException {
            Path dir = basedir.relativize(path);
            if (fileMatcher.matchesDirectory(dir)) {
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(PATH path, BasicFileAttributes attrs) throws IOException {
            Path file = basedir.relativize(path);
            if (attrs.isRegularFile() && attrs.size() != 0L && fileMatcher.matches(file)) {
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.TERMINATE;
        }
    }

    private static final class FileMatcher {
        private final List<PathMatcher> includeMatchers;
        private final Set<String> includeDirs;
        private final Set<String> includeFiles;
        private final List<PathMatcher> excludeMatchers;
        private final Set<String> excludeDirs;
        private final Set<String> excludeFiles;
        private final List<String> includePrefixList;
        private final List<String> excludePrefixList;

        FileMatcher(Configuration config) {
            List<Object> include = config.getList("include", asList("**/*.js", "*.js"));
            List<Object> includeDirs = config.getList("include.dirs", emptyList());
            List<Object> includeFiles = config.getList("include.files", emptyList());
            List<Object> exclude = config.getList("exclude", emptyList());
            List<Object> excludeDirs = config.getList("exclude.dirs", emptyList());
            List<Object> excludeFiles = config.getList("exclude.files", emptyList());

            this.includeMatchers = matchers(toStrings(include));
            this.includeDirs = intoCollection(toStrings(includeDirs), new HashSet<String>());
            this.includeFiles = intoCollection(toStrings(includeFiles), new HashSet<String>());
            this.excludeMatchers = matchers(toStrings(exclude));
            this.excludeDirs = intoCollection(toStrings(excludeDirs), new HashSet<String>());
            this.excludeFiles = intoCollection(toStrings(excludeFiles), new HashSet<String>());
            this.includePrefixList = toPrefixList(toStrings(include));
            this.excludePrefixList = toPrefixList(toStrings(exclude));
        }

        private static List<String> toPrefixList(Iterable<String> patterns) {
            Pattern dirPattern = Pattern.compile("^(?:glob:)?((?:[^/*?,{}\\[\\]\\\\]+/)+).*$");
            List<String> prefixList = new ArrayList<>();
            for (String pattern : patterns) {
                Matcher m = dirPattern.matcher(pattern);
                if (!m.matches()) {
                    return Collections.emptyList();
                }
                prefixList.add(m.group(1));
            }
            return prefixList;
        }

        public boolean matchesDirectory(Path directory) {
            if (!matches(excludeDirs, directory.getFileName())
                    && !prefixMatches(excludePrefixList, directory.toString())
                    && matches(includePrefixList, directory.toString())) {
                return true;
            }
            return false;
        }

        public boolean matches(Path file) {
            if (!matches(excludeMatchers, file) && matches(includeMatchers, file)) {
                if (!matches(excludeFiles, file.getFileName())
                        && (includeDirs.isEmpty() || matches(includeDirs, file.getParent()))
                        && (includeFiles.isEmpty() || matches(includeFiles, file.getFileName()))) {
                    return true;
                }
            }
            return false;
        }

        private static List<PathMatcher> matchers(Iterable<String> patterns) {
            List<PathMatcher> matchers = new ArrayList<>();
            for (String pattern : patterns) {
                if (!(pattern.startsWith("glob:") || pattern.startsWith("regex:"))) {
                    pattern = "glob:" + pattern;
                }
                matchers.add(FileSystems.getDefault().getPathMatcher(pattern));
            }
            return matchers;
        }

        private static boolean matches(Set<String> names, Path path) {
            for (Path p : path) {
                if (names.contains(p.getFileName().toString())) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matches(List<PathMatcher> matchers, Path path) {
            for (PathMatcher matcher : matchers) {
                if (matcher.matches(path)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matches(List<String> list, String string) {
            if (string.isEmpty() || list.isEmpty()) {
                return true;
            }
            String search = string.replace(File.separatorChar, '/') + "/";
            for (String item : list) {
                if (item.regionMatches(0, search, 0, Math.min(item.length(), search.length()))) {
                    return true;
                }
            }
            return false;
        }

        private static boolean prefixMatches(List<String> prefixList, String string) {
            String search = string.replace(File.separatorChar, '/') + "/";
            for (String prefix : prefixList) {
                if (search.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
