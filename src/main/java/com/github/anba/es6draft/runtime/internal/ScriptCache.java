/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;

/**
 * Simple cache for compiled script files.
 */
public final class ScriptCache {
    private static final int DEFAULT_MAX_SIZE = 10;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = .75f;
    private final Map<CacheKey, Script> cache;

    @SuppressWarnings("serial")
    private static final class Cache extends LinkedHashMap<CacheKey, Script> {
        private final int maxSize;

        Cache(int maxSize, int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<CacheKey, Script> eldest) {
            return size() > maxSize;
        }
    }

    private static final class CacheKey {
        private final URI uri;
        private final long size;
        private final long lastModified;

        CacheKey(URI uri, long size, long lastModified) {
            this.uri = uri;
            this.size = size;
            this.lastModified = lastModified;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != CacheKey.class) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return size == other.size && lastModified == other.lastModified && uri.equals(other.uri);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
            result = prime * result + (int) (size ^ (size >>> 32));
            result = prime * result + uri.hashCode();
            return result;
        }
    }

    private CacheKey keyFor(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        return new CacheKey(path.toUri(), attributes.size(), attributes.lastModifiedTime().toMillis());
    }

    private CacheKey keyFor(URL url) throws URISyntaxException {
        return new CacheKey(url.toURI(), 0L, 0L);
    }

    /**
     * Constructs a new {@link ScriptCache} object.
     */
    public ScriptCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new {@link ScriptCache} object.
     * 
     * @param maxSize
     *            the maximum size
     */
    public ScriptCache(int maxSize) {
        this(maxSize, DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new {@link ScriptCache} object.
     * 
     * @param maxSize
     *            the maximum capacity
     * @param initialCapacity
     *            the initial capacity
     * @param loadFactor
     *            the load factor
     */
    public ScriptCache(int maxSize, int initialCapacity, float loadFactor) {
        this.cache = Collections.synchronizedMap(new Cache(maxSize, initialCapacity, loadFactor));
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result.
     * 
     * @param scriptLoader
     *            the script loader
     * @param file
     *            the script file path
     * @return the compiled script
     * @throws IOException
     *             if there was any I/O error
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source could not be compiled
     */
    public Script get(ScriptLoader scriptLoader, Path file) throws IOException, ParserException, CompilationException {
        CacheKey cacheKey = keyFor(file);
        Script cachedScript = cache.get(cacheKey);
        if (cachedScript != null) {
            return cachedScript;
        }
        Source source = new Source(file, Objects.requireNonNull(file.getFileName()).toString(), 1);
        Script script = scriptLoader.script(source, file);
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
    public Script get(ScriptLoader scriptLoader, URL file)
            throws IOException, URISyntaxException, ParserException, CompilationException {
        CacheKey cacheKey = keyFor(file);
        Script cachedScript = cache.get(cacheKey);
        if (cachedScript != null) {
            return cachedScript;
        }
        Source source = new Source(file.getPath(), 1);
        Script script = scriptLoader.script(source, file);
        cache.put(cacheKey, script);
        return script;
    }
}
