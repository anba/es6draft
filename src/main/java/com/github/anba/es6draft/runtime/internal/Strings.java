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
    public static String trimLeft(String s) {
        int start = 0, end = s.length();
        for (; start < end; ++start) {
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
        return s.substring(start, end);
    }

    /**
     * Remove leading and trailing whitespace
     */
    public static String trim(String s) {
        int start = 0, end = s.length();
        for (; start < end; ++start) {
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
        return s.substring(start, end);
    }

    /**
     * If {@code s} is an integer indexed property key less than {@code 0x7FFFFFF}, its integer
     * value is returned. Otherwise {@code -1} is returned.
     */
    public static int toIndex(String s) {
        return (int) toIndex(s, 0x7FFF_FFFFL);
    }

    /**
     * If {@code s} is an integer indexed property key less than {@code 0xFFFFFFF}, its integer
     * value is returned. Otherwise {@code -1} is returned.
     */
    public static long toArrayIndex(String s) {
        return toIndex(s, 0xFFFF_FFFFL);
    }

    private static long toIndex(String s, long limit) {
        int length = s.length();
        if (length < 1 || length > 10) {
            // empty string or definitely greater than "2147483647" or "4294967295"
            // "2147483647".length == 10
            // "4294967295".length == 10
            return -1;
        }
        if (s.charAt(0) == '0') {
            return (length == 1 ? 0 : -1);
        }
        long index = 0L;
        for (int i = 0; i < length; ++i) {
            char c = s.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                return -1;
            }
            index = index * 10 + (c - '0');
        }
        return index < limit ? index : -1;
    }
}
