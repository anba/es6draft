/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.runtime.internal.Errors.throwReferenceError;
import static com.github.anba.es6draft.runtime.internal.Errors.throwSyntaxError;

import java.text.MessageFormat;
import java.util.Locale;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * {@link RuntimeException} subclass for parser exceptions
 */
@SuppressWarnings("serial")
public class ParserException extends RuntimeException {
    public enum ExceptionType {
        SyntaxError, ReferenceError
    }

    private final Messages.Key messageKey;
    private final int line;
    private final ExceptionType type;
    private final String[] messageArguments;

    public ParserException(ExceptionType type, int line, Messages.Key messageKey, String... args) {
        super(messageKey.name());
        this.type = type;
        this.line = line;
        this.messageKey = messageKey;
        this.messageArguments = args;
    }

    @Override
    public String getMessage() {
        String message = getFormattedMessage();
        if (line != -1) {
            message += " (line " + line + ")";
        }
        return message;
    }

    public String getFormattedMessage() {
        return getFormattedMessage(Locale.getDefault());
    }

    public String getFormattedMessage(Locale locale) {
        String pattern = Messages.create(locale).getString(messageKey);
        MessageFormat format = new MessageFormat(pattern, locale);
        return format.format(messageArguments);
    }

    public ScriptException toScriptException(ExecutionContext cx) {
        if (getExceptionType() == ExceptionType.ReferenceError) {
            return throwReferenceError(cx, messageKey, messageArguments);
        }
        return throwSyntaxError(cx, messageKey, messageArguments);
    }

    public ExceptionType getExceptionType() {
        return type;
    }

    public int getLine() {
        return line;
    }
}
