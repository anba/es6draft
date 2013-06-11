/**
 * Copyright (c) 2012-2013 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.runtime.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.Parser.Option;
import com.github.anba.es6draft.parser.ParserException;

/**
 * Simple cache for compiled js-files
 */
public class ScriptCache {
    private static final int MAX_SIZE = 10;

    @SuppressWarnings("serial")
    private Map<Path, Script> cache = Collections.synchronizedMap(new LinkedHashMap<Path, Script>(
            16, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Path, Script> eldest) {
            return (size() > MAX_SIZE);
        }
    });

    private EnumSet<Option> options;
    private AtomicInteger scriptCounter = new AtomicInteger(0);

    private String nextScriptName() {
        return "Script_" + scriptCounter.incrementAndGet();
    }

    public ScriptCache(EnumSet<Parser.Option> options) {
        this.options = options;
    }

    /**
     * Returns a new {@link Reader} for the {@code stream} parameter
     */
    private Reader newReader(InputStream stream) throws IOException {
        return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    /**
     * Parses the javascript source
     */
    private com.github.anba.es6draft.ast.Script parse(String sourceFile, int sourceLine,
            Reader reader) throws ParserException, IOException {
        String source = readFully(reader);
        Parser parser = new Parser(sourceFile, sourceLine, options);
        com.github.anba.es6draft.ast.Script parsedScript = parser.parse(source);
        return parsedScript;
    }

    private static String readFully(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(4096);
        char cbuf[] = new char[4096];
        for (int len; (len = reader.read(cbuf)) != -1;) {
            sb.append(cbuf, 0, len);
        }
        return sb.toString();
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, Path file) throws IOException,
            ParserException, CompilationException {
        return script(sourceName, sourceLine, Files.newInputStream(file));
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, InputStream stream) throws IOException,
            ParserException, CompilationException {
        try (Reader r = newReader(stream)) {
            return ScriptLoader.load(nextScriptName(), parse(sourceName, sourceLine, r));
        }
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, Reader reader) throws IOException,
            ParserException, CompilationException {
        try (Reader r = reader) {
            return ScriptLoader.load(nextScriptName(), parse(sourceName, sourceLine, r));
        }
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result
     */
    public Script get(Path file) throws IOException, ParserException, CompilationException {
        if (cache.containsKey(file)) {
            return cache.get(file);
        }
        String sourceName = file.getFileName().toString();
        int sourceLine = 1;
        Script script = script(sourceName, sourceLine, file);
        cache.put(file, script);
        return script;
    }
}