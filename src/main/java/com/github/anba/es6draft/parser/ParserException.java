/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import java.util.Locale;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.InternalException;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * {@link InternalException} subclass for parser exceptions.
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

    /**
     * Constructs a new parser exception with the given arguments.
     * 
     * @param type
     *            the exception type
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

    @Override
    public String getMessage() {
        String message = type.toString() + ": " + getFormattedMessage();
        if (line != -1 && column != -1) {
            message += " (line " + line + ", column " + column + ")";
        } else if (line != -1) {
            message += " (line " + line + ")";
        }
        // FIXME: Add file info
        return message;
    }

    /**
     * Returns the type of this parser exception.
     * 
     * @return the exception type
     */
    public ExceptionType getType() {
        return type;
    }

    /**
     * Returns the source file location.
     * 
     * @return the source file location
     */
    public String getFile() {
        return file;
    }

    /**
     * Returns the line number of this exception.
     * 
     * @return the line number describing the error location
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number of this exception.
     * 
     * @return the column number describing the error location
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the message key of this exception.
     * 
     * @return the error message key
     */
    public Messages.Key getMessageKey() {
        return messageKey;
    }

    /**
     * Returns the message arguments of this exception.
     * 
     * @return the error message arguments
     */
    public String[] getMessageArguments() {
        return messageArguments.clone();
    }

    /**
     * Returns a formatted message describing this exception.
     * 
     * @return the formatted error message in the default locale
     */
    public String getFormattedMessage() {
        return getFormattedMessage(Locale.getDefault());
    }

    /**
     * Returns a formatted message describing this exception.
     * 
     * @param locale
     *            the requested locale
     * @return the formatted error message in the specified locale
     */
    public String getFormattedMessage(Locale locale) {
        return Messages.create(locale).getMessage(messageKey, messageArguments);
    }

    /**
     * Returns a formatted message describing this exception.
     * 
     * @param realm
     *            the realm object
     * @return the formatted error message in the locale of the realm object
     */
    public String getFormattedMessage(Realm realm) {
        return realm.message(messageKey, messageArguments);
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        String message = getFormattedMessage(cx.getRealm());
        if (type == ExceptionType.ReferenceError) {
            return Errors.newReferenceError(cx, this, message, getFile(), getLine(), getColumn());
        }
        return Errors.newSyntaxError(cx, this, message, getFile(), getLine(), getColumn());
    }
}
