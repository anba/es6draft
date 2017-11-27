/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.scripting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;

import javax.script.ScriptContext;

import com.github.anba.es6draft.runtime.internal.Console;

/**
 *
 */
final class ScriptingConsole implements Console {
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final PrintWriter errorWriter;

    ScriptingConsole(ScriptContext context) {
        this.reader = new BufferedReader(context.getReader());
        this.writer = printWriter(context.getWriter());
        this.errorWriter = printWriter(context.getErrorWriter());
    }

    private static PrintWriter printWriter(Writer writer) {
        if (writer instanceof PrintWriter) {
            return (PrintWriter) writer;
        }
        return new PrintWriter(writer, true);
    }

    @Override
    public void printf(String format, Object... args) {
        writer.format(format, args).flush();
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
            throw new UncheckedIOException(e);
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
