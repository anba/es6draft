/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

import org.mozilla.javascript.ConsString;

/**
 * UTF-16 encoding with modified case-folding
 */
final class UTF16Encoding extends UEncoding {
    public static final UTF16Encoding INSTANCE = new UTF16Encoding();

    protected UTF16Encoding() {
        super("UTF-16", 2, 4);
    }

    @Override
    public byte[] toBytes(CharSequence cs) {
        if (cs instanceof ConsString) {
            return ((ConsString) cs).toByteArray(new byte[cs.length() * 2 + 2]);
        }
        return toBytes(cs.toString());
    }

    @Override
    public byte[] toBytes(String s) {
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
    public int strLength(CharSequence cs, int start, int count) {
        return count >> 1;
    }

    @Override
    public int length(CharSequence cs) {
        return cs.length() * 2;
    }

    @Override
    public int length(CharSequence cs, int start, int end) {
        assert 0 <= start && start <= end && end <= cs.length();
        return (end - start) * 2;
    }

    @Override
    public int length(CharSequence cs, int byteIndex) {
        int index = strLength(cs, 0, byteIndex);
        return Character.charCount(Character.codePointAt(cs, index)) * 2;
    }

    @Override
    public int length(byte c) {
        return 2;
    }

    private char codeUnit(byte[] bytes, int p) {
        return (char) (((bytes[p] & 0xff) << 8) | (bytes[p + 1] & 0xff));
    }

    @Override
    public int length(byte[] bytes, int p, int end) {
        if (p + 1 < end) {
            char c = codeUnit(bytes, p);
            if (Character.isHighSurrogate(c) && p + 3 < end
                    && Character.isLowSurrogate(codeUnit(bytes, p + 2))) {
                return 4;
            }
            return 2;
        }
        return missing(1);
    }

    @Override
    public int mbcToCode(byte[] bytes, int p, int end) {
        char c = codeUnit(bytes, p);
        if (Character.isHighSurrogate(c) && p + 3 < end) {
            char d = codeUnit(bytes, p + 2);
            if (Character.isLowSurrogate(d)) {
                return Character.toCodePoint(c, d);
            }
        }
        return c;
    }

    @Override
    public int codeToMbcLength(int code) {
        if (Character.isBmpCodePoint(code)) {
            return 2;
        }
        return 4;
    }

    @Override
    public int codeToMbc(int code, byte[] bytes, int p) {
        if (Character.isBmpCodePoint(code)) {
            bytes[p + 0] = (byte) ((code >>> 8) & 0xff);
            bytes[p + 1] = (byte) ((code >>> 0) & 0xff);
            return 2;
        }
        char high = Character.highSurrogate(code);
        char low = Character.lowSurrogate(code);
        bytes[p + 0] = (byte) ((high >>> 8) & 0xff);
        bytes[p + 1] = (byte) ((high >>> 0) & 0xff);
        bytes[p + 2] = (byte) ((low >>> 8) & 0xff);
        bytes[p + 3] = (byte) ((low >>> 0) & 0xff);
        return 4;
    }

    @Override
    public int leftAdjustCharHead(byte[] bytes, int p, int s, int end) {
        if (s > p) {
            int q = s - ((s - p) & 1);
            if (Character.isLowSurrogate(codeUnit(bytes, q)) && q - 1 > p
                    && Character.isHighSurrogate(codeUnit(bytes, q - 2))) {
                return q - 2;
            }
            return q;
        }
        return s;
    }

    @Override
    public int strLength(byte[] bytes, int p, int end) {
        return super.strLength(bytes, p, end);
    }

    @Override
    public int strCodeAt(byte[] bytes, int p, int end, int index) {
        return super.strCodeAt(bytes, p, end, index);
    }
}
