/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.runners.Parameterized;

import com.github.anba.es6draft.util.Functional.BiFunction;

/**
 * Base class to store test information
 */
public class TestInfo {
    public Path script;
    public boolean enable = true;
    public boolean expect = true;

    public TestInfo(Path script) {
        this.script = script;
    }

    @Override
    public String toString() {
        return script.toString();
    }

    /**
     * {@link Parameterized} expects a list of {@code Object[]}
     */
    public static Iterable<TestInfo[]> toObjectArray(Iterable<? extends TestInfo> iterable) {
        List<TestInfo[]> list = new ArrayList<TestInfo[]>();
        for (TestInfo o : iterable) {
            list.add(new TestInfo[] { o });
        }
        return list;
    }

    /**
     * Recursively searches for js-file test cases in {@code searchdir} and its sub-directories
     */
    public static <T extends TestInfo> List<T> loadTests(Path searchdir, final Path basedir,
            final Set<String> excludeDirs, final Set<String> excludeFiles,
            final BiFunction<Path, BufferedReader, T> create) throws IOException {
        return loadTests(searchdir, basedir, excludeDirs, excludeFiles, StandardCharsets.UTF_8,
                create);
    }

    /**
     * Recursively searches for js-file test cases in {@code searchdir} and its sub-directories
     */
    public static <T extends TestInfo> List<T> loadTests(Path searchdir, final Path basedir,
            final Set<String> excludeDirs, final Set<String> excludeFiles, final Charset charset,
            final BiFunction<Path, BufferedReader, T> create) throws IOException {
        final List<T> tests = new ArrayList<>();
        Files.walkFileTree(searchdir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (excludeDirs.contains(dir.getFileName().toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (attrs.isRegularFile() && attrs.size() != 0L) {
                    String name = file.getFileName().toString();
                    if (!excludeFiles.contains(name) && name.endsWith(".js")) {
                        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
                            tests.add(create.apply(basedir.relativize(file), reader));
                        } catch (UncheckedIOException e) {
                            throw e.getCause();
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return tests;
    }

    /**
     * Filter the initially collected test cases
     */
    public static <T extends TestInfo> List<T> filterTests(List<T> tests, String filename)
            throws IOException {
        // list->map
        Map<Path, TestInfo> map = new LinkedHashMap<>();
        for (TestInfo test : tests) {
            map.put(test.script, test);
        }
        // disable tests
        List<TestInfo> disabledTests = new ArrayList<>();
        InputStream res = TestInfo.class.getResourceAsStream(filename);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res,
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
}
