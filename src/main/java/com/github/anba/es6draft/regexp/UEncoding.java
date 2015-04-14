/**
 * Copyright (c) 2012-2015 André Bargull
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

import com.github.anba.es6draft.parser.Characters;

/**
 *
 */
abstract class UEncoding extends UnicodeEncoding {
    protected UEncoding(String name, int minLength, int maxLength) {
        super(name, minLength, maxLength, null);
    }

    public abstract byte[] toBytes(CharSequence cs);

    public abstract byte[] toBytes(String s);

    public abstract int length(CharSequence cs);

    public abstract int length(CharSequence cs, int byteIndex);

    public abstract int stringIndex(CharSequence cs, int startIndex, int byteIndex);

    public abstract int byteIndex(CharSequence cs, int startIndex, int stringIndex);

    @Override
    public int mbcCaseFold(int flag, byte[] bytes, IntHolder pp, int end, byte[] to) {
        int p = pp.value;
        pp.value += length(bytes, p, end);
        int codePoint = mbcToCode(bytes, p, end);
        int caseFold = CaseFoldData.caseFold(codePoint);
        return codeToMbc(caseFold >= 0 ? caseFold : codePoint, to, 0);
    }

    @Override
    public void applyAllCaseFold(int flag, ApplyAllCaseFoldFunction fun, Object arg) {
        int[] code = new int[1];
        int[] unfoldFrom = CaseFoldData.caseUnfoldFrom();
        int[][] unfoldTo = CaseFoldData.caseUnfoldTo();
        for (int i = 0; i < unfoldFrom.length; ++i) {
            int from = unfoldFrom[i];
            int[] to = unfoldTo[i];
            for (int j = 0; j < to.length; ++j) {
                int codePoint = to[j];

                code[0] = from;
                fun.apply(codePoint, code, 1, arg);

                code[0] = codePoint;
                fun.apply(from, code, 1, arg);

                for (int k = 0; k < j; k++) {
                    int otherCodePoint = to[k];

                    code[0] = otherCodePoint;
                    fun.apply(codePoint, code, 1, arg);

                    code[0] = codePoint;
                    fun.apply(otherCodePoint, code, 1, arg);
                }
            }
        }
    }

    @Override
    public CaseFoldCodeItem[] caseFoldCodesByString(int flag, byte[] bytes, int p, int end) {
        int codePoint = mbcToCode(bytes, p, end);
        int length = length(bytes, p, end);
        int caseFold = CaseFoldData.caseFold(codePoint);
        if (caseFold >= 0) {
            int n = 1;
            int[] to = CaseFoldData.caseUnfold(caseFold);
            if (to != null) {
                for (int i = 0; i < to.length; ++i) {
                    if (to[i] != codePoint) {
                        n += 1;
                    }
                }
            }
            int k = 0;
            CaseFoldCodeItem[] items = new CaseFoldCodeItem[n];
            items[k++] = new CaseFoldCodeItem(length, 1, new int[] { caseFold });
            if (to != null) {
                for (int i = 0; i < to.length; ++i) {
                    if (to[i] != codePoint) {
                        items[k++] = new CaseFoldCodeItem(length, 1, new int[] { to[i] });
                    }
                }
            }
            return items;
        }
        int[] to = CaseFoldData.caseUnfold(codePoint);
        if (to != null) {
            CaseFoldCodeItem[] items = new CaseFoldCodeItem[to.length];
            for (int i = 0; i < to.length; ++i) {
                items[i] = new CaseFoldCodeItem(length, 1, new int[] { to[i] });
            }
            return items;
        }
        return EMPTY_FOLD_CODES;
    }

    @Override
    public final boolean isNewLine(byte[] bytes, int p, int end) {
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
    public final int propertyNameToCType(byte[] bytes, int p, int end) {
        return super.propertyNameToCType(bytes, p, end);
    }

    @Override
    public final boolean isCodeCType(int code, int ctype) {
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
    public final int[] ctypeCodeRange(int ctype, IntHolder sbOut) {
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
    public final boolean isReverseMatchAllowed(byte[] bytes, int p, int end) {
        return false;
    }
}
