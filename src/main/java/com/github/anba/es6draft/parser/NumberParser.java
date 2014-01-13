/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import org.mozilla.javascript.StringToNumber;

/**
 * Utility class for parsing number literals
 */
final class NumberParser {
    private NumberParser() {
    }

    static double parseDecimal(char[] cbuf, int length) {
        String string = new String(cbuf, 0, length);
        return Double.parseDouble(string);
    }

    static double parseBinary(char[] cbuf, int length) {
        if (length < 32) {
            // integer [0, 7FFFFFFF]
            int num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num << 1) | digit(cbuf[i]);
            }
            return num;
        } else if (length < 64) {
            // integer [0, 7FFFFFFFFFFFFFFF]
            long num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num << 1) | digit(cbuf[i]);
            }
            return num;
        } else {
            // integer ]7FFFFFFFFFFFFFFF, ...]
            String string = new String(cbuf, 0, length);
            return StringToNumber.stringToNumber(string, 0, 2);
        }
    }

    static double parseOctal(char[] cbuf, int length) {
        if (length <= 10) {
            // integer [0, 07777777777]
            int num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num << 3) | digit(cbuf[i]);
            }
            return num;
        } else if (length <= 21) {
            // integer [0, 0777777777777777777777]
            long num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num << 3) | digit(cbuf[i]);
            }
            return num;
        } else {
            // integer ]0777777777777777777777, ...]
            String string = new String(cbuf, 0, length);
            return StringToNumber.stringToNumber(string, 0, 8);
        }
    }

    static double parseHex(char[] cbuf, int length) {
        if (length < 8) {
            // integer [0, FFFFFFF]
            int num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num << 4) | hexDigit(cbuf[i]);
            }
            return num;
        } else if (length < 16) {
            // integer [0, FFFFFFFFFFFFFFF]
            long num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num << 4) | hexDigit(cbuf[i]);
            }
            return num;
        } else {
            // integer ]FFFFFFFFFFFFFFF, ...]
            String string = new String(cbuf, 0, length);
            return StringToNumber.stringToNumber(string, 0, 16);
        }
    }

    private static int digit(int c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        }
        return -1;
    }

    private static int hexDigit(int c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        } else if (c >= 'A' && c <= 'F') {
            return (c - ('A' - 10));
        } else if (c >= 'a' && c <= 'f') {
            return (c - ('a' - 10));
        }
        return -1;
    }

}
