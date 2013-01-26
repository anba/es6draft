/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public final class SourceCompressor {
    private static final int BUFFER_SIZE = 256;
    private static final String ENCODING = "ISO-8859-1";

    private SourceCompressor() {
    }

    private static class CompressedSource implements Callable<String> {
        final String source;

        CompressedSource(String source) {
            this.source = source;
        }

        @Override
        public String call() throws Exception {
            ByteArrayOutputStream bout = new ByteArrayOutputStream(BUFFER_SIZE);
            GZIPOutputStream out = new GZIPOutputStream(bout, BUFFER_SIZE);
            out.write(source.getBytes(ENCODING));
            out.close();

            return bout.toString(ENCODING);
        }
    }

    private static class DecompressedSource implements Callable<String> {
        final String source;

        DecompressedSource(String source) {
            this.source = source;
        }

        @Override
        public String call() throws Exception {
            byte[] compressed = source.getBytes(ENCODING);
            ByteArrayInputStream bin = new ByteArrayInputStream(compressed);
            GZIPInputStream in = new GZIPInputStream(bin, BUFFER_SIZE);

            ByteArrayOutputStream bout = new ByteArrayOutputStream(BUFFER_SIZE);
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = in.read(buf)) != -1) {
                bout.write(buf, 0, n);
            }

            return new String(bout.toByteArray(), ENCODING);
        }
    }

    public static Callable<String> compress(String source) {
        return new CompressedSource(source);
    }

    public static Callable<String> decompress(String source) {
        return new DecompressedSource(source);
    }
}
