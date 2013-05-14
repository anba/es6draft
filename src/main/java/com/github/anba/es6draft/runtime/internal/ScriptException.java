/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.util.Objects;

import com.github.anba.es6draft.runtime.AbstractOperations;
import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * 
 */
@SuppressWarnings("serial")
public class ScriptException extends RuntimeException {
    private final Object value;

    public ScriptException(Object value) {
        // super(null, null, false, false);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        try {
            return AbstractOperations.ToFlatString(null, value);
        } catch (ScriptException | NullPointerException t) {
            return Objects.toString(value);
        }
    }

    public String getMessage(ExecutionContext cx) {
        try {
            return AbstractOperations.ToFlatString(cx, value);
        } catch (ScriptException t) {
            return Objects.toString(value);
        }
    }
}
