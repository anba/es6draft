/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.List;

/**
 * Console interface.
 */
public interface ShellConsole {
    /**
     * Prints the format string.
     * 
     * @param format
     *            the format string
     * @param args
     *            the format string arguments
     */
    void printf(String format, Object... args);

    /**
     * Reads the current line.
     * 
     * @return the current line
     */
    String readLine();

    /**
     * Reads the current line.
     * 
     * @param prompt
     *            the command line prompt
     * @return the current line
     */
    String readLine(String prompt);

    /**
     * Returns the underlying reader.
     * 
     * @return the reader
     */
    Reader reader();

    /**
     * Returns the underlying writer.
     * 
     * @return the writer
     */
    PrintWriter writer();

    /**
     * Returns the underlying error writer.
     * 
     * @return the writer
     */
    PrintWriter errorWriter();

    /**
     * Checks whether or not ANSI mode is supported.
     * 
     * @return {@code true} if ANSI mode is supported
     */
    boolean isAnsiSupported();

    /**
     * Installs tab-completion support.
     * 
     * @param completer
     *            the shell completer
     */
    void addCompleter(Completer completer);

    /**
     * Shell completer interface.
     */
    interface Completer {
        /**
         * Tab-completion.
         * 
         * @param line
         *            the current line
         * @param cursor
         *            the line cursor
         * @return the tab completion result
         */
        // TODO: Use Optional when available
        Completion complete(String line, int cursor);
    }

    /**
     * Shell completion result.
     */
    final class Completion {
        private final String line;
        private final int start, end;
        private final List<String> result;

        /**
         * Constructs a new tab-completion result.
         * 
         * @param line
         *            the input line
         * @param start
         *            the start position
         * @param end
         *            the end position
         * @param result
         *            the completion result
         */
        public Completion(String line, int start, int end, List<String> result) {
            this.line = line;
            this.start = start;
            this.end = end;
            this.result = result;
        }

        /**
         * Returns the line.
         * 
         * @return the input line
         */
        public String line() {
            return line;
        }

        /**
         * Returns the start position.
         * 
         * @return the start position
         */
        public int start() {
            return start;
        }

        /**
         * Returns the end position.
         * 
         * @return the end position
         */
        public int end() {
            return end;
        }

        /**
         * Returns the completion result.
         * 
         * @return the completion result
         */
        public List<String> result() {
            return result;
        }
    }
}
