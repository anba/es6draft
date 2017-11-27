/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.PrintWriter;
import java.io.Reader;

/**
 * Console interface.
 */
public interface Console {
    /**
     * Prints the format string to the console output writer.
     * 
     * @param format
     *            the format string
     * @param args
     *            the format string arguments
     */
    void printf(String format, Object... args);

    /**
     * Flushes the console output writer.
     */
    void flush();

    /**
     * Reads a single line from the console input reader.
     * 
     * @return the line string or {@code null} if no input available
     */
    String readLine();

    /**
     * Reads a single line from the console input reader.
     * 
     * @param prompt
     *            the optional command line prompt
     * @return the line string or {@code null} if no input available
     */
    String readLine(String prompt);

    /**
     * Returns the reader ({@code stdin}) bound to this console object.
     * 
     * @return the console reader
     */
    Reader reader();

    /**
     * Returns the writer ({@code stdout}) bound to this console object.
     * 
     * @return the console writer
     */
    PrintWriter writer();

    /**
     * Returns the error writer ({@code stderr}) bound to this console object.
     * 
     * @return the console error writer
     */
    PrintWriter errorWriter();
}
