/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Formatter;

/**
 * {@link ShellConsole} implementation for legacy consoles.
 */
public final class LegacyConsole implements ShellConsole {
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final PrintWriter errorWriter;
    private final Formatter formatter;

    public LegacyConsole() {
        this.reader = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
        this.writer = new PrintWriter(System.out, true);
        this.errorWriter = new PrintWriter(System.err, true);
        this.formatter = new Formatter(writer);
    }

    @Override
    public void printf(String format, Object... args) {
        formatter.format(format, args).flush();
    }

    @Override
    public void flush() {
        writer.flush();
        errorWriter.flush();
    }

    @Override
    public String readLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public String readLine(String prompt) {
        if (!prompt.isEmpty()) {
            writer.append(prompt).flush();
        }
        return readLine();
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
}
