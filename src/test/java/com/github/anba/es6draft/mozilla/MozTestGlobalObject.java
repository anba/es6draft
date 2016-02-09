/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.mozilla;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.compiler.CompilationException;
import com.github.anba.es6draft.parser.ParserException;
import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.BaseShellFunctions;
import com.github.anba.es6draft.repl.global.MozShellFunctions;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;

/**
 *
 */
final class MozTestGlobalObject extends ShellGlobalObject {
    MozTestGlobalObject(Realm realm, ShellConsole console) {
        super(realm, console);
    }

    static void testLoadInitializationScript() throws IOException {
        getScriptURL("mozlegacy.js");
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException, ParserException, CompilationException {
        includeNative("mozlegacy.js");
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(getConsole()), BaseShellFunctions.class);
        createGlobalProperties(new MozShellFunctions(), MozShellFunctions.class);
        createGlobalProperties(new TestingFunctions(), TestingFunctions.class);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @return the object allocator to construct new global object instances
     */
    static ObjectAllocator<MozTestGlobalObject> newGlobalObjectAllocator(final ShellConsole console) {
        return new ObjectAllocator<MozTestGlobalObject>() {
            @Override
            public MozTestGlobalObject newInstance(Realm realm) {
                return new MozTestGlobalObject(realm, console);
            }
        };
    }
}
