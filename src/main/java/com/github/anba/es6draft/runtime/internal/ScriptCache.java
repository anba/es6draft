/**
 * Copyright (c) 2012-2014 André Bargull
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.ScriptLoader;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.compiler.Compiler;
import com.github.anba.es6draft.parser.Parser;
import com.github.anba.es6draft.parser.ParserException;

/**
 * Simple cache for compiled js-files
 */
public final class ScriptCache {
    private static final int MAX_SIZE = 10;

    @SuppressWarnings("serial")
    private Map<URI, Script> cache = Collections.synchronizedMap(new LinkedHashMap<URI, Script>(16,
            .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<URI, Script> eldest) {
            return (size() > MAX_SIZE);
        }
    });

    private final EnumSet<CompatibilityOption> options;
    private final EnumSet<Parser.Option> parserOptions;
    private final EnumSet<Compiler.Option> compilerOptions;
    private final AtomicInteger scriptCounter = new AtomicInteger(0);

    private String nextScriptName() {
        return "Script_" + scriptCounter.incrementAndGet();
    }

    public ScriptCache(Set<CompatibilityOption> options) {
        this.options = EnumSet.copyOf(options);
        this.parserOptions = EnumSet.noneOf(Parser.Option.class);
        this.compilerOptions = EnumSet.noneOf(Compiler.Option.class);
    }

    public ScriptCache(Set<CompatibilityOption> options, Set<Parser.Option> parserOptions) {
        this.options = EnumSet.copyOf(options);
        this.parserOptions = EnumSet.copyOf(parserOptions);
        this.compilerOptions = EnumSet.noneOf(Compiler.Option.class);
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
        Parser parser = new Parser(sourceFile, sourceLine, options, parserOptions);
        com.github.anba.es6draft.ast.Script parsedScript = parser.parseScript(source);
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
        assert file.isAbsolute() : String.format("'%s' is not an absolute path", file);
        return script(sourceName, sourceLine, Files.newInputStream(file));
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, URL file) throws IOException,
            ParserException, CompilationException {
        return script(sourceName, sourceLine, file.openStream());
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, InputStream stream) throws IOException,
            ParserException, CompilationException {
        try (Reader r = newReader(stream)) {
            com.github.anba.es6draft.ast.Script parsedScript = parse(sourceName, sourceLine, r);
            return ScriptLoader.load(parsedScript, nextScriptName(), compilerOptions);
        }
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, Reader reader) throws IOException,
            ParserException, CompilationException {
        try (Reader r = reader) {
            com.github.anba.es6draft.ast.Script parsedScript = parse(sourceName, sourceLine, r);
            return ScriptLoader.load(parsedScript, nextScriptName(), compilerOptions);
        }
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result
     */
    public Script get(Path file) throws IOException, ParserException, CompilationException {
        URI cacheKey = file.toUri();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        String sourceName = file.getFileName().toString();
        int sourceLine = 1;
        Script script = script(sourceName, sourceLine, file);
        cache.put(cacheKey, script);
        return script;
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result
     */
    public Script get(URL file) throws IOException, URISyntaxException, ParserException,
            CompilationException {
        URI cacheKey = file.toURI();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        String sourceName = file.getFile();
        int sourceLine = 1;
        Script script = script(sourceName, sourceLine, file);
        cache.put(cacheKey, script);
        return script;
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, Path file, ExecutorService executor)
            throws IOException, ParserException, CompilationException {
        assert file.isAbsolute() : String.format("'%s' is not an absolute path", file);
        return script(sourceName, sourceLine, Files.newInputStream(file), executor);
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, URL file, ExecutorService executor)
            throws IOException, ParserException, CompilationException {
        return script(sourceName, sourceLine, file.openStream(), executor);
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, InputStream stream,
            ExecutorService executor) throws IOException, ParserException, CompilationException {
        try (Reader r = newReader(stream)) {
            com.github.anba.es6draft.ast.Script parsedScript = parse(sourceName, sourceLine, r);
            return ScriptLoader.load(parsedScript, nextScriptName(), executor, compilerOptions);
        }
    }

    /**
     * Parses and compiles the javascript file
     */
    public Script script(String sourceName, int sourceLine, Reader reader, ExecutorService executor)
            throws IOException, ParserException, CompilationException {
        try (Reader r = reader) {
            com.github.anba.es6draft.ast.Script parsedScript = parse(sourceName, sourceLine, r);
            return ScriptLoader.load(parsedScript, nextScriptName(), executor, compilerOptions);
        }
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result
     */
    public Script get(Path file, ExecutorService executor) throws IOException, ParserException,
            CompilationException {
        URI cacheKey = file.toUri();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        String sourceName = file.getFileName().toString();
        int sourceLine = 1;
        Script script = script(sourceName, sourceLine, file, executor);
        cache.put(cacheKey, script);
        return script;
    }

    /**
     * Compiles {@code file} to a {@link Script} and caches the result
     */
    public Script get(URL file, ExecutorService executor) throws IOException, URISyntaxException,
            ParserException, CompilationException {
        URI cacheKey = file.toURI();
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        String sourceName = file.getFile();
        int sourceLine = 1;
        Script script = script(sourceName, sourceLine, file, executor);
        cache.put(cacheKey, script);
        return script;
    }
}
