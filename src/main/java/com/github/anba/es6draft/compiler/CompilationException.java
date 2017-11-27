/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.compiler;

import java.util.Locale;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.InternalException;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * Exception for compilation errors.
 */
@SuppressWarnings("serial")
public final class CompilationException extends InternalException {
    private final Messages.Key messageKey;
    private final String[] messageArguments;

    /**
     * Constructs a new compilation exception.
     * 
     * @param message
     *            the error message
     */
    public CompilationException(String message) {
        this.messageKey = Messages.Key.InternalError;
        this.messageArguments = new String[] { message };
    }

    /**
     * Constructs a new compilation exception.
     * 
     * @param message
     *            the error message
     * @param cause
     *            the exception cause
     */
    public CompilationException(String message, Throwable cause) {
        super(cause);
        this.messageKey = Messages.Key.InternalError;
        this.messageArguments = new String[] { message };
    }

    /**
     * Constructs a new compilation exception.
     * 
     * @param messageKey
     *            the message key
     * @param args
     *            the message arguments
     */
    public CompilationException(Messages.Key messageKey, String... args) {
        this.messageKey = messageKey;
        this.messageArguments = args;
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        return Errors.newInternalError(cx, this, messageKey, messageArguments);
    }

    @Override
    public String getMessage() {
        return Messages.create(Locale.ROOT).getMessage(messageKey, messageArguments);
    }

    @Override
    public String getLocalizedMessage() {
        return Messages.create(Locale.getDefault()).getMessage(messageKey, messageArguments);
    }
}
