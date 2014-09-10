/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft;

import com.github.anba.es6draft.repl.console.ShellConsole;
import com.github.anba.es6draft.repl.global.SimpleShellGlobalObject;
import com.github.anba.es6draft.runtime.Realm;
import com.github.anba.es6draft.runtime.internal.ObjectAllocator;
import com.github.anba.es6draft.runtime.internal.ScriptCache;
import com.github.anba.es6draft.util.TestInfo;

/**
 *
 */
public final class TestGlobalObject extends SimpleShellGlobalObject {
    private final TestInfo test;

    public TestGlobalObject(Realm realm, ShellConsole console, TestInfo test,
            ScriptCache scriptCache) {
        super(realm, console, test.getBaseDir(), test.getScript(), scriptCache);
        this.test = test;
    }

    /**
     * Returns the test descriptor.
     * 
     * @return the test descriptor
     */
    public TestInfo getTest() {
        return test;
    }

    /**
     * Returns an object to allocate new instances of this class.
     * 
     * @param console
     *            the console object
     * @param test
     *            the test descriptor
     * @param scriptCache
     *            the script cache
     * @return the object allocator to construct new global object instances
     */
    public static ObjectAllocator<TestGlobalObject> newGlobalObjectAllocator(
            final ShellConsole console, final TestInfo test, final ScriptCache scriptCache) {
        return new ObjectAllocator<TestGlobalObject>() {
            @Override
            public TestGlobalObject newInstance(Realm realm) {
                return new TestGlobalObject(realm, console, test, scriptCache);
            }
        };
    }
}
