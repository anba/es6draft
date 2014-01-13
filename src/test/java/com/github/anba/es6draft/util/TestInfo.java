/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.runners.Parameterized;

import com.github.anba.es6draft.util.Functional.BiFunction;

/**
 * Base class to store test information
 */
public class TestInfo {
    public final Path basedir;
    public final Path script;
    public boolean enable = true;
    public boolean expect = true;

    public TestInfo(Path basedir, Path file) {
        this.basedir = basedir;
        this.script = basedir.relativize(file);
    }

    @Override
    public String toString() {
        return script.toString();
    }

    public Path toFile() {
        return basedir.resolve(script);
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

    private static final BiFunction<Path, Path, TestInfo> defaultCreate = new BiFunction<Path, Path, TestInfo>() {
        @Override
        public TestInfo apply(Path basedir, Path file) {
            return new TestInfo(basedir, file);
        }
    };

    /**
     * Recursively searches for js-file test cases in {@code searchdir} and its sub-directories
     */
    public static List<TestInfo> loadTests(Path searchdir, final Path basedir,
            final Set<String> excludeDirs, final Set<String> excludeFiles) throws IOException {
        return loadTests(searchdir, basedir, excludeDirs, excludeFiles, defaultCreate);
    }

    /**
     * Recursively searches for js-file test cases in {@code searchdir} and its sub-directories
     */
    public static <T extends TestInfo> List<T> loadTests(Path searchdir, final Path basedir,
            final Set<String> excludeDirs, final Set<String> excludeFiles,
            final BiFunction<Path, Path, T> create) throws IOException {
        final List<T> tests = new ArrayList<>();
        Files.walkFileTree(searchdir, new TestFileVisitor(excludeDirs, excludeFiles) {
            @Override
            public void visitFile(Path file) throws IOException {
                tests.add(create.apply(basedir, file));
            }
        });
        return tests;
    }

    /**
     * Recursively searches for js-file test cases in {@code searchdir} and its sub-directories
     */
    public static <T extends TestInfo> List<T> loadTests(Path searchdir, Set<String> excludeDirs,
            Set<String> excludeFiles, final BiFunction<Path, Iterator<String>, T> create)
            throws IOException {
        final List<T> tests = new ArrayList<>();
        Files.walkFileTree(searchdir, new TestFileVisitor(excludeDirs, excludeFiles) {
            @Override
            public void visitFile(Path file) throws IOException {
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    tests.add(create.apply(file, new LineIterator(reader)));
                } catch (UncheckedIOException e) {
                    throw e.getCause();
                }
            }
        });
        return tests;
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

    private static abstract class TestFileVisitor extends SimpleFileVisitor<Path> {
        private final Set<String> excludeDirs;
        private final Set<String> excludeFiles;
        private final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.js");

        TestFileVisitor(Set<String> excludeDirs, Set<String> excludeFiles) {
            this.excludeDirs = excludeDirs;
            this.excludeFiles = excludeFiles;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
            if (excludeDirs.contains(dir.getFileName().toString())) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (attrs.isRegularFile() && attrs.size() != 0L && matcher.matches(file)) {
                String name = file.getFileName().toString();
                if (!excludeFiles.contains(name)) {
                    visitFile(file);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        protected abstract void visitFile(Path path) throws IOException;
    }
}
