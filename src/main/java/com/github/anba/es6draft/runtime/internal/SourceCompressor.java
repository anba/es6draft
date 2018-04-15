/**
 * Copyright (c) André Bargull
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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 */
public final class SourceCompressor {
    private static final int BUFFER_SIZE = 256;

    private SourceCompressor() {
    }

    private static final class ByteOutputStream extends ByteArrayOutputStream {
        ByteOutputStream(int size) {
            super(size);
        }

        // TODO: Provided by default starting with Java 10.
        // @Override
        @SuppressWarnings("all")
        public String toString(Charset cs) {
            return new String(buf, 0, count, cs);
        }
    }

    /**
     * Returns the compressed source string.
     * 
     * @param source
     *            the source string
     * @return the compressed source
     * @throws IOException
     *             if there was any I/O error
     */
    public static String compress(String source) throws IOException {
        ByteOutputStream bout = new ByteOutputStream(BUFFER_SIZE);
        GZIPOutputStream out = new GZIPOutputStream(bout, BUFFER_SIZE);
        out.write(source.getBytes(StandardCharsets.UTF_8));
        out.close();

        return bout.toString(StandardCharsets.ISO_8859_1);
    }

    /**
     * Returns the decompressed source string.
     * 
     * @param source
     *            the compressed source string
     * @return the decompressed source
     * @throws IOException
     *             if there was any I/O error
     */
    public static String decompress(String source) throws IOException {
        byte[] compressed = source.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayInputStream bin = new ByteArrayInputStream(compressed);
        GZIPInputStream in = new GZIPInputStream(bin, BUFFER_SIZE);

        ByteOutputStream bout = new ByteOutputStream(BUFFER_SIZE);
        byte[] buf = new byte[BUFFER_SIZE];
        for (int n; (n = in.read(buf)) != -1;) {
            bout.write(buf, 0, n);
        }
        bout.close();

        return bout.toString(StandardCharsets.UTF_8);
    }
}
