/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;

/**
 * Simple cache for compiled js-files
 */
public final class ScriptCache {
    private static final int MAX_SIZE = 10;
    private Map<CacheKey, Script> cache = Collections.synchronizedMap(new Cache());

    @SuppressWarnings("serial")
    private static final class Cache extends LinkedHashMap<CacheKey, Script> {
        Cache() {
            super(16, .75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Script> eldest) {
            return size() > MAX_SIZE;
        }
    }

    private static final class CacheKey {
        private final URI uri;

        CacheKey(URI uri) {
            this.uri = uri;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj.getClass() != CacheKey.class) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return uri.equals(other.uri);
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }
    }

    private CacheKey keyFor(URI uri) {
        return new CacheKey(uri);
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result.
     * 
     * @param scriptLoader
     *            the script loader
     * @param file
     *            the script file path
     * @param executor
     *            the executor for parallel compilation
     * @return the compiled script
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script get(ScriptLoader scriptLoader, Path file) throws IOException, ParserException,
            CompilationException {
        CacheKey cacheKey = keyFor(file.toUri());
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        String sourceName = file.getFileName().toString();
        int sourceLine = 1;
        Script script = scriptLoader.script(sourceName, sourceLine, file);
        cache.put(cacheKey, script);
        return script;
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result.
     * 
     * @param scriptLoader
     *            the script loader
     * @param file
     *            the script file URL
     * @return the compiled script
     * @throws IOException
     *             if there was any I/O error
     * @throws URISyntaxException
     *             if the URL is not a valid URI
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script get(ScriptLoader scriptLoader, URL file) throws IOException, URISyntaxException,
            ParserException, CompilationException {
        CacheKey cacheKey = keyFor(file.toURI());
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        String sourceName = file.getFile();
        int sourceLine = 1;
        Script script = scriptLoader.script(sourceName, sourceLine, file);
        cache.put(cacheKey, script);
        return script;
    }
}
