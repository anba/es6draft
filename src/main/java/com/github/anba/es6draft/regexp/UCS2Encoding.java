/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import org.jcodings.ApplyAllCaseFoldFunction;
import org.jcodings.CaseFoldCodeItem;
import org.jcodings.IntHolder;
import org.jcodings.constants.CharacterType;
import org.jcodings.unicode.UnicodeEncoding;
import org.mozilla.javascript.ConsString;

import com.github.anba.es6draft.parser.Characters;

/**
 * UCS-2 encoding with modified case-folding
 */
final class UCS2Encoding extends UnicodeEncoding {
    public static final UCS2Encoding INSTANCE = new UCS2Encoding();

    protected UCS2Encoding() {
        super("UCS-2", 2, 2, null);
    }

    public static byte[] toBytes(CharSequence cs) {
        if (cs instanceof ConsString) {
            return ((ConsString) cs).toByteArray(new byte[cs.length() * 2 + 2]);
        }
        return toBytes(cs.toString());
    }

    public static byte[] toBytes(String s) {
        char[] chars = s.toCharArray();
        byte[] bytes = new byte[chars.length * 2 + 2]; // null-terminated c-string
        for (int i = 0, j = 0, len = chars.length; i < len; ++i) {
            char c = chars[i];
            bytes[j++] = (byte) ((c >>> 8) & 0xff);
            bytes[j++] = (byte) ((c >>> 0) & 0xff);
        }
        return bytes;
    }

    @Override
    public int length(byte c) {
        return 2;
    }

    @Override
    public int length(byte[] bytes, int p, int end) {
        return p + 1 < end ? 2 : missing(1);
    }

    @Override
    public boolean isNewLine(byte[] bytes, int p, int end) {
        int codePoint = mbcToCode(bytes, p, end);
        switch (codePoint) {
        case 0x000A:
        case 0x000D:
        case 0x2028:
        case 0x2029:
            return true;
        default:
            return false;
        }
    }

    @Override
    public int mbcToCode(byte[] bytes, int p, int end) {
        return ((bytes[p + 0] & 0xff) << 8) | (bytes[p + 1] & 0xff);
    }

    @Override
    public int codeToMbcLength(int code) {
        return 2;
    }

    @Override
    public int codeToMbc(int code, byte[] bytes, int p) {
        bytes[p + 0] = (byte) ((code >>> 8) & 0xff);
        bytes[p + 1] = (byte) ((code >>> 0) & 0xff);
        return 2;
    }

    @Override
    public int mbcCaseFold(int flag, byte[] bytes, IntHolder pp, int end, byte[] to) {
        int p = pp.value;
        pp.value += 2;
        int codePoint = mbcToCode(bytes, p, end);
        int caseFold = codePoint;
        if (isAscii(codePoint)) {
            caseFold = asciiToUpper(codePoint);
        } else {
            int toUpper = Character.toUpperCase(codePoint);
            int toLower = Character.toLowerCase(codePoint);
            if (CaseFoldData.isValidCaseFold(codePoint, toUpper, toLower)) {
                caseFold = toUpper;
            }
        }
        return codeToMbc(caseFold, to, 0);
    }

    private static int[] allCaseFoldData;

    @Override
    public void applyAllCaseFold(int flag, ApplyAllCaseFoldFunction fun, Object arg) {
        int[] caseFoldData = allCaseFoldData;
        if (caseFoldData == null) {
            allCaseFoldData = caseFoldData = CaseFoldData.allCaseFoldData();
        }

        int[] to = { 0 };
        for (int i = 0, len = caseFoldData.length; i < len; ++i) {
            int entry = caseFoldData[i];
            int codePoint = (entry >>> 16) & 0xffff;
            int foldCodePoint = (entry >>> 0) & 0xffff;
            to[0] = foldCodePoint;
            fun.apply(codePoint, to, 1, arg);
        }
    }

    @Override
    public CaseFoldCodeItem[] caseFoldCodesByString(int flag, byte[] bytes, int p, int end) {
        int codePoint = mbcToCode(bytes, p, end);
        if (isAscii(codePoint)) {
            if (Characters.isASCIIAlpha(codePoint)) {
                return new CaseFoldCodeItem[] { new CaseFoldCodeItem(2, 1,
                        new int[] { codePoint ^ 0x20 }) };
            }
            return EMPTY_FOLD_CODES;
        }

        int toUpper = Character.toUpperCase(codePoint);
        int toLower = Character.toLowerCase(codePoint);
        if (CaseFoldData.isValidCaseFold(codePoint, toUpper, toLower)) {
            int caseFold1 = CaseFoldData.caseFold1(codePoint);
            int caseFold2 = CaseFoldData.caseFold2(codePoint);

            int n = 0;
            n += (codePoint != toUpper ? 1 : 0);
            n += (codePoint != toLower && CaseFoldData.isValidToLower(codePoint) ? 1 : 0);
            n += (caseFold1 != -1 ? 1 : 0);
            n += (caseFold2 != -1 ? 1 : 0);
            int k = 0;
            CaseFoldCodeItem[] items = new CaseFoldCodeItem[n];
            if (codePoint != toUpper) {
                items[k++] = new CaseFoldCodeItem(2, 1, new int[] { toUpper });
            }
            if (codePoint != toLower && CaseFoldData.isValidToLower(codePoint)) {
                items[k++] = new CaseFoldCodeItem(2, 1, new int[] { toLower });
            }
            if (caseFold1 != -1) {
                items[k++] = new CaseFoldCodeItem(2, 1, new int[] { caseFold1 });
            }
            if (caseFold2 != -1) {
                items[k++] = new CaseFoldCodeItem(2, 1, new int[] { caseFold2 });
            }
            return items;
        }
        return EMPTY_FOLD_CODES;
    }

    @Override
    public int propertyNameToCType(byte[] bytes, int p, int end) {
        return super.propertyNameToCType(bytes, p, end);
    }

    @Override
    public boolean isCodeCType(int code, int ctype) {
        switch (ctype) {
        case CharacterType.DIGIT:
            return Characters.isDecimalDigit(code);
        case CharacterType.SPACE:
            return Characters.isWhitespaceOrLineTerminator(code);
        case CharacterType.UPPER:
            // needs to be implemented to parse hexadecimal digits
            return Character.isUpperCase(code);
        case CharacterType.XDIGIT:
            // needs to be implemented to parse hexadecimal digits
            return Characters.isHexDigit(code);
        case CharacterType.WORD:
            return Characters.isASCIIAlphaNumericUnderscore(code);
        default:
            assert false : "unreachable: " + ctype;
            return super.isCodeCType(code, ctype);
        }
    }

    private static final int[] codeRangeDigit, codeRangeWord, codeRangeSpace;
    static {
        codeRangeDigit = new int[] { 1, '0', '9' };
        codeRangeWord = new int[] { 4, '0', '9', 'A', 'Z', '_', '_', 'a', 'z' };
        codeRangeSpace = new int[] { 11, 0x0009, 0x000d, 0x0020, 0x0020, 0x00a0, 0x00a0, 0x1680,
                0x1680, 0x180e, 0x180e, 0x2000, 0x200a, 0x2028, 0x2029, 0x202f, 0x202f, 0x205f,
                0x205f, 0x3000, 0x3000, 0xfeff, 0xfeff };
    }

    @Override
    public int[] ctypeCodeRange(int ctype, IntHolder sbOut) {
        sbOut.value = 0x00; // ?
        switch (ctype) {
        case CharacterType.DIGIT:
            return codeRangeDigit;
        case CharacterType.WORD:
            return codeRangeWord;
        case CharacterType.SPACE:
            return codeRangeSpace;
        default:
            assert false : "unreachable";
            return super.ctypeCodeRange(ctype);
        }
    }

    @Override
    public int leftAdjustCharHead(byte[] bytes, int p, int s, int end) {
        if (s > p && ((s - p) & 1) == 1)
            return s - 1;
        return s;
    }

    @Override
    public boolean isReverseMatchAllowed(byte[] bytes, int p, int end) {
        return false;
    }

    @Override
    public int strLength(byte[] bytes, int p, int end) {
        return p <= end ? (end - p) >>> 1 : 0;
    }

    @Override
    public int strCodeAt(byte[] bytes, int p, int end, int index) {
        int q = p + (index << 1);
        return q + 1 < end ? mbcToCode(bytes, q, end) : -1;
    }
}
