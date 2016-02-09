/**
 * Copyright (c) 2012-2016 André Bargull
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
    private static final int BACKREF_LIMIT = 0xFFFF;
    private static final int DEPTH_LIMIT = 0xFFFF;
    private static final char[] HEXDIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B',
            'C', 'D', 'E', 'F' };

    // CharacterClass \w ~ [a-zA-Z0-9_] and LATIN SMALL LETTER LONG S and KELVIN SIGN
    private static final String characterClass_wu = "a-zA-Z0-9_\\u017f\\u212a";
    // CharacterClass \W ~ [\u0000-\u002F\u003A-\u0040\u005B-\u005E\u0060\u007B-\x{10ffff}]
    private static final String characterClass_Wu = "\\u0000-\\u002F\\u003A-\\u0040\\u005B-\\u005E\\u0060\\u007B-\\x{10ffff}\\x53\\x73\\x4B\\x6B";
    // [] => matches nothing
    private static final String emptyCharacterClass = "(?:\\Z )";
    // [^] => matches everything
    private static final String emptyNegCharacterClass = "(?s:.)";

    private final String source;
    private final int length;
    private final String sourceFile;
    private final int sourceLine;
    private final int sourceColumn;
    private final int flags;
    private final boolean webRegExp;
    private final StringBuilder out;

    // Current source position.
    private int pos = 0;

    // Map of groups created within negative lookahead.
    private final BitSet negativeLAGroups = new BitSet();

    private RegExpParser(String source, String flags, String sourceFile, int sourceLine, int sourceColumn,
            boolean webRegExp) {
        this.source = source;
        this.length = source.length();
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.sourceColumn = sourceColumn;
        // Call after source information was set
        this.flags = toFlags(flags);
        this.webRegExp = webRegExp;
        this.out = new StringBuilder(length);
    }

    public static RegExpMatcher parse(String pattern, String flags, String sourceFile, int sourceLine, int sourceColumn,
            boolean webRegExp) throws ParserException {
        RegExpParser parser = new RegExpParser(pattern, flags, sourceFile, sourceLine, sourceColumn, webRegExp);
        parser.pattern();

        return new JoniRegExpMatcher(parser.out.toString(), parser.flags, parser.negativeLAGroups);
    }

    public static void syntaxParse(String pattern, String flags, String sourceFile, int sourceLine, int sourceColumn,
            boolean webRegExp) throws ParserException {
        RegExpParser parser = new RegExpParser(pattern, flags, sourceFile, sourceLine, sourceColumn, webRegExp);
        parser.pattern();
    }

    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn + pos, messageKey,
                args);
    }

    private ParserException error(Messages.Key messageKey, int offset, char offending) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn + pos + offset,
                messageKey, String.valueOf(offending));
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
            return (hexDigit(get()) << 12) | (hexDigit(get()) << 8) | (hexDigit(get()) << 4) | hexDigit(get());
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
     */
    private void characterClass() {
        final StringBuilder out = this.out;
        boolean negation = match('^');
        if (match(']')) {
            // empty character class
            out.append(!negation ? emptyCharacterClass : emptyNegCharacterClass);
            return;
        }
        out.append('[');
        if (negation) {
            out.append('^');
        }

        final boolean unicode = isUnicode();
        final boolean web = isWebRegularExpression();
        int rangeStartCV = 0;
        boolean inRange = false, inCCRange = false;
        charclass: for (;;) {
            if (eof()) {
                throw error(Messages.Key.RegExpUnmatchedCharacter, "[");
            }

            final int cv, c = get(unicode);
            classatom: switch (c) {
            case ']':
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

                default: {
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
                    out.append('\\').append(get());
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
                    if (num > backreflimit) {
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
                        if (num > backrefmax) {
                            backrefmax = num;
                        }
                        if (num <= groups && validGroups.get(num)) {
                            out.append('\\').append(num);
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
                    if (unicode ? !isSyntaxCharacterOrSlash(d) : !web && isUnicodeIDContinue(d)) {
                        throw error(Messages.Key.RegExpInvalidEscape, new String(Character.toChars(d)));
                    }
                    appendIdentityEscape(d);
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
                characterClass();
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
                out.append('.');
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

        return true;
    }

    private void appendCharacterClassEscape(char c, boolean cclass) {
        if ((c == 'w' || c == 'W') && isIgnoreCase() && isUnicode()) {
            if (!cclass) {
                out.append('[');
            }
            out.append(c == 'w' ? characterClass_wu : characterClass_Wu);
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
}
