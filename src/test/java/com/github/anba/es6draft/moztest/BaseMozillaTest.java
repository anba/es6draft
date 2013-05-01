/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import static java.util.Arrays.asList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.runners.Parameterized;

/**
 *
 */
public abstract class BaseMozillaTest {
    /**
     * Simple class to store information for each test case
     */
    protected static class MozTest {
        boolean enable = true;
        boolean expect = true;
        boolean random = false;
        boolean slow = false;
        boolean debug = false;
        Path script = null;
        String error = null;

        @Override
        public String toString() {
            return script.toString();
        }
    }

    private static final Pattern testInfoPattern = Pattern.compile("//\\s*\\|(.+?)\\|\\s*(.*)");

    private static void applyPatternInfo(MozTest test, String line) {
        Matcher m = testInfoPattern.matcher(line);
        if (!m.matches()) {
            // ignore if pattern invalid or not present
            return;
        }
        switch (m.group(1)) {
        case "reftest":
            applyRefTestInfo(test, m.group(2));
            break;
        case "jit-test":
            applyJitTestInfo(test, m.group(2));
            break;
        default:
            System.err.printf("invalid tag '%s' in line: %s\n", m.group(1), line);
        }
    }

    private static String[] split(String line) {
        final String comment = "--";
        final String ws = "[ \t\n\r\f\013]+";
        // remove comment if any
        int k = line.indexOf(comment);
        if (k != -1) {
            line = line.substring(0, k);
        }
        // split at whitespace
        return line.trim().split(ws);
    }

    private static void applyRefTestInfo(MozTest test, String content) {
        for (String p : split(content)) {
            if (p.equals("fails")) {
                test.expect = false;
            } else if (p.equals("skip")) {
                test.expect = test.enable = false;
            } else if (p.equals("random")) {
                test.random = true;
            } else if (p.equals("slow")) {
                test.slow = true;
            } else if (p.startsWith("fails-if") || p.startsWith("asserts-if")
                    || p.startsWith("skip-if") || p.startsWith("random-if")
                    || p.startsWith("require-or") || p.equals("silentfail")) {
                // ignore for now...
            } else {
                System.err.printf("invalid manifest line: %s\n", p);
            }
        }
    }

    private static void applyJitTestInfo(MozTest test, String content) {
        for (String p : content.split(";")) {
            int sep = p.indexOf(':');
            if (sep != -1) {
                String name = p.substring(0, sep).trim();
                String value = p.substring(sep + 1).trim();
                switch (name) {
                case "error":
                    test.error = value;
                    break;
                case "exitstatus":
                    // ignore for now...
                    break;
                default:
                    System.err.printf("unknown option '%s' in line: %s\n", name, content);
                }
            } else {
                String name = p.trim();
                switch (name) {
                case "slow":
                    test.slow = true;
                    break;
                case "debug":
                    test.debug = true;
                    break;
                case "allow-oom":
                case "valgrind":
                case "tz-pacific":
                case "mjitalways":
                case "mjit":
                case "no-jm":
                case "ion-eager":
                case "dump-bytecode":
                    // ignore for now...
                    break;
                case "":
                    // ignore empty string
                    break;
                default:
                    System.err.printf("unknown option '%s' in line: %s\n", name, content);
                }
            }
        }
    }

    // Any file who's basename matches something in this set is ignored
    private static final Set<String> excludedSet = new HashSet<>(asList("browser.js", "shell.js",
            "jsref.js", "template.js", "user.js", "js-test-driver-begin.js",
            "js-test-driver-end.js"));

    /**
     * Recursively searches for js-file test cases in {@code basedir} and its sub-directories
     */
    protected static List<MozTest> loadTests(Path searchdir, final Path basedir) throws IOException {
        final List<MozTest> tests = new ArrayList<>();
        Files.walkFileTree(searchdir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (attrs.isRegularFile() && attrs.size() != 0L) {
                    String name = file.getFileName().toString();
                    if (!excludedSet.contains(name) && name.endsWith(".js")) {
                        MozTest test = new MozTest();
                        test.script = basedir.relativize(file);
                        // negative tests end with "-n"
                        if (name.endsWith("-n.js")) {
                            test.expect = false;
                        }
                        try (BufferedReader reader = Files.newBufferedReader(file,
                                StandardCharsets.ISO_8859_1)) {
                            String line = reader.readLine();
                            applyPatternInfo(test, line);
                        }
                        tests.add(test);
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
    protected static List<MozTest> filterTests(List<MozTest> tests, String filename)
            throws IOException {
        // list->map
        Map<Path, MozTest> map = new LinkedHashMap<>();
        for (MozTest test : tests) {
            map.put(test.script, test);
        }
        // disable tests
        List<MozTest> disabledTests = new ArrayList<>();
        InputStream res = MozillaJSTest.class.getResourceAsStream(filename);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                MozTest t = map.get(Paths.get(line));
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
     * {@link Parameterized} expects a list of {@code Object[]}
     */
    protected static Iterable<Object[]> toObjectArray(Iterable<MozTest> iterable) {
        List<Object[]> list = new ArrayList<Object[]>();
        for (MozTest o : iterable) {
            list.add(new Object[] { o });
        }
        return list;
    }
}
