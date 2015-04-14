/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to generate the case folding data used in {@link CaseFoldData}
 */
final class CaseFoldDataGenerator {
    private CaseFoldDataGenerator() {
    }

    public static void main(String[] args) throws Exception {
        // generateSpaceRange();
        // generateInvalidToLowerCases();
        // generateCaseFoldData();
        // generateCaseFoldMethods();
        // generateUnicodeCaseFoldRange();

        // findSpecialUnicodeCaseFold();
        // findEntries();
    }

    public static void generateCaseFoldData() throws IOException {
        List<Integer> caseFold_From = new ArrayList<>();
        List<Integer> caseFold_To = new ArrayList<>();
        List<Integer> caseUnfold_From = new ArrayList<>();
        Map<Integer, List<Integer>> caseUnfold_To = new LinkedHashMap<>();

        Pattern p = Pattern
                .compile("([0-9A-F]{4,5}); ([CFST]); ([0-9A-F]{4,5})(?: ([0-9A-F]{4,5}))?(?: ([0-9A-F]{4,5}))?; # .*");
        Path caseFolding = Paths.get("/tmp/Unicode6.3/CaseFolding.txt");
        List<String> lines = Files.readAllLines(caseFolding);
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            Matcher m = p.matcher(line);
            if (!m.matches()) {
                System.err.println("Invalid line: " + line);
            }
            char kind = m.group(2).charAt(0);
            if (kind == 'T' || kind == 'F') {
                continue;
            }
            if (m.group(3 + 1) != null || m.group(3 + 2) != null) {
                System.err.println("Invalid line: " + line);
            }
            int from = Integer.parseInt(m.group(1), 16);
            int to = Integer.parseInt(m.group(3), 16);

            caseFold_From.add(from);
            caseFold_To.add(to);
            if (!caseUnfold_To.containsKey(to)) {
                caseUnfold_From.add(to);
                caseUnfold_To.put(to, new ArrayList<Integer>());
            }
            caseUnfold_To.get(to).add(from);
        }

        System.out.println(array("CaseFold_From", caseFold_From));
        System.out.println(array("CaseFold_To", caseFold_To));
        System.out.println(array("CaseUnfold_From", caseUnfold_From));
        System.out.println(array("CaseUnfold_To", caseUnfold_To.values()));
    }

    private static String array(String name, List<Integer> codePoints) {
        try (Formatter fmt = new Formatter(new StringBuilder(), Locale.ROOT)) {
            fmt.format("static final int[] %s = {%n/* @formatter:off */%n", name);
            boolean isNewLine = true;
            int index = 0;
            for (int codePoint : codePoints) {
                if (isNewLine) {
                    isNewLine = false;
                    // fmt.format("    ");
                }
                fmt.format("0x%x,", codePoint);
                if (++index % 8 == 0) {
                    isNewLine = true;
                    fmt.format("%n");
                } else {
                    fmt.format(" ");
                }
            }
            if (!isNewLine) {
                fmt.format("%n");
            }
            fmt.format("/* @formatter:on */%n};%n");
            return fmt.toString();
        }
    }

    private static String array(String name, Collection<List<Integer>> codePoints) {
        try (Formatter fmt = new Formatter(new StringBuilder(), Locale.ROOT)) {
            fmt.format("static final int[][] %s = {%n/* @formatter:off */%n", name);
            boolean isNewLine = true;
            int index = 0;
            for (List<Integer> codePoint : codePoints) {
                if (isNewLine) {
                    isNewLine = false;
                    // fmt.format("    ");
                }
                fmt.format("{");
                String prefix = "";
                for (int cp : codePoint) {
                    fmt.format("%s0x%x", prefix, cp);
                    prefix = ", ";
                }
                fmt.format("},");
                if (++index % 6 == 0) {
                    isNewLine = true;
                    fmt.format("%n");
                } else {
                    fmt.format(" ");
                }
            }
            if (!isNewLine) {
                fmt.format("%n");
            }
            fmt.format("/* @formatter:on */%n};%n");
            return fmt.toString();
        }
    }

    /**
     * Create {@link CaseFoldData#appendCaseInsensitiveUnicodeRange(RegExpParser, int, int)} method
     */
    public static void generateUnicodeCaseFoldRange() {
        StringBuilder code = new StringBuilder();
        code.append("public static final void appendCaseInsensitiveUnicodeRange(RegExpParser parser, int startChar, int endChar) {\n");
        code.append("// Type 1\n");
        addType1Characters(code);
        code.append("\n// Type 2\n");
        addType2Characters(code);
        code.append("\n// Type 3\n");
        addType3Characters(code);
        code.append("}\n");

        System.out.println(code);
    }

    private static void addType1Characters(StringBuilder code) {
        final int minCodePoint = Character.MIN_CODE_POINT, maxCodePoint = Character.MAX_CODE_POINT;

        for (int codePoint = minCodePoint; codePoint <= maxCodePoint; ++codePoint) {
            int toUpper = Character.toUpperCase(codePoint);
            int caseFold = Character.toLowerCase(toUpper);
            int foldUpper = Character.toUpperCase(caseFold);
            if (toUpper == caseFold || !isCommonOrSimpleCaseFold(codePoint)) {
                // skip if no case fold or neither common nor simple case fold
                continue;
            }
            assert Character.toLowerCase(foldUpper) == caseFold;

            // code points which are lowercase but not casefold-lowercase
            if (codePoint != toUpper && codePoint != caseFold) {
                addIfStatement2(code, codePoint, toUpper, caseFold, toUpper, caseFold);
            }
        }
    }

    private static void addType2Characters(StringBuilder code) {
        final int minCodePoint = Character.MIN_CODE_POINT, maxCodePoint = Character.MAX_CODE_POINT;

        for (int codePoint = minCodePoint; codePoint <= maxCodePoint; ++codePoint) {
            int toUpper = Character.toUpperCase(codePoint);
            int caseFold = Character.toLowerCase(toUpper);
            int foldUpper = Character.toUpperCase(caseFold);
            if (toUpper == caseFold || !isCommonOrSimpleCaseFold(codePoint)) {
                // skip if no case fold or neither common nor simple case fold
                continue;
            }
            assert Character.toLowerCase(foldUpper) == caseFold;

            // code points which are uppercase but not casefold-uppercase:
            // - case fold has different upper and lower case representations
            if (codePoint == toUpper && codePoint != foldUpper && caseFold != foldUpper) {
                addIfStatement2(code, codePoint, foldUpper, caseFold, foldUpper, caseFold);
                addIfStatement2(code, foldUpper, toUpper, caseFold, toUpper);
            }
        }
    }

    private static void addType3Characters(StringBuilder code) {
        final int minCodePoint = Character.MIN_CODE_POINT, maxCodePoint = Character.MAX_CODE_POINT;

        for (int codePoint = minCodePoint; codePoint <= maxCodePoint; ++codePoint) {
            int toUpper = Character.toUpperCase(codePoint);
            int caseFold = Character.toLowerCase(toUpper);
            int foldUpper = Character.toUpperCase(caseFold);
            if (toUpper == caseFold || !isCommonOrSimpleCaseFold(codePoint)) {
                // skip if no case fold or neither common nor simple case fold
                continue;
            }
            assert Character.toLowerCase(foldUpper) == caseFold;

            // code points which are uppercase but not casefold-uppercase
            // - case fold has same upper and lower case representation
            if (codePoint == toUpper && codePoint != foldUpper && caseFold == foldUpper) {
                addIfStatement1(code, codePoint, foldUpper);
                addIfStatement1(code, foldUpper, toUpper);
            }
        }
    }

    private static boolean isCommonOrSimpleCaseFold(int codePoint) {
        switch (codePoint) {
        case 0x0130:
        case 0x0131:
            return false;
        default:
            return true;
        }
    }

    private static String rangeCheck(int v) {
        return String.format("startChar <= 0x%04x && 0x%04x <= endChar", v, v);
    }

    private static String rangeCheck(int t, int v) {
        if (t < v) {
            return String.format("0x%04x <= endChar", v);
        }
        if (t > v) {
            return String.format("startChar <= 0x%04x", v);
        }
        throw new AssertionError();
    }

    private static String block(int... args) {
        StringBuilder block = new StringBuilder();
        for (int i : args) {
            block.append(String.format("parser.appendCharacter(0x%04x);", i));
        }
        return block.toString();
    }

    private static void addIfStatement1(StringBuilder code, int test, int range1) {
        assert test != range1;

        String cond = String.format("%s && !(%s)", rangeCheck(test), rangeCheck(test, range1));
        String block = block(range1);
        String ifStatement = String.format("if (%s) { %s }%n", cond, block);
        code.append(ifStatement);
    }

    private static void addIfStatement2(StringBuilder code, int test, int range1, int range2,
            int... args) {
        assert test != range1 && test != range2 && range1 != range2;

        String cond;
        if (test < range1 && test < range2) {
            cond = String.format("%s && !(%s)", rangeCheck(test),
                    rangeCheck(test, Math.min(range1, range2)));
        } else if (test > range1 && test > range2) {
            cond = String.format("%s && !(%s)", rangeCheck(test),
                    rangeCheck(test, Math.max(range1, range2)));
        } else {
            cond = String.format("%s && !(%s || %s)", rangeCheck(test), rangeCheck(test, range1),
                    rangeCheck(test, range2));
        }
        String block = block(args);
        String ifStatement = String.format("if (%s) { %s }%n", cond, block);
        code.append(ifStatement);
    }

    /**
     * Create {@link CaseFoldData#isValidToLower(int)} method
     */
    public static void generateInvalidToLowerCases() {
        StringBuilder code = new StringBuilder();
        String methodName = "isValidToLower";
        code.append(String.format("public static final boolean %s(int codePoint) {%n", methodName));
        code.append("switch(codePoint) {\n");
        for (int codePoint = Character.MIN_VALUE; codePoint <= Character.MAX_VALUE; ++codePoint) {
            int toLower = Character.toLowerCase(codePoint);
            int toUpper = Character.toUpperCase(codePoint);
            if (toUpper != Character.toUpperCase(toLower)) {
                assert codePoint == toUpper;
                // System.out.printf("u+%04x -> u+%04x%n", codePoint, toLower);
                code.append(String.format("case 0x%04x:%n", codePoint));
            }
        }
        code.append("return false;\n");
        code.append("default:\nreturn true;\n");
        code.append("}\n");
        code.append("}\n");
        System.out.println(code);
    }

    /**
     * Check {@link CaseFoldData#hasAdditionalUnicodeCaseFold(int)} is correct
     */
    public static void findSpecialUnicodeCaseFold() {
        LinkedHashMap<Integer, Integer> entries = new LinkedHashMap<>();
        for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; ++codePoint) {
            if (!CaseFoldData.caseFoldType(codePoint)) {
                continue;
            }
            int toUpper = Character.toUpperCase(codePoint);
            int caseFold = Character.toLowerCase(toUpper);
            if (toUpper == caseFold) {
                for (int other = Character.MIN_CODE_POINT; other <= Character.MAX_CODE_POINT; ++other) {
                    if (other == codePoint) {
                        continue;
                    }
                    int toUpper2 = Character.toUpperCase(other);
                    int caseFold2 = Character.toLowerCase(toUpper2);
                    if (caseFold == caseFold2) {
                        System.out.printf("u+%04x -> u+%04x%n", codePoint, other);
                        entries.put(codePoint, other);
                    }
                }
            }
        }

        // Test only <u+00df, u+1e9e> has this special behaviour
        assert entries.equals(Collections.singletonMap(0x00df, 0x1e9e));
    }

    /**
     * Generate {@link UCS2Encoding#codeRangeSpace} array
     */
    public static void generateSpaceRange() {
        StringBuilder code = new StringBuilder();
        int count = 0;
        for (int c = Character.MIN_VALUE; c <= Character.MAX_VALUE; ++c) {
            if (isSpace(c)) {
                count += 1;
                int from = c, to = c;
                for (int d = from + 1; d <= Character.MAX_VALUE && isSpace(d); ++d) {
                    to = d;
                }
                code.append(String.format(", 0x%04x, 0x%04x", from, to));
                c = to;
            }
        }
        code.insert(0, count);
        System.out.println(code);
    }

    private static boolean isSpace(int c) {
        switch (c) {
        case 0x0009:
        case 0x000B:
        case 0x000C:
        case 0x0020:
        case 0x00A0:
        case 0xFEFF:
        case 0x000A:
        case 0x000D:
        case 0x2028:
        case 0x2029:
            return true;
        default:
            return Character.getType(c) == Character.SPACE_SEPARATOR;
        }
    }

    private static TreeMap<Integer, List<Integer>> findEntries() {
        TreeMap<Integer, List<Integer>> entries = new TreeMap<>();
        for (int codePoint = Character.MIN_VALUE; codePoint <= Character.MAX_VALUE; ++codePoint) {
            if (!CaseFoldData.caseFoldType(codePoint)) {
                continue;
            }
            if (codePoint <= 0x7f) {
                // ignore mappings from ASCII
                continue;
            }
            int toLower = Character.toLowerCase(codePoint);
            int toUpper = Character.toUpperCase(codePoint);
            if (!CaseFoldData.isValidCaseFold(codePoint, toUpper, toLower)) {
                continue;
            }
            for (int other = Character.MIN_VALUE; other <= Character.MAX_VALUE; ++other) {
                if (other == codePoint || other == toLower || other == toUpper) {
                    continue;
                }
                if (!CaseFoldData.caseFoldType(other)) {
                    continue;
                }
                if (other <= 0x7f) {
                    // ignore mappings to ASCII
                    continue;
                }
                int toUpper2 = Character.toUpperCase(other);
                if (toUpper == toUpper2) {
                    // found two different code points with same uppercase representation
                    // System.out.printf("u+%04x -> u+%04x%n", codePoint, other);

                    // additional entry for codePoint -> other
                    if (!entries.containsKey(codePoint)) {
                        entries.put(codePoint, new ArrayList<Integer>());
                    }
                    entries.get(codePoint).add(other);
                }
            }

            assert toUpper > 0x7f : Integer.toString(codePoint, 16);
            assert toLower > 0x7f || !CaseFoldData.isValidToLower(codePoint) : Integer.toString(
                    codePoint, 16);

            // check for duplicates
            ArrayList<Integer> out = new ArrayList<>();
            out.add(codePoint);
            if (codePoint != toUpper) {
                out.add(toUpper);
            }
            if (codePoint != toLower && CaseFoldData.isValidToLower(codePoint)) {
                out.add(toLower);
            }
            if (entries.containsKey(codePoint)) {
                out.addAll(entries.get(codePoint));
            }
            assert new HashSet<>(out).size() == out.size() : "duplicates: " + out;
        }

        return entries;
    }

    public static void generateCaseFoldMethods() {
        TreeMap<Integer, List<Integer>> entries = findEntries();
        for (int minListSize = 1;; minListSize += 1) {
            StringBuilder code = new StringBuilder();
            String methodName = "caseFold" + minListSize;
            code.append(String.format("public static final int %s(int codePoint) {%n", methodName));
            code.append("switch(codePoint) {\n");
            LinkedHashMap<Integer, List<Integer>> clauses = collect(entries, minListSize);
            if (clauses.isEmpty()) {
                break;
            }
            for (Map.Entry<Integer, List<Integer>> clause : clauses.entrySet()) {
                for (int codePoint : clause.getValue()) {
                    code.append(String.format("case 0x%04x: %n", codePoint));
                }
                code.append(String.format(" return 0x%04x;%n", clause.getKey()));
            }
            code.append("default: return -1;\n");
            code.append("}\n");
            code.append("}\n");
            System.out.println(code);
        }
    }

    private static LinkedHashMap<Integer, List<Integer>> collect(
            TreeMap<Integer, List<Integer>> entries, int minListSize) {
        LinkedHashMap<Integer, List<Integer>> clauses = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : entries.entrySet()) {
            if (entry.getValue().size() >= minListSize) {
                int codePoint = entry.getKey();
                int other = entry.getValue().get(minListSize - 1);
                if (!clauses.containsKey(other)) {
                    clauses.put(other, new ArrayList<Integer>());
                }
                clauses.get(other).add(codePoint);
            }
        }
        return clauses;
    }
}
