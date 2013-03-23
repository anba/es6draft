/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.util;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;

/**
 * Simple cache for the compiled js-files
 */
@SuppressWarnings("serial")
public class ScriptCache {
    private static final int MAX_SIZE = 10;

    private LinkedHashMap<Path, Script> cache = new LinkedHashMap<Path, Script>(16, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, Script> eldest) {
            return (size() > MAX_SIZE);
        }
    };

    private final Charset charset;
    private AtomicInteger scriptCounter = new AtomicInteger(0);

    public ScriptCache(Charset charset) {
        this.charset = charset;
    }

    private String nextScriptName() {
        return "Script_" + scriptCounter.incrementAndGet();
    }

    /**
     * Returns a new {@link Reader} for the {@code file} parameter
     */
    private Reader newReader(Path file) throws IOException {
        if (charset.equals(StandardCharsets.UTF_8)) {
            InputStream stream = Files.newInputStream(file);
            BOMInputStream bomstream = new BOMInputStream(stream, ByteOrderMark.UTF_8,
                    ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE);
            String charsetName = defaultIfNull(bomstream.getBOMCharsetName(), charset.name());
            return new InputStreamReader(bomstream, charsetName);
        }
        return Files.newBufferedReader(file, charset);
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, Path file) throws IOException {
        String className = nextScriptName();
        try (Reader reader = newReader(file)) {
            return ScriptLoader.load(sourceName, className, IOUtils.toString(reader));
        }
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result
     */
    public Script get(Path file) throws IOException {
        if (cache.containsKey(file)) {
            return cache.get(file);
        }
        String sourceName = file.getFileName().toString();
        Script script = script(sourceName, file);
        cache.put(file, script);
        return script;
    }
}