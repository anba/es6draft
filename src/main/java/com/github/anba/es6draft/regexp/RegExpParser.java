/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import java.util.Arrays;
import java.util.BitSet;
import java.util.regex.Pattern;

import org.joni.Config;

import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.parser.ParserException.ExceptionType;
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
    private static final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F' };

    // CharacterClass \s
    private static final String characterClass_s = "[ \\t\\n\\u000B\\f\\r\\u2028\\u2029\\u00A0\\uFEFF\\p{gc=Zs}]";
    // CharacterClass \S
    private static final String characterClass_S = "[^ \\t\\n\\u000B\\f\\r\\u2028\\u2029\\u00A0\\uFEFF\\p{gc=Zs}]";
    // [] => matches nothing
    private static final String emptyCharacterClass = "(?:\\Z )";
    // [^] => matches everything
    private static final String emptyNegCharacterClass = "(?s:.)";
    // Character class for . (dot)
    private static final String characterClass_Dot = "[^\\n\\r\\u2028\\u2029]";

    private final String source;
    private final int length;
    private final int flags;
    private final boolean joni;
    private final String sourceFile;
    private final int sourceLine;
    private final int sourceColumn;
    private final StringBuilder out;
    private int pos = 0;

    // map of groups created within negative lookahead
    private BitSet negativeLAGroups = new BitSet();

    private RegExpParser(String source, int flags, String sourceFile, int sourceLine,
            int sourceColumn) {
        this.source = source;
        this.length = source.length();
        this.flags = flags;
        this.joni = !isUnicode();
        this.sourceFile = sourceFile;
        this.sourceLine = sourceLine;
        this.sourceColumn = sourceColumn;
        this.out = new StringBuilder(length);
    }

    public static RegExpMatcher parse(String pattern, String flags, String sourceFile,
            int sourceLine, int sourceColumn) throws ParserException {
        // flags :: g | i | m | u | y
        final int global = 0b00001, ignoreCase = 0b00010, multiline = 0b00100, unicode = 0b01000, sticky = 0b10000;
        int mask = 0b00000;
        for (int i = 0, len = flags.length(); i < len; ++i) {
            char c = flags.charAt(i);
            int flag = (c == 'g' ? global : c == 'i' ? ignoreCase : c == 'm' ? multiline
                    : c == 'u' ? unicode : c == 'y' ? sticky : -1);
            if (flag != -1 && (mask & flag) == 0) {
                mask |= flag;
            } else {
                String detail;
                Messages.Key reason;
                switch (flag) {
                case global:
                    detail = "global";
                    reason = Messages.Key.RegExpDuplicateFlag;
                    break;
                case ignoreCase:
                    detail = "ignoreCase";
                    reason = Messages.Key.RegExpDuplicateFlag;
                    break;
                case multiline:
                    detail = "multiline";
                    reason = Messages.Key.RegExpDuplicateFlag;
                    break;
                case unicode:
                    detail = "unicode";
                    reason = Messages.Key.RegExpDuplicateFlag;
                    break;
                case sticky:
                    detail = "sticky";
                    reason = Messages.Key.RegExpDuplicateFlag;
                    break;
                default:
                    detail = String.valueOf(c);
                    reason = Messages.Key.RegExpInvalidFlag;
                    break;
                }
                throw error(sourceFile, sourceLine, sourceColumn, reason, detail);
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

        RegExpParser parser = new RegExpParser(pattern, iflags, sourceFile, sourceLine,
                sourceColumn);
        parser.pattern();

        // System.out.printf("pattern = %s%n", parser.out.toString());

        if (parser.useJoniRegExp()) {
            return new JoniRegExpMatcher(parser.out.toString(), iflags, parser.negativeLAGroups);
        }
        return new JDKRegExpMatcher(parser.out.toString(), iflags, parser.negativeLAGroups);
    }

    private static ParserException error(String file, int line, int column,
            Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, file, line, column, messageKey, args);
    }

    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, sourceFile, sourceLine, sourceColumn,
                messageKey, args);
    }

    private boolean useJoniRegExp() {
        return joni;
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
     * @return {@code true} if case sensitive patterns are supported
     */
    private boolean caseInsensitive() {
        return joni;
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
        char c = peek(0);
        if (!(c >= '0' && c <= '9')) {
            return -1;
        }
        long num = get() - '0';
        for (;;) {
            c = peek(0);
            if (!(c >= '0' && c <= '9')) {
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
        char d = peek(0);
        if (d >= '0' && d <= '7') {
            num = num * 8 + (get() - '0');
            if (num <= 037) {
                d = peek(0);
                if (d >= '0' && d <= '7') {
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
            char d = peek(0);
            if (!(d >= '0' && d <= '9')) {
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
        boolean unicode = isUnicode();
        if (unicode && match('{')) {
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
        } else {
            int start = pos;
            int c = hex4Digits();
            SURROGATE: if (unicode && Character.isHighSurrogate((char) c)) {
                int startLow = pos;
                if (match('\\') && match('u')) {
                    int d = hex4Digits();
                    if (Character.isLowSurrogate((char) d)) {
                        c = Character.toCodePoint((char) c, (char) d);
                        break SURROGATE;
                    }
                }
                // lone high surrogate, discard parsed characters
                reset(startLow);
            }
            if (!Character.isValidCodePoint(c)) {
                // TODO: always report error if in unicode-mode?
                // invalid unicode escape sequence, discard parsed characters
                reset(start);
            }
            return c;
        }
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
     * NonemptyClassRangesNoDash<span><sub>[U]</sub></span> ::
     *     ClassAtom<span><sub>[?U]</sub></span>
     *     ClassAtomNoDash<span><sub>[?U]</sub></span> NonemptyClassRangesNoDash<span><sub>[?U]</sub></span>
     *     ClassAtomNoDash<span><sub>[?U]</sub></span> <b>-</b> ClassAtom<span><sub>[?U]</sub></span> ClassRanges<span><sub>[?U]</sub></span>
     * ClassAtom<span><sub>[U]</sub></span> ::
     *     <b>-</b>
     *     ClassAtomNoDash<span><sub>[?U]</sub></span>
     * ClassAtomNoDash<span><sub>[U]</sub></span> ::
     *     SourceCharacter <b>but not one of \ or ] or -</b>
     *     <b>\</b> ClassEscape<span><sub>[?U]</sub></span>
     * ClassEscape<span><sub>[U]</sub></span> ::
     *     DecimalEscape
     *     <b>b</b>
     *     CharacterEscape<span><sub>[?U]</sub></span>
     *     CharacterClassEscape
     * </pre>
     * 
     * @param negation
     *            flag to mark negative character classes
     */
    private void characterclass(boolean negation) {
        final boolean ignoreCase = isIgnoreCase();
        final boolean unicode = isUnicode();

        StringBuilder out = this.out;
        int startLength = out.length();
        int rangeStartCV = 0, rangeStartPos = 0;
        boolean inrange = false;
        // additional classes need to go to the end of the character-class if in negated class
        StringBuilder additionalClasses = null;
        charclass: for (;;) {
            if (eof()) {
                throw error(Messages.Key.RegExpUnmatchedCharacter, "[");
            }

            int cv, c = get(unicode), outStart = out.length();
            classatom: switch (c) {
            case ']':
                if (additionalClasses != null) {
                    if (out.length() != startLength) {
                        out.append("&&");
                    }
                    out.append(additionalClasses);
                }
                return;
            case '\\': {
                switch (peek(0)) {
                case 'd':
                case 'D':
                case 'w':
                case 'W':
                case 's':
                case 'S':
                    // class escape (cannot start/end range)
                    if (inrange)
                        throw error(Messages.Key.RegExpInvalidCharacterRange);
                    String escape = appendCharacterClassEscape(get(), negation);
                    if (escape != null) {
                        if (additionalClasses == null) {
                            additionalClasses = new StringBuilder();
                        } else {
                            additionalClasses.append("&&");
                        }
                        additionalClasses.append(escape);
                    }
                    continue charclass;

                case 'b':
                    // CharacterEscape :: ControlEscape
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
                    if (isASCIIAlphaNumericUnderscore(peek(1))) {
                        // extended control letters with 0-9 and _
                        out.append('\\').append(get());
                        int d = get() & 0x1F;
                        out.append(toControlLetter(d));
                        cv = d;
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
                    int u = readUnicodeEscapeSequence();
                    if (Character.isValidCodePoint(u)) {
                        appendUnicodeEscapeSequence(u, true);
                        cv = u;
                    } else {
                        // invalid unicode escape sequence, use "u"
                        out.append("u");
                        cv = 'u';
                    }
                    break classatom;
                }

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7': {
                    int num = readOctalEscapeSequence();
                    appendOctalEscapeSequence(num, true);
                    cv = num;
                    break classatom;
                }
                case '8':
                case '9': {
                    char d = get();
                    out.append("\\\\").append(d);
                    cv = d;
                    break classatom;
                }

                default: {
                    if (eof()) {
                        throw error(Messages.Key.RegExpTrailingSlash);
                    }
                    // TODO: need to check next drafts, current draft (rev21) only allows
                    // IdentityEscape for SyntaxCharacters in unicode-mode
                    int d = get(unicode);
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
                if (c > 0x7f && ignoreCase) {
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
                    // `RegExp("[\u00b5]", "ui").test("\u03bc")` works in Java, but
                    // `RegExp("[\u00b5-\u00b5]", "ui").test("\u03bc")` doesn't. If we hit the
                    // latter case, rewrite the pattern to include the additional code-points.
                    if (CaseFoldData.hasAdditionalUnicodeCaseFold(rangeStartCV)
                            || CaseFoldData.hasAdditionalUnicodeCaseFold(cv)) {
                        // remove sharp-s pattern and use standard range expression
                        out.setLength(rangeStartPos);
                        appendCharacter(rangeStartCV);
                        out.append('-');
                        appendCharacter(cv);
                    }
                    appendCaseInsensitiveUnicodeRange(rangeStartCV, cv);
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
     * Assertio<span><sub>[U]</sub></span>n ::
     *     <b>^</b>
     *     <b>$</b>
     *     <b>\ b</b>
     *     <b>\ B</b>
     *     <b>( ? =</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     *     <b>( ? !</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     * Atom<span><sub>[U]</sub></span> ::
     *     PatternCharacter
     *     <b>.</b>
     *     <b>\</b> AtomEscape<span><sub>[?U]</sub></span>
     *     CharacterClass<span><sub>[?U]</sub></span>
     *     <b>(</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     *     <b>( ? :</b> Disjunction<span><sub>[?U]</sub></span> <b>)</b>
     * PatternCharacter ::
     *     SourceCharacter but not one of <b>^ $ \ . * + ? ( ) [ ] { } |</b>
     * AtomEscape<span><sub>[U]</sub></span> ::
     *     DecimalEscape
     *     CharacterEscape<span><sub>[?U]</sub></span>
     *     CharacterClassEscape
     * CharacterEscape<span><sub>[U]</sub></span> ::
     *     ControlEscape
     *     <b>c</b> ControlLetter
     *     HexEscapeSequence
     *     RegExpUnicodeEscapeSequence<span><sub>[?U]</sub></span>
     *     IdentityEscape<span><sub>[?U]</sub></span>
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
     * DecimalEscape ::
     *     DecimalIntegerLiteral  [LA &#x2209; DecimalDigit]
     * CharacterClassEscape :: one of
     *     <b>d D s S w W</b>
     * </pre>
     */
    private void pattern() {
        final boolean ignoreCase = isIgnoreCase();
        final boolean unicode = isUnicode();
        StringBuilder out = this.out;

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
                    } else {
                        // convert invalid ControlLetter to \\
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
                    } else {
                        // invalid hex escape sequence, use "x"
                        out.append("x");
                    }
                    break atom;
                }
                case 'u': {
                    // CharacterEscape :: RegExpUnicodeEscapeSequence
                    mustMatch('u');
                    int u = readUnicodeEscapeSequence();
                    if (Character.isValidCodePoint(u)) {
                        appendUnicodeEscapeSequence(u, false);
                    } else {
                        // invalid unicode escape sequence, use "u"
                        out.append("u");
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
                        if (peek(0) < '8') {
                            // case 1: octal escape sequence
                            appendOctalEscapeSequence(readOctalEscapeSequence(), false);
                        } else {
                            // case 2 (\8 or \9): invalid octal escape sequence
                            out.append("\\\\");
                        }
                    } else {
                        if (num > backrefmax) {
                            backrefmax = num;
                        }
                        if (num <= groups && validGroups.get(num)) {
                            appendBackReference(num);
                        } else {
                            // omit forward reference (TODO: check this!) or backward reference into
                            // capturing group from negative lookahead
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
                    // TODO: need to check next drafts, current draft (rev21) only allows
                    // IdentityEscape for SyntaxCharacters in unicode-mode
                    int d = get(unicode);
                    appendIdentityEscape(d, false);
                    break atom;
                }
                }
                // assert false : "not reached";
            }

            case '(': {
                boolean negative = false, capturing = false;
                if (match('?')) {
                    // (?=X) or (?!X) or (?:X)
                    char d = eof() ? '\0' : get();
                    switch (d) {
                    case '!':
                        negative = true;
                        // fall-through
                    case '=':
                    case ':':
                        break;
                    default:
                        throw error(Messages.Key.RegExpInvalidQuantifier);
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
                }
                if (negative) {
                    negativedepth += 1;
                    negativeGroup.set(depth);
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
                if (capturingGroup.get(depth)) {
                    capturingGroup.clear(depth);
                    // update group information after parsing ")"
                    int g = groupStack[--groupStackSP];
                    validGroups.set(g);
                    if (negativedepth > 0) {
                        negativeLAGroups.set(g);
                    }
                }
                if (negativeGroup.get(depth)) {
                    negativeGroup.clear(depth);
                    // invalidate all capturing groups created within the negative lookahead
                    int g = groupStack[--groupStackSP];
                    for (int v = groups; v != g; --v) {
                        validGroups.clear(v);
                    }
                    negativedepth -= 1;
                }
                depth -= 1;
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
                    characterclass(negation);
                    out.append(']');
                } else {
                    appendEmptyCharacterClass(negation);
                }
                break atom;
            }

            case '*':
            case '+':
            case '?':
            case '{': {
                if (quantifier((char) c)) {
                    // parsed quantifier, but there was no applicable atom -> error!
                    throw error(Messages.Key.RegExpInvalidQuantifier);
                }
                // fall-through
            }
            case ']':
            case '}':
                // web-reality
                out.append('\\').append((char) c);
                break atom;

            case '.':
                appendDotCharacterClass();
                break atom;

            default: {
                // inlined: appendPatternCharacter(c)
                if (c > 0x7f && ignoreCase) {
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
            // make web-reality aware
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
            assert false : "unreachable";
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
            assert false : "unreachable";
            return null;
        }
    }

    private String appendCharacterClassEscape(char c, boolean negCharacterClass) {
        if (characterProperty()) {
            char mod = (char) ('P' | (c & 0x20));
            String propertyName = getCharacterClassPropertyName(c);
            out.append('\\').append(mod).append('{').append(propertyName).append('}');
            return null;
        }
        switch (c) {
        case 'd':
        case 'D':
        case 'w':
        case 'W':
            out.append('\\').append(c);
            break;
        case 's':
            if (negCharacterClass) {
                return characterClass_S;
            }
            out.append(characterClass_s);
            break;
        case 'S':
            if (negCharacterClass) {
                return characterClass_s;
            }
            out.append(characterClass_S);
            break;
        default:
            assert false : "unreachable";
        }
        return null;
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
        if (o > 0x7f && isIgnoreCase()) {
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
        if (x > 0x7f && isIgnoreCase()) {
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
        if (u > 0x7f && isIgnoreCase()) {
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

    private void appendPatternCharacter(int ch, boolean characterClass) {
        if (ch > 0x7f && isIgnoreCase()) {
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
            appendByteCodeUnit(ch);
        } else if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            appendCodeUnit(ch);
        } else {
            appendCodePoint(ch);
        }
    }

    private void appendByteCodeUnit(int codeUnit) {
        assert codeUnit >>> 8 == 0;
        if (alignBytes()) {
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
        assert Character.isSupplementaryCodePoint(codePoint);
        out.append("\\x{").append(Integer.toHexString(codePoint)).append("}");
    }

    private void appendCaseInsensitive(int codePoint, boolean characterClass) {
        assert codePoint > 0x7f;
        if (caseInsensitive()) {
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
        assert codePoint > 0x7f;
        if (CaseFoldData.hasAdditionalUnicodeCaseFold(codePoint)) {
            if (!characterClass) {
                out.append('[');
            }
            CaseFoldData.appendCaseInsensitiveUnicode(this, codePoint);
            if (!characterClass) {
                out.append(']');
            }
        } else {
            appendCharacter(codePoint);
        }
    }

    private void appendCaseInsensitiveUnicodeRange(int startChar, int endChar) {
        CaseFoldData.appendCaseInsensitiveUnicodeRange(this, startChar, endChar);
    }

    private static final char toControlLetter(int c) {
        return (char) ('A' - 1 + (c & 0x1F));
    }

    private static final char toHexDigit(int c, int shift) {
        return HEXDIGITS[(c >> shift) & 0xf];
    }

    private static final int hexDigit(int c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        } else if (c >= 'A' && c <= 'F') {
            return (c - ('A' - 10));
        } else if (c >= 'a' && c <= 'f') {
            return (c - ('a' - 10));
        }
        return -1;
    }

    private static final boolean isASCIIAlpha(int c) {
        return (c | 0x20) >= 'a' && (c | 0x20) <= 'z';
    }

    private static final boolean isASCIIAlphaNumericUnderscore(int c) {
        return (c >= '0' && c <= '9') || ((c | 0x20) >= 'a' && (c | 0x20) <= 'z') || c == '_';
    }
}
