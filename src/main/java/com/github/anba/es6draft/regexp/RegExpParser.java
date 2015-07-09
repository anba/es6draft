/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import static com.github.anba.es6draft.parser.Characters.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.regex.Pattern;

import org.joni.Config;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * <h1>21 Text Processing</h1><br>
 * <h2>21.2 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>21.2.1 Patterns
 * <li>21.2.2 Pattern Semantics
 * </ul>
 */
public final class RegExpParser {
    // TODO: Remove JDK mode.
    private static final boolean USE_JONI = true;
    private static final int BACKREF_LIMIT = 0xFFFF;
    private static final int DEPTH_LIMIT = 0xFFFF;
    private static final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' };

    // JSC treats /\8/ the same as /\\8/ whereas other engines treat it as /8/, follow the majority
    // and use /8/, too.
    private static final boolean INVALID_OCTAL_WITH_BACKSLASH = false;

    // CharacterClass \s
    private static final String characterClass_s = "[ \\t\\n\\u000B\\f\\r\\u2028\\u2029\\u00A0\\uFEFF\\p{gc=Zs}]";
    // CharacterClass \S
    private static final String characterClass_S = "[^ \\t\\n\\u000B\\f\\r\\u2028\\u2029\\u00A0\\uFEFF\\p{gc=Zs}]";
    // CharacterClass \w ~ [a-zA-Z0-9_] and LATIN SMALL LETTER LONG S and KELVIN SIGN
    private static final String characterClass_wu = "a-zA-Z0-9_\\u017f\\u212a";
    // CharacterClass \W ~ [\u0000-\u002F\u003A-\u0040\u005B-\u005E\u0060\u007B-\x{10ffff}]
    private static final String characterClass_Wu = "\\u0000-\\u002F\\u003A-\\u0040\\u005B-\\u005E\\u0060\\u007B-\\x{10ffff}\\x53\\x73\\x4B\\x6B";
    // [] => matches nothing
    private static final String emptyCharacterClass = "(?:\\Z )";
    // [^] => matches everything
    private static final String emptyNegCharacterClass = "(?s:.)";
    // Character class for . (dot)
    private static final String characterClass_Dot = "[^\\n\\r\\u2028\\u2029]";

    private final String source;
    private final int length;
    private final String sourceFile;
    private final int sourceLine;
    private final int sourceColumn;
    private final int flags;
    private final boolean webRegExp;
    private final boolean joni;
    private final StringBuilder out;

    private int pos = 0;

    // map of groups created within negative lookahead
    private BitSet negativeLAGroups = new BitSet();

    private RegExpParser(String source, String flags, String sourceFile, int sourceLine,
            int sourceColumn, boolean webRegExp) {
        this.source = source;
        this.length = source.length();
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.sourceColumn = sourceColumn;
        // Call after source information was set
        this.flags = toFlags(flags);
        this.webRegExp = webRegExp;
        this.joni = USE_JONI;
        this.out = new StringBuilder(length);
    }

    public static RegExpMatcher parse(String pattern, String flags, String sourceFile,
            int sourceLine, int sourceColumn, boolean webRegExp) throws ParserException {
        RegExpParser parser = new RegExpParser(pattern, flags, sourceFile, sourceLine,
                sourceColumn, webRegExp);
        parser.pattern();

        if (parser.useJoniRegExp()) {
            return new JoniRegExpMatcher(parser.out.toString(), parser.flags,
                    parser.negativeLAGroups);
        }
        return new JDKRegExpMatcher(parser.out.toString(), parser.flags, parser.negativeLAGroups);
    }

    public static void syntaxParse(String pattern, String flags, String sourceFile, int sourceLine,
            int sourceColumn, boolean webRegExp) throws ParserException {
        RegExpParser parser = new RegExpParser(pattern, flags, sourceFile, sourceLine,
                sourceColumn, webRegExp);
        parser.pattern();
    }

    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn
                + pos, messageKey, args);
    }

    private ParserException error(Messages.Key messageKey, int offset, char offending) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn
                + pos + offset, messageKey, String.valueOf(offending));
    }

    private int toFlags(String flags) {
        // flags :: g | i | m | u | y
        final int global = 0b00001, ignoreCase = 0b00010, multiline = 0b00100, unicode = 0b01000, sticky = 0b10000;
        int mask = 0b00000;
        for (int i = 0, len = flags.length(); i < len; ++i) {
            char c = flags.charAt(i);
            int flag;
            String name;
            switch (c) {
            case 'g':
                flag = global;
                name = "global";
                break;
            case 'i':
                flag = ignoreCase;
                name = "ignoreCase";
                break;
            case 'm':
                flag = multiline;
                name = "multiline";
                break;
            case 'u':
                flag = unicode;
                name = "unicode";
                break;
            case 'y':
                flag = sticky;
                name = "sticky";
                break;
            default:
                throw error(Messages.Key.RegExpInvalidFlag, String.valueOf(c));
            }
            if ((mask & flag) == 0) {
                mask |= flag;
            } else {
                throw error(Messages.Key.RegExpDuplicateFlag, name);
            }
        }

        int iflags = 0;
        if ((mask & ignoreCase) != 0) {
            iflags |= Pattern.CASE_INSENSITIVE;
        }
        if ((mask & unicode) != 0) {
            iflags |= Pattern.UNICODE_CASE;
        }
        if ((mask & multiline) != 0) {
            iflags |= Pattern.MULTILINE;
        }
        return iflags;
    }

    private boolean useJoniRegExp() {
        return joni;
    }

    /**
     * Returns {@code true} if regular expression patterns support the
     * {@link CompatibilityOption#WebRegularExpressions} extension.
     * 
     * @return {@code true} if patterns are in web-compatibility mode
     */
    private boolean isWebRegularExpression() {
        return webRegExp;
    }

    /**
     * Whether or not octal escapes are supported.
     * 
     * @return {@code true} if octal escapes are supported
     */
    private boolean octalEscape() {
        return !joni;
    }

    /**
     * Whether or not hexadecimal escapes need to be aligned to two bytes.
     * 
     * @return {@code true} if hexadecimal escapes need be aligned
     */
    private boolean alignBytes() {
        return joni;
    }

    /**
     * Whether or not empty character classes (i.e. `[]` and `[^]`) are supported.
     * 
     * @return {@code true} if empty character classes are supported
     */
    private boolean emptyCharacterClass() {
        // works in Joni if OP2_OPTION_ECMASCRIPT is enabled, but that breaks nested repeats...
        // return joni;
        return false;
    }

    /**
     * Whether or not empty \S and \s character classes are supported.
     * 
     * @return {@code true} if \S and \s character classes are supported
     */
    private boolean spaceCharacterClass() {
        return joni;
    }

    /**
     * Whether or not character property classes are used to represent character class escapes.
     * 
     * @return {@code true} if character property classes are supported
     */
    private boolean characterProperty() {
        return joni;
    }

    /**
     * Whether or not . character class is compliant.
     * 
     * @return {@code true} if the {@code .} character class is compliant
     */
    private boolean dotCharacterClass() {
        return joni;
    }

    /**
     * Whether or not case insensitive patterns need to be rewritten.
     * 
     * @return {@code true} if case insensitive patterns are supported
     */
    private boolean caseInsensitive() {
        return joni;
    }

    /**
     * Whether or not the special case folding for I is enabled.
     * 
     * @return {@code true} if the case folding for I is enabled
     */
    private boolean caseFoldedI() {
        return !joni;
    }

    /**
     * Returns {@code true} if unicode characters are natively supported.
     * 
     * @return {@code true} if unicode characters are natively supported
     */
    private boolean unicodeCharacters() {
        return joni;
    }

    /**
     * Returns {@code true} if unicode mode is disabled for \b and \B assertions.
     * 
     * @return {@code true} if unicode mode is disabled for \b and \B assertions
     */
    private boolean disableUnicodeInAssertion() {
        return !joni;
    }

    /**
     * Maximum supported repeat.
     * 
     * @return the maximum number of repeats
     */
    private int repeatMaximum() {
        // clamp at max-int to avoid overflow in Java
        return joni ? Config.MAX_REPEAT_NUM : Integer.MAX_VALUE;
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

    private boolean match(char c) {
        if (c == peek(0)) {
            get();
            return true;
        }
        return false;
    }

    private char mustMatch(char c) {
        if (c != get()) {
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
            return (hexDigit(get()) << 12) | (hexDigit(get()) << 8) | (hexDigit(get()) << 4)
                    | hexDigit(get());
        }
        return -1;
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
     * 
     * @param negation
     *            flag to mark negative character classes
     */
    private void characterClass(boolean negation) {
        final boolean ignoreCase = isIgnoreCase();
        final boolean unicode = isUnicode();
        final boolean web = isWebRegularExpression();

        final StringBuilder out = this.out;
        final int startLength = out.length();
        int rangeStartCV = 0, rangeStartPos = 0;
        boolean inrange = false;
        boolean asciiI = false, iWithDot = false, dotlessI = false;
        // additional classes need to go to the end of the character-class if in negated class
        StringBuilder additionalClasses = null;
        charclass: for (;;) {
            if (eof()) {
                throw error(Messages.Key.RegExpUnmatchedCharacter, "[");
            }

            final int cv, c = get(unicode), outStart = out.length();
            classatom: switch (c) {
            case ']':
                if (additionalClasses != null) {
                    if (out.length() != startLength) {
                        out.append("&&");
                    }
                    out.append(additionalClasses);
                }
                if (caseFoldedI()) {
                    if (asciiI) {
                        if (!iWithDot && !dotlessI) {
                            excludeRange('\u0130', '\u0131', negation);
                        } else if (!iWithDot) {
                            excludeRange('\u0130', '\u0130', negation);
                        } else if (!dotlessI) {
                            excludeRange('\u0131', '\u0131', negation);
                        }
                    }
                    if (iWithDot && dotlessI) {
                        if (!asciiI) {
                            excludeChar('I', '\u0131', negation);
                        }
                    } else if (iWithDot) {
                        if (!asciiI) {
                            excludeChar('I', '\u0130', negation);
                        }
                    } else if (dotlessI) {
                        excludeRange('\u0130', '\u0130', negation);
                        if (!asciiI) {
                            excludeChar('I', '\u0131', negation);
                        }
                    }
                }
                return;
            case '\\': {
                switch (peek(0)) {
                case 'd':
                case 'D':
                case 's':
                case 'S':
                case 'w':
                case 'W': {
                    // class escape (cannot start/end range)
                    if (inrange) {
                        if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidCharacterRange);
                        }
                        // escape range character "-"
                        assert out.charAt(out.length() - 1) == '-';
                        out.setCharAt(out.length() - 1, '\\');
                        out.append('-');
                        inrange = false;
                    }
                    char classEscape = get();
                    if ((!web || unicode) && peek(0) == '-' && peek(1) != ']') {
                        throw error(Messages.Key.RegExpInvalidCharacterRange);
                    }
                    if (unicode && ignoreCase) {
                        if (classEscape == 'w') {
                            asciiI = true;
                        } else if (classEscape == 'W') {
                            iWithDot = true;
                            dotlessI = true;
                        }
                    }
                    String escape = appendCharacterClassEscape(classEscape, true, negation);
                    if (escape != null) {
                        if (additionalClasses == null) {
                            additionalClasses = new StringBuilder();
                        } else {
                            additionalClasses.append("&&");
                        }
                        additionalClasses.append(escape);
                    }
                    continue charclass;
                }

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
                        appendHexEscapeSequence(x, true);
                        cv = x;
                    } else if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, "x");
                    } else {
                        // invalid hex escape sequence, use "x"
                        out.append("x");
                        cv = 'x';
                    }
                    break classatom;
                }
                case 'u': {
                    // CharacterEscape :: RegExpUnicodeEscapeSequence
                    mustMatch('u');
                    if (unicode && match('{')) {
                        int u = readExtendedUnicodeEscapeSequence();
                        appendExtendedUnicodeEscapeSequence(u, true);
                        cv = u;
                    } else {
                        int u = readUnicodeEscapeSequence();
                        if (u >= 0) {
                            appendUnicodeEscapeSequence(u, true);
                            cv = u;
                        } else if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidEscape, "u");
                        } else {
                            // invalid unicode escape sequence, use "u"
                            out.append("u");
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
                    appendOctalEscapeSequence(num, true);
                    cv = num;
                    break classatom;
                }
                case '8':
                case '9': {
                    if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, +1, peek(0));
                    }
                    char d = get();
                    if (INVALID_OCTAL_WITH_BACKSLASH) {
                        out.append("\\\\");
                    }
                    out.append(d);
                    cv = d;
                    break classatom;
                }

                default: {
                    if (eof()) {
                        throw error(Messages.Key.RegExpTrailingSlash);
                    }
                    int d = get(unicode);
                    if (unicode ? !isSyntaxCharacter(d) : !web && isUnicodeIDContinue(d)) {
                        throw error(Messages.Key.RegExpInvalidEscape,
                                new String(Character.toChars(d)));
                    }
                    appendIdentityEscape(d, true);
                    cv = d;
                    break classatom;
                }
                }
            }
            case '-':
            case '[':
            case '&':
                // need to escape these characters for Java
                out.append('\\').append((char) c);
                cv = c;
                break classatom;
            default: {
                // inlined: appendPatternCharacter(c)
                if (ignoreCase) {
                    if (unicode) {
                        appendCaseInsensitiveUnicode(c, true);
                    } else {
                        appendCaseInsensitive(c, true);
                    }
                } else {
                    out.appendCodePoint(c);
                }
                cv = c;
                break classatom;
            }
            }

            if (unicode && ignoreCase) {
                if (cv == 'i' || cv == 'I') {
                    asciiI = true;
                } else if (cv == '\u0130') {
                    iWithDot = true;
                } else if (cv == '\u0131') {
                    dotlessI = true;
                }
            }

            if (inrange) {
                // end range
                inrange = false;
                if (cv < rangeStartCV) {
                    throw error(Messages.Key.RegExpInvalidCharacterRange);
                }
                if ((rangeStartCV > 0x7f || cv > 0x7f) && ignoreCase && !unicode) {
                    // replace content in output with case insensitive range
                    out.setLength(rangeStartPos);
                    appendCaseInsensitiveRange(rangeStartCV, cv);
                }
                if (ignoreCase && unicode) {
                    // replace content in output with case insensitive range
                    out.setLength(rangeStartPos);
                    appendCaseInsensitiveUnicodeRange(rangeStartCV, cv);
                    if ((rangeStartCV <= 'i' && 'i' <= cv) || (rangeStartCV <= 'I' && 'I' <= cv)) {
                        asciiI = true;
                    }
                    if (rangeStartCV <= '\u0130' && '\u0130' <= cv) {
                        iWithDot = true;
                    }
                    if (rangeStartCV <= '\u0131' && '\u0131' <= cv) {
                        dotlessI = true;
                    }
                }
            } else if (peek(0) == '-' && peek(1) != ']') {
                // start range
                out.append(mustMatch('-'));
                inrange = true;
                rangeStartCV = cv;
                rangeStartPos = outStart;
            } else {
                // no range
            }
            continue charclass;
        }
    }

    private void excludeRange(char start, char end, boolean negation) {
        if (negation) {
            intersection(start, end);
        } else {
            subtraction(start, end);
        }
    }

    private void excludeChar(char c, char keep, boolean negation) {
        if (negation) {
            intersection(c, c);
            subtraction(keep, keep);
        } else {
            subtraction(c, c);
            intersection(keep, keep);
        }
    }

    private void subtraction(char start, char end) {
        out.append("&&[^");
        appendCodeUnit(start);
        out.append('-');
        appendCodeUnit(end);
        out.append(']');
    }

    private void intersection(char start, char end) {
        out.append('[');
        appendCodeUnit(start);
        out.append('-');
        appendCodeUnit(end);
        out.append(']');
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
        final boolean ignoreCase = isIgnoreCase();
        final boolean unicode = isUnicode();
        final boolean web = isWebRegularExpression();
        final StringBuilder out = this.out;

        // map of valid groups
        BitSet validGroups = new BitSet();
        // number of groups
        int groups = 0;
        // maximum back-reference found
        int backrefmax = 0;
        // back-reference limit
        int backreflimit = BACKREF_LIMIT;
        // current depths
        int depth = 0;
        int negativedepth = 0;
        // map: depth -> negative
        BitSet negativeGroup = new BitSet();
        // map: depth -> positive
        BitSet positiveGroup = new BitSet();
        // map: depth -> capturing
        BitSet capturingGroup = new BitSet();
        // stack: groups
        int[] groupStack = new int[8];
        int groupStackSP = 0;

        term: for (;;) {
            if (eof()) {
                if (depth > 0) {
                    throw error(Messages.Key.RegExpUnmatchedCharacter, "(");
                }
                if (backrefmax > groups && backreflimit == BACKREF_LIMIT) {
                    // discard state and restart parsing
                    out.setLength(0);
                    pos = 0;
                    negativeLAGroups.clear();
                    // remember correct back reference limit
                    backreflimit = groups;
                    assert backreflimit != BACKREF_LIMIT;
                    // reset locals
                    validGroups.clear();
                    groups = 0;
                    backrefmax = 0;
                    // assert other locals don't carry any state
                    assert depth == 0;
                    assert negativedepth == 0;
                    assert negativeGroup.isEmpty();
                    assert positiveGroup.isEmpty();
                    assert capturingGroup.isEmpty();
                    assert groupStackSP == 0;
                    continue term;
                }
                assert backrefmax <= groups;
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
                    out.append("\\z");
                }
                continue term;

            case '\\': {
                /* Assertion, AtomEscape */
                switch (peek(0)) {
                case 'b':
                case 'B':
                    // Assertion
                    if (unicode && disableUnicodeInAssertion()) {
                        // Disable unicode - does not fix all spec violations, but it's better than
                        // nothing.
                        out.append("(?-u:").append('\\').append(get()).append(")");
                    } else {
                        out.append('\\').append(get());
                    }
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
                        appendHexEscapeSequence(x, false);
                    } else if (!web || unicode) {
                        throw error(Messages.Key.RegExpInvalidEscape, "x");
                    } else {
                        // invalid hex escape sequence, use "x"
                        out.append("x");
                    }
                    break atom;
                }
                case 'u': {
                    // CharacterEscape :: RegExpUnicodeEscapeSequence
                    mustMatch('u');
                    if (unicode && match('{')) {
                        int u = readExtendedUnicodeEscapeSequence();
                        appendExtendedUnicodeEscapeSequence(u, false);
                    } else {
                        int u = readUnicodeEscapeSequence();
                        if (u >= 0) {
                            appendUnicodeEscapeSequence(u, false);
                        } else if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidEscape, "u");
                        } else {
                            // invalid unicode escape sequence, use "u"
                            out.append("u");
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
                    appendCharacterClassEscape(get(), false, false);
                    break atom;

                case '0':
                    // "\0" or octal sequence
                    if ((!web || unicode) && isDecimalDigit(peek(1))) {
                        throw error(Messages.Key.RegExpInvalidEscape, +2, peek(1));
                    }
                    appendOctalEscapeSequence(readOctalEscapeSequence(), false);
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
                    if (num > backreflimit) {
                        // invalid backreference -> roll back to start of decimal escape
                        reset(start);
                        if (!web || unicode) {
                            throw error(Messages.Key.RegExpInvalidEscape, +1, peek(0));
                        }
                        if (peek(0) < '8') {
                            // case 1: octal escape sequence
                            appendOctalEscapeSequence(readOctalEscapeSequence(), false);
                        } else {
                            // case 2 (\8 or \9): invalid octal escape sequence
                            if (INVALID_OCTAL_WITH_BACKSLASH) {
                                out.append("\\\\");
                            }
                        }
                    } else {
                        if (num > backrefmax) {
                            backrefmax = num;
                        }
                        if (num <= groups && validGroups.get(num)) {
                            appendBackReference(num);
                        } else {
                            // omit forward reference or backward reference into capturing group
                            // from negative lookahead
                            out.append("(?:)");
                        }
                    }
                    break atom;
                }

                default: {
                    // CharacterEscape :: IdentityEscape
                    if (eof()) {
                        throw error(Messages.Key.RegExpTrailingSlash);
                    }
                    int d = get(unicode);
                    if (unicode ? !isSyntaxCharacter(d) : !web && isUnicodeIDContinue(d)) {
                        throw error(Messages.Key.RegExpInvalidEscape,
                                new String(Character.toChars(d)));
                    }
                    appendIdentityEscape(d, false);
                    break atom;
                }
                }
            }

            case '(': {
                boolean negative = false, positive = false, capturing = false;
                if (match('?')) {
                    // (?=X) or (?!X) or (?:X)
                    if (eof()) {
                        throw error(Messages.Key.RegExpUnexpectedCharacter, "?");
                    }
                    char d = get();
                    switch (d) {
                    case '!':
                        negative = true;
                        break;
                    case '=':
                        positive = true;
                        break;
                    case ':':
                        // non-capturing
                        break;
                    default:
                        throw error(Messages.Key.RegExpUnexpectedCharacter, String.valueOf(d));
                    }
                    out.append("(?").append(d);
                } else {
                    capturing = true;
                    out.append('(');
                }
                depth += 1;
                if (capturing) {
                    groups += 1;
                    capturingGroup.set(depth);
                } else if (negative) {
                    negativedepth += 1;
                    negativeGroup.set(depth);
                } else if (positive) {
                    positiveGroup.set(depth);
                }
                if (capturing || negative) {
                    if (groupStackSP == groupStack.length) {
                        groupStack = Arrays.copyOf(groupStack, groupStackSP << 1);
                    }
                    groupStack[groupStackSP++] = groups;
                }
                if (depth >= DEPTH_LIMIT || groups >= BACKREF_LIMIT) {
                    throw error(Messages.Key.RegExpPatternTooComplex);
                }
                continue term;
            }

            case ')': {
                out.append(')');
                if (depth == 0) {
                    throw error(Messages.Key.RegExpUnmatchedCharacter, ")");
                }
                boolean lookaround = false;
                if (capturingGroup.get(depth)) {
                    capturingGroup.clear(depth);
                    // update group information after parsing ")"
                    int g = groupStack[--groupStackSP];
                    validGroups.set(g);
                    if (negativedepth > 0) {
                        negativeLAGroups.set(g);
                    }
                } else if (negativeGroup.get(depth)) {
                    negativeGroup.clear(depth);
                    // invalidate all capturing groups created within the negative lookahead
                    int g = groupStack[--groupStackSP];
                    for (int v = groups; v != g; --v) {
                        validGroups.clear(v);
                    }
                    negativedepth -= 1;
                    lookaround = true;
                } else if (positiveGroup.get(depth)) {
                    positiveGroup.clear(depth);
                    lookaround = true;
                }
                depth -= 1;
                if (lookaround && (!web || unicode)) {
                    continue term;
                }
                break atom;
            }

            case '[': {
                // CharacterClass
                boolean negation = match('^');
                if (!match(']')) {
                    // non-empty character class
                    out.append('[');
                    if (negation) {
                        out.append('^');
                    }
                    characterClass(negation);
                    out.append(']');
                } else {
                    appendEmptyCharacterClass(negation);
                }
                break atom;
            }

            case '*':
            case '+':
            case '?':
                // quantifier without applicable atom
                throw error(Messages.Key.RegExpInvalidQuantifier);

            case '{': {
                if (quantifier((char) c)) {
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
                appendDotCharacterClass();
                break atom;

            default: {
                // inlined: appendPatternCharacter(c)
                if (ignoreCase) {
                    if (unicode) {
                        appendCaseInsensitiveUnicode(c, false);
                    } else {
                        appendCaseInsensitive(c, false);
                    }
                } else {
                    out.appendCodePoint(c);
                }
                break atom;
            }
            }

            /* Quantifier (optional) */
            switch (peek(0)) {
            case '*':
            case '+':
            case '?':
            case '{':
                if (!quantifier(get())) {
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
     * @return {@code true} if the input could be parsed as a quantifier
     */
    private boolean quantifier(char c) {
        StringBuilder out = this.out;

        // Greedy/Reluctant quantifiers
        quantifier: switch (c) {
        case '*':
        case '+':
        case '?':
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

            // output result
            out.append('{').append((int) Math.min(min, repeatMaximum()));
            if (comma) {
                if (max != -1) {
                    out.append(',').append((int) Math.min(max, repeatMaximum()));
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

        return true;
    }

    private void appendBackReference(int num) {
        if (isIgnoreCase() && !isUnicode() && !caseInsensitive()) {
            // need to enable unicode mode temporarily for jdk pattern
            out.append("(?u:\\").append(num).append(')');
        } else {
            out.append('\\').append(num);
        }
    }

    private void appendDotCharacterClass() {
        if (dotCharacterClass()) {
            out.append('.');
        } else {
            out.append(characterClass_Dot);
        }
    }

    private void appendEmptyCharacterClass(boolean negation) {
        String characterClass;
        if (emptyCharacterClass()) {
            characterClass = !negation ? "[]" : "[^]";
        } else {
            characterClass = !negation ? emptyCharacterClass : emptyNegCharacterClass;
        }
        out.append(characterClass);
    }

    private String appendCharacterClassEscape(char c, boolean cclass, boolean negCharacterClass) {
        switch (c) {
        case 'd':
        case 'D':
            appendCharacterClassEscape(c);
            return null;
        case 'w':
            if (isIgnoreCase() && isUnicode()) {
                if (!cclass) {
                    if (caseFoldedI()) {
                        out.append("(?-u:[");
                        out.append(characterClass_wu);
                        out.append("])");
                    } else {
                        out.append('[');
                        out.append(characterClass_wu);
                        out.append(']');
                    }
                } else {
                    out.append(characterClass_wu);
                }
            } else {
                appendCharacterClassEscape(c);
            }
            return null;
        case 'W':
            if (isIgnoreCase() && isUnicode()) {
                if (!cclass) {
                    if (caseFoldedI()) {
                        out.append("(?-u:[");
                        out.append(characterClass_Wu);
                        out.append("])");
                    } else {
                        out.append('[');
                        out.append(characterClass_Wu);
                        out.append(']');
                    }
                } else {
                    out.append(characterClass_Wu);
                }
            } else {
                appendCharacterClassEscape(c);
            }
            return null;
        case 's':
            if (spaceCharacterClass()) {
                appendCharacterClassEscape(c);
            } else {
                if (negCharacterClass) {
                    return characterClass_S;
                }
                out.append(characterClass_s);
            }
            return null;
        case 'S':
            if (spaceCharacterClass()) {
                appendCharacterClassEscape(c);
            } else {
                if (negCharacterClass) {
                    return characterClass_s;
                }
                out.append(characterClass_S);
            }
            return null;
        default:
            throw new AssertionError("unreachable");
        }
    }

    private void appendCharacterClassEscape(char c) {
        if (characterProperty()) {
            char mod = (char) ('P' | (c & 0x20));
            String propertyName = getCharacterClassPropertyName(c);
            out.append('\\').append(mod).append('{').append(propertyName).append('}');
        } else {
            out.append('\\').append(c);
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

    private void appendIdentityEscape(int ch, boolean characterClass) {
        if (isASCIIAlpha(ch)) {
            // Don't escape ASCII alpha characters to avoid turning them into flags
            out.append((char) ch);
        } else if (ch <= 0x7f) {
            // Apply identity escape for other ASCII characters
            out.append('\\').append((char) ch);
        } else {
            appendPatternCharacter(ch, characterClass);
        }
    }

    private void appendOctalEscapeSequence(int o, boolean characterClass) {
        if (isIgnoreCase()) {
            if (isUnicode()) {
                appendCaseInsensitiveUnicode(o, characterClass);
            } else {
                appendCaseInsensitive(o, characterClass);
            }
        } else {
            if (octalEscape()) {
                out.append("\\0").append(Integer.toOctalString(o));
            } else {
                appendByteCodeUnit(o);
            }
        }
    }

    private void appendHexEscapeSequence(int x, boolean characterClass) {
        if (isIgnoreCase()) {
            if (isUnicode()) {
                appendCaseInsensitiveUnicode(x, characterClass);
            } else {
                appendCaseInsensitive(x, characterClass);
            }
        } else {
            appendByteCodeUnit(x);
        }
    }

    private void appendUnicodeEscapeSequence(int u, boolean characterClass) {
        if (isIgnoreCase()) {
            if (isUnicode()) {
                appendCaseInsensitiveUnicode(u, characterClass);
            } else {
                appendCaseInsensitive(u, characterClass);
            }
        } else if (u < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            appendCodeUnit(u);
        } else {
            appendCodePoint(u);
        }
    }

    private void appendExtendedUnicodeEscapeSequence(int u, boolean characterClass) {
        assert isUnicode();
        if (isIgnoreCase()) {
            appendCaseInsensitiveExtendedUnicode(u, characterClass);
        } else {
            appendCodePoint(u);
        }
    }

    private void appendPatternCharacter(int ch, boolean characterClass) {
        if (isIgnoreCase()) {
            if (isUnicode()) {
                appendCaseInsensitiveUnicode(ch, characterClass);
            } else {
                appendCaseInsensitive(ch, characterClass);
            }
        } else {
            appendCharacter(ch);
        }
    }

    // package private for CaseFoldData
    void appendCharacter(int ch) {
        if (ch < 0x100) {
            if (isASCIIAlphaNumericUnderscore(ch)) {
                appendASCIICodeUnit(ch);
            } else {
                appendByteCodeUnit(ch);
            }
        } else if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            appendCodeUnit(ch);
        } else {
            appendCodePoint(ch);
        }
    }

    private void appendASCIICodeUnit(int codeUnit) {
        assert codeUnit >>> 8 == 0;
        out.append((char) codeUnit);
    }

    private void appendByteCodeUnit(int codeUnit) {
        assert codeUnit >>> 8 == 0;
        if (alignBytes()) {
            if (isUnicode()) {
                out.append("\\u00").append(toHexDigit(codeUnit, 4)).append(toHexDigit(codeUnit, 0));
                return;
            }
            out.append("\\x00");
        }
        out.append("\\x").append(toHexDigit(codeUnit, 4)).append(toHexDigit(codeUnit, 0));
    }

    private void appendCodeUnit(int codeUnit) {
        assert Character.isBmpCodePoint(codeUnit);
        out.append("\\u").append(toHexDigit(codeUnit, 12)).append(toHexDigit(codeUnit, 8))
                .append(toHexDigit(codeUnit, 4)).append(toHexDigit(codeUnit, 0));
    }

    private void appendCodePoint(int codePoint) {
        out.append("\\x{").append(Integer.toHexString(codePoint)).append("}");
    }

    private void appendCaseInsensitive(int codePoint, boolean characterClass) {
        if (codePoint <= 0x7f || caseInsensitive()) {
            appendCharacter(codePoint);
            return;
        }
        int toUpper = Character.toUpperCase(codePoint);
        int toLower = Character.toLowerCase(codePoint);
        if (CaseFoldData.isValidCaseFold(codePoint, toUpper, toLower)) {
            int caseFold1 = CaseFoldData.caseFold1(codePoint);
            int caseFold2 = CaseFoldData.caseFold2(codePoint);
            if (!characterClass) {
                out.append('[');
            }

            appendCharacter(codePoint);
            if (codePoint != toUpper) {
                appendCharacter(toUpper);
            }
            if (codePoint != toLower && CaseFoldData.isValidToLower(codePoint)) {
                appendCharacter(toLower);
            }
            if (caseFold1 != -1) {
                appendCharacter(caseFold1);
            }
            if (caseFold2 != -1) {
                appendCharacter(caseFold2);
            }

            if (!characterClass) {
                out.append(']');
            }
        } else {
            appendCharacter(codePoint);
        }
    }

    private void appendCaseInsensitiveRange(int startChar, int endChar) {
        if (caseInsensitive()) {
            appendCharacter(startChar);
            out.append('-');
            appendCharacter(endChar);
            return;
        }
        Intervals intervals = intervals(startChar, endChar);
        for (int i = 0; i < intervals.length; ++i) {
            int startSubRange = intervals.start[i];
            int endSubRange = intervals.end[i];
            appendCharacter(startSubRange);
            if (startSubRange != endSubRange) {
                out.append('-');
                appendCharacter(endSubRange);
            }
        }
    }

    private static Intervals intervals(int startChar, int endChar) {
        Intervals intervals = new Intervals();
        for (int ch = startChar; ch <= endChar; ch += Character.charCount(ch)) {
            if (ch <= 0x7f) {
                intervals.add(ch);
            } else {
                addCodePoint(intervals, ch);
            }
        }
        return intervals;
    }

    private static void addCodePoint(Intervals intervals, int codePoint) {
        assert codePoint > 0x7f;
        int toUpper = Character.toUpperCase(codePoint);
        int toLower = Character.toLowerCase(codePoint);
        if (CaseFoldData.isValidCaseFold(codePoint, toUpper, toLower)) {
            int caseFold1 = CaseFoldData.caseFold1(codePoint);
            int caseFold2 = CaseFoldData.caseFold2(codePoint);

            intervals.add(codePoint);
            if (codePoint != toUpper) {
                intervals.add(toUpper);
            }
            if (codePoint != toLower && CaseFoldData.isValidToLower(codePoint)) {
                intervals.add(toLower);
            }
            if (caseFold1 != -1) {
                intervals.add(caseFold1);
            }
            if (caseFold2 != -1) {
                intervals.add(caseFold2);
            }
        } else {
            intervals.add(codePoint);
        }
    }

    private static final class Intervals {
        private int[] start = new int[4], end = new int[4];
        private int length = 0;

        private void mergeInterval(int index) {
            int start[] = this.start, end[] = this.end, length = this.length;

            // Combine intervals 'index' and 'index + 1'
            end[index] = end[index + 1];
            // Move entries after the merged intervals to the left
            System.arraycopy(start, index + 2, start, index + 1, length - (index + 2));
            System.arraycopy(end, index + 2, end, index + 1, length - (index + 2));
            this.length -= 1;
        }

        private void newInterval(int insert, int v) {
            int start[] = this.start, end[] = this.end, length = this.length;

            // Increase storage if necessary
            if (start.length == length) {
                this.start = start = Arrays.copyOf(start, length << 1);
                this.end = end = Arrays.copyOf(end, length << 1);
            }

            // Move entries right to insertion position by one
            if (length - insert > 0) {
                System.arraycopy(start, insert, start, insert + 1, length - insert);
                System.arraycopy(end, insert, end, insert + 1, length - insert);
            }

            // Insert new single element interval
            this.start[insert] = v;
            this.end[insert] = v;
            this.length += 1;
        }

        void add(int v) {
            int start[] = this.start, end[] = this.end, length = this.length;

            final int index = Arrays.binarySearch(start, 0, length, v);
            if (index >= 0) {
                // The interval at position 'index' starts with 'v'
                return;
            }
            int insert = -index - 1;
            int prev = insert - 1;
            if (prev >= 0 && start[prev] <= v && v <= end[prev]) {
                // The interval before the insertion position contains 'v'
            } else if (prev >= 0 && end[prev] == v - 1) {
                // The interval before the insertion position ends with 'v - 1'
                end[prev] = v;
                if (insert < length && start[insert] == v + 1) {
                    // Merge adjacent intervals
                    mergeInterval(prev);
                }
            } else if (insert < length && start[insert] == v + 1) {
                // The interval at the insertion position starts with 'v + 1'
                start[insert] = v;
            } else {
                newInterval(insert, v);
            }
        }
    }

    private void appendCaseInsensitiveUnicode(int codePoint, boolean characterClass) {
        if (unicodeCharacters()) {
            appendCharacter(codePoint);
            return;
        }
        if (CaseFoldData.hasAdditionalUnicodeCaseFold(codePoint)) {
            if (!characterClass) {
                out.append('[');
            }
            CaseFoldData.appendCaseInsensitiveUnicode(this, codePoint);
            if (!characterClass) {
                out.append(']');
            }
        } else if (CaseFoldData.hasRestrictedUnicodeCaseFold(codePoint) && caseFoldedI()) {
            if (!characterClass) {
                out.append("(?-u:");
                appendCharacter(codePoint);
                out.append(')');
            } else {
                appendCharacter(codePoint);
            }
        } else {
            appendCharacter(codePoint);
        }
    }

    private void appendCaseInsensitiveExtendedUnicode(int codePoint, boolean characterClass) {
        if (Character.isBmpCodePoint(codePoint) && Character.isSurrogate((char) codePoint)) {
            appendUnpairedSurrogate(codePoint);
        } else {
            appendCaseInsensitiveUnicode(codePoint, characterClass);
        }
    }

    private void appendCaseInsensitiveUnicodeRange(int startChar, int endChar) {
        appendCharacterAsSingle(startChar);
        out.append('-');
        appendCharacterAsSingle(endChar);
        if (!unicodeCharacters()) {
            CaseFoldData.appendCaseInsensitiveUnicodeRange(this, startChar, endChar);
        }
    }

    private void appendCharacterAsSingle(int codePoint) {
        if (Character.isBmpCodePoint(codePoint) && Character.isSurrogate((char) codePoint)) {
            appendUnpairedSurrogate(codePoint);
        } else {
            appendCharacter(codePoint);
        }
    }

    private void appendUnpairedSurrogate(int codePoint) {
        // output unpaired surrogate in extended form
        appendCodePoint(codePoint);
    }

    private static final char toControlLetter(int c) {
        return (char) ('A' - 1 + (c & 0x1F));
    }

    private static final char toHexDigit(int c, int shift) {
        return HEXDIGITS[(c >> shift) & 0xf];
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
    private static final boolean isSyntaxCharacter(int c) {
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
