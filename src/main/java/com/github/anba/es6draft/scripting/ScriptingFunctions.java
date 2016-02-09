/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import java.io.PrintWriter;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Strings;

/**
 * Built-in functions for the JSR-223 Scripting API.
 */
public final class ScriptingFunctions {
    /**
     * builtin-function: {@code print(message)}
     *
     * @param cx
     *            the execution context
     * @param messages
     *            the string to print
     */
    @Function(name = "print", arity = 1)
    public void print(ExecutionContext cx, String... messages) {
        PrintWriter writer = cx.getRuntimeContext().getWriter();
        writer.println(Strings.concatWith(' ', messages));
        writer.flush();
    }
}
