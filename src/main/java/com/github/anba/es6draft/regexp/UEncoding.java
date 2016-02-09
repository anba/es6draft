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

    /**
     * Encodes the charsequence into a byte array using this encoding.
     * 
     * @param cs
     *            the charsequence
     * @return the encoded byte array
     */
    public abstract byte[] toBytes(CharSequence cs);

    /**
     * Encodes the string into a byte array using this encoding.
     * 
     * @param s
     *            the string
     * @return the encoded byte array
     */
    public abstract byte[] toBytes(String s);

    /**
     * Returns the string length when reading {@code count} bytes from the substring
     * {@code cs.subSequence(start, cs.length)}.
     * 
     * @param cs
     *            the charsequence
     * @param start
     *            the string start index
     * @param count
     *            the number of bytes to read
     * @return the string length
     */
    public abstract int strLength(CharSequence cs, int start, int count);

    /**
     * Returns the number of bytes required to represent the charsequence in this encoding.
     * 
     * @param cs
     *            the charsequence
     * @return the byte length
     */
    public abstract int length(CharSequence cs);

    /**
     * Returns {@code length(cs.subSequence(start, end))}.
     * 
     * @param cs
     *            the charsequence
     * @param start
     *            the string start index
     * @param end
     *            the string end index
     * @return the byte length
     */
    public abstract int length(CharSequence cs, int start, int end);

    /**
     * Returns the length in bytes of the character at the byte index {@code byteIndex}.
     * 
     * @param cs
     *            the charsequence
     * @param byteIndex
     *            the byte index
     * @return the character byte length
     */
    public abstract int length(CharSequence cs, int byteIndex);

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
        int[] unfoldFrom = CaseFoldData.caseUnfoldFrom();
        int[][] unfoldTo = CaseFoldData.caseUnfoldTo();
        applyAllCaseFold(fun, arg, unfoldFrom, unfoldTo);
    }

    protected final void applyAllCaseFold(ApplyAllCaseFoldFunction f, Object arg, int[] unfoldFrom, int[][] unfoldTo) {
        int[] code = new int[1];
        for (int i = 0; i < unfoldFrom.length; ++i) {
            int from = unfoldFrom[i];
            int[] to = unfoldTo[i];
            for (int j = 0; j < to.length; ++j) {
                int codePoint = to[j];

                code[0] = from;
                f.apply(codePoint, code, 1, arg);

                code[0] = codePoint;
                f.apply(from, code, 1, arg);

                for (int k = 0; k < j; k++) {
                    int otherCodePoint = to[k];

                    code[0] = otherCodePoint;
                    f.apply(codePoint, code, 1, arg);

                    code[0] = codePoint;
                    f.apply(otherCodePoint, code, 1, arg);
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
            int[] to = CaseFoldData.caseUnfold(caseFold);
            if (to != null) {
                return caseFoldCodesByString(codePoint, length, caseFold, to);
            }
            return new CaseFoldCodeItem[] { new CaseFoldCodeItem(length, 1, new int[] { caseFold }) };
        }
        int[] to = CaseFoldData.caseUnfold(codePoint);
        if (to != null) {
            return caseFoldCodesByString(codePoint, length, to);
        }
        return EMPTY_FOLD_CODES;
    }

    protected final CaseFoldCodeItem[] caseFoldCodesByString(int codePoint, int length, int caseFold, int[] to) {
        int n = 1;
        for (int i = 0; i < to.length; ++i) {
            if (to[i] != codePoint) {
                n += 1;
            }
        }
        int k = 0;
        CaseFoldCodeItem[] items = new CaseFoldCodeItem[n];
        items[k++] = new CaseFoldCodeItem(length, 1, new int[] { caseFold });
        for (int i = 0; i < to.length; ++i) {
            if (to[i] != codePoint) {
                items[k++] = new CaseFoldCodeItem(length, 1, new int[] { to[i] });
            }
        }
        return items;
    }

    protected final CaseFoldCodeItem[] caseFoldCodesByString(int codePoint, int length, int[] to) {
        CaseFoldCodeItem[] items = new CaseFoldCodeItem[to.length];
        for (int i = 0; i < to.length; ++i) {
            items[i] = new CaseFoldCodeItem(length, 1, new int[] { to[i] });
        }
        return items;
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
            return 'A' <= code && code <= 'Z';
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
        codeRangeSpace = new int[] { 10, 0x0009, 0x000d, 0x0020, 0x0020, 0x00a0, 0x00a0, 0x1680, 0x1680, 0x2000, 0x200a,
                0x2028, 0x2029, 0x202f, 0x202f, 0x205f, 0x205f, 0x3000, 0x3000, 0xfeff, 0xfeff };
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
