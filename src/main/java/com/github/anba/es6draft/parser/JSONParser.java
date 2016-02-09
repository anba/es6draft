/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.runtime.types.Null.NULL;
import static com.github.anba.es6draft.runtime.types.builtins.ArrayObject.ArrayCreate;
import static com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject.ObjectCreate;

import com.github.anba.es6draft.parser.ParserException.ExceptionType;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.types.Intrinsics;
import com.github.anba.es6draft.runtime.types.PropertyDescriptor;
import com.github.anba.es6draft.runtime.types.builtins.ArrayObject;
import com.github.anba.es6draft.runtime.types.builtins.OrdinaryObject;

/**
 * <h1>24 Structured Data</h1><br>
 * <h2>24.3 The JSON Object</h2><br>
 * <h3>24.3.1 The JSON Grammar</h3>
 * <ul>
 * <li>24.3.1.2 The JSON Syntactic Grammar
 * </ul>
 */
public final class JSONParser {
    private static final boolean DEBUG = false;

    private boolean parseCalled = false;
    private final JSONTokenStream ts;
    private final ExecutionContext cx;
    private final String sourceName;

    public JSONParser(ExecutionContext cx, String source) {
        this.cx = cx;
        this.sourceName = "<json>";
        ts = new JSONTokenStream(this, new TokenStreamInput(source));
    }

    private static int toLine(long sourcePosition) {
        return (int) sourcePosition;
    }

    private static int toColumn(long sourcePosition) {
        return (int) (sourcePosition >>> 32);
    }

    String getSourceName() {
        return sourceName;
    }

    /**
     * Reports a mismatched token error from tokenstream's current position.
     * 
     * @param expected
     *            the expected token
     * @param actual
     *            the actual token in the token stream
     * @return the parser exception
     */
    private ParserException reportTokenMismatch(Token expected, Token actual) {
        if (actual == Token.EOF) {
            throw reportEofError(Messages.Key.UnexpectedEndOfFile, expected.toString());
        }
        if (actual == Token.ERROR) {
            throw reportSyntaxError(Messages.Key.UnexpectedCharacter,
                    String.valueOf(ts.lastChar()), expected.toString());
        }
        throw reportSyntaxError(Messages.Key.UnexpectedToken, actual.toString(),
                expected.toString());
    }

    /**
     * Reports a parser eof-error from tokenstream's current position.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserEOFException reportEofError(Messages.Key messageKey, String... args) {
        long sourcePosition = ts.sourcePosition();
        int line = toLine(sourcePosition), column = toColumn(sourcePosition);
        throw new ParserEOFException(getSourceName(), line, column, messageKey, args);
    }

    /**
     * Reports a syntax error from tokenstream's current position.
     * 
     * @param messageKey
     *            the error message key
     * @param args
     *            the error message arguments
     * @return the parser exception
     */
    private ParserException reportSyntaxError(Messages.Key messageKey, String... args) {
        long sourcePosition = ts.sourcePosition();
        int line = toLine(sourcePosition), column = toColumn(sourcePosition);
        throw new ParserException(ExceptionType.SyntaxError, getSourceName(), line, column,
                messageKey, args);
    }

    /**
     * Returns the current token in the token-stream.
     * 
     * @return the current token
     */
    private Token token() {
        return ts.currentToken();
    }

    /**
     * Consumes the current token in the token-stream and advances the stream to the next token.
     * 
     * @param tok
     *            the token to consume
     */
    private void consume(Token tok) {
        if (tok != token())
            reportTokenMismatch(tok, token());
        Token next = ts.nextToken();
        if (DEBUG)
            System.out.printf("consume(%s) -> %s\n", tok, next);
    }

    /**
     * Parses the input source string as a JSON text and returns its value. Throws a
     * {@link ParserException} if the source string is not a valid JSON text.
     * 
     * @return the value of the parsed JSON text
     * @throws ParserException
     *             if the input source is not a valid JSON text
     */
    public Object parse() throws ParserException {
        if (parseCalled)
            throw new IllegalStateException();
        parseCalled = true;
        return jsonText();
    }

    private <DOCUMENT, OBJECT, ARRAY, VALUE> DOCUMENT parse(
            JSONBuilder<DOCUMENT, OBJECT, ARRAY, VALUE> builder) throws ParserException {
        if (parseCalled)
            throw new IllegalStateException();
        parseCalled = true;
        return jsonText(builder);
    }

    /**
     * Parses the input source string as a JSON text and returns its value. Throws a
     * {@link ParserException} if the source string is not a valid JSON text.
     * 
     * @param cx
     *            the execution context
     * @param source
     *            the source string
     * @return the value of the parsed JSON text
     * @throws ParserException
     *             if the input source is not a valid JSON text
     */
    public static Object parse(ExecutionContext cx, String source) throws ParserException {
        return new JSONParser(cx, source).parse();
    }

    /**
     * Parses the input source string as a JSON text. Throws a {@link ParserException} if the source
     * string is not a valid JSON text.
     * 
     * @param <DOCUMENT>
     *            the document type
     * @param <OBJECT>
     *            the object type
     * @param <ARRAY>
     *            the array type
     * @param <VALUE>
     *            the value type
     * @param source
     *            the source string
     * @param builder
     *            the builder object
     * @return the value of the parsed JSON text
     * @throws ParserException
     *             if the input source is not a valid JSON text
     */
    public static <DOCUMENT, OBJECT, ARRAY, VALUE> DOCUMENT parse(String source,
            JSONBuilder<DOCUMENT, OBJECT, ARRAY, VALUE> builder) throws ParserException {
        return new JSONParser(null, source).parse(builder);
    }

    /* ***************************************************************************************** */

    /**
     * <pre>
     * JSONText :   
     *      JSONValue
     * </pre>
     * 
     * @return the value of JSON text
     */
    private Object jsonText() {
        Object value = jsonValue();
        consume(Token.EOF);
        return value;
    }

    private <DOCUMENT, OBJECT, ARRAY, VALUE> DOCUMENT jsonText(
            JSONBuilder<DOCUMENT, OBJECT, ARRAY, VALUE> builder) {
        VALUE value = jsonValue(builder);
        consume(Token.EOF);
        return builder.createDocument(value);
    }

    /**
     * <pre>
     * JSONValue : 
     *      JSONNullLiteral
     *      JSONBooleanLiteral
     *      JSONObject
     *      JSONArray
     *      JSONString
     *      JSONNumber
     * </pre>
     * 
     * @return the parsed JSON value
     */
    private Object jsonValue() {
        Token tok = token();
        switch (tok) {
        case NULL:
            consume(tok);
            return NULL;
        case FALSE:
            consume(tok);
            return Boolean.FALSE;
        case TRUE:
            consume(tok);
            return Boolean.TRUE;
        case STRING:
            consume(tok);
            return ts.getString();
        case NUMBER:
            consume(tok);
            return ts.getNumber();
        case LC:
            return jsonObject();
        case LB:
            return jsonArray();
        default:
            reportSyntaxError(Messages.Key.InvalidToken, tok.toString());
            return null;
        }
    }

    private <DOCUMENT, OBJECT, ARRAY, VALUE> VALUE jsonValue(
            JSONBuilder<DOCUMENT, OBJECT, ARRAY, VALUE> builder) {
        Token tok = token();
        switch (tok) {
        case NULL:
            consume(tok);
            return builder.newNull();
        case FALSE:
            consume(tok);
            return builder.newBoolean(false);
        case TRUE:
            consume(tok);
            return builder.newBoolean(true);
        case STRING: {
            String string = ts.getString();
            String rawValue = ts.getRaw();
            consume(tok);
            return builder.newString(string, rawValue);
        }
        case NUMBER: {
            double number = ts.getNumber();
            String rawValue = ts.getRaw();
            consume(tok);
            return builder.newNumber(number, rawValue);
        }
        case LC:
            return jsonObject(builder);
        case LB:
            return jsonArray(builder);
        default:
            throw reportSyntaxError(Messages.Key.InvalidToken, tok.toString());
        }
    }

    /**
     * <pre>
     * JSONObject :
     *      { }
     *      { JSONMemberList }
     * JSONMemberList :
     *      JSONMember 
     *      JSONMemberList , JSONMember
     * JSONMember :
     *      JSONString : JSONValue
     * </pre>
     * 
     * @return the script object represented by the JSON object
     */
    private OrdinaryObject jsonObject() {
        OrdinaryObject object = ObjectCreate(cx, Intrinsics.ObjectPrototype);
        consume(Token.LC);
        if (token() != Token.RC) {
            for (;;) {
                consume(Token.STRING);
                String name = ts.getString();
                consume(Token.COLON);
                Object value = jsonValue();
                object.defineOwnProperty(cx, name, new PropertyDescriptor(value, true, true, true));
                if (token() == Token.RC) {
                    break;
                }
                consume(Token.COMMA);
            }
        }
        consume(Token.RC);
        return object;
    }

    private <DOCUMENT, OBJECT, ARRAY, VALUE> VALUE jsonObject(
            JSONBuilder<DOCUMENT, OBJECT, ARRAY, VALUE> builder) {
        consume(Token.LC);
        OBJECT object = builder.newObject();
        if (token() != Token.RC) {
            for (long index = 0;; ++index) {
                String name = ts.getString();
                String rawName = ts.getRaw();
                consume(Token.STRING);
                builder.newProperty(object, name, rawName, index);
                consume(Token.COLON);
                VALUE value = jsonValue(builder);
                builder.finishProperty(object, name, rawName, index, value);
                if (token() == Token.RC) {
                    break;
                }
                consume(Token.COMMA);
            }
        }
        consume(Token.RC);
        return builder.finishObject(object);
    }

    /**
     * <pre>
     * JSONArray :
     *      [ ]
     *      [ JSONElementList ]
     * JSONElementList :
     *      JSONValue
     *      JSONElementList , JSONValue
     * </pre>
     * 
     * @return the script object represented by the JSON array
     */
    private ArrayObject jsonArray() {
        ArrayObject array = ArrayCreate(cx, 0);
        consume(Token.LB);
        if (token() != Token.RB) {
            for (long index = 0;;) {
                Object value = jsonValue();
                array.defineOwnProperty(cx, index++,
                        new PropertyDescriptor(value, true, true, true));
                if (token() == Token.RB) {
                    break;
                }
                consume(Token.COMMA);
            }
        }
        consume(Token.RB);
        return array;
    }

    private <DOCUMENT, OBJECT, ARRAY, VALUE> VALUE jsonArray(
            JSONBuilder<DOCUMENT, OBJECT, ARRAY, VALUE> builder) {
        consume(Token.LB);
        ARRAY array = builder.newArray();
        if (token() != Token.RB) {
            for (long index = 0;; ++index) {
                builder.newElement(array, index);
                VALUE value = jsonValue(builder);
                builder.finishElement(array, index, value);
                if (token() == Token.RB) {
                    break;
                }
                consume(Token.COMMA);
            }
        }
        consume(Token.RB);
        return builder.finishArray(array);
    }
}
