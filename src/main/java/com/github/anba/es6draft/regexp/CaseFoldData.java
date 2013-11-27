/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

/**
 * Additional case fold support class
 */
final class CaseFoldData {
    /**
     * Is {@code codePoint} a code point which is applicable for case folding
     */
    public static boolean caseFoldType(int codePoint) {
        switch (Character.getType(codePoint)) {
        case Character.UPPERCASE_LETTER:
        case Character.LOWERCASE_LETTER:
        case Character.TITLECASE_LETTER:
        case Character.NON_SPACING_MARK:
        case Character.LETTER_NUMBER:
        case Character.OTHER_SYMBOL:
            return true;
        default:
            return false;
        }
    }

    /**
     * Tests whether or not the case fold is applicable for non-unicode case fold per [21.2.2.8.2]
     */
    public static final boolean isValidCaseFold(int codePoint, int toUpper, int toLower) {
        // 21.2.2.8.2: Canonicalize Abstract Operation
        // Conditions:
        // - simple or common case folding mapping available: toUpper != toLower
        // - exclude case folding from non-ASCII to ASCII: codePoint <= 0x7f || toUpper > 0x7f
        // - restrict case folding to basic plane: toUpper <= 0xffff
        return toUpper != toLower && (codePoint <= 0x7f || toUpper > 0x7f) && toUpper <= 0xffff;
    }

    /**
     * Returns {@code true} if {@code ToUpper(codePoint) == ToUpper(ToLower(codePoint))}
     */
    public static boolean isValidToLower(int codePoint) {
        switch (codePoint) {
        case 0x0130:
        case 0x03f4:
        case 0x1e9e:
        case 0x2126:
        case 0x212a:
        case 0x212b:
            return false;
        default:
            return true;
        }
    }

    /*
     * !Generated method!
     */
    public static final int caseFold1(int codePoint) {
        switch (codePoint) {
        case 0x00b5:
            return 0x03bc;
        case 0x01c4:
        case 0x01c6:
            return 0x01c5;
        case 0x01c7:
        case 0x01c9:
            return 0x01c8;
        case 0x01ca:
        case 0x01cc:
            return 0x01cb;
        case 0x01f1:
        case 0x01f3:
            return 0x01f2;
        case 0x0345:
            return 0x03b9;
        case 0x0392:
        case 0x03b2:
            return 0x03d0;
        case 0x0395:
        case 0x03b5:
            return 0x03f5;
        case 0x0398:
        case 0x03b8:
            return 0x03d1;
        case 0x0399:
        case 0x03b9:
        case 0x1fbe:
            return 0x0345;
        case 0x039a:
        case 0x03ba:
            return 0x03f0;
        case 0x039c:
        case 0x03bc:
            return 0x00b5;
        case 0x03a0:
        case 0x03c0:
            return 0x03d6;
        case 0x03a1:
        case 0x03c1:
            return 0x03f1;
        case 0x03a3:
        case 0x03c3:
            return 0x03c2;
        case 0x03a6:
        case 0x03c6:
            return 0x03d5;
        case 0x03c2:
            return 0x03c3;
        case 0x03d0:
            return 0x03b2;
        case 0x03d1:
            return 0x03b8;
        case 0x03d5:
            return 0x03c6;
        case 0x03d6:
            return 0x03c0;
        case 0x03f0:
            return 0x03ba;
        case 0x03f1:
            return 0x03c1;
        case 0x03f5:
            return 0x03b5;
        case 0x1e60:
        case 0x1e61:
            return 0x1e9b;
        case 0x1e9b:
            return 0x1e61;
        default:
            return -1;
        }
    }

    /*
     * !Generated method!
     */
    public static final int caseFold2(int codePoint) {
        switch (codePoint) {
        case 0x0345:
        case 0x0399:
        case 0x03b9:
            return 0x1fbe;
        case 0x1fbe:
            return 0x03b9;
        default:
            return -1;
        }
    }

    public static final boolean hasAdditionalUnicodeCaseFold(int codePoint) {
        // special case for sharp-s, `/\xdf/ui.test("\u1e9e")` does not work in Java out-of-the-box,
        // also see CaseFoldDataGenerator#findSpecialUnicodeCaseFold()
        return codePoint == 0x00df;
    }

    public static final void appendCaseInsensitiveUnicode(RegExpParser parser, int codePoint) {
        assert hasAdditionalUnicodeCaseFold(codePoint);
        parser.appendCharacter(0x00df);
        parser.appendCharacter(0x1e9e);
    }

    /*
     * !Generated method!
     */
    public static final void appendCaseInsensitiveUnicodeRange(RegExpParser parser, int startChar,
            int endChar) {
        // Type 1
        if (startChar <= 0x00b5
                && 0x00b5 <= endChar
                && !((startChar <= 0x039c && 0x039c <= endChar) || (startChar <= 0x03bc && 0x03bc <= endChar))) {
            parser.appendCharacter(0x039c);
            parser.appendCharacter(0x03bc);
        }
        if (startChar <= 0x0131
                && 0x0131 <= endChar
                && !((startChar <= 0x0049 && 0x0049 <= endChar) || (startChar <= 0x0069 && 0x0069 <= endChar))) {
            parser.appendCharacter(0x0049);
            parser.appendCharacter(0x0069);
        }
        if (startChar <= 0x017f
                && 0x017f <= endChar
                && !((startChar <= 0x0053 && 0x0053 <= endChar) || (startChar <= 0x0073 && 0x0073 <= endChar))) {
            parser.appendCharacter(0x0053);
            parser.appendCharacter(0x0073);
        }
        if (startChar <= 0x01c5
                && 0x01c5 <= endChar
                && !((startChar <= 0x01c4 && 0x01c4 <= endChar) || (startChar <= 0x01c6 && 0x01c6 <= endChar))) {
            parser.appendCharacter(0x01c4);
            parser.appendCharacter(0x01c6);
        }
        if (startChar <= 0x01c8
                && 0x01c8 <= endChar
                && !((startChar <= 0x01c7 && 0x01c7 <= endChar) || (startChar <= 0x01c9 && 0x01c9 <= endChar))) {
            parser.appendCharacter(0x01c7);
            parser.appendCharacter(0x01c9);
        }
        if (startChar <= 0x01cb
                && 0x01cb <= endChar
                && !((startChar <= 0x01ca && 0x01ca <= endChar) || (startChar <= 0x01cc && 0x01cc <= endChar))) {
            parser.appendCharacter(0x01ca);
            parser.appendCharacter(0x01cc);
        }
        if (startChar <= 0x01f2
                && 0x01f2 <= endChar
                && !((startChar <= 0x01f1 && 0x01f1 <= endChar) || (startChar <= 0x01f3 && 0x01f3 <= endChar))) {
            parser.appendCharacter(0x01f1);
            parser.appendCharacter(0x01f3);
        }
        if (startChar <= 0x0345
                && 0x0345 <= endChar
                && !((startChar <= 0x0399 && 0x0399 <= endChar) || (startChar <= 0x03b9 && 0x03b9 <= endChar))) {
            parser.appendCharacter(0x0399);
            parser.appendCharacter(0x03b9);
        }
        if (startChar <= 0x03c2
                && 0x03c2 <= endChar
                && !((startChar <= 0x03a3 && 0x03a3 <= endChar) || (startChar <= 0x03c3 && 0x03c3 <= endChar))) {
            parser.appendCharacter(0x03a3);
            parser.appendCharacter(0x03c3);
        }
        if (startChar <= 0x03d0
                && 0x03d0 <= endChar
                && !((startChar <= 0x0392 && 0x0392 <= endChar) || (startChar <= 0x03b2 && 0x03b2 <= endChar))) {
            parser.appendCharacter(0x0392);
            parser.appendCharacter(0x03b2);
        }
        if (startChar <= 0x03d1
                && 0x03d1 <= endChar
                && !((startChar <= 0x0398 && 0x0398 <= endChar) || (startChar <= 0x03b8 && 0x03b8 <= endChar))) {
            parser.appendCharacter(0x0398);
            parser.appendCharacter(0x03b8);
        }
        if (startChar <= 0x03d5
                && 0x03d5 <= endChar
                && !((startChar <= 0x03a6 && 0x03a6 <= endChar) || (startChar <= 0x03c6 && 0x03c6 <= endChar))) {
            parser.appendCharacter(0x03a6);
            parser.appendCharacter(0x03c6);
        }
        if (startChar <= 0x03d6
                && 0x03d6 <= endChar
                && !((startChar <= 0x03a0 && 0x03a0 <= endChar) || (startChar <= 0x03c0 && 0x03c0 <= endChar))) {
            parser.appendCharacter(0x03a0);
            parser.appendCharacter(0x03c0);
        }
        if (startChar <= 0x03f0
                && 0x03f0 <= endChar
                && !((startChar <= 0x039a && 0x039a <= endChar) || (startChar <= 0x03ba && 0x03ba <= endChar))) {
            parser.appendCharacter(0x039a);
            parser.appendCharacter(0x03ba);
        }
        if (startChar <= 0x03f1
                && 0x03f1 <= endChar
                && !((startChar <= 0x03a1 && 0x03a1 <= endChar) || (startChar <= 0x03c1 && 0x03c1 <= endChar))) {
            parser.appendCharacter(0x03a1);
            parser.appendCharacter(0x03c1);
        }
        if (startChar <= 0x03f5
                && 0x03f5 <= endChar
                && !((startChar <= 0x0395 && 0x0395 <= endChar) || (startChar <= 0x03b5 && 0x03b5 <= endChar))) {
            parser.appendCharacter(0x0395);
            parser.appendCharacter(0x03b5);
        }
        if (startChar <= 0x1e9b
                && 0x1e9b <= endChar
                && !((startChar <= 0x1e60 && 0x1e60 <= endChar) || (startChar <= 0x1e61 && 0x1e61 <= endChar))) {
            parser.appendCharacter(0x1e60);
            parser.appendCharacter(0x1e61);
        }
        if (startChar <= 0x1fbe
                && 0x1fbe <= endChar
                && !((startChar <= 0x0399 && 0x0399 <= endChar) || (startChar <= 0x03b9 && 0x03b9 <= endChar))) {
            parser.appendCharacter(0x0399);
            parser.appendCharacter(0x03b9);
        }

        // Type 2
        if (startChar <= 0x0130
                && 0x0130 <= endChar
                && !((startChar <= 0x0049 && 0x0049 <= endChar) || (startChar <= 0x0069 && 0x0069 <= endChar))) {
            parser.appendCharacter(0x0049);
            parser.appendCharacter(0x0069);
        }
        if (startChar <= 0x0049
                && 0x0049 <= endChar
                && !((startChar <= 0x0130 && 0x0130 <= endChar) || (startChar <= 0x0069 && 0x0069 <= endChar))) {
            parser.appendCharacter(0x0130);
        }
        if (startChar <= 0x03f4
                && 0x03f4 <= endChar
                && !((startChar <= 0x0398 && 0x0398 <= endChar) || (startChar <= 0x03b8 && 0x03b8 <= endChar))) {
            parser.appendCharacter(0x0398);
            parser.appendCharacter(0x03b8);
        }
        if (startChar <= 0x0398
                && 0x0398 <= endChar
                && !((startChar <= 0x03f4 && 0x03f4 <= endChar) || (startChar <= 0x03b8 && 0x03b8 <= endChar))) {
            parser.appendCharacter(0x03f4);
        }
        if (startChar <= 0x2126
                && 0x2126 <= endChar
                && !((startChar <= 0x03a9 && 0x03a9 <= endChar) || (startChar <= 0x03c9 && 0x03c9 <= endChar))) {
            parser.appendCharacter(0x03a9);
            parser.appendCharacter(0x03c9);
        }
        if (startChar <= 0x03a9
                && 0x03a9 <= endChar
                && !((startChar <= 0x2126 && 0x2126 <= endChar) || (startChar <= 0x03c9 && 0x03c9 <= endChar))) {
            parser.appendCharacter(0x2126);
        }
        if (startChar <= 0x212a
                && 0x212a <= endChar
                && !((startChar <= 0x004b && 0x004b <= endChar) || (startChar <= 0x006b && 0x006b <= endChar))) {
            parser.appendCharacter(0x004b);
            parser.appendCharacter(0x006b);
        }
        if (startChar <= 0x004b
                && 0x004b <= endChar
                && !((startChar <= 0x212a && 0x212a <= endChar) || (startChar <= 0x006b && 0x006b <= endChar))) {
            parser.appendCharacter(0x212a);
        }
        if (startChar <= 0x212b
                && 0x212b <= endChar
                && !((startChar <= 0x00c5 && 0x00c5 <= endChar) || (startChar <= 0x00e5 && 0x00e5 <= endChar))) {
            parser.appendCharacter(0x00c5);
            parser.appendCharacter(0x00e5);
        }
        if (startChar <= 0x00c5
                && 0x00c5 <= endChar
                && !((startChar <= 0x212b && 0x212b <= endChar) || (startChar <= 0x00e5 && 0x00e5 <= endChar))) {
            parser.appendCharacter(0x212b);
        }

        // Type 3
        if (startChar <= 0x1e9e && 0x1e9e <= endChar && !(startChar <= 0x00df && 0x00df <= endChar)) {
            parser.appendCharacter(0x00df);
        }
        if (startChar <= 0x00df && 0x00df <= endChar && !(startChar <= 0x1e9e && 0x1e9e <= endChar)) {
            parser.appendCharacter(0x1e9e);
        }
    }
}
