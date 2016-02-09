/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.parser;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UCharacterCategory;

/**
 * 
 */
public final class Characters {
    private Characters() {
    }

    /**
     * <strong>[11.2] White Space</strong>
     * 
     * <pre>
     * WhiteSpace ::
     *     {@literal <TAB>}  (U+0009)
     *     {@literal <VT>}   (U+000B)
     *     {@literal <FF>}   (U+000C)
     *     {@literal <SP>}   (U+0020)
     *     {@literal <NBSP>} (U+00A0)
     *     {@literal <ZWNBSP>}  (U+FEFF)
     *     {@literal <USP>}  ("Zs")
     * </pre>
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is a whitespace
     */
    public static boolean isWhitespace(int c) {
        if (c <= 127) {
            return c == 0x09 || c == 0x0B || c == 0x0C || c == 0x20;
        }
        return c == 0xA0 || c == 0xFEFF || isSpaceSeparator(c);
    }

    /**
     * Unicode category "Zs" (space separator)
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is space separator
     */
    public static boolean isSpaceSeparator(int c) {
        // Unicode 8.0
        return c == 0x20 || c == 0xA0 || c == 0x1680 || (0x2000 <= c && c <= 0x200A) || c == 0x202F || c == 0x205F
                || c == 0x3000;
    }

    /**
     * <strong>[11.3] Line Terminators</strong>
     * 
     * <pre>
     * LineTerminator ::
     *     {@literal <LF>} (U+000A)
     *     {@literal <CR>} (U+000D)
     *     {@literal <LS>} (U+2028)
     *     {@literal <PS>} (U+2029)
     * </pre>
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is a line terminator
     */
    public static boolean isLineTerminator(int c) {
        if ((c & ~0b0010_0000_0010_1111) != 0) {
            return false;
        }
        return c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029;
    }

    /**
     * <strong>[11.2] White Space</strong><br>
     * <strong>[11.3] Line Terminators</strong>
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is whitespace or a line terminator
     */
    public static boolean isWhitespaceOrLineTerminator(int c) {
        if (c <= 127) {
            return (0x09 <= c && c <= 0x0D) || c == 0x20;
        }
        return isWhitespace(c) || isLineTerminator(c);
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * IdentifierStart ::
     *     UnicodeIDStart
     *     $
     *     _
     *     \ UnicodeEscapeSequence
     * UnicodeIDStart ::
     *     any Unicode character with the Unicode property "ID_Start".
     * </pre>
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is an identifier start character
     */
    public static boolean isIdentifierStart(int c) {
        if (c <= 127) {
            return ('a' <= (c | 0x20) && (c | 0x20) <= 'z') || c == '$' || c == '_';
        }
        return isIdentifierStartUnlikely(c);
    }

    // Cf. definition of "ID_Start" from http://www.unicode.org/reports/tr31/.
    private static final int ID_START_MASK = 1 << UCharacterCategory.UPPERCASE_LETTER
            | 1 << UCharacterCategory.LOWERCASE_LETTER | 1 << UCharacterCategory.TITLECASE_LETTER
            | 1 << UCharacterCategory.MODIFIER_LETTER | 1 << UCharacterCategory.OTHER_LETTER
            | 1 << UCharacterCategory.LETTER_NUMBER;

    private static boolean isIdentifierStartUnlikely(int c) {
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
        if ((1 << UCharacter.getType(c) & ID_START_MASK) != 0) {
            return true;
        }
        // Grandfathered characters (Other_ID_Start) [Unicode 8.0].
        switch (c) {
        case '\u2118':
        case '\u212E':
        case '\u309B':
        case '\u309C':
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * IdentifierPart ::
     *     UnicodeIDContinue
     *     $
     *     _
     *     \ UnicodeEscapeSequence
     *     &lt;ZWNJ&gt;
     *     &lt;ZWJ&gt;
     * UnicodeIDContinue ::
     *     any Unicode character with the Unicode property "ID_Continue"
     * </pre>
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is an identifier part character
     */
    public static boolean isIdentifierPart(int c) {
        if (c <= 127) {
            return ('a' <= (c | 0x20) && (c | 0x20) <= 'z') || ('0' <= c && c <= '9') || c == '$' || c == '_';
        }
        return isIdentifierPartUnlikely(c);
    }

    // Cf. definition of "ID_Continue" from http://www.unicode.org/reports/tr31/.
    private static final int ID_CONTINUE_MASK = 1 << UCharacterCategory.UPPERCASE_LETTER
            | 1 << UCharacterCategory.LOWERCASE_LETTER | 1 << UCharacterCategory.TITLECASE_LETTER
            | 1 << UCharacterCategory.MODIFIER_LETTER | 1 << UCharacterCategory.OTHER_LETTER
            | 1 << UCharacterCategory.LETTER_NUMBER | 1 << UCharacterCategory.NON_SPACING_MARK
            | 1 << UCharacterCategory.COMBINING_SPACING_MARK | 1 << UCharacterCategory.DECIMAL_DIGIT_NUMBER
            | 1 << UCharacterCategory.CONNECTOR_PUNCTUATION;

    private static boolean isIdentifierPartUnlikely(int c) {
        if (c == '\u200C' || c == '\u200D')
            return true;
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
        if ((1 << UCharacter.getType(c) & ID_CONTINUE_MASK) != 0) {
            return true;
        }
        // Grandfathered characters (Other_ID_Start + Other_ID_Continue) [Unicode 8.0].
        switch (c) {
        case '\u00B7':
        case '\u0387':
        case '\u1369':
        case '\u136A':
        case '\u136B':
        case '\u136C':
        case '\u136D':
        case '\u136E':
        case '\u136F':
        case '\u1370':
        case '\u1371':
        case '\u19DA':
        case '\u2118':
        case '\u212E':
        case '\u309B':
        case '\u309C':
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * UnicodeIDStart ::
     *     any Unicode character with the Unicode property "ID_Start".
     * </pre>
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is an identifier start character
     */
    public static boolean isUnicodeIDStart(int c) {
        if (c <= 127) {
            return ('a' <= (c | 0x20) && (c | 0x20) <= 'z');
        }
        return isUnicodeIDStartUnlikely(c);
    }

    private static boolean isUnicodeIDStartUnlikely(int c) {
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
        if ((1 << UCharacter.getType(c) & ID_START_MASK) != 0) {
            return true;
        }
        // Grandfathered characters (Other_ID_Start) [Unicode 8.0].
        switch (c) {
        case '\u2118':
        case '\u212E':
        case '\u309B':
        case '\u309C':
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.6] Names and Keywords</strong>
     * 
     * <pre>
     * UnicodeIDContinue ::
     *     any Unicode character with the Unicode property "ID_Continue"
     * </pre>
     * 
     * @param c
     *            the character
     * @return {@code true} if the character is an identifier part character
     */
    public static boolean isUnicodeIDContinue(int c) {
        if (c <= 127) {
            return ('a' <= (c | 0x20) && (c | 0x20) <= 'z') || ('0' <= c && c <= '9') || c == '_';
        }
        return isUnicodeIDContinueUnlikely(c);
    }

    private static boolean isUnicodeIDContinueUnlikely(int c) {
        if (c == '\u2E2F') {
            // VERTICAL TILDE is in 'Lm' and [:Pattern_Syntax:]
            return false;
        }
        if ((1 << UCharacter.getType(c) & ID_CONTINUE_MASK) != 0) {
            return true;
        }
        // Grandfathered characters (Other_ID_Start + Other_ID_Continue) [Unicode 8.0].
        switch (c) {
        case '\u00B7':
        case '\u0387':
        case '\u1369':
        case '\u136A':
        case '\u136B':
        case '\u136C':
        case '\u136D':
        case '\u136E':
        case '\u136F':
        case '\u1370':
        case '\u1371':
        case '\u19DA':
        case '\u2118':
        case '\u212E':
        case '\u309B':
        case '\u309C':
            return true;
        default:
            return false;
        }
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * BinaryDigit :: one of
     *     0  1
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is a binary digit
     */
    public static boolean isBinaryDigit(int c) {
        return (c == '0' || c == '1');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * OctalDigit :: one of
     *     0  1  2  3  4  5  6  7
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is an octal digit
     */
    public static boolean isOctalDigit(int c) {
        return ('0' <= c && c <= '7');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * DecimalDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is decimal digit
     */
    public static boolean isDecimalDigit(int c) {
        return ('0' <= c && c <= '9');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
     * </pre>
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is a hexadecimal digit
     */
    public static boolean isHexDigit(int c) {
        return ('0' <= c && c <= '9') || ('A' <= c && c <= 'F') || ('a' <= c && c <= 'f');
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * DecimalDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9
     * </pre>
     * 
     * @param c
     *            the character to convert
     * @return the converted integer or {@code -1} if not a valid decimal-digit
     */
    public static int digit(int c) {
        if ('0' <= c && c <= '9') {
            return (c - '0');
        }
        return -1;
    }

    /**
     * <strong>[11.8.3] Numeric Literals</strong>
     * 
     * <pre>
     * HexDigit :: one of
     *     0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
     * </pre>
     * 
     * @param c
     *            the character to convert
     * @return the converted integer or {@code -1} if not a valid hex-digit
     */
    public static int hexDigit(int c) {
        if ('0' <= c && c <= '9') {
            return (c - '0');
        } else if ('A' <= c && c <= 'F') {
            return (c - ('A' - 10));
        } else if ('a' <= c && c <= 'f') {
            return (c - ('a' - 10));
        }
        return -1;
    }

    /**
     * Tests for ASCII alphabetical letter, i.e. a character in the ranges {@code A-Z} and {@code a-z} .
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is an ASCII alphabetical letter
     */
    public static boolean isASCIIAlpha(int c) {
        return 'a' <= (c | 0x20) && (c | 0x20) <= 'z';
    }

    /**
     * Tests for ASCII alphanumeric letter including underscore, i.e. a character in the ranges {@code 0-9}, {@code A-Z}
     * and {@code a-z} plus the single letter {@code _}.
     * 
     * @param c
     *            the character to test
     * @return {@code true} if the character is an ASCII alphanumeric letter or underscore
     */
    public static boolean isASCIIAlphaNumericUnderscore(int c) {
        return ('0' <= c && c <= '9') || ('a' <= (c | 0x20) && (c | 0x20) <= 'z') || c == '_';
    }
}
