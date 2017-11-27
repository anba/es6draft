/**
 * Copyright (c) André Bargull
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
 * Exception for malformed module name errors.
 */
@SuppressWarnings("serial")
public final class MalformedNameException extends Exception implements InternalThrowable {
    private final String unnormalizedName;

    /**
     * Constructs a new malformed-name exception.
     * 
     * @param unnormalizedName
     *            the unnormalized module name
     */
    public MalformedNameException(String unnormalizedName) {
        this.unnormalizedName = unnormalizedName;
    }

    private String getFormattedMessage(Locale locale) {
        return Messages.create(locale).getMessage(Messages.Key.ModulesInvalidName, unnormalizedName);
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
        return Errors.newSyntaxError(cx, this, Messages.Key.ModulesInvalidName, unnormalizedName);
    }
}
