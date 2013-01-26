/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * {@link ScriptException} subclass for parser exceptions
 */
@SuppressWarnings("serial")
public class ParserEOFException extends ParserException {
    public ParserEOFException(String message, int line) {
        super(message, line, ExceptionType.SyntaxError);
    }
}
