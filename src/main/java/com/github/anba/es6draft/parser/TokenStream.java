/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.parser.Characters.*;
import static com.github.anba.es6draft.parser.NumberParser.*;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.CompatibilityOption;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * Lexer for ECMAScript source code
 * <ul>
 * <li>10 ECMAScript Language: Source Code
 * <li>11 ECMAScript Language: Lexical Grammar
 * </ul>
 */
final class TokenStream {
    private final Parser parser;
    private final TokenStreamInput input;

    /** current line number */
    private int line;
    /** start position of current line */
    private int linestart;
    /** start position of current token, includes leading whitespace and comments */
    private int position, startPosition;
    /** start position of next token, includes leading whitespace and comments */
    private int nextPosition, nextStartPosition;

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
    private final StrBuffer buffer;
    private String string = null;
    private Number number = null;
    private boolean hasEscape = false;

    /**
     * Resets and returns the internal character buffer.
     * 
     * @return the character buffer
     */
    private StrBuffer buffer() {
        StrBuffer buffer = this.buffer;
        buffer.clear();
        return buffer;
    }

    /**
     * Returns {@code true} if the compatibility option is enabled.
     * 
     * @param option
     *            the compatibility option
     * @return {@code true} if the compatibility option is enabled.
     */
    private boolean isEnabled(CompatibilityOption option) {
        return parser.isEnabled(option);
    }

    /**
     * Returns {@code true} if parsing module code.
     * 
     * @return {@code true} if parsing module code
     */
    private boolean isModule() {
        return parser.isModule();
    }

    /**
     * Updates line state information for line breaks within literals, does <strong>not</strong> set the
     * {@link #hasLineTerminator} flag.
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
        nextStartPosition = input.position() - 1;
    }

    /**
     * Public constructor, token stream still needs to be initialized by calling the {@link #initialize()} method.
     * 
     * @param parser
     *            the parser instance
     * @param input
     *            the token stream instance
     */
    public TokenStream(Parser parser, TokenStreamInput input) {
        this.parser = parser;
        this.input = input;
        this.buffer = new StrBuffer(input.length());
    }

    /**
     * Returns the start position of current token, includes leading whitespace and comments. Also needed to reset the
     * token stream.
     * 
     * @return the token start position
     * @see #reset(long, long)
     */
    public int position() {
        return position;
    }

    /**
     * Returns the start position of current token, does not include leading whitespace and comments.
     * 
     * @return the token start position
     */
    public int startPosition() {
        return startPosition;
    }

    /**
     * Returns the last character from the input source.
     * 
     * @return the last character
     */
    public char lastChar() {
        return (char) input.lastChar();
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
     * Returns the encoded line/column information of the next source position.
     * 
     * @return the next line/column information
     */
    public long nextSourcePosition() {
        assert next != null;
        return nextSourcePosition;
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
     * Returns the encoded end line/column information for current position.
     * 
     * @return the end line/column information
     */
    public long rawEndPosition() {
        // add one to make columns 1-indexed
        return ((long) (1 + input.position() - linestart) << 32) | line;
    }

    /**
     * Initializes this token stream, needs to be called before fetching any tokens.
     * 
     * @return this token stream
     */
    public TokenStream initialize() {
        return initialize(parser.getSourceLine());
    }

    /**
     * Initializes this token stream, needs to be called before fetching any tokens.
     * 
     * @param line
     *            the start line number
     * @return this token stream
     */
    public TokenStream initialize(int line) {
        // set internal state to default values
        this.hasLineTerminator = true;
        this.hasCurrentLineTerminator = true;
        this.position = input.position();
        this.line = line;
        this.linestart = input.position();
        this.current = scanTokenNoComment();
        this.startPosition = nextStartPosition;
        this.sourcePosition = nextSourcePosition;
        this.nextPosition = input.position();
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
        this.linestart = (int) lineinfo;
        this.current = scanTokenNoComment();
        this.startPosition = nextStartPosition;
        this.sourcePosition = nextSourcePosition;
        this.nextPosition = input.position();
        this.next = null;
        // reset line state last, effectively ignoring any changes from scanTokenNoComment()
        this.line = (int) (lineinfo >>> 32);
        this.linestart = (int) lineinfo;
        // adjust source position to account for line breaks
        this.sourcePosition = (this.sourcePosition & ~0xFFFF_FFFFL) | this.line;
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
     * Returns the string data of the next token.
     * 
     * @return the next string data
     */
    public String getNextString() {
        if (next == null) {
            peekToken();
        }
        return buffer.toString();
    }

    /**
     * Returns <code>true</code> if the current token is a string literal which contains an escape sequence.
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
    public Number getNumber() {
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

    /* token operations */

    /**
     * Advances the token stream to the next token.
     * 
     * @return the next token
     */
    public Token nextToken() {
        if (next == null) {
            hasLineTerminator = false;
            nextPosition = input.position();
            next = scanTokenNoComment();
        }
        current = next;
        position = nextPosition;
        startPosition = nextStartPosition;
        sourcePosition = nextSourcePosition;
        hasCurrentLineTerminator = hasLineTerminator;
        string = null;
        next = null;
        nextPosition = input.position();
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
        assert !(current == Token.DIV || current == Token.ASSIGN_DIV || current == Token.ERROR);
        if (next == null) {
            switch (current) {
            case NAME:
            case ESCAPED_NAME:
            case ESCAPED_RESERVED_WORD:
            case ESCAPED_STRICT_RESERVED_WORD:
            case ESCAPED_YIELD:
            case ESCAPED_ASYNC:
            case ESCAPED_AWAIT:
            case ESCAPED_LET:
            case PRIVATE_NAME:
            case STRING:
                string = getString();
            default:
            }
            hasLineTerminator = false;
            nextPosition = input.position();
            next = scanTokenNoComment();
        }
        return next;
    }

    /* lexer operations */

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
     * @return the regular expression pattern
     */
    public String readRegularExpression(Token start) {
        assert start == Token.DIV || start == Token.ASSIGN_DIV;
        assert next == null : "regular expression in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StrBuffer buffer = buffer();
        if (start == Token.ASSIGN_DIV) {
            buffer.append('=');
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
                buffer.append(c);
                c = input.getChar();
            } else if (c == '[') {
                inClass = true;
            } else if (c == ']') {
                inClass = false;
            } else if (c == '/' && !inClass) {
                return buffer.toString();
            }
            if (c == EOF || isLineTerminator(c)) {
                throw error(Messages.Key.UnterminatedRegExpLiteral);
            }
            buffer.append(c);
        }
    }

    /**
     * <strong>[11.8.5] Regular Expression Literals</strong>
     * 
     * <pre>
     * RegularExpressionFlags ::
     *     [empty]
     *     RegularExpressionFlags IdentifierPart
     * </pre>
     * 
     * @return the regular expression literal flags
     */
    public String readRegularExpressionFlags() {
        TokenStreamInput input = this.input;
        StrBuffer buffer = buffer();
        for (;;) {
            int c = input.get();
            if (!isIdentifierPart(c)) {
                if (c == '\\' && match('u')) {
                    readUnicodeEscape();
                    throw error(Messages.Key.UnicodeEscapeInRegExpFlags);
                }
                input.unget(c);
                return buffer.toString();
            }
            buffer.appendCodePoint(c);
        }
    }

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
     *     $ [LA &#x2260; { ]
     *     \ EscapeSequence
     *     LineContinuation
     *     LineTerminatorSequence
     *     SourceCharacter but not one of ` or \ or $ or LineTerminator
     * </pre>
     * 
     * @param startToken
     *            the start token of the template literal, either {@link Token#TEMPLATE} or {@link Token#RC}
     * @param tagged
     *            the flag for tagged template literals
     * @return string tuple {cooked, raw} for the template literal
     */
    public String[] readTemplateLiteral(Token startToken, boolean tagged) {
        assert startToken == Token.TEMPLATE || startToken == Token.RC;
        assert currentToken() == startToken;
        assert next == null : "template literal in lookahead";

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuilder raw = new StringBuilder();
        StrBuffer buffer = buffer();
        boolean hasInvalidEscape = false;
        int pos = input.position();
        int rawPos = input.position();
        for (;;) {
            int c = input.getChar();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedTemplateLiteral);
            }
            if (c == '`') {
                current = Token.TEMPLATE;
                buffer.append(input, pos, input.position() - 1);
                raw.append(input.range(rawPos, input.position() - 1));
                String cooked = hasInvalidEscape ? null : buffer.toString();
                return new String[] { cooked, raw.toString() };
            }
            if (c == '$' && match('{')) {
                current = Token.LC;
                buffer.append(input, pos, input.position() - 2);
                raw.append(input.range(rawPos, input.position() - 2));
                String cooked = hasInvalidEscape ? null : buffer.toString();
                return new String[] { cooked, raw.toString() };
            }
            if (c != '\\') {
                if (isLineTerminator(c)) {
                    // line terminator sequence
                    if (c == '\r') {
                        // normalize \r and \r\n to \n
                        buffer.append(input, pos, input.position() - 1);
                        buffer.append('\n');
                        raw.append(input.range(rawPos, input.position() - 1)).append('\n');
                        match('\n');
                        pos = rawPos = input.position();
                    }
                    incrementLine();
                }
                continue;
            }
            buffer.append(input, pos, input.position() - 1);

            c = input.getChar();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedTemplateLiteral);
            }
            if (isLineTerminator(c)) {
                // line continuation
                if (c == '\r') {
                    // normalize \r and \r\n to \n
                    raw.append(input.range(rawPos, input.position() - 1)).append('\n');
                    match('\n');
                    rawPos = input.position();
                }
                incrementLine();
            } else {
                int e = readTemplateEscapeSequence(c, tagged);
                if (e < 0) {
                    hasInvalidEscape = true;
                } else {
                    buffer.appendCodePoint(e);
                }
            }
            pos = input.position();
        }
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * <pre>
     * EscapeSequence ::
     *     CharacterEscapeSequence
     *     0  [lookahead &#x2209; DecimalDigit]
     *     HexEscapeSequence
     *     UnicodeEscapeSequence
     * CharacterEscapeSequence ::
     *     SingleEscapeCharacter
     *     NonEscapeCharacter
     * SingleEscapeCharacter :: one of
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
     * @param c
     *            the start character
     * @param allowNonEscape
     *            if {@code true} non-escape sequences are allowed
     * @return the escaped character
     */
    private int readTemplateEscapeSequence(int c, boolean allowNonEscape) {
        TokenStreamInput input = this.input;
        switch (c) {
        case 'b':
            return '\b';
        case 'f':
            return '\f';
        case 'n':
            return '\n';
        case 'r':
            return '\r';
        case 't':
            return '\t';
        case 'v':
            return '\u000B';
        case 'x':
            return allowNonEscape ? readHexEscapeUnchecked() : readHexEscape();
        case 'u':
            return allowNonEscape ? readUnicodeEscapeUnchecked() : readUnicodeEscape();
        case '0':
            if (isDecimalDigit(input.peek(0))) {
                if (allowNonEscape) {
                    return -1;
                }
                throw error(Messages.Key.InvalidNULLEscape);
            }
            return '\0';
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            if (allowNonEscape) {
                return -1;
            }
            throw error(Messages.Key.OctalEscapeSequence);
        case '"':
        case '\'':
        case '\\':
        default:
            return c;
        }
    }

    /**
     * <strong>[11] ECMAScript Language: Lexical Grammar</strong>
     * 
     * <pre>
     * InputElementDiv ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     CommonToken
     *     DivPunctuator
     *     RightBracePunctuator
     * InputElementRegExp ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     CommonToken
     *     RightBracePunctuator
     *     RegularExpressionLiteral
     * InputElementRegExpOrTemplateTail ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     CommonToken
     *     RegularExpressionLiteral
     *     TemplateSubstitutionTail
     * InputElementTemplateTail ::
     *     WhiteSpace
     *     LineTerminator
     *     Comment
     *     CommonToken
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
     * <strong>[11.5] Tokens</strong>
     * 
     * <pre>
     * CommonToken ::
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
                    && isEnabled(CompatibilityOption.HTMLComments) && !isModule()) {
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
                if (input.peek(0) == '>' && hasLineTerminator && isEnabled(CompatibilityOption.HTMLComments)
                        && !isModule()) {
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
            if (input.peek(0) == '*') {
                mustMatch('*');
                if (match('=')) {
                    return Token.ASSIGN_EXP;
                }
                return Token.EXP;
            } else if (match('=')) {
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
        case '@':
            if (isEnabled(CompatibilityOption.Decorator)) {
                return Token.AT;
            }
            return Token.ERROR;
        case '#':
            if (isEnabled(CompatibilityOption.ClassFields) || isEnabled(CompatibilityOption.PrivateMethods)) {
                return readPrivateName();
            }
            return Token.ERROR;
        case '\\':
            mustMatch('u');
            c = readUnicodeEscape();
            if (isIdentifierStart(c)) {
                return readIdentifier(c, true);
            }
            throw error(Messages.Key.InvalidUnicodeEscapedIdentifierStart);
        default:
            if (isIdentifierStart(c)) {
                return readIdentifier(c, false);
            }
            return Token.ERROR;
        }
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
     * @param buffer
     *            the string buffer
     * @param c
     *            the start character of the identifier
     * @return {@code true} if the identifier name contains escaped characters
     */
    private boolean readIdentifierName(StrBuffer buffer, int c) {
        assert isIdentifierStart(c);

        TokenStreamInput input = this.input;
        buffer.appendCodePoint(c);
        boolean hasEscape = false;
        for (;;) {
            c = input.get();
            if (isIdentifierPart(c)) {
                buffer.appendCodePoint(c);
            } else if (c == '\\') {
                hasEscape = true;
                mustMatch('u');
                c = readUnicodeEscape();
                if (!isIdentifierPart(c)) {
                    throw error(Messages.Key.InvalidUnicodeEscapedIdentifierPart);
                }
                buffer.appendCodePoint(c);
            } else {
                input.unget(c);
                break;
            }
        }
        return hasEscape;
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * Identifier ::
     *     IdentifierName but not ReservedWord
     * </pre>
     * 
     * @param c
     *            the start character of the identifier
     * @param identifierStartHasEscape
     *            the flag for escaped identifiers
     * @return the identifier token
     */
    private Token readIdentifier(int c, boolean identifierStartHasEscape) {
        assert isIdentifierStart(c);

        StrBuffer buffer = this.buffer();
        boolean identifierPartHasEscape = readIdentifierName(buffer, c);
        Token tok = readReservedWord(buffer.array(), buffer.length());
        if (identifierStartHasEscape || identifierPartHasEscape) {
            return Token.toEscapedNameToken(tok);
        }
        return tok;
    }

    /**
     * Extension: Private Fields
     * 
     * <pre>
     * PrivateName ::
     *     `#` IdentifierName
     * </pre>
     * 
     * @return the private name token
     */
    private Token readPrivateName() {
        StrBuffer buffer = this.buffer();
        buffer.append('#');

        int c = input.get();
        if (!isIdentifierStart(c)) {
            throw error(Messages.Key.InvalidUnicodeEscapedIdentifierStart);
        }
        readIdentifierName(buffer, c);

        return Token.PRIVATE_NAME;
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
    private int readUnicodeEscape() {
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
            c = (hexDigit(c) << 12) | (hexDigit(input.getChar()) << 8) | (hexDigit(input.getChar()) << 4)
                    | hexDigit(input.getChar());
        }
        if (c < 0 || c > 0x10FFFF) {
            throw error(Messages.Key.InvalidUnicodeEscape);
        }
        return c;
    }

    private int readUnicodeEscapeUnchecked() {
        TokenStreamInput input = this.input;
        int p = input.position();
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
            c = (hexDigit(c) << 12) | (hexDigit(input.getChar()) << 8) | (hexDigit(input.getChar()) << 4)
                    | hexDigit(input.getChar());
        }
        if (c < 0 || c > 0x10FFFF) {
            input.reset(p);
            return -1;
        }
        return c;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * <pre>
     * HexEscapeSequence ::
     *     x HexDigit HexDigit
     * </pre>
     * 
     * @return the hex-escape sequence value
     */
    private int readHexEscape() {
        TokenStreamInput input = this.input;
        int x = (hexDigit(input.getChar()) << 4) | hexDigit(input.getChar());
        if (x < 0) {
            throw error(Messages.Key.InvalidHexEscape);
        }
        return x;
    }

    private int readHexEscapeUnchecked() {
        TokenStreamInput input = this.input;
        if (isHexDigit(input.peek(0)) && isHexDigit(input.peek(1))) {
            return (hexDigit(input.getChar()) << 4) | hexDigit(input.getChar());
        }
        return -1;
    }

    static Token readReservedWord(String name) {
        int length = name.length();
        if (length < 2 || length > 10)
            return Token.NAME;
        return readReservedWord(name.toCharArray(), length);
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
     *     await
     * </pre>
     * 
     * <pre>
     * StrictFutureReservedWord :: one of
     *     implements  package     protected
     *     interface   private     public
     * </pre>
     * 
     * <pre>
     * ContextualKeyword :: one of
     *     let
     *     static
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
     * @param cbuf
     *            the character array
     * @param length
     *            the number of characters to read
     * @return the token type for the identifier
     */
    private static Token readReservedWord(char[] cbuf, int length) {
        if (length < 2 || length > 10)
            return Token.NAME;
        char c0 = cbuf[0], c1 = cbuf[1];
        Token test = null;
        switch (c0) {
        case 'a':
            // async, await
            if (length == 5)
                test = (c1 == 's' ? Token.ASYNC : Token.AWAIT);
            break;
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
        StrBuffer buffer = this.buffer();
        hasEscape = false;
        for (;;) {
            int c = input.getChar();
            if (c == EOF) {
                throw eofError(Messages.Key.UnterminatedStringLiteral);
            }
            if (c == quoteChar) {
                buffer.append(input, start, input.position() - 1);
                break;
            }
            if (isLineTerminator(c)) {
                throw error(Messages.Key.UnterminatedStringLiteral);
            }
            if (c != '\\') {
                continue;
            }
            buffer.append(input, start, input.position() - 1);

            // EscapeSequence or LineContinuation
            hasEscape = true;
            c = input.getChar();
            if (isLineTerminator(c)) {
                if (c == '\r' && match('\n')) {
                    // \r\n sequence
                }
                incrementLine();
            } else {
                buffer.appendCodePoint(readStringEscapeSequence(c));
            }
            start = input.position();
        }

        return Token.STRING;
    }

    /**
     * <strong>[11.8.4] String Literals</strong>
     * 
     * <pre>
     * EscapeSequence ::
     *     CharacterEscapeSequence
     *     0  [lookahead &#x2209; DecimalDigit]
     *     HexEscapeSequence
     *     UnicodeEscapeSequence
     * CharacterEscapeSequence ::
     *     SingleEscapeCharacter
     *     NonEscapeCharacter
     * SingleEscapeCharacter :: one of
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
     *     LegacyOctalEscapeSequence
     *     HexEscapeSequence
     *     UnicodeEscapeSequence
     * </pre>
     * 
     * @param c
     *            the start character
     * @return the escaped character
     */
    private int readStringEscapeSequence(int c) {
        TokenStreamInput input = this.input;
        switch (c) {
        case 'b':
            return '\b';
        case 'f':
            return '\f';
        case 'n':
            return '\n';
        case 'r':
            return '\r';
        case 't':
            return '\t';
        case 'v':
            return '\u000B';
        case 'x':
            return readHexEscape();
        case 'u':
            return readUnicodeEscape();
        case '0':
            if (isDecimalDigit(input.peek(0))) {
                if (!isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                    throw error(Messages.Key.InvalidNULLEscape);
                }
                return readLegacyOctalEscape(c);
            }
            return '\0';
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
            if (!isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                throw error(Messages.Key.OctalEscapeSequence);
            }
            return readLegacyOctalEscape(c);
        case '8':
        case '9':
            // FIXME: spec bug - undefined behaviour for \8 and \9
            if (!isEnabled(CompatibilityOption.OctalEscapeSequence)) {
                throw error(Messages.Key.OctalEscapeSequence);
            }
            // fall-through
        case '"':
        case '\'':
        case '\\':
        default:
            return c;
        }
    }

    /**
     * <strong>[B.1.2] String Literals</strong>
     * 
     * <pre>
     * LegacyOctalEscapeSequence ::
     *     OctalDigit [lookahead &#x2209; OctalDigit]
     *     ZeroToThree OctalDigit [lookahead &#x2209; OctalDigit]
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
    private int readLegacyOctalEscape(int c) {
        assert '0' <= c && c <= '7';
        strictModeError(Messages.Key.StrictModeOctalEscapeSequence);
        int d = (c - '0');
        c = input.getChar();
        if (isOctalDigit(c)) {
            d = d * 8 + (c - '0');
            if (d <= 037) {
                c = input.getChar();
                if (isOctalDigit(c)) {
                    d = d * 8 + (c - '0');
                } else {
                    input.ungetChar(c);
                }
            }
        } else {
            input.ungetChar(c);
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
     *     LegacyOctalIntegerLiteral
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
            } else if (isDecimalDigit(d)) {
                if (isEnabled(CompatibilityOption.LegacyOctalIntegerLiteral)) {
                    input.ungetChar(d);
                    number = readLegacyOctalIntegerLiteral();
                } else {
                    throw error(Messages.Key.InvalidNumberLiteral);
                }
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
    private Number readHexIntegerLiteral() {
        TokenStreamInput input = this.input;
        StrBuffer buffer = this.buffer();
        int c;
        if (isEnabled(CompatibilityOption.NumericSeparators)) {
            do {
                c = input.get();
                if (!isHexDigit(c)) {
                    throw error(Messages.Key.InvalidHexIntegerLiteral);
                }
                do {
                    buffer.append(c);
                } while (isHexDigit(c = input.get()));
            } while (c == '_');
        } else {
            while (isHexDigit(c = input.get())) {
                buffer.append(c);
            }
            if (buffer.length() == 0) {
                throw error(Messages.Key.InvalidHexIntegerLiteral);
            }
        }
        if (c == 'n' && isEnabled(CompatibilityOption.BigInt)) {
            return parseBigIntHex(buffer.array(), buffer.length());
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidHexIntegerLiteral);
        }
        input.unget(c);
        return parseHex(buffer.array(), buffer.length());
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
    private Number readBinaryIntegerLiteral() {
        TokenStreamInput input = this.input;
        StrBuffer buffer = this.buffer();
        int c;
        if (isEnabled(CompatibilityOption.NumericSeparators)) {
            do {
                c = input.get();
                if (!isBinaryDigit(c)) {
                    throw error(Messages.Key.InvalidBinaryIntegerLiteral);
                }
                do {
                    buffer.append(c);
                } while (isBinaryDigit(c = input.get()));
            } while (c == '_');
        } else {
            while (isBinaryDigit(c = input.get())) {
                buffer.append(c);
            }
            if (buffer.length() == 0) {
                throw error(Messages.Key.InvalidBinaryIntegerLiteral);
            }
        }
        if (c == 'n' && isEnabled(CompatibilityOption.BigInt)) {
            return parseBigIntBinary(buffer.array(), buffer.length());
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidBinaryIntegerLiteral);
        }
        input.unget(c);
        return parseBinary(buffer.array(), buffer.length());
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
    private Number readOctalIntegerLiteral() {
        TokenStreamInput input = this.input;
        StrBuffer buffer = this.buffer();
        int c;
        if (isEnabled(CompatibilityOption.NumericSeparators)) {
            do {
                c = input.get();
                if (!isOctalDigit(c)) {
                    throw error(Messages.Key.InvalidOctalIntegerLiteral);
                }
                do {
                    buffer.append(c);
                } while (isOctalDigit(c = input.get()));
            } while (c == '_');
        } else {
            while (isOctalDigit(c = input.get())) {
                buffer.append(c);
            }
            if (buffer.length() == 0) {
                throw error(Messages.Key.InvalidOctalIntegerLiteral);
            }
        }
        if (c == 'n' && isEnabled(CompatibilityOption.BigInt)) {
            return parseBigIntOctal(buffer.array(), buffer.length());
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        input.unget(c);
        return parseOctal(buffer.array(), buffer.length());
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
    private Number readLegacyOctalIntegerLiteral() {
        TokenStreamInput input = this.input;
        StrBuffer buffer = this.buffer();
        int c;
        while (isOctalDigit(c = input.get())) {
            buffer.append(c);
        }
        if (c == '8' || c == '9') {
            // invalid octal integer literal -> treat as decimal literal in non-strict mode
            strictModeError(Messages.Key.StrictModeDecimalLeadingZero);
            return readDecimalLiteral(c, true);
        }
        strictModeError(Messages.Key.StrictModeOctalIntegerLiteral);
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidOctalIntegerLiteral);
        }
        input.unget(c);
        assert buffer.length() != 0;
        return parseOctal(buffer.array(), buffer.length());
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
     * <strong>[B.1.1] Numeric Literals</strong>
     * 
     * <pre>
     * DecimalIntegerLiteral ::
     *     0
     *     NonZeroDigit DecimalDigits<span><sub>opt</sub></span>
     *     NonOctalDecimalIntegerLiteral
     * NonOctalDecimalIntegerLiteral ::
     *     0 NonOctalDigit
     *     LegacyOctalLikeDecimalIntegerLiteral NonOctalDigit
     *     NonOctalDecimalIntegerLiteral DecimalDigit
     * LegacyOctalLikeDecimalIntegerLiteral ::
     *     0 OctalDigit
     *     LegacyOctalLikeDecimalIntegerLiteral OctalDigit
     * NonOctalDigit :: one of
     *     8 9
     * </pre>
     * 
     * @param c
     *            the start character of the decimal integer literal
     * @return the decimal integer literal
     */
    private Number readDecimalLiteral(int c) {
        return readDecimalLiteral(c, false);
    }

    private Number readDecimalLiteral(int c, boolean isInvalidLegacyOctal) {
        assert c == '.' || isDecimalDigit(c);
        boolean isInteger = true;
        TokenStreamInput input = this.input;
        StrBuffer buffer = isInvalidLegacyOctal ? this.buffer : this.buffer();
        if (c != '.' && c != '0') {
            // FIXME: spec issue - numeric separator proposal disallows `08_1`, but allows `08.1_2`.
            c = parseDecimalDigits(c, !isInvalidLegacyOctal);
        } else if (c == '0') {
            buffer.append(c);
            c = input.get();
        }
        if (c == '.') {
            isInteger = false;
            buffer.append(c);
            if (isDecimalDigit(c = input.get())) {
                c = parseDecimalDigits(c, true);
            }
        }
        if (c == 'e' || c == 'E') {
            isInteger = false;
            buffer.append(c);
            c = input.get();
            if (c == '+' || c == '-') {
                buffer.append(c);
                c = input.get();
            }
            if (!isDecimalDigit(c)) {
                throw error(Messages.Key.InvalidNumberLiteral);
            }
            c = parseDecimalDigits(c, true);
        }
        if (c == 'n' && isEnabled(CompatibilityOption.BigInt)) {
            if (!isInteger || isInvalidLegacyOctal) {
                throw error(Messages.Key.InvalidNumberLiteral);
            }
            return parseBigInt(buffer.array(), buffer.length());
        }
        if (isDecimalDigitOrIdentifierStart(c)) {
            throw error(Messages.Key.InvalidNumberLiteral);
        }
        input.unget(c);
        if (isInteger) {
            return parseInteger(buffer.array(), buffer.length());
        }
        return parseDecimal(buffer.array(), buffer.length());
    }

    private int parseDecimalDigits(int c, boolean allowSeparator) {
        assert isDecimalDigit(c);
        TokenStreamInput input = this.input;
        StrBuffer buffer = this.buffer;
        if (allowSeparator && isEnabled(CompatibilityOption.NumericSeparators)) {
            while (true) {
                do {
                    buffer.append(c);
                } while (isDecimalDigit(c = input.get()));
                if (c != '_') {
                    break;
                }
                if (!isDecimalDigit(c = input.get())) {
                    throw error(Messages.Key.InvalidNumberLiteral);
                }
            }
        } else {
            do {
                buffer.append(c);
            } while (isDecimalDigit(c = input.get()));
        }
        return c;
    }

    /**
     * Returns <code>true</code> if {@code c} is either a decimal digit or an identifier start character.
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is either a decimal digit or an identifier start character
     */
    private boolean isDecimalDigitOrIdentifierStart(int c) {
        return isDecimalDigit(c) || isIdentifierStart(c);
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
        throw new ParserException(ExceptionType.SyntaxError, parser.getSourceName(), getLine(), getColumn(), messageKey,
                args);
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
        throw new ParserEOFException(parser.getSourceName(), getLine(), getColumn(), messageKey, args);
    }

    /**
     * Reports a strict-mode error.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private void strictModeError(Messages.Key messageKey, String... args) {
        // Report the error from the start position of the currently parsed token.
        long sourcePosition = nextSourcePosition;
        parser.reportStrictModeError(ExceptionType.SyntaxError, sourcePosition, messageKey, args);
    }

    /**
     * Returns <code>true</code> and advances the source position if the current character is {@code c}. Otherwise
     * returns <code>false</code> and does not advance the source position.
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the current character matches
     */
    private boolean match(char c) {
        return input.match(c);
    }

    /**
     * Advances the source position if the current character is {@code c}. Otherwise throws a parser exception.
     * 
     * @param c
     *            the character to test
     */
    private void mustMatch(char c) {
        if (input.getChar() != c) {
            throw error(Messages.Key.IllegalCharacter, String.valueOf(c));
        }
    }
}
