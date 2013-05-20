/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl;

/**
 * Console abstraction for {@link MozShellGlobalObject}
 */
public interface ShellConsole {
    String readLine();

    void putstr(String s);

    void print(String s);

    void printErr(String s);
}
