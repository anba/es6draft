/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.language;

import com.github.anba.es6draft.runtime.ExecutionContext;

/**
 * 
 */
public final class DebuggerOperations {
    private DebuggerOperations() {
    }

    /**
     * 13.15 The debugger statement
     * 
     * @param cx
     *            the execution context
     */
    public static void debugger(ExecutionContext cx) {
        cx.getRuntimeContext().getDebugger().accept(cx);
    }
}
