/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.repl.console.ShellConsole;

/**
 *
 */
final class ScriptTestConsole implements ShellConsole {
    @Override
    public String readLine() {
        return "";
    }

    @Override
    public void putstr(String s) {
        System.out.print(s);
    }

    @Override
    public void print(String s) {
        System.out.println(s);
    }

    @Override
    public void printErr(String s) {
        System.err.println(s);
    }
}
