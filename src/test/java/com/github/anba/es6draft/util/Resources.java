/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.lang.text.StrLookup;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Resource and configuration loading utility
 */
public final class Resources {
    private static final String RESOURCE_PREFIX = "resource:";

    private Resources() {
    }

    /**
     * {@link ConfigurationInterpolator} which reports an error for missing variables.
     */
    private static final ConfigurationInterpolator MISSING_VAR = new ConfigurationInterpolator() {
        private final StrLookup errorLookup = new StrLookup() {
            @Override
            public String lookup(String key) {
                throw new NoSuchElementException(String.format("Variable '%s' is not set", key));
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
        return loadConfiguration(clazz.getAnnotation(TestConfiguration.class));
    }

    /**
     * Loads the configuration file.
     */
    public static Configuration loadConfiguration(TestConfiguration config) {
        String file = config.file();
        String name = config.name();
        try {
            PropertiesConfiguration properties = new PropertiesConfiguration();
            // entries are mandatory unless an explicit default value was given
            properties.setThrowExceptionOnMissing(true);
            properties.setDelimiterParsingDisabled(true);
            properties.getInterpolator().setParentInterpolator(MISSING_VAR);
            properties.load(resource(file, Paths.get("")), "UTF-8");

            SystemConfiguration systemConfiguration = new SystemConfiguration();
            systemConfiguration.setDelimiterParsingDisabled(true);

            return new CompositeConfiguration(Arrays.asList(systemConfiguration, properties)).subset(name);
        } catch (ConfigurationException | IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchElementException e) {
            throw e;
        }
    }

    /**
     * Loads the named resource through {@link Class#getResourceAsStream(String)} if the uri starts with "resource:",
     * otherwise loads the resource with {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}.
     */
    private static InputStream resource(String uri, Path basedir) throws IOException {
        if (uri.startsWith(RESOURCE_PREFIX)) {
            String name = uri.substring(RESOURCE_PREFIX.length());
            InputStream res = Resources.class.getResourceAsStream(name);
            if (res == null) {
                throw new IOException("resource not found: " + name);
            }
            return res;
        }
        return Files.newInputStream(basedir.resolve(Paths.get(uri)));
    }

    /**
     * Returns the script resource.
     */
    public static Map.Entry<Either<Path, URL>, InputStream> resourceScript(String uri, Path basedir)
            throws IOException {
        InputStream resourceStream = resource(uri, basedir); // throws IOException if resource not found
        if (uri.startsWith(RESOURCE_PREFIX)) {
            String name = uri.substring(RESOURCE_PREFIX.length());
            URL resourceURL = Resources.class.getResource(name);
            return new AbstractMap.SimpleImmutableEntry<>(Either.right(resourceURL), resourceStream);
        }
        Path resourcePath = basedir.resolve(Paths.get(uri)).toAbsolutePath();
        return new AbstractMap.SimpleImmutableEntry<>(Either.left(resourcePath), resourceStream);
    }

    /**
     * Returns the module resource or {@code null}.
     */
    public static Map.Entry<Path, String> resourceModule(String uri) throws IOException {
        if (uri.startsWith(RESOURCE_PREFIX)) {
            String moduleURI, moduleName;
            int equals = uri.indexOf('=');
            if (equals < 0) {
                moduleURI = uri;
                moduleName = uri.substring(RESOURCE_PREFIX.length());
            } else {
                moduleURI = uri.substring(0, equals);
                moduleName = uri.substring(equals + 1);
            }

            StringBuilder sb = new StringBuilder(4096);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(Resources.resource(moduleURI, null), StandardCharsets.UTF_8))) {
                char cbuf[] = new char[4096];
                for (int len; (len = reader.read(cbuf)) != -1;) {
                    sb.append(cbuf, 0, len);
                }
            }

            return new AbstractMap.SimpleImmutableEntry<>(Paths.get(moduleName), sb.toString());
        }
        return null;
    }

    /**
     * Reads a property set.
     * 
     * @param configuration
     *            the configuration
     * @param name
     *            the property name
     * @param defaultValue
     *            the default value
     * @return the property set
     */
    public static Set<String> set(Configuration configuration, String name, Set<String> defaultValue) {
        return toCollection(configuration, name, defaultValue, Collectors.toSet());
    }

    /**
     * Reads a property list.
     * 
     * @param configuration
     *            the configuration
     * @param name
     *            the property name
     * @param defaultValue
     *            the default value
     * @return the property list
     */
    public static List<String> list(Configuration configuration, String name, List<String> defaultValue) {
        return toCollection(configuration, name, defaultValue, Collectors.toList());
    }

    /**
     * Reads a property list.
     * 
     * @param configuration
     *            the configuration
     * @param name
     *            the property name
     * @param defaultValue
     *            the default value
     * @return the property stream
     */
    public static Stream<String> stream(Configuration configuration, String name, Stream<String> defaultValue) {
        return toStream(configuration, name, defaultValue);
    }

    private static <C extends Collection<String>> C toCollection(Configuration configuration, String name,
            C defaultValue, Collector<String, ?, C> collector) {
        return toStream(configuration, name).map(s -> s.collect(collector)).orElse(defaultValue);
    }

    private static Stream<String> toStream(Configuration configuration, String name, Stream<String> defaultValue) {
        return toStream(configuration, name).orElse(defaultValue);
    }

    private static Optional<Stream<String>> toStream(Configuration configuration, String name) {
        String s = configuration.getString(name, "");
        if (s.isEmpty()) {
            return Optional.empty();
        }
        Predicate<String> notEmpty = ((Predicate<String>) String::isEmpty).negate();
        return Optional.of(Arrays.stream(s.split(",")).map(String::trim).filter(notEmpty));
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
        return loadTests(config, TestInfo::new);
    }

    /**
     * Load the test files based on the supplied {@link Configuration}.
     */
    public static <TEST extends TestInfo> List<TEST> loadTests(Configuration config, BiFunction<Path, Path, TEST> fn)
            throws IOException {
        if (!isEnabled(config)) {
            return emptyList();
        }
        Path basedir = getTestSuitePath(config);
        if (basedir == null) {
            return emptyList();
        }
        return loadTests(config, partial(fn, basedir), basedir);
    }

    /**
     * Load the test files based on the supplied {@link Configuration}.
     */
    public static <TEST extends TestInfo> List<TEST> loadTests(Configuration config, BiFunction<Path, Path, TEST> fn,
            BiConsumer<TEST, Stream<String>> c) throws IOException {
        if (!isEnabled(config)) {
            return emptyList();
        }
        Path basedir = getTestSuitePath(config);
        if (basedir == null) {
            return emptyList();
        }
        return loadTests(config, partial(fn, basedir).andThen(t -> {
            try (Stream<String> lines = Files.lines(t.toFile(), StandardCharsets.UTF_8)) {
                c.accept(t, lines);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return t;
        }), basedir);
    }

    /**
     * Recursively searches for js-file test cases in {@code basedir} and its sub-directories.
     */
    private static <TEST extends TestInfo> List<TEST> loadTests(Configuration config, Function<Path, TEST> mapper,
            Path basedir) throws IOException {
        FilterFileVisitor<Path> ffv = new FilterFileVisitor<Path>(basedir, new FileMatcher(config));
        CollectorFileVisitor<Path, TEST> cfv = new CollectorFileVisitor<>(ffv, mapper);
        Files.walkFileTree(basedir, cfv);
        List<TEST> tests = cfv.getResult();
        filterTests(tests, basedir, config);
        if (config.getBoolean("exclude.remove", false)) {
            tests = tests.stream().filter(TEST::isEnabled).collect(Collectors.toCollection(ArrayList::new));
        }
        return tests;
    }

    /**
     * Filter the initially collected test cases.
     */
    private static void filterTests(List<? extends TestInfo> tests, Path basedir, Configuration config)
            throws IOException {
        if (config.getBoolean("exclude.all", false)) {
            for (TestInfo test : tests) {
                test.setEnabled(false);
            }
        }
        if (config.containsKey("include.list")) {
            Stream<InputStream> inclusionLists = stream(config, "include.list", Stream.empty()).map(uri -> {
                try {
                    return Resources.resource(uri, basedir);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            filterTests(tests, inclusionLists, config, true);
        }
        if (config.containsKey("exclude.list")) {
            Stream<InputStream> exclusionLists = stream(config, "exclude.list", Stream.empty()).map(uri -> {
                try {
                    return Resources.resource(uri, basedir);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            filterTests(tests, exclusionLists, config, false);
        }
    }

    /**
     * Filter the initially collected test cases.
     */
    private static void filterTests(List<? extends TestInfo> tests, Stream<InputStream> resources, Configuration config,
            boolean enableValue) throws IOException {
        // list->map
        Map<Path, TestInfo> map = new LinkedHashMap<>();
        for (TestInfo test : tests) {
            map.put(test.getScript(), test);
        }
        // disable tests
        for (Iterator<InputStream> iterator = resources.iterator(); iterator.hasNext();) {
            InputStream resource = iterator.next();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8))) {
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
                    test.setEnabled(enableValue);
                }
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
     * Reads the xml-structure from {@link Reader} and returns the corresponding {@link Document}.
     */
    public static Document xml(Reader xml) throws IOException {
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

    private static <T, U, R> Function<U, R> partial(BiFunction<T, U, R> fn, T argument) {
        return v -> fn.apply(argument, v);
    }

    private static final class CollectorFileVisitor<PATH extends Path, T> extends SimpleFileVisitor<PATH> {
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
        public FileVisitResult preVisitDirectory(PATH dir, BasicFileAttributes attrs) throws IOException {
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
        public FileVisitResult preVisitDirectory(PATH path, BasicFileAttributes attrs) throws IOException {
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
            List<String> include = list(config, "include", asList("**/*.js", "*.js"));
            List<String> exclude = list(config, "exclude", emptyList());

            this.includeMatchers = matchers(include);
            this.includeDirs = set(config, "include.dirs", emptySet());
            this.includeFiles = set(config, "include.files", emptySet());
            this.excludeMatchers = matchers(exclude);
            this.excludeDirs = set(config, "exclude.dirs", emptySet());
            this.excludeFiles = set(config, "exclude.files", emptySet());
            this.includePrefixList = toPrefixList(include);
            this.excludePrefixList = toPrefixList(exclude);
        }

        private static List<String> toPrefixList(List<String> patterns) {
            Pattern dirPattern = Pattern.compile("^(?:glob:)?((?:[^/*?,{}\\[\\]\\\\]+/)+).*$");
            List<String> list = patterns.stream().map(dirPattern::matcher).map(m -> m.matches() ? m.group(1) : null)
                    .collect(Collectors.toList());
            return list.stream().allMatch(Objects::nonNull) ? list : Collections.emptyList();
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
                        && (includeDirs.isEmpty()
                                || (file.getParent() != null && matches(includeDirs, file.getParent())))
                        && (includeFiles.isEmpty() || matches(includeFiles, file.getFileName()))) {
                    return true;
                }
            }
            return false;
        }

        private static List<PathMatcher> matchers(List<String> patterns) {
            return patterns.stream().map(pattern -> {
                if (!(pattern.startsWith("glob:") || pattern.startsWith("regex:"))) {
                    pattern = "glob:" + pattern;
                }
                return pattern;
            }).map(pattern -> FileSystems.getDefault().getPathMatcher(pattern)).collect(Collectors.toList());
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
