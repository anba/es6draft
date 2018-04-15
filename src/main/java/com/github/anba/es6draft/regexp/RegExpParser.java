/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import static com.github.anba.es6draft.parser.Characters.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

import org.joni.Config;

import com.github.anba.es6draft.parser.Characters;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.RuntimeContext;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.2 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>21.2.1 Patterns
 * <li>21.2.2 Pattern Semantics
 * </ul>
 */
public final class RegExpParser {
    private static final boolean UNICODE_PROPERTY_TESTER_ENABLED = true;
    private static final int BACKREF_LIMIT = 0xFFFF;
    private static final int DEPTH_LIMIT = 0xFFFF;
    private static final char[] HEXDIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F' };

    /** \w ~ [a-zA-Z0-9_] and LATIN SMALL LETTER LONG S and KELVIN SIGN */
    private static final String characterClass_wui = "a-zA-Z0-9_\\u017F\\u212A";
    /** \W ~ [\u0000-\u002F\u003A-\u0040\u005B-\u005E\u0060\u007B-\u017E\u0180-\u2129\u212B-\x{10FFFF}] */
    private static final String characterClass_Wui = "\\u0000-\\u002F\\u003A-\\u0040\\u005B-\\u005E\\u0060\\u007B-\\u017E\\u0180-\\u2129\\u212B-\\x{10FFFF}";
    /** \b ~ {@literal (?:(?=\w)(?<!\w)|(?<=\w)(?!\w))} */
    private static final String assertion_bui = String.format(Locale.US,
            "(?:(?=[%1$s])(?<![%1$s])|(?<=[%1$s])(?![%1$s]))", characterClass_wui);
    /** \B ~ {@literal (?:(?=\w)(?<=\w)|(?<!\w)(?!\w))} */
    private static final String assertion_Bui = String.format(Locale.US,
            "(?:(?=[%1$s])(?<=[%1$s])|(?<![%1$s])(?![%1$s]))", characterClass_wui);
    /** {@literal .} in {@link Pattern#DOTALL} mode */
    private static final String dotAll = "(?s:.)";

    private final String source;
    private final int length;
    private final int flags;
    private final String sourceFile;
    private final int sourceLine;
    private final int sourceColumn;
    private final boolean webRegExp;
    private final boolean lookBehind;
    private final boolean namedCapture;
    private final boolean unicodeProperties;
    private final boolean possessive;

    // Normalized regular expression.
    private final StringBuilder out;

    // Current source position.
    private int pos = 0;

    // Map of groups created within negative lookahead.
    private final BitSet negativeLAGroups = new BitSet();

    // Map of named capturing groups.
    private final Map<String, Integer> namedGroups;

    private RegExpParser(RuntimeContext context, String source, int flags, String sourceFile, int sourceLine,
            int sourceColumn) {
        this.source = source;
        this.length = source.length();
        this.flags = flags;
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.sourceColumn = sourceColumn;
        this.webRegExp = context.isEnabled(CompatibilityOption.WebRegularExpressions);
        this.lookBehind = context.isEnabled(CompatibilityOption.RegExpLookBehind);
        this.namedCapture = context.isEnabled(CompatibilityOption.RegExpNamedCapture);
        this.unicodeProperties = isUnicode() && context.isEnabled(CompatibilityOption.RegExpUnicodeProperties);
        this.possessive = context.isEnabled(CompatibilityOption.RegExpPossessive);
        this.namedGroups = namedCapture ? new LinkedHashMap<>() : Collections.emptyMap();
        this.out = new StringBuilder(length);
    }

    public static RegExpMatcher parse(RuntimeContext context, String pattern, String flags, String sourceFile,
            int sourceLine, int sourceColumn) throws ParserException {
        int iflags = parseFlags(context, flags, sourceFile, sourceLine, sourceColumn);
        if ((iflags & Pattern.CASE_INSENSITIVE) == 0 && isSimpleRegExp(pattern)) {
            return new SimpleRegExpMatcher(pattern);
        }
        if (iflags == Pattern.UNICODE_CASE && context.isEnabled(CompatibilityOption.RegExpUnicodeProperties)
                && isUnicodePropertyTester(pattern)) {
            SimpleUnicodeRegExpMatcher matcher = createUnicodePropertyMatcher(pattern);
            if (matcher != null) {
                return matcher;
            }
        }
        RegExpParser parser = new RegExpParser(context, pattern, iflags, sourceFile, sourceLine, sourceColumn);
        parser.pattern();
        return new JoniRegExpMatcher(parser.out.toString(), parser.flags, parser.negativeLAGroups, parser.namedGroups);
    }

    public static void syntaxParse(RuntimeContext context, String pattern, String flags, String sourceFile,
            int sourceLine, int sourceColumn) throws ParserException {
        int iflags = parseFlags(context, flags, sourceFile, sourceLine, sourceColumn);
        if (!isSimpleRegExp(pattern)) {
            RegExpParser parser = new RegExpParser(context, pattern, iflags, sourceFile, sourceLine, sourceColumn);
            parser.pattern();
        }
    }

    private static boolean isSimpleRegExp(String pattern) {
        for (int i = 0; i < pattern.length(); ++i) {
            char c = pattern.charAt(i);
            if (isSyntaxCharacter(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUnicodePropertyTester(String pattern) {
        if (!UNICODE_PROPERTY_TESTER_ENABLED) {
            return false;
        }
        // Accept pattern: ^\p{...}+$ (complete string)
        // Accept pattern: ^\p{...}+ (starts with)
        // Accept pattern: \p{...}+ (contains)
        final int length = pattern.length();

        int start = 0;
        if (start < length && pattern.charAt(start) == '^') {
            start += 1;
        }
        if (!(start < length && pattern.charAt(start) == '\\')) {
            return false;
        }
        start += 1;
        if (!(start < length && (pattern.charAt(start) == 'p') || pattern.charAt(start) == 'P')) {
            return false;
        }
        start += 1;
        if (!(start < length && pattern.charAt(start) == '{')) {
            return false;
        }
        start += 1;

        int end = length - 1;
        if (end >= 0 && pattern.charAt(end) == '$' && pattern.charAt(0) == '^') {
            end -= 1;
        }
        if (end >= 0 && pattern.charAt(end) == '+') {
            end -= 1;
        }
        if (!(end >= 0 && pattern.charAt(end) == '}')) {
            return false;
        }
        end -= 1;

        boolean hasEquals = false;
        for (int i = start; i <= end; ++i) {
            char c = pattern.charAt(i);
            if (isASCIIAlphaNumericUnderscore(c)) {
                continue;
            }
            if (c == '=' && i > start && !hasEquals) {
                hasEquals = true;
                continue;
            }
            return false;
        }
        return true;
    }

    private static SimpleUnicodeRegExpMatcher createUnicodePropertyMatcher(String pattern) {
        int start = 0, end = pattern.length() - 1;
        boolean startsWith = pattern.charAt(start) == '^';
        if (startsWith) {
            start += 1;
        }
        boolean inverse = pattern.charAt(start + 1) == 'P';
        boolean endsWith = pattern.charAt(end) == '$';
        if (endsWith) {
            end -= 1;
        }
        boolean plus = pattern.charAt(end) == '+';
        if (plus) {
            end -= 1;
        }

        IntPredicate predicate;
        String unicodeProperty = pattern.substring(start + 3, end);
        int equals = unicodeProperty.indexOf('=');
        if (equals < 0) {
            // UnicodePropertyValueExpression :: LoneUnicodePropertyNameOrValue
            String name = unicodeProperty;
            if (UnicodeData.EnumProperty.General_Category.isValue(name)) {
                predicate = UnicodeData.EnumProperty.General_Category.predicate(name);
            } else {
                switch (name) {
                case "Any":
                    predicate = c -> Character.MIN_CODE_POINT <= c && c <= Character.MAX_CODE_POINT;
                    break;
                case "ASCII":
                    predicate = c -> Character.MIN_CODE_POINT <= c && c <= 0x7f;
                    break;
                case "Assigned":
                    predicate = UnicodeData.EnumProperty.General_Category.predicate("Cn");
                    inverse = !inverse;
                    break;
                default:
                    UnicodeData.Property property = UnicodeData.Property.from(name);
                    if (!(property instanceof UnicodeData.BinaryProperty)) {
                        return null;
                    }
                    predicate = ((UnicodeData.BinaryProperty) property).predicate();
                    break;
                }
            }
        } else {
            // UnicodePropertyValueExpression :: UnicodePropertyName=UnicodePropertyValue
            String name = unicodeProperty.substring(0, equals);
            String value = unicodeProperty.substring(equals + 1);
            UnicodeData.Property property = UnicodeData.Property.from(name);
            if (!(property instanceof UnicodeData.EnumProperty)) {
                return null;
            }
            UnicodeData.EnumProperty enumProperty = (UnicodeData.EnumProperty) property;
            if (!enumProperty.isValue(value)) {
                return null;
            }
            predicate = enumProperty.predicate(value);
        }
        if (inverse) {
            predicate = predicate.negate();
        }

        SimpleUnicodeRegExpMatcher.Term term;
        if (plus) {
            term = SimpleUnicodeRegExpMatcher.plus(predicate);
        } else {
            term = SimpleUnicodeRegExpMatcher.character(predicate);
        }

        SimpleUnicodeRegExpMatcher.Matcher matcher;
        if (startsWith && endsWith) {
            matcher = SimpleUnicodeRegExpMatcher.stringMatches(term);
        } else if (startsWith && !endsWith) {
            matcher = SimpleUnicodeRegExpMatcher.startsWith(term);
        } else {
            assert !startsWith && !endsWith;
            matcher = SimpleUnicodeRegExpMatcher.contains(term, predicate);
        }
        return new SimpleUnicodeRegExpMatcher(pattern, matcher);
    }

    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn + pos, messageKey,
                args);
    }

    private ParserException error(Messages.Key messageKey, int offset, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn + pos + offset,
                messageKey, args);
    }

    private ParserException error(Messages.Key messageKey, int offset, char offending) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn + pos + offset,
                messageKey, String.valueOf(offending));
    }

    private ParserException errorAt(Messages.Key messageKey, int pos, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn + pos, messageKey,
                args);
    }

    private static int parseFlags(RuntimeContext context, String flags, String sourceFile, int sourceLine,
            int sourceColumn) {
        boolean allowDotAll = context.isEnabled(CompatibilityOption.RegExpDotAll);
        int mask = 0;
        for (int i = 0; i < flags.length(); ++i) {
            char c = flags.charAt(i);
            int flag = flagMask(c, allowDotAll);
            if (flag < 0) {
                throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn,
                        Messages.Key.RegExpInvalidFlag, String.valueOf(c));
            }
            if ((mask & flag) != 0) {
                throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn,
                        Messages.Key.RegExpDuplicateFlag, flagName(c));
            }
            mask |= flag;
        }
        return toPatternFlags(mask);
    }

    // flags :: g | i | m | u | y
    private static final class Flags {
        static final int GLOBAL = 0x01;
        static final int IGNORE_CASE = 0x02;
        static final int MULTILINE = 0x04;
        static final int UNICODE = 0x08;
        static final int STICKY = 0x10;
        static final int DOTALL = 0x20;
    }

    private static int toPatternFlags(int mask) {
        int flags = 0;
        if ((mask & Flags.IGNORE_CASE) != 0) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        if ((mask & Flags.UNICODE) != 0) {
            flags |= Pattern.UNICODE_CASE;
        }
        if ((mask & Flags.MULTILINE) != 0) {
            flags |= Pattern.MULTILINE;
        }
        if ((mask & Flags.DOTALL) != 0) {
            flags |= Pattern.DOTALL;
        }
        return flags;
    }

    private static int flagMask(char c, boolean allowDotAll) {
        switch (c) {
        case 'g':
            return Flags.GLOBAL;
        case 'i':
            return Flags.IGNORE_CASE;
        case 'm':
            return Flags.MULTILINE;
        case 'u':
            return Flags.UNICODE;
        case 'y':
            return Flags.STICKY;
        case 's':
            return allowDotAll ? Flags.DOTALL : -1;
        default:
            return -1;
        }
    }

    private static String flagName(char c) {
        switch (c) {
        case 'g':
            return "global";
        case 'i':
            return "ignoreCase";
        case 'm':
            return "multiline";
        case 'u':
            return "unicode";
        case 'y':
            return "sticky";
        case 's':
            return "dotAll";
        default:
            throw new AssertionError();
        }
    }

    /**
     * Returns {@code true} if regular expression patterns support the {@link CompatibilityOption#WebRegularExpressions}
     * extension.
     * 
     * @return {@code true} if patterns are in web-compatibility mode
     */
    private boolean isWebRegularExpression() {
        return webRegExp;
    }

    private boolean isIgnoreCase() {
        return (flags & Pattern.CASE_INSENSITIVE) != 0;
    }

    private boolean isUnicode() {
        return (flags & Pattern.UNICODE_CASE) != 0;
    }

    private boolean isMultiline() {
        return (flags & Pattern.MULTILINE) != 0;
    }

    private boolean isDotAll() {
        return (flags & Pattern.DOTALL) != 0;
    }

    private void resetParser() {
        out.setLength(0);
        pos = 0;
        negativeLAGroups.clear();
        namedGroups.clear();
    }

    private void reset(int p) {
        pos = p;
    }

    private char peek(int i) {
        return pos + i < length ? source.charAt(pos + i) : '\0';
    }

    private char get() {
        return source.charAt(pos++);
    }

    private int get(boolean unicode) {
        char c = source.charAt(pos++);
        if (unicode && Character.isHighSurrogate(c) && Character.isLowSurrogate(peek(0))) {
            return Character.toCodePoint(c, get());
        }
        return c;
    }

    private String substr(int len) {
        String s = source.substring(pos, pos + len);
        pos += len;
        return s;
    }

    private boolean match(char c) {
        if (c == peek(0)) {
            get();
            return true;
        }
        return false;
    }

    private char mustMatch(char c) {
        if (!match(c)) {
            throw error(Messages.Key.RegExpUnexpectedCharacter, String.valueOf(c));
        }
        return c;
    }

    private boolean eof() {
        return pos >= length;
    }

    /**
     * <pre>
     * DecimalDigits ::
     *     DecimalDigit
     *     DecimalDigits DecimalDigit
     * </pre>
     * 
     * @return the parsed decimal integer value
     */
    private long decimal() {
        if (!isDecimalDigit(peek(0))) {
            return -1;
        }
        long num = get() - '0';
        for (;;) {
            if (!isDecimalDigit(peek(0))) {
                return num;
            }
            num = num * 10 + (get() - '0');
            if (num >= 0xFFFF_FFFFL) {
                throw error(Messages.Key.RegExpInvalidQuantifier);
            }
        }
    }

    private int readOctalEscapeSequence() {
        int num = get() - '0';
        if (isOctalDigit(peek(0))) {
            num = num * 8 + (get() - '0');
            if (num <= 037) {
                if (isOctalDigit(peek(0))) {
                    num = num * 8 + (get() - '0');
                }
            }
        }
        assert 0 <= num && num <= 0377;
        return num;
    }

    private int readDecimalEscape() {
        int num = get() - '0';
        for (;;) {
            if (!isDecimalDigit(peek(0))) {
                break;
            }
            num = num * 10 + (get() - '0');
            if (num > BACKREF_LIMIT) {
                num = BACKREF_LIMIT;
                break;
            }
        }
        return num;
    }

    private int readHexEscapeSequence() {
        int start = pos;
        int c = hex2Digits();
        if (c < 0) {
            // invalid hex escape sequence, discard parsed characters
            reset(start);
        }
        return c;
    }

    private int readUnicodeEscapeSequence() {
        int start = pos;
        int c = hex4Digits();
        if (c < 0) {
            // invalid unicode escape sequence, discard parsed characters
            reset(start);
            return c;
        }
        if (isUnicode() && Character.isHighSurrogate((char) c)) {
            int startLow = pos;
            if (match('\\') && match('u')) {
                int d = hex4Digits();
                if (Character.isLowSurrogate((char) d)) {
                    return Character.toCodePoint((char) c, (char) d);
                }
            }
            // lone high surrogate, discard parsed characters
            reset(startLow);
        }
        return c;
    }

    private int readExtendedUnicodeEscapeSequence() {
        assert isUnicode();
        if (eof() || match('}')) {
            throw error(Messages.Key.InvalidUnicodeEscape);
        }
        int c = 0;
        for (char d; (d = get()) != '}';) {
            c = (c << 4) | hexDigit(d);
            if (!Character.isValidCodePoint(c) || eof()) {
                throw error(Messages.Key.InvalidUnicodeEscape);
            }
        }
        return c;
    }

    private int hex2Digits() {
        if (pos + 2 <= length) {
            return (hexDigit(get()) << 4) | hexDigit(get());
        }
        return -1;
    }

    private int hex4Digits() {
        if (pos + 4 <= length) {
            return (hexDigit(get()) << 12) | (hexDigit(get()) << 8) | (hexDigit(get()) << 4) | hexDigit(get());
        }
        return -1;
    }

    private String readGroupName() {
        boolean unicode = isUnicode();
        StringBuilder name = new StringBuilder();
        while (!eof()) {
            int c = get(unicode);
            if (c == '>') {
                if (name.length() == 0) {
                    break;
                }
                pos -= 1;
                return name.toString();
            }
            // Unicode escape sequence
            if (c == '\\' && match('u')) {
                if (unicode && match('{')) {
                    c = readExtendedUnicodeEscapeSequence();
                } else {
                    c = readUnicodeEscapeSequence();
                }
                if (c < 0) {
                    throw error(Messages.Key.InvalidUnicodeEscape);
                }
            }
            if (!(name.length() == 0 ? Characters.isIdentifierStart(c) : Characters.isIdentifierPart(c))) {
                break;
            }
            name.appendCodePoint(c);
        }
        throw error(Messages.Key.RegExpInvalidGroup);
    }

    /**
     * <pre>
     * UnicodePropertyValueExpression ::
     *     UnicodePropertyName = UnicodePropertyValue
     *     LoneUnicodePropertyNameOrValue
     * </pre>
     * 
     * @return the property value tuple [name, value]
     */
    private String[] readUnicodePropertyValueExpression() {
        int i = 0, sep = -1;
        for (; !eof(); ++i) {
            char c = peek(i);
            if (c == '}') {
                if (i == 0 || (sep != -1 && sep + 1 == i)) {
                    break;
                }
                String name, value;
                if (sep == -1) {
                    name = substr(i);
                    value = null;
                } else {
                    name = substr(sep);
                    mustMatch('=');
                    value = substr(i - sep - 1);
                }
                mustMatch('}');
                return new String[] { name, value };
            }
            if (c == '=') {
                if (i == 0 || sep != -1) {
                    break;
                }
                sep = i;
            } else if (!isASCIIAlphaNumericUnderscore(c)) {
                break;
            }
        }
        throw error(Messages.Key.RegExpInvalidUnicodeProperty, i + 1);
    }

    /**
     * <pre>
     * CharacterClass<span><sub>[U]</sub></span>  ::
     *     [ [LA &#x2209; {<b>^</b>}] ClassRanges<span><sub>[?U]</sub></span> ]
     *     [ <b>^</b> ClassRanges<span><sub>[?U]</sub></span> ]
     * ClassRanges<span><sub>[U]</sub></span> ::
     *     [empty]
     *     NonemptyClassRanges<span><sub>[?U]</sub></span>
     * NonemptyClassRanges<span><sub>[U]</sub></span> ::
     *     ClassAtom<span><sub>[?U]</sub></span>
     *     ClassAtom<span><sub>[?U]</sub></span> NonemptyClassRangesNoDash<span><sub>[?U]</sub></span>
     *     ClassAtom<span><sub>[?U]</sub></span> <b>-</b> ClassAtom<span><sub>[?U]</sub></span> ClassRanges<span><sub>[?U]</sub></span>
     * 
     *     <span><sub>[+U]</sub></span> ClassAtom<span><sub>[U]</sub></span> <b>-</b> ClassAtom<span><sub>[U]</sub></span> ClassRanges<span><sub>[U]</sub></span>
     *     <span><sub>[~U]</sub></span> ClassAtomInRange <b>-</b> ClassAtomInRange ClassRanges
     * NonemptyClassRangesNoDash<span><sub>[U]</sub></span> ::
     *     ClassAtom<span><sub>[?U]</sub></span>
     *     ClassAtomNoDash<span><sub>[?U]</sub></span> NonemptyClassRangesNoDash<span><sub>[?U]</sub></span>
     *     ClassAtomNoDash<span><sub>[?U]</sub></span> <b>-</b> ClassAtom<span><sub>[?U]</sub></span> ClassRanges<span><sub>[?U]</sub></span>
     * 
     *     <span><sub>[+U]</sub></span> ClassAtomNoDash<span><sub>[U]</sub></span> <b>-</b> ClassAtom<span><sub>[U]</sub></span> ClassRanges<span><sub>[U]</sub></span>
     *     <span><sub>[~U]</sub></span> ClassAtomNoDashInRange <b>-</b> ClassAtomInRange ClassRanges
     * ClassAtom<span><sub>[U]</sub></span> ::
     *     <b>-</b>
     *     ClassAtomNoDash<span><sub>[?U]</sub></span>
     * ClassAtomNoDash<span><sub>[U]</sub></span> ::
     *     SourceCharacter <b>but not one of \ or ] or -</b>
     *     <b>\</b> ClassEscape<span><sub>[?U]</sub></span>
     * ClassAtomInRange ::
     *     <b>-</b>
     *     ClassAtomNoDashInRange
     * ClassAtomNoDashInRange ::
     *     SourceCharacter <b>but not one of \ or ] or -</b>
     *     <b>\</b> ClassEscape but only if ClassEscape evaluates to a CharSet with exactly one character
     *     <b>\</b> IdentityEscape
     * ClassEscape<span><sub>[U]</sub></span> ::
     *     DecimalEscape
     *     <b>b</b>
     *     CharacterEscape<span><sub>[?U]</sub></span>
     *     CharacterClassEscape
     * 
     *     <span><sub>[+U]</sub></span> DecimalEscape
     *     <span><sub>[~U]</sub></span> DecimalEscape but only if integer value of DecimalEscape is <= NCapturingParens
     *     <b>b</b>
     *     <span><sub>[+U]</sub></span> CharacterEscape<span><sub>[U]</sub></span>
     *     <span><sub>[+U]</sub></span> CharacterClassEscape
     *     <span><sub>[~U]</sub></span> CharacterClassEscape
     *     <span><sub>[~U]</sub></span> CharacterEscape
     * </pre>
     */
    private void characterClass() {
        final StringBuilder out = this.out;
        boolean negation = match('^');

        out.append('[');
        if (negation) {
            out.append('^');
        }
        final boolean unicode = isUnicode();
        final boolean web = isWebRegularExpression();
        int startCClass = out.length();
        int rangeStartCV = 0;
        boolean inRange = false, inCCRange = false;
        charclass: for (;;) {
            if (eof()) {
                throw error(Messages.Key.RegExpUnmatchedCharacter, "[");
            }

            final int cv, c = get(unicode);
            classatom: switch (c) {
            case ']':
                if (startCClass == out.length()) {
                    // Empty character class: [] or [^]. This case can also happen for "\P{Any}". Treat "[\P{Any}]"
                    // like "[]", and "[^\P{Any}]" like "[^]".
                    // Output an empty range by using b-a.
                    out.append("b-a");
                }
                out.append(']');
                return;
            case '\\': {
                switch (peek(0)) {
                case 'd':
                case 'D':
                case 's':
                case 'S':
                case 'w':
                case 'W': {
                    // ClassEscape :: CharacterClassEscape
                    char classEscape = get();
                    if (inRange) {
                        if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidCharacterRange);
                        }
                        if (!inCCRange) {
                            // escape range character "-"
                            assert out.charAt(out.length() - 1) == '-';
                            out.setCharAt(out.length() - 1, '\\');
                            out.append('-');
                        }
                        inRange = inCCRange = false;
                    } else if (peek(0) == '-' && peek(1) != ']') {
                        if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidCharacterRange);
                        }
                        inRange = inCCRange = true;
                    }
                    appendCharacterClassEscape(classEscape, true);
                    if (inRange) {
                        out.append('\\').append(mustMatch('-'));
                    }
                    continue charclass;
                }

                case 'p':
                case 'P':
                    if (unicodeProperties) {
                        boolean inverse = get() == 'P';
                        if (!match('{')) {
                            throw error(Messages.Key.RegExpInvalidEscape, inverse ? "P" : "p");
                        }
                        String[] nameValue = readUnicodePropertyValueExpression();
                        if (inRange || (peek(0) == '-' && peek(1) != ']')) {
                            throw error(Messages.Key.RegExpInvalidCharacterRange);
                        }
                        unicodeProperty(nameValue[0], nameValue[1], inverse, true);
                        continue charclass;
                    }
                    break;

                case '-': {
                    // ClassEscape :: [+U] -
                    out.append('\\').append(get());
                    cv = '-';
                    break classatom;
                }
                case 'b':
                    // ClassEscape :: b
                    mustMatch('b');
                    out.append('\u0008');
                    cv = 0x08;
                    break classatom;
                case 'f':
                    // CharacterEscape :: ControlEscape
                    out.append('\\').append(get());
                    cv = '\f';
                    break classatom;
                case 'n':
                    // CharacterEscape :: ControlEscape
                    out.append('\\').append(get());
                    cv = '\n';
                    break classatom;
                case 'r':
                    // CharacterEscape :: ControlEscape
                    out.append('\\').append(get());
                    cv = '\r';
                    break classatom;
                case 't':
                    // CharacterEscape :: ControlEscape
                    out.append('\\').append(get());
                    cv = '\t';
                    break classatom;
                case 'v':
                    // CharacterEscape :: ControlEscape
                    mustMatch('v');
                    out.append('\u000B');
                    cv = 0x0B;
                    break classatom;
                case 'c': {
                    // CharacterEscape :: c ControlLetter
                    char cc = peek(1);
                    if ((!web || unicode) ? isASCIIAlpha(cc) : isASCIIAlphaNumericUnderscore(cc)) {
                        // extended control letters with 0-9 and _ in web-compat mode
                        out.append('\\').append(get());
                        int d = get() & 0x1F;
                        out.append(toControlLetter(d));
                        cv = d;
                    } else if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, +2, cc);
                    } else {
                        // convert invalid ControlLetter to \\
                        out.append("\\\\");
                        cv = '\\';
                    }
                    break classatom;
                }
                case 'x': {
                    // CharacterEscape :: HexEscapeSequence
                    mustMatch('x');
                    int x = readHexEscapeSequence();
                    if (x >= 0x00 && x <= 0xff) {
                        appendByteCodeUnit(x);
                        cv = x;
                    } else if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, "x");
                    } else {
                        // invalid hex escape sequence, use "x"
                        out.append('x');
                        cv = 'x';
                    }
                    break classatom;
                }
                case 'u': {
                    // CharacterEscape :: RegExpUnicodeEscapeSequence
                    mustMatch('u');
                    if (unicode && match('{')) {
                        int u = readExtendedUnicodeEscapeSequence();
                        appendCodePoint(u);
                        cv = u;
                    } else {
                        int u = readUnicodeEscapeSequence();
                        if (u >= 0) {
                            if (Character.isBmpCodePoint(u)) {
                                appendCodeUnit(u);
                            } else {
                                appendCodePoint(u);
                            }
                            cv = u;
                        } else if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidEscape, "u");
                        } else {
                            // invalid unicode escape sequence, use "u"
                            out.append('u');
                            cv = 'u';
                        }
                    }
                    break classatom;
                }

                case '0':
                    if (!isDecimalDigit(peek(1))) {
                        mustMatch('0');
                        out.append('\u0000');
                        cv = 0;
                        break classatom;
                    }
                    if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, +2, peek(1));
                    }
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7': {
                    if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, +1, peek(0));
                    }
                    int num = readOctalEscapeSequence();
                    appendByteCodeUnit(num);
                    cv = num;
                    break classatom;
                }
                case '8':
                case '9': {
                    if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, +1, peek(0));
                    }
                    char d = get();
                    appendByteCodeUnit(d);
                    cv = d;
                    break classatom;
                }
                }

                // IdentityEscape
                if (eof()) {
                    throw error(Messages.Key.RegExpTrailingSlash);
                }
                int d = get(unicode);
                if (unicode ? !isSyntaxCharacterOrSlash(d) : !web && isUnicodeIDContinue(d)) {
                    throw error(Messages.Key.RegExpInvalidEscape, new String(Character.toChars(d)));
                }
                appendIdentityEscape(d);
                cv = d;
                break classatom;
            }
            case '-':
            case '[':
            case '&':
                // need to escape these characters for Java
                out.append('\\').append((char) c);
                cv = c;
                break classatom;
            default: {
                out.appendCodePoint(c);
                cv = c;
                break classatom;
            }
            }

            if (inRange) {
                // end range
                inRange = false;
                if (inCCRange) {
                    inCCRange = false;
                    continue charclass;
                }
                if (cv < rangeStartCV) {
                    throw error(Messages.Key.RegExpInvalidCharacterRange);
                }
            } else if (peek(0) == '-' && peek(1) != ']') {
                // start range
                out.append(mustMatch('-'));
                inRange = true;
                rangeStartCV = cv;
            } else {
                // no range
            }
            continue charclass;
        }
    }

    /**
     * <pre>
     * Pattern<span><sub>[U]</sub></span> ::
     *     Disjunction<span><sub>[?U]</sub></span>
     * Disjunction<span><sub>[U]</sub></span> ::
     *     Alternative<span><sub>[?U]</sub></span>
     *     Alternative<span><sub>[?U]</sub></span> <b>|</b> Disjunction<span><sub>[?U]</sub></span>
     * Alternative<span><sub>[U]</sub></span> ::
     *     [empty]
     *     Alternative<span><sub>[?U]</sub></span> Term<span><sub>[?U]</sub></span>
     * Term<span><sub>[U]</sub></span> ::
     *     Assertion<span><sub>[?U]</sub></span>
     *     Atom<span><sub>[?U]</sub></span>
     *     Atom<span><sub>[?U]</sub></span> Quantifier
     * 
     *     <span><sub>[~U]</sub></span> ExtendedTerm
     *     <span><sub>[+U]</sub></span> Assertion<span><sub>[U]</sub></span>
     *     <span><sub>[+U]</sub></span> Atom<span><sub>[U]</sub></span>
     *     <span><sub>[+U]</sub></span> Atom<span><sub>[U]</sub></span> Quantifier
     * ExtendedTerm ::
     *     Assertion
     *     AtomNoBrace Quantifier
     *     Atom
     *     QuantifiableAssertion Quantifier
     * Assertion<span><sub>[U]</sub></span> ::
     *     <b>^</b>
     *     <b>$</b>
     *     <b>\ b</b>
     *     <b>\ B</b>
     *     <b>( ? =</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     *     <b>( ? !</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     * 
     *     <span><sub>[+U]</sub></span> <b>( ? =</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     *     <span><sub>[+U]</sub></span> <b>( ? !</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     *     <span><sub>[~U]</sub></span> QuantifiableAssertion
     * AtomNoBrace ::
     *     PatternCharacterNoBrace
     *     <b>.</b>
     *     <b>\</b> AtomEscape
     *     CharacterClass
     *     <b>(</b> Disjunction <b>)</b>
     *     <b>( ? :</b> Disjunction <b>)</b>
     * Atom<span><sub>[U]</sub></span> ::
     *     PatternCharacter
     *     <b>.</b>
     *     <b>\</b> AtomEscape<span><sub>[?U]</sub></span>
     *     CharacterClass<span><sub>[?U]</sub></span>
     *     <b>(</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     *     <b>( ? :</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     * SyntaxCharacter :: <b>one of</b>
     *     <b>^ $ \ . * + ? ( ) [ ] { } |</b>
     * PatternCharacterNoBrace ::
     *     SourceCharacter but not one of <b>^ $ \ . * + ? ( ) [ ] { } |</b>
     * PatternCharacter ::
     *     SourceCharacter but not SyntaxCharacter
     * 
     *     SourceCharacter but not one of <b>^ $ \ . * + ? ( ) [ ] |</b>
     * QuantifiableAssertion ::
     *     <b>( ? =</b> Disjunction <b>)</b>
     *     <b>( ? !</b> Disjunction <b>)</b>
     * AtomEscape<span><sub>[U]</sub></span> ::
     *     DecimalEscape
     *     CharacterEscape<span><sub>[?U]</sub></span>
     *     CharacterClassEscape
     * 
     *     <span><sub>[+U]</sub></span> DecimalEscape
     *     <span><sub>[~U]</sub></span> DecimalEscape but only if the integer value of DecimalEscape is <= NCapturingParens
     *     <span><sub>[+U]</sub></span> CharacterEscape<span><sub>[U]</sub></span>
     *     <span><sub>[+U]</sub></span> CharacterClassEscape
     *     <span><sub>[~U]</sub></span> CharacterClassEscape
     *     <span><sub>[~U]</sub></span> CharacterEscape
     * CharacterEscape<span><sub>[U]</sub></span> ::
     *     ControlEscape
     *     <b>c</b> ControlLetter
     *     HexEscapeSequence
     *     RegExpUnicodeEscapeSequence<span><sub>[?U]</sub></span>
     *     IdentityEscape<span><sub>[?U]</sub></span>
     * 
     *     <span><sub>[~U]</sub></span>LegacyOctalEscapeSequence
     * ControlEscape ::  one of
     *     <b>f n r t v</b>
     * ControlLetter :: one of
     *     <b>a b c d e f g h i j k l m n o p q r s t u v w x y z</b>
     *     <b>A B C D E F G H I J K L M N O P Q R S T U V W X Y Z</b>
     * RegExpUnicodeEscapeSequence<span><sub>[U]</sub></span> ::
     *     <span><sub>[+U]</sub></span> <b>u</b> LeadSurrogate <b>\\u</b> TrailSurrogate
     *     <b>u</b> Hex4Digits
     *     <span><sub>[+U]</sub></span> <b>u {</b> HexDigits <b>} </b>
     * LeadSurrogate ::
     *     Hex4Digits <span><sub>[match only if the CV of Hex4Digits is in the inclusive range of 0xD800 and 0xDBFF]</sub></span>
     * TrailSurrogate ::
     *     Hex4Digits <span><sub>[match only if the CV of Hex4Digits is in the inclusive range of 0xDC00 and 0xDFFF]</sub></span>
     * IdentityEscape<span><sub>[U]</sub></span> ::
     *     <span><sub>[+U]</sub></span> SyntaxCharacter
     *     <span><sub>[~U]</sub></span> SourceCharacter but not IdentifierPart
     *     <span><sub>[~U]</sub></span> &lt;ZWJ&gt;
     *     <span><sub>[~U]</sub></span> &lt;ZWNJ&gt;
     * 
     *     <span><sub>[~U]</sub></span> SourceCharacter <b>but not c</b>
     * DecimalEscape ::
     *     DecimalIntegerLiteral  [LA &#x2209; DecimalDigit]
     * CharacterClassEscape :: one of
     *     <b>d D s S w W</b>
     * </pre>
     */
    private void pattern() {
        final boolean unicode = isUnicode();
        final boolean web = isWebRegularExpression();
        final StringBuilder out = this.out;

        final class State {
            // map of valid groups
            final BitSet validGroups = new BitSet();
            // number of groups
            int groups = 0;
            // maximum back-reference found
            int backrefmax = 0;
            // back-reference limit
            int backreflimit = BACKREF_LIMIT;
            // current depths
            int depth = 0;
            // map: depth -> negative
            final BitSet negativeGroup = new BitSet();
            // map: depth -> positive
            final BitSet positiveGroup = new BitSet();
            // map: depth -> capturing
            final BitSet capturingGroup = new BitSet();
            // map: depth -> lookbehind
            final BitSet lookbehindGroup = new BitSet();
            // stack: groups
            int[] groupStack = new int[8];
            int groupStackSP = 0;
            // Map of unresolved named capturing groups.
            final Map<String, Integer> unresolvedNamedGroups = namedCapture ? new LinkedHashMap<>()
                    : Collections.emptyMap();
            boolean namedBackref = namedCapture && (!web || unicode);
            boolean namedBackrefSeen = false;

            void resetForNamedBackref() {
                // Reset all locals except 'backreflimit'.
                validGroups.clear();
                groups = 0;
                backrefmax = 0;
                depth = 0;
                negativeGroup.clear();
                positiveGroup.clear();
                capturingGroup.clear();
                lookbehindGroup.clear();
                groupStackSP = 0;
                unresolvedNamedGroups.clear();
            }

            void resetForBackrefLimit() {
                // remember correct back reference limit
                backreflimit = groups;
                assert backreflimit != BACKREF_LIMIT;
                // reset locals
                validGroups.clear();
                unresolvedNamedGroups.clear();
                groups = 0;
                backrefmax = 0;
                // assert other locals don't carry any state
                assert depth == 0;
                assert negativeGroup.isEmpty();
                assert positiveGroup.isEmpty();
                assert capturingGroup.isEmpty();
                assert lookbehindGroup.isEmpty();
                assert groupStackSP == 0;
            }
        }

        State state = new State();

        term: for (;;) {
            if (eof()) {
                if (state.depth > 0) {
                    throw error(Messages.Key.RegExpUnmatchedCharacter, "(");
                }
                if (state.backrefmax > state.groups && state.backreflimit == BACKREF_LIMIT) {
                    // discard state and restart parsing
                    resetParser();
                    state.resetForBackrefLimit();
                    continue term;
                }
                assert state.backrefmax <= state.groups;
                if (!state.unresolvedNamedGroups.isEmpty()) {
                    Map.Entry<String, Integer> unresolved = state.unresolvedNamedGroups.entrySet().iterator().next();
                    throw errorAt(Messages.Key.RegExpUnknownGroup, unresolved.getValue(), unresolved.getKey());
                }
                return;
            }

            final int c = get(unicode);

            /* Disjunction, Assertion and Atom */
            atom: switch (c) {
            case '|':
                /* Disjunction */
                out.append((char) c);
                continue term;

            case '^':
                /* Assertion */
                if (isMultiline()) {
                    out.append((char) c);
                } else {
                    out.append("\\A");
                }
                continue term;

            case '$':
                /* Assertion */
                if (isMultiline()) {
                    out.append((char) c);
                } else {
                    if (!state.lookbehindGroup.isEmpty()) {
                        error(Messages.Key.RegExpAssertionInLookbehind);
                    }
                    out.append("\\z");
                }
                continue term;

            case '\\': {
                /* Assertion, AtomEscape */
                switch (peek(0)) {
                case 'b':
                case 'B':
                    // Assertion
                    appendWordBoundary(get(), !state.lookbehindGroup.isEmpty());
                    continue term;
                case 'f':
                case 'n':
                case 'r':
                case 't':
                    // CharacterEscape :: ControlEscape
                    out.append('\\').append(get());
                    break atom;
                case 'v':
                    // CharacterEscape :: ControlEscape
                    mustMatch('v');
                    out.append('\u000B');
                    break atom;
                case 'c': {
                    // CharacterEscape :: c ControlLetter
                    if (isASCIIAlpha(peek(1))) {
                        out.append('\\').append(get()).append(toControlLetter(get()));
                    } else if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, +2, peek(1));
                    } else {
                        // convert invalid ControlLetter to \
                        out.append("\\\\");
                    }
                    break atom;
                }
                case 'x': {
                    // CharacterEscape :: HexEscapeSequence
                    mustMatch('x');
                    int x = readHexEscapeSequence();
                    if (x >= 0x00 && x <= 0xff) {
                        appendByteCodeUnit(x);
                    } else if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, "x");
                    } else {
                        // invalid hex escape sequence, use "x"
                        out.append('x');
                    }
                    break atom;
                }
                case 'u': {
                    // CharacterEscape :: RegExpUnicodeEscapeSequence
                    mustMatch('u');
                    if (unicode && match('{')) {
                        int u = readExtendedUnicodeEscapeSequence();
                        appendCodePoint(u);
                    } else {
                        int u = readUnicodeEscapeSequence();
                        if (u >= 0) {
                            if (Character.isBmpCodePoint(u)) {
                                appendCodeUnit(u);
                            } else {
                                appendCodePoint(u);
                            }
                        } else if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidEscape, "u");
                        } else {
                            // invalid unicode escape sequence, use "u"
                            out.append('u');
                        }
                    }
                    break atom;
                }

                case 'd':
                case 'D':
                case 'w':
                case 'W':
                case 's':
                case 'S':
                    // CharacterClassEscape
                    appendCharacterClassEscape(get(), false);
                    break atom;

                case '0':
                    // "\0" or octal sequence
                    if ((!web || unicode) && isDecimalDigit(peek(1))) {
                        throw error(Messages.Key.RegExpInvalidEscape, +2, peek(1));
                    }
                    appendByteCodeUnit(readOctalEscapeSequence());
                    break atom;

                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9': {
                    // DecimalEscape - back-reference or invalid escape
                    int start = pos;
                    int num = readDecimalEscape();
                    if (num > state.backreflimit) {
                        // invalid backreference -> roll back to start of decimal escape
                        reset(start);
                        if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidEscape, +1, peek(0));
                        }
                        if (peek(0) < '8') {
                            // case 1: octal escape sequence
                            appendByteCodeUnit(readOctalEscapeSequence());
                        } else {
                            // case 2 (\8 or \9): invalid octal escape sequence
                            appendByteCodeUnit(get());
                        }
                    } else {
                        if (num > state.backrefmax) {
                            state.backrefmax = num;
                        }
                        if (state.lookbehindGroup.isEmpty()) {
                            if (num <= state.groups && state.validGroups.get(num)) {
                                out.append('\\').append(num);
                            } else {
                                // omit forward reference or backward reference into capturing group
                                // from negative lookahead
                                out.append("(?:)");
                            }
                        } else {
                            // Joni doesn't support back-references in lookbehind groups.
                            throw error(Messages.Key.RegExpBackreferenceInLookbehind);
                        }
                    }
                    break atom;
                }

                case 'k':
                    if (namedCapture) {
                        state.namedBackrefSeen = true;
                        if (!state.namedBackref) {
                            break;
                        }
                        int start = pos;
                        mustMatch('k');
                        mustMatch('<');
                        String name = readGroupName();
                        mustMatch('>');
                        int num = namedGroups.getOrDefault(name, -1);
                        if (num < 0) {
                            state.unresolvedNamedGroups.putIfAbsent(name, start);
                            // omit forward reference
                            out.append("(?:)");
                        } else {
                            assert 0 < num && num <= state.groups;
                            if (state.lookbehindGroup.isEmpty()) {
                                if (state.validGroups.get(num)) {
                                    out.append('\\').append(num);
                                } else {
                                    // omit backward reference into capturing group from negative lookahead
                                    out.append("(?:)");
                                }
                            } else {
                                // Joni doesn't support back-references in lookbehind groups.
                                throw error(Messages.Key.RegExpBackreferenceInLookbehind);
                            }
                        }
                        break atom;
                    }
                    break;

                case 'p':
                case 'P':
                    if (unicodeProperties) {
                        boolean inverse = get() == 'P';
                        if (!match('{')) {
                            throw error(Messages.Key.RegExpInvalidEscape, inverse ? "P" : "p");
                        }
                        String[] nameValue = readUnicodePropertyValueExpression();
                        unicodeProperty(nameValue[0], nameValue[1], inverse, false);
                        break atom;
                    }
                    break;
                }

                // CharacterEscape :: IdentityEscape
                if (eof()) {
                    throw error(Messages.Key.RegExpTrailingSlash);
                }
                int d = get(unicode);
                if (unicode ? !isSyntaxCharacterOrSlash(d) : !web && isUnicodeIDContinue(d)) {
                    throw error(Messages.Key.RegExpInvalidEscape, new String(Character.toChars(d)));
                }
                appendIdentityEscape(d);
                break atom;
            }

            case '(': {
                boolean negative = false, positive = false, capturing = false, behind = false;
                if (match('?')) {
                    if (eof()) {
                        throw error(Messages.Key.RegExpUnexpectedCharacter, "?");
                    }
                    char d = get();
                    switch (d) {
                    case '!':
                        negative = true;
                        out.append("(?!");
                        break;
                    case '=':
                        positive = true;
                        out.append("(?=");
                        break;
                    case ':':
                        // non-capturing
                        out.append("(?:");
                        break;
                    case '<':
                        if (lookBehind) {
                            if (match('!')) {
                                negative = true;
                                behind = true;
                                out.append("(?<!");
                                break;
                            }
                            if (match('=')) {
                                positive = true;
                                behind = true;
                                out.append("(?<=");
                                break;
                            }
                        }
                        if (namedCapture) {
                            String name = readGroupName();
                            mustMatch('>');
                            if (!state.namedBackref) {
                                state.namedBackref = true;
                                if (state.namedBackrefSeen) {
                                    // We have seen the named-capturing group reference syntax "\k<" before entering
                                    // named-capturing mode; throw away the current state and restart parsing.
                                    resetParser();
                                    state.resetForNamedBackref();
                                    continue term;
                                }
                            }
                            if (namedGroups.containsKey(name)) {
                                throw error(Messages.Key.RegExpDuplicateGroup, name);
                            }
                            namedGroups.put(name, state.groups + 1);
                            state.unresolvedNamedGroups.remove(name);
                            capturing = true;
                            out.append('(');
                            break;
                        }
                        // fall-through
                    default:
                        throw error(Messages.Key.RegExpUnexpectedCharacter, String.valueOf(d));
                    }
                } else {
                    capturing = true;
                    out.append('(');
                }
                if (!state.lookbehindGroup.isEmpty() && (capturing || negative || positive)) {
                    // Joni doesn't support nested capturing groups in lookbehind contexts.
                    throw error(Messages.Key.RegExpCaptureInLookbehind);
                }
                state.depth += 1;
                if (capturing) {
                    state.groups += 1;
                    state.capturingGroup.set(state.depth);
                } else if (negative) {
                    state.negativeGroup.set(state.depth);
                } else if (positive) {
                    state.positiveGroup.set(state.depth);
                }
                if (behind) {
                    state.lookbehindGroup.set(state.depth);
                }
                if (capturing || negative) {
                    if (state.groupStackSP == state.groupStack.length) {
                        state.groupStack = Arrays.copyOf(state.groupStack, state.groupStackSP << 1);
                    }
                    state.groupStack[state.groupStackSP++] = state.groups;
                }
                if (state.depth >= DEPTH_LIMIT || state.groups >= BACKREF_LIMIT) {
                    throw error(Messages.Key.RegExpPatternTooComplex);
                }
                continue term;
            }

            case ')': {
                out.append(')');
                if (state.depth == 0) {
                    throw error(Messages.Key.RegExpUnmatchedCharacter, ")");
                }
                boolean lookaround = false, lookbehind = false;
                if (state.capturingGroup.get(state.depth)) {
                    state.capturingGroup.clear(state.depth);
                    // update group information after parsing ")"
                    int g = state.groupStack[--state.groupStackSP];
                    state.validGroups.set(g);
                    if (!state.negativeGroup.isEmpty()) {
                        negativeLAGroups.set(g);
                    }
                } else if (state.negativeGroup.get(state.depth)) {
                    lookaround = true;
                    lookbehind = state.lookbehindGroup.get(state.depth);
                    state.negativeGroup.clear(state.depth);
                    state.lookbehindGroup.clear(state.depth);
                    // invalidate all capturing groups created within the negative lookaround
                    int g = state.groupStack[--state.groupStackSP];
                    for (int v = state.groups; v != g; --v) {
                        state.validGroups.clear(v);
                    }
                } else if (state.positiveGroup.get(state.depth)) {
                    lookaround = true;
                    lookbehind = state.lookbehindGroup.get(state.depth);
                    state.positiveGroup.clear(state.depth);
                    state.lookbehindGroup.clear(state.depth);
                }
                state.depth -= 1;
                if ((lookaround && (!web || unicode)) || lookbehind) {
                    continue term;
                }
                break atom;
            }

            case '[': {
                // CharacterClass
                characterClass();
                break atom;
            }

            case '*':
            case '+':
            case '?':
                // quantifier without applicable atom
                throw error(Messages.Key.RegExpInvalidQuantifier);

            case '{': {
                if (quantifier((char) c, !state.lookbehindGroup.isEmpty())) {
                    // quantifier without applicable atom
                    throw error(Messages.Key.RegExpInvalidQuantifier);
                }
                // fall-through
            }
            case ']':
            case '}':
                if (unicode || !web) {
                    throw error(Messages.Key.RegExpUnexpectedCharacter, String.valueOf((char) c));
                }
                out.append('\\').append((char) c);
                break atom;

            case '.':
                if (isDotAll()) {
                    out.append(dotAll);
                } else {
                    out.append('.');
                }
                break atom;

            default: {
                out.appendCodePoint(c);
                break atom;
            }
            }

            /* Quantifier (optional) */
            switch (peek(0)) {
            case '*':
            case '+':
            case '?':
            case '{':
                if (!quantifier(get(), !state.lookbehindGroup.isEmpty())) {
                    if (unicode || !web) {
                        throw error(Messages.Key.RegExpUnexpectedCharacter, "{");
                    }
                    reset(pos - 1);
                }
            }

            continue term;
        }
    }

    /**
     * <pre>
     * Quantifier ::
     *     QuantifierPrefix
     *     QuantifierPrefix <b>?</b>
     * QuantifierPrefix ::
     *     <b>*</b>
     *     <b>+</b>
     *     <b>?</b>
     *     <b>{</b> DecimalDigits <b>}</b>
     *     <b>{</b> DecimalDigits <b>, }</b>
     *     <b>{</b> DecimalDigits <b>,</b> DecimalDigits <b>}</b>
     * </pre>
     * 
     * @param c
     *            the start character of the quantifier
     * @param inLookBehind
     *            {@code true} if in look-behind group
     * @return {@code true} if the input could be parsed as a quantifier
     */
    private boolean quantifier(char c, boolean inLookBehind) {
        StringBuilder out = this.out;

        // Greedy/Reluctant quantifiers
        quantifier: switch (c) {
        case '*':
        case '+':
        case '?':
            if (inLookBehind) {
                throw error(Messages.Key.RegExpInvalidQuantifier);
            }
            out.append(c);
            break quantifier;
        case '{': {
            int start = pos;
            long min = decimal();
            if (min < 0) {
                reset(start);
                return false;
            }
            boolean comma;
            long max = -1;
            if ((comma = match(',')) && peek(0) != '}') {
                max = decimal();
                if (max < 0) {
                    reset(start);
                    return false;
                }
            }
            if (!match('}')) {
                reset(start);
                return false;
            }
            if (max != -1 && min > max) {
                throw error(Messages.Key.RegExpInvalidQuantifier);
            }
            if (inLookBehind && comma && min != max) {
                throw error(Messages.Key.RegExpInvalidQuantifier);
            }

            // output result
            out.append('{').append((int) Math.min(min, Config.MAX_REPEAT_NUM));
            if (comma) {
                if (max != -1) {
                    out.append(',').append((int) Math.min(max, Config.MAX_REPEAT_NUM));
                } else {
                    out.append(',');
                }
            }
            out.append('}');
            break quantifier;
        }
        default:
            throw new AssertionError("unreachable");
        }

        // Reluctant quantifiers
        if (match('?')) {
            out.append('?');
        }
        // Possessive quantifiers
        else if (possessive && match('+')) {
            out.append('+');
        }

        return true;
    }

    private void appendWordBoundary(char c, boolean inLookBehind) {
        if (isIgnoreCase() && isUnicode()) {
            if (inLookBehind) {
                error(Messages.Key.RegExpAssertionInLookbehind);
            }
            if (c == 'b') {
                out.append(assertion_bui);
            } else {
                assert c == 'B';
                out.append(assertion_Bui);
            }
        } else {
            out.append('\\').append(c);
        }
    }

    private void appendCharacterClassEscape(char c, boolean cclass) {
        if ((c == 'w' || c == 'W') && isIgnoreCase() && isUnicode()) {
            if (!cclass) {
                out.append('[');
            }
            if (c == 'w') {
                out.append(characterClass_wui);
            } else {
                out.append(characterClass_Wui);
            }
            if (!cclass) {
                out.append(']');
            }
        } else {
            char mod = (char) ('P' | (c & 0x20));
            String propertyName = getCharacterClassPropertyName(c);
            out.append('\\').append(mod).append('{').append(propertyName).append('}');
        }
    }

    private static String getCharacterClassPropertyName(char c) {
        switch (c) {
        case 'd':
        case 'D':
            return "Digit";
        case 'w':
        case 'W':
            return "Word";
        case 's':
        case 'S':
            return "Space";
        default:
            throw new AssertionError("unreachable");
        }
    }

    private void unicodeProperty(String name, String value, boolean inverse, boolean cclass) {
        if (value == null) {
            // UnicodePropertyValueExpression :: LoneUnicodePropertyNameOrValue
            int nameOffset = -name.length();
            if (UnicodeData.EnumProperty.General_Category.isValue(name)) {
                appendUnicodeProperty(UnicodeData.EnumProperty.General_Category, name, inverse, cclass);
            } else {
                switch (name) {
                case "Any":
                    appendRange(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT, inverse, cclass);
                    break;
                case "ASCII":
                    appendRange(0, 0x7f, inverse, cclass);
                    break;
                case "Assigned":
                    appendUnicodeProperty(UnicodeData.EnumProperty.General_Category, "Cn", !inverse, cclass);
                    break;
                default: {
                    UnicodeData.Property unaliased = unicodeMatchProperty(name, nameOffset);
                    if (unaliased instanceof UnicodeData.BinaryProperty) {
                        appendUnicodeProperty((UnicodeData.BinaryProperty) unaliased, inverse, cclass);
                    } else {
                        assert unaliased instanceof UnicodeData.EnumProperty;
                        throw error(Messages.Key.RegExpMissingUnicodePropertyValue, nameOffset, unaliased.getName());
                    }
                    break;
                }
                }
            }
        } else {
            // UnicodePropertyValueExpression :: UnicodePropertyName=UnicodePropertyValue
            int nameOffset = -(name.length() + 1 + value.length());
            int valueOffset = -value.length();
            UnicodeData.Property unaliasedName = unicodeMatchProperty(name, nameOffset);
            if (unaliasedName instanceof UnicodeData.BinaryProperty) {
                throw error(Messages.Key.RegExpInvalidUnicodePropertyValue, valueOffset, unaliasedName.getName(),
                        value);
            }
            assert unaliasedName instanceof UnicodeData.EnumProperty;
            UnicodeData.EnumProperty enumProperty = (UnicodeData.EnumProperty) unaliasedName;
            String unaliasedValue = unicodeMatchPropertyValue(enumProperty, value, valueOffset);
            appendUnicodeProperty(enumProperty, unaliasedValue, inverse, cclass);
        }
    }

    private void appendRange(int start, int end, boolean inverse, boolean cclass) {
        if (!cclass) {
            out.append('[');
            if (inverse) {
                out.append('^');
            }
            appendCodePoint(start);
            out.append('-');
            appendCodePoint(end);
            out.append(']');
        } else if (!inverse) {
            appendCodePoint(start);
            out.append('-');
            appendCodePoint(end);
        } else {
            if (start > Character.MIN_CODE_POINT) {
                appendCodePoint(Character.MIN_CODE_POINT);
                out.append('-');
                appendCodePoint(start - 1);
            }
            if (end < Character.MAX_CODE_POINT) {
                appendCodePoint(end + 1);
                out.append('-');
                appendCodePoint(Character.MAX_CODE_POINT);
            }
        }
    }

    private void appendUnicodeProperty(UnicodeData.BinaryProperty property, boolean inverse, boolean cclass) {
        // Place in character class to enforce case-folding in Joni.
        // TODO: Report Joni bug (`/\p{Lu}/i.match("a")` returns nil in JRuby, but MatchData in Ruby)
        if (!cclass) {
            out.append('[');
        }
        out.append('\\').append(inverse ? 'P' : 'p').append('{').append(property.getName()).append('}');
        if (!cclass) {
            out.append(']');
        }
    }

    private void appendUnicodeProperty(UnicodeData.EnumProperty property, String value, boolean inverse,
            boolean cclass) {
        // Place in character class to enforce case-folding in Joni.
        if (!cclass) {
            out.append('[');
        }
        out.append('\\').append(inverse ? 'P' : 'p').append('{').append(property.getName()).append('=').append(value)
                .append('}');
        if (!cclass) {
            out.append(']');
        }
    }

    /**
     * Runtime Semantics: UnicodeMatchProperty ( p )
     * 
     * @param property
     *            the unicode property name
     * @param offset
     *            the start offset
     * @return the Unicode property
     */
    private UnicodeData.Property unicodeMatchProperty(String property, int offset) {
        UnicodeData.Property p = UnicodeData.Property.from(property);
        if (p != null) {
            return p;
        }
        throw error(Messages.Key.RegExpInvalidUnicodeCategory, offset, property);
    }

    /**
     * Runtime Semantics: UnicodeMatchPropertyValue ( p, v )
     * 
     * @param property
     *            the property name
     * @param value
     *            the property value
     * @param offset
     *            the start offset
     * @return the unaliased property value
     */
    private String unicodeMatchPropertyValue(UnicodeData.EnumProperty property, String value, int offset) {
        if (property.isValue(value)) {
            return value;
        }
        throw error(Messages.Key.RegExpInvalidUnicodePropertyValue, offset, property.getName(), value);
    }

    private void appendIdentityEscape(int ch) {
        if (isASCIIAlpha(ch)) {
            // Don't escape ASCII alpha characters to avoid turning them into flags.
            out.append((char) ch);
        } else if (ch <= 0x7f) {
            // Apply identity escape for other ASCII characters.
            out.append('\\').append((char) ch);
        } else if (ch < 0x100) {
            appendByteCodeUnit(ch);
        } else if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            appendCodeUnit(ch);
        } else {
            appendCodePoint(ch);
        }
    }

    private void appendByteCodeUnit(int codeUnit) {
        assert codeUnit >>> 8 == 0;
        if (isUnicode()) {
            out.append("\\u00").append(toHexDigit(codeUnit >> 4)).append(toHexDigit(codeUnit >> 0));
        } else {
            out.append("\\x00").append("\\x").append(toHexDigit(codeUnit >> 4)).append(toHexDigit(codeUnit >> 0));
        }
    }

    private void appendCodeUnit(int codeUnit) {
        assert Character.isBmpCodePoint(codeUnit);
        out.append("\\u").append(toHexDigit(codeUnit >> 12)).append(toHexDigit(codeUnit >> 8))
                .append(toHexDigit(codeUnit >> 4)).append(toHexDigit(codeUnit >> 0));
    }

    private void appendCodePoint(int codePoint) {
        out.append("\\x{").append(Integer.toHexString(codePoint)).append('}');
    }

    private static char toControlLetter(int c) {
        return (char) ('A' - 1 + (c & 0x1f));
    }

    private static char toHexDigit(int c) {
        return HEXDIGITS[c & 0xf];
    }

    /**
     * <pre>
     * SyntaxCharacter :: <b>one of</b>
     *     <b>^ $ \ . * + ? ( ) [ ] { } |</b>
     * </pre>
     * 
     * @param c
     *            the character to inspect
     * @return {@code true} if the character is a syntax character or a forward slash ({@code /})
     */
    private static boolean isSyntaxCharacterOrSlash(int c) {
        switch (c) {
        case '^':
        case '$':
        case '\\':
        case '.':
        case '*':
        case '+':
        case '?':
        case '(':
        case ')':
        case '[':
        case ']':
        case '{':
        case '}':
        case '|':
        case '/':
            return true;
        default:
            return false;
        }
    }

    /**
     * <pre>
     * SyntaxCharacter :: <b>one of</b>
     *     <b>^ $ \ . * + ? ( ) [ ] { } |</b>
     * </pre>
     * 
     * @param c
     *            the character to inspect
     * @return {@code true} if the character is a syntax character
     */
    private static boolean isSyntaxCharacter(int c) {
        switch (c) {
        case '^':
        case '$':
        case '\\':
        case '.':
        case '*':
        case '+':
        case '?':
        case '(':
        case ')':
        case '[':
        case ']':
        case '{':
        case '}':
        case '|':
            return true;
        default:
            return false;
        }
    }
}
