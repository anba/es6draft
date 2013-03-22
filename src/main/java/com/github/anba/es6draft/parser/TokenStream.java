/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.parser.NumberParser.parseBinary;
import static com.github.anba.es6draft.parser.NumberParser.parseDecimal;
import static com.github.anba.es6draft.parser.NumberParser.parseHex;
import static com.github.anba.es6draft.parser.NumberParser.parseOctal;

import java.util.Arrays;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * Lexer for ECMAScript6 source code
 */
public class TokenStream {
    private static final boolean DEBUG = false;

    private final Parser parser;
    private final TokenStreamInput input;
    private int line;
    // stream position
    private int position;
    private int nextposition;
    // token data
    public Token current;
    public Token next;
    private boolean hasLineTerminator;
    private boolean hasCurrentLineTerminator;
    // literal data
    private StringBuffer buffer = new StringBuffer();
    private String string = null;
    private double number = 0;
    private boolean hasEscape = false;

    private static final class StringBuffer {
        char[] cbuf = new char[512];
        int length = 0;

        void clear() {
            length = 0;
        }

        void add(int c) {
            int len = length;
            if (len == cbuf.length) {
                cbuf = Arrays.copyOf(cbuf, len << 1);
            }
            cbuf[len] = (char) c;
            length = len + 1;
        }

        void addCodepoint(int c) {
            if (c > 0xFFFF) {
                add(Character.highSurrogate(c));
                add(Character.lowSurrogate(c));
            } else {
                add(c);
            }
        }

        @Override
        public String toString() {
            return new String(cbuf, 0, length);
        }
    }

    private StringBuffer buffer() {
        StringBuffer buffer = this.buffer;
        buffer.clear();
        return buffer;
    }

    public TokenStream(Parser parser, TokenStreamInput input, int line) {
        this.parser = parser;
        this.input = input;
        this.line = line;
    }

    public int position() {
        return position;
    }

    public String range(int from, int to) {
        return input.range(from, to);
    }

    public long marker() {
        return ((long) line << 32) | position;
    }

    public void init() {
        this.hasLineTerminator = false;
        this.hasCurrentLineTerminator = true;
        this.position = input.position();
        this.current = scanTokenNoComment();
        this.nextposition = input.position();
        this.next = null;
    }

    public void reset(long marker) {
        input.reset((int) marker);
        this.line = (int) (marker >>> 32);
        //
        this.hasLineTerminator = false;
        this.hasCurrentLineTerminator = true;
        this.position = input.position();
        this.current = scanTokenNoComment();
        this.nextposition = input.position();
        this.next = null;
    }

    public String getString() {
        if (string == null) {
            string = buffer.toString();
        }
        return string;
    }

    public boolean hasEscape() {
        return hasEscape;
    }

    public double getNumber() {
        return number;
    }

    public int getLine() {
        return line;
    }

    //

    public Token nextToken() {
        if (next == null) {
            hasLineTerminator = false;
            nextposition = input.position();
            next = scanTokenNoComment();
        }
        if (hasLineTerminator) {
            line += 1;
        }
        current = next;
        position = nextposition;
        hasCurrentLineTerminator = hasLineTerminator;
        string = null;
        next = null;
        nextposition = input.position();
        hasLineTerminator = false;
        return current;
    }

    public Token currentToken() {
        return current;
    }

    public Token peekToken() {
        assert !(current == Token.DIV || current == Token.ASSIGN_DIV);
        if (next == null) {
            hasLineTerminator = false;
            if (current == Token.NAME || current == Token.STRING) {
                string = getString();
            }
            nextposition = input.position();
            next = scanTokenNoComment();
        }
        return next;
    }

    public boolean hasCurrentLineTerminator() {
        assert current != null;
        return hasCurrentLineTerminator;
    }

    public boolean hasNextLineTerminator() {
        assert next != null;
        return hasLineTerminator;
    }

    //

    public String[] readRegularExpression(Token start) {
        assert start == Token.DIV || start == Token.ASSIGN_DIV;
        assert next == null : "regular expression in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuffer buffer = buffer();
        if (start == Token.ASSIGN_DIV) {
            buffer.add('=');
        } else {
            int c = peek();
            if (c == '/' || c == '*') {
                throw error(Messages.Key.InvalidRegExpLiteral);
            }
        }
        boolean inClass = false;
        for (;;) {
            int c = input.get();
            if (c == '\\') {
                // escape sequence
                buffer.add(c);
                c = input.get();
            } else if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '/' && !inClass) {
                break;
            }
            if (c == EOF || isLineTerminator(c)) {
                throw error(Messages.Key.UnterminatedRegExpLiteral);
            }
            buffer.add(c);
        }
        String regexp = buffer.toString();

        buffer.clear();
        for (;;) {
            int c = input.get();
            if (!isIdentifierPart(c)) {
                input.unget(c);
                break;
            }
            buffer.add(c);
        }

        String flags = buffer.toString();
        return new String[] { regexp, flags };
    }

    //

    /**
     * <strong>[7.8.6] Template Literal Lexical Components</strong>
     * 
     * <pre>
     * Template ::
     *     NoSubstitutionTemplate 
     *     TemplateHead
     * NoSubstitutionTemplate ::
     *     ` TemplateCharacters<sub>opt</sub>`
     * TemplateHead ::
     *     ` TemplateCharacters<sub>opt</sub>${
     * TemplateSubstitutionTail ::
     *     TemplateMiddle 
     *     TemplateTail
     * TemplateMiddle ::
     *     } TemplateCharacters<sub>opt</sub>${
     * TemplateTail ::
     *     } TemplateCharacters<sub>opt</sub>`
     * TemplateCharacters ::
     *     TemplateCharacter TemplateCharacters<sub>opt</sub>
     * TemplateCharacter ::
     *     SourceCharacter but not one of ` or \ or $ 
     *     $ [LA &#x2209; { ]
     *     \ EscapeSequence
     *     LineContinuation
     * </pre>
     */
    public String[] readTemplateLiteral(Token start) {
        assert start == Token.TEMPLATE || start == Token.RC;
        assert currentToken() == start;
        assert next == null : "template literal in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuffer buffer = buffer();
        int pos = input.position();
        for (;;) {
            int c = input.get();
            if (c == EOF) {
                throw error(Messages.Key.UnterminatedTemplateLiteral);
            }
            if (c == '`') {
                current = Token.TEMPLATE;
                String raw = input.range(pos, input.position() - 1);
                return new String[] { buffer.toString(), raw };
            }
            if (c == '$' && match('{')) {
                current = Token.LC;
                String raw = input.range(pos, input.position() - 2);
                return new String[] { buffer.toString(), raw };
            }
            if (c != '\\') {
                if (isLineTerminator(c)) {
                    // line continuation
                    if (c == '\r' && match('\n')) {
                        // \r\n sequence
                        buffer.add('\r');
                        buffer.add('\n');
                    } else {
                        buffer.add(c);
                    }
                    line += 1;
                    continue;
                }
                // TODO: add substring range
                buffer.add(c);
                continue;
            }

            // EscapeSequence
            if (isLineTerminator(c)) {
                // line continuation
                if (c == '\r' && match('\n')) {
                    // \r\n sequence
                }
                line += 1;
                continue;
            }
            switch (c) {
            case 'b':
                c = '\b';
                break;
            case 'f':
                c = '\f';
                break;
            case 'n':
                c = '\n';
                break;
            case 'r':
                c = '\r';
                break;
            case 't':
                c = '\t';
                break;
            case 'v':
                c = '\u000B';
                break;
            case '0':
                if (isDecimalDigit(peek())) {
                    throw error(Messages.Key.InvalidNULLEscape);
                }
                c = '\0';
                break;
            case 'x':
                c = (hexDigit(input.get()) << 4) | hexDigit(input.get());
                if (c < 0) {
                    throw error(Messages.Key.InvalidHexEscape);
                }
                break;
            case 'u':
                c = readUnicode();
                break;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                parser.reportStrictModeSyntaxError(Messages.Key.StrictModeOctalEscapeSequence);
                // fall-through
            case '"':
            case '\'':
            case '\\':
            default:
                // fall-through
            }
            buffer.addCodepoint(c);
        }
    }

    //

    private Token scanTokenNoComment() {
        Token tok;
        do {
            tok = scanToken();
        } while (tok == Token.COMMENT);
        return tok;
    }

    private Token scanToken() {
        TokenStreamInput input = this.input;

        int c;
        for (;;) {
            c = input.get();
            if (c == TokenStreamInput.EOF) {
                return Token.EOF;
            } else if (c <= 0x20) {
                if (c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20) {
                    // skip over whitespace
                    continue;
                }
                if (c == '\n') {
                    if (hasLineTerminator)
                        line += 1;
                    hasLineTerminator = true;
                    continue;
                }
                if (c == '\r') {
                    match('\n');
                    if (hasLineTerminator)
                        line += 1;
                    hasLineTerminator = true;
                    continue;
                }
            } else if (c >= 0xA0) {
                if (isWhitespace(c)) {
                    // skip over whitespace
                    continue;
                }
                if (isLineTerminator(c)) {
                    if (hasLineTerminator)
                        line += 1;
                    hasLineTerminator = true;
                    continue;
                }
            }
            break;
        }
        if (DEBUG)
            System.out.printf("scanToken() -> %c\n", (char) c);

        switch (c) {
        case '\'':
        case '"':
            return readString(c);
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return readNumberLiteral(c);
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
        case 'H':
        case 'I':
        case 'J':
        case 'K':
        case 'L':
        case 'M':
        case 'N':
        case 'O':
        case 'P':
        case 'Q':
        case 'R':
        case 'S':
        case 'T':
        case 'U':
        case 'V':
        case 'W':
        case 'X':
        case 'Y':
        case 'Z':
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
        case 'g':
        case 'h':
        case 'i':
        case 'j':
        case 'k':
        case 'l':
        case 'm':
        case 'n':
        case 'o':
        case 'p':
        case 'q':
        case 'r':
        case 's':
        case 't':
        case 'u':
        case 'v':
        case 'w':
        case 'x':
        case 'y':
        case 'z':
        case '$':
        case '_':
            return readIdentifier(c);
        case '{':
            return Token.LC;
        case '}':
            return Token.RC;
        case '(':
            return Token.LP;
        case ')':
            return Token.RP;
        case '[':
            return Token.LB;
        case ']':
            return Token.RB;
        case '.':
            switch (peek()) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return readNumberLiteral(c);
            case '.':
                if (peek2() == '.') {
                    mustMatch('.');
                    mustMatch('.');
                    return Token.TRIPLE_DOT;
                }
            }
            return Token.DOT;
        case ';':
            return Token.SEMI;
        case ',':
            return Token.COMMA;
        case '~':
            return Token.BITNOT;
        case '?':
            return Token.HOOK;
        case ':':
            return Token.COLON;
        case '<':
            if (match('<')) {
                if (match('=')) {
                    return Token.ASSIGN_SHL;
                } else {
                    return Token.SHL;
                }
            } else if (match('=')) {
                return Token.LE;
            } else {
                return Token.LT;
            }
        case '>':
            if (match('>')) {
                if (match('>')) {
                    if (match('=')) {
                        return Token.ASSIGN_USHR;
                    } else {
                        return Token.USHR;
                    }
                } else if (match('=')) {
                    return Token.ASSIGN_SHR;
                } else {
                    return Token.SHR;
                }
            } else if (match('=')) {
                return Token.GE;
            } else {
                return Token.GT;
            }
        case '=':
            if (match('=')) {
                if (match('=')) {
                    return Token.SHEQ;
                } else {
                    return Token.EQ;
                }
            } else if (match('>')) {
                return Token.ARROW;
            } else {
                return Token.ASSIGN;
            }
        case '!':
            if (match('=')) {
                if (match('=')) {
                    return Token.SHNE;
                } else {
                    return Token.NE;
                }
            } else {
                return Token.NOT;
            }
        case '+':
            if (match('+')) {
                return Token.INC;
            } else if (match('=')) {
                return Token.ASSIGN_ADD;
            } else {
                return Token.ADD;
            }
        case '-':
            if (match('-')) {
                return Token.DEC;
            } else if (match('=')) {
                return Token.ASSIGN_SUB;
            } else {
                return Token.SUB;
            }
        case '*':
            if (match('=')) {
                return Token.ASSIGN_MUL;
            } else {
                return Token.MUL;
            }
        case '%':
            if (match('=')) {
                return Token.ASSIGN_MOD;
            } else {
                return Token.MOD;
            }
        case '/':
            if (match('=')) {
                return Token.ASSIGN_DIV;
            } else if (match('/')) {
                readSingleComment();
                return Token.COMMENT;
            } else if (match('*')) {
                readMultiComment();
                return Token.COMMENT;
            } else {
                return Token.DIV;
            }
        case '&':
            if (match('&')) {
                return Token.AND;
            } else if (match('=')) {
                return Token.ASSIGN_BITAND;
            } else {
                return Token.BITAND;
            }
        case '|':
            if (match('|')) {
                return Token.OR;
            } else if (match('=')) {
                return Token.ASSIGN_BITOR;
            } else {
                return Token.BITOR;
            }
        case '^':
            if (match('=')) {
                return Token.ASSIGN_BITXOR;
            } else {
                return Token.BITXOR;
            }
        case '`':
            return Token.TEMPLATE;
        }

        if (c == '\\') {
            mustMatch('u');
            c = readUnicode();
        }
        if (isIdentifierStart(c)) {
            return readIdentifier(c);
        }

        return Token.ERROR;
    }

    private static boolean isIdentifierStart(int c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '$' || c == '_')
            return true;
        if (c <= 127)
            return false;
        // cf. http://www.unicode.org/reports/tr31/ for definition of "UnicodeIDStart"
        switch (Character.getType(c)) {
        case Character.UPPERCASE_LETTER:
        case Character.LOWERCASE_LETTER:
        case Character.TITLECASE_LETTER:
        case Character.MODIFIER_LETTER:
        case Character.OTHER_LETTER:
        case Character.LETTER_NUMBER:
            return true;
        }
        return false;
    }

    private static boolean isIdentifierPart(int c) {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '$'
                || c == '_')
            return true;
        if (c <= 127)
            return false;
        if (c == '\u200C' || c == '\u200D')
            return true;
        // cf. http://www.unicode.org/reports/tr31/ for definition of "UnicodeIDContinue"
        switch (Character.getType(c)) {
        case Character.UPPERCASE_LETTER:
        case Character.LOWERCASE_LETTER:
        case Character.TITLECASE_LETTER:
        case Character.MODIFIER_LETTER:
        case Character.OTHER_LETTER:
        case Character.LETTER_NUMBER:
        case Character.NON_SPACING_MARK:
        case Character.COMBINING_SPACING_MARK:
        case Character.DECIMAL_DIGIT_NUMBER:
        case Character.CONNECTOR_PUNCTUATION:
            return true;
        }
        return false;
    }

    /**
     * <strong>[7.2] White Space</strong>
     * 
     * <pre>
     * WhiteSpace ::
     *     &lt;TAB>  (U+0009)
     *     &lt;VT>   (U+000B)
     *     &lt;FF>   (U+000C)
     *     &lt;SP>   (U+0020)
     *     &lt;NBSP> (U+00A0)
     *     &lt;BOM>  (U+FEFF)
     *     &lt;USP>  ("Zs")
     * </pre>
     */
    private static boolean isWhitespace(int c) {
        return (c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20 || c == 0xA0 || c == 0xFEFF || isSpaceSeparator(c));
    }

    /**
     * Unicode category "Zs" (space separator)
     */
    private static boolean isSpaceSeparator(int c) {
        return (c == 0x20 || c == 0xA0 || c == 0x1680 || c == 0x180E
                || (c >= 0x2000 && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000);
    }

    /**
     * <strong>[7.3] Line Terminators</strong>
     * 
     * <pre>
     * LineTerminator ::
     *     &lt;LF> (U+000A)
     *     &lt;CR> (U+000D)
     *     &lt;LS> (U+2028)
     *     &lt;PS> (U+2029)
     * </pre>
     */
    private static boolean isLineTerminator(int c) {
        if ((c & ~0b0010_0000_0010_1111) != 0) {
            return false;
        }
        return (c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029);
    }

    private Token readSingleComment() {
        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        for (;;) {
            int c = input.get();
            if (c == EOF) {
                break;
            }
            if (isLineTerminator(c)) {
                // EOL is not part of the single-line comment!
                input.unget(c);
                break;
            }
        }
        return Token.COMMENT;
    }

    private Token readMultiComment() {
        final int EOF = TokenStreamInput.EOF;
        @SuppressWarnings("unused")
        int start = line;
        TokenStreamInput input = this.input;
        loop: for (;;) {
            int c = input.get();
            while (c == '*') {
                if ((c = input.get()) == '/')
                    break loop;
            }
            if (isLineTerminator(c)) {
                if (c == '\r') {
                    match('\n');
                }
                if (hasLineTerminator)
                    line += 1;
                hasLineTerminator = true;
            }
            if (c == EOF) {
                throw error(Messages.Key.UnterminatedComment);
            }
        }
        return Token.COMMENT;
    }

    private Token readIdentifier(int c) {
        assert isIdentifierStart(c);

        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        buffer.addCodepoint(c);
        for (;;) {
            c = input.get();
            if (isIdentifierPart(c)) {
                buffer.add(c);
            } else if (c == '\\') {
                mustMatch('u');
                c = readUnicode();
                if (!isIdentifierPart(c)) {
                    throw error(Messages.Key.InvalidUnicodeEscapedIdentifierPart);
                }
                buffer.addCodepoint(c);
                continue;
            } else {
                input.unget(c);
                break;
            }
        }

        Token tok = keywordOrLiteral(buffer);
        if (tok != null) {
            return tok;
        }
        return Token.NAME;
    }

    private int readUnicode() {
        TokenStreamInput input = this.input;
        int c = input.get();
        if (c == '{') {
            int acc = 0;
            c = input.get();
            do {
                acc = (acc << 4) | hexDigit(c);
            } while ((acc >= 0 && acc < 0x10FFFF) && (c = input.get()) != '}');
            if (c == '}') {
                c = acc;
            } else {
                c = -1;
            }
        } else {
            c = (hexDigit(c) << 12) | (hexDigit(input.get()) << 8) | (hexDigit(input.get()) << 4)
                    | hexDigit(input.get());
        }
        if (c < 0 || c > 0x10FFFF) {
            throw error(Messages.Key.InvalidUnicodeEscape);
        }
        return c;
    }

    private Token keywordOrLiteral(StringBuffer buffer) {
        int length = buffer.length;
        if (length < 2 || length > 10)
            return null;
        char[] cbuf = buffer.cbuf;
        char c0 = cbuf[0], c1 = cbuf[1];
        Token test = null;
        switch (c0) {
        case 'b':
            // break
            if (length == 5)
                test = Token.BREAK;
            break;
        case 'c':
            // case, catch, continue, class, const
            if (length == 4)
                test = Token.CASE;
            else if (length == 5)
                test = (c1 == 'a' ? Token.CATCH : c1 == 'l' ? Token.CLASS : Token.CONST);
            else if (length == 8)
                test = Token.CONTINUE;
            break;
        case 'd':
            // debugger, default, delete, do
            if (length == 2)
                test = Token.DO;
            else if (length == 6)
                test = Token.DELETE;
            else if (length == 7)
                test = Token.DEFAULT;
            else if (length == 8)
                test = Token.DEBUGGER;
            break;
        case 'e':
            // else, enum, export, extends
            if (length == 4)
                test = (c1 == 'l' ? Token.ELSE : Token.ENUM);
            else if (length == 6)
                test = Token.EXPORT;
            else if (length == 7)
                test = Token.EXTENDS;
            break;
        case 'f':
            // finally, for, function, false
            if (length == 3)
                test = Token.FOR;
            else if (length == 5)
                test = Token.FALSE;
            else if (length == 7)
                test = Token.FINALLY;
            else if (length == 8)
                test = Token.FUNCTION;
            break;
        case 'i':
            // if, in, instanceof, import, implements, interface
            if (length == 2)
                test = (c1 == 'f' ? Token.IF : Token.IN);
            else if (length == 6)
                test = Token.IMPORT;
            else if (length == 9)
                test = Token.INTERFACE;
            else if (length == 10)
                test = (c1 == 'n' ? Token.INSTANCEOF : Token.IMPLEMENTS);
            break;
        case 'l':
            // let
            if (length == 3)
                test = Token.LET;
            break;
        case 'n':
            // new, null
            if (length == 3)
                test = Token.NEW;
            else if (length == 4)
                test = Token.NULL;
            break;
        case 'p':
            // package, private, protected, public
            if (length == 6)
                test = Token.PUBLIC;
            else if (length == 7)
                test = (c1 == 'a' ? Token.PACKAGE : Token.PRIVATE);
            else if (length == 9)
                test = Token.PROTECTED;
            break;
        case 'r':
            // return
            if (length == 6)
                test = Token.RETURN;
            break;
        case 's':
            // switch, super, static
            if (length == 5)
                test = Token.SUPER;
            else if (length == 6)
                test = (c1 == 'w' ? Token.SWITCH : Token.STATIC);
            break;
        case 't':
            // this, throw, try, typeof, true
            if (length == 3)
                test = Token.TRY;
            else if (length == 4)
                test = (c1 == 'h' ? Token.THIS : Token.TRUE);
            else if (length == 5)
                test = Token.THROW;
            else if (length == 6)
                test = Token.TYPEOF;
            break;
        case 'v':
            // var, void
            if (length == 3)
                test = Token.VAR;
            else if (length == 4)
                test = Token.VOID;
            break;
        case 'w':
            // while, with
            if (length == 4)
                test = Token.WITH;
            else if (length == 5)
                test = Token.WHILE;
            break;
        case 'y':
            // yield
            if (length == 5)
                test = Token.YIELD;
            break;
        }
        if (test != null && equals(cbuf, test.getName())) {
            return test;
        }
        return null;
    }

    private static boolean equals(char[] cbuf, String test) {
        for (int i = 0, length = test.length(); i < length; ++i) {
            if (cbuf[i] != test.charAt(i))
                return false;
        }
        return true;
    }

    private Token readString(int quoteChar) {
        assert quoteChar == '"' || quoteChar == '\'';

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        hasEscape = false;
        for (;;) {
            int c = input.get();
            if (c == EOF) {
                throw error(Messages.Key.UnterminatedStringLiteral);
            }
            if (c == quoteChar) {
                break;
            }
            if (isLineTerminator(c)) {
                throw error(Messages.Key.UnterminatedStringLiteral);
            }
            if (c != '\\') {
                // TODO: add substring range
                buffer.add(c);
                continue;
            }
            hasEscape = true;
            c = input.get();
            if (isLineTerminator(c)) {
                // line continuation
                if (c == '\r' && match('\n')) {
                    // \r\n sequence
                }
                line += 1;
                continue;
            }
            // escape sequences
            switch (c) {
            case 'b':
                c = '\b';
                break;
            case 'f':
                c = '\f';
                break;
            case 'n':
                c = '\n';
                break;
            case 'r':
                c = '\r';
                break;
            case 't':
                c = '\t';
                break;
            case 'v':
                c = '\u000B';
                break;
            case '0':
                if (isDecimalDigit(peek())) {
                    throw error(Messages.Key.InvalidNULLEscape);
                }
                c = '\0';
                break;
            case 'x':
                c = (hexDigit(input.get()) << 4) | hexDigit(input.get());
                if (c < 0) {
                    throw error(Messages.Key.InvalidHexEscape);
                }
                break;
            case 'u':
                c = readUnicode();
                break;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                parser.reportStrictModeSyntaxError(Messages.Key.StrictModeOctalEscapeSequence);
                // fall-through
            case '"':
            case '\'':
            case '\\':
            default:
                // fall-through
            }
            buffer.addCodepoint(c);
        }

        return Token.STRING;
    }

    private Token readNumberLiteral(int c) {
        if (c == '0') {
            int d = input.get();
            if (d == 'x' || d == 'X') {
                number = readHexIntegerLiteral();
            } else if (d == 'b' || d == 'B') {
                number = readBinaryIntegerLiteral();
            } else if (d == 'o' || d == 'O') {
                number = readOctalIntegerLiteral();
            } else if (isDecimalDigit(d)) {
                parser.reportStrictModeSyntaxError(Messages.Key.StrictModeOctalIntegerLiteral);
                input.unget(d);
                number = readOctalIntegerLiteral();
            } else {
                input.unget(d);
                number = readDecimalLiteral(c);
            }
        } else {
            number = readDecimalLiteral(c);
        }
        return Token.NUMBER;
    }

    private double readHexIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isHexDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        return parseHex(buffer.cbuf, buffer.length);
    }

    private double readBinaryIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isBinaryDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidBinaryIntegerLiteral);
        }
        return parseBinary(buffer.cbuf, buffer.length);
    }

    private double readOctalIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isOctalDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        return parseOctal(buffer.cbuf, buffer.length);
    }

    private double readDecimalLiteral(int c) {
        assert c == '.' || isDecimalDigit(c);
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        if (c != '.' && c != '0') {
            buffer.add(c);
            while (isDecimalDigit(c = input.get())) {
                buffer.add(c);
            }
        } else if (c == '0') {
            buffer.add(c);
            c = input.get();
        }
        if (c == '.') {
            buffer.add(c);
            while (isDecimalDigit(c = input.get())) {
                buffer.add(c);
            }
        }
        if (c == 'e' || c == 'E') {
            buffer.add(c);
            c = input.get();
            if (c == '+' || c == '-') {
                buffer.add(c);
                c = input.get();
            }
            if (!isDecimalDigit(c)) {
                throw error(Messages.Key.InvalidNumberLiteral);
            }
            buffer.add(c);
            while (isDecimalDigit(c = input.get())) {
                buffer.add(c);
            }
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        input.unget(c);
        return parseDecimal(buffer.cbuf, buffer.length);
    }

    private boolean isDecimalDigitOrIdentifierStart(int c) {
        return (c >= '0' && c <= '9') || isIdentifierStart(c);
    }

    private static boolean isDecimalDigit(int c) {
        return (c >= '0' && c <= '9');
    }

    private static boolean isBinaryDigit(int c) {
        return (c == '0' || c == '1');
    }

    private static boolean isOctalDigit(int c) {
        return (c >= '0' && c <= '7');
    }

    private static boolean isHexDigit(int c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
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

    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, line, messageKey, args);
    }

    private boolean match(int c) {
        int d = input.get();
        if (c == d) {
            return true;
        }
        input.unget(d);
        return false;
    }

    private void mustMatch(int c) {
        if (!match(c)) {
            throw error(Messages.Key.UnexpectedCharacter, String.valueOf((char) c));
        }
    }

    private int peek() {
        int c = input.get();
        input.unget(c);
        return c;
    }

    private int peek2() {
        int c = input.get();
        int d = input.get();
        input.unget(d);
        input.unget(c);
        return d;
    }
}
