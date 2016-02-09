/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.runtime.ExecutionContext;

/** 
 *
 */
public interface InternalThrowable {
    /**
     * Returns a {@link ScriptException} for this exception object.
     * 
     * @param cx
     *            the execution context to construct the script exception
     * @return the script exception for this exception
     */
    ScriptException toScriptException(ExecutionContext cx);
}
