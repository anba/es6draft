/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.webkit;

import org.junit.rules.ErrorCollector;

import com.github.anba.es6draft.repl.console.ShellConsole;

/**
 *
 */
class WebKitTestConsole implements ShellConsole {
    private final ErrorCollector collector;

    WebKitTestConsole(ErrorCollector collector) {
        this.collector = collector;
    }

    @Override
    public String readLine() {
        return "";
    }

    @Override
    public void putstr(String s) {
    }

    @Override
    public void print(String s) {
        if (s.startsWith("FAIL ")) {
            // collect all failures instead of calling fail() directly
            collector.addError(new AssertionError(s));
        }
    }

    @Override
    public void printErr(String s) {
    }
}
