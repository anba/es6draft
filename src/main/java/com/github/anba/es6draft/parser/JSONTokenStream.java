/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.parser.NumberParser.parseDecimal;

import java.util.Arrays;

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
public class JSONTokenStream {
    private static final boolean DEBUG = false;

    private final TokenStreamInput input;
    // token data
    private Token current;
    // literal data
    private StringBuffer buffer = new StringBuffer();
    private double number = 0;

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
    }

    public JSONTokenStream(TokenStreamInput input) {
        this.input = input;
        this.current = scanToken();
    }

    public String getString() {
        return new String(buffer.cbuf, 0, buffer.length);
    }

    public double getNumber() {
        return number;
    }

    //

    public Token nextToken() {
        return (current = scanToken());
    }

    public Token currentToken() {
        return current;
    }

    //

    private Token scanToken() {
        TokenStreamInput input = this.input;

        int c;
        for (;;) {
            c = input.get();
            if (c == TokenStreamInput.EOF) {
                return Token.EOF;
            } else if (c <= 0x20) {
                if (c == 0x09 || c == 0x0A || c == 0x0D || c == 0x20) {
                    // skip over whitespace
                    continue;
                }
            }
            break;
        }
        if (DEBUG)
            System.out.printf("scanToken() -> %c\n", (char) c);

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

    private Token readNullLiteral(int c) {
        TokenStreamInput input = this.input;
        if (c == 'n' && input.get() == 'u' && input.get() == 'l' && input.get() == 'l') {
            return Token.NULL;
        }
        return Token.ERROR;
    }

    private Token readFalseLiteral(int c) {
        TokenStreamInput input = this.input;
        if (c == 'f' && input.get() == 'a' && input.get() == 'l' && input.get() == 's'
                && input.get() == 'e') {
            return Token.FALSE;
        }
        return Token.ERROR;
    }

    private Token readTrueLiteral(int c) {
        TokenStreamInput input = this.input;
        if (c == 't' && input.get() == 'r' && input.get() == 'u' && input.get() == 'e') {
            return Token.TRUE;
        }
        return Token.ERROR;
    }

    private Token readString(int quoteChar) {
        assert quoteChar == '"';

        final int EOF = TokenStreamInput.EOF;
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        for (;;) {
            int c = input.get();
            if (c == EOF) {
                throw error(Messages.Key.JSONUnterminatedStringLiteral);
            }
            if (c == quoteChar) {
                break;
            }
            if (c >= 0 && c <= 0x1F) {
                throw error(Messages.Key.JSONInvalidStringLiteral);
            }
            if (c != '\\') {
                // TODO: add substring range
                buffer.add(c);
                continue;
            }
            c = input.get();
            // escape sequences
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
                c = (hexDigit(input.get()) << 12) | (hexDigit(input.get()) << 8)
                        | (hexDigit(input.get()) << 4) | hexDigit(input.get());
                if (c < 0) {
                    throw error(Messages.Key.JSONInvalidUnicodeEscape);
                }
                break;
            default:
                throw error(Messages.Key.JSONInvalidStringLiteral);
            }
            buffer.add(c);
        }

        return Token.STRING;
    }

    /**
     * <pre>
     * JSONNumber ::
     *      -[opt]  DecimalIntegerLiteral JSONFraction[opt]  ExponentPart[opt]
     * DecimalIntegerLiteral ::
     *      0
     *      NonZeroDigit DecimalDigits[opt]
     * JSONFraction ::
     *      . DecimalDigits
     * ExponentPart ::
     *      ExponentIndicator SignedInteger
     * </pre>
     */
    private Token readNumber(int c) {
        number = readDecimalLiteral(c);
        return Token.NUMBER;
    }

    private double readDecimalLiteral(int c) {
        assert c == '-' || isDigit(c);
        TokenStreamInput input = this.input;
        StringBuffer buffer = this.buffer();
        if (c == '-') {
            buffer.add(c);
            if (!isDigit(c = input.get())) {
                throw error(Messages.Key.JSONInvalidNumberLiteral);
            }
        }
        buffer.add(c);
        if (c != '0') {
            while (isDigit(c = input.get())) {
                buffer.add(c);
            }
        } else {
            c = input.get();
        }
        if (c == '.') {
            buffer.add(c);
            if (!isDigit(c = input.get())) {
                throw error(Messages.Key.JSONInvalidNumberLiteral);
            }
            buffer.add(c);
            while (isDigit(c = input.get())) {
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
            if (!isDigit(c)) {
                throw error(Messages.Key.JSONInvalidNumberLiteral);
            }
            buffer.add(c);
            while (isDigit(c = input.get())) {
                buffer.add(c);
            }
        }
        input.unget(c);
        return parseDecimal(buffer.cbuf, buffer.length);
    }

    private StringBuffer buffer() {
        StringBuffer buffer = this.buffer;
        buffer.clear();
        return buffer;
    }

    private static boolean isDigit(int c) {
        return (c >= '0' && c <= '9');
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
        throw new ParserException(ExceptionType.SyntaxError, "<json>", 1, 1, messageKey, args);
    }
}
