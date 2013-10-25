/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * Class to provide character-based access to an input source string
 */
public final class TokenStreamInput {
    /**
     * The end-of-file marker
     */
    public static final int EOF = -1;

    private final String source;
    private final int length;
    private int cursor = 0;

    public TokenStreamInput(CharSequence source) {
        this.source = source.toString();
        this.length = source.length();
    }

    /**
     * Returns the current character or {@link TokenStreamInput#EOF} when the end of the input has
     * been reached
     */
    public int get() {
        if (cursor >= length)
            return EOF;
        return source.charAt(cursor++);
    }

    /**
     * Ungets the character {@code c}
     */
    public int unget(int c) {
        assert c != EOF ? source.charAt(cursor - 1) == c : cursor >= length;
        if (c != EOF)
            cursor -= 1;
        return c;
    }

    /**
     * Advances the position if the current character is equal to {@code c}
     */
    public boolean match(char c) {
        if (cursor >= length || source.charAt(cursor) != c)
            return false;
        cursor += 1;
        return true;
    }

    /**
     * Returns the character at {@code position() + 1} without changing the actual position
     */
    public int peek(int i) {
        assert i >= 0;
        if (cursor + i >= length)
            return EOF;
        return source.charAt(cursor + i);
    }

    /**
     * Returns the current position in the input
     */
    public int position() {
        return cursor;
    }

    /**
     * Resets the position to {@code pos}
     */
    public void reset(int pos) {
        assert pos >= 0 && pos <= cursor;
        cursor = pos;
    }

    /**
     * Returns the source characters from position {@code from} to position {@code to} (exclusive)
     */
    public String range(int from, int to) {
        return source.substring(from, to);
    }

    /**
     * Copies the source characters from position {@code from} to position {@code to} (exclusive)
     */
    public void chars(int from, int to, char[] array, int offset) {
        source.getChars(from, to, array, offset);
    }
}
