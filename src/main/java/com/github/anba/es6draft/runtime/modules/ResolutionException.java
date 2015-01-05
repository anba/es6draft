/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.modules;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.InternalException;
import com.github.anba.es6draft.runtime.internal.Messages;
import com.github.anba.es6draft.runtime.internal.ScriptException;

/**
 * 
 */
@SuppressWarnings("serial")
public final class ResolutionException extends InternalException {
    private final Messages.Key messageKey;
    private final String messageArgument;

    public ResolutionException(Messages.Key messageKey, String messageArgument) {
        super(messageKey.toString() + ": " + messageArgument);
        this.messageKey = messageKey;
        this.messageArgument = messageArgument;
    }

    @Override
    public ScriptException toScriptException(ExecutionContext cx) {
        throw Errors.newSyntaxError(cx, messageKey, messageArgument);
    }
}
