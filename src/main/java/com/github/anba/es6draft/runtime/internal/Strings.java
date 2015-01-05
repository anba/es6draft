/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.parser.Characters.isWhitespaceOrLineTerminator;

import java.util.Arrays;

/**
 * Operations on strings
 */
public final class Strings {
    private Strings() {
    }

    /**
     * Removes leading whitespace.
     * 
     * @param s
     *            the string
     * @return the string with leading whitespace removed
     */
    public static String trimLeft(String s) {
        int start = 0, end = s.length();
        for (; start < end; ++start) {
            char c = s.charAt(start);
            if (!isWhitespaceOrLineTerminator(c)) {
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
     * Removes leading and trailing whitespace.
     * 
     * @param s
     *            the string
     * @return the string with leading and trailing whitespace removed
     */
    public static String trim(String s) {
        int start = 0, end = s.length();
        for (; start < end; ++start) {
            char c = s.charAt(start);
            if (!isWhitespaceOrLineTerminator(c)) {
                break;
            }
        }
        for (; end > start; --end) {
            char c = s.charAt(end - 1);
            if (!isWhitespaceOrLineTerminator(c)) {
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
     * Concatenates the input strings, adjacent strings are separated by the given separator char.
     * 
     * @param separator
     *            the separator character
     * @param strings
     *            the input strings
     * @return the concatenated string
     */
    public static String concatWith(char separator, String... strings) {
        if (strings.length == 0) {
            return "";
        } else if (strings.length == 1) {
            return strings[0];
        } else {
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string).append(separator);
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

    /**
     * Concatenates two strings.
     * 
     * @param s1
     *            the first string
     * @param s2
     *            the second string
     * @return the concatenated string
     */
    public static String concat(String s1, String s2) {
        char[] ca = new char[s1.length() + s2.length()];
        s1.getChars(0, s1.length(), ca, 0);
        s2.getChars(0, s2.length(), ca, s1.length());
        return new String(ca);
    }

    /**
     * Repeat {@code c} {@code n}-times.
     * 
     * @param c
     *            the character to repeat
     * @param n
     *            the repetition count
     * @return the result string
     */
    public static String repeat(char c, int n) {
        assert n >= 0;
        char[] value = new char[n];
        Arrays.fill(value, c);
        return new String(value);
    }

    private static final char[] HEXDIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Quotes the input string.
     * 
     * @param s
     *            the string
     * @return the quoted string
     */
    public static String quote(String s) {
        for (int i = 0, length = s.length(); i < length; ++i) {
            char c = s.charAt(i);
            switch (c) {
            case '"':
            case '\\':
            case '\b':
            case '\f':
            case '\n':
            case '\r':
            case '\t':
                break;
            default:
                if (c < ' ' || c > 0xff) {
                    break;
                }
                continue;
            }
            return quote(s, i);
        }
        return new StringBuilder(s.length() + 2).append('"').append(s).append('"').toString();
    }

    private static String quote(String s, int start) {
        StringBuilder sb = new StringBuilder(s.length() + 5);
        sb.append('"').append(s.substring(0, start));
        for (int i = start, length = s.length(); i < length; ++i) {
            char c = s.charAt(i);
            switch (c) {
            case '"':
            case '\\':
                sb.append('\\').append(c);
                break;
            case '\b':
                sb.append('\\').append('b');
                break;
            case '\f':
                sb.append('\\').append('f');
                break;
            case '\n':
                sb.append('\\').append('n');
                break;
            case '\r':
                sb.append('\\').append('r');
                break;
            case '\t':
                sb.append('\\').append('t');
                break;
            default:
                if (c < ' ' || c > 0xff) {
                    sb.append('\\').append('u')//
                            .append(HEXDIGITS[(c >> 12) & 0xf])//
                            .append(HEXDIGITS[(c >> 8) & 0xf])//
                            .append(HEXDIGITS[(c >> 4) & 0xf])//
                            .append(HEXDIGITS[(c >> 0) & 0xf]);
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * If {@code s} is an argument index less than {@code 2}<span><sup>{@code 31}</sup></span>
     * {@code -1} ({@code 0x7FFFFFF}), its integer value is returned. Otherwise {@code -1} is
     * returned.
     * 
     * @param s
     *            the property key
     * @return the integer index or {@code -1}
     */
    public static int toArgumentIndex(String s) {
        // "2147483647".length == 10
        return (int) toIndex(s, 10, 0x7FFF_FFFFL);
    }

    /**
     * If {@code s} is a string index less than {@code 2}<span><sup>{@code 31}</sup></span>
     * {@code -1} ({@code 0x7FFFFFF}), its integer value is returned. Otherwise {@code -1} is
     * returned.
     * 
     * @param s
     *            the property key
     * @return the integer index or {@code -1}
     */
    public static int toStringIndex(String s) {
        // "2147483647".length == 10
        return (int) toIndex(s, 10, 0x7FFF_FFFFL);
    }

    /**
     * If {@code s} is an integer indexed property key less than {@code 2}<span><sup>{@code 32}
     * </sup></span>{@code -1} ({@code 0xFFFFFFF}), its integer value is returned. Otherwise
     * {@code -1} is returned.
     * 
     * @param s
     *            the property key
     * @return the array index or {@code -1}
     */
    public static long toArrayIndex(String s) {
        // "4294967295".length == 10
        return toIndex(s, 10, 0xFFFF_FFFFL);
    }

    /**
     * If {@code s} is an integer indexed property key less than {@code 2}<span><sup>{@code 53}
     * </sup></span>{@code -1}, its integer value is returned. Otherwise {@code -1} is returned.
     * 
     * @param propertyKey
     *            the property key
     * @return the integer index or {@code -1}
     */
    static long toIndex(String propertyKey) {
        // "9007199254740991".length == 16
        return toIndex(propertyKey, 16, 0x1F_FFFF_FFFF_FFFFL);
    }

    private static long toIndex(String s, int maxLength, long limit) {
        int length = s.length();
        if (length < 1 || length > maxLength) {
            // empty string or definitely greater than maximum value
            return -1;
        }
        if (s.charAt(0) == '0') {
            return length == 1 ? 0 : -1;
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
