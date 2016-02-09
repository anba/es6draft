/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import static com.github.anba.es6draft.repl.global.SharedFunctions.absolutePath;
import static com.github.anba.es6draft.repl.global.SharedFunctions.loadScript;
import static com.github.anba.es6draft.repl.global.SharedFunctions.readFile;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.Strings;

/**
 * Standard shell functions.
 */
public final class BaseShellFunctions {
    /**
     * shell-function: {@code readline()}
     * 
     * @param cx
     *            the execution context
     * @return the read line from stdin
     */
    @Function(name = "readline", arity = 0)
    public String readline(ExecutionContext cx) {
        return cx.getRuntimeContext().getConsole().readLine();
    }

    /**
     * shell-function: {@code print(message)}
     * 
     * @param cx
     *            the execution context
     * @param messages
     *            the string to print
     */
    @Function(name = "print", arity = 1)
    public void print(ExecutionContext cx, String... messages) {
        PrintWriter writer = cx.getRuntimeContext().getConsole().writer();
        writer.println(Strings.concatWith(' ', messages));
    }

    /**
     * shell-function: {@code load(filename)}
     *
     * @param cx
     *            the execution context
     * @param filename
     *            the file to load
     */
    @Function(name = "load", arity = 1)
    public void load(ExecutionContext cx, String filename) {
        Path file = Paths.get(filename);
        loadScript(cx, file, absolutePath(cx, file));
    }

    /**
     * shell-function: {@code read(filename)}
     * 
     * @param cx
     *            the execution context
     * @param filename
     *            the file to load
     * @return the file content
     */
    @Function(name = "read", arity = 1)
    public String read(ExecutionContext cx, String filename) {
        Path file = Paths.get(filename);
        return readFile(cx, file, absolutePath(cx, file));
    }

    /**
     * shell-function: {@code quit()}
     */
    @Function(name = "quit", arity = 0)
    public void quit() {
        throw new StopExecutionException(StopExecutionException.Reason.Quit);
    }
}
