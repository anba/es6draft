/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import com.github.anba.es6draft.repl.console.ShellConsole;

/**
 *
 */
final class TraceurConsole implements ShellConsole {
    @Override
    public String readLine() {
        return "";
    }

    @Override
    public void putstr(String s) {
    }

    @Override
    public void print(String s) {
    }

    @Override
    public void printErr(String s) {
    }
}
