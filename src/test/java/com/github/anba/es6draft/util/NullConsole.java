/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import com.github.anba.es6draft.runtime.internal.Console;

/** 
 *
 */
public final class NullConsole implements Console {
    private final StringReader reader = new StringReader("");
    private final PrintWriter writer = new PrintWriter(new NullWriter());

    @Override
    public void printf(String format, Object... args) {
    }

    @Override
    public void flush() {
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
        return writer;
    }
}
