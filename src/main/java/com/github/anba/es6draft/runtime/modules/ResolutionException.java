/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import java.util.Locale;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.InternalThrowable;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * Exception for module resolution errors.
 */
@SuppressWarnings("serial")
public final class ResolutionException extends Exception implements InternalThrowable {
    private final Messages.Key messageKey;
    private final String[] messageArguments;

    /**
     * Constructs a new module resolution exception.
     * 
     * @param messageKey
     *            the message key
     * @param messageArguments
     *            the message arguments
     */
    public ResolutionException(Messages.Key messageKey, String... messageArguments) {
        this.messageKey = messageKey;
        this.messageArguments = messageArguments;
    }

    private String getFormattedMessage(Locale locale) {
        return Messages.create(locale).getMessage(messageKey, messageArguments);
    }

    @Override
    public String getMessage() {
        return getFormattedMessage(Locale.ROOT);
    }

    @Override
    public String getLocalizedMessage() {
        return getFormattedMessage(Locale.getDefault());
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        throw Errors.newSyntaxError(cx, this, messageKey, messageArguments);
    }

    public String getScriptMessage(ExecutionContext cx) {
        return Errors.newSyntaxError(cx, this, messageKey, messageArguments).getMessage(cx);
    }
}
