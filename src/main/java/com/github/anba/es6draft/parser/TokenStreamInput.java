/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * Interface for character-based access to an input object
 */
public interface TokenStreamInput {
    /**
     * The end-of-file marker
     */
    int EOF = -1;

    /**
     * Returns the current character or {@link TokenStreamInput#EOF} when the end of the input has
     * been reached
     */
    int get();

    /**
     * Ungets the character {@code c}
     */
    int unget(int c);

    /**
     * Returns the character at {@code position() + 1} without changing the actual position
     */
    int peek(int i);

    /**
     * Returns the current position in the input
     */
    int position();

    /**
     * Resets the position to {@code pos}
     */
    void reset(int pos);

    /**
     * Returns the source characters from position {@code from} to position {@code to} (exclusive)
     */
    String range(int from, int to);
}
