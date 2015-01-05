/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import static com.github.anba.es6draft.parser.Characters.digit;
import static com.github.anba.es6draft.parser.Characters.hexDigit;

import org.mozilla.javascript.StringToNumber;

/**
 * Utility class for parsing number literals
 */
public final class NumberParser {
    private NumberParser() {
    }

    /**
     * Parse a decimal integer literal.
     * 
     * @param cbuf
     *            the characters to parse
     * @param length
     *            the length of the characters
     * @return the parsed integer
     */
    static double parseInteger(char[] cbuf, int length) {
        if (length < 10) {
            // integer [0, 9999_99999]
            int num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num * 10) + digit(cbuf[i]);
            }
            return num;
        } else if (length < 19) {
            // integer [0, 999_99999_99999_99999]
            long num = 0;
            for (int i = 0; i < length; ++i) {
                num = (num * 10) + digit(cbuf[i]);
            }
            return num;
        } else {
            // integer ]999_99999_99999_99999, ...]
            return parseDecimal(cbuf, length);
        }
    }

    /**
     * Parse a decimal integer literal.
     * 
     * @param s
     *            the string to parse
     * @return the parsed integer
     */
    public static double parseInteger(String s) {
        final char sign = s.charAt(0);
        final int start = (sign == '-' || sign == '+') ? 1 : 0;
        final int length = s.length();
        if (length - start < 10) {
            // integer [0, 9999_99999]
            int num = 0;
            for (int i = start; i < length; ++i) {
                num = (num * 10) + digit(s.charAt(i));
            }
            return (sign == '-') ? -(double) num : num;
        } else if (length - start < 19) {
            // integer [0, 999_99999_99999_99999]
            long num = 0;
            for (int i = start; i < length; ++i) {
                num = (num * 10) + digit(s.charAt(i));
            }
            return (sign == '-') ? -(double) num : num;
        } else {
            // integer ]999_99999_99999_99999, ...]
            return Double.parseDouble(s);
        }
    }

    /**
     * Parse a decimal integer literal.
     * 
     * @param s
     *            the string to parse
     * @param end
     *            the end index
     * @return the parsed integer
     */
    public static double parseInteger(String s, final int end) {
        final char sign = s.charAt(0);
        final int start = (sign == '-' || sign == '+') ? 1 : 0;
        final int length = s.length();
        if (end <= start || end >= length) {
            throw new IndexOutOfBoundsException();
        }
        if (end - start < 10) {
            // integer [0, 9999_99999]
            int num = 0;
            for (int i = start; i < end; ++i) {
                num = (num * 10) + digit(s.charAt(i));
            }
            return (sign == '-') ? -(double) num : num;
        } else if (end - start < 19) {
            // integer [0, 999_99999_99999_99999]
            long num = 0;
            for (int i = start; i < end; ++i) {
                num = (num * 10) + digit(s.charAt(i));
            }
            return (sign == '-') ? -(double) num : num;
        } else {
            // integer ]999_99999_99999_99999, ...]
            return Double.parseDouble(s.substring(0, end));
        }
    }

    /**
     * Parse a decimal number literal.
     * 
     * @param cbuf
     *            the characters to parse
     * @param length
     *            the length of the characters
     * @return the parsed decimal
     */
    static double parseDecimal(char[] cbuf, int length) {
        String string = new String(cbuf, 0, length);
        return Double.parseDouble(string);
    }

    /**
     * Parse a decimal number literal.
     * 
     * @param s
     *            the string to parse
     * @return the parsed decimal
     */
    public static double parseDecimal(String s) {
        return Double.parseDouble(s);
    }

    /**
     * Parse a decimal number literal.
     * 
     * @param s
     *            the string to parse
     * @param end
     *            the end index
     * @return the parsed decimal
     */
    public static double parseDecimal(String s, int end) {
        return Double.parseDouble(s.substring(0, end));
    }

    /**
     * Parse a binary integer literal.
     * 
     * @param cbuf
     *            the characters to parse
     * @param length
     *            the length of the characters
     * @return the binary integer
     */
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

    /**
     * Parse a binary integer literal.
     * 
     * @param s
     *            the string to parse
     * @return the binary integer
     */
    public static double parseBinary(String s) {
        final int start = 2; // "0b" prefix
        final int length = s.length();
        if (length - start < 32) {
            // integer [0, 7FFFFFFF]
            int num = 0;
            for (int i = start; i < length; ++i) {
                num = (num << 1) | digit(s.charAt(i));
            }
            return num;
        } else if (length - start < 64) {
            // integer [0, 7FFFFFFFFFFFFFFF]
            long num = 0;
            for (int i = start; i < length; ++i) {
                num = (num << 1) | digit(s.charAt(i));
            }
            return num;
        } else {
            // integer ]7FFFFFFFFFFFFFFF, ...]
            return StringToNumber.stringToNumber(s, start, 2);
        }
    }

    /**
     * Parse an octal integer literal.
     * 
     * @param cbuf
     *            the characters to parse
     * @param length
     *            the length of the characters
     * @return the octal integer
     */
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

    /**
     * Parse an octal integer literal.
     * 
     * @param s
     *            the string to parse
     * @return the octal integer
     */
    public static double parseOctal(String s) {
        final int start = 2; // "0o" prefix
        final int length = s.length();
        if (length - start <= 10) {
            // integer [0, 07777777777]
            int num = 0;
            for (int i = start; i < length; ++i) {
                num = (num << 3) | digit(s.charAt(i));
            }
            return num;
        } else if (length - start <= 21) {
            // integer [0, 0777777777777777777777]
            long num = 0;
            for (int i = start; i < length; ++i) {
                num = (num << 3) | digit(s.charAt(i));
            }
            return num;
        } else {
            // integer ]0777777777777777777777, ...]
            return StringToNumber.stringToNumber(s, start, 8);
        }
    }

    /**
     * Parse a hexadecimal integer literal.
     * 
     * @param cbuf
     *            the characters to parse
     * @param length
     *            the length of the characters
     * @return the hexadecimal integer
     */
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

    /**
     * Parse a hexadecimal integer literal.
     * 
     * @param s
     *            the characters to parse
     * @return the hexadecimal integer
     */
    public static double parseHex(String s) {
        final int start = 2; // "0x" prefix
        final int length = s.length();
        if (length - start < 8) {
            // integer [0, FFFFFFF]
            int num = 0;
            for (int i = start; i < length; ++i) {
                num = (num << 4) | hexDigit(s.charAt(i));
            }
            return num;
        } else if (length - start < 16) {
            // integer [0, FFFFFFFFFFFFFFF]
            long num = 0;
            for (int i = start; i < length; ++i) {
                num = (num << 4) | hexDigit(s.charAt(i));
            }
            return num;
        } else {
            // integer ]FFFFFFFFFFFFFFF, ...]
            return StringToNumber.stringToNumber(s, start, 16);
        }
    }
}
