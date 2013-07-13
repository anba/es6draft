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
import com.github.anba.es6draft.runtime.internal.InternalException;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * {@link RuntimeException} subclass for parser exceptions
 */
@SuppressWarnings("serial")
public class ParserException extends InternalException {
    public enum ExceptionType {
        SyntaxError, ReferenceError
    }

    private final Messages.Key messageKey;
    private final int line, column;
    private final ExceptionType type;
    private final String[] messageArguments;

    public ParserException(ExceptionType type, int line, int column, Messages.Key messageKey,
            String... args) {
        super(messageKey.name());
        this.type = type;
        this.line = line;
        this.column = column;
        this.messageKey = messageKey;
        this.messageArguments = args;
    }

    @Override
    public String getMessage() {
        String message = type.toString() + ": " + getFormattedMessage();
        if (line != -1 && column != -1) {
            message += " (line " + line + ", column " + column + ")";
        } else if (line != -1) {
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

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        if (type == ExceptionType.ReferenceError) {
            return throwReferenceError(cx, messageKey, messageArguments);
        }
        return throwSyntaxError(cx, messageKey, messageArguments);
    }
}
