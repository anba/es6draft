/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.Properties.Function;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.runtime.internal.Strings;

/**
 * Global object class with support for some v8-shell functions
 */
public class V8ShellGlobalObject extends ShellGlobalObject {
    private static final String LEGACY_SCRIPT = "v8legacy.js";

    public V8ShellGlobalObject(Realm realm, ShellConsole console, Path baseDir, Path script,
            ScriptCache scriptCache) {
        super(realm, console, baseDir, script, scriptCache);
    }

    @Override
    protected void initializeExtensions() {
        super.initializeExtensions();
        install(this, V8ShellGlobalObject.class);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @param baseDir
     *            the base directory
     * @param script
     *            the main script file
     * @param scriptCache
     *            the script cache
     * @return the object allocator to construct new global object instances
     */
    public static ObjectAllocator<V8ShellGlobalObject> newGlobalObjectAllocator(
            final ShellConsole console, final Path baseDir, final Path script,
            final ScriptCache scriptCache) {
        return new ObjectAllocator<V8ShellGlobalObject>() {
            @Override
            public V8ShellGlobalObject newInstance(Realm realm) {
                return new V8ShellGlobalObject(realm, console, baseDir, script, scriptCache);
            }
        };
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException, ParserException,
            CompilationException {
        includeNative(LEGACY_SCRIPT);
    }

    /**
     * shell-function: {@code write(message)}
     *
     * @param messages
     *            the strings to write
     */
    @Function(name = "write", arity = 1)
    public void write(String... messages) {
        console.putstr(Strings.concatWith(' ', messages));
    }
}
