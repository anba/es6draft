/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.moztest;

import java.util.ArrayList;
import java.util.List;

import com.github.anba.es6draft.repl.MozShellConsole;

/**
 *
 */
class MozTestConsole implements MozShellConsole {
    private List<Throwable> failures = new ArrayList<Throwable>();

    public List<Throwable> getFailures() {
        return failures;
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
            failures.add(new AssertionError(s));
        }
    }

    @Override
    public void printErr(String s) {
    }
}
