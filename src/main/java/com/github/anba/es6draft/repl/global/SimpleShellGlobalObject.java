/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.global;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;

/**
 *
 */
public final class SimpleShellGlobalObject extends ShellGlobalObject {
    SimpleShellGlobalObject(Realm realm, ShellConsole console) {
        super(realm, console);
    }

    @Override
    public void initializeExtensions() {
        createGlobalProperties(new BaseShellFunctions(getConsole()), BaseShellFunctions.class);
        createGlobalProperties(new ShellFunctions(), ShellFunctions.class);
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @return the object allocator to construct new global object instances
     */
    public static ObjectAllocator<SimpleShellGlobalObject> newGlobalObjectAllocator(final ShellConsole console) {
        return new ObjectAllocator<SimpleShellGlobalObject>() {
            @Override
            public SimpleShellGlobalObject newInstance(Realm realm) {
                return new SimpleShellGlobalObject(realm, console);
            }
        };
    }
}
