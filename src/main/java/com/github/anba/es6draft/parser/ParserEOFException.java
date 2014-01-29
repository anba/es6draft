/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import com.github.anba.es6draft.runtime.internal.Messages;

/**
 * {@link ParserException} subclass for parser EOF exceptions
 */
@SuppressWarnings("serial")
public final class ParserEOFException extends ParserException {
    public ParserEOFException(String file, int line, int column, Messages.Key messageKey,
            String... args) {
        super(ExceptionType.SyntaxError, file, line, column, messageKey, args);
    }
}
