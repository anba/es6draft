/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.github.anba.es6draft.Script;
import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.runtime.ExecutionContext;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.Errors;
import com.github.anba.es6draft.runtime.internal.ScriptException;
import com.github.anba.es6draft.runtime.internal.Source;

/**
 *
 */
final class SharedFunctions {
    private SharedFunctions() {
    }

    /**
     * Resolves a file from the runtime base directory location.
     * 
     * @param cx
     *            the execution context
     * @param file
     *            the file
     * @return the file resolved from the runtime base directory
     */
    static Path absolutePath(ExecutionContext cx, Path file) {
        return cx.getRuntimeContext().getBaseDirectory().resolve(file);
    }

    /**
     * Resolves a file from the current script file location.
     * 
     * @param cx
     *            the execution context
     * @param caller
     *            the caller execution context
     * @param file
     *            the file
     * @return the path resolved from the current script
     */
    static Path relativePathToScript(ExecutionContext cx, ExecutionContext caller, Path file) {
        Source source = Objects.requireNonNull(cx.getRealm().sourceInfo(caller));
        Path sourceFile = Objects.requireNonNull(source.getFile());
        Path relativeFile = Objects.requireNonNull(sourceFile.getParent()).resolve(file);
        return absolutePath(cx, relativeFile);
    }

    /**
     * Reads a file and returns its content.
     * 
     * @param cx
     *            the execution context
     * @param fileName
     *            the file name
     * @param path
     *            the file path
     * @return the file content
     */
    static String readFile(ExecutionContext cx, Path fileName, Path path) {
        if (!Files.exists(path)) {
            throw new ScriptException(String.format("can't open '%s'", fileName.toString()));
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw Errors.newError(cx, Objects.toString(e.getMessage(), ""));
        }
    }

    /**
     * Reads a file and evalutes its content.
     * 
     * @param cx
     *            the execution context
     * @param fileName
     *            the file name
     * @param path
     *            the file path
     * @throws ParserException
     *             if the source contains any syntax errors
     * @throws CompilationException
     *             if the parsed source cannot be compiled
     */
    static void loadScript(ExecutionContext cx, Path fileName, Path path) throws ParserException, CompilationException {
        if (!Files.exists(path)) {
            throw new ScriptException(String.format("can't open '%s'", fileName.toString()));
        }
        try {
            Realm realm = cx.getRealm();
            Source source = new Source(path, fileName.toString(), 1);
            Script script = realm.getScriptLoader().script(source, path);
            script.evaluate(realm);
        } catch (IOException e) {
            throw Errors.newError(cx, Objects.toString(e.getMessage(), ""));
        }
    }
}
