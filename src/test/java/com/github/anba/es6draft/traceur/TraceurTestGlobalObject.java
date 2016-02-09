/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.traceur;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;

/**
 * 
 */
final class TraceurTestGlobalObject extends ShellGlobalObject {
    TraceurTestGlobalObject(Realm realm, ShellConsole console) {
        super(realm, console);
    }

    static void testLoadInitializationScript() throws IOException {
        getScriptURL("v8legacy.js");
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException, ParserException, CompilationException {
        includeNative("v8legacy.js");
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @return the object allocator to construct new global object instances
     */
    static ObjectAllocator<TraceurTestGlobalObject> newGlobalObjectAllocator(final ShellConsole console) {
        return new ObjectAllocator<TraceurTestGlobalObject>() {
            @Override
            public TraceurTestGlobalObject newInstance(Realm realm) {
                return new TraceurTestGlobalObject(realm, console);
            }
        };
    }
}
