/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import com.github.anba.es6draft.parser.TokenStream;

/**
 * Operations on strings
 * 
 */
public final class Strings {
    private Strings() {
    }

    /**
     * @see TokenStream#isLineTerminator(int)
     */
    public static boolean isLineTerminator(int c) {
        if ((c & ~0b0010_0000_0010_1111) != 0) {
            return false;
        }
        return (c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029);
    }

    /**
     * @see TokenStream#isWhitespace(int)
     */
    public static boolean isWhitespace(int c) {
        return (c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20 || c == 0xA0 || c == 0xFEFF || isSpaceSeparator(c));
    }

    /**
     * @see TokenStream#isSpaceSeparator(int)
     */
    private static boolean isSpaceSeparator(int c) {
        return (c == 0x20 || c == 0xA0 || c == 0x1680 || c == 0x180E
                || (c >= 0x2000 && c <= 0x200A) || c == 0x202F || c == 0x205F || c == 0x3000);
    }

    /**
     * Remove leading whitespace
     */
    public static CharSequence trimLeft(CharSequence s) {
        int length = s.length();
        int start = 0, end = length;
        for (; start < length; ++start) {
            char c = s.charAt(start);
            if (!(isWhitespace(c) || isLineTerminator(c))) {
                break;
            }
        }
        assert start <= end;
        if (start == end) {
            // empty string
            return "";
        }
        return s.subSequence(start, end);
    }

    /**
     * Remove leading and trailing whitespace
     */
    public static CharSequence trim(CharSequence s) {
        int length = s.length();
        int start = 0, end = length;
        for (; start < length; ++start) {
            char c = s.charAt(start);
            if (!(isWhitespace(c) || isLineTerminator(c))) {
                break;
            }
        }
        for (; end > start; --end) {
            char c = s.charAt(end - 1);
            if (!(isWhitespace(c) || isLineTerminator(c))) {
                break;
            }
        }
        assert start <= end;
        if (start == end) {
            // empty string
            return "";
        }
        return s.subSequence(start, end);
    }
}
