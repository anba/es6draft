/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import java.util.Arrays;

/**
 * Simple string buffer class for internal use.
 */
final class StrBuffer {
    private char[] array = new char[512];
    private int length = 0;

    /**
     * Returns the underlying character array.
     * 
     * @return the character array
     */
    public char[] array() {
        return array;
    }

    /**
     * Returns the current buffer length.
     * 
     * @return the buffer length
     */
    public int length() {
        return length;
    }

    /**
     * Clears the buffer content.
     */
    public void clear() {
        length = 0;
    }

    /**
     * Appends the character to the buffer.
     * 
     * @param c
     *            the character
     */
    public void append(int c) {
        int len = length;
        if (len == array.length) {
            array = Arrays.copyOf(array, length << 1);
        }
        array[len] = (char) c;
        length = len + 1;
    }

    /**
     * Appends the code point to the buffer.
     * 
     * @param c
     *            the code point
     */
    public void appendCodePoint(int c) {
        if (Character.isBmpCodePoint(c)) {
            append(c);
        } else {
            append(Character.highSurrogate(c));
            append(Character.lowSurrogate(c));
        }
    }

    /**
     * Appends the character range to the buffer.
     * 
     * @param in
     *            the token stream input
     * @param from
     *            the start index
     * @param to
     *            the end index
     */
    public void append(TokenStreamInput in, int from, int to) {
        assert from <= to;
        int range = to - from;
        if (range > 0) {
            int offset = length;
            int newLength = offset + range;
            if (newLength > array.length) {
                array = Arrays.copyOf(array, Integer.highestOneBit(newLength) << 1);
            }
            in.chars(from, to, array, offset);
            length = newLength;
        }
    }

    @Override
    public String toString() {
        return new String(array, 0, length);
    }
}
