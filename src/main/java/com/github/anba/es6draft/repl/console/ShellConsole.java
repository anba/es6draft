/**
 * Copyright (c) 2012-2016 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import java.util.List;
import java.util.Optional;

import com.github.anba.es6draft.runtime.internal.Console;

/**
 * Interactive shell console interface.
 */
public interface ShellConsole extends Console {
    /**
     * Checks whether or not ANSI mode is supported. (Optional method)
     * 
     * @return {@code true} if ANSI mode is supported
     */
    default boolean isAnsiSupported() {
        return false;
    }

    /**
     * Installs tab-completion support. (Optional method)
     * 
     * @param completer
     *            the shell completer
     */
    default void addCompleter(Completer completer) {
    }

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
        Optional<Completion> complete(String line, int cursor);
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
