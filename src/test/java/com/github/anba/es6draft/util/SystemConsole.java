/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import com.github.anba.es6draft.repl.console.ShellConsole;

/** 
 *
 */
public final class SystemConsole implements ShellConsole {
    private final StringReader reader = new StringReader("");
    private final PrintWriter writer = new PrintWriter(System.out);
    private final PrintWriter errorWriter = new PrintWriter(System.err);

    @Override
    public void printf(String format, Object... args) {
        writer.printf(format, args);
    }

    @Override
    public String readLine() {
        return "";
    }

    @Override
    public String readLine(String prompt) {
        return "";
    }

    @Override
    public Reader reader() {
        return reader;
    }

    @Override
    public PrintWriter writer() {
        return writer;
    }

    @Override
    public PrintWriter errorWriter() {
        return errorWriter;
    }

    @Override
    public boolean isAnsiSupported() {
        return false;
    }

    @Override
    public void addCompleter(Completer completer) {
    }
}
