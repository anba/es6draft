/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import com.github.anba.es6draft.repl.Repl;
import com.github.anba.es6draft.runtime.Realm;

/**
 * Extended console abstraction for {@link Repl}
 */
public interface ReplConsole extends ShellConsole {
    /**
     * Install completion support
     */
    boolean addCompletion(Realm realm);

    /**
     * Whether or not ANSI mode is supported
     */
    boolean isAnsiSupported();

    /**
     * Print the format string
     */
    void printf(String format, Object... args);

    /**
     * Read the current line
     */
    String readLine(String prompt);
}
