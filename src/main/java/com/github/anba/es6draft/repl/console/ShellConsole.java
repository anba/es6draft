/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import com.github.anba.es6draft.repl.global.ShellGlobalObject;

/**
 * Console abstraction for {@link ShellGlobalObject}
 */
public interface ShellConsole {
    /**
     * Read the current line.
     * 
     * @return the current line
     */
    String readLine();

    /**
     * Ouput a single string.
     * 
     * @param s
     *            the output string
     */
    void putstr(String s);

    /**
     * Output a single string and add a line-break.
     * 
     * @param s
     *            the output string
     */
    void print(String s);

    /**
     * Output an error message.
     * 
     * @param s
     *            the output error message
     */
    void printErr(String s);
}
