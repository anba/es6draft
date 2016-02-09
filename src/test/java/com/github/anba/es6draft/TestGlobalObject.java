/**
 * Copyright (c) 2012-2015 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.BaseShellFunctions;
import com.github.anba.es6draft.repl.global.ShellFunctions;
import com.github.anba.es6draft.repl.global.ShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;

/**
 *
 */
public final class TestGlobalObject extends ShellGlobalObject {
    TestGlobalObject(Realm realm, ShellConsole console) {
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
    public static ObjectAllocator<TestGlobalObject> newGlobalObjectAllocator(final ShellConsole console) {
        return new ObjectAllocator<TestGlobalObject>() {
            @Override
            public TestGlobalObject newInstance(Realm realm) {
                return new TestGlobalObject(realm, console);
            }
        };
    }
}
