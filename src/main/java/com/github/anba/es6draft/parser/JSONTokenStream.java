/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.parser.Characters.hexDigit;
import static com.github.anba.es6draft.parser.Characters.isDecimalDigit;
import static com.github.anba.es6draft.parser.NumberParser.parseDecimal;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.3 The JSON Object</h2><br>
 * <h3>24.3.1 The JSON Grammar</h3>
 * <ul>
 * <li>24.3.1.1 The JSON Lexical Grammar
 * </ul>
 */
final class JSONTokenStream {
    private final JSONParser parser;
    private final TokenStreamInput input;

    /** current line number */
    private int line;
    /** start position of current line */
    private int linestart;

    // token data
    /** current token in stream */
    private Token current;
    /** start line/column info for current token */
    private long sourcePosition;

    // literal data
    private final StrBuffer buffer;
    private double number = 0;

    public JSONTokenStream(JSONParser parser, TokenStreamInput input) {
        this.parser = parser;
        this.input = input;
        this.buffer = new StrBuffer(input.length());
        this.line = 1;
        this.linestart = input.position();
        this.current = scanToken();
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
     * Returns the last character from the input source.
     * 
     * @return the last character
     */
    public char lastChar() {
        return (char) input.lastChar();
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
     * Returns the string data of the current token.
     * 
     * @return the current string data
     */
    public String getString() {
        return buffer.toString();
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
     * Returns the raw input data of the current token.
     * 
     * @return the raw input data
     */
    public String getRaw() {
        int start = (int) ((sourcePosition >>> 32) + linestart - 1);
        return input.range(start, input.position());
    }

    /* token operations */

    /**
     * Advances the token stream to the next token.
     * 
     * @return the next token in the token stream
     */
    public Token nextToken() {
        return (current = scanToken());
    }

    /**
     * Returns the current token.
     * 
     * @return the current token
     */
    public Token currentToken() {
        return current;
    }

    /* lexer operations */

    private Token scanToken() {
        TokenStreamInput input = this.input;

        int c;
        for (;;) {
            c = input.getChar();
            if (c == TokenStreamInput.EOF) {
                return Token.EOF;
            } else if (c <= 0x20) {
                switch (c) {
                case 0x0D:
                    input.match('\n');
                case 0x0A:
                    incrementLine();
                case 0x09:
                case 0x20:
                    // skip over whitespace
                    continue;
                }
            }
            break;
        }
        sourcePosition = ((long) (input.position() - linestart) << 32) | line;

        switch (c) {
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
        case '-':
            return readNumber(c);
        case 'f':
            return readFalseLiteral(c);
        case 'n':
            return readNullLiteral(c);
        case 't':
            return readTrueLiteral(c);
        case '{':
            return Token.LC;
        case '}':
            return Token.RC;
        case '[':
            return Token.LB;
        case ']':
            return Token.RB;
        case ',':
            return Token.COMMA;
        case ':':
            return Token.COLON;
        }

        return Token.ERROR;
    }

    /**
     * <pre>
     * JSONNullLiteral::NullLiteral
     * </pre>
     * 
     * @param c
     *            the current character
     * @return the null token or {@link Token#ERROR}
     */
    private Token readNullLiteral(int c) {
        TokenStreamInput input = this.input;
        if (c == 'n' && input.getChar() == 'u' && input.getChar() == 'l' && input.getChar() == 'l') {
            return Token.NULL;
        }
        return Token.ERROR;
    }

    /**
     * <pre>
     * JSONBooleanLiteral::BooleanLiteral
     * </pre>
     * 
     * @param c
     *            the current character
     * @return the false token or {@link Token#ERROR}
     */
    private Token readFalseLiteral(int c) {
        TokenStreamInput input = this.input;
        if (c == 'f' && input.getChar() == 'a' && input.getChar() == 'l' && input.getChar() == 's'
                && input.getChar() == 'e') {
            return Token.FALSE;
        }
        return Token.ERROR;
    }

    /**
     * <pre>
     * JSONBooleanLiteral::BooleanLiteral
     * </pre>
     * 
     * @param c
     *            the current character
     * @return the true token or {@link Token#ERROR}
     */
    private Token readTrueLiteral(int c) {
        TokenStreamInput input = this.input;
        if (c == 't' && input.getChar() == 'r' && input.getChar() == 'u' && input.getChar() == 'e') {
            return Token.TRUE;
        }
        return Token.ERROR;
    }

    /**
     * <pre>
     * JSONString ::
     *     " JSONStringCharacters<span><sub>opt</sub></span> "
     * JSONStringCharacters ::
     *     JSONStringCharacter JSONStringCharacters<span><sub>opt</sub></span>
     * JSONStringCharacter ::
     *     SourceCharacter but not one of " or \ or U+0000 through U+001F
     *     \ JSONEscapeSequence
     * </pre>
     * 
     * @param quoteChar
     *            the quote character
     * @return the string token
     */
    private Token readString(int quoteChar) {
        assert quoteChar == '"';

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StrBuffer buffer = this.buffer();
        int start = input.position();
        for (;;) {
            int c = input.getChar();
            if (c == EOF) {
                throw error(Messages.Key.JSONUnterminatedStringLiteral);
            }
            if (c == quoteChar) {
                buffer.append(input, start, input.position() - 1);
                break;
            }
            if (c >= 0 && c <= 0x1F) {
                throw error(Messages.Key.JSONInvalidStringLiteral);
            }
            if (c != '\\') {
                continue;
            }
            buffer.append(input, start, input.position() - 1);
            buffer.append(readEscapeSequence());
            start = input.position();
        }
        return Token.STRING;
    }

    /**
     * <pre>
     * JSONEscapeSequence ::
     *     JSONEscapeCharacter
     *     u HexDigit HexDigit HexDigit HexDigit
     * JSONEscapeCharacter :: one of
     *     " / \ b f n r t
     * </pre>
     * 
     * @return the escaped character
     */
    private int readEscapeSequence() {
        TokenStreamInput input = this.input;
        int c = input.getChar();
        switch (c) {
        case '"':
        case '/':
        case '\\':
            break;
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
        case 'u':
            c = (hexDigit(input.getChar()) << 12) | (hexDigit(input.getChar()) << 8)
                    | (hexDigit(input.getChar()) << 4) | hexDigit(input.getChar());
            if (c < 0) {
                throw error(Messages.Key.JSONInvalidUnicodeEscape);
            }
            break;
        default:
            throw error(Messages.Key.JSONInvalidStringLiteral);
        }
        return c;
    }

    /**
     * <pre>
     * JSONNumber ::
     *      -<span><sub>opt</sub></span> DecimalIntegerLiteral JSONFraction<span><sub>opt</sub></span> ExponentPart<span><sub>opt</sub></span>
     * DecimalIntegerLiteral ::
     *      0
     *      NonZeroDigit DecimalDigits<span><sub>opt</sub></span>
     * JSONFraction ::
     *      . DecimalDigits
     * ExponentPart ::
     *      ExponentIndicator SignedInteger
     * </pre>
     * 
     * @param c
     *            the current character
     * @return the number token
     */
    private Token readNumber(int c) {
        number = readDecimalLiteral(c);
        return Token.NUMBER;
    }

    private double readDecimalLiteral(int c) {
        assert c == '-' || isDecimalDigit(c);
        TokenStreamInput input = this.input;
        StrBuffer buffer = this.buffer();
        if (c == '-') {
            buffer.append(c);
            if (!isDecimalDigit(c = input.getChar())) {
                throw error(Messages.Key.JSONInvalidNumberLiteral);
            }
        }
        buffer.append(c);
        if (c != '0') {
            while (isDecimalDigit(c = input.getChar())) {
                buffer.append(c);
            }
        } else {
            c = input.getChar();
        }
        if (c == '.') {
            buffer.append(c);
            if (!isDecimalDigit(c = input.getChar())) {
                throw error(Messages.Key.JSONInvalidNumberLiteral);
            }
            buffer.append(c);
            while (isDecimalDigit(c = input.getChar())) {
                buffer.append(c);
            }
        }
        if (c == 'e' || c == 'E') {
            buffer.append(c);
            c = input.getChar();
            if (c == '+' || c == '-') {
                buffer.append(c);
                c = input.getChar();
            }
            if (!isDecimalDigit(c)) {
                throw error(Messages.Key.JSONInvalidNumberLiteral);
            }
            buffer.append(c);
            while (isDecimalDigit(c = input.getChar())) {
                buffer.append(c);
            }
        }
        input.ungetChar(c);
        return parseDecimal(buffer.array(), buffer.length());
    }

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
     * Throws a {@link ParserException}.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException error(Messages.Key messageKey, String... args) {
        throw new ParserException(ExceptionType.SyntaxError, parser.getSourceName(), getLine(),
                getColumn(), messageKey, args);
    }
}
