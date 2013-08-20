/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

/**
 * {@link TokenStreamInput} to traverse strings
 */
public class StringTokenStreamInput implements TokenStreamInput {
    private final String source;
    private final int length;
    private int cursor = 0;

    public StringTokenStreamInput(CharSequence source) {
        this.source = source.toString();
        this.length = source.length();
    }

    @Override
    public int get() {
        if (cursor >= length)
            return EOF;
        return source.charAt(cursor++);
    }

    @Override
    public int unget(int c) {
        assert c != EOF ? source.charAt(cursor - 1) == c : cursor >= length;
        if (c != EOF)
            cursor -= 1;
        return c;
    }

    @Override
    public boolean match(char c) {
        if (cursor >= length || source.charAt(cursor) != c)
            return false;
        cursor += 1;
        return true;
    }

    @Override
    public int peek(int i) {
        assert i >= 0;
        if (cursor + i >= length)
            return EOF;
        return source.charAt(cursor + i);
    }

    @Override
    public int position() {
        return cursor;
    }

    @Override
    public void reset(int pos) {
        assert pos >= 0 && pos <= cursor;
        cursor = pos;
    }

    @Override
    public String range(int from, int to) {
        return source.substring(from, to);
    }
}
