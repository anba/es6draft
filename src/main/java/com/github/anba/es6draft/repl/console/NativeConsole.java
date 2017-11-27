/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import java.io.Console;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * {@link ShellConsole} implementation for native consoles.
 */
public final class NativeConsole implements ShellConsole {
    private final Console console;
    private final PrintWriter errorWriter;

    public NativeConsole() {
        this.console = System.console();
        this.errorWriter = new PrintWriter(System.err, true);
    }

    @Override
    public void printf(String format, Object... args) {
        console.printf(format, args);
        console.flush();
    }

    @Override
    public void flush() {
        console.flush();
        errorWriter.flush();
    }

    @Override
    public String readLine() {
        return console.readLine();
    }

    @Override
    public String readLine(String prompt) {
        if (!prompt.isEmpty()) {
            console.writer().append(prompt).flush();
        }
        return readLine();
    }

    @Override
    public Reader reader() {
        return console.reader();
    }

    @Override
    public PrintWriter writer() {
        return console.writer();
    }

    @Override
    public PrintWriter errorWriter() {
        return errorWriter;
    }
}
