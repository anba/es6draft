/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import static com.github.anba.es6draft.runtime.internal.Errors.newInternalError;

import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.types.builtins.StringObject;

/**
 * {@link StringBuilder}-like class with additional overflow checking.
 */
public final class StrBuilder implements Appendable, CharSequence {
    private final ExecutionContext cx;
    private final StringBuilder sb;

    /**
     * Constructs a new {@link StrBuilder}.
     * 
     * @param cx
     *            the execution context
     * @see StringBuilder#StringBuilder()
     */
    public StrBuilder(ExecutionContext cx) {
        this.cx = cx;
        this.sb = new StringBuilder();
    }

    /**
     * Constructs a new {@link StrBuilder}.
     * 
     * @param cx
     *            the execution context
     * @param capacity
     *            the initial capacity
     * @see StringBuilder#StringBuilder(int)
     */
    public StrBuilder(ExecutionContext cx, int capacity) {
        this.cx = cx;
        this.sb = new StringBuilder(capacity);
    }

    /**
     * Constructs a new {@link StrBuilder}.
     * 
     * @param cx
     *            the execution context
     * @param csq
     *            the first char sequence
     * @see StringBuilder#StringBuilder(CharSequence)
     */
    public StrBuilder(ExecutionContext cx, CharSequence csq) {
        this.cx = cx;
        this.sb = new StringBuilder(csq);
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public int length() {
        return sb.length();
    }

    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    private void checkOOM(int length) {
        if (length > StringObject.MAX_LENGTH - sb.length()) {
            throw newInternalError(cx, Messages.Key.InvalidStringSize);
        }
    }

    /**
     * Ensures the underlying {@link StringBuilder} has the requested minimum capacity.
     * 
     * @param minimumCapacity
     *            the minimum capacity
     * @see StringBuilder#ensureCapacity(int)
     */
    public void ensureCapacity(int minimumCapacity) {
        sb.ensureCapacity(minimumCapacity);
    }

    /**
     * Appends the boolean to this builder.
     * 
     * @param b
     *            the boolean to append
     * @return this string builder
     * @see StringBuilder#append(boolean)
     */
    public StrBuilder append(boolean b) {
        checkOOM(5);
        sb.append(b);
        return this;
    }

    /**
     * Appends the string to this builder.
     * 
     * @param s
     *            the string to append
     * @return this string builder
     * @see StringBuilder#append(String)
     */
    public StrBuilder append(String s) {
        checkOOM(s.length());
        sb.append(s);
        return this;
    }

    @Override
    public StrBuilder append(CharSequence s) {
        checkOOM(s.length());
        sb.append(s);
        return this;
    }

    @Override
    public StrBuilder append(CharSequence csq, int start, int end) {
        checkOOM(Math.min(end - start, 0));
        sb.append(csq, start, end);
        return this;
    }

    @Override
    public StrBuilder append(char c) {
        checkOOM(1);
        sb.append(c);
        return this;
    }

    public StrBuilder appendCodePoint(int codePoint) {
        checkOOM(Character.charCount(codePoint));
        sb.appendCodePoint(codePoint);
        return this;
    }

    public StrBuilder append(byte i) {
        checkOOM(3 + 1); // worst case is 3 + 1 characters.
        sb.append(i);
        return this;
    }

    public StrBuilder append(short i) {
        checkOOM(5 + 1); // worst case is 5 + 1 characters.
        sb.append(i);
        return this;
    }

    public StrBuilder append(int i) {
        checkOOM(10 + 1); // worst case is 10 + 1 characters.
        sb.append(i);
        return this;
    }

    public StrBuilder append(long i) {
        checkOOM(19 + 1); // worst case is 19 + 1 characters.
        sb.append(i);
        return this;
    }
}
