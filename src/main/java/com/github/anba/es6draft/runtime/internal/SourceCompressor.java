/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public final class SourceCompressor {
    private static final int BUFFER_SIZE = 256;

    private SourceCompressor() {
    }

    private static class ByteOutputStream extends ByteArrayOutputStream {
        public ByteOutputStream(int size) {
            super(size);
        }

        public String toString(Charset cs) {
            return new String(buf, 0, count, cs);
        }
    }

    private static class CompressedSource implements Callable<String> {
        final String source;

        CompressedSource(String source) {
            this.source = source;
        }

        @Override
        public String call() throws IOException {
            ByteOutputStream bout = new ByteOutputStream(BUFFER_SIZE);
            GZIPOutputStream out = new GZIPOutputStream(bout, BUFFER_SIZE);
            out.write(source.getBytes(StandardCharsets.UTF_8));
            out.close();

            return bout.toString(StandardCharsets.ISO_8859_1);
        }
    }

    private static class DecompressedSource implements Callable<String> {
        final String source;

        DecompressedSource(String source) {
            this.source = source;
        }

        @Override
        public String call() throws IOException {
            byte[] compressed = source.getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayInputStream bin = new ByteArrayInputStream(compressed);
            GZIPInputStream in = new GZIPInputStream(bin, BUFFER_SIZE);

            ByteOutputStream bout = new ByteOutputStream(BUFFER_SIZE);
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = in.read(buf)) != -1) {
                bout.write(buf, 0, n);
            }
            bout.close();

            return bout.toString(StandardCharsets.UTF_8);
        }
    }

    public static Callable<String> compress(String source) {
        return new CompressedSource(source);
    }

    public static Callable<String> decompress(String source) {
        return new DecompressedSource(source);
    }
}
