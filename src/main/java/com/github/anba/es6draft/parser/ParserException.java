/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * {@link RuntimeException} subclass for parser exceptions
 */
@SuppressWarnings("serial")
public class ParserException extends RuntimeException {
    public enum ExceptionType {
        SyntaxError, ReferenceError
    }

    private final int line;
    private final ExceptionType type;

    public ParserException(String message, int line, ExceptionType type) {
        super(message);
        this.line = line;
        this.type = type;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (line != -1) {
            message += " (@" + line + ")";
        }
        return message;
    }

    public String getPlainMessage() {
        return super.getMessage();
    }

    public ExceptionType getExceptionType() {
        return type;
    }
}
