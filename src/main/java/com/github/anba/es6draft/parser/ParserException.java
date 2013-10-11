/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import java.text.MessageFormat;
import java.util.Locale;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.InternalException;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.types.Intrinsics;

/**
 * {@link RuntimeException} subclass for parser exceptions
 */
@SuppressWarnings("serial")
public class ParserException extends InternalException {
    public enum ExceptionType {
        SyntaxError, ReferenceError
    }

    private final ExceptionType type;
    private final String file;
    private final int line, column;
    private final Messages.Key messageKey;
    private final String[] messageArguments;

    public ParserException(ExceptionType type, String file, int line, int column,
            Messages.Key messageKey, String... args) {
        super(messageKey.name());
        this.type = type;
        this.file = file;
        this.line = line;
        this.column = column;
        this.messageKey = messageKey;
        this.messageArguments = args;
    }

    private String format(String pattern, Locale locale) {
        MessageFormat format = new MessageFormat(pattern, locale);
        return format.format(messageArguments);
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

    public ExceptionType getType() {
        return type;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getFormattedMessage() {
        return getFormattedMessage(Locale.getDefault());
    }

    public String getFormattedMessage(Locale locale) {
        return format(Messages.create(locale).getString(messageKey), locale);
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        Realm realm = cx.getRealm();
        String message = format(realm.message(messageKey), realm.getLocale());
        if (type == ExceptionType.ReferenceError) {
            return Errors.newError(cx, Intrinsics.ReferenceError, message, getFile(), getLine(),
                    getColumn());
        }
        return Errors.newError(cx, Intrinsics.SyntaxError, message, getFile(), getLine(),
                getColumn());
    }
}
