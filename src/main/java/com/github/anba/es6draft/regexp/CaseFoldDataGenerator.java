/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Utility class to generate the case folding data used in {@link CaseFoldData}
 */
@SuppressWarnings("unused")
final class CaseFoldDataGenerator {
    private CaseFoldDataGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path unicode = Paths.get(args.length > 0 ? args[0] : "/tmp/unicode/Unicode8.0");

        // String range = generateSpaceRange(unicode);
        // System.out.println(range);

        CaseFold bmpMapping = generateCaseFoldDataBMP(unicode);
        CaseFold unicodeMapping = generateCaseFoldDataUnicode(unicode);
        CaseFold caseFoldMapping = generateCaseFoldData(unicode);

        System.out.println(unicodeMapping.equals(caseFoldMapping));
        // unicodeMapping.printTo(System.out);

        // String caseFoldTest = generateCaseFoldTest(unicode);
        // System.out.println(caseFoldTest);
    }

    static CaseFold generateCaseFoldDataBMP(Path unicode) throws IOException {
        Map<Integer, Integer> toUpper = new HashMap<>();
        Map<Integer, Integer> toLower = new HashMap<>();
        caseMappings(unicode, toUpper, toLower);

        CaseFold caseFolding = new CaseFold();
        codePoints(unicode).forEach(codeValue -> {
            int caseFold;
            if (toUpper.containsKey(codeValue)) {
                caseFold = toUpper.get(codeValue);
            } else {
                return;
            }
            // ES2015, 21.2.2.8.2 Runtime Semantics: Canonicalize ( ch )
            // 1. Ignore non-BMP code points.
            if (codeValue > 0xffff) {
                return;
            }
            // 2. Ignore mapping outside of basic multilingual plane.
            if (caseFold > 0xffff) {
                return;
            }
            // 3. Ignore mapping from non-ASCII to ASCII.
            if (codeValue > 0x7f && caseFold <= 0x7f) {
                return;
            }
            caseFolding.add(codeValue, caseFold);
        });
        return caseFolding;
    }

    static CaseFold generateCaseFoldDataUnicode(Path unicode) throws IOException {
        Map<Integer, Integer> toUpper = new HashMap<>();
        Map<Integer, Integer> toLower = new HashMap<>();
        caseMappings(unicode, toUpper, toLower);

        CaseFold caseFolding = new CaseFold();
        codePoints(unicode).forEach(codeValue -> {
            int caseFold;
            if (isCherokeeUppercase(codeValue)) {
                // Switch Cherokee uppercase/lowercase for compatibility with CaseFolding.txt output.
                return;
            } else if (isCherokeeLowercase(codeValue)) {
                caseFold = toUpper.get(codeValue);
            } else if (toLower.containsKey(codeValue)) {
                caseFold = toLower.get(codeValue);
            } else if (toUpper.containsKey(codeValue) && toLower.containsKey(toUpper.get(codeValue))
                    && codeValue != toLower.get(toUpper.get(codeValue))) {
                caseFold = toLower.get(toUpper.get(codeValue));
            } else {
                return;
            }
            caseFolding.add(codeValue, caseFold);
        });
        return caseFolding;
    }

    static CaseFold generateCaseFoldData(Path unicode) throws IOException {
        CaseFold caseFolding = new CaseFold();
        caseFolding(unicode).forEach(m -> {
            char kind = m.group("status").charAt(0);
            if (kind == 'T' || kind == 'F') {
                return;
            }
            if (!isSingleCodePoint(m.group("mapping"))) {
                System.err.println("Invalid line: " + m.group());
            }
            int codeValue = Integer.parseInt(m.group("code"), 16);
            int caseFold = Integer.parseInt(m.group("mapping"), 16);
            caseFolding.add(codeValue, caseFold);
        });
        return caseFolding;
    }

    /**
     * Generates the test data for "unicode_case_folding.jsm".
     */
    static String generateCaseFoldTest(Path unicode) throws IOException {
        class CaseFoldRange {
            final Stream.Builder<int[]> builder = Stream.builder();
            boolean started, inRange;
            int startCodeValue, startCaseFold;
            int endCodeValue, endCaseFold;
            int steps;

            Stream<int[]> stream() {
                if (started) {
                    builder.add(new int[] { startCodeValue, startCaseFold, endCodeValue, endCaseFold, steps });
                }
                return builder.build();
            }

            CaseFoldRange add(int[] m) {
                int codeValue = m[0];
                int caseFold = m[1];
                assert codeValue > endCodeValue;
                int step1 = codeValue - endCodeValue;
                int step2 = caseFold - endCaseFold;
                if (started && step1 == step2 && (!inRange || step1 == steps)) {
                    endCodeValue = codeValue;
                    endCaseFold = caseFold;
                    steps = step1;
                    inRange = true;
                } else {
                    if (started) {
                        builder.add(new int[] { startCodeValue, startCaseFold, endCodeValue, endCaseFold, steps });
                    }
                    startCodeValue = endCodeValue = codeValue;
                    startCaseFold = endCaseFold = caseFold;
                    steps = 1;
                    started = true;
                    inRange = false;
                }
                return this;
            }

            CaseFoldRange unsupportedCombine(CaseFoldRange other) {
                throw new IllegalStateException();
            }
        }
        Map<Integer, Integer> toUpper = new HashMap<>();
        Map<Integer, Integer> toLower = new HashMap<>();
        caseMappings(unicode, toUpper, toLower);

        return caseFolding(unicode).filter(m -> {
            char kind = m.group("status").charAt(0);
            return kind == 'C' || kind == 'S';
        }).map(m -> {
            return new int[] { Integer.parseInt(m.group("code"), 16), Integer.parseInt(m.group("mapping"), 16) };
        }).sequential().reduce(new CaseFoldRange(), CaseFoldRange::add, CaseFoldRange::unsupportedCombine).stream()
                .map(range -> {
                    int startCodeValue = range[0], startCaseFold = range[1];
                    int endCodeValue = range[2], endCaseFold = range[3];
                    int steps = range[4];
                    String type;
                    if (startCodeValue <= 0xff && startCaseFold <= 0xff && endCodeValue <= 0xff
                            && endCaseFold <= 0xff) {
                        type = "latin";
                    } else if (startCodeValue <= 0xffff && startCaseFold <= 0xffff && endCodeValue <= 0xffff
                            && endCaseFold <= 0xffff) {
                        type = "basic";
                    } else {
                        type = "supplementary";
                    }
                    String options = "";
                    if (startCodeValue == endCodeValue && startCaseFold == endCaseFold) {
                        if (startCodeValue > 0x7f && startCaseFold <= 0x7f) {
                            // Single mapping with case folding into ASCII range.
                            options = ", {unicode: true}";
                        } else {
                            // Single mapping with different case-fold-upper value.
                            int codePoint = startCodeValue;
                            int upperCase = toUpper.getOrDefault(codePoint, codePoint);
                            int caseFold = toLower.getOrDefault(upperCase, upperCase);
                            int caseFoldUpper = toUpper.getOrDefault(caseFold, caseFold);
                            if (codePoint == upperCase && codePoint != caseFoldUpper) {
                                options = ", {unicode: true}";
                            }
                        }
                    }
                    return String.format("test(range(0x%x, 0x%x, %d), range(0x%x, 0x%x, %d), %s%s);", startCodeValue,
                            endCodeValue, steps, startCaseFold, endCaseFold, steps, type, options);
                }).collect(Collectors.joining("\n"));
    }

    private static final class CaseFold {
        private final List<Integer> caseFold_From = new ArrayList<>();
        private final List<Integer> caseFold_To = new ArrayList<>();
        private final List<Integer> caseUnfold_From = new ArrayList<>();
        private final Map<Integer, List<Integer>> caseUnfold_To = new LinkedHashMap<>();

        void add(int codeValue, int caseFold) {
            assert codeValue != caseFold : String.format("%d == %d", codeValue, caseFold);
            caseFold_From.add(codeValue);
            caseFold_To.add(caseFold);
            if (!caseUnfold_To.containsKey(caseFold)) {
                caseUnfold_From.add(caseFold);
                caseUnfold_To.put(caseFold, new ArrayList<>());
            }
            caseUnfold_To.get(caseFold).add(codeValue);
        }

        void printTo(PrintStream stream) {
            stream.println(array("CaseFold_From", caseFold_From));
            stream.println(array("CaseFold_To", caseFold_To));
            stream.println(array("CaseUnfold_From", caseUnfold_From));
            stream.println(array("CaseUnfold_To", caseUnfold_To.values()));
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CaseFold)) {
                return false;
            }
            CaseFold other = (CaseFold) obj;
            if (!caseFold_From.equals(other.caseFold_From)) {
                return false;
            }
            if (!caseFold_To.equals(other.caseFold_To)) {
                return false;
            }
            if (!caseUnfold_From.equals(other.caseUnfold_From)) {
                return false;
            }
            if (!caseUnfold_To.equals(other.caseUnfold_To)) {
                return false;
            }
            return true;
        }

        private static String array(String name, List<Integer> codePoints) {
            try (Formatter fmt = new Formatter(new StringBuilder(), Locale.ROOT)) {
                fmt.format("static final int[] %s = {%n/* @formatter:off */%n", name);
                boolean isNewLine = true;
                int index = 0;
                for (int codePoint : codePoints) {
                    if (isNewLine) {
                        isNewLine = false;
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
    }

    private static IntStream codePoints(Path unicode) throws IOException {
        return unicodeData(unicode).mapToInt(m -> Integer.parseInt(m.group("codeValue"), 16));
    }

    private static Stream<Matcher> unicodeData(Path unicode) throws IOException {
        // ftp://ftp.unicode.org/Public/3.0-Update/UnicodeData-3.0.0.html
        String codeValue = "(?<codeValue>[0-9A-F]{4,6})";
        String characterName = "(?<characterName>[A-Z0-9\\- ]+|<control>|<(?<rangeName>[A-Za-z0-9 ]+), (?<range>First|Last)>)";
        String generalCategory = "(?<generalCategory>[A-Z][a-z])";
        String canonicalCombiningClass = "(?<canonicalCombiningClass>[0-9]+)";
        String bidirectionalCategory = "(?<bidirectionalCategory>[A-Z]{1,3})";
        String characterDecompositionMapping = "(?<characterDecompositionMapping>(?:<[A-Za-z]+> )?[0-9A-F]{4,6}(?: [0-9A-F]{4,6})*)?";
        String decimalDigitValue = "(?<decimalDigitValue>[0-9])?";
        String digitValue = "(?<digitValue>[0-9]+)?";
        String numericValue = "(?<numericValue>-?[0-9]+(?:/[0-9]+)?)?";
        String mirrored = "(?<mirrored>Y|N)";
        String unicode1Name = "(?<unicode1Name>[^;]*)";
        String commentField = "(?<commentField>[^;]*)";
        String uppercaseMapping = "(?<uppercaseMapping>[0-9A-F]{4,6})?";
        String lowercaseMapping = "(?<lowercaseMapping>[0-9A-F]{4,6})?";
        String titlecaseMapping = "(?<titlecaseMapping>[0-9A-F]{4,6})?";
        Pattern p = Pattern.compile(String.join(";", codeValue, characterName, generalCategory, canonicalCombiningClass,
                bidirectionalCategory, characterDecompositionMapping, decimalDigitValue, digitValue, numericValue,
                mirrored, unicode1Name, commentField, uppercaseMapping, lowercaseMapping, titlecaseMapping));

        return unicodeStream(unicode.resolve("UnicodeData.txt"), p);
    }

    private static Stream<Matcher> caseFolding(Path unicode) throws IOException {
        // Format "<code>; <status>; <mapping>; # <name>" defined in CaseFolding.txt.
        String code = "(?<code>[0-9A-F]{4,6})";
        String status = "(?<status>[CFST])";
        String mapping = "(?<mapping>[0-9A-F]{4,6}(?: [0-9A-F]{4,6})*)";
        String name = "# (?<name>.*)";
        Pattern p = Pattern.compile(String.join("; ", code, status, mapping, name));

        return unicodeStream(unicode.resolve("CaseFolding.txt"), p);
    }

    private static Stream<Matcher> unicodeStream(Path path, Pattern pattern) throws IOException {
        return Files.lines(path, StandardCharsets.UTF_8).filter(line -> !(line.isEmpty() || line.charAt(0) == '#'))
                .map(line -> {
                    Matcher matcher = pattern.matcher(line);
                    if (!matcher.matches()) {
                        System.err.println("Invalid line: " + line);
                    }
                    return matcher;
                });
    }

    private static void caseMappings(Path unicode, Map<Integer, Integer> toUpper, Map<Integer, Integer> toLower)
            throws IOException {
        unicodeData(unicode).forEach(matcher -> {
            int codeValue = Integer.parseInt(matcher.group("codeValue"), 16);
            if (codeValue == 0x0130 || codeValue == 0x0131) {
                // Skip: LATIN CAPITAL LETTER I WITH DOT ABOVE
                // Skip: LATIN SMALL LETTER DOTLESS I
                return;
            }
            String uppercaseMapping = matcher.group("uppercaseMapping");
            String lowercaseMapping = matcher.group("lowercaseMapping");
            if (uppercaseMapping != null && isSingleCodePoint(uppercaseMapping)) {
                toUpper.put(codeValue, Integer.parseInt(uppercaseMapping, 16));
            }
            if (lowercaseMapping != null && isSingleCodePoint(lowercaseMapping)) {
                toLower.put(codeValue, Integer.parseInt(lowercaseMapping, 16));
            }
        });
    }

    private static boolean isCherokeeUppercase(int codeValue) {
        return (0x13A0 <= codeValue && codeValue <= 0x13EF) || (0x13F0 <= codeValue && codeValue <= 0x13F5);
    }

    private static boolean isCherokeeLowercase(int codeValue) {
        return (0xAB70 <= codeValue && codeValue <= 0xABBF) || (0x13F8 <= codeValue && codeValue <= 0x13FD);
    }

    private static boolean isSingleCodePoint(String s) {
        if (4 <= s.length() && s.length() <= 6) {
            for (int i = 0; i < s.length(); ++i) {
                if (Character.digit(s.charAt(i), 16) < 0) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Generate {@link UEncoding#codeRangeSpace} array
     */
    static String generateSpaceRange(Path unicode) throws IOException {
        Set<Integer> spaceSeparator = unicodeData(unicode).filter(m -> "Zs".equals(m.group("generalCategory")))
                .map(m -> Integer.parseInt(m.group("codeValue"), 16)).collect(Collectors.toSet());
        StringBuilder code = new StringBuilder();
        int count = 0;
        for (int c = Character.MIN_CODE_POINT; c <= Character.MAX_CODE_POINT; ++c) {
            if (isSpace(c, spaceSeparator)) {
                count += 1;
                int from = c, to = c;
                for (int d = from + 1; d <= Character.MAX_VALUE && isSpace(d, spaceSeparator); ++d) {
                    to = d;
                }
                code.append(String.format(", 0x%04x, 0x%04x", from, to));
                c = to;
            }
        }
        return code.insert(0, count).toString();
    }

    private static boolean isSpace(int c, Set<Integer> spaceSeparator) {
        switch (c) {
        /* ES2015 11.2 White Space */
        case 0x0009:
        case 0x000B:
        case 0x000C:
        case 0x0020:
        case 0x00A0:
        case 0xFEFF:
            return true;
        /* ES2015 11.3 Line Terminators */
        case 0x000A:
        case 0x000D:
        case 0x2028:
        case 0x2029:
            return true;
        /* ES2015 11.2 White Space */
        default:
            return spaceSeparator.contains(c);
        }
    }
}
