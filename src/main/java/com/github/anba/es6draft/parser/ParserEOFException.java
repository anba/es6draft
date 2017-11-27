/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * {@link ParserException} subclass for end-of-file exceptions.
 */
@SuppressWarnings("serial")
public final class ParserEOFException extends ParserException {
    /**
     * Constructs a new parser exception with the given arguments.
     * 
     * @param file
     *            the source file location
     * @param line
     *            the source line
     * @param column
     *            the source column
     * @param messageKey
     *            the message key
     * @param args
     *            the message arguments
     */
    public ParserEOFException(String file, int line, int column, Messages.Key messageKey, String... args) {
        super(ExceptionType.SyntaxError, file, line, column, messageKey, args);
    }
}
