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
import com.github.anba.es6draft.runtime.types.Type;

/**
 * 
 */
@SuppressWarnings("serial")
public class ScriptException extends RuntimeException {
    private final Object value;

    public ScriptException(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String getMessage() {
        if (!Type.isObject(value)) {
            return AbstractOperations.ToFlatString(null, value);
        }
        return Objects.toString(value);
    }

    public String getMessage(ExecutionContext cx) {
        try {
            return AbstractOperations.ToFlatString(cx, value);
        } catch (ScriptException t) {
            return Objects.toString(value);
        }
    }
}
