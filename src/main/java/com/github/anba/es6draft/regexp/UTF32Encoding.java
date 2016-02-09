/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.regexp;

/**
 * UTF-32 encoding with modified case-folding
 */
final class UTF32Encoding extends UEncoding {
    public static final UTF32Encoding INSTANCE = new UTF32Encoding();

    protected UTF32Encoding() {
        super("UTF-32", 4, 4);
    }

    @Override
    public byte[] toBytes(CharSequence cs) {
        return toBytes(cs.toString());
    }

    @Override
    public byte[] toBytes(String s) {
        int length = 0;
        for (int cp, i = 0, len = s.length(); i < len; i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            length += 1;
        }
        byte[] bytes = new byte[length * 4 + 4]; // null-terminated c-string
        for (int cp, i = 0, j = 0, len = s.length(); i < len; i += Character.charCount(cp)) {
            cp = s.codePointAt(i);
            bytes[j++] = (byte) ((cp >>> 24) & 0xff);
            bytes[j++] = (byte) ((cp >>> 16) & 0xff);
            bytes[j++] = (byte) ((cp >>> 8) & 0xff);
            bytes[j++] = (byte) ((cp >>> 0) & 0xff);
        }
        return bytes;
    }

    @Override
    public int strLength(CharSequence cs, int start, int count) {
        int index = start;
        for (int j = 0; j < count; j += 4) {
            index += Character.charCount(Character.codePointAt(cs, index));
        }
        return index - start;
    }

    @Override
    public int length(CharSequence cs) {
        return Character.codePointCount(cs, 0, cs.length()) * 4;
    }

    @Override
    public int length(CharSequence cs, int start, int end) {
        assert 0 <= start && start <= end && end <= cs.length();
        return Character.codePointCount(cs, start, end) * 4;
    }

    @Override
    public int length(CharSequence cs, int byteIndex) {
        return 4;
    }

    @Override
    public int length(byte c) {
        return 4;
    }

    @Override
    public int length(byte[] bytes, int p, int end) {
        return p + 3 < end ? 4 : missing(p + 2 < end ? 1 : p + 1 < end ? 2 : 3);
    }

    @Override
    public int mbcToCode(byte[] bytes, int p, int end) {
        return ((bytes[p + 0] & 0xff) << 24) | ((bytes[p + 1] & 0xff) << 16) | ((bytes[p + 2] & 0xff) << 8)
                | (bytes[p + 3] & 0xff);
    }

    @Override
    public int codeToMbcLength(int code) {
        return 4;
    }

    @Override
    public int codeToMbc(int code, byte[] bytes, int p) {
        bytes[p + 0] = (byte) ((code >>> 24) & 0xff);
        bytes[p + 1] = (byte) ((code >>> 16) & 0xff);
        bytes[p + 2] = (byte) ((code >>> 8) & 0xff);
        bytes[p + 3] = (byte) ((code >>> 0) & 0xff);
        return 4;
    }

    @Override
    public int leftAdjustCharHead(byte[] bytes, int p, int s, int end) {
        return s > p ? s - ((s - p) & 3) : s;
    }

    @Override
    public int strLength(byte[] bytes, int p, int end) {
        return p <= end ? (end - p) >>> 2 : 0;
    }

    @Override
    public int strCodeAt(byte[] bytes, int p, int end, int index) {
        int q = p + (index << 2);
        return q + 3 < end ? mbcToCode(bytes, q, end) : -1;
    }
}
