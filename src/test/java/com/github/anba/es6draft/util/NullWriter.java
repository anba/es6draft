/**
 * Copyright (c) André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import java.io.Writer;

/**
 *
 */
public final class NullWriter extends Writer {
    @Override
    public Writer append(char c) {
        return this;
    }

    @Override
    public Writer append(CharSequence csq) {
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) {
        return this;
    }

    @Override
    public void write(int c) {
    }

    @Override
    public void write(char[] cbuf) {
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
    }

    @Override
    public void write(String str) {
    }

    @Override
    public void write(String str, int off, int len) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }
}
