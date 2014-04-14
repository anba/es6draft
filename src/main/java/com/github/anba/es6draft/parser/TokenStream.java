/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.parser.NumberParser.*;

import java.util.Arrays;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * Lexer for ECMAScript 6 source code
 * <ul>
 * <li>10 ECMAScript Language: Source Code
 * <li>11 ECMAScript Language: Lexical Grammar
 * </ul>
 */
public final class TokenStream {
    private final Parser parser;
    private final TokenStreamInput input;

    /** current line number */
    private int line;
    /** start position of current line */
    private int linestart;
    /** start position of current token, includes leading whitespace and comments */
    private int position;
    /** start position of next token, includes leading whitespace and comments */
    private int nextposition;

    // token data
    /** current token in stream */
    private Token current;
    /** next token in stream */
    private Token next;
    /** line terminator preceding current token? */
    private boolean hasCurrentLineTerminator;
    /** line terminator preceding next token? */
    private boolean hasLineTerminator;
    /** start line/column info for current token */
    private long sourcePosition;
    /** start line/column info for next token */
    private long nextSourcePosition;

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

        void add(String s) {
            int len = length;
            int newlen = len + s.length();
            if (newlen > cbuf.length) {
                cbuf = Arrays.copyOf(cbuf, Integer.highestOneBit(newlen) << 1);
            }
            s.getChars(0, s.length(), cbuf, len);
            length = newlen;
        }

        @Override
        public String toString() {
            return new String(cbuf, 0, length);
        }
    }

    /**
     * Resets and returns the internal character buffer.
     * 
     * @return the character buffer
     */
    private StringBuffer buffer() {
        StringBuffer buffer = this.buffer;
        buffer.clear();
        return buffer;
    }

    /**
     * Updates line state information for line breaks within literals, does <strong>not</strong> set
     * the {@link #hasLineTerminator} flag.
     */
    private void incrementLine() {
        line += 1;
        linestart = input.position();
    }

    /**
     * Updates the line state information, must not be used for line breaks within literals.
     */
    private void incrementLineAndUpdate() {
        line += 1;
        linestart = input.position();
        hasLineTerminator = true;
    }

    /**
     * Sets the source position (line / column information) for the next token.
     */
    private void updateSourcePosition() {
        nextSourcePosition = ((long) (input.position() - linestart) << 32) | line;
    }

    /**
     * Public constructor, token stream still needs to be initialised by calling the
     * {@link #initialise()} method.
     * 
     * @param parser
     *            the parser instance
     * @param input
     *            the token stream instance
     */
    public TokenStream(Parser parser, TokenStreamInput input) {
        this.parser = parser;
        this.input = input;
    }

    /**
     * Return the start position of current token, includes leading whitespace and comments. Also
     * needed to reset the token stream.
     * 
     * @return the token start position
     * @see #reset(long, long)
     */
    public int position() {
        return position;
    }

    /**
     * Returns the raw source characters from the underlying input source.
     * 
     * @param from
     *            the start position (inclusive)
     * @param to
     *            the end position (exclusive)
     * @return the source characters in the given range
     */
    public String range(int from, int to) {
        return input.range(from, to);
    }

    /**
     * Returns the encoded line information, needed to reset the token stream.
     * 
     * @return the current line information
     * @see #reset(long, long)
     */
    public long lineinfo() {
        return ((long) line << 32) | linestart;
    }

    /**
     * Returns the encoded line/column information of the current source position.
     * 
     * @return the current line/column information
     */
    public long sourcePosition() {
        return sourcePosition;
    }

    /**
     * Returns the encoded start line/column information for current token.
     * 
     * @return the begin line/column information
     */
    public long beginPosition() {
        return sourcePosition;
    }

    /**
     * Returns the encoded end line/column information for current token.
     * 
     * @return the end line/column information
     */
    public long endPosition() {
        // add one to make columns 1-indexed
        return ((long) (1 + position - linestart) << 32) | line;
    }

    /**
     * Initialises this token stream, needs to be called before fetching any tokens.
     * 
     * @return this token stream
     */
    public TokenStream initialise() {
        // set internal state to default values
        this.hasLineTerminator = true;
        this.hasCurrentLineTerminator = true;
        this.position = input.position();
        this.line = parser.getSourceLine();
        this.linestart = input.position();
        this.current = scanTokenNoComment();
        this.sourcePosition = nextSourcePosition;
        this.nextposition = input.position();
        this.next = null;
        return this;
    }

    /**
     * Resets this token stream to the requested position.
     * 
     * @param position
     *            the new position
     * @param lineinfo
     *            the new line information
     * @see #position()
     * @see #lineinfo()
     */
    public void reset(long position, long lineinfo) {
        // reset character stream
        input.reset((int) position);
        // reset internal state
        this.hasLineTerminator = false;
        this.hasCurrentLineTerminator = true;
        this.position = input.position();
        this.current = scanTokenNoComment();
        this.sourcePosition = nextSourcePosition;
        this.nextposition = input.position();
        this.next = null;
        // reset line state last, effectively ignoring any changes from scanTokenNoComment()
        this.line = (int) (lineinfo >>> 32);
        this.linestart = (int) lineinfo;
    }

    /**
     * Returns the string data of the current token.
     * 
     * @return the current string data
     */
    public String getString() {
        if (string == null) {
            string = buffer.toString();
        }
        return string;
    }

    /**
     * Returns <code>true</code> if the current token is a string literal which contains an escape
     * sequence.
     * 
     * @return {@code true} if the string literal contains an escape sequence
     */
    public boolean hasEscape() {
        return hasEscape;
    }

    /**
     * Returns the number data of the current token.
     * 
     * @return the current number data
     */
    public double getNumber() {
        return number;
    }

    /**
     * Returns the current line number.
     * 
     * @return the line number
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the current column number.
     * 
     * @return the column number
     */
    public int getColumn() {
        return input.position() - linestart;
    }

    //

    /**
     * Advances the token stream to the next token.
     * 
     * @return the next token
     */
    public Token nextToken() {
        if (next == null) {
            hasLineTerminator = false;
            nextposition = input.position();
            next = scanTokenNoComment();
        }
        current = next;
        sourcePosition = nextSourcePosition;
        position = nextposition;
        hasCurrentLineTerminator = hasLineTerminator;
        string = null;
        next = null;
        nextposition = input.position();
        hasLineTerminator = false;
        return current;
    }

    /**
     * Returns the current token.
     * 
     * @return the current token
     */
    public Token currentToken() {
        return current;
    }

    /**
     * Peeks the next token in this token stream.
     * 
     * @return the next token
     */
    public Token peekToken() {
        assert !(current == Token.DIV || current == Token.ASSIGN_DIV);
        if (next == null) {
            switch (current) {
            case NAME:
            case ESCAPED_NAME:
            case ESCAPED_RESERVED_WORD:
            case ESCAPED_STRICT_RESERVED_WORD:
            case ESCAPED_YIELD:
            case ESCAPED_LET:
            case STRING:
                string = getString();
            default:
            }
            hasLineTerminator = false;
            nextposition = input.position();
            next = scanTokenNoComment();
        }
        return next;
    }

    /**
     * Returns <code>true</code> if there is a line terminator before the current token.
     * 
     * @return {@code true} if there is a line terminator
     */
    public boolean hasCurrentLineTerminator() {
        assert current != null;
        return hasCurrentLineTerminator;
    }

    /**
     * Returns <code>true</code> if there is a line terminator before the next token.
     * 
     * @return {@code true} if there is a line terminator
     */
    public boolean hasNextLineTerminator() {
        assert next != null;
        return hasLineTerminator;
    }

    //

    /**
     * <strong>[11.8.5] Regular Expression Literals</strong>
     * 
     * <pre>
     * RegularExpressionLiteral ::
     *     / RegularExpressionBody / RegularExpressionFlags
     * RegularExpressionBody ::
     *     RegularExpressionFirstChar RegularExpressionChars
     * RegularExpressionChars ::
     *     [empty]
     *     RegularExpressionChars RegularExpressionChar
     * RegularExpressionFirstChar ::
     *     RegularExpressionNonTerminator but not one of * or \ or / or [
     *     RegularExpressionBackslashSequence
     *     RegularExpressionClass
     * RegularExpressionChar ::
     *     RegularExpressionNonTerminator but not one of \ or / or [
     *     RegularExpressionBackslashSequence
     *     RegularExpressionClass
     * RegularExpressionBackslashSequence ::
     *     \ RegularExpressionNonTerminator
     * RegularExpressionNonTerminator ::
     *     SourceCharacter but not LineTerminator
     * RegularExpressionClass ::
     *     [ RegularExpressionClassChars ]
     * RegularExpressionClassChars ::
     *     [empty]
     *     RegularExpressionClassChars RegularExpressionClassChar
     * RegularExpressionClassChar ::
     *     RegularExpressionNonTerminator but not one of ] or \
     *     RegularExpressionBackslashSequence
     * RegularExpressionFlags ::
     *     [empty]
     *     RegularExpressionFlags IdentifierPart
     * </pre>
     * 
     * @param start
     *            the start token of the regular expression literal, either {@link Token#DIV} or
     *            {@link Token#ASSIGN_DIV}
     * @return string tuple {pattern, flags} for the regular expression literal
     */
    public String[] readRegularExpression(Token start) {
        assert start == Token.DIV || start == Token.ASSIGN_DIV;
        assert next == null : "regular expression in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuffer buffer = buffer();
        if (start == Token.ASSIGN_DIV) {
            buffer.add('=');
        } else {
            int c = input.peek(0);
            if (c == '/' || c == '*') {
                throw error(Messages.Key.InvalidRegExpLiteral);
            }
        }
        boolean inClass = false;
        for (;;) {
            int c = input.getChar();
            if (c == '\\') {
                // escape sequence
                buffer.add(c);
                c = input.getChar();
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
                if (c == '\\' && match('u')) {
                    readUnicode();
                    throw error(Messages.Key.UnicodeEscapeInRegExpFlags);
                }
                input.unget(c);
                break;
            }
            buffer.addCodepoint(c);
        }

        String flags = buffer.toString();
        return new String[] { regexp, flags };
    }

    //

    /**
     * <strong>[11.8.6] Template Literal Lexical Components</strong>
     * 
     * <pre>
     * Template ::
     *     NoSubstitutionTemplate 
     *     TemplateHead
     * NoSubstitutionTemplate ::
     *     ` TemplateCharacters<span><sub>opt</sub></span>`
     * TemplateHead ::
     *     ` TemplateCharacters<span><sub>opt</sub></span>${
     * TemplateSubstitutionTail ::
     *     TemplateMiddle 
     *     TemplateTail
     * TemplateMiddle ::
     *     } TemplateCharacters<span><sub>opt</sub></span>${
     * TemplateTail ::
     *     } TemplateCharacters<span><sub>opt</sub></span>`
     * TemplateCharacters ::
     *     TemplateCharacter TemplateCharacters<span><sub>opt</sub></span>
     * TemplateCharacter ::
     *     SourceCharacter but not one of ` or \ or $ 
     *     $ [LA &#x2209; { ]
     *     \ EscapeSequence
     *     LineContinuation
     * </pre>
     * 
     * @param start
     *            the start token of the template literal, either {@link Token#TEMPLATE} or
     *            {@link Token#RC}
     * @return string tuple {cooked, raw} for the template literal
     */
    public String[] readTemplateLiteral(Token start) {
        assert start == Token.TEMPLATE || start == Token.RC;
        assert currentToken() == start;
        assert next == null : "template literal in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuilder raw = new StringBuilder();
        StringBuffer buffer = buffer();
        int pos = input.position();
        for (;;) {
            int c = input.getChar();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedTemplateLiteral);
            }
            if (c == '`') {
                current = Token.TEMPLATE;
                raw.append(input.range(pos, input.position() - 1));
                return new String[] { buffer.toString(), raw.toString() };
            }
            if (c == '$' && match('{')) {
                current = Token.LC;
                raw.append(input.range(pos, input.position() - 2));
                return new String[] { buffer.toString(), raw.toString() };
            }
            if (c != '\\') {
                if (isLineTerminator(c)) {
                    // line terminator sequence
                    if (c == '\r') {
                        // normalise \r and \r\n to \n
                        raw.append(input.range(pos, input.position() - 1)).append('\n');
                        match('\n');
                        pos = input.position();
                        c = '\n';
                    }
                    buffer.add(c);
                    incrementLine();
                    continue;
                }
                // TODO: add substring range
                buffer.add(c);
                continue;
            }

            c = input.getChar();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedTemplateLiteral);
            }
            // EscapeSequence
            if (isLineTerminator(c)) {
                // line continuation
                if (c == '\r') {
                    // normalise \r and \r\n to \n
                    raw.append(input.range(pos, input.position() - 1)).append('\n');
                    match('\n');
                    pos = input.position();
                }
                incrementLine();
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
                if (isDecimalDigit(input.peek(0))) {
                    throw error(Messages.Key.InvalidNULLEscape);
                }
                c = '\0';
                break;
            case 'x':
                c = (hexDigit(input.getChar()) << 4) | hexDigit(input.getChar());
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
            case '8':
            case '9':
                throw error(Messages.Key.StrictModeOctalEscapeSequence);
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

    /**
     * <strong>[11] ECMAScript Language: Lexical Grammar</strong>
     * 
     * <pre>
     * InputElementDiv ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     Token
     *     DivPunctuator
     *     RightBracePunctuator
     * InputElementRegExp ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     Token
     *     RightBracePunctuator
     *     RegularExpressionLiteral
     * InputElementTemplateTail ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     Token
     *     DivPunctuator
     *     TemplateSubstitutionTail
     * </pre>
     * 
     * @return the next token
     */
    private Token scanTokenNoComment() {
        Token tok;
        do {
            tok = scanToken();
        } while (tok == Token.COMMENT);
        return tok;
    }

    /**
     * <strong>[11.5] Token</strong>
     * 
     * <pre>
     * Token ::
     *     IdentifierName
     *     Punctuator
     *     NumericLiteral
     *     StringLiteral
     *     Template
     * </pre>
     * 
     * @return the next token
     */
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
                    incrementLineAndUpdate();
                    continue;
                }
                if (c == '\r') {
                    match('\n');
                    incrementLineAndUpdate();
                    continue;
                }
            } else if (c >= 0xA0) {
                if (isWhitespace(c)) {
                    // skip over whitespace
                    continue;
                }
                if (isLineTerminator(c)) {
                    incrementLineAndUpdate();
                    continue;
                }
            }
            break;
        }
        updateSourcePosition();

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
            return readIdentifier(c, false);
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
            switch (input.peek(0)) {
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
                if (input.peek(1) == '.') {
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
            } else if (input.peek(0) == '!' && input.peek(1) == '-' && input.peek(2) == '-'
                    && parser.isEnabled(CompatibilityOption.HTMLComments)) {
                // html start-comment
                mustMatch('!');
                mustMatch('-');
                mustMatch('-');
                readSingleLineComment();
                return Token.COMMENT;
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
                if (input.peek(0) == '>' && hasLineTerminator
                        && parser.isEnabled(CompatibilityOption.HTMLComments)) {
                    // html end-comment at line start
                    mustMatch('>');
                    readSingleLineComment();
                    return Token.COMMENT;
                }
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
                readSingleLineComment();
                return Token.COMMENT;
            } else if (match('*')) {
                readMultiLineComment();
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
        case '\\':
            mustMatch('u');
            c = readUnicode();
            if (isIdentifierStart(c)) {
                return readIdentifier(c, true);
            }
            return Token.ERROR;
        default:
            if (isIdentifierStart(c)) {
                return readIdentifier(c, false);
            }
            return Token.ERROR;
        }
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * IdentifierStart ::
     *     UnicodeIDStart
     *     $
     *     _
     *     \ UnicodeEscapeSequence
     * UnicodeIDStart ::
     *     any Unicode character with the Unicode property "ID_Start".
     * </pre>
     * 
     * @param c
     *            the character to inspect
     * @return {@code true} if the character is an identifier start character
     */
    private static boolean isIdentifierStart(int c) {
        if (c <= 127) {
            return ((c | 0x20) >= 'a' && (c | 0x20) <= 'z') || c == '$' || c == '_';
        }
        return isIdentifierStartUnlikely(c);
    }

    private static boolean isIdentifierStartUnlikely(int c) {
        // cf. http://www.unicode.org/reports/tr31/ for definition of "ID_Start"
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
        switch (Character.getType(c)) {
        case Character.UPPERCASE_LETTER:
        case Character.LOWERCASE_LETTER:
        case Character.TITLECASE_LETTER:
        case Character.MODIFIER_LETTER:
        case Character.OTHER_LETTER:
        case Character.LETTER_NUMBER:
            return true;
        }
        // Additional characters for ID_Start based on Unicode 5.1.
        // Also applies to Unicode 6.0 (Java 7), Unicode 6.2 (Java 8) and Unicode 6.3 (Current).
        switch (c) {
        case '\u2118':
        case '\u212E':
        case '\u309B':
        case '\u309C':
            return true;
        }
        return false;
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * IdentifierPart ::
     *     UnicodeIDContinue
     *     $
     *     _
     *     \ UnicodeEscapeSequence
     *     &lt;ZWNJ&gt;
     *     &lt;ZWJ&gt;
     * UnicodeIDContinue ::
     *     any Unicode character with the Unicode property "ID_Continue"
     * </pre>
     * 
     * @param c
     *            the character to inspect
     * @return {@code true} if the character is an identifier part character
     */
    private static boolean isIdentifierPart(int c) {
        if (c <= 127) {
            return ((c | 0x20) >= 'a' && (c | 0x20) <= 'z') || (c >= '0' && c <= '9') || c == '$'
                    || c == '_';
        }
        return isIdentifierPartUnlikely(c);
    }

    private static boolean isIdentifierPartUnlikely(int c) {
        if (c == '\u200C' || c == '\u200D')
            return true;
        // cf. http://www.unicode.org/reports/tr31/ for definition of "ID_Continue"
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
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
        // Additional characters for ID_Continue based on Unicode 5.1.
        switch (c) {
        case '\u00B7':
        case '\u0387':
        case '\u1369':
        case '\u136A':
        case '\u136B':
        case '\u136C':
        case '\u136D':
        case '\u136E':
        case '\u136F':
        case '\u1370':
        case '\u1371':
        case '\u2118':
        case '\u212E':
        case '\u309B':
        case '\u309C':
            return true;
        }
        // Additional characters for ID_Continue based on Unicode 6.0 (Java 7).
        // Also applies to Unicode 6.2 (Java 8) and Unicode 6.3 (Current).
        switch (c) {
        case '\u19DA':
            return true;
        }
        return false;
    }

    /**
     * <strong>[11.2] White Space</strong>
     * 
     * <pre>
     * WhiteSpace ::
     *     {@literal <TAB>}  (U+0009)
     *     {@literal <VT>}   (U+000B)
     *     {@literal <FF>}   (U+000C)
     *     {@literal <SP>}   (U+0020)
     *     {@literal <NBSP>} (U+00A0)
     *     {@literal <BOM>}  (U+FEFF)
     *     {@literal <USP>}  ("Zs")
     * </pre>
     * 
     * @param c
     *            the character to inspect
     * @return {@code true} if the character is a whitespace
     */
    private static boolean isWhitespace(int c) {
        return (c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20 || c == 0xA0 || c == 0xFEFF || isSpaceSeparator(c));
    }

    /**
     * Unicode category "Zs" (space separator)
     * 
     * @param c
     *            the character to inspect
     * @return {@code true} if the character is space separator
     */
    private static boolean isSpaceSeparator(int c) {
        return (c == 0x20 || c == 0xA0 || c == 0x1680 || c == 0x180E
                || (c >= 0x2000 && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000);
    }

    /**
     * <strong>[11.3] Line Terminators</strong>
     * 
     * <pre>
     * LineTerminator ::
     *     {@literal <LF>} (U+000A)
     *     {@literal <CR>} (U+000D)
     *     {@literal <LS>} (U+2028)
     *     {@literal <PS>} (U+2029)
     * </pre>
     * 
     * @param c
     *            the character to inspect
     * @return {@code true} if the character is a line terminator
     */
    private static boolean isLineTerminator(int c) {
        if ((c & ~0b0010_0000_0010_1111) != 0) {
            return false;
        }
        return (c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029);
    }

    /**
     * <strong>[11.4] Comments</strong>
     * 
     * <pre>
     * SingleLineComment ::
     *     // SingleLineCommentChars<span><sub>opt</sub></span>
     * SingleLineCommentChars ::
     *     SingleLineCommentChar SingleLineCommentChars<span><sub>opt</sub></span>
     * SingleLineCommentChar ::
     *     SourceCharacter but not LineTerminator
     * </pre>
     * 
     * @return the comment token
     */
    private Token readSingleLineComment() {
        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        for (;;) {
            int c = input.getChar();
            if (c == EOF) {
                break;
            }
            if (isLineTerminator(c)) {
                // EOL is not part of the single-line comment!
                input.ungetChar(c);
                break;
            }
        }
        return Token.COMMENT;
    }

    /**
     * <strong>[11.4] Comments</strong>
     * 
     * <pre>
     * MultiLineComment ::
     *     /* MultiLineCommentChars<span><sub>opt</sub></span> &#42;/
     * MultiLineCommentChars ::
     *     MultiLineNotAsteriskChar MultiLineCommentChars<span><sub>opt</sub></span>
     *     PostAsteriskCommentChars<span><sub>opt</sub></span>
     * PostAsteriskCommentChars ::
     *     MultiLineNotForwardSlashOrAsteriskChar MultiLineCommentChars<span><sub>opt</sub></span>
     *     PostAsteriskCommentChars<span><sub>opt</sub></span>
     * MultiLineNotAsteriskChar ::
     *     SourceCharacter but not *
     * MultiLineNotForwardSlashOrAsteriskChar ::
     *     SourceCharacter but not one of / or *
     * </pre>
     * 
     * @return the comment token
     */
    private Token readMultiLineComment() {
        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        loop: for (;;) {
            int c = input.getChar();
            while (c == '*') {
                if ((c = input.getChar()) == '/')
                    break loop;
            }
            if (isLineTerminator(c)) {
                if (c == '\r') {
                    match('\n');
                }
                incrementLineAndUpdate();
            }
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedComment);
            }
        }
        return Token.COMMENT;
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * Identifier ::
     *     IdentifierName but not ReservedWord
     * IdentifierName ::
     *     IdentifierStart
     *     IdentifierName IdentifierPart
     * </pre>
     * 
     * @param c
     *            the start character of the identifier
     * @param hasEscape
     *            the flag for escaped identifiers
     * @return the identifier token
     */
    private Token readIdentifier(int c, boolean hasEscape) {
        assert isIdentifierStart(c);

        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        buffer.addCodepoint(c);
        for (;;) {
            c = input.get();
            if (isIdentifierPart(c)) {
                buffer.addCodepoint(c);
            } else if (c == '\\') {
                hasEscape = true;
                mustMatch('u');
                c = readUnicode();
                if (!isIdentifierPart(c)) {
                    throw error(Messages.Key.InvalidUnicodeEscapedIdentifierPart);
                }
                buffer.addCodepoint(c);
            } else {
                input.unget(c);
                break;
            }
        }

        Token tok = readReservedWord(buffer);
        if (hasEscape) {
            return Token.toEscapedNameToken(tok);
        }
        return tok;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * <pre>
     * UnicodeEscapeSequence ::
     *     u HexDigit HexDigit HexDigit HexDigit
     *     u{ HexDigits }
     * </pre>
     * 
     * @return the unicode escape sequence value
     */
    private int readUnicode() {
        TokenStreamInput input = this.input;
        int c = input.getChar();
        if (c == '{') {
            int acc = 0;
            c = input.getChar();
            do {
                acc = (acc << 4) | hexDigit(c);
            } while ((acc >= 0 && acc <= 0x10FFFF) && (c = input.getChar()) != '}');
            if (c == '}') {
                c = acc;
            } else {
                c = -1;
            }
        } else {
            c = (hexDigit(c) << 12) | (hexDigit(input.getChar()) << 8)
                    | (hexDigit(input.getChar()) << 4) | hexDigit(input.getChar());
        }
        if (c < 0 || c > 0x10FFFF) {
            throw error(Messages.Key.InvalidUnicodeEscape);
        }
        return c;
    }

    /**
     * <strong>[11.6.2] Reserved Words</strong>
     * 
     * <pre>
     * ReservedWord ::
     *     Keyword
     *     FutureReservedWord
     *     NullLiteral
     *     BooleanLiteral
     * </pre>
     * 
     * <strong>[11.6.2.1] Keywords</strong>
     * 
     * <pre>
     * Keyword :: one of
     *     break       do          in          typeof
     *     case        else        instanceof  var
     *     catch       export      new         void
     *     class       extends     return      while
     *     const       finally     super       with
     *     continue    for         switch      yield
     *     debugger    function    this
     *     default     if          throw
     *     delete      import      try
     * </pre>
     * 
     * <strong>[11.6.2.2] Future Reserved Words</strong>
     * 
     * <pre>
     * FutureReservedWord :: one of
     *     enum
     * </pre>
     * 
     * <pre>
     * StrictFutureReservedWord :: one of
     *     implements  package     protected   static
     *     interface   private     public
     * </pre>
     * 
     * <pre>
     * ContextualKeyword :: one of
     *     let
     * </pre>
     * 
     * <strong>[11.8.1] Null Literals</strong>
     * 
     * <pre>
     * NullLiteral ::
     *     null
     * </pre>
     * 
     * <strong>[11.8.2] Boolean Literals</strong>
     * 
     * <pre>
     * BooleanLiteral ::
     *     true
     *     false
     * </pre>
     * 
     * @param buffer
     *            the string buffer containing identifier
     * @return the token type for the identifier
     */
    private static Token readReservedWord(StringBuffer buffer) {
        int length = buffer.length;
        if (length < 2 || length > 10)
            return Token.NAME;
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
        return Token.NAME;
    }

    private static boolean equals(char[] cbuf, String test) {
        for (int i = 0, length = test.length(); i < length; ++i) {
            if (cbuf[i] != test.charAt(i))
                return false;
        }
        return true;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * <pre>
     * StringLiteral ::
     *     " DoubleStringCharacters<span><sub>opt</sub></span> "
     *     ' SingleStringCharacters<span><sub>opt</sub></span> '
     * DoubleStringCharacters ::
     *     DoubleStringCharacter DoubleStringCharacters<span><sub>opt</sub></span>
     * SingleStringCharacters ::
     *     SingleStringCharacter SingleStringCharacters<span><sub>opt</sub></span>
     * DoubleStringCharacter ::
     *     SourceCharacter but not one of " or \ or LineTerminator
     *     \ EscapeSequence
     *     LineContinuation
     * SingleStringCharacter ::
     *     SourceCharacter but not one of ' or \ or LineTerminator
     *     \ EscapeSequence
     *     LineContinuation
     * LineContinuation ::
     *     \ LineTerminatorSequence
     * EscapeSequence ::
     *     CharacterEscapeSequence
     *     0  [lookahead &#x2209; DecimalDigit]
     *     HexEscapeSequence
     *     UnicodeEscapeSequence
     * CharacterEscapeSequence ::
     *     SingleEscapeCharacter
     *     NonEscapeCharacter
     * SingleEscapeCharacter ::  one of
     *     ' "  \  b f n r t v
     * NonEscapeCharacter ::
     *     SourceCharacter but not one of EscapeCharacter or LineTerminator
     * EscapeCharacter ::
     *     SingleEscapeCharacter
     *     DecimalDigit
     *     x
     *     u
     * HexEscapeSequence ::
     *     x HexDigit HexDigit
     * UnicodeEscapeSequence ::
     *     u HexDigit HexDigit HexDigit HexDigit
     *     u{ HexDigits }
     * </pre>
     * 
     * <strong>[B.1.2] String Literals</strong>
     * 
     * <pre>
     * EscapeSequence ::
     *     CharacterEscapeSequence
     *     OctalEscapeSequence
     *     HexEscapeSequence
     *     UnicodeEscapeSequence
     * </pre>
     * 
     * @param quoteChar
     *            the quotation character for the string literal
     * @return the string literal value
     */
    private Token readString(int quoteChar) {
        assert quoteChar == '"' || quoteChar == '\'';

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        int start = input.position();
        StringBuffer buffer = this.buffer();
        hasEscape = false;
        for (;;) {
            int c = input.getChar();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedStringLiteral);
            }
            if (c == quoteChar) {
                buffer.add(input.range(start, input.position() - 1));
                break;
            }
            if (isLineTerminator(c)) {
                throw error(Messages.Key.UnterminatedStringLiteral);
            }
            if (c != '\\') {
                continue;
            }
            buffer.add(input.range(start, input.position() - 1));
            hasEscape = true;
            c = input.getChar();
            if (isLineTerminator(c)) {
                // line continuation
                if (c == '\r' && match('\n')) {
                    // \r\n sequence
                }
                incrementLine();
                start = input.position();
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
            case 'x':
                c = (hexDigit(input.getChar()) << 4) | hexDigit(input.getChar());
                if (c < 0) {
                    throw error(Messages.Key.InvalidHexEscape);
                }
                break;
            case 'u':
                c = readUnicode();
                break;
            case '0':
                if (isDecimalDigit(input.peek(0))) {
                    if (!parser.isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                        throw error(Messages.Key.InvalidNULLEscape);
                    }
                    c = readOctalEscape(c);
                } else {
                    c = '\0';
                }
                break;
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                if (!parser.isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                    throw error(Messages.Key.StrictModeOctalEscapeSequence);
                }
                c = readOctalEscape(c);
                break;
            case '8':
            case '9':
                // FIXME: spec bug - undefined behaviour for \8 and \9
                if (!parser.isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                    throw error(Messages.Key.StrictModeOctalEscapeSequence);
                }
                // fall-through
            case '"':
            case '\'':
            case '\\':
            default:
                // fall-through
            }
            buffer.addCodepoint(c);
            start = input.position();
        }

        return Token.STRING;
    }

    /**
     * <strong>[B.1.2] String Literals</strong>
     * 
     * <pre>
     * OctalEscapeSequence ::
     *     OctalDigit [lookahead &#x2209; DecimalDigit]
     *     ZeroToThree OctalDigit [lookahead &#x2209; DecimalDigit]
     *     FourToSeven OctalDigit
     *     ZeroToThree OctalDigit OctalDigit
     * ZeroToThree :: one of
     *     0 1 2 3
     * FourToSeven :: one of
     *     4 5 6 7
     * </pre>
     * 
     * @param c
     *            the start character of the octal escape sequence
     * @return the octal escape value
     */
    private int readOctalEscape(int c) {
        parser.reportStrictModeSyntaxError(Messages.Key.StrictModeOctalEscapeSequence);
        int d = (c - '0');
        c = input.getChar();
        if (c < '0' || c > '7') {
            // FIXME: spec bug? behaviour for non-octal decimal digits?
            input.ungetChar(c);
        } else {
            d = d * 8 + (c - '0');
            if (d <= 037) {
                c = input.getChar();
                if (c < '0' || c > '7') {
                    // FIXME: spec bug? behaviour for non-octal decimal digits?
                    input.ungetChar(c);
                } else {
                    d = d * 8 + (c - '0');
                }
            }
        }
        return d;
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * NumericLiteral ::
     *     DecimalLiteral
     *     BinaryIntegerLiteral
     *     OctalIntegerLiteral
     *     HexIntegerLiteral
     * </pre>
     * 
     * @param c
     *            the start character of the decimal integer literal
     * @return the number token
     */
    private Token readNumberLiteral(int c) {
        if (c == '0') {
            int d = input.getChar();
            if (d == 'x' || d == 'X') {
                number = readHexIntegerLiteral();
            } else if (d == 'b' || d == 'B') {
                number = readBinaryIntegerLiteral();
            } else if (d == 'o' || d == 'O') {
                number = readOctalIntegerLiteral();
            } else if (isDecimalDigit(d)
                    && parser.isEnabled(CompatibilityOption.LegacyOctalIntegerLiteral)) {
                input.ungetChar(d);
                number = readLegacyOctalIntegerLiteral();
            } else {
                input.ungetChar(d);
                number = readDecimalLiteral(c);
            }
        } else {
            number = readDecimalLiteral(c);
        }
        return Token.NUMBER;
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexIntegerLiteral ::
     *     0x HexDigits
     *     0X HexDigits
     * HexDigits ::
     *     HexDigit
     *     HexDigits HexDigit
     * </pre>
     * 
     * @return the hexadecimal integer literal
     */
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

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * BinaryIntegerLiteral ::
     *     0b BinaryDigit
     *     0B BinaryDigit
     *     BinaryIntegerLiteral BinaryDigit
     * </pre>
     * 
     * @return the binary integer literal
     */
    private double readBinaryIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isBinaryDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidBinaryIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidBinaryIntegerLiteral);
        }
        return parseBinary(buffer.cbuf, buffer.length);
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * OctalIntegerLiteral ::
     *     0o OctalDigit
     *     0O OctalDigit
     *     OctalIntegerLiteral OctalDigit
     * </pre>
     * 
     * @return the octal integer literal
     */
    private double readOctalIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isOctalDigit(c = input.get())) {
            buffer.add(c);
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        return parseOctal(buffer.cbuf, buffer.length);
    }

    /**
     * <strong>[B.1.1] Numeric Literals</strong>
     * 
     * <pre>
     * LegacyOctalIntegerLiteral ::
     *     0 OctalDigit
     *     LegacyOctalIntegerLiteral OctalDigit
     * </pre>
     * 
     * @return the octal integer literal
     */
    private double readLegacyOctalIntegerLiteral() {
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        int c;
        while (isOctalDigit(c = input.get())) {
            buffer.add(c);
        }
        if (c == '8' || c == '9') {
            // invalid octal integer literal -> treat as decimal literal, no strict-mode error
            // FIXME: spec bug? undefined behaviour - SM reports a strict-mode error in this case
            return readDecimalLiteral(c, false);
        }
        parser.reportStrictModeSyntaxError(Messages.Key.StrictModeOctalIntegerLiteral);
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        input.unget(c);
        if (buffer.length == 0) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        return parseOctal(buffer.cbuf, buffer.length);
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * DecimalLiteral ::
     *     DecimalIntegerLiteral . DecimalDigits<span><sub>opt</sub></span> ExponentPart<span><sub>opt</sub></span>
     *     . DecimalDigits ExponentPart<span><sub>opt</sub></span>
     *     DecimalIntegerLiteral ExponentPart<span><sub>opt</sub></span>
     * DecimalIntegerLiteral ::
     *     0
     *     NonZeroDigit DecimalDigits<span><sub>opt</sub></span>
     * DecimalDigits ::
     *     DecimalDigit
     *     DecimalDigits DecimalDigit
     * NonZeroDigit :: one of
     *     1 2 3 4 5 6 7 8 9
     * ExponentPart ::
     *     ExponentIndicator SignedInteger
     * ExponentIndicator :: one of
     *     e E
     * SignedInteger ::
     *     DecimalDigits
     *     + DecimalDigits
     *     - DecimalDigits
     * </pre>
     * 
     * @param c
     *            the start character of the decimal integer literal
     * @return the decimal integer literal
     */
    private double readDecimalLiteral(int c) {
        return readDecimalLiteral(c, true);
    }

    private double readDecimalLiteral(int c, boolean reset) {
        assert c == '.' || isDecimalDigit(c);
        boolean isInteger = true;
        TokenStreamInput input = this.input;
        StringBuffer buffer = reset ? this.buffer() : this.buffer;
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
            isInteger = false;
            buffer.add(c);
            while (isDecimalDigit(c = input.get())) {
                buffer.add(c);
            }
        }
        if (c == 'e' || c == 'E') {
            isInteger = false;
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
            throw error(Messages.Key.InvalidNumberLiteral);
        }
        input.unget(c);
        if (isInteger) {
            return parseInteger(buffer.cbuf, buffer.length);
        }
        return parseDecimal(buffer.cbuf, buffer.length);
    }

    /**
     * Returns <code>true</code> if {@code c} is either a decimal digit or an identifier start
     * character.
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is either a decimal digit or an identifier start
     *         character
     */
    private boolean isDecimalDigitOrIdentifierStart(int c) {
        return isDecimalDigit(c) || isIdentifierStart(c);
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * DecimalDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is decimal digit
     */
    private static boolean isDecimalDigit(int c) {
        return (c >= '0' && c <= '9');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * BinaryDigit :: one of
     *     0  1
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is a binary digit
     */
    private static boolean isBinaryDigit(int c) {
        return (c == '0' || c == '1');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * OctalDigit :: one of
     *     0  1  2  3  4  5  6  7
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is an octal digit
     */
    private static boolean isOctalDigit(int c) {
        return (c >= '0' && c <= '7');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is a hexadecimal digit
     */
    private static boolean isHexDigit(int c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
     * </pre>
     * 
     * @param c
     *            the character to convert
     * @return the converted integer
     */
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
     * Throws a {@link ParserException}.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, parser.getSourceFile(), getLine(),
                getColumn(), messageKey, args);
    }

    /**
     * Throws a {@link ParserEOFException}.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException eofError(Messages.Key messageKey, String... args) {
        throw new ParserEOFException(parser.getSourceFile(), getLine(), getColumn(), messageKey,
                args);
    }

    /**
     * Returns <code>true</code> and advances the source position if the current character is
     * {@code c}. Otherwise returns <code>false</code> and does not advance the source position.
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the current character matches
     */
    private boolean match(char c) {
        return input.match(c);
    }

    /**
     * Advances the source position if the current character is {@code c}. Otherwise throws a parser
     * exception.
     * 
     * @param c
     *            the character to test
     */
    private void mustMatch(char c) {
        if (input.getChar() != c) {
            throw error(Messages.Key.UnexpectedCharacter, String.valueOf(c));
        }
    }
}
