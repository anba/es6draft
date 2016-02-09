/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;

/**
 * Global object class with support for some v8-shell functions
 */
public final class V8ShellGlobalObject extends ShellGlobalObject {
    V8ShellGlobalObject(Realm realm, ShellConsole console) {
        super(realm, console);
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException, ParserException, CompilationException {
        includeNative("v8legacy.js");
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(getConsole()), BaseShellFunctions.class);
        createGlobalProperties(new V8ShellFunctions(), V8ShellFunctions.class);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @return the object allocator to construct new global object instances
     */
    public static ObjectAllocator<V8ShellGlobalObject> newGlobalObjectAllocator(final ShellConsole console) {
        return new ObjectAllocator<V8ShellGlobalObject>() {
            @Override
            public V8ShellGlobalObject newInstance(Realm realm) {
                return new V8ShellGlobalObject(realm, console);
            }
        };
    }
}
