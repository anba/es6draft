/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import java.io.IOException;
import java.net.URISyntaxException;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;

/**
 * Global object class with support for some moz-shell functions
 */
public final class MozShellGlobalObject extends ShellGlobalObject {
    MozShellGlobalObject(Realm realm, ShellConsole console) {
        super(realm, console);
    }

    @Override
    public void initializeScripted() throws IOException, URISyntaxException {
        includeNative("mozlegacy.js");
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(getConsole()), BaseShellFunctions.class);
        createGlobalProperties(new MozShellFunctions(), MozShellFunctions.class);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @return the object allocator to construct new global object instances
     */
    public static ObjectAllocator<MozShellGlobalObject> newGlobalObjectAllocator(final ShellConsole console) {
        return new ObjectAllocator<MozShellGlobalObject>() {
            @Override
            public MozShellGlobalObject newInstance(Realm realm) {
                return new MozShellGlobalObject(realm, console);
            }
        };
    }
}
