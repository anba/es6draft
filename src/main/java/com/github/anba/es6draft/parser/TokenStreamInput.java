/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * Class to provide character-based access to an input source string
 */
final class TokenStreamInput {
    /**
     * The end-of-file marker
     */
    public static final int EOF = -1;

    private final String source;
    private final int length;
    private int cursor = 0;

    public TokenStreamInput(String source) {
        this.source = source;
        this.length = source.length();
    }

    /**
     * Returns the input length.
     * 
     * @return the input length
     */
    public int length() {
        return length;
    }

    /**
     * Returns the current code point or {@link TokenStreamInput#EOF} when the end of the input has been reached.
     * 
     * @return the current code point or {@link TokenStreamInput#EOF}
     */
    public int get() {
        if (cursor >= length)
            return EOF;
        int cp = source.codePointAt(cursor);
        cursor += Character.charCount(cp);
        return cp;
    }

    /**
     * Returns the current character or {@link TokenStreamInput#EOF} when the end of the input has been reached.
     * 
     * @return the current character or {@link TokenStreamInput#EOF}
     */
    public int getChar() {
        if (cursor >= length)
            return EOF;
        return source.charAt(cursor++);
    }

    /**
     * Returns the last character.
     * 
     * @return the last character
     */
    public int lastChar() {
        assert cursor > 0 : cursor;
        return source.charAt(cursor - 1);
    }

    /**
     * Ungets the code point {@code c}.
     * 
     * @param c
     *            the code point to read back
     */
    public void unget(int c) {
        assert c != EOF ? source.codePointAt(cursor - Character.charCount(c)) == c : cursor >= length;
        if (c != EOF)
            cursor -= Character.charCount(c);
    }

    /**
     * Ungets the character {@code c}.
     * 
     * @param c
     *            the character to read back
     */
    public void ungetChar(int c) {
        assert c != EOF ? source.charAt(cursor - 1) == c : cursor >= length;
        if (c != EOF)
            cursor -= 1;
    }

    /**
     * Advances the position if the current character is equal to {@code c}.
     * 
     * @param c
     *            the current to test
     * @return {@code true} if the current character matches
     */
    public boolean match(char c) {
        if (cursor >= length || source.charAt(cursor) != c)
            return false;
        cursor += 1;
        return true;
    }

    /**
     * Returns the character at {@code position() + offset} without changing the actual position.
     * 
     * @param offset
     *            the source position offset
     * @return the character at {@code position() + offset} or {@link TokenStreamInput#EOF}
     */
    public int peek(int offset) {
        assert offset >= 0;
        if (cursor + offset >= length)
            return EOF;
        return source.charAt(cursor + offset);
    }

    /**
     * Returns the current position in the input.
     * 
     * @return the current position
     */
    public int position() {
        return cursor;
    }

    /**
     * Resets the position to {@code position}.
     * 
     * @param position
     *            the new position
     */
    public void reset(int position) {
        assert position >= 0 && position <= cursor;
        cursor = position;
    }

    /**
     * Returns the source characters from position {@code from} to position {@code to} (exclusive).
     * 
     * @param from
     *            the start position (inclusive)
     * @param to
     *            the end position (exclusive)
     * @return the source characters in the given range
     */
    public String range(int from, int to) {
        return source.substring(from, to);
    }

    /**
     * Copies the source characters from position {@code from} to position {@code to} (exclusive).
     * 
     * @param from
     *            the start position (inclusive)
     * @param to
     *            the end position (exclusive)
     * @param array
     *            the destination array
     * @param offset
     *            the start offset in the array
     */
    public void chars(int from, int to, char[] array, int offset) {
        source.getChars(from, to, array, offset);
    }
}
