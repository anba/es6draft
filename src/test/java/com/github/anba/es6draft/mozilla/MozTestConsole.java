/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import org.junit.rules.ErrorCollector;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.util.TestAssertions;

/**
 *
 */
final class MozTestConsole implements ShellConsole {
    private final ErrorCollector collector;

    MozTestConsole(ErrorCollector collector) {
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
        if (s.startsWith(" FAILED! ")) {
            // collect all failures instead of calling fail() directly
            collector.addError(TestAssertions.newAssertionError(s));
        }
    }

    @Override
    public void printErr(String s) {
    }
}
