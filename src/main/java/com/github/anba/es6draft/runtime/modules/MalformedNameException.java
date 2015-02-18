/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

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
        super("cannot normalize name: " + unnormalizedName);
        this.unnormalizedName = unnormalizedName;
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        return Errors.newSyntaxError(cx, Messages.Key.ModulesInvalidName, unnormalizedName);
    }
}
