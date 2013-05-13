/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import java.util.BitSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * <h1>15 Standard Built-in ECMAScript Objects</h1><br>
 * <h2>15.10 RegExp (Regular Expression) Objects</h2>
 * <ul>
 * <li>15.10.1 Patterns
 * <li>15.10.2 Pattern Semantics
 * </ul>
 */
public class RegExpParser {
    private static final int BACKREF_LIMIT = 0xffff;
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

    private final String source;
    private final int length;
    private final int flags;
    private int sourceLine;
    private StringBuilder out;
    private int pos = 0;

    // Java pattern for the input RegExp
    private Pattern pattern;
    // map of groups created within negative lookahead
    private BitSet negativeLAGroups = new BitSet();
    // map of invalidated groups
    private BitSet validGroups = new BitSet();
    // number of groups
    private int groups = 0;
    // maximum backref found
    private int backrefmax = 0;
    // backref limit
    private int backreflimit = BACKREF_LIMIT;

    private RegExpParser(String source, int flags, int sourceLine) {
        this.source = source;
        this.length = source.length();
        this.flags = flags;
        this.sourceLine = sourceLine;
        this.out = new StringBuilder(length);
    }

    public static RegExpParser parse(String p, String f, int sourceLine) throws ParserException {
        // flags :: g | i | m | u | y
        final int global = 0b00001, ignoreCase = 0b00010, multiline = 0b00100, unicode = 0b01000, sticky = 0b10000;
        int flags = 0b00000;
        for (int i = 0, len = f.length(); i < len; ++i) {
            char c = f.charAt(i);
            int flag = (c == 'g' ? global : c == 'i' ? ignoreCase : c == 'm' ? multiline
                    : c == 'u' ? unicode : c == 'y' ? sticky : -1);
            if (flag != -1 && (flags & flag) == 0) {
                flags |= flag;
            } else {
                switch (flag) {
                case global:
                    throw error(sourceLine, Messages.Key.DuplicateRegExpFlag, "global");
                case ignoreCase:
                    throw error(sourceLine, Messages.Key.DuplicateRegExpFlag, "ignoreCase");
                case multiline:
                    throw error(sourceLine, Messages.Key.DuplicateRegExpFlag, "multiline");
                case unicode:
                    throw error(sourceLine, Messages.Key.DuplicateRegExpFlag, "unicode");
                case sticky:
                    throw error(sourceLine, Messages.Key.DuplicateRegExpFlag, "sticky");
                default:
                    throw error(sourceLine, Messages.Key.InvalidRegExpFlag, String.valueOf(c));
                }
            }
        }

        int iflags = 0;
        if ((flags & ignoreCase) != 0) {
            iflags |= Pattern.CASE_INSENSITIVE;
            iflags |= Pattern.UNICODE_CASE;
        }
        if ((flags & multiline) != 0) {
            iflags |= Pattern.MULTILINE;
        }

        RegExpParser parser = new RegExpParser(p, iflags, sourceLine);
        parser.pattern();
        String regexp = parser.out.toString();
        try {
            parser.pattern = Pattern.compile(regexp, iflags);
        } catch (PatternSyntaxException e) {
            throw error(sourceLine, Messages.Key.InvalidRegExpPattern, e.getMessage());
        }
        return parser;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public BitSet getNegativeLookaheadGroups() {
        return negativeLAGroups;
    }

    private static ParserException error(int line, Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, line, messageKey, args);
    }

    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, sourceLine, messageKey, args);
    }

    private boolean isMultiline() {
        return (flags & Pattern.MULTILINE) != 0;
    }

    private int peek(int i) {
        return pos + i < length ? source.charAt(pos + i) : -1;
    }

    private int get() {
        return pos < length ? source.charAt(pos++) : -1;
    }

    private boolean match(char c) {
        if (c == peek(0)) {
            get();
            return true;
        }
        return false;
    }

    private int mustMatch(char c) {
        if (c != get()) {
            throw error(Messages.Key.RegExpUnexpectedCharacter, String.valueOf((char) c));
        }
        return c;
    }

    private boolean eof() {
        return pos >= length;
    }

    private void pattern() {
        assert pos == 0;
        disjunction(0, 0);
        if (backrefmax > groups) {
            out.setLength(0);
            pos = 0;
            groups = 0;
            backreflimit = groups;
            backrefmax = 0;
            negativeLAGroups.clear();
            validGroups.clear();
            disjunction(0, 0);
            assert backrefmax <= groups;
        }
    }

    /**
     * DecimalDigits :: DecimalDigit DecimalDigits DecimalDigit
     */
    private int decimal() {
        int c = peek(0);
        if (!(c >= '0' && c <= '9')) {
            return -1;
        }
        int num = get() - '0';
        for (;;) {
            c = peek(0);
            if (!(c >= '0' && c <= '9')) {
                return num;
            }
            num = num * 10 + (get() - '0');
            if (num >= 0x0CCCCCCC) {
                return decimal(num);
            }
        }
    }

    private int decimal(long num) {
        for (;;) {
            int c = peek(0);
            if (!(c >= '0' && c <= '9')) {
                return (int) Math.min(num, Integer.MAX_VALUE);
            }
            num = num * 10 + (get() - '0');
            if (num >= 0xFFFFFFFFL) {
                throw error(Messages.Key.RegExpInvalidQualifier);
            }
        }
    }

    private static int hexDigit(int c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        } else if (c >= 'A' && c <= 'F') {
            return (c - ('A' - 10));
        } else if (c >= 'a' && c <= 'f') {
            return (c - ('a' - 10));
        }
        return -1;
    }

    /**
     * Copied from {@link TokenStream#readUnicode()}
     */
    private int readUnicode() {
        int c = get();
        if (c == '{') {
            int acc = 0;
            c = get();
            do {
                acc = (acc << 4) | hexDigit(c);
            } while ((acc >= 0 && acc < 0x10FFFF) || (c = get()) != '}');
            if (c == '}') {
                c = acc;
            } else {
                c = -1;
            }
        } else {
            c = (hexDigit(c) << 12) | (hexDigit(get()) << 8) | (hexDigit(get()) << 4)
                    | hexDigit(get());
        }
        if (c < 0 || c > 0x10FFFF) {
            return -1;
        }
        return c;
    }

    private void characterclass(boolean negation) {
        // TODO: check range [start-end] is valid
        int startpos = pos;
        boolean inrange = false;
        charclass: for (;;) {
            if (eof()) {
                throw error(Messages.Key.RegExpUnmatchedCharacter, "[");
            }

            int c = get();
            classatom: switch (c) {
            case ']':
                pos -= 1; // so we can match ']' below again
                return;
            case '\\': {
                switch (peek(0)) {
                case 'd':
                case 'D':
                case 'w':
                case 'W':
                    // class escape (cannot start/end range)
                    if (inrange)
                        throw error(Messages.Key.RegExpInvalidCharacterRange);
                    out.append('\\').append((char) get());
                    continue charclass;
                case 's':
                    // class escape (cannot start/end range)
                    if (inrange)
                        throw error(Messages.Key.RegExpInvalidCharacterRange);
                    mustMatch('s');
                    if (!negation) {
                        out.append(characterClass_s);
                    } else {
                        if (pos - 2 != startpos) {
                            out.append("&&");
                        }
                        out.append(characterClass_S);
                    }
                    continue charclass;
                case 'S':
                    // class escape (cannot start/end range)
                    if (inrange)
                        throw error(Messages.Key.RegExpInvalidCharacterRange);
                    mustMatch('S');
                    if (!negation) {
                        out.append(characterClass_S);
                    } else {
                        if (pos - 2 != startpos) {
                            out.append("&&");
                        }
                        out.append(characterClass_s);
                    }
                    continue charclass;

                case 'b':
                    // CharacterEscape :: ControlEscape
                    mustMatch('b');
                    out.append("\\x08");
                    break classatom;
                case 'f':
                case 'n':
                case 'r':
                case 't':
                    // CharacterEscape :: ControlEscape
                    out.append('\\').append((char) get());
                    break classatom;
                case 'v':
                    // CharacterEscape :: ControlEscape
                    mustMatch('v');
                    out.append('\u000B');
                    break classatom;
                case 'c': {
                    // CharacterEscape :: c ControlLetter
                    int cc = peek(1);
                    if ((cc >= 'a' && cc <= 'z') || (cc >= 'A' && cc <= 'Z')
                            || (cc >= '0' && cc <= '9') || cc == '_') {
                        // extended control letters with 0-9 and _
                        out.append('\\').append((char) get())
                                .append((char) ((get() | 0x40) & ~0x20));
                        break classatom;
                    }
                    // convert invalid ControlLetter to \\
                    out.append("\\\\");
                    break classatom;
                }
                case 'x': {
                    // CharacterEscape :: HexEscapeSequence
                    mustMatch('x');
                    int start = pos;
                    int x = (hexDigit(get()) << 4) | hexDigit(get());
                    if (x >= 0x00 && x <= 0xff) {
                        out.append("\\x").append(HEXDIGITS[(x >> 4) & 0xf])
                                .append(HEXDIGITS[x & 0xf]);
                    } else {
                        // invalid hex escape sequence, use "x"
                        pos = start;
                        out.append("x");
                    }
                    break classatom;
                }
                case 'u': {
                    // CharacterEscape :: UnicodeEscapeSequence
                    mustMatch('u');
                    int start = pos;
                    int u = readUnicode();
                    if (u >= 0 && u <= 0x10ffff) {
                        out.append("\\x{").append(Integer.toHexString(u)).append("}");
                    } else {
                        // invalid hex escape sequence, use "u"
                        pos = start;
                        out.append("u");
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
                    int num = get() - '0';
                    while (num <= 037) {
                        int d = peek(0);
                        if (!(d >= '0' && d <= '7')) {
                            break;
                        }
                        num = num * 8 + (get() - '0');
                    }
                    out.append("\\0").append(Integer.toOctalString(num));
                    break classatom;
                }
                case '8':
                case '9': {
                    out.append("\\\\").append((char) get());
                    break classatom;
                }

                default: {
                    int d = get();
                    if (d == -1) {
                        throw error(Messages.Key.RegExpTrailingSlash);
                    } else if ((d >= 'a' && d <= 'z') || (d >= 'A' && d <= 'Z')) {
                        // don't escape alpha chars
                        out.append((char) d);
                    } else {
                        // identity escape
                        out.append('\\').append((char) d);
                    }
                    break classatom;
                }
                }
            }
            case '-':
            case '[':
            case '&':
                // need to escape these characters for Java
                out.append('\\').append((char) c);
                break classatom;
            default:
                out.append((char) c);
                break classatom;
            }

            if (inrange) {
                // end range
                inrange = false;
            } else if (peek(0) == '-' && !(peek(1) == -1 || peek(1) == ']')) {
                // start range
                mustMatch('-');
                out.append('-');
                inrange = true;
            } else {
                // no range
            }
            continue charclass;
        }
    }

    /**
     * <pre>
     * Pattern ::
     *      Disjunction
     * Disjunction ::
     *      Alternative
     *      Alternative | Disjunction
     * Alternative ::
     *      [empty]
     *      Alternative Term
     * Term ::
     *      Assertion
     *      Atom
     *      Atom Quantifier
     * </pre>
     */
    private void disjunction(int depth, int negativedepth) {
        if (depth > 0xffff) {
            throw error(Messages.Key.RegExpPatternTooComplex);
        }

        term: for (;;) {
            if (eof()) {
                if (depth > 0) {
                    throw error(Messages.Key.RegExpUnmatchedCharacter, "(");
                }
                return;
            }

            final int c = get();

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
                    out.append('\\').append((char) get());
                    continue term;
                case 'f':
                case 'n':
                case 'r':
                case 't':
                    // CharacterEscape :: ControlEscape
                    out.append('\\').append((char) get());
                    break atom;
                case 'v':
                    // CharacterEscape :: ControlEscape
                    mustMatch('v');
                    out.append('\u000B');
                    break atom;
                case 'c': {
                    // CharacterEscape :: c ControlLetter
                    int cc = peek(1);
                    if ((cc >= 'a' && cc <= 'z') || (cc >= 'A' && cc <= 'Z')) {
                        out.append('\\').append((char) get()).append((char) (get() & ~0x20));
                        break atom;
                    }
                    // convert invalid ControlLetter to \\
                    out.append("\\\\");
                    break atom;
                }
                case 'x': {
                    // CharacterEscape :: HexEscapeSequence
                    mustMatch('x');
                    int start = pos;
                    int x = (hexDigit(get()) << 4) | hexDigit(get());
                    if (x >= 0x00 && x <= 0xff) {
                        out.append("\\x").append(HEXDIGITS[(x >> 4) & 0xf])
                                .append(HEXDIGITS[x & 0xf]);
                    } else {
                        // invalid hex escape sequence, use "x"
                        pos = start;
                        out.append("x");
                    }
                    break atom;
                }
                case 'u': {
                    // CharacterEscape :: UnicodeEscapeSequence
                    mustMatch('u');
                    int start = pos;
                    int u = readUnicode();
                    if (u >= 0 && u <= 0x10ffff) {
                        out.append("\\x{").append(Integer.toHexString(u)).append("}");
                    } else {
                        // invalid hex escape sequence, use "u"
                        pos = start;
                        out.append("u");
                    }
                    break atom;
                }

                case 'd':
                case 'D':
                case 'w':
                case 'W':
                    // CharacterClassEscape
                    out.append('\\').append((char) get());
                    break atom;
                case 's':
                    mustMatch('s');
                    out.append(characterClass_s);
                    break atom;
                case 'S':
                    mustMatch('S');
                    out.append(characterClass_S);
                    break atom;

                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9': {
                    // DecimalEscape
                    int start = pos;
                    int num = get() - '0';
                    if (num == 0) {
                        // "\0" or octal sequence
                        // NB: compliant to Spidermonkey/JSC; V8 only allows three characters in
                        // octal escape sequence, that means "\0000" === "\0" + "0" in V8, whereas
                        // "\0000" === "\0" in Spidermonkey/JSC (restriction applies to Strings and
                        // RegExps!)
                        while (num <= 037) {
                            int d = peek(0);
                            if (!(d >= '0' && d <= '7')) {
                                break;
                            }
                            num = num * 8 + (get() - '0');
                        }
                        // output "\0" as "\00"
                        out.append("\\0").append(Integer.toOctalString(num));
                        break atom;
                    } else {
                        // back-reference or invalid escape
                        for (;;) {
                            int d = peek(0);
                            if (!(d >= '0' && d <= '9')) {
                                break;
                            }
                            num = num * 10 + (get() - '0');
                            if (num > BACKREF_LIMIT) {
                                num = BACKREF_LIMIT;
                                break;
                            }
                        }
                        if (num > backreflimit) {
                            // roll back to start of decimal escape
                            pos = start;
                            if (num == 8 || num == 9) {
                                // case 1 (\8 or \9): invalid octal escape sequence
                                out.append("\\\\");
                            } else {
                                // case 2: octal escape sequence
                                num = get() - '0';
                                while (num <= 037) {
                                    int d = peek(0);
                                    if (!(d >= '0' && d <= '7')) {
                                        break;
                                    }
                                    num = num * 8 + (get() - '0');
                                }
                                out.append("\\0").append(Integer.toOctalString(num));
                            }
                            break atom;
                        }
                        if (num > backrefmax) {
                            backrefmax = num;
                        }
                        if (num <= groups && validGroups.get(num)) {
                            out.append('\\').append(num);
                        } else {
                            // omit forward reference (TODO: check this!) or backward reference into
                            // capturing group from negative lookahead
                        }
                        break atom;
                    }
                    /* unreachable */
                }

                default: {
                    // CharacterEscape :: IdentityEscape
                    int d = get();
                    if (d == -1) {
                        throw error(Messages.Key.RegExpTrailingSlash);
                    } else if ((d >= 'a' && d <= 'z') || (d >= 'A' && d <= 'Z')) {
                        // don't escape alpha chars
                        out.append((char) d);
                    } else {
                        // identity escape
                        out.append('\\').append((char) d);
                    }
                    break atom;
                }
                }
                // assert false : "not reached";
            }

            case '(': {
                boolean negativeLA = false;
                if (match('?')) {
                    // (?=X) or (?!X) or (?:X)
                    int d = get();
                    switch (d) {
                    case '!':
                        negativeLA = true;
                        // fall-through
                    case '=':
                    case ':':
                        out.append("(?").append((char) d);
                        break;
                    default:
                        throw error(Messages.Key.RegExpInvalidQualifier);
                    }
                    if (negativeLA) {
                        int g = groups;
                        disjunction(depth + 1, negativedepth + 1);
                        // invalidate all capturing groups created within the negative lookahead
                        for (int v = groups; v != g; --v) {
                            validGroups.clear(v);
                        }
                    } else {
                        disjunction(depth + 1, negativedepth);
                    }
                } else {
                    out.append('(');
                    int g = ++groups;
                    disjunction(depth + 1, negativedepth);
                    validGroups.set(g);
                    if (negativedepth > 0) {
                        negativeLAGroups.set(g);
                    }
                }
                out.append((char) mustMatch(')'));
                break atom;
            }

            case ')': {
                if (depth > 0) {
                    // -1 so caller can match ')' again
                    pos -= 1;
                    return;
                }
                throw error(Messages.Key.RegExpUnmatchedCharacter, ")");
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
                    out.append((char) mustMatch(']'));
                } else if (!negation) {
                    // empty character class: []
                    out.append(emptyCharacterClass);
                } else {
                    // empty character class: [^]
                    out.append(emptyNegCharacterClass);
                }
                break atom;
            }

            case '*':
            case '+':
            case '?':
                throw error(Messages.Key.RegExpInvalidQualifier);

            case '{': {
                // make web-reality aware
                out.append('\\').append((char) c);
                int start = pos;
                if (decimal() < 0) {
                    pos = start;
                    break atom;
                }
                if (match(',') && peek(0) != '}') {
                    if (decimal() < 0) {
                        pos = start;
                        break atom;
                    }
                }
                if (!match('}')) {
                    pos = start;
                    break atom;
                }
                // parsed quantifier, but there was no applicable atom -> error!
                throw error(Messages.Key.RegExpInvalidQualifier);
            }

            case ']':
            case '}':
                // web-reality
                out.append('\\').append((char) c);
                break atom;

            case '.':
                // fall-through
            default:
                // PatternCharacter
                out.append((char) c);
                break atom;
            }

            /* Quantifier (optional) */
            // Greedy/Reluctant quantifiers
            quantifier: switch (peek(0)) {
            case '*':
            case '+':
            case '?':
                out.append((char) get());
                break quantifier;
            case '{': {
                // make web-reality aware
                int start = pos;
                mustMatch('{');
                int min = decimal();
                if (min < 0) {
                    pos = start;
                    continue term;
                }
                boolean comma;
                int max = -1;
                if ((comma = match(',')) && peek(0) != '}') {
                    max = decimal();
                    if (max < 0) {
                        pos = start;
                        continue term;
                    }
                }
                if (!match('}')) {
                    pos = start;
                    continue term;
                }
                if (max != -1 && min > max) {
                    throw error(Messages.Key.RegExpInvalidQualifier);
                }

                // output result
                out.append('{').append(Integer.toString(min));
                if (comma) {
                    if (max != -1) {
                        out.append(',').append(Integer.toString(max));
                    } else {
                        out.append(',');
                    }
                }
                out.append('}');
                break quantifier;
            }
            default:
                continue term;
            }

            // Reluctant quantifiers
            if (match('?')) {
                out.append('?');
            }

            continue term;
        }
    }
}
