/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.PrintWriter;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Strings;

/**
 * Built-in functions for the v8-shell.
 */
public final class V8ShellFunctions {
    /**
     * shell-function: {@code write(message)}
     *
     * @param cx
     *            the execution context
     * @param messages
     *            the strings to write
     */
    @Function(name = "write", arity = 1)
    public void write(ExecutionContext cx, String... messages) {
        PrintWriter writer = cx.getRuntimeContext().getWriter();
        writer.print(Strings.concatWith(' ', messages));
        writer.flush();
    }
}
